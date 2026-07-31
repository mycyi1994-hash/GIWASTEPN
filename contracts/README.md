# StepUp Contracts — GIWA

온체인 정산 컨트랙트 4종. Solidity 0.8.28 · OpenZeppelin 5.x · Hardhat.

앱의 리워드 계산(`app/.../domain/RewardEconomy.kt`, `domain/Sneaker.kt`)에 있는
상수를 그대로 옮겨 왔습니다. 숫자가 새로 생긴 게 아니라, **서버가 주장하던 값을
누구나 검증할 수 있는 곳으로 옮긴 것**입니다.

| 컨트랙트 | 표준 | 역할 |
|---|---|---|
| `SUPToken` | ERC-20 | 10억 SUP 고정 발행. **배포 후 추가 발행 함수 없음** |
| `SneakerNFT` | ERC-721 | 44종 스니커즈. 스탯 온체인, EIP-712 서명 민팅, 결정적 강화 |
| `RewardDistributor` | — | 서명된 러닝 증명 → SUP 지급. **일일 배출 예산 하드캡** |
| `CourseRegistry` | — | 코스 작성자·완주 횟수 공개 기록, 거리별 정량 보상 |

전체 경제 모델: [`../docs/TOKENOMICS.md`](../docs/TOKENOMICS.md)

---

## 설계에서 중요한 세 가지

**1. 발행 상한이 파라미터가 아니라 `constant`입니다.**
최강 신발(레전더리 만렙)의 부스트는 `1780 bps = +17.8%`. 소유자도 바꿀 수
없습니다. `test/StepUp.test.js`가 전 등급을 순회하며 이 천장을 검증합니다.

**2. 어테스터는 공급을 늘릴 수 없습니다.**
`RewardDistributor`는 "누구에게 얼마"는 어테스터에게 맡기되, "얼마나 존재할 수
있는가"는 넘기지 않습니다. 모든 청구는 `dailyBudget(day)`에서 차감되고, 그날
예산이 소진되면 **누가 서명했든** 청구가 실패합니다. 어테스터 키가 탈취돼도
하루치 배출이 잘못 나갈 뿐, 토큰이 인플레이션되지는 않습니다.

**3. 리워드 풀에서 SUP가 나가는 길은 `claim` 하나뿐입니다.**
`withdraw`, `sweep`, `rescue` 같은 함수가 없습니다. 소유자 권한은 어테스터
교체와 일시정지뿐입니다. 테스트가 ABI를 훑어 탈출구가 없음을 확인합니다.

---

## 빠른 시작 (로컬)

```bash
cd contracts
npm install
npm run build     # 컴파일
npm test          # 테스트 43개
```

> **참고** — 사내망·CI처럼 `binaries.soliditylang.org`로 나갈 수 없는 환경에서는
> `hardhat.config.js`가 npm으로 설치된 `solc`로 자동 대체합니다. 같은 버전이면
> 바이트코드가 동일하므로 익스플로러 소스 검증에 영향이 없습니다.

---

## GIWA Sepolia 테스트넷에 배포하기

### 네트워크 정보

| 항목 | 값 |
|---|---|
| 이름 | GIWA Sepolia |
| Chain ID | **91342** |
| RPC | `https://sepolia-rpc.giwa.io` |
| 익스플로러 | `https://sepolia-explorer.giwa.io` (Blockscout) |
| 파우셋 | `https://faucet.giwa.io` |
| 가스 토큰 | ETH |
| 기반 | OP Stack (Optimism) L2 on Ethereum Sepolia |

GIWA 메인넷(Chain ID 9134)은 준비 중입니다. RPC가 공개되면 `.env`의
`GIWA_MAINNET_RPC`만 채우면 `--network giwa`가 그대로 동작합니다.

### Step 0 — 저장소를 **올바른 브랜치로** 받기

> ⚠️ **`git clone` 만 치면 안 됩니다.** 저장소 기본 브랜치는 아직 옛 버전이라
> `contracts/` 폴더가 들어 있지 않습니다. 그대로 받으면 다음 단계에서
> "지정된 경로를 찾을 수 없습니다" 가 납니다. 반드시 `-b` 로 브랜치를 지정하세요.

**Windows (명령 프롬프트)**

```bat
cd C:\stepup
git clone -b claude/work-history-pjm57c https://github.com/mycyi1994-hash/GIWASTEPN.git work
cd C:\stepup\work\contracts
dir
```

**macOS · Linux**

```bash
git clone -b claude/work-history-pjm57c https://github.com/mycyi1994-hash/GIWASTEPN.git work
cd work/contracts
ls
```

`contracts` `scripts` `test` `hardhat.config.js` `package.json` 이 보이면 성공입니다.
안 보이면 브랜치가 잘못 받아진 것이니 여기서 멈추고 다시 받으세요.

