/**
 * Blockscout에 소스를 **파일 업로드로** 직접 올린다. hardhat-verify를 거치지 않는다.
 *
 *   npx hardhat run scripts/verify-blockscout.js --network giwaSepolia
 *
 * ## 왜 hardhat-verify로는 안 되는가
 *
 * hardhat-verify는 Etherscan 호환 `/api` 로 소스를 **폼 필드에 문자열로** 넣어
 * 보낸다. 우리 표준 JSON 입력은 필요한 소스만 골라도 180KB다. Blockscout 앞단이
 * 그 크기의 폼 필드를 거절하고 HTML 오류 페이지를 돌려주면, hardhat은 그걸
 * JSON으로 파싱하려다 터진다:
 *
 *     Unexpected token '<', "<!DOCTYPE "... is not valid JSON
 *
 * 실제로 12KB짜리 `CourseRegistry` 하나만 통과하고 나머지 셋이 전부 이 자리에서
 * 막혔다. 재시도로는 풀리지 않는다 — 크기는 시간이 지나도 줄지 않는다.
 *
 * ## 그래서 어떻게 하는가
 *
 * 익스플로러 웹 화면이 실제로 쓰는 v2 엔드포인트에 **multipart 파일 첨부**로
 * 보낸다. 파일은 폼 필드와 달리 크기 제한이 사실상 없다. 웹 화면에서 손으로
 * 올리는 것과 완전히 같은 경로이고, 그 과정을 스크립트가 대신할 뿐이다.
 *
 * 실패하면 서버가 뭐라고 했는지 **응답 본문을 그대로** 보여준다. 파싱하다
 * 터져서 원인을 못 보는 일이 없게.
 */
const fs = require("fs");
const path = require("path");
const hre = require("hardhat");
const { minimalInput, compilerVersion } = require("./lib/minimal-input.js");

/** 검증은 서버가 비동기로 처리한다. 제출 뒤 이만큼까지 기다린다. */
const POLL_TIMEOUT_MS = 90_000;
const POLL_EVERY_MS = 5_000;

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

function explorerFor(chainId) {
  return chainId === 91342 ? "https://sepolia-explorer.giwa.io" : "https://explorer.giwa.io";
}

async function isVerified(explorer, address) {
  try {
    const res = await fetch(`${explorer}/api/v2/smart-contracts/${address}`, {
      headers: { accept: "application/json" },
    });
    if (!res.ok) return null;
    const body = await res.json();
    return Boolean(body?.is_verified);
  } catch {
    return null;
  }
}

/**
 * 한 번 제출한다. 서버 응답을 그대로 돌려준다.
 *
 * `contractName` 을 바꿔가며 여러 번 부를 수 있게 만들어 뒀다. Blockscout
 * 버전에 따라 완전한 이름(`contracts/X.sol:X`)을 받기도 하고 짧은 이름만
 * 받기도 해서, 한 형태로 단정하지 않는다.
 */
async function submit(explorer, address, { json, compiler, contractName, constructorArgs }) {
  const form = new FormData();
  form.append("compiler_version", compiler);
  form.append("license_type", "mit");
  form.append("autodetect_constructor_args", "false");
  form.append("constructor_args", constructorArgs ? `0x${constructorArgs}` : "");
  if (contractName) form.append("contract_name", contractName);
  form.append(
    "files[0]",
    new Blob([json], { type: "application/json" }),
    "standard-input.json",
  );

  const res = await fetch(
    `${explorer}/api/v2/smart-contracts/${address}/verification/via/standard-input`,
    { method: "POST", body: form, headers: { accept: "application/json" } },
  );

  const text = await res.text();
  let body;
  try {
    body = JSON.parse(text);
  } catch {
    body = { raw: text.slice(0, 300) };
  }
  return { ok: res.ok, status: res.status, body };
}

