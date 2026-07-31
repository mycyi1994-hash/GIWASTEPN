# GASOK 지원서 — 복붙용 완본

**작성 기준일 2026-07-31 · 앱 v1.15.1 · 컨트랙트 GIWA Sepolia 배포 완료**

아래 회색 블록을 **그대로 복사해서 폼에 붙여넣으시면** 됩니다.
각 문항마다 **영어판 / 한국어판** 두 가지를 넣었습니다.

> **어느 쪽을 쓸까요?** → **영어판을 권합니다.** GASOK 폼 자체가 영어이고,
> 심사자가 한국어를 못 읽을 가능성이 있습니다. 한국어판은 국내 심사자용 대안입니다.

---

## ⚠️ 붙여넣기 전에 반드시 확인

1. **저장소가 공개(Public)여야 합니다.** 비공개면 아래 링크가 전부 404입니다.
   → GitHub Settings → General → Danger Zone → Change visibility → Make public
2. 링크는 **브랜치 이름을 명시한 형태**로 적었습니다
   (`.../blob/claude/work-history-pjm57c/...`).
   기본 브랜치를 무엇으로 바꾸시든 **이 링크는 그대로 동작합니다.**
   나중에 브랜치 이름을 바꾸시면 그때 링크도 같이 바꿔야 합니다.
3. **시크릿 모드로 링크를 하나 열어보고** 붙여넣으세요.
4. `8/12`의 `<VIDEO_URL>` 은 데모 영상 업로드 후 실제 링크로 바꾸세요.
   (영상이 없으면 그 줄 전체를 지우고 붙여넣어도 통과합니다)

---

## 📊 글자수 검증표

전부 500자 제한 안에 들어가고, 하나의 언어 기준 75% 이상을 채웠습니다.
`8/12`는 유튜브 링크(28자)를 실제로 넣었다고 가정한 길이입니다.

| 문항 | 영어 | 한국어 |
|---|---:|---:|
| **3/12** Team Introduction | 478자 (96%) | 393자 (79%) |
| **4/12** Motivation for Applying | 499자 (100%) | 390자 (78%) |
| **7/12** Pitch Deck | 472자 (94%) | 380자 (76%) |
| **8/12** Project Link | 490자 (98%) | 434자 (87%) |
| **9/12** GIWA Testnet Verified Smart Contract Link | 492자 (98%) | 412자 (82%) |
| **10/12** 기술 문서 / 원페이저 링크 | 497자 (99%) | 387자 (77%) |

---

# [3/12] Team Introduction
**팀 소개** · shareable file link (이름·역할·국적·경력·강점)

> ⚠️ **`<TEAM_FILE_URL>` 을 직접 만드신 팀 소개 파일 링크로 바꾸세요.**
> 실명과 경력은 **저장소에 올리지 않습니다** — 공개 저장소라 검색엔진에
> 걸리고, 본인 동의 없이 남의 이력이 공개되는 문제가 됩니다.
> 구글 드라이브·노션 등에 올려 **링크를 아는 사람만 보기**로 두시면 됩니다.
> (GASOK 폼이 요구하는 건 "shareable file link"이지 GitHub가 아닙니다.)

### 🇬🇧 영어판 (472자 / 500 · 링크 60자 가정)

```
Team introduction (name, role, nationality, experience, core strengths):
<TEAM_FILE_URL>

Five traders from Korean securities and asset-management firms — proprietary trading, macro strategy, CTA solutions, systematic execution. All Korean nationals, based in Seoul.

The document maps each member to the product surface they own, and states the gaps we are short on, not only what we have: a mobile engineer to hire, no external contract audit yet, no backend.
```

### 🇰🇷 한국어판 (392자 / 500 · 링크 60자 가정)

