/**
 * deployments/<network>.json 을 읽어 컨트랙트 4종의 소스를 검증한다.
 * GIWA 익스플로러는 Blockscout이라 API 키가 필요 없다.
 *
 *   npx hardhat run scripts/verify.js --network giwaSepolia
 *
 * ## 왜 이렇게 방어적인가
 *
 * Blockscout은 검증 요청이 연달아 들어오면 JSON 대신 HTML 오류 페이지를
 * 돌려준다. hardhat-verify는 그걸 파싱하다 터지고
 * (`Unexpected token '<', "<!DOCTYPE "...`), 우리 눈에는 "실패"로만 보인다.
 * 실제로는 잠시 뒤 다시 보내면 통과하는 경우가 대부분이다.
 *
 * 그래서 세 가지를 한다.
 *
 *   1. 보내기 전에 익스플로러에 물어본다 — 이미 검증됐으면 건너뛴다.
 *   2. 네트워크성 오류는 간격을 늘려가며 다시 보낸다.
 *   3. 다 끝나면 **hardhat의 성공/실패가 아니라 익스플로러의 답**으로
 *      최종 상태를 판정한다. 이게 지원서에 적어도 되는지의 유일한 근거다.
 *
 * 재실행해도 안전하다.
 */
const fs = require("fs");
const path = require("path");
const hre = require("hardhat");

/** Blockscout이 숨을 돌릴 시간. 컨트랙트 사이 간격. */
const GAP_MS = 6_000;

/** 네트워크성 오류 재시도 간격 — 6s, 12s, 24s */
const BACKOFF_MS = [6_000, 12_000, 24_000];

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

function explorerFor(chainId) {
  return chainId === 91342 ? "https://sepolia-explorer.giwa.io" : "https://explorer.giwa.io";
}

/**
 * 익스플로러에 직접 물어본다: 이 주소, 소스가 올라가 있나?
 *
 * hardhat이 뭐라고 하든 이게 사실이다. 판단을 여기 한 곳에 모아두면
 * "성공이라는데 페이지엔 안 보인다" 같은 혼란이 생기지 않는다.
 */
async function isVerified(explorer, address) {
  try {
    const res = await fetch(
      `${explorer}/api?module=contract&action=getsourcecode&address=${address}`,
      { headers: { accept: "application/json" } },
    );
    if (!res.ok) return null; // 판단 불가 — 실패로 단정하지 않는다
    const body = await res.json();
    const source = body?.result?.[0]?.SourceCode;
    return typeof source === "string" && source.length > 0;
  } catch {
    return null;
  }
}

/** 다시 보내면 될 오류인가, 코드가 틀린 건가 */
function isTransient(message) {
  return (
    /Unexpected token '<'/.test(message) ||
    /<!DOCTYPE/i.test(message) ||
    /network request failed/i.test(message) ||
    /Failed to send contract verification request/i.test(message) ||
    /ETIMEDOUT|ECONNRESET|ENOTFOUND|socket hang up|fetch failed/i.test(message)
  );
}

function alreadyVerified(message) {
  return /already verified|Smart-contract already verified|Contract source code already verified/i.test(
    message,
  );
}

async function verifyOne(name, contract, explorer) {
  const pre = await isVerified(explorer, contract.address);
  if (pre === true) {
    console.log("이미 검증됨 (건너뜀)");
    return "verified";
  }

  for (let attempt = 0; attempt <= BACKOFF_MS.length; attempt++) {
    try {
      await hre.run("verify:verify", {
        address: contract.address,
        constructorArguments: contract.args,
      });
      console.log("검증 완료");
      return "verified";
    } catch (e) {
      const message = String(e?.message || e);

      if (alreadyVerified(message)) {
        console.log("이미 검증됨");
        return "verified";
      }

      // hardhat이 실패라고 해도 익스플로러엔 올라간 경우가 있다.
      // (Blockscout 검증은 통과했는데 그 뒤 단계에서 터지는 경우)
      if (await isVerified(explorer, contract.address)) {
        console.log("검증 완료 (익스플로러 확인)");
        return "verified";
      }

      if (attempt < BACKOFF_MS.length && isTransient(message)) {
        const wait = BACKOFF_MS[attempt];
        console.log(`일시 오류 — ${wait / 1000}초 후 재시도 (${attempt + 1}/${BACKOFF_MS.length})`);
        await sleep(wait);
        process.stdout.write(`${name.padEnd(18)} ${contract.address} … `);
        continue;
      }

      console.log("실패");
      return `failed: ${message.split("\n")[0]}`;
    }
  }
  return "failed: 재시도 횟수 초과";
}

async function main() {
  const net = hre.network.name;
  const file = path.join(__dirname, "..", "deployments", `${net}.json`);

  if (!fs.existsSync(file)) {
    throw new Error(
      `배포 기록이 없습니다: ${path.relative(process.cwd(), file)}\n먼저 scripts/deploy.js 를 실행하세요.`,
    );
  }

  const record = JSON.parse(fs.readFileSync(file, "utf8"));
  const explorer = explorerFor(record.chainId);
  const entries = Object.entries(record.contracts);
  const results = [];

  for (const [i, [name, c]] of entries.entries()) {
    process.stdout.write(`${name.padEnd(18)} ${c.address} … `);
    results.push([name, await verifyOne(name, c, explorer)]);
    if (i < entries.length - 1) await sleep(GAP_MS);
  }

  // ── 최종 판정은 익스플로러에게 다시 묻는다 ──────────────────
  console.log("");
  console.log("익스플로러에 최종 확인 중…");
  const finalState = [];
  for (const [name] of entries) {
    const addr = record.contracts[name].address;
    finalState.push([name, addr, await isVerified(explorer, addr)]);
  }

  const ok = finalState.filter(([, , v]) => v === true);
  const bad = finalState.filter(([, , v]) => v === false);
  const unknown = finalState.filter(([, , v]) => v === null);

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

  if (bad.length || unknown.length) {
    console.log("");
    console.log("아직 안 된 것이 있습니다. 이 명령을 그대로 한 번 더 실행하세요:");
    console.log(`  npx hardhat run scripts/verify.js --network ${net}`);
    console.log("");
    console.log("여러 번 해도 같으면 익스플로러에서 직접 올릴 수 있습니다:");
    console.log("  주소 페이지 → Contract 탭 → Verify & Publish");
    console.log("  → Solidity (Standard JSON Input) → 아래 파일 업로드");
    console.log(`  ${path.join("contracts", "artifacts", "build-info", "<가장 최근>.json")} 안의 "input"`);
  }
  console.log("─".repeat(64));

  // 하나라도 실패면 0이 아닌 종료 코드 — CI에서 바로 드러나게
  if (ok.length !== entries.length) process.exitCode = 1;
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