async function verifyOne({ explorer, name, contract, buildInfo }) {
  if (await isVerified(explorer, contract.address)) {
    console.log("  이미 검증됨 — 건너뜁니다");
    return true;
  }

  const sourceName = `contracts/${name}.sol`;
  const { input, kept, total } = minimalInput(buildInfo, sourceName);
  const json = JSON.stringify(input);
  console.log(`  소스 ${kept}/${total}개, ${(json.length / 1024).toFixed(0)}KB`);

  const artifact = await hre.artifacts.readArtifact(`${sourceName}:${name}`);
  const ctor = artifact.abi.find((f) => f.type === "constructor");
  const types = (ctor?.inputs ?? []).map((i) => i.type);
  const constructorArgs = types.length
    ? hre.ethers.AbiCoder.defaultAbiCoder().encode(types, contract.args).slice(2)
    : "";

  // 이름 형태를 바꿔가며 시도한다. 셋 다 실패해야 진짜 실패다.
  const attempts = [`${sourceName}:${name}`, name, null];
  const compiler = compilerVersion(buildInfo);

  for (const contractName of attempts) {
    const label = contractName ?? "(이름 생략, 바이트코드로 자동 탐지)";
    const res = await submit(explorer, contract.address, {
      json,
      compiler,
      contractName,
      constructorArgs,
    });

    if (res.ok) {
      console.log(`  제출됨 (${label}) — 서버 처리 대기 중…`);
      const deadline = Date.now() + POLL_TIMEOUT_MS;
      while (Date.now() < deadline) {
        await sleep(POLL_EVERY_MS);
        if (await isVerified(explorer, contract.address)) {
          console.log("  ✓ 검증 완료");
          return true;
        }
      }
      console.log("  제출은 됐는데 아직 반영 전입니다. 잠시 뒤 다시 실행하세요.");
      return false;
    }

    const reason = res.body?.message || res.body?.errors || res.body?.raw || `HTTP ${res.status}`;
    console.log(`  ✗ ${label} → ${JSON.stringify(reason).slice(0, 200)}`);

    // 이미 검증된 상태라고 답하는 경우도 있다
    if (/already verified/i.test(JSON.stringify(res.body))) {
      console.log("  ✓ 이미 검증돼 있습니다");
      return true;
    }
  }
  return false;
}

async function main() {
  if (typeof FormData === "undefined" || typeof Blob === "undefined") {
    throw new Error(
      "이 스크립트는 Node.js 18 이상이 필요합니다. `node -v` 로 확인하고 nodejs.org 에서 LTS를 설치하세요.",
    );
  }

  const net = hre.network.name;
  const file = path.join(__dirname, "..", "deployments", `${net}.json`);
  if (!fs.existsSync(file)) {
    throw new Error(`배포 기록이 없습니다: ${path.relative(process.cwd(), file)}`);
  }

  const record = JSON.parse(fs.readFileSync(file, "utf8"));
  const explorer = explorerFor(record.chainId);
  const entries = Object.entries(record.contracts);

  // 어느 컨트랙트든 build-info는 같은 컴파일 작업에서 나온다
  const buildInfo = await hre.artifacts.getBuildInfo(
    `contracts/${entries[0][0]}.sol:${entries[0][0]}`,
  );
  if (!buildInfo) throw new Error("빌드 정보가 없습니다. 먼저 `npm run build` 를 실행하세요.");

  console.log("─".repeat(64));
  console.log(`익스플로러  ${explorer}`);
  console.log(`컴파일러    ${compilerVersion(buildInfo)}`);
  console.log("─".repeat(64));

  for (const [name, contract] of entries) {
    console.log(`${name}  ${contract.address}`);
    await verifyOne({ explorer, name, contract, buildInfo });
    console.log("");
  }

  // ── 최종 판정은 익스플로러에게 다시 묻는다 ──────────────────
  console.log("익스플로러에 최종 확인 중…");
  const finalState = [];
  for (const [name, c] of entries) {
    finalState.push([name, c.address, await isVerified(explorer, c.address)]);
  }

  const ok = finalState.filter(([, , v]) => v === true);

  console.log("");
  console.log("─".repeat(64));
  console.log(`검증 완료 ${ok.length}/${entries.length}`);
  console.log("");
  console.log("지원서 9번(Verified Contract Link)에 붙여넣을 링크:");
  console.log("");
  for (const [name, addr, verified] of finalState) {
    const mark = verified === true ? "✓" : verified === false ? "✗" : "?";
    console.log(`${mark} ${name.padEnd(18)} ${explorer}/address/${addr}#code`);
  }

  if (ok.length !== entries.length) {
    console.log("");
    console.log("안 된 것이 있으면 한 번 더 실행해 보세요. 그래도 같으면:");
    console.log("  npm run standard-json:giwa");
    console.log("  → 웹 화면에서 직접 올릴 파일과 생성자 인자를 뽑아 줍니다.");
    process.exitCode = 1;
  }
  console.log("─".repeat(64));
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
