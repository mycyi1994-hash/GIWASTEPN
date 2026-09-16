<div align="center">

# 직접 하셔야 하는 것 — 출시까지

**2026-09-16 기준 · 앱 v1.15.1 · [정식 출시 계획](LAUNCH-PLAN.md) Phase 0 진행 중**

코드로 되는 일은 제가 합니다. 이 문서에는 **계정·결제·법률처럼 본인 확인이 필요해
제가 대신할 수 없는 일**만 적습니다.

</div>

---

## 지금 해야 하는 것 (Phase 0)

| 순서 | 할 일 | 소요 | 이걸 안 하면 |
|:--:|---|:--:|---|
| **1** | [컨트랙트 소스 검증](#1-컨트랙트-소스-검증) | 10분 | 익스플로러에서 코드를 못 봄 — "배포했다"는 주장만 남음 |
| **2** | [어테스터 배포](#2-어테스터-배포-cloudflare) | 15분 | 러닝 증명 서명이 안 됨 → Phase 2 전체가 막힘 |
| **3** | [Google Play 개발자 등록](#3-google-play-개발자-등록) | 30분 + 심사 최대 2일 | 스토어 출시 자체가 불가 |
| **4** | [업로드 키 생성 + GitHub Secrets 등록](#4-업로드-키-생성) | 20분 | CI가 릴리즈 AAB를 못 만듦 |
| **5** | [법률 검토 착수](#5-법률-검토-착수) | 상담 예약 | 출시 국가·출금 허용 여부를 못 정함 |

> **1·2번이 가장 급합니다.** 나머지 개발(Phase 1)은 3·4·5번 없이도 진행됩니다.

---

## ✅ 이미 끝난 것

| 항목 | 확인 |
|---|---|
| GitHub 저장소 **공개 전환** | `github.com/mycyi1994-hash/GIWASTEPN` 누구나 열림 |
| **기본 브랜치** 정리 | `claude/work-history-pjm57c` |
| **컨트랙트 배포** (GIWA Sepolia 91342) | 4종 배포 + 리워드 풀 5,000만 SUP — [기록](../contracts/deployments/giwaSepolia.json) |
| APK 자동 빌드·배포 | 푸시마다 `apk-dist` 브랜치 갱신 |

---

# 1. 컨트랙트 소스 검증

배포는 끝났지만 익스플로러에 **소스 코드가 안 올라가 있습니다.** 지금은 바이트코드만
보이는 상태라, 보는 사람이 컨트랙트가 무슨 일을 하는지 확인할 수 없습니다.

> 💻 **PC에서 터미널**이 필요합니다. (제 작업 환경에서는 GIWA 익스플로러로
> 나가는 네트워크가 막혀 있어 대신 실행할 수 없습니다.)

**Windows (명령 프롬프트)**

```bat
cd C:\stepup
git clone -b claude/work-history-pjm57c https://github.com/mycyi1994-hash/GIWASTEPN.git work
cd C:\stepup\work\contracts
npm install
npm run verify:giwa
```

**macOS · Linux**

```bash
git clone -b claude/work-history-pjm57c https://github.com/mycyi1994-hash/GIWASTEPN.git work
cd work/contracts
npm install
npm run verify:giwa
```

- **개인키가 필요 없습니다.** 검증은 읽기 작업이고, GIWA 익스플로러는 Blockscout이라
  API 키도 필요 없습니다.
- 성공하면 컨트랙트 4종의 익스플로러 링크가 출력됩니다. 각 주소 페이지에 **`Code` 탭**이
  생기면 끝난 겁니다.
### Node.js 없이 하는 방법 (터미널이 부담되시면 이쪽)

익스플로러 웹 화면에 파일만 올리면 되도록 **미리 뽑아 뒀습니다.**
[`contracts/verification-giwa-sepolia/`](../contracts/verification-giwa-sepolia/) 폴더의
README를 그대로 따라 하시면 됩니다 — 컨트랙트 4개, 각각 2분쯤 걸립니다.
GitHub에서 JSON 파일을 내려받아 업로드하고, 적어 둔 생성자 인자를 붙여넣는 게 전부입니다.

**막히면 터미널에 나온 에러 메시지를 그대로 보내주세요.**

---

# 2. 어테스터 배포 (Cloudflare)

러닝이 진짜인지 판정하고 서명하는 서비스입니다. 코드와 테스트는 끝났고 **배포만**
남았습니다. 무료 티어로 하루 10만 요청까지 됩니다.

### 2-1. 서명 전용 지갑 만들기

**배포에 쓴 지갑을 재사용하지 마세요.** MetaMask에서 새 계정을 하나 더 만듭니다.

1. MetaMask → 계정 목록 → **계정 추가** → **새 계정 추가**
2. 이름을 "StepUp 어테스터"로 지정
3. **계정 세부 정보 → 개인 키 표시** → 복사

> 🔐 **이 개인키를 저에게 보내지 마세요.** 채팅·스크린샷·이슈 전부 포함입니다.
> Cloudflare 시크릿에만 들어갑니다.

### 2-2. 배포

```bash
cd work/attester
npm install
npx wrangler login          # 브라우저가 열립니다
npx wrangler secret put ATTESTER_PRIVATE_KEY
# 프롬프트에 2-1에서 복사한 키를 붙여넣기 (화면에 안 보이는 게 정상입니다)
npx wrangler deploy
```

배포되면 `https://stepup-attester.<계정>.workers.dev` 주소가 나옵니다.

### 2-3. 확인

```bash
curl https://stepup-attester.<계정>.workers.dev/health
```

`"ok": true` 와 어테스터 주소가 나오면 성공입니다. **그 주소를 저에게 알려주세요**
(주소는 공개 정보라 안전합니다 — 개인키가 아닙니다).

### 2-4. 컨트랙트에 어테스터 등록

배포 지갑으로 `RewardDistributor.setAttester(<2-3의 주소>)` 를 호출해야 서명이
받아들여집니다. 2-3 주소를 주시면 실행 스크립트를 만들어 드리겠습니다.

---

# 3. Google Play 개발자 등록

1. `https://play.google.com/console` 접속 → Google 계정으로 로그인
2. **개발자 계정 만들기** → 개인 또는 사업자 선택
   - **개인 계정은 출시 전 20명 테스터 × 14일 비공개 테스트가 의무**입니다 (2023년 정책).
     Phase 3 클로즈드 베타가 이 요건을 겸합니다.
   - 사업자 계정은 이 요건이 없지만 사업자등록번호·D-U-N-S 번호가 필요합니다.
3. **등록비 $25** (1회, 평생) 카드 결제
4. 신원 확인 — 신분증 제출, 심사 최대 2일
5. 승인되면 **앱 만들기** → 이름 `StepUp`, 기본 언어 한국어, 무료 앱

> 💡 3번은 지금 시작해 두시는 게 좋습니다. 심사에 며칠 걸리고, 그동안 개발은 계속됩니다.

---

# 4. 업로드 키 생성

CI가 릴리즈 AAB에 서명하려면 키가 필요합니다. **이 키를 잃어버리면 앱 업데이트를
영원히 못 올립니다.** 반드시 백업하세요.

### 4-1. 키 만들기

```bash
keytool -genkeypair -v \
  -keystore stepup-upload.jks \
  -keyalg RSA -keysize 4096 -validity 10000 \
  -alias stepup-upload
```

비밀번호를 묻습니다. **비밀번호와 `stepup-upload.jks` 파일을 비밀번호 관리자에
백업**하세요. 저장소에는 절대 커밋하지 않습니다.

### 4-2. GitHub Secrets에 등록

키 파일을 base64로 바꿉니다.

```bash
# macOS · Linux
base64 -i stepup-upload.jks | tr -d '\n' > keystore.b64
# Windows PowerShell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("stepup-upload.jks")) > keystore.b64
```

`github.com/mycyi1994-hash/GIWASTEPN` → **Settings → Secrets and variables → Actions**
→ **New repository secret** 로 4개를 등록합니다.

| 이름 | 값 |
|---|---|
| `RELEASE_KEYSTORE_BASE64` | `keystore.b64` 파일 내용 전체 |
| `RELEASE_KEYSTORE_PASSWORD` | 4-1의 키스토어 비밀번호 |
| `RELEASE_KEY_ALIAS` | `stepup-upload` |
| `RELEASE_KEY_PASSWORD` | 4-1의 키 비밀번호 (같게 했다면 동일) |

등록하면 CI가 자동으로 서명된 AAB를 만듭니다. **시크릿이 없으면 그 단계는 조용히
건너뛰므로**, 지금 당장 안 하셔도 다른 빌드는 정상 동작합니다.

> Play Console에서 **Play App Signing**을 켜면 구글이 배포 키를 따로 관리합니다.
> 이 업로드 키는 "구글에 올릴 때 쓰는 열쇠"일 뿐이라, 최악의 경우 재발급이 가능합니다.

---

# 5. 법률 검토 착수

**가장 먼저 답이 나와야 하는 질문입니다.** 답에 따라 Phase 2 이후 설계가 바뀝니다.

### 변호사에게 물어볼 것

1. **SUP 토큰이 국내에서 어떤 지위인가** — 가상자산이용자보호법 적용 대상인지
2. **P2E 판정 리스크** — 걸음으로 토큰을 벌고 NFT 뽑기가 있는 피트니스 앱이
   게임산업법상 게임물로 분류될 수 있는지. 분류되면 국내 출금은 사실상 불가합니다.
3. **위치기반서비스사업 신고** — GPS 러닝 기록 수집이 신고 대상인지
4. **1차 출시 국가를 어디로 해야 하는가** — 한국 / 일본 / EU(MiCA) / 미국은 취급이 전부 다릅니다

### 검토 결과에 따른 갈림길

| 결론 | 대응 |
|---|---|
| 국내 출금 가능 | 계획대로 진행 |
| 국내 출금 불가 | **투트랙** — 국내는 출금 없는 빌드(지금 구조 그대로 완결됨), 허용 국가부터 온체인 활성화 |

앱이 이미 "지갑 없이도 완전히 플레이되는" 구조라 두 번째 결론이 나와도 출시가
막히지는 않습니다. **다만 그 사실을 알고 설계해야 하므로 지금 물어보셔야 합니다.**

> ⚖️ 저는 변호사가 아니고, 이 문서의 어떤 내용도 법률 자문이 아닙니다.
> 블록체인·핀테크를 다뤄 본 로펌에 상담하세요.

---

# ❓ 막혔을 때

| 증상 | 원인 / 해결 |
|---|---|
| `npm run verify:giwa` 실패 | 에러 메시지 그대로 보내주세요. 수동 업로드 경로는 `npm run verify:blockscout` |
| `wrangler login` 이 안 열림 | `npx wrangler login --browser=false` 로 나온 URL을 직접 붙여넣기 |
| `/health` 가 `"ok": false` | 시크릿 등록 누락 → 2-2의 `wrangler secret put` 다시 |
| APK "앱이 설치되지 않았습니다" | 다른 서명 버전이 이미 설치됨 → 기존 StepUp 삭제 후 재설치 |
| Play Console 신원 확인 반려 | 신분증 사진의 네 모서리가 다 보이게, 빛 반사 없이 재촬영 |
| GitHub Actions 빨간불 | Actions 탭 → 실패한 잡 → 로그를 그대로 보내주세요 |

**에러가 나면 메시지를 그대로 복사해서 보내주세요.** 단, **개인키가 들어간 줄은 빼고**
보내주셔야 합니다.

---

<div align="center">

전체 계획 · [정식 출시 계획](LAUNCH-PLAN.md) · [컨트랙트 가이드](../contracts/README.md) · [어테스터](../attester/README.md)

</div>
