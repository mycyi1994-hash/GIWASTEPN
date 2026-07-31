<div align="center">

# StepUp — Token Economy (SUP)

**Move-to-Earn on the [GIWA](https://giwa.io) chain**
Version 1.0 · Aligned with app build v1.12.1

[English](#1-summary) · [한국어 ↓](#한국어)

[One-Pager](ONEPAGER.md) · [Pitch Deck](PITCH.md) · [Team](TEAM.md)

</div>

---

## 1. Summary

SUP is the reward and utility asset of StepUp. It is minted by **physical
distance actually covered** and destroyed by **collection progression**.

The design has one governing rule:

> **Every SUP that enters circulation must be paid for with real, measurable
> physical effort — and the amount an individual can extract per unit of effort
> is capped by construction, not by discretion.**

Two things follow from that rule, and they are what make this economy different
from most M2E launches:

1. **There is no multiplier arms race.** The strongest possible sneaker (a
   Legendary at max upgrade) earns **+17.8%** more per step than a brand-new
   free one. Not 3×, not 10×. Capital cannot outrun legs.
2. **Sinks are sized against sources, per persona, not in aggregate.** A
   committed runner's yearly accrual (~40,000 SUP) and the cost of the
   progression they actually want (~30,000 SUP) are the same order of magnitude
   — by construction. See §6.

Everything in §2–§4 is **implemented and running today** in the Android client
(`domain/RewardEconomy.kt`, unit-tested). Everything in §7–§9 is the **on-chain
design** targeted for GIWA, and is labelled as such.

---

## 2. Emission — how SUP is created

### 2.1 Base accrual

| Parameter | Value | Source |
|---|---|---|
| Reward per step | **0.01 SUP** | `POINTS_PER_STEP` |
| Counted steps | **Only during an active run session** | `WalkSessionService` |
| Steps per energy cell | **600** | `STEPS_PER_ENERGY` |
| Base energy capacity | **10 cells** (= 6,000 rewardable steps/day) | `BASE_MAX_ENERGY` |
| Energy refill | Full, at local midnight | `UserPrefs` |
| At 0 energy | **Accrual stops entirely** | `sessionReward()` |

Background steps do not pay. Walking to the fridge does not pay. The user must
open the app and start a session, which is what makes the reward auditable —
each session produces exactly one `(steps, distance, boost, payout)` record.

### 2.2 Multipliers

```
payout = rewardedSteps × 0.01 × sneakerMultiplier × partyMultiplier × boostMultiplier
```

| Multiplier | Range | Notes |
|---|---|---|
| **Sneaker** | ×1.000 – **×1.178** | `rarity% + variant×0.3% + (level−1)×0.5%` |
| **Party run** | ×1.0 – **×1.50** | +10% per crew member, capped at 5 extra runners |
| **XP Booster** | ×1.0 or **×2.0** | 24 h, must be bought for 200 SUP — a sink, not a gift |

Sneaker multiplier ceiling, worked out:

| Rarity | Base | Max variant | Max level | Max boost |
|---|---|---|---|---|
| Common | 0.0% | +0.6% | Lv.10 → +4.5% | **+5.1%** |
| Rare | 1.0% | +0.6% | Lv.15 → +7.0% | **+8.6%** |
| Epic | 2.0% | +0.6% | Lv.20 → +9.5% | **+12.1%** |
| Legendary | 3.0% | +0.3% | Lv.30 → +14.5% | **+17.8%** |

A free Common at Lv.1 earns **0.0100 SUP/step**. A maxed Legendary earns
**0.0118 SUP/step**. That 18-point spread is the entire pay-to-earn surface of
this game.

### 2.3 Energy capacity — the real progression lever

Sneaker level raises the daily **cap on rewardable steps**, not the rate:

```
maxEnergy(level) = 10 + (level − 1) × 2      cells
rewardableSteps  = maxEnergy × 600 ÷ energyEfficiency
```

| Sneaker level | Energy | Rewardable steps/day | ≈ distance |
|---|---|---|---|
| Lv.1 | 10 | 6,000 | 4.6 km |
| Lv.5 | 18 | 10,800 | 8.2 km |
| Lv.15 | 38 | 22,800 | 17.4 km |
| Lv.30 | 68 | 40,800 (up to 48,000 with max Comfort) | 31–37 km |

This is deliberately the *only* place where progression meaningfully scales, and
it is self-limiting: raising the cap is worthless unless the user can physically
walk into it. Above ~20,000 steps/day the binding constraint stops being the
token model and starts being the human body.

Comfort reduces energy drain by up to 15% (`energyEfficiency` floor 0.85), which
raises the cap but never the per-step rate.

### 2.4 Bonus emissions

| Source | Amount | Gate |
|---|---|---|
| **Daily goal** | 20 SUP × (1 + 0.1 × min(streak−1, 7)) → **20–34 SUP** | Once per day, goal met |
| **Course completion** | `distance_km × 1.0 SUP`, capped at **42 SUP** | ≥98% of the course actually covered, GPS-measured |
| **Events / challenges** | Fixed, published per campaign | Tied to real weekly step totals |

Course rewards are **fully deterministic** — no random roll, no loot box. The
same 5 km course pays 5.0 SUP to everyone who finishes it, always. This is
intentional: a reward a user can compute in advance is a reward they can trust.

---

## 3. Sinks — how SUP is destroyed

| Sink | Cost | Frequency |
|---|---|---|
| **Sneaker mint** | **500 SUP** | Repeatable — the primary sink |
| **Upgrade** | `level × 100 × (1 + rarityIndex × 0.25)` | Per level, escalating |
| **Energy Cell** | 50 SUP | Instant +2 energy |
| **Streak Shield** | 120 SUP | 24 h streak protection |
| **XP Booster** | 200 SUP | 24 h ×2 accrual |

Full upgrade cost to a rarity's max level:

| Rarity | Multiplier | Max level | Total upgrade cost |
|---|---|---|---|
| Common | ×1.00 | 10 | **4,500 SUP** |
| Rare | ×1.25 | 15 | **13,125 SUP** |
| Epic | ×1.50 | 20 | **28,500 SUP** |
| Legendary | ×1.75 | 30 | **76,125 SUP** |

### Mint odds and the expected cost of chasing rarity

| Rarity | Weight | Base probability |
|---|---|---|
| Common | 55 | 55% |
| Rare | 28 | 28% |
| Epic | 13 | 13% |
| Legendary | 4 | **4%** |

Luck biases the roll (`weight × (1 + luck × 0.15 × rarityIndex)`) but never
guarantees. At base odds, the **expected cost of obtaining one Legendary is
25 mints = 12,500 SUP** — and that is *before* the 76,125 SUP needed to max it.

This is the load-bearing sink. The reward for reaching the top of the collection
is +17.8% earning and a 40,800-step cap; the price is on the order of 88,000 SUP.
The ratio is intentionally unattractive as an *investment* and attractive as a
*goal*. Collection is meant to be a status pursuit, not a yield strategy.

---

## 4. Balance check — one year, three personas

Gross accrual vs. the sinks that persona actually spends into, over 365 days:

| | Casual | Committed | Elite |
|---|---|---|---|
| Session steps/day | 4,000 | 10,000 | 18,000 |
| Sneaker | Common Lv.5 | Epic Lv.15 | Legendary Lv.30 |
| Energy cap binding? | No (10,800) | No (22,800) | No (40,800) |
| **Gross accrual/yr** | ≈ **15,000 SUP** | ≈ **40,000 SUP** | ≈ **77,000 SUP** |
| + goal bonus (streak) | ≈ 7,300 | ≈ 12,400 | ≈ 12,400 |
| **Total in** | ≈ **22,300** | ≈ **52,400** | ≈ **89,400** |
| Mints attempted | 2 → 1,000 | 10 → 5,000 | 25 → 12,500 |
| Upgrades | 1,000 | 15,750 | 76,125 |
| Boosts | ≈ 3,000 | ≈ 10,000 | ≈ 15,000 |
| **Total out** | ≈ **5,000** | ≈ **30,750** | ≈ **103,625** |
| **Net** | **+17,300** | **+21,650** | **−14,225** |

Read this honestly: **the economy is net-emissive for casual and committed
players and net-deflationary for the top of the ladder.** That is the intended
shape — new users must accumulate something for the loop to feel worth starting,
and the players with the most SUP must have somewhere expensive to put it. The
on-chain budget in §7 is what bounds the aggregate of the positive rows.

We are not claiming the sinks alone close the loop. They compress it; the
emission budget closes it.

---

## 5. What SUP is *not*

Stated plainly, because grant reviewers should not have to guess:

- **SUP is not a security.** No revenue share, no dividend, no profit expectation
  from the efforts of the team is offered or implied.
- **SUP is not sold to users.** There is no token sale, no presale, no bonding
  curve in the product. The only way a user gets SUP is by moving.
- **Today, SUP is not on-chain.** It is a local ledger (Room) inside the app.
  §7 describes how that becomes an ERC-20 on GIWA. Nothing in this document
  should be read as a claim that an on-chain SUP exists at the time of writing.

---

## 6. Supply and allocation *(proposed — not yet deployed)*

| | |
|---|---|
| **Ticker** | SUP |
| **Standard** | ERC-20 |
| **Chain** | GIWA |
| **Decimals** | 18 |
| **Total supply** | **1,000,000,000 SUP — hard cap, no mint authority after deployment** |

| Allocation | Share | Amount | Unlock |
|---|---:|---:|---|
| **Move-to-Earn rewards** | 50% | 500,000,000 | Daily budget, halving every 730 days (§7.2) |
| **Ecosystem & community** | 15% | 150,000,000 | 48-month linear — events, crew grants, course-creator rewards |
| **Team & contributors** | 15% | 150,000,000 | 12-month cliff, then 36-month linear |
| **Treasury & liquidity** | 12% | 120,000,000 | 20% at TGE, remainder 24-month linear |
| **Grants & early supporters** | 8% | 80,000,000 | 6-month cliff, then 18-month linear |

The team allocation vests **behind** the reward pool's first halving on purpose:
the team should not be liquid before the emission schedule has proven it can
survive a full epoch.

---

## 7. On-chain settlement design *(target)*

### 7.1 Two layers, one token

| Layer | What it is | Status |
|---|---|---|
| **In-app SUP** | Off-chain accrual ledger. Earned per step, spent on mint / upgrade / boost. Never leaves the device economy. | **Live today** |
| **On-chain SUP** | ERC-20 on GIWA. Obtained by *claiming* in-app SUP through the distributor, bounded by the daily budget. | Designed |

Most SUP never needs to touch the chain — it is earned and burned inside the
collection loop. Only the **net surplus a user chooses to withdraw** consumes
the on-chain budget. This is why the app is fully playable with no wallet, and
why gas is never in the new-user funnel.

### 7.2 Emission budget and the distribution rate

The reward pool releases a **global daily budget** `B(d)` that halves each
730-day epoch:

| Epoch | Days | Daily budget | Epoch total |
|---|---|---:|---:|
| E1 | 1–730 | 250,000 SUP | 182,500,000 |
| E2 | 731–1460 | 125,000 SUP | 91,250,000 |
| E3 | 1461–2190 | 62,500 SUP | 45,625,000 |
| E4 | 2191–2920 | 31,250 SUP | 22,812,500 |
| E5 | 2921–3650 | 15,625 SUP | 11,406,250 |
| … | … | … | … |
| **Series limit** | | | **365,000,000** |
| **Governance reserve** | | | **135,000,000** |

Each user's claim is their share of the day's budget:

```
rate(d)  = min(1, B(d) / Σ claimScore(d))
payout_u = claimScore_u × rate(d)
```

`rate(d) = 1` while the network is small — early users are paid in full. As
claim pressure grows past the budget, the rate falls for everyone
proportionally. **The token cannot hyperinflate no matter how many users join**,
because the budget is fixed before the users are counted. The trade-off is
explicit and disclosed in-app: the rate is shown before a claim is signed.

The 135M reserve exists so that governance can extend the tail if the halving
proves too steep, **without** raising the 1B cap.

### 7.3 Contracts

Written and unit-tested in [`contracts/`](../contracts/) — Solidity 0.8.28,
OpenZeppelin 5.x, 43 passing tests. Not yet deployed.

| Contract | Standard | Responsibility |
|---|---|---|
| `SUPToken` | ERC-20 | Fixed 1B supply, no mint after deploy, burn-from-holder for on-chain sinks |
| `SneakerNFT` | ERC-721 | 44 base designs; `factionId`, `rarity`, `variant`, `level`, `luck`, `comfort` on-chain; art pinned to IPFS |
| `RewardDistributor` | — | Verifies a signed run proof, enforces `rate(d)`, transfers from the reward pool, records the session hash to prevent replay |
| `CourseRegistry` | — | `createCourse(nameHash, polylineHash, distanceM, author)`; makes course authorship and completion counts publicly verifiable |

Claim flow:

```
client                    backend attester              GIWA
  │ session record            │                          │
  ├──────────────────────────▶│                          │
  │  (steps, distance, GPS,   │  plausibility checks      │
  │   boosts, deviceAttest)   │  → EIP-712 signature      │
  │◀──────────────────────────┤                          │
  │  claim(sessionHash, amount, sig)                     │
  ├─────────────────────────────────────────────────────▶│
  │                           │   RewardDistributor:      │
  │                           │   verify sig, check       │
  │                           │   replay, apply rate(d),  │
  │                           │   transfer SUP            │
```

The Android client **already produces the exact record this flow consumes** —
`SessionReward(rewardedSteps, points, energyUsed)` plus the GPS polyline. The
missing pieces are the attester service and the contracts, not the client.

### 7.4 NFT economics

- Minting on-chain burns 500 SUP (100% burn — the mint is a sink, not revenue).
- Upgrades burn the full upgrade cost.
- Secondary sales carry a **5% royalty** to the treasury (EIP-2981).
- Sneaker stats are on-chain, so a sneaker's earning power is verifiable by
  anyone and cannot be inflated client-side.

---

## 8. Anti-abuse

An M2E token is only as sound as its proof that someone actually moved. Planned
controls, in order of implementation:

1. **Cadence sanity** — steps/second outside human range (>4 Hz sustained) void the session.
2. **GPS plausibility** — speed, acceleration and teleport checks against the
   recorded polyline; distance from the step count must agree with distance from
   GPS within tolerance.
3. **Device attestation** — Play Integrity API; rooted/emulated devices can play
   but cannot claim on-chain.
4. **Session replay protection** — each claim is keyed by session hash on-chain.
5. **Per-account daily ceiling** — the energy cap already bounds this at
   48,000 steps/day, on top of the global `rate(d)`.
6. **Sybil cost** — a new account earns at Lv.1 (6,000 steps/day cap) and must
   physically walk; farming N accounts requires N bodies or N spoofed GPS traces
   that survive check 2.

Controls 1, 2 and 5 are the ones that matter; 3 and 4 are the ones that make
industrial farming uneconomic.

---

## 9. Open questions we are not pretending to have solved

- **Rate transparency vs. UX.** A falling `rate(d)` is honest but discouraging.
  We show it pre-claim; whether users accept it at scale is unproven.
- **Bootstrapping liquidity.** 120M treasury/liquidity is an allocation, not a
  market-making plan. That plan is out of scope for this document.
- **Regulatory posture per market.** Korea, Japan and the EU treat reward tokens
  differently. Launch order will follow legal review, not product readiness.
- **The attester is centralised at launch.** A single signer is a trust
  assumption. Decentralising it (multi-attester threshold signatures) is a
  post-mainnet milestone, not a v1 claim.

---

<div align="center">

**StepUp** · [Repository](https://github.com/mycyi1994-hash/GIWASTEPN) · [One-pager](ONEPAGER.md)

</div>

---
---

<a name="한국어"></a>

# StepUp — 토큰 이코노미 (SUP) · 한국어

**[GIWA](https://giwa.io) 체인 기반 Move-to-Earn**
버전 1.0 · 앱 빌드 v1.12.1 기준

---

## 1. 요약

SUP는 StepUp의 리워드·유틸리티 자산입니다. **실제로 이동한 거리**로 발행되고,
**컬렉션 성장**으로 소각됩니다.

설계를 지배하는 원칙은 하나입니다.

> **유통에 들어오는 모든 SUP는 실측 가능한 신체 활동으로 대가를 치러야 하며,
> 개인이 같은 노력당 뽑아낼 수 있는 양은 재량이 아니라 구조로 상한이 걸린다.**

여기서 두 가지가 따라 나오고, 이것이 다른 M2E 프로젝트와의 차이입니다.

1. **배율 군비경쟁이 없습니다.** 최강 신발(만렙 레전더리)이 갓 시작한 무료
   신발보다 걸음당 **+17.8%** 더 법니다. 3배도, 10배도 아닙니다. 자본이 다리를
   이길 수 없습니다.
2. **소각처를 총량이 아니라 페르소나별로 발행량에 맞췄습니다.** 열심히 뛰는
   유저의 연간 적립(약 40,000 SUP)과 그가 실제로 원하는 성장의 비용(약
   30,000 SUP)이 같은 자릿수입니다 — 의도된 설계입니다. §6 참고.

§2~§4는 안드로이드 클라이언트에 **이미 구현되어 동작 중**입니다
(`domain/RewardEconomy.kt`, 단위 테스트 포함). §7~§9는 GIWA를 목표로 한
**온체인 설계**이며, 그렇게 명시했습니다.

---

## 2. 발행 — SUP는 어떻게 생기나

### 2.1 기본 적립

| 항목 | 값 | 근거 |
|---|---|---|
| 1보당 리워드 | **0.01 SUP** | `POINTS_PER_STEP` |
| 인정 걸음 | **러닝 세션 중에만** | `WalkSessionService` |
| 에너지 1칸당 걸음 | **600보** | `STEPS_PER_ENERGY` |
| 기본 에너지 | **10칸** (= 하루 6,000보 적립) | `BASE_MAX_ENERGY` |
| 리필 | 매일 자정, 최대치로 | `UserPrefs` |
| 에너지 0일 때 | **적립 완전 중단** | `sessionReward()` |

백그라운드 걸음은 적립되지 않습니다. 냉장고까지 걸어간 것도 안 됩니다. 유저가
앱을 열고 세션을 시작해야 하며, 그래서 리워드가 감사 가능해집니다 — 세션 하나가
정확히 하나의 `(걸음, 거리, 부스트, 지급액)` 레코드를 만듭니다.

### 2.2 배율

```
지급액 = 인정걸음 × 0.01 × 신발배율 × 파티배율 × 부스트배율
```

| 배율 | 범위 | 비고 |
|---|---|---|
| **신발** | ×1.000 ~ **×1.178** | `등급% + 변형×0.3% + (레벨−1)×0.5%` |
| **파티런** | ×1.0 ~ **×1.50** | 크루원 1명당 +10%, 추가 5명까지 |
| **XP 부스터** | ×1.0 또는 **×2.0** | 24시간, 200 SUP로 **구매** — 선물이 아니라 소각처 |

신발 배율 상한 계산:

| 등급 | 기본 | 최대 변형 | 최대 레벨 | 최대 부스트 |
|---|---|---|---|---|
| Common | 0.0% | +0.6% | Lv.10 → +4.5% | **+5.1%** |
| Rare | 1.0% | +0.6% | Lv.15 → +7.0% | **+8.6%** |
| Epic | 2.0% | +0.6% | Lv.20 → +9.5% | **+12.1%** |
| Legendary | 3.0% | +0.3% | Lv.30 → +14.5% | **+17.8%** |

무료 Common Lv.1은 걸음당 **0.0100 SUP**, 만렙 레전더리는 **0.0118 SUP**을
법니다. 이 18%p 격차가 이 게임의 pay-to-earn 표면 전부입니다.

### 2.3 에너지 용량 — 진짜 성장 레버

신발 레벨은 적립 **속도**가 아니라 하루 **인정 걸음 상한**을 올립니다.

```
maxEnergy(레벨) = 10 + (레벨 − 1) × 2      칸
인정걸음        = maxEnergy × 600 ÷ 에너지효율
```

| 신발 레벨 | 에너지 | 하루 인정 걸음 | ≈ 거리 |
|---|---|---|---|
| Lv.1 | 10 | 6,000 | 4.6 km |
| Lv.5 | 18 | 10,800 | 8.2 km |
| Lv.15 | 38 | 22,800 | 17.4 km |
| Lv.30 | 68 | 40,800 (착화감 최대 시 48,000) | 31~37 km |

성장이 의미 있게 확장되는 **유일한** 지점을 여기로 몰아넣었고, 이 레버는
자기제한적입니다. 상한을 올려도 몸이 거기까지 걷지 못하면 아무 소용이 없습니다.
하루 2만 보를 넘어가면 제약은 토큰 모델이 아니라 인체가 됩니다.

착화감은 에너지 소모를 최대 15% 줄여(`energyEfficiency` 하한 0.85) 상한을
올리지만, 걸음당 단가는 절대 올리지 않습니다.

### 2.4 보너스 발행

| 출처 | 금액 | 조건 |
|---|---|---|
| **일일 목표** | 20 SUP × (1 + 0.1 × min(스트릭−1, 7)) → **20~34 SUP** | 하루 1회, 목표 달성 |
| **코스 완주** | `거리(km) × 1.0 SUP`, 최대 **42 SUP** | GPS 기준 코스의 98% 이상 실제 주파 |
| **이벤트 / 챌린지** | 캠페인별 고정 공개 | 실제 주간 걸음 수 연동 |

코스 보상은 **완전히 결정적**입니다 — 확률도, 랜덤 박스도 없습니다. 같은 5 km
코스는 완주한 모두에게 언제나 5.0 SUP를 줍니다. 의도적입니다. 유저가 미리 계산할
수 있는 보상만이 신뢰할 수 있는 보상입니다.

---

## 3. 소각처 — SUP는 어떻게 사라지나

| 소각처 | 비용 | 빈도 |
|---|---|---|
| **신발 민팅** | **500 SUP** | 반복 가능 — 핵심 소각처 |
| **강화** | `레벨 × 100 × (1 + 등급인덱스 × 0.25)` | 레벨당, 체증 |
| **에너지 셀** | 50 SUP | 즉시 +2 에너지 |
| **스트릭 실드** | 120 SUP | 24시간 스트릭 보호 |
| **XP 부스터** | 200 SUP | 24시간 적립 ×2 |

등급별 최대 레벨까지 총 강화 비용:

| 등급 | 배수 | 최대 레벨 | 총 강화 비용 |
|---|---|---|---|
| Common | ×1.00 | 10 | **4,500 SUP** |
| Rare | ×1.25 | 15 | **13,125 SUP** |
| Epic | ×1.50 | 20 | **28,500 SUP** |
| Legendary | ×1.75 | 30 | **76,125 SUP** |

### 민팅 확률과 등급 추구의 기대 비용

| 등급 | 가중치 | 기본 확률 |
|---|---|---|
| Common | 55 | 55% |
| Rare | 28 | 28% |
| Epic | 13 | 13% |
| Legendary | 4 | **4%** |

행운 스탯이 확률을 보정하지만(`가중치 × (1 + 행운 × 0.15 × 등급인덱스)`) 보장하지는
않습니다. 기본 확률에서 **레전더리 1개의 기대 획득 비용은 25회 민팅 =
12,500 SUP**이고, 이는 만렙까지의 76,125 SUP를 **쓰기 전** 금액입니다.

이것이 하중을 받는 소각처입니다. 컬렉션 정점의 보상은 +17.8% 적립과 40,800보
상한이고, 대가는 대략 88,000 SUP입니다. 이 비율은 **투자로서는 의도적으로
매력이 없고, 목표로서는 매력적이도록** 잡았습니다. 수집은 수익 전략이 아니라
과시 욕구여야 합니다.

---

## 4. 밸런스 점검 — 1년, 3가지 페르소나

365일 기준 총 적립 vs 그 페르소나가 실제로 쓰는 소각처:

| | 캐주얼 | 열심 | 엘리트 |
|---|---|---|---|
| 세션 걸음/일 | 4,000 | 10,000 | 18,000 |
| 신발 | Common Lv.5 | Epic Lv.15 | Legendary Lv.30 |
| 에너지 상한에 걸리나? | 아니오 (10,800) | 아니오 (22,800) | 아니오 (40,800) |
| **연간 총 적립** | 약 **15,000 SUP** | 약 **40,000 SUP** | 약 **77,000 SUP** |
| + 목표 보너스(스트릭) | 약 7,300 | 약 12,400 | 약 12,400 |
| **총 유입** | 약 **22,300** | 약 **52,400** | 약 **89,400** |
| 민팅 시도 | 2회 → 1,000 | 10회 → 5,000 | 25회 → 12,500 |
| 강화 | 1,000 | 15,750 | 76,125 |
| 부스트 | 약 3,000 | 약 10,000 | 약 15,000 |
| **총 유출** | 약 **5,000** | 약 **30,750** | 약 **103,625** |
| **순증감** | **+17,300** | **+21,650** | **−14,225** |

정직하게 읽어 주십시오. **이 경제는 캐주얼·열심 구간에서는 순발행이고, 사다리
꼭대기에서는 순소각입니다.** 의도한 모양입니다 — 신규 유저는 뭔가 쌓여야 루프를
시작할 이유가 생기고, SUP를 가장 많이 가진 유저에게는 비싼 쓸 곳이 있어야
합니다. 위쪽 양(+) 행들의 총합을 묶는 장치가 §7의 온체인 예산입니다.

소각처만으로 루프가 닫힌다고 주장하지 않습니다. 소각처는 압축하고, **닫는 것은
발행 예산**입니다.

---

## 5. SUP가 **아닌** 것

심사자가 추측하지 않아도 되도록 분명히 적습니다.

- **SUP는 증권이 아닙니다.** 수익 배분도, 배당도, 팀의 노력에 기댄 이익 기대도
  제공하거나 암시하지 않습니다.
- **SUP를 유저에게 판매하지 않습니다.** 프로덕트 안에 토큰 세일·프리세일·본딩
  커브가 없습니다. 유저가 SUP를 얻는 유일한 방법은 움직이는 것입니다.
- **현재 SUP는 온체인이 아닙니다.** 앱 내부의 로컬 원장(Room)입니다. §7이 이것을
  GIWA의 ERC-20으로 만드는 방법입니다. 이 문서의 어떤 문장도 작성 시점에
  온체인 SUP가 존재한다는 뜻으로 읽혀서는 안 됩니다.

---

## 6. 공급량과 배분 *(제안 — 미배포)*

| | |
|---|---|
| **티커** | SUP |
| **표준** | ERC-20 |
| **체인** | GIWA |
| **소수점** | 18 |
| **총 발행량** | **1,000,000,000 SUP — 하드캡, 배포 후 추가 발행 권한 없음** |

| 배분 | 비율 | 수량 | 언락 |
|---|---:|---:|---|
| **Move-to-Earn 리워드** | 50% | 500,000,000 | 일일 예산, 730일마다 반감 (§7.2) |
| **생태계 & 커뮤니티** | 15% | 150,000,000 | 48개월 선형 — 이벤트, 크루 지원, 코스 제작자 보상 |
| **팀 & 기여자** | 15% | 150,000,000 | 12개월 클리프 후 36개월 선형 |
| **트레저리 & 유동성** | 12% | 120,000,000 | TGE 20%, 나머지 24개월 선형 |
| **그랜트 & 초기 지원자** | 8% | 80,000,000 | 6개월 클리프 후 18개월 선형 |

팀 물량이 리워드 풀의 첫 반감기 **뒤에** 풀리도록 일부러 잡았습니다. 발행
스케줄이 한 에포크를 견뎌내는 걸 증명하기 전에 팀이 유동화되어서는 안 됩니다.

---

## 7. 온체인 정산 설계 *(목표)*

### 7.1 두 개의 레이어, 하나의 토큰

| 레이어 | 정체 | 상태 |
|---|---|---|
| **인앱 SUP** | 오프체인 적립 원장. 걸음으로 벌고 민팅·강화·부스트로 씀. 기기 경제 밖으로 나가지 않음 | **현재 동작 중** |
| **온체인 SUP** | GIWA의 ERC-20. 인앱 SUP를 분배 컨트랙트로 *청구*해 획득, 일일 예산으로 제한 | 설계 단계 |

대부분의 SUP는 체인에 닿을 필요가 없습니다 — 컬렉션 루프 안에서 벌리고 태워집니다.
**유저가 인출하기로 선택한 순잉여**만 온체인 예산을 씁니다. 그래서 지갑 없이도
앱이 완전히 플레이되고, 신규 유저 퍼널에 가스비가 등장하지 않습니다.

### 7.2 발행 예산과 분배율

리워드 풀은 730일 에포크마다 반감하는 **글로벌 일일 예산** `B(d)`를 방출합니다.

| 에포크 | 일수 | 일일 예산 | 에포크 총량 |
|---|---|---:|---:|
| E1 | 1~730 | 250,000 SUP | 182,500,000 |
| E2 | 731~1460 | 125,000 SUP | 91,250,000 |
| E3 | 1461~2190 | 62,500 SUP | 45,625,000 |
| E4 | 2191~2920 | 31,250 SUP | 22,812,500 |
| E5 | 2921~3650 | 15,625 SUP | 11,406,250 |
| … | … | … | … |
| **급수 극한** | | | **365,000,000** |
| **거버넌스 예비** | | | **135,000,000** |

각 유저의 청구액은 그날 예산에 대한 지분입니다.

```
rate(d)  = min(1, B(d) / Σ claimScore(d))
지급액_u = claimScore_u × rate(d)
```

네트워크가 작을 때는 `rate(d) = 1`이라 초기 유저는 전액을 받습니다. 청구 압력이
예산을 넘어서면 모두에게 비례해서 요율이 내려갑니다. **유저가 아무리 늘어도
토큰은 하이퍼인플레이션을 일으킬 수 없습니다.** 유저를 세기 전에 예산이 먼저
고정되기 때문입니다. 이 트레이드오프는 앱에 명시합니다 — 청구 서명 전에 현재
요율을 보여줍니다.

135M 예비 물량은 반감이 지나치게 가파른 것으로 드러났을 때 거버넌스가 꼬리를
연장할 수 있게 하기 위한 것이며, **10억 하드캡을 올리지는 않습니다.**

### 7.3 컨트랙트

[`contracts/`](../contracts/)에 작성·단위 테스트까지 되어 있습니다 — Solidity
0.8.28, OpenZeppelin 5.x, 테스트 43개 통과. 아직 배포 전입니다.

| 컨트랙트 | 표준 | 역할 |
|---|---|---|
| `SUPToken` | ERC-20 | 10억 고정 공급, 배포 후 발행 불가, 온체인 소각처를 위한 보유자 소각 |
| `SneakerNFT` | ERC-721 | 44종 기본 디자인. `factionId`, `rarity`, `variant`, `level`, `luck`, `comfort`를 온체인 보관, 아트는 IPFS 고정 |
| `RewardDistributor` | — | 서명된 러닝 증명 검증, `rate(d)` 적용, 리워드 풀에서 전송, 세션 해시 기록으로 재사용 차단 |
| `CourseRegistry` | — | `createCourse(nameHash, polylineHash, distanceM, author)` — 코스 작성자와 완주 횟수를 공개 검증 가능하게 |

청구 흐름:

```
클라이언트                백엔드 어테스터            GIWA
  │ 세션 레코드                │                       │
  ├──────────────────────────▶│                       │
  │  (걸음, 거리, GPS,         │  타당성 검사           │
  │   부스트, 기기증명)         │  → EIP-712 서명        │
  │◀──────────────────────────┤                       │
  │  claim(sessionHash, amount, sig)                  │
  ├──────────────────────────────────────────────────▶│
  │                           │  RewardDistributor:    │
  │                           │  서명 검증, 재사용 확인, │
  │                           │  rate(d) 적용, SUP 전송 │
```

안드로이드 클라이언트는 **이 흐름이 소비하는 레코드를 이미 그대로 생성하고
있습니다** — `SessionReward(rewardedSteps, points, energyUsed)`와 GPS 폴리라인.
빠진 것은 어테스터 서비스와 컨트랙트이지, 클라이언트가 아닙니다.

### 7.4 NFT 경제

- 온체인 민팅은 500 SUP를 소각합니다(100% 소각 — 민팅은 수익이 아니라 소각처).
- 강화는 강화 비용 전액을 소각합니다.
- 2차 판매에는 트레저리로 가는 **5% 로열티**(EIP-2981)가 붙습니다.
- 신발 스탯이 온체인이므로 신발의 적립 능력을 누구나 검증할 수 있고,
  클라이언트에서 부풀릴 수 없습니다.

---

## 8. 어뷰징 방지

M2E 토큰의 건전성은 "정말 움직였다"는 증명만큼입니다. 구현 순서대로:

1. **케이던스 정합성** — 초당 걸음이 인간 범위를 벗어나면(지속 4 Hz 초과) 세션 무효.
2. **GPS 타당성** — 기록된 폴리라인에 대한 속도·가속도·순간이동 검사. 걸음 수로
   계산한 거리와 GPS로 계산한 거리가 오차 범위 안에서 일치해야 함.
3. **기기 증명** — Play Integrity API. 루팅·에뮬레이터 기기도 플레이는 되지만
   온체인 청구는 불가.
4. **세션 재사용 방지** — 모든 청구를 온체인 세션 해시로 키잉.
5. **계정별 일일 상한** — 에너지 상한이 이미 하루 48,000보로 묶고, 그 위에
   글로벌 `rate(d)`가 걸림.
6. **시빌 비용** — 신규 계정은 Lv.1(하루 6,000보 상한)에서 시작하고 실제로 걸어야
   함. N개 계정 파밍에는 N개의 몸이나, 2번 검사를 통과하는 N개의 위조 GPS 궤적이
   필요.

실질적으로 중요한 것은 1·2·5번이고, 산업적 파밍을 수지 안 맞게 만드는 것은
3·4번입니다.

---

## 9. 해결한 척하지 않는 미결 과제

- **요율 투명성 vs UX.** 내려가는 `rate(d)`는 정직하지만 의욕을 꺾습니다. 청구
  전에 보여주긴 하지만, 규모가 커졌을 때 유저가 이를 받아들일지는 검증되지
  않았습니다.
- **유동성 부트스트랩.** 트레저리·유동성 120M은 배분이지 마켓메이킹 계획이
  아닙니다. 그 계획은 이 문서의 범위 밖입니다.
- **시장별 규제 포지션.** 한국·일본·EU가 리워드 토큰을 다르게 다룹니다. 출시
  순서는 제품 준비도가 아니라 법률 검토를 따릅니다.
- **론칭 시점의 어테스터는 중앙화되어 있습니다.** 단일 서명자는 신뢰 가정입니다.
  이를 탈중앙화하는 것(다중 어테스터 임계 서명)은 메인넷 이후 마일스톤이며,
  v1에서 주장하지 않습니다.

---

<div align="center">

**StepUp** · [저장소](https://github.com/mycyi1994-hash/GIWASTEPN) · [원페이저](ONEPAGER.md)

</div>