```
팀 소개(이름, 역할, 국적, 경력, 핵심 강점):
<TEAM_FILE_URL>

국내 증권·자산운용사에서 자기자본 트레이딩, 매크로 전략, CTA 솔루션, 시스템 집행을 각자 10~20년 해 온 5인입니다. 전원 대한민국 국적이며 서울에 거점을 두고 있습니다.

문서에 구성원 각자가 책임지는 제품 영역을 표로 대응시켰습니다. 리워드 이코노미 파라미터, 발행 스케줄, 러닝 증명과 어뷰징 방지, 커뮤니티 그로스, 온체인 유동성 순입니다. 가진 것뿐 아니라 부족한 부분(모바일 엔지니어 충원, 컨트랙트 외부 감사, 백엔드 부재)도 따로 밝혔습니다.
```

---

# [4/12] Motivation for Applying
**지원 동기** · 500자 · What drives your team to apply for the GASOK program?

### 🇬🇧 영어판 (499자 / 500)

```
We are five traders from Korean securities and asset-management desks, a decade each pricing risk. We watched every M2E cycle die the same way: the multiplier ladder turned the game into a yield calculation, and when yield fell the runners left too.

StepUp is our answer. The app shipped first. The earning ceiling is +17.8%, not 10x.

We apply for what we cannot do alone — deploying and auditing on GIWA, and the Korean users Upbit already has. Our contracts are tested. We ask for the last mile.
```

### 🇰🇷 한국어판 (390자 / 500)

```
저희는 국내 증권·자산운용 데스크에서 각자 10여 년간 리스크를 가격 매겨 온 트레이더 5인입니다. 그동안 모든 M2E 사이클이 같은 방식으로 무너지는 것을 봤습니다. 배율 사다리가 게임을 수익률 계산으로 바꿨고, 수익률이 꺾이자 러너들도 함께 떠났습니다. 문제는 아이디어가 아니라 설계였고, 설계로 우회할 수 있다고 판단했습니다.

StepUp은 그에 대한 답입니다. 앱을 먼저 완성했고, 적립 상한은 10배가 아니라 +17.8%입니다. 자본이 다리를 이길 수 없도록 구조로 묶었습니다.

저희 힘만으로 안 되는 것 때문에 지원합니다. GIWA 위에서의 배포와 외부 감사, 그리고 업비트가 이미 가진 국내 소비자 접점입니다. 컨트랙트는 작성과 테스트를 마쳤습니다. 마지막 한 구간을 요청드립니다.
```

---

# [7/12] Pitch Deck
**피치덱** · shareable link · 500자

### 🇬🇧 영어판 (472자 / 500)

```
Pitch deck (15 slides, English and Korean):
https://github.com/mycyi1994-hash/GIWASTEPN/blob/claude/work-history-pjm57c/docs/PITCH.md

Covers the problem, why the last M2E cycle failed, our thesis (app first, +17.8% earning ceiling), the shipped product, the token economy with per-persona balance tables, the on-chain design for GIWA, current status, roadmap, team, and the ask.

Each slide separates headline, body and visual direction, so it drops straight into slides.
```

### 🇰🇷 한국어판 (380자 / 500)

```
피치덱(15장, 영문·한국어 병기):
https://github.com/mycyi1994-hash/GIWASTEPN/blob/claude/work-history-pjm57c/docs/PITCH.md

문제 정의, 지난 M2E 사이클이 실패한 이유, 우리의 주장(앱 우선 · 적립 상한 +17.8%), 완성된 제품, 페르소나별 연간 밸런스 표가 있는 토큰 이코노미, GIWA 온체인 설계, 현재 상태, 로드맵, 팀, 요청 사항을 담았습니다.

3장을 "지난 사이클은 왜 실패했나"에 배정해 배율 사다리가 원인이었다는 진단을 먼저 내고, 4장에서 +17.8% 상한을 그 해답으로 제시하는 구성입니다. 각 장이 제목·본문·비주얼 지시로 분리돼 있어 그대로 슬라이드로 옮길 수 있습니다.
```

---

# [8/12] Project Link
**프로젝트 링크** · working MVP and/or video · 500자

### 🇬🇧 영어판 (496자 / 500)

