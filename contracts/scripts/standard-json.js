/**
 * 익스플로러에 **손으로** 소스를 올릴 때 필요한 파일을 만들어 준다.
 *
 *   npx hardhat run scripts/standard-json.js --network giwaSepolia
 *
 * ## 왜 필요한가
 *
 * Blockscout의 검증 API는 페이로드가 크면(OpenZeppelin을 끌어오는 컨트랙트가
 * 그렇다) JSON 대신 HTML 오류 페이지를 돌려주는 일이 있다. 그러면
 * `scripts/verify.js` 를 몇 번 돌려도 같은 자리에서 막힌다.
 *
 * 그럴 때 익스플로러 웹 화면에서 직접 올리면 된다. 그 화면이 요구하는 것이
 * **Standard JSON Input** 파일과 **ABI 인코딩된 생성자 인자**인데, 둘 다
 * 사람이 손으로 만들 물건이 아니다. 이 스크립트가 대신 뽑아 준다.
 *
 * 결과는 `verification/` 폴더에 컨트랙트별로 하나씩 떨어진다.
 */
const fs = require("fs");
const path = require("path");
const hre = require("hardhat");
const { minimalInput, compilerVersion } = require("./lib/minimal-input.js");

async function main() {
  const net = hre.network.name;
  const file = path.join(__dirname, "..", "deployments", `${net}.json`);
  if (!fs.existsSync(file)) {
    throw new Error(`배포 기록이 없습니다: ${path.relative(process.cwd(), file)}`);
  }

  const record = JSON.parse(fs.readFileSync(file, "utf8"));
  const explorer = record.chainId === 91342
    ? "https://sepolia-explorer.giwa.io"
    : "https://explorer.giwa.io";

  const outDir = path.join(__dirname, "..", "verification");
  fs.mkdirSync(outDir, { recursive: true });

  const solc = hre.config.solidity.compilers[0];
  const firstName = Object.keys(record.contracts)[0];
  const anyBuildInfo = await hre.artifacts.getBuildInfo(`contracts/${firstName}.sol:${firstName}`);

  console.log("─".repeat(64));
  console.log("익스플로러 수동 검증에 넣을 값");
  console.log("─".repeat(64));
  console.log(`컴파일러      ${anyBuildInfo ? compilerVersion(anyBuildInfo) : `v${solc.version}`}`);
  console.log(`최적화        ${solc.settings.optimizer.enabled ? "Yes" : "No"} / runs ${solc.settings.optimizer.runs}`);
  console.log(`EVM 버전      ${solc.settings.evmVersion}`);
  console.log("");

  for (const [name, c] of Object.entries(record.contracts)) {
    const fqn = `contracts/${name}.sol:${name}`;
    const buildInfo = await hre.artifacts.getBuildInfo(fqn);
    if (!buildInfo) {
      console.log(`${name.padEnd(18)} ⚠ 빌드 정보 없음 — 먼저 npm run build`);
      continue;
    }

    // 이 컨트랙트에 실제로 필요한 소스만 남긴다. 통째로 넣으면 프로젝트 전체
    // 43개(278KB)가 들어가는데, 익스플로러가 큰 파일에서 자주 막힌다.
    const { input, kept, total } = minimalInput(buildInfo, `contracts/${name}.sol`);
    const jsonPath = path.join(outDir, `${name}.standard-input.json`);
    const serialized = `${JSON.stringify(input, null, 2)}\n`;
    fs.writeFileSync(jsonPath, serialized);

    // 생성자 인자를 ABI로 인코딩한다. 익스플로러 폼이 0x 없는 hex를 받는다.
    const artifact = await hre.artifacts.readArtifact(fqn);
    const ctor = artifact.abi.find((f) => f.type === "constructor");
    const types = (ctor?.inputs ?? []).map((i) => i.type);
    const encoded = types.length
      ? hre.ethers.AbiCoder.defaultAbiCoder().encode(types, c.args).slice(2)
      : "";

    console.log(`${name}`);
    console.log(`  주소          ${c.address}`);
    console.log(`  검증 페이지   ${explorer}/address/${c.address}/contract-verification`);
    console.log(`  JSON 파일     ${path.relative(process.cwd(), jsonPath)}`);
    console.log(`                소스 ${kept}/${total}개, ${(serialized.length / 1024).toFixed(0)}KB`);
    console.log(`  생성자 인자   ${encoded ? `0x${encoded}` : "(없음)"}`);
    console.log("");
  }

  console.log("─".repeat(64));
  console.log("익스플로러 화면에서:");
  console.log("  Verification method → Solidity (Standard JSON Input)");
  console.log("  Compiler → 위에 적힌 버전 그대로");
  console.log("  파일 업로드 → 위 JSON 파일");
  console.log("  Constructor arguments → 위 hex 문자열 (없으면 비워 둠)");
  console.log("─".repeat(64));
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
