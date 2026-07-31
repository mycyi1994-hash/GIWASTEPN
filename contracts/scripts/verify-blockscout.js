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
    {
      method: "POST",
      body: form,
      headers: {
        accept: "application/json",
        // 브라우저처럼 보이게 한다. Cloudflare 봇 차단이 원인이라면 이것만으로
        // 통과하고, 크기 제한이 원인이라면 아무 차이가 없다 — 어느 쪽인지
        // 가려내는 데도 쓰인다.
        "user-agent":
          "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
        "accept-language": "en-US,en;q=0.9",
        origin: explorer,
        referer: `${explorer}/address/${address}/contract-verification`,
      },
    },
  );

  const text = await res.text();
  let body;
  try {
    body = JSON.parse(text);
  } catch {
    body = { raw: text };
  }
  return { ok: res.ok, status: res.status, body, text };
}

/**
 * 두 번째 경로 — 소스를 **파일 여러 개로 쪼개서** 보낸다.
 *
 * 표준 JSON 입력은 아무리 줄여도 덩어리 하나가 180KB다. 앞단이 **파트 하나의
 * 크기**를 제한하는 것이라면, 같은 내용을 22개 파일로 나눠 보내면 통과한다
 * (가장 큰 파일이 35KB). 총량 제한이라면 이것도 막힌다 — 어느 쪽인지 시도해
 * 봐야 안다.
 *
 * 컴파일 설정은 표준 JSON에 들어 있던 값을 그대로 꺼내 쓴다. 하나라도
 * 어긋나면 바이트코드가 달라져 검증이 실패한다.
 */
async function submitMultiPart(explorer, address, { input, compiler, contractName, constructorArgs }) {
  const settings = input.settings ?? {};
  const form = new FormData();
  form.append("compiler_version", compiler);
  form.append("evm_version", settings.evmVersion ?? "default");
  form.append("is_optimization_enabled", String(Boolean(settings.optimizer?.enabled)));
  if (settings.optimizer?.enabled) {
    form.append("optimization_runs", String(settings.optimizer.runs ?? 200));
  }
  form.append("autodetect_constructor_args", "false");
  form.append("constructor_args", constructorArgs ? `0x${constructorArgs}` : "");
  if (contractName) form.append("contract_name", contractName);

  Object.entries(input.sources).forEach(([sourceName, { content }], i) => {
    form.append(
      `files[${i}]`,
      new Blob([content], { type: "text/plain" }),
      sourceName,
    );
  });

  const res = await fetch(
    `${explorer}/api/v2/smart-contracts/${address}/verification/via/multi-part`,
    {
      method: "POST",
      body: form,
      headers: {
        accept: "application/json",
        "user-agent":
          "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
        origin: explorer,
        referer: `${explorer}/address/${address}/contract-verification`,
      },
    },
  );

  const text = await res.text();
  let body;
  try {
    body = JSON.parse(text);
  } catch {
    body = { raw: text };
  }
  return { ok: res.ok, status: res.status, body, text };
}

/**
 * 실패 이유를 한 줄로 요약한다.
 *
 * Cloudflare가 막으면 본문이 통째로 HTML이라, 앞부분만 잘라 보여줘 봐야
 * `<!DOCTYPE html>` 밖에 안 보인다. 정작 필요한 것은 **HTTP 상태 코드**와
 * **Cloudflare 오류 번호**다. 그 둘이 원인을 갈라 준다.
 *
 *   413        → 요청이 너무 크다. 소스를 줄이거나 다른 경로로 가야 한다.
 *   403 / 1020 → 방화벽 규칙. 브라우저에서는 되는데 스크립트라서 막힌 것.
 *   1015       → 레이트 리밋. 기다리면 된다.
 *   5xx        → 익스플로러 쪽 장애. 우리가 할 게 없다.
 */
function explainFailure({ status, body, text }) {
  if (body && !body.raw) {
    const message = body.message || body.errors || body;
    return `HTTP ${status} · ${JSON.stringify(message).slice(0, 200)}`;
  }

  const html = text || "";
  const title = html.match(/<title>([^<]*)<\/title>/i)?.[1]?.trim();
  const cfCode = html.match(/error code:\s*(\d+)/i)?.[1] ||
    html.match(/Error\s+(1\d{3})\b/)?.[1];
  const rayId = html.match(/Ray ID:\s*<[^>]*>([0-9a-f]+)/i)?.[1];

  const parts = [`HTTP ${status}`];
  if (title) parts.push(title);
  if (cfCode) parts.push(`Cloudflare ${cfCode}`);
  if (rayId) parts.push(`Ray ${rayId}`);
  parts.push("HTML 응답 (Cloudflare가 막음)");
  return parts.join(" · ");
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

  const compiler = compilerVersion(buildInfo);
  const common = { json, input, compiler, constructorArgs };

  // 경로를 순서대로 시도한다. 앞의 것이 나은 이유는 표준 JSON 입력이
  // 컴파일 설정을 통째로 담고 있어 어긋날 여지가 없기 때문이다.
  // 파일 하나가 커서 막히면 여러 파일로 쪼개 보내는 쪽으로 넘어간다.
  const routes = [
    { via: "standard-input", send: submit, contractName: `${sourceName}:${name}` },
    { via: "standard-input", send: submit, contractName: name },
    { via: "standard-input", send: submit, contractName: null },
    { via: "multi-part", send: submitMultiPart, contractName: `${sourceName}:${name}` },
    { via: "multi-part", send: submitMultiPart, contractName: name },
  ];

  let blockedOn = null;

  for (const route of routes) {
    // 같은 방식이 Cloudflare 벽에 부딪혔으면 이름만 바꿔 다시 보낼 이유가 없다.
    if (blockedOn === route.via) continue;

    const label = `${route.via} · ${route.contractName ?? "이름 자동 탐지"}`;
    const res = await route.send(explorer, contract.address, {
      ...common,
      contractName: route.contractName,
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

    console.log(`  ✗ ${label} → ${explainFailure(res)}`);

    // 이미 검증된 상태라고 답하는 경우도 있다
    if (/already verified/i.test(res.text || "")) {
      console.log("  ✓ 이미 검증돼 있습니다");
      return true;
    }

    // HTML이 돌아왔다면 앞단이 막은 것이다. 이름을 바꿔 봐야 같은 벽이다.
    if (res.body?.raw) blockedOn = route.via;
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