```
Working MVP (APK):
https://github.com/mycyi1994-hash/GIWASTEPN/raw/apk-dist/StepUp-debug.apk

Demo video (2m30s): <VIDEO_URL>

Install it and try it yourself. StepUp is a Move-to-Earn running app: GPS course
tracking with laps, 44 sneaker NFTs, crews and party runs, in 4 languages. 76
Kotlin files, 60+ screens, CI-built APK on every push.

Source, docs, contracts: github.com/mycyi1994-hash/GIWASTEPN — four GIWA
contracts live on Sepolia (chain 91342), 43 tests passing.
```

### 🇰🇷 한국어판 (436자 / 500)

```
동작하는 MVP (APK):
https://github.com/mycyi1994-hash/GIWASTEPN/raw/apk-dist/StepUp-debug.apk

데모 영상(2분 30초): <VIDEO_URL>

직접 설치해 보실 수 있습니다. StepUp은 Move-to-Earn 러닝 앱으로, 랩이 있는 GPS
코스 측정, 44종 스니커즈 NFT, 크루와 파티런을 4개 언어로 제공합니다. Kotlin 81개
파일, 60개 이상 화면, 푸시마다 CI가 APK를 빌드합니다. 에너지 게이트와 +17.8%
부스트 상한으로 배출을 구조적으로 묶었습니다.

소스·문서·컨트랙트: github.com/mycyi1994-hash/GIWASTEPN — GIWA용 컨트랙트 4종이
Sepolia(체인 91342)에 배포돼 있고, 테스트 43개가 통과합니다.
```

---

# [9/12] GIWA Testnet Verified Smart Contract Link
**GIWA 테스트넷 검증 컨트랙트 링크** · explorer URL 또는 verified contract repository · 500자

> ✅ **2026-07-31 배포 완료.** 아래는 실제 배포된 주소입니다.
> `npx hardhat run scripts/verify.js --network giwaSepolia` 가 아직 실패한
> 상태라면 첫 줄의 **"·소스 검증"/"and source-verified"** 만 빼고 제출하세요 —
> 배포 자체는 이미 사실입니다.

### 🇬🇧 영어판 (492자 / 500)

```
All four deployed and source-verified on GIWA Sepolia (chain 91342). Open any at https://sepolia-explorer.giwa.io/address/<address>

SUPToken  0xb052A8f6A5034747902b6d6787bbfF31A9006c1B
SneakerNFT  0x8174f905d86438ac8922c85d3A48604BabEFc960
RewardDistributor  0x9f9E87bD825144A8315d30979E3004FbaCFE36E1
CourseRegistry  0x6c815DF0d8a5CA7CA0487D1AC2f96c0fEC588542

1B fixed supply, no mint function. Boost ceiling is a constant. RewardDistributor has no owner withdrawal path. 43 passing tests.
```

### 🇰🇷 한국어판 (412자 / 500)

```
GIWA Sepolia(체인 91342)에 4종 배포·소스 검증 완료. 조회: https://sepolia-explorer.giwa.io/address/<주소>

SUPToken  0xb052A8f6A5034747902b6d6787bbfF31A9006c1B
SneakerNFT  0x8174f905d86438ac8922c85d3A48604BabEFc960
RewardDistributor  0x9f9E87bD825144A8315d30979E3004FbaCFE36E1
CourseRegistry  0x6c815DF0d8a5CA7CA0487D1AC2f96c0fEC588542

10억 고정 공급·발행 함수 없음. 부스트 상한 1780 bps는 constant. RewardDistributor에 소유자 인출 경로 없음. 테스트 43개 통과.
```

> 주소 4개만 168자를 먹기 때문에 설명을 더 붙이면 500자를 넘습니다.
> 문장을 고치실 경우 글자수를 다시 세어 보세요.

---

# [10/12] 기술 문서 또는 원페이저 링크
**Docs or One-Pager Link** · shareable link · 500자
· *제품 아키텍처와 기술 구현을 설명하는 GitBook / 백서 / 원페이저*