### Step 1 — 배포 전용 지갑 만들기

**기존 지갑을 쓰지 마세요.** 테스트넷 배포용 지갑을 새로 만듭니다.

1. MetaMask → 계정 목록 → **계정 추가** → **새 계정 추가**
2. 이름을 "StepUp 배포용" 등으로 지정
3. 계정 메뉴 → **계정 세부 정보** → **개인 키 표시** → 비밀번호 입력 → 키 복사

> 🔐 **이 개인키는 어디에도 붙여넣지 마세요.** 채팅, 이메일, 이슈, 스크린샷
> 전부 포함입니다. 다음 단계에서 만들 `.env` 파일에만 들어갑니다.

### Step 2 — `.env` 만들기

> **먼저 Node.js가 있어야 합니다.** 명령창에 `node -v` 를 쳐서 버전이 안 나오면
> https://nodejs.org 에서 **LTS**를 설치하고 **명령창을 껐다가 다시 켜세요.**

**Windows (명령 프롬프트)**

```bat
cd C:\stepup\work\contracts
copy .env.example .env
notepad .env
```

**macOS · Linux**

```bash
cd work/contracts
cp .env.example .env
```

열린 파일에 두 줄을 채우고 저장합니다.

```
DEPLOYER_PRIVATE_KEY=0x배포용_계정의_개인키
ATTESTER_ADDRESS=0x심판용_계정의_주소
```

`DEPLOYER_PRIVATE_KEY`는 **개인키**, `ATTESTER_ADDRESS`는 **주소**입니다.
헷갈리기 쉬우니 한 번 더 확인하세요. 심판(어테스터) 계정은 서명만 하므로
가스비가 필요 없고, 그 개인키는 나중에 Cloudflare Secret으로 따로 넣습니다.

`.env`는 `.gitignore`에 있어 커밋되지 않습니다.

### Step 3 — 가스비 받기

1. MetaMask에 GIWA Sepolia 네트워크를 추가합니다
   (`https://chainlist.org/chain/91342` 에서 **Connect Wallet → Add to MetaMask**가 가장 빠릅니다)
2. https://faucet.giwa.io 에서 Step 1의 지갑 주소로 테스트 ETH를 받습니다
3. MetaMask에서 잔액이 0보다 큰지 확인합니다

> 파우셋이 Ethereum Sepolia ETH를 요구하면, 먼저 공개 Sepolia 파우셋에서
> 받은 뒤 https://bridge.giwa.io 로 GIWA Sepolia에 브리지하면 됩니다.

### Step 4 — 배포

```bash
npm run deploy:giwa
```

성공하면 컨트랙트 4종의 주소가 출력되고, 리워드 풀에 5,000만 SUP가 들어가며,
`deployments/giwaSepolia.json`에 기록이 남습니다.

가스가 없으면 이렇게 멈춥니다 — Step 3으로 돌아가세요.

```
Error: 배포 계정에 가스가 없습니다. https://faucet.giwa.io 에서 테스트 ETH를 받으세요.
```

### Step 5 — 소스 검증

```bash
npm run verify:giwa
```

배포 기록을 읽어 4종을 순서대로 검증하고, 마지막에 **지원서 9번 문항에 붙여넣을
링크**를 출력합니다.

```
SUPToken           https://sepolia-explorer.giwa.io/address/0x…#code
SneakerNFT         https://sepolia-explorer.giwa.io/address/0x…#code
RewardDistributor  https://sepolia-explorer.giwa.io/address/0x…#code
CourseRegistry     https://sepolia-explorer.giwa.io/address/0x…#code
```

GIWA 익스플로러는 Blockscout이라 **API 키가 필요 없습니다.** 이미 검증된
컨트랙트는 건너뛰므로 재실행해도 안전합니다.

### Step 6 — 배포 기록 커밋

```bat
cd ..
git add contracts/deployments/giwaSepolia.json
git commit -m "chore: record GIWA Sepolia deployment"
git push -u origin claude/work-history-pjm57c
```

> Windows·macOS·Linux 모두 같은 명령입니다.

주소가 저장소에 남아야 심사자가 코드와 배포본을 대조할 수 있습니다.

---

## 컨트랙트 상세

### `SUPToken`

```
name        StepUp
symbol      SUP
decimals    18
supply      1,000,000,000 (고정)
확장         ERC20Burnable, ERC20Permit
```

생성자에서 전량을 `treasury`로 발행하고 끝입니다. `mint` 함수가 아예 없으므로
공급량은 소각으로만 줄어듭니다. 테스트가 ABI에 `mint`가 없음을 확인합니다.

### `SneakerNFT`

앱과 동일한 게임 수식이 순수 함수로 들어 있습니다.

