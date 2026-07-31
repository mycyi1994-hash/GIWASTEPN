<div align="center">

# StepUp — Pitch Deck

**Move-to-Earn on GIWA · 15 slides**
`v1.12.1` · Working Android app, installable today

[English](#slide-1--title) · [한국어 ↓](#한국어)

**[⬇️ Install the APK](https://github.com/mycyi1994-hash/GIWASTEPN/raw/apk-dist/StepUp-debug.apk)** · [Repository](https://github.com/mycyi1994-hash/GIWASTEPN) · [One-Pager](ONEPAGER.md) · [Tokenomics](TOKENOMICS.md)

</div>

---

## Slide 1 — Title

# StepUp
### Every Step. Every Stride. Every Day.

**Move-to-Earn, built app-first, for the GIWA chain.**

*Visual: the Volt splash — black canvas, neon-lime wordmark, hero sneaker.*
*Footer: v1.12.1 · Android · Seoul, Korea*

---

## Slide 2 — The problem

**Three failures, stacked on top of each other.**

| | |
|---|---|
| **Fitness apps churn.** | A step counter has no reason to survive a broken streak. |
| **M2E apps collapse.** | When emissions fall, DAU falls with them — because the token *was* the product. |
| **Chains lose the funnel.** | Seed phrases and gas kill Web2 users before their first run. |

> Every M2E cycle has ended the same way: the token arrived first, the product never did.

*Visual: three descending bars — DAU vs. token price, converging to zero.*

---

## Slide 3 — Why the last cycle failed (the honest slide)

**It wasn't the idea. It was the multiplier ladder.**

- Earning scaled 5×–20× with NFT spend → the game became a yield calculation
- Yield attracted capital, not runners
- Capital left when yield fell — and it took the DAU with it, because there was
  nothing else to open the app for

**The failure mode is specific and it is designable-around.**
Cap the multiplier and the token stops being an investment instrument.
Build a product worth opening and the DAU stops being rented.

*Visual: the STEPN GST chart, 2022 — annotated with "emission cut" and "DAU follows".*

---

## Slide 4 — Our thesis

> ## App first. Token second.
> ### And the multiplier ceiling is **+17.8%**, not 10×.

Three commitments that fall out of that:

1. **Delete the token and it is still a running app people use.**
2. **Capital cannot outrun legs.** Best possible sneaker earns +17.8% per step
   over a free one.
3. **No wallet in the new-user funnel.** SUP is earned and spent locally from
   second one; the wallet is for withdrawing, not for starting.

*Visual: two columns — "What most M2E shipped" vs "What we shipped".*

---

## Slide 5 — What's built (not a mockup)

**5 tabs · 60+ screens · 4 languages · installable APK today**

| | |
|---|---|
| 76 | Kotlin source files |
| 648 | localized strings × ko / en / zh / ja |
| 44 | sneaker NFT designs |
| 100 | achievements across 13 categories |
| 60 | runner levels (≈2,914.6 km to max) |

**Verify it yourself:** install the APK, or run `./gradlew :app:testDebugUnitTest`.

*Visual: 6 real device screenshots — home, live run, community, items, events, profile.*

---

## Slide 6 — The run screen

**Real GPS, real course, real laps.**

- Foreground service (`health|location`) — survives screen-off and app switching
- Haversine distance, track simplification, `cos(latitude)`-corrected normalization
- Live GPS trace drawn over the selected course on a neon map
- 9 live metrics: pace, lap split, HR, cadence, kcal, steps, speed, elevation, ETA
- Laps: automatic at every kilometre + manual button

*Visual: the live-run screenshot, callouts on the GPS badge, goal ring, metric grid.*

---

## Slide 7 — Courses: the differentiator

**코스 선택 · 코스 만들기 · 코스 게시판**

- Record a run → save the GPS track as a reusable course
- Publish it → other runners take it, like it, run it
- **Completion reward = `km × 1.0 SUP`, capped at 42, paid at ≥98% GPS-verified**

**Fully deterministic. No random roll, no loot box.** The same 5 km course pays
5.0 SUP to everyone who finishes it, always.

> A reward a user can compute in advance is a reward they can trust.

*Visual: course board grid + the neon course map with start dot and finish flag.*

---

## Slide 8 — Retention is social, not monetary

**Crews · party runs · flash-runs · comments**

- **Party run:** ready-check → 3·2·1 countdown → synchronized measurement →
  **+10% per crew member (max +50%)**
- **Flash-run (번개러닝):** meeting point opens in Google Maps, live capacity,
  proximity-sorted board
- Threaded replies with notifications; crew-only boards; weekly + lifetime ranking

The token rewards the run. **The crew is why you show up.**

*Visual: community screenshot + party-run countdown state.*

---

## Slide 9 — The collection

**4 factions × 11 rarity variants = 44 designs**

| Rarity | Mint odds | Max boost | Max level |
|---|---:|---:|---:|
| Common | 55% | +5.1% | 10 |
| Rare | 28% | +8.6% | 15 |
| Epic | 13% | +12.1% | 20 |
| Legendary | **4%** | **+17.8%** | 30 |

Sneaker **level** raises the daily cap on rewardable steps (6,000 → 40,800), not
the per-step rate. Raising the cap is worthless unless you can physically walk
into it.

*Visual: the 44-piece collection grid, Legendary highlighted.*

---

## Slide 10 — Emission

**Every SUP is paid for with measured physical effort.**

| | |
|---|---|
| Accrual | **0.01 SUP/step**, only during an active run session |
| Gate | Energy: 1 cell = 600 steps, midnight refill; at 0, accrual stops entirely |
| Multipliers | sneaker ×1.000–**×1.178** · party ×1.0–×1.50 · booster ×2.0 (bought for 200 SUP) |
| Bonuses | daily goal 20–34 SUP · course completion `km × 1.0`, cap 42 |

Background steps don't pay. Walking to the fridge doesn't pay. Each session
produces exactly one auditable `(steps, distance, boost, payout)` record.

*Visual: the emission formula, large, on black.*

---

## Slide 11 — Sinks, and the balance we actually get

**One year, three personas — gross in vs. what they spend into**

| | Casual | Committed | Elite |
|---|---:|---:|---:|
| Session steps/day | 4,000 | 10,000 | 18,000 |
| **Total in** | 22,300 | 52,400 | 89,400 |
| **Total out** | 5,000 | 30,750 | 103,625 |
| **Net** | +17,300 | +21,650 | **−14,225** |

**Net-emissive at the bottom, net-deflationary at the top — by design.** New
users must accumulate something for the loop to be worth starting; the players
with the most SUP must have somewhere expensive to put it.

**We don't claim sinks alone close the loop.** They compress it. The on-chain
emission budget closes it.

*Visual: the three-persona bar chart, Elite bar crossing below zero.*

---

## Slide 12 — On-chain design for GIWA

**Two layers, one token.**

- **In-app SUP** — off-chain ledger, earned per step, burned on mint/upgrade/boost.
  **Live today.** Most SUP never touches the chain.
- **On-chain SUP** — ERC-20, claimed through the distributor, bounded by a
  **global daily budget that halves every 730 days**

```
rate(d) = min(1, B(d) / Σ claimScore(d))
```

Supply cannot hyperinflate no matter how many users join — the budget is fixed
before the users are counted.

| Contract | Role |
|---|---|
| `SUPToken` | ERC-20, 1B hard cap, no mint after deploy |
| `SneakerNFT` | ERC-721, stats on-chain, art on IPFS |
| `RewardDistributor` | Signed run proof, replay-protected claim, applies `rate(d)` |
| `CourseRegistry` | Publicly verifiable course authorship |

*Visual: the claim-flow sequence diagram (client → attester → GIWA).*

---

## Slide 13 — Status: precisely where we are

| | Today | Next |
|---|---|---|
| Client | **Shipped, installable** | — |
| Contracts | **Live on GIWA Sepolia, 43 tests passing** | Mainnet + external audit |
| SUP | **ERC-20 deployed**, 1B fixed, 50M in the reward pool | App claims against it |
| NFT | 44 designs working in-app; `SneakerNFT` deployed | Mint on chain, metadata on IPFS |
| Settlement | On-device `RewardEconomy` | `RewardDistributor` (deployed, not yet called) |
| Wallet | Ledger + withdrawal UX in place | Connect + claim |

> **The contracts are on chain; the app is not connected to them yet, and we
> are not claiming otherwise.** All four deployed to GIWA Sepolia on 2026-07-31.
> The client already emits exactly the per-session record they consume. What is
> missing is the wallet-connect and claim path in the app, and deploying the
> attester service that signs those claims.

*Visual: two-column status table, "Today" column mostly green.*

---

## Slide 14 — Team

**Traditional-finance veterans moving into crypto. Five people, Seoul.**

| Role | Background | Owns |
|---|---|---|
| **CEO** | Proprietary trader 2008–2018, securities & investment · Head of Asset Management | Reward-economy parameters |
| **CIO** | Proprietary trader 2006–2018, asset management · Macro senior trader · Head of CTA Solutions | Emission schedule, treasury |
| **Systems** | Electronics engineering · System trader 2010–2018 · Investment advisory 2018–2022 | Run proof, attester, anti-abuse |
| **Community** | Crypto KOL — Korean market audience & campaigns | Crews, growth, creator channels |
| **Markets** | Active crypto trading desk | On-chain liquidity |

> Names, affiliations and full CVs are supplied separately with the application
> (question 3), not published in this repository.

**Why it matters:** most M2E economies were designed by product people, not by
people who price risk. Ours was designed by traders. That is why the ceiling is
+17.8% and why the tokenomics has a section called *"open questions we are not
pretending to have solved."*

*Visual: five portrait cards on black, Volt accent rule under each name.*

---

## Slide 15 — The ask

> ## Support to complete M2 and M3

**M2** — `SUPToken`, `SneakerNFT`, `RewardDistributor`, `CourseRegistry`
deployed and source-verified on **GIWA testnet**

**M3** — Wallet connect + on-chain claim live in the app, with the attester
service running cadence and GPS plausibility checks

Everything else already works on-device. We are asking for support to put a
reward loop that already runs onto GIWA.

**[github.com/mycyi1994-hash/GIWASTEPN](https://github.com/mycyi1994-hash/GIWASTEPN)**

*Visual: the Volt START RUN CTA, full-bleed.*

---
---

<a name="한국어"></a>

# StepUp — 피치덱 · 한국어

**GIWA 기반 Move-to-Earn · 15장**
`v1.12.1` · 지금 설치 가능한 안드로이드 앱

---

## 1장 — 표지

# StepUp
### Every Step. Every Stride. Every Day.

**앱을 먼저 만든 Move-to-Earn, GIWA 체인 위에서.**

*비주얼: Volt 스플래시 — 검정 캔버스, 네온 라임 워드마크, 히어로 스니커즈.*
*하단: v1.12.1 · 안드로이드 · 대한민국 서울*

---

## 2장 — 문제

**세 개의 실패가 겹쳐 있습니다.**

| | |
|---|---|
| **피트니스 앱은 이탈합니다.** | 만보기는 연속 기록이 끊기면 살아남을 이유가 없습니다. |
| **M2E 앱은 무너집니다.** | 배출이 줄면 DAU도 같이 줍니다 — 토큰이 곧 제품이었으니까요. |
| **체인은 퍼널을 잃습니다.** | 시드 구문과 가스비가 첫 러닝 전에 웹2 유저를 쫓아냅니다. |

> 모든 M2E 사이클은 같은 방식으로 끝났습니다. 토큰이 먼저 왔고, 제품은 끝내 오지 않았습니다.

*비주얼: 하강하는 막대 3개 — DAU와 토큰 가격이 함께 0으로 수렴.*

---

## 3장 — 지난 사이클은 왜 실패했나 (정직한 장표)

**아이디어의 문제가 아니었습니다. 배율 사다리의 문제였습니다.**

- NFT 지출에 따라 적립이 5~20배로 확장 → 게임이 수익률 계산으로 변질
- 수익률은 러너가 아니라 자본을 끌어왔습니다
- 수익률이 꺾이자 자본이 떠났고, DAU도 함께 떠났습니다. 앱을 열 다른 이유가
  없었으니까요

**실패 양상이 구체적이므로, 설계로 우회할 수 있습니다.**
배율에 상한을 걸면 토큰이 투자 상품이기를 멈춥니다.
열 만한 제품을 만들면 DAU를 빌려오지 않아도 됩니다.

*비주얼: 2022년 STEPN GST 차트 — "배출 축소", "DAU 동반 하락" 주석.*

---

## 4장 — 우리의 주장

> ## 앱이 먼저. 토큰은 그다음.
> ### 그리고 배율 상한은 10배가 아니라 **+17.8%**입니다.

여기서 따라 나오는 세 가지 약속:

1. **토큰을 지워도 사람들이 쓰는 러닝 앱으로 남습니다.**
2. **자본이 다리를 이길 수 없습니다.** 최강 신발이 무료 신발보다 걸음당
   +17.8% 법니다.
3. **신규 유저 퍼널에 지갑이 없습니다.** SUP는 1초차부터 로컬에서 벌리고
   쓰입니다. 지갑은 출금용이지 시작용이 아닙니다.

*비주얼: 2단 비교 — "대부분의 M2E가 낸 것" vs "우리가 낸 것".*

---

## 5장 — 만들어진 것 (목업 아님)

**5탭 · 60개 이상 화면 · 4개 언어 · 지금 설치 가능한 APK**

| | |
|---|---|
| 76 | Kotlin 소스 파일 |
| 648 | 현지화 문자열 × ko / en / zh / ja |
| 44 | 스니커즈 NFT 디자인 |
| 100 | 13개 카테고리 업적 |
| 60 | 러너 레벨 (만렙까지 약 2,914.6 km) |

**직접 검증하십시오:** APK를 설치하거나 `./gradlew :app:testDebugUnitTest`를 실행.

*비주얼: 실기기 스크린샷 6장 — 홈, 러닝, 커뮤니티, 아이템, 이벤트, 프로필.*

---

## 6장 — 러닝 화면

**실제 GPS, 실제 코스, 실제 랩.**

- 포그라운드 서비스(`health|location`) — 화면을 꺼도, 앱을 전환해도 유지
- 하버사인 거리, 경로 솎기, `cos(위도)` 보정 정규화
- 선택한 코스 위에 실시간 GPS 경로를 네온 지도로 중첩
- 실시간 지표 9종: 페이스, 랩 스플릿, 심박, 케이던스, 칼로리, 걸음, 속도, 상승, 예상완료
- 랩: 1km마다 자동 + 수동 버튼

*비주얼: 러닝 화면 스크린샷, GPS 배지·목표 링·지표 그리드에 콜아웃.*

---

## 7장 — 코스: 차별점

**코스 선택 · 코스 만들기 · 코스 게시판**

- 달린 경로를 기록 → 재사용 가능한 코스로 저장
- 게시 → 다른 러너가 가져가고, 좋아요를 누르고, 달립니다
- **완주 보상 = `km × 1.0 SUP`, 최대 42, GPS 기준 98% 이상 주파 시 지급**

**완전히 결정적입니다. 확률도, 랜덤 박스도 없습니다.** 같은 5km 코스는 완주한
모두에게 언제나 5.0 SUP를 줍니다.

> 유저가 미리 계산할 수 있는 보상만이 신뢰할 수 있는 보상입니다.

*비주얼: 코스 게시판 그리드 + 시작점·도착 깃발이 있는 네온 코스맵.*

---

## 8장 — 리텐션은 금전이 아니라 관계에서

**크루 · 파티런 · 번개러닝 · 댓글**

- **파티런:** 레디체크 → 3·2·1 카운트다운 → 동시 측정 → **크루원 1명당 +10%
  (최대 +50%)**
- **번개러닝:** 집결지를 누르면 구글 지도 연결, 실시간 정원, 거리순 정렬 게시판
- 알림이 붙는 대댓글, 크루 전용 게시판, 주간·누적 랭킹

토큰은 러닝에 보상합니다. **크루는 나가는 이유입니다.**

*비주얼: 커뮤니티 스크린샷 + 파티런 카운트다운 상태.*

---

## 9장 — 도감

**4속성 × 11등급 변형 = 44종**

| 등급 | 민팅 확률 | 최대 부스트 | 최대 레벨 |
|---|---:|---:|---:|
| Common | 55% | +5.1% | 10 |
| Rare | 28% | +8.6% | 15 |
| Epic | 13% | +12.1% | 20 |
| Legendary | **4%** | **+17.8%** | 30 |

신발 **레벨**은 걸음당 단가가 아니라 하루 인정 걸음 상한(6,000 → 40,800)을
올립니다. 몸이 거기까지 걷지 못하면 상한을 올려도 소용없습니다.

*비주얼: 44종 도감 그리드, 레전더리 강조.*

---

## 10장 — 발행

**모든 SUP는 실측된 신체 활동으로 대가를 치릅니다.**

| | |
|---|---|
| 적립 | **1보당 0.01 SUP**, 러닝 세션 중에만 |
| 게이트 | 에너지 1칸 = 600보, 자정 리필. 0이 되면 적립 완전 중단 |
| 배율 | 신발 ×1.000~**×1.178** · 파티 ×1.0~×1.50 · 부스터 ×2.0(200 SUP로 구매) |
| 보너스 | 일일 목표 20~34 SUP · 코스 완주 `km × 1.0`, 최대 42 |

백그라운드 걸음은 적립되지 않습니다. 냉장고까지 걸어간 것도 안 됩니다. 세션
하나가 정확히 하나의 감사 가능한 `(걸음, 거리, 부스트, 지급액)` 레코드를 만듭니다.

*비주얼: 검정 배경 위 큼직한 적립 공식.*

---

## 11장 — 소각처, 그리고 실제로 나오는 밸런스

**1년, 3가지 페르소나 — 총 유입 vs 실제 지출**

| | 캐주얼 | 열심 | 엘리트 |
|---|---:|---:|---:|
| 세션 걸음/일 | 4,000 | 10,000 | 18,000 |
| **총 유입** | 22,300 | 52,400 | 89,400 |
| **총 유출** | 5,000 | 30,750 | 103,625 |
| **순증감** | +17,300 | +21,650 | **−14,225** |

**아래쪽은 순발행, 위쪽은 순소각 — 의도한 설계입니다.** 신규 유저는 뭔가 쌓여야
루프를 시작할 이유가 생기고, SUP를 가장 많이 가진 유저에게는 비싼 쓸 곳이
있어야 합니다.

**소각처만으로 루프가 닫힌다고 주장하지 않습니다.** 소각처는 압축하고, 닫는 것은
온체인 발행 예산입니다.

*비주얼: 3 페르소나 막대 차트, 엘리트 막대가 0 아래로.*

---

## 12장 — GIWA를 위한 온체인 설계

**두 레이어, 하나의 토큰.**

- **인앱 SUP** — 오프체인 원장. 걸음으로 벌고 민팅·강화·부스트로 소각.
  **현재 동작 중.** 대부분의 SUP는 체인에 닿지 않습니다.
- **온체인 SUP** — ERC-20. 분배 컨트랙트로 청구하며, **730일마다 반감하는
  글로벌 일일 예산**으로 제한됩니다

```
rate(d) = min(1, B(d) / Σ claimScore(d))
```

유저가 아무리 늘어도 공급은 하이퍼인플레이션을 일으킬 수 없습니다 — 유저를 세기
전에 예산이 먼저 고정되기 때문입니다.

| 컨트랙트 | 역할 |
|---|---|
| `SUPToken` | ERC-20, 10억 하드캡, 배포 후 발행 불가 |
| `SneakerNFT` | ERC-721, 스탯 온체인, 아트 IPFS |
| `RewardDistributor` | 서명된 러닝 증명, 재사용 방지 청구, `rate(d)` 적용 |
| `CourseRegistry` | 코스 작성자 공개 검증 |

*비주얼: 청구 흐름 시퀀스 다이어그램 (클라이언트 → 어테스터 → GIWA).*

---

## 13장 — 현황: 정확히 어디까지 왔나

| | 현재 | 다음 |
|---|---|---|
| 클라이언트 | **완성, 설치 가능** | — |
| 컨트랙트 | **GIWA Sepolia 배포 완료, 테스트 43개 통과** | 메인넷 + 외부 감사 |
| SUP | **ERC-20 배포 완료**, 10억 고정, 리워드 풀 5천만 | 앱에서 청구 |
| NFT | 44종 앱 내 동작, `SneakerNFT` 배포 완료 | 온체인 민팅, 메타데이터 IPFS |
| 정산 | 기기 내 `RewardEconomy` | `RewardDistributor` (배포됨, 아직 호출 안 함) |
| 지갑 | 원장 + 출금 UX 구현됨 | 연결 + 청구 |

> **컨트랙트는 체인 위에 있고, 앱은 아직 거기 연결돼 있지 않습니다. 그렇지 않은
> 척하지 않습니다.** 2026-07-31에 4종 모두 GIWA Sepolia에 배포했습니다.
> 클라이언트는 그것들이 소비할 세션 레코드를 이미 그대로 생성합니다. 빠진 것은
> 앱의 지갑 연결·청구 경로와, 그 청구에 서명할 어테스터 서비스 배포입니다.

*비주얼: 2단 현황 표, "현재" 열이 대부분 초록.*

---

## 14장 — 팀

**전통금융 베테랑들의 크립토 진출. 5인, 서울.**

| 역할 | 경력 | 담당 |
|---|---|---|
| **CEO** | 증권·투자 자기자본 트레이더 2008~2018 · 자산운용 총괄 | 리워드 이코노미 파라미터 |
| **CIO** | 자산운용 자기자본 트레이더 2006~2018 · 매크로 시니어 트레이더 · CTA 솔루션 총괄 | 발행 스케줄, 트레저리 |
| **시스템** | 전자 엔지니어링 · 시스템 트레이더 2010~2018 · 투자자문업 2018~2022 | 러닝 증명, 어테스터, 어뷰징 방지 |
| **커뮤니티** | 크립토 KOL — 국내 시장 오디언스·캠페인 | 크루, 그로스, 크리에이터 채널 |
| **마켓** | 크립토 트레이딩 데스크 | 온체인 유동성 |

> 실명·소속·상세 이력은 이 저장소에 올리지 않고, 지원서 3번 문항에 **별도
> 파일로** 제출합니다.

**왜 중요한가:** 대부분의 M2E 이코노미는 리스크를 가격 매기는 사람이 아니라
제품 하는 사람이 설계했습니다. 우리 것은 트레이더가 설계했습니다. 그래서 상한이
+17.8%이고, 토크노믹스에 *"해결한 척하지 않는 미결 과제"* 라는 절이 있습니다.

*비주얼: 검정 배경 위 인물 카드 5개, 이름 아래 볼트 강조선.*

---

## 15장 — 요청

> ## M2와 M3 완성에 대한 지원

**M2** — `SUPToken`, `SneakerNFT`, `RewardDistributor`, `CourseRegistry`를
**GIWA 테스트넷**에 배포하고 소스 검증

**M3** — 앱 내 지갑 연결 + 온체인 청구를 실제로 가동. 케이던스·GPS 타당성 검사를
수행하는 어테스터 서비스 포함

나머지는 이미 기기에서 동작합니다. 이미 돌아가는 리워드 루프를 GIWA 위에 올리는
일에 대한 지원을 요청합니다.

**[github.com/mycyi1994-hash/GIWASTEPN](https://github.com/mycyi1994-hash/GIWASTEPN)**

*비주얼: Volt START RUN CTA, 화면 가득.*