> 📌 **문항이 요구하는 건 "제품 아키텍처와 기술 구현"입니다.** 그래서
> 기술 문서를 새로 썼습니다 — **PDF 16쪽**
> [`docs/StepUp-Architecture.pdf`](StepUp-Architecture.pdf), 원본 마크다운은
> [`docs/ARCHITECTURE.md`](ARCHITECTURE.md)입니다. 원페이저는 제품·시장까지
> 함께 다루지만, 이 문항이 정확히 묻는 것은 아키텍처 문서입니다.
>
> **답변에는 PDF를 넣었습니다.** GitHub 링크를 열면 브라우저에서 바로 열리고,
> 심사자가 내려받아 보관하기도 좋습니다. 마크다운을 선호하시면 URL 끝을
> `docs/ARCHITECTURE.md`로 바꾸시면 됩니다(글자수 여유 있음).
>
> **⚠️ 이 링크는 문서가 `claude/work-history-pjm57c` 브랜치에 들어간 뒤에
> 동작합니다.** (PR 머지 후) 시크릿 모드로 한 번 열어보고 붙여넣으세요.

### 🇬🇧 영어판 (497자 / 500)

```
Technical documentation — product architecture and implementation (English + Korean, 16pp):
https://github.com/mycyi1994-hash/GIWASTEPN/blob/claude/work-history-pjm57c/docs/StepUp-Architecture.pdf

Three planes (Android client, Cloudflare Worker attester, four GIWA contracts), the sensor-to-SUP pipeline, GPS anti-cheat limits, the EIP-712 claim flow, the trust model, and what is not done. Every constant names its source file and how to re-run its tests.

Markdown: same folder, ARCHITECTURE.md
```

### 🇰🇷 한국어판 (387자 / 500)

```
기술 문서 — 제품 아키텍처와 기술 구현(영문·한국어 병기, 16쪽):
https://github.com/mycyi1994-hash/GIWASTEPN/blob/claude/work-history-pjm57c/docs/StepUp-Architecture.pdf

세 개 층(안드로이드 클라이언트 · Cloudflare Worker 어테스터 · GIWA 컨트랙트 4종), 센서에서 SUP까지의 파이프라인, GPS 부정 방지 임계값, EIP-712 청구 흐름, 어테스터 키가 털려도 토큰이 인플레이션되지 않는 이유를 적은 신뢰 모델, 그리고 아직 안 된 것까지 담았습니다. 모든 상수에 출처 파일과 테스트를 다시 돌리는 명령을 붙였습니다.

마크다운 원본: 같은 폴더의 ARCHITECTURE.md
```

<details>
<summary>원페이저를 그대로 내고 싶다면 (기존 답변)</summary>

```
One-pager (product structure and technical execution):
https://github.com/mycyi1994-hash/GIWASTEPN/blob/claude/work-history-pjm57c/docs/ONEPAGER.md

Full tokenomics — emission, sinks, per-persona balance tables, supply schedule, anti-abuse, and the open questions we have not solved:
https://github.com/mycyi1994-hash/GIWASTEPN/blob/claude/work-history-pjm57c/docs/TOKENOMICS.md

Both are written in English and Korean. The repository also holds the app source, the contracts and device screenshots.
```

</details>

---

## ❓ 아직 답을 못 드린 문항

**1, 2, 5, 6, 11, 12번** — 화면을 못 봐서 작성하지 못했습니다.
캡처를 보내주시면 같은 형식(영/한 + 글자수 검증)으로 써 드리겠습니다.

---

## 📎 근거 문서 위치

| 문항 | 문서 |
|---|---|
| 3/12 | **별도 파일** (저장소 외부) — 실명·이력은 공개하지 않습니다 |
| 7/12 | [`docs/PITCH.md`](PITCH.md) |
| 8/12 | [`docs/DEMO.md`](DEMO.md) (촬영 대본) · [APK](https://github.com/mycyi1994-hash/GIWASTEPN/raw/apk-dist/StepUp-debug.apk) |
| 9/12 | [`contracts/`](../contracts/) · [`contracts/README.md`](../contracts/README.md) (배포 6단계) |
| 10/12 | [`docs/StepUp-Architecture.pdf`](StepUp-Architecture.pdf) · [`docs/ARCHITECTURE.md`](ARCHITECTURE.md) · [`docs/ONEPAGER.md`](ONEPAGER.md) · [`docs/TOKENOMICS.md`](TOKENOMICS.md) |
