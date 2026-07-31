require("@nomicfoundation/hardhat-toolbox");
require("dotenv").config();
const { subtask } = require("hardhat/config");
const { TASK_COMPILE_SOLIDITY_GET_SOLC_BUILD } = require("hardhat/builtin-tasks/task-names");

// Hardhat은 기본적으로 binaries.soliditylang.org에서 solc를 내려받는다.
// 사내망·CI처럼 그 호스트로 나갈 수 없는 환경에서는 npm으로 설치한
// solc(soljson wasm)로 대체한다. 같은 버전이면 바이트코드도 동일하므로
// 익스플로러 소스 검증에 영향이 없다.
subtask(TASK_COMPILE_SOLIDITY_GET_SOLC_BUILD, async (args, _hre, runSuper) => {
  try {
    return await runSuper();
  } catch (downloadError) {
    let solcjsPath;
    try {
      solcjsPath = require.resolve("solc/soljson.js");
    } catch {
      throw downloadError;
    }
    // eslint-disable-next-line import/no-extraneous-dependencies
    const longVersion = require("solc").version();
    console.warn(`solc 다운로드 실패 → 로컬 solcjs 사용 (${longVersion})`);
    return {
      compilerPath: solcjsPath,
      isSolcJs: true,
      version: args.solcVersion,
      longVersion,
    };
  }
});

// 배포 키는 .env 에만 둡니다. 저장소에 커밋하지 마세요(.gitignore 처리됨).
const DEPLOYER_KEY = process.env.DEPLOYER_PRIVATE_KEY;
const accounts = DEPLOYER_KEY ? [DEPLOYER_KEY] : [];

// GIWA Sepolia — Upbit/Dunamu의 OP Stack L2 테스트넷 (Ethereum Sepolia 위)
const GIWA_SEPOLIA_RPC = process.env.GIWA_SEPOLIA_RPC || "https://sepolia-rpc.giwa.io";
const GIWA_SEPOLIA_CHAIN_ID = 91342;
const GIWA_SEPOLIA_EXPLORER = "https://sepolia-explorer.giwa.io";

// GIWA 메인넷 — 준비 중. RPC가 공개되면 .env로 주입하면 그대로 동작합니다.
const GIWA_MAINNET_RPC = process.env.GIWA_MAINNET_RPC || "";
const GIWA_MAINNET_CHAIN_ID = 9134;
const GIWA_MAINNET_EXPLORER = process.env.GIWA_MAINNET_EXPLORER || "https://explorer.giwa.io";

/** @type import('hardhat/config').HardhatUserConfig */
module.exports = {
  solidity: {
    version: "0.8.28",
    settings: {
      optimizer: { enabled: true, runs: 200 },
      // OP Stack은 Ecotone(2024-03)부터 Cancun EVM을 켰다. GIWA는 그 이후에
      // 올라온 체인이라 MCOPY/TSTORE를 쓸 수 있고, OpenZeppelin 5.6도 이를 요구한다.
      evmVersion: "cancun",
    },
  },

  networks: {
    hardhat: {
      chainId: 31337,
    },
    giwaSepolia: {
      url: GIWA_SEPOLIA_RPC,
      chainId: GIWA_SEPOLIA_CHAIN_ID,
      accounts,
    },
    giwa: {
      url: GIWA_MAINNET_RPC,
      chainId: GIWA_MAINNET_CHAIN_ID,
      accounts,
    },
  },

  // GIWA 익스플로러는 Blockscout이다. Blockscout은 Etherscan 호환 /api를
  // 제공하므로 hardhat-verify를 그대로 쓸 수 있고, API 키는 필요 없다
  // (형식상 아무 문자열이나 넣어야 해서 "blockscout"으로 채운다).
  etherscan: {
    apiKey: {
      giwaSepolia: process.env.GIWA_EXPLORER_API_KEY || "blockscout",
      giwa: process.env.GIWA_EXPLORER_API_KEY || "blockscout",
    },
    customChains: [
      {
        network: "giwaSepolia",
        chainId: GIWA_SEPOLIA_CHAIN_ID,
        urls: {
          apiURL: `${GIWA_SEPOLIA_EXPLORER}/api`,
          browserURL: GIWA_SEPOLIA_EXPLORER,
        },
      },
      {
        network: "giwa",
        chainId: GIWA_MAINNET_CHAIN_ID,
        urls: {
          apiURL: `${GIWA_MAINNET_EXPLORER}/api`,
          browserURL: GIWA_MAINNET_EXPLORER,
        },
      },
    ],
  },

  sourcify: {
    enabled: true,
  },

  gasReporter: {
    enabled: process.env.REPORT_GAS === "true",
  },
};
