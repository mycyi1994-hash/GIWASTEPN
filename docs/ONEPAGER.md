<div align="center">

# StepUp — One-Pager

### Every Step. Every Stride. Every Day.

**A Move-to-Earn consumer app for the [GIWA](https://giwa.io) chain.**
Walk or run in the real world → earn SUP → collect and upgrade sneaker NFTs.

`v1.12.1` · Android · Kotlin + Jetpack Compose · **Working APK, installable today**

[English](#1-what-it-is) · [한국어 ↓](#한국어)

**[⬇️ Install the APK](https://github.com/mycyi1994-hash/GIWASTEPN/raw/apk-dist/StepUp-debug.apk)** · [Repository](https://github.com/mycyi1994-hash/GIWASTEPN) · [Tokenomics](TOKENOMICS.md) · [Pitch Deck](PITCH.md)

</div>

---

## 1. What it is

StepUp is a **complete consumer running app** — GPS course tracking, lap splits,
a crew community, a 44-piece NFT collection, 100 achievements — with a
Move-to-Earn economy wired underneath it, targeting GIWA as the settlement layer.

We built the app first and the token second. That order is the thesis.

## 2. The problem

| | |
|---|---|
| **Fitness apps churn.** | A step counter has no reason to survive a broken streak. |
| **M2E apps collapse.** | When emissions fall, DAU falls with them — the token *was* the product. |
| **Chains lose the funnel.** | Seed phrases and gas kill Web2 users before their first run. |

## 3. What we do about it

1. **The app is worth opening without the token.** Real GPS course maps, course
   creation and sharing, party runs, flash-run meetups, a collection worth
   completing. Delete the token and it is still a running app people use.
2. **Emissions are capped by construction, not by discretion.** The strongest
   possible sneaker earns **+17.8%** per step over a free one — not 3×, not 10×.
   Capital cannot outrun legs. ([Tokenomics §2.2](TOKENOMICS.md))
3. **The chain is deferred, not dumped on the user.** SUP is earned and spent
   locally from second one. A wallet is what you connect to *withdraw*, not what
   you fight to *start*.

## 4. Product structure

**5 tabs · 60+ screens · 4 languages · shipped**

| Tab | What's in it |
|---|---|
| **Home** | Single no-scroll screen — steps, energy ring with midnight refill, weekly distance, equipped sneaker, one-tap **START RUN** |
| **Run** | Foreground session. Course card with live GPS track on a neon map, goal ring, 9 live metrics (pace, lap split, HR, cadence, kcal, steps, speed, elevation, ETA), **laps** (auto at 1 km + manual) |
| **Courses** | 코스 선택 · 코스 만들기 · 코스 게시판 — record a run, save it as a reusable course, publish it for other runners. Distance-scaled completion rewards. |
| **Community** | Boards (flash-run / free / tips) with proximity sort and search · comments and threaded replies with notifications · **crews** with crew-only boards and **party runs** (ready-check → 3·2·1 → synchronized measurement → +10%/member) · ranking |
| **Items** | 44 sneaker NFTs (4 factions × 11 rarity variants), minting with luck-weighted rolls, upgrades, boost shop |
| **Events** | Seasonal campaign, weekly challenges wired to real step data, referrals, claims that credit real SUP |
| **Profile** | Runner level from **cumulative kilometres** (60 levels, ≈2,914.6 km to max), lifetime stats, 100 achievements across 13 categories, wallet, notifications, analytics, settings |

**Runner level is the user's XP, not the NFT's.** Sneakers are bought, sold and
swapped; the distance a person has covered is theirs and never resets.

## 5. Technical execution

| Layer | Implementation |
|---|---|
| **Client** | Kotlin 2.0.21, Jetpack Compose + Material 3, Navigation Compose, minSdk 26 / targetSdk 35 |
| **Architecture** | MVVM + Repository, manual DI (`ServiceLocator`), `StateFlow` end to end |
| **Measurement** | `TYPE_STEP_COUNTER` with midnight/reboot baseline correction + `LocationManager` GPS, in a foreground service (`health\|location`) that survives screen-off |
| **Geo** | Haversine distance, track simplification, `cos(latitude)`-corrected normalization, rendered as a neon course map on Canvas |
| **Persistence** | Room 2.6.1 (KSP) — daily records, sessions, reward ledger, community, courses · DataStore for settings |
| **Economy** | `domain/RewardEconomy.kt`, pure functions, **unit-tested** in CI |
| **i18n** | 648 strings × ko / en / zh / ja, `localeConfig` in-app language override |
| **CI/CD** | GitHub Actions — unit tests → `assembleDebug` → **APK signature fingerprint verification** → publish to `apk-dist` on every push |

**Scale:** 76 Kotlin source files · 648 localized strings · 44 NFT designs ·
100 achievements · 60 runner levels · Room schema v6.

**Verifiable now:** clone the repo, run `./gradlew :app:testDebugUnitTest`, or
install the APK from the link above on any Android 8.0+ phone.

## 6. Token economy in five lines

| | |
|---|---|
| **Accrual** | 0.01 SUP/step, **only during an active run session** |
| **Gate** | Energy: 1 cell = 600 steps, refills at midnight; at 0 energy accrual stops entirely |
| **Ceiling** | Sneaker ×1.000–**×1.178** · party ×1.0–×1.50 · booster ×2.0 (bought for 200 SUP) |
| **Deterministic reward** | Course completion = `km × 1.0 SUP` capped at 42, paid at ≥98% GPS-verified |
| **Sinks** | Mint 500 · upgrade to max 4,500–76,125 by rarity · boosts 50–200 |

Full model, balance tables per persona, supply schedule and open questions:
**[TOKENOMICS.md](TOKENOMICS.md)**

## 7. GIWA integration — precisely where we are

| Layer | Today | Next |
|---|---|---|
| SUP balance | Local ledger (Room), every accrual and spend is a row | ERC-20 `SUPToken` on GIWA |
| Sneaker NFT | 44 designs, mint/upgrade/equip fully working | ERC-721 `SneakerNFT`, metadata on IPFS |
| Settlement | On-device `RewardEconomy` | `RewardDistributor` — signed run proof, replay-protected claim |
| Courses | Room, shared in-app | `CourseRegistry` — publicly verifiable authorship |
| Wallet | Ledger + withdrawal UX in place | GIWA wallet connect + withdraw |

**All four contracts are live on GIWA Sepolia (chain 91342) as of 2026-07-31**,
with 50M SUP funded into the reward pool —
[`contracts/deployments/giwaSepolia.json`](../contracts/deployments/giwaSepolia.json).
They carry **43 passing tests** asserting the on-chain constants match the
client's, including that the boost ceiling really is 1780 bps and that the
reward pool has no owner withdrawal path.

**What is not done, stated plainly:** the app still settles locally. It does not
yet connect a wallet or claim on chain, and the attester service is written and
tested but not deployed. That wiring is the remaining work — not the app, not
the contracts.

## 8. Roadmap

| Milestone | Scope |
|---|---|
| **M1 — done** | Full client: run tracking, courses, NFTs, community, i18n, CI, installable APK |
| **M2 — in progress** | Contracts written + 43 tests passing. Next: deploy and source-verify on **GIWA Sepolia** (chain 91342) |
| **M3** | Wallet connect + on-chain claim in the app; attester service with cadence/GPS plausibility checks |
| **M4** | Backend for community, ranking and course sharing (currently local + seeded) |
| **M5** | NFT marketplace (trade / rent), Health Connect, decentralised attestation |

## 9. Why GIWA

M2E settlement is a **high-frequency, low-value** workload: many small claims,
every day, from consumer phones. It needs cheap and predictable execution more
than it needs anything else, and it needs a chain where a consumer app is a
first-class citizen rather than an afterthought to DeFi. That is the fit we are
building for.

## 10. What we're asking for

Support to complete **M2 and M3** — putting the reward loop that already works
on-device onto GIWA, with contracts published and verified, and the claim flow
live in the app.

---

<div align="center">

**StepUp** · [github.com/mycyi1994-hash/GIWASTEPN](https://github.com/mycyi1994-hash/GIWASTEPN)

</div>

---
---

<a name="한국어"></a>

# StepUp — 원페이저 · 한국어

### Every Step. Every Stride. Every Day.

**[GIWA](https://giwa.io) 체인 기반 Move-to-Earn 컨슈머 앱.**
실제로 걷고 달리면 → SUP를 적립하고 → 스니커즈 NFT를 모으고 강화합니다.

`v1.12.1` · 안드로이드 · Kotlin + Jetpack Compose · **지금 설치 가능한 APK**

**[⬇️ APK 설치](https://github.com/mycyi1994-hash/GIWASTEPN/raw/apk-dist/StepUp-debug.apk)** · [저장소](https://github.com/mycyi1994-hash/GIWASTEPN) · [토크노믹스](TOKENOMICS.md) · [피치덱](PITCH.md)

---

## 1. 무엇인가

StepUp은 **완결된 컨슈머 러닝 앱**입니다 — GPS 코스 기록, 랩 스플릿, 크루
커뮤니티, 44종 NFT 도감, 업적 100종. 그 아래에 Move-to-Earn 이코노미를 깔았고,
정산 레이어로 GIWA를 목표로 합니다.

앱을 먼저 만들고 토큰을 나중에 붙였습니다. 그 순서가 곧 우리의 주장입니다.

## 2. 문제

| | |
|---|---|
| **피트니스 앱은 이탈합니다.** | 만보기는 연속 기록이 끊기면 살아남을 이유가 없습니다. |
| **M2E 앱은 무너집니다.** | 배출이 줄면 DAU도 같이 줍니다 — 토큰이 곧 제품이었으니까요. |
| **체인은 퍼널을 잃습니다.** | 시드 구문과 가스비가 첫 러닝 전에 웹2 유저를 쫓아냅니다. |

## 3. 우리의 답

1. **토큰이 없어도 열 만한 앱입니다.** 실제 GPS 코스맵, 코스 제작·공유, 파티런,
   번개러닝, 완성할 가치가 있는 도감. 토큰을 지워도 사람들이 쓰는 러닝 앱으로
   남습니다.
2. **배출 상한이 재량이 아니라 구조입니다.** 최강 신발이 무료 신발보다 걸음당
   **+17.8%** 법니다 — 3배도, 10배도 아닙니다. 자본이 다리를 이길 수 없습니다.
   ([토크노믹스 §2.2](TOKENOMICS.md))
3. **체인을 유저에게 떠넘기지 않습니다.** SUP는 1초차부터 로컬에서 벌리고
   쓰입니다. 지갑은 **출금할 때** 연결하는 것이지, **시작할 때** 싸워야 하는
   관문이 아닙니다.

## 4. 제품 구조

**5탭 · 60개 이상 화면 · 4개 언어 · 구현 완료**

| 탭 | 내용 |
|---|---|
| **홈** | 스크롤 없는 단일 화면 — 걸음, 자정 리필 에너지 링, 주간 거리, 착용 스니커즈, 원탭 **START RUN** |
| **러닝** | 포그라운드 세션. 네온 지도 위 실시간 GPS 경로가 얹힌 코스 카드, 목표 링, 실시간 지표 9종(페이스·랩 스플릿·심박·케이던스·칼로리·걸음·속도·상승·예상완료), **랩**(1km 자동 + 수동) |
| **코스** | 코스 선택 · 코스 만들기 · 코스 게시판 — 달린 경로를 재사용 코스로 저장하고 다른 러너에게 공개. 거리별 정량 완주 보상 |
| **커뮤니티** | 게시판(번개러닝/자유/꿀팁) 거리순 정렬·검색 · 댓글과 대댓글 + 알림 · **크루** 전용 게시판과 **파티런**(레디체크 → 3·2·1 → 동시 측정 → 인원당 +10%) · 랭킹 |
| **아이템** | 스니커즈 NFT 44종(4속성 × 11등급 변형), 행운 가중 민팅, 강화, 부스트 상점 |
| **이벤트** | 시즌 캠페인, 실제 걸음 데이터와 연동된 주간 챌린지, 친구 초대, 실제 SUP가 적립되는 보상 수령 |
| **프로필** | **누적 킬로미터** 기반 러너 레벨(60단계, 만렙까지 약 2,914.6 km), 누적 통계, 13개 카테고리 업적 100종, 지갑, 알림함, 통계, 설정 |

**러너 레벨은 NFT가 아니라 유저의 경험치입니다.** 신발은 사고팔고 갈아 신지만,
그 사람이 걸어온 거리는 그 사람의 것이고 되돌아가지 않습니다.

## 5. 기술 실행

| 레이어 | 구현 |
|---|---|
| **클라이언트** | Kotlin 2.0.21, Jetpack Compose + Material 3, Navigation Compose, minSdk 26 / targetSdk 35 |
| **아키텍처** | MVVM + Repository, 수동 DI(`ServiceLocator`), 전 구간 `StateFlow` |
| **측정** | 자정·재부팅 기준점 보정이 들어간 `TYPE_STEP_COUNTER` + `LocationManager` GPS를, 화면을 꺼도 살아남는 포그라운드 서비스(`health\|location`)에서 실행 |
| **지오** | 하버사인 거리, 경로 솎기, `cos(위도)` 보정 정규화, Canvas 네온 코스맵 렌더링 |
| **저장소** | Room 2.6.1 (KSP) — 일별 기록·세션·리워드 원장·커뮤니티·코스 · 설정은 DataStore |
| **이코노미** | `domain/RewardEconomy.kt`, 순수 함수, CI에서 **단위 테스트** |
| **다국어** | 문자열 648개 × ko / en / zh / ja, `localeConfig` 앱 내 언어 설정 |
| **CI/CD** | GitHub Actions — 단위 테스트 → `assembleDebug` → **APK 서명 지문 검증** → 푸시마다 `apk-dist` 배포 |

**규모:** Kotlin 76개 파일 · 현지화 문자열 648개 · NFT 44종 · 업적 100종 ·
러너 레벨 60단계 · Room 스키마 v6.

**지금 검증 가능:** 저장소를 클론해 `./gradlew :app:testDebugUnitTest`를
돌리거나, 위 링크의 APK를 Android 8.0 이상 기기에 설치해 보세요.

## 6. 다섯 줄로 보는 토큰 이코노미

| | |
|---|---|
| **적립** | 1보당 0.01 SUP, **러닝 세션 중에만** |
| **게이트** | 에너지 1칸 = 600보, 자정 리필. 0이 되면 적립 완전 중단 |
| **상한** | 신발 ×1.000~**×1.178** · 파티 ×1.0~×1.50 · 부스터 ×2.0(200 SUP로 구매) |
| **결정적 보상** | 코스 완주 = `km × 1.0 SUP`, 최대 42, GPS 기준 98% 이상 주파 시 지급 |
| **소각처** | 민팅 500 · 등급별 만렙 강화 4,500~76,125 · 부스트 50~200 |

전체 모델, 페르소나별 밸런스 표, 공급 스케줄, 미결 과제:
**[TOKENOMICS.md](TOKENOMICS.md)**

## 7. GIWA 연동 — 정확히 어디까지 왔나

| 레이어 | 현재 | 다음 |
|---|---|---|
| SUP 잔액 | 로컬 원장(Room), 모든 적립·사용이 한 줄 | GIWA의 ERC-20 `SUPToken` |
| 스니커즈 NFT | 44종, 민팅·강화·착용 완전 동작 | ERC-721 `SneakerNFT`, 메타데이터 IPFS |
| 정산 | 기기 내 `RewardEconomy` | `RewardDistributor` — 서명된 러닝 증명, 재사용 방지 청구 |
| 코스 | Room 저장, 앱 내 공유 | `CourseRegistry` — 작성자 공개 검증 |
| 지갑 | 원장 + 출금 UX 구현됨 | GIWA 지갑 연결 + 출금 |

**2026-07-31 기준 컨트랙트 4종 모두 GIWA Sepolia(체인 91342)에 배포돼 있고**,
리워드 풀에 5천만 SUP가 들어가 있습니다 —
[`contracts/deployments/giwaSepolia.json`](../contracts/deployments/giwaSepolia.json).
온체인 상수가 앱과 일치하는지 확인하는 **테스트 43개가 통과**합니다 — 부스트
천장이 실제로 1780 bps인지, 리워드 풀에 소유자 인출 경로가 없는지까지 포함합니다.

**안 된 것도 그대로 적습니다.** 앱은 아직 로컬에서 정산합니다. 지갑을 연결하지도,
체인에 청구하지도 않습니다. 어테스터 서비스도 작성·테스트는 끝났지만 배포 전입니다.
남은 것은 그 연결이지, 앱도 컨트랙트도 아닙니다.

## 8. 로드맵

| 마일스톤 | 범위 |
|---|---|
| **M1 — 완료** | 전체 클라이언트: 러닝 측정, 코스, NFT, 커뮤니티, 다국어, CI, 설치 가능한 APK |
| **M2 — 진행 중** | 컨트랙트 작성 + 테스트 43개 통과. 다음: **GIWA Sepolia**(체인 91342)에 배포·소스 검증 |
| **M3** | 앱 내 지갑 연결 + 온체인 청구. 케이던스·GPS 타당성 검사를 갖춘 어테스터 서비스 |
| **M4** | 커뮤니티·랭킹·코스 공유 백엔드 (현재는 로컬 + 시드 데이터) |
| **M5** | NFT 마켓(거래/임대), Health Connect, 어테스테이션 탈중앙화 |

## 9. 왜 GIWA인가

M2E 정산은 **고빈도·소액** 워크로드입니다. 매일, 소비자 폰에서, 작은 청구가
아주 많이 발생합니다. 무엇보다 싸고 예측 가능한 실행이 필요하고, 컨슈머 앱이
DeFi의 곁다리가 아니라 1급 시민인 체인이 필요합니다. 우리가 겨냥하는 적합성이
바로 그것입니다.

## 10. 무엇을 요청하는가

**M2와 M3의 완성**에 대한 지원입니다 — 이미 기기에서 동작하는 리워드 루프를
GIWA 위에 올리고, 컨트랙트를 공개·검증하고, 청구 흐름을 앱에서 실제로 돌리는
것까지입니다.

---

<div align="center">

**StepUp** · [github.com/mycyi1994-hash/GIWASTEPN](https://github.com/mycyi1994-hash/GIWASTEPN)

</div>
