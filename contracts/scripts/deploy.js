/**
 * StepUp 컨트랙트 4종을 순서대로 배포한다.
 *
 *   1. SUPToken          — 10억 SUP 전량을 treasury로 발행
 *   2. SneakerNFT        — SUP를 소각해 민팅/강화
 *   3. RewardDistributor — 러닝 증명을 받아 SUP 지급
 *   4. CourseRegistry    — 코스 작성자·완주 기록 공개
 *
 * 배포 후 리워드 풀에 SUP를 넣는 것까지 한 번에 처리하고,
 * 결과를 deployments/<network>.json 으로 남긴다(검증 스크립트가 이걸 읽는다).
 *
 *   npx hardhat run scripts/deploy.js --network giwaSepolia
 */
const fs = require("fs");
const path = require("path");
const hre = require("hardhat");

// 리워드 풀 초기 자금 — 전체 공급의 5%. 테스트넷에서 첫 에포크를 돌리기 충분하다.
const INITIAL_POOL = hre.ethers.parseEther("50000000");

// 스니커즈 메타데이터 베이스 URI. IPFS에 올린 뒤 .env로 덮어쓰면 된다.
const DEFAULT_BASE_URI = "ipfs://REPLACE_WITH_CID/";

async function main() {
  const [deployer] = await hre.ethers.getSigners();
  const net = hre.network.name;
  const chainId = (await hre.ethers.provider.getNetwork()).chainId;

  // treasury / attester 를 따로 지정하지 않으면 배포자가 겸한다.
  // 실서비스에서는 반드시 분리하고, treasury는 멀티시그여야 한다.
  const treasury = process.env.TREASURY_ADDRESS || deployer.address;
  const attester = process.env.ATTESTER_ADDRESS || deployer.address;
  const roller = process.env.ROLLER_ADDRESS || attester;
  const baseURI = process.env.SNEAKER_BASE_URI || DEFAULT_BASE_URI;

  const balance = await hre.ethers.provider.getBalance(deployer.address);

  console.log("─".repeat(64));
  console.log(`network   : ${net} (chainId ${chainId})`);
  console.log(`deployer  : ${deployer.address}`);
  console.log(`balance   : ${hre.ethers.formatEther(balance)} ETH`);
  console.log(`treasury  : ${treasury}${treasury === deployer.address ? "  (= deployer)" : ""}`);
  console.log(`attester  : ${attester}${attester === deployer.address ? "  (= deployer)" : ""}`);
  console.log(`roller    : ${roller}`);
  console.log("─".repeat(64));

  if (balance === 0n) {
    throw new Error("배포 계정에 가스가 없습니다. https://faucet.giwa.io 에서 테스트 ETH를 받으세요.");
  }

  // 1) SUPToken
  const SUPToken = await hre.ethers.getContractFactory("SUPToken");
  const sup = await SUPToken.deploy(treasury);
  await sup.waitForDeployment();
  const supAddress = await sup.getAddress();
  console.log(`✓ SUPToken           ${supAddress}`);

  // 2) SneakerNFT
  const SneakerNFT = await hre.ethers.getContractFactory("SneakerNFT");
  const sneaker = await SneakerNFT.deploy(supAddress, treasury, roller, baseURI);
  await sneaker.waitForDeployment();
  const sneakerAddress = await sneaker.getAddress();
  console.log(`✓ SneakerNFT         ${sneakerAddress}`);

  // 3) RewardDistributor
  const RewardDistributor = await hre.ethers.getContractFactory("RewardDistributor");
  const distributor = await RewardDistributor.deploy(supAddress, attester);
  await distributor.waitForDeployment();
  const distributorAddress = await distributor.getAddress();
  console.log(`✓ RewardDistributor  ${distributorAddress}`);

  // 4) CourseRegistry
  const CourseRegistry = await hre.ethers.getContractFactory("CourseRegistry");
  const registry = await CourseRegistry.deploy(attester);
  await registry.waitForDeployment();
  const registryAddress = await registry.getAddress();
  console.log(`✓ CourseRegistry     ${registryAddress}`);

  // 5) 리워드 풀 자금 투입 — treasury가 배포자일 때만 자동으로 처리한다.
  let funded = "0";
  if (treasury.toLowerCase() === deployer.address.toLowerCase()) {
    const approveTx = await sup.approve(distributorAddress, INITIAL_POOL);
    await approveTx.wait();
    const fundTx = await distributor.fund(INITIAL_POOL);
    await fundTx.wait();
    funded = hre.ethers.formatEther(INITIAL_POOL);
    console.log(`✓ reward pool funded ${funded} SUP`);
  } else {
    console.log(`! treasury가 배포자와 달라 풀 자금 투입은 건너뜁니다.`);
    console.log(`  treasury에서 직접: sup.approve(${distributorAddress}, amount) → distributor.fund(amount)`);
  }

  // 배포 기록 저장
  const record = {
    network: net,
    chainId: Number(chainId),
    deployedAt: new Date().toISOString(),
    deployer: deployer.address,
    treasury,
    attester,
    roller,
    baseURI,
    rewardPoolFunded: funded,
    contracts: {
      SUPToken: { address: supAddress, args: [treasury] },
      SneakerNFT: { address: sneakerAddress, args: [supAddress, treasury, roller, baseURI] },
      RewardDistributor: { address: distributorAddress, args: [supAddress, attester] },
      CourseRegistry: { address: registryAddress, args: [attester] },
    },
  };

  const outDir = path.join(__dirname, "..", "deployments");
  fs.mkdirSync(outDir, { recursive: true });
  const outFile = path.join(outDir, `${net}.json`);
  fs.writeFileSync(outFile, `${JSON.stringify(record, null, 2)}\n`);

  console.log("─".repeat(64));
  console.log(`배포 기록 → ${path.relative(process.cwd(), outFile)}`);
  console.log("");
  console.log("다음 단계 — 소스 검증:");
  console.log(`  npx hardhat run scripts/verify.js --network ${net}`);
  console.log("");
  console.log("익스플로러에서 확인:");
  const explorer = chainId === 91342n ? "https://sepolia-explorer.giwa.io" : "https://explorer.giwa.io";
  for (const [name, c] of Object.entries(record.contracts)) {
    console.log(`  ${name.padEnd(18)} ${explorer}/address/${c.address}`);
  }
  console.log("─".repeat(64));
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
