/**
 * deployments/<network>.json 을 읽어 컨트랙트 4종의 소스를 한 번에 검증한다.
 * GIWA 익스플로러는 Blockscout이라 API 키가 필요 없다.
 *
 *   npx hardhat run scripts/verify.js --network giwaSepolia
 *
 * 이미 검증된 컨트랙트는 건너뛴다(재실행해도 안전하다).
 */
const fs = require("fs");
const path = require("path");
const hre = require("hardhat");

async function main() {
  const net = hre.network.name;
  const file = path.join(__dirname, "..", "deployments", `${net}.json`);

  if (!fs.existsSync(file)) {
    throw new Error(`배포 기록이 없습니다: ${path.relative(process.cwd(), file)}\n먼저 scripts/deploy.js 를 실행하세요.`);
  }

  const record = JSON.parse(fs.readFileSync(file, "utf8"));
  const results = [];

  for (const [name, c] of Object.entries(record.contracts)) {
    process.stdout.write(`${name.padEnd(18)} ${c.address} … `);
    try {
      await hre.run("verify:verify", { address: c.address, constructorArguments: c.args });
      console.log("검증 완료");
      results.push([name, "verified"]);
    } catch (e) {
      const message = String(e.message || e);
      if (/already verified|Smart-contract already verified/i.test(message)) {
        console.log("이미 검증됨");
        results.push([name, "already verified"]);
      } else {
        console.log("실패");
        console.error(`   ↳ ${message.split("\n")[0]}`);
        results.push([name, `failed: ${message.split("\n")[0]}`]);
      }
    }
  }

  const explorer = record.chainId === 91342 ? "https://sepolia-explorer.giwa.io" : "https://explorer.giwa.io";

  console.log("");
  console.log("─".repeat(64));
  console.log("지원서 9번(Verified Contract Link)에 붙여넣을 링크:");
  console.log("");
  for (const [name, status] of results) {
    const addr = record.contracts[name].address;
    console.log(`${name.padEnd(18)} ${explorer}/address/${addr}#code`);
    if (!status.startsWith("verified") && !status.startsWith("already")) {
      console.log(`  ⚠ ${status}`);
    }
  }
  console.log("─".repeat(64));
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