```solidity
boostBps(rarity, variant, level) = rarityBps + variant*30 + (level-1)*50
upgradeCost(rarity, level)       = level * 100 SUP * (4 + rarity) / 4
energyCells(level)               = 10 + (level-1) * 2
```

| 등급 | 변형 | 최대 레벨 | 최대 부스트 | 만렙 강화 총비용 |
|---|---|---|---|---|
| Common | 3 | 10 | +5.1% | 4,500 SUP |
| Rare | 3 | 15 | +8.6% | 13,125 SUP |
| Epic | 3 | 20 | +12.1% | 28,500 SUP |
| Legendary | 2 | 30 | **+17.8%** | 76,125 SUP |

**민팅에 서명이 필요한 이유** — 등급은 행운 스탯이 보정하는 가중 추첨입니다.
유저가 커밋 전에 볼 수 있는 온체인 난수는 난수가 아니므로, 추첨은 오프체인에서
하고 `roller`의 EIP-712 서명으로 전달됩니다. 트랜잭션은 여전히 유저가 보내고
`msg.sender`에게서 500 SUP를 소각합니다. **서명은 "무엇이" 민팅되는지를 정할
뿐, "돈을 냈는지"는 정하지 않습니다.**

**강화에는 서명이 없습니다** — 비용과 효과가 결정적이라 소유자가 직접 호출하고,
컨트랙트가 유일한 권한입니다.

### `RewardDistributor`

```solidity
dailyBudget(day) = 250,000 SUP >> (day / 730)     // 730일마다 반감
claim(Claim, signature)                            // EIP-712, 세션 해시로 재사용 방지
```

- 급수 극한 **365,000,000 SUP** (테스트로 검증)
- 세션 해시당 1회만 지급
- `day`는 현재일 이하 + 7일 이내여야 함
- **누구나 제출 가능** — SUP는 항상 `c.runner`로 갑니다. 가스가 없는 신규
  유저를 위해 릴레이어가 대납할 수 있습니다
- 풀이 비면 예산이 남아 있어도 지급되지 않음
- 소유자 권한: 어테스터 교체, 일시정지. **그게 전부입니다**

### `CourseRegistry`

```solidity
rewardFor(distanceM)        = min(distanceM * 1 SUP / 1000, 42 SUP)
requiredDistanceM(distanceM) = distanceM * 98%
```

코스 작성은 **누구나** 가능합니다(작성자 기록이 핵심). 완주 기록은 `recorder`
(어테스터)만 씁니다 — 보상을 막는 GPS 검사와 같은 검사가 카운터도 막아야 하기
때문입니다.

폴리라인 자체는 온체인에 올리지 않고 `polylineHash`만 저장합니다. 코스 하나에
좌표 수백 개를 칼데이터로 올릴 이유가 없고, 앱이 서빙하는 트랙이 등록된 것과
같은지 확인하는 데는 해시로 충분합니다.

---

## 테스트

```bash
npm test
```

43개 테스트가 다음을 확인합니다.

- **경제 상수가 앱과 일치** — 부스트 표, 강화 비용 표, 에너지 상한 표
- **부스트 천장 1780 bps** — 전 등급·전 변형·만렙을 순회해 확인
- **서명 위조·변조·재사용 차단** — 잘못된 서명자, 등급 변조, 논스 재사용, 만료
- **어테스터가 공급을 늘릴 수 없음** — 유효한 서명이어도 일일 예산 초과 시 실패
- **리워드 풀 탈출구 없음** — ABI에 withdraw/sweep/rescue 계열 함수가 없음
- **코스 보상이 정량** — 5 km → 5.0 SUP, 100 km → 42 SUP(상한)

---

## 앱 연동 (다음 단계)

이 컨트랙트들은 앱이 **이미 만들고 있는 데이터**를 소비하도록 설계했습니다.

```
WalkSessionService  →  SessionReward(rewardedSteps, points, energyUsed)
                       + GPS 폴리라인
                            │
                            ▼
                    어테스터 서비스 (미구현)
                       케이던스·GPS 타당성·기기 증명 검사
                       → EIP-712 서명
                            │
                            ▼
                    RewardDistributor.claim()
```

빠진 것은 **어테스터 서비스와 앱의 지갑 연결**이지, 세션 레코드를 만드는
클라이언트가 아닙니다.

---

## 보안 관련 고지

- **감사받지 않았습니다.** 외부 감사 없이 메인넷에 올리지 않습니다.
- **어테스터가 중앙화되어 있습니다.** 단일 서명 키는 신뢰 가정입니다. 임계
  서명으로의 전환은 메인넷 이후 마일스톤입니다
  ([TOKENOMICS §9](../docs/TOKENOMICS.md)).
- **`deployments/*.json`에는 주소만 들어갑니다.** 키는 들어가지 않습니다.
