<div align="center">

# StepUp

### Every Step. Every Stride. Every Day.

**A Move-to-Earn (M2E) running app for the [GIWA](https://giwa.io) chain.**
Walk or run in the real world, earn **SUP**, and grow a collection of sneaker NFTs.

[![Platform](https://img.shields.io/badge/platform-Android-3DDC84)](#)
[![minSdk](https://img.shields.io/badge/minSdk-26-blue)](#)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF)](#)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4)](#)
[![Chain](https://img.shields.io/badge/chain-GIWA-C3FF3E)](#)

**[⬇️ Download the APK](#-download--try-it)** · [Screenshots](#-screenshots) · [Build from source](#-build-from-source)

**English** · [한국어](README.ko.md)

📄 **[One-Pager](docs/ONEPAGER.md)** · 🏗️ **[Architecture](docs/ARCHITECTURE.md)** ([PDF](docs/StepUp-Architecture.pdf)) · 💰 **[Tokenomics](docs/TOKENOMICS.md)** · 🎤 **[Pitch Deck](docs/PITCH.md)** ([PDF](docs/StepUp-PitchDeck.pdf)) · ⛓️ **[Contracts](contracts/)** · 🌐 **[Landing page](web/)** · 🔏 **[Attester](attester/)** · 🚀 **[Launch plan](docs/LAUNCH-PLAN.md)**

</div>

---

## 📌 What is StepUp?

StepUp turns everyday walking and running into an on-chain reward loop.

Most fitness apps stop at a step counter. Most M2E apps stop at a token faucet.
StepUp is built as a **complete consumer app first** — a running tracker, a
sneaker NFT collection, a course-sharing map, a crew community — and then wires
that behaviour to a token economy that GIWA can settle.

| | |
|---|---|
| **Category** | Consumer / SocialFi / Move-to-Earn |
| **Platform** | Native Android (Kotlin + Jetpack Compose) |
| **Chain** | GIWA (target settlement layer for SUP and sneaker NFTs) |
| **Status** | Working app, installable APK, **60+ screens shipped**. On-chain settlement is the next milestone — see the [roadmap](#-roadmap). |
| **Scale today** | 81 Kotlin source files · 666 localized strings × 4 languages · 44 sneaker NFT designs · 100 achievements · 60 runner levels |

### The problem

1. **Fitness apps have no reason to keep you.** Streaks break, the app is deleted.
2. **M2E apps have no reason to exist beyond the token.** When emissions drop, so does the DAU.
3. **Onboarding Web2 users to a chain is brutal.** Seed phrases and gas kill the funnel before the first run.

### What StepUp does about it

1. **The app is worth opening without the token.** GPS course maps, lap splits,
   crew party-runs, flash-run meetups, a 44-piece NFT collection, 100 achievements.
2. **Emissions are capped and honest.** Rewards accrue *only* during an active
   run session, *only* while energy remains, and sneaker boosts top out at ~+18%
   so whales cannot outfarm newcomers. See [Token economy](#-token-economy-sup).
3. **Chain complexity is deferred, not dumped on the user.** SUP is earned and
   spent locally from the first second; the GIWA wallet is something you connect
   when you want to withdraw, not something you fight before your first step.

---

## 📥 Download & try it

**Direct APK (latest CI build):**

```
https://github.com/mycyi1994-hash/GIWASTEPN/raw/apk-dist/StepUp-debug.apk
```

Or with `curl`:

```bash
curl -L -o StepUp.apk \
  https://github.com/mycyi1994-hash/GIWASTEPN/raw/apk-dist/StepUp-debug.apk
```

**Install on an Android phone (API 26+ / Android 8.0+):**

1. Open the link above in the phone's browser and download the `.apk`.
2. Android will ask to allow installs from this source — allow it for the browser.
3. Open the downloaded file and tap **Install**.
4. On first launch, grant **Physical activity** (step counter) and **Location**
   (GPS course tracking). Notifications are optional but recommended.

Every push to the development branch rebuilds the APK through
[GitHub Actions](.github/workflows/build-apk.yml) and force-pushes it to the
[`apk-dist`](../../tree/apk-dist) branch, so the link above is always the newest build.
The workflow also uploads a `StepUp-debug-apk` artifact on each run.

> **Testing without a step sensor?** Emulators usually have no
> `TYPE_STEP_COUNTER`. Debug builds expose a **"+100 steps (debug)"** button on
> the run screen so the whole earn → energy → ledger flow can be exercised.

---

## 📱 Screenshots

| Home | Live run · GPS course | Community |
|:--:|:--:|:--:|
| ![Home](docs/screenshots/01-home.png) | ![Live run](docs/screenshots/02-live-run.png) | ![Community](docs/screenshots/03-community.png) |
| Steps, energy ring, equipped sneaker, one-tap **START RUN** | Real GPS track on a neon course map, goal ring, 9 live metrics, laps | Boards, crews, nearby flash-runs, ranking |

| Items · NFT | Events | Profile |
|:--:|:--:|:--:|
| ![Items](docs/screenshots/04-items.png) | ![Events](docs/screenshots/05-events.png) | ![Profile](docs/screenshots/06-profile.png) |
| Sneaker NFT collection, minting, upgrades, boost shop | Seasonal campaigns, weekly challenges, referrals | Runner level, lifetime stats, achievements, settings |

---

## ✨ Features

### Run tracking

- **Foreground run session** — `TYPE_STEP_COUNTER` + `LocationManager` GPS in a
  foreground service (`health|location`), survives screen-off and app switching.
- **Real GPS course maps** — coordinates are Haversine-measured, simplified, and
  normalized (with `cos(latitude)` correction so the shape never skews), then
  drawn as a neon track. Start dot, finish flag, live runner dot.
- **Laps** — automatic split at every kilometre plus a manual lap button.
- **Live metrics** — pace/km, lap split, heart rate, cadence (spm), calories,
  steps, speed, elevation gain, elapsed time, projected finish time.

### Courses (create · select · share)

- **코스 선택 / Course picker** — pick a course before a run; the run screen then
  renders your live GPS trace against it.
- **코스 만들기 / Course builder** — record a run, then save the recorded GPS
  track as a named, reusable course.
- **코스 게시판 / Course board** — publish a course so other runners can take it,
  like it, and run it. Like and run counts are tracked per course.
- **Distance-scaled completion rewards** — finishing a course pays
  `distance_km × 1.0 SUP`, capped at 42 SUP (marathon distance). Fully
  deterministic, no random rolls, and it only pays at ≥98% of the course length.
- Seeded with five real Seoul courses (Yeouido 4.68 km, Banpo 4.10 km, Olympic
  Park 3.24 km, Namsan 2.73 km, Seoul Forest 2.26 km).

### Sneaker NFTs — a 44-piece collection

- **4 factions** (🔥 Fire · 💧 Water · ⚡ Lightning · 🌪 Wind)
  × **11 rarity variants** (Common 3 · Rare 3 · Epic 3 · Legendary 2) = **44 designs**.
- Faction sets the colourway and effect, rarity sets the silhouette, ornament and boost.
- **Minting** costs 500 SUP; the Luck stat biases the rarity roll. Each mint gets
  a serial (`#0001`).
- **Boosts are deliberately small** — +1% per rarity tier, +0.3% per variant,
  +0.5% per level. Even a max-rolled Legendary at Lv.30 is only ≈ **+18%**, so
  collecting stays fun without becoming pay-to-farm.
- Comfort reduces energy drain (up to 15%); rarity caps the upgrade level (10–30).

### Runner level — XP from real kilometres

Level is **not** the NFT's level. It is the user's own XP, earned from distance
actually covered.

- 60 levels. Level *n* requires `3.0 + 1.6 × (n−1)` km, so the cumulative
  requirement to Lv.60 is ≈ **2,914.6 km**.
- Titles unlock along the way: Rookie → Strider → Trailblazer → Pacesetter →
  Elite → Master → Legend.

### Community

- **Boards** — flash-run (번개러닝), free talk, and tips, sorted by proximity for
  flash-runs, with category chips and full-text search.
- **Flash-run detail** — meeting place opens in **Google Maps**, participant
  roster, join/leave with live capacity.
- **Comments and replies** — threaded replies (대댓글) generate a notification for
  the parent author.
- **Crews** — join or create a crew, crew-only board, and **party runs**:
  ready-check → 3·2·1 countdown → synchronized measurement → **+10% per crew
  member (max +50%)** applied to the settled SUP.
- **Ranking** — weekly steps and lifetime SUP, with a podium and a full ladder.

### Events, items, profile

- Seasonal campaign (Neon Horizon), weekly challenges wired to real step data,
  referral sharing, and claims that credit real SUP.
- Boost shop (energy cell, streak shield, XP booster) with real purchase and effect.
- Profile with lifetime stats, 100 achievements across 13 categories, monthly
  summary, daily goal, wallet, notification inbox, analytics, and settings.

### Localization

Korean · English · 简体中文 · 日本語 — **666 strings**, fully translated.
Follows the system language by default, and on Android 13+ the in-app
**language setting** (`localeConfig`) can override it per-app. Every user-facing
string is localized; only proper nouns (StepUp, SUP, GIWA) stay fixed.

---

## 💰 Token economy (SUP)

`domain/RewardEconomy.kt` — unit-tested in `app/src/test/`.

| Rule | Value |
|---|---|
| **Base accrual** | `0.01 SUP` per step, **only during an active run session** |
| **Energy gate** | 1 energy cell = 600 rewardable steps. Refills at local midnight. At 0 energy, accrual stops. |
| **Sneaker boost** | rarity **+1%** / variant **+0.3%** / level **+0.5%** → max ≈ **+18%** |
| **Party run** | **+10%** per crew member, capped at **+50%** |
| **Daily goal bonus** | **20 SUP**, plus **+10%** per consecutive day (7-day cap) |
| **Course completion** | `distance_km × 1.0 SUP`, capped at **42 SUP**, paid at ≥98% completion |
| **Sinks** | Minting (500 SUP), upgrades (100 SUP+), boost shop items (50–200 SUP) |

**Design intent.** Every emission is gated by *physical distance actually
covered*, and the multiplier ceiling is low by construction. A brand-new player
with no NFT earns within ~18% of a maxed-out player on the same run — so the
economy is driven by how much people move, not by how much they staked. Sinks
(minting, upgrades, boosts) are priced against that emission rate so SUP has a
reason to leave circulation.

📖 Full model — supply schedule, per-persona balance tables, anti-abuse, and the
open questions we have *not* solved: **[docs/TOKENOMICS.md](docs/TOKENOMICS.md)**

---

## ⛓️ GIWA integration plan

Being precise about what exists today, because reviewers deserve that.
The contracts are written and unit-tested in [`contracts/`](contracts/) —
Solidity 0.8.28, OpenZeppelin 5.x, **43 passing tests** — and target
**GIWA Sepolia** (chain ID 91342). None of them is deployed yet.

| Layer | Today | Next |
|---|---|---|
| **SUP balance** | Local ledger (Room), every accrual and spend recorded as a row | ERC-20 `SUPToken` on GIWA; the local ledger becomes the off-chain accrual buffer |
| **Sneaker NFT** | 44 designs, minting/upgrade/equip fully implemented locally | ERC-721 `SneakerNFT` on GIWA, metadata pinned to IPFS |
| **Reward settlement** | Computed on-device by `RewardEconomy` | `RewardDistributor` contract; the client submits a signed run proof and claims |
| **Courses** | Room, shared in-app | `CourseRegistry` contract so a course and its author are publicly verifiable |
| **Wallet** | Wallet screen with ledger and withdrawal UX in place | GIWA wallet connect + withdraw |

The reward math, the ledger schema, and the wallet UX were all built to be
settled on-chain — the client already produces exactly the per-session
`(steps, distance, boost, payout)` record a distributor contract needs.

---

## ⛓️ Contracts

Solidity 0.8.28 · OpenZeppelin 5.x · Hardhat · **43 passing tests** ·
**live on GIWA Sepolia (91342)** since 2026-07-31

| Deployed | Address |
|---|---|
| `SUPToken` | [`0xb052A8f6…9006c1B`](https://sepolia-explorer.giwa.io/address/0xb052A8f6A5034747902b6d6787bbfF31A9006c1B) |
| `SneakerNFT` | [`0x8174f905…BabEFc960`](https://sepolia-explorer.giwa.io/address/0x8174f905d86438ac8922c85d3A48604BabEFc960) |
| `RewardDistributor` | [`0x9f9E87bD…aCFE36E1`](https://sepolia-explorer.giwa.io/address/0x9f9E87bD825144A8315d30979E3004FbaCFE36E1) |
| `CourseRegistry` | [`0x6c815DF0…C588542`](https://sepolia-explorer.giwa.io/address/0x6c815DF0d8a5CA7CA0487D1AC2f96c0fEC588542) |

The reward pool holds 50,000,000 SUP (5% of supply). Full record:
[`contracts/deployments/giwaSepolia.json`](contracts/deployments/giwaSepolia.json).
The app still settles locally — wiring the client to these contracts is the
next step, and this README will say so plainly until it is done.

| Contract | Standard | What it guarantees |
|---|---|---|
| `SUPToken` | ERC-20 | 1,000,000,000 SUP minted once. **No mint function exists**, so supply can only fall. |
| `SneakerNFT` | ERC-721 | The boost ceiling is `1780 bps` — a `constant`, not a parameter. Stats on-chain, so earning power is verifiable rather than asserted. |
| `RewardDistributor` | — | The attester decides who gets paid; the contract decides how much may exist. Every claim is charged against a halving daily budget, so a compromised signer cannot inflate the token. **No withdraw, sweep or rescue** — SUP leaves only via `claim`, only to a runner. |
| `CourseRegistry` | — | Course authorship is public and permissionless. `rewardFor(distanceM)` is pure: `km × 1.0 SUP`, capped at 42. |

```bash
cd contracts && npm install && npm test
```

Deployment is a six-step walkthrough (wallet → faucet → deploy → verify) in
**[contracts/README.md](contracts/README.md)**.

---

## 🛠 Tech stack

| Layer | Choice |
|---|---|
| Language | Kotlin 2.0.21 |
| UI | Jetpack Compose (BOM 2024.12.01) + Material 3, Navigation Compose |
| Architecture | MVVM + Repository, manual DI (`ServiceLocator`), `StateFlow` throughout |
| Persistence | Room 2.6.1 (KSP) — daily records, sessions, reward ledger, community, courses · DataStore Preferences for settings |
| Sensors | `TYPE_STEP_COUNTER` (midnight/reboot baseline correction) + `LocationManager` GPS |
| Background | Foreground service, `health\|location` type |
| Build | AGP 8.7.3, compileSdk 35, minSdk 26, targetSdk 35 |
| CI | GitHub Actions — unit tests → assembleDebug → signature verification → publish APK |

### Project structure

```
app/src/main/java/com/stepup/android/
├── core/            # ServiceLocator (manual DI), AppLocale
├── data/
│   ├── local/       # Room entities, DAOs, database (v6)
│   ├── prefs/       # DataStore — goal, energy, streak, language, selected course
│   └── repo/        # Step, Reward, Sneaker, Community, Crew, Course repositories
├── domain/
│   ├── RewardEconomy.kt   # M2E economy — unit tested
│   ├── Sneaker.kt         # faction × rarity × silhouette × minting
│   ├── RunnerLevel.kt     # user XP from cumulative kilometres (60 levels)
│   ├── RunCourse.kt       # GeoPoint, Haversine, simplify, normalize, rewards
│   └── Community.kt       # posts, categories, ranking
├── sensor/          # StepTracker — step sensor wrapper
├── service/         # WalkSessionService — run session, laps, GPS track
└── ui/
    ├── theme/       # Volt palette, brushes, typography, shapes
    ├── components/  # HexEmblem, NeonRing, GlowCard, CourseTrackMap, SneakerImage
    ├── guide/       # spotlight onboarding tour
    └── screens/     # home · community · items · events · profile · walk · rewards · settings · splash · login
```

---

## 🎨 Design — "Volt"

Pure black canvas, a single neon-lime accent, and softness that comes from
curvature and negative space rather than colour.

| Axis | Rule |
|---|---|
| **Colour** | Deep black `#060708` + Volt lime `#C3FF3E` as the only accent — nothing competes for saturation |
| **Type** | ExtraBold/Black with tight tracking for numbers and headings; wide-tracked uppercase for micro labels |
| **Surface** | Carbon cards (12–32 dp radius) with hairline borders; only the hero card gets a Volt outline |

Signature elements: the hexagon token emblem, the neon progress ring, the glow
route map, the Volt **START RUN** CTA, and the hexagonal level-badge avatar.

Sneaker artwork is composited rather than pasted: each image is exported with a
lifted black point so the backdrop crushes to true black, then given a padded
alpha feather **baked into the file**, so the art dissolves into the card
without ever clipping the shoe itself.

---

## 🔨 Build from source

```bash
git clone https://github.com/mycyi1994-hash/GIWASTEPN.git
cd GIWASTEPN

./gradlew :app:assembleDebug        # debug APK → app/build/outputs/apk/debug/
./gradlew :app:testDebugUnitTest    # economy unit tests
```

Requires JDK 17 and the Android SDK (compileSdk 35). Opening the project in
Android Studio (Ladybug or newer) syncs everything automatically.

### About the committed debug keystore

`app/debug.keystore` is committed on purpose. CI runners are recreated per run,
so AGP's auto-generated debug key would differ on every build — and a differently
signed APK cannot be installed over the previous one
(`INSTALL_FAILED_UPDATE_INCOMPATIBLE`). Pinning the key keeps updates installable
for testers.

This is a standard-issue Android debug key (password `android`) with **no release
authority**. A Play Store build must inject a separate release keystore from CI
secrets. Every CI run verifies that the produced APK's SHA-256 certificate
fingerprint matches this keystore.

---

## 🗺 Roadmap

- [x] Volt design system + 5 tabs + 60 screens
- [x] Foreground run session with step sensor + GPS
- [x] Sneaker NFT collection — 4 factions × 11 variants (44 designs)
- [x] Runner XP level system from cumulative kilometres (60 levels)
- [x] Community boards, crews, party runs, ranking, comments and replies
- [x] Course system — create, select, share, distance-scaled completion rewards
- [x] Localization — Korean / English / Chinese / Japanese, in-app language setting
- [x] **Contracts written and tested** — `SUPToken` (ERC-20), `SneakerNFT` (ERC-721), `RewardDistributor`, `CourseRegistry` — see [`contracts/`](contracts/)
- [x] **Deployed on GIWA Sepolia** (chain ID 91342) — 4 contracts live since 2026-07-31, reward pool funded with 50,000,000 SUP ([addresses](#-contracts))
- [ ] Source-verify the deployed contracts on the GIWA explorer
- [ ] Real Google sign-in and account sync (today's sign-in is a local demo)
- [ ] Backend for community, ranking and course sharing (currently local + seeded)
- [ ] **Wallet + on-chain SUP withdrawal** — the app still settles entirely on-device
- [ ] Sneaker NFT marketplace (trade / rent)
- [ ] Health Connect integration
- [ ] Anti-cheat — GPS plausibility, cadence sanity, server-side run proof

The full path from here to a Play Store release — architecture, design decisions, phases and exit criteria — is in **[docs/LAUNCH-PLAN.md](docs/LAUNCH-PLAN.md)**.

---

<div align="center">

**StepUp** · built for the GIWA ecosystem

</div>
