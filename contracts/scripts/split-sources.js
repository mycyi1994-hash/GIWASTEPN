/**
 * 익스플로러의 **Multi-part files** 검증에 올릴 `.sol` 파일들을 뽑아 준다.
 *
 *   npx hardhat run scripts/split-sources.js --network giwaSepolia
 *
 * ## 왜 이게 필요한가
 *
 * Standard JSON Input은 컨트랙트 하나가 185KB짜리 파일 하나다. GIWA 익스플로러는
 * 그 크기를 스크립트로도 브라우저로도 받지 않았다. 한도가 **요청 전체**가 아니라
 * **파일 하나**에 걸린 것이라면, 같은 내용을 22~32개 파일로 나눠 올리면 통과한다
 * (가장 큰 파일이 35KB).
 *
 * 총량 한도라면 이것도 막힌다. 해 보기 전에는 알 수 없고, 5분이면 판가름 난다.
 *
 * 파일 이름에 원래 경로를 담는다 — `@openzeppelin/contracts/token/ERC20/ERC20.sol`
 * 은 `@openzeppelin_contracts_token_ERC20_ERC20.sol` 이 된다. Blockscout이
 * import 경로를 파일명으로 되돌려 찾기 때문에, 이 규칙을 지켜야 붙는다.
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

  const names = Object.keys(record.contracts);
  const buildInfo = await hre.artifacts.getBuildInfo(`contracts/${names[0]}.sol:${names[0]}`);
  if (!buildInfo) throw new Error("빌드 정보가 없습니다. 먼저 `npm run build` 를 실행하세요.");

  const settings = buildInfo.input.settings ?? {};

  console.log("─".repeat(64));
  console.log("Multi-part files 검증에 넣을 값");
  console.log("─".repeat(64));
  console.log(`컴파일러      ${compilerVersion(buildInfo)}`);
  console.log(`최적화        ${settings.optimizer?.enabled ? "Yes" : "No"} / runs ${settings.optimizer?.runs ?? 200}`);
  console.log(`EVM 버전      ${settings.evmVersion ?? "default"}`);
  console.log("");

  for (const [name, contract] of Object.entries(record.contracts)) {
    const { input, kept } = minimalInput(buildInfo, `contracts/${name}.sol`);
    const outDir = path.join(__dirname, "..", "verification", name);
    fs.rmSync(outDir, { recursive: true, force: true });
    fs.mkdirSync(outDir, { recursive: true });

    let biggest = 0;
    for (const [sourceName, { content }] of Object.entries(input.sources)) {
      // 경로 구분자를 파일명에 녹인다. Blockscout이 이걸로 import를 되찾는다.
      const flat = sourceName.replace(/[\\/]/g, "_");
      fs.writeFileSync(path.join(outDir, flat), content);
      biggest = Math.max(biggest, content.length);
    }

    console.log(`${name}`);
    console.log(`  검증 페이지   ${explorer}/address/${contract.address}/contract-verification`);
    console.log(`  폴더          ${path.relative(process.cwd(), outDir)}`);
    console.log(`  파일 ${kept}개, 가장 큰 파일 ${(biggest / 1024).toFixed(0)}KB`);
    console.log("");
  }

  console.log("─".repeat(64));
  console.log("익스플로러 화면에서:");
  console.log("  Verification method → Solidity (Multi-part files)");
  console.log("  Compiler → 위 버전, Optimization → Yes / 200, EVM → cancun");
  console.log("  파일 선택 → 위 폴더를 열고 Ctrl+A 로 전부 선택해 드래그");
  console.log("─".repeat(64));
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
