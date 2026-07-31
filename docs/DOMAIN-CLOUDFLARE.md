# 도메인 + 랜딩 페이지 — Cloudflare로 올리기

**소요 30~40분 · 비용 연 1만~1.5만원 (도메인 값만, 호스팅은 무료)**

지원서 **8/12 Project Link**에 GitHub 주소 대신 `stepup.run` 같은 자기 도메인을
넣기 위한 절차입니다.

---

## 먼저 — 이게 꼭 필요한가?

**필수는 아닙니다.** 8/12는 "working MVP 또는 데모 영상에 접근할 수 있는 URL"을
요구하고, GitHub APK 링크로도 통과합니다.

다만 도메인이 있으면 이런 차이가 납니다.

| | GitHub 링크만 | 자기 도메인 |
|---|---|---|
| 심사자 첫인상 | 저장소 파일 목록 | 제품 페이지 |
| 링크 길이 | 92자 | 20자 안팎 (500자 제한에 여유) |
| APK·문서·컨트랙트 | 링크 4개를 각각 | 한 페이지에서 전부 |
| 팀의 진지함 | 판단 어려움 | 도메인 값을 낸 팀 |

**랜딩 페이지는 이미 만들어 뒀습니다** — 저장소의 [`web/`](../web/) 폴더입니다.
APK 다운로드, 스크린샷 6장, 이코노미 표, 컨트랙트 4종, 그리고 "지금 어디까지
왔는가"를 정직하게 적은 현황이 한 페이지에 들어 있습니다. 이 문서는 그걸
인터넷에 올리는 방법입니다.

---

## 전체 그림

```
GitHub 저장소 (web/ 폴더)
        │  push 할 때마다 자동 배포
        ▼
Cloudflare Pages  →  stepup-run.pages.dev   (무료, 즉시)
        │  커스텀 도메인 연결
        ▼
   stepup.run     (도메인 값만 지불)
```

Cloudflare Pages는 **무료**입니다. 대역폭 무제한, SSL 인증서 자동. 돈이 드는 건
도메인 이름 하나뿐입니다.

---

# 1단계 · Cloudflare 계정 만들기 (3분)

1. https://dash.cloudflare.com/sign-up 접속
2. 이메일 + 비밀번호 입력 → **Sign Up**
3. 받은 메일의 인증 링크 클릭

> 신용카드는 이 단계에서 필요 없습니다. 도메인을 살 때만 넣습니다.

---

# 2단계 · 도메인 사기 (10분)

Cloudflare Registrar는 **원가로 팝니다** — 갱신할 때 값을 올리지 않습니다.
(다른 등록업체는 첫해 1천원, 갱신 3만원 같은 방식이 흔합니다.)

1. 대시보드 왼쪽 메뉴 → **Domain Registration** → **Register Domains**
2. 원하는 이름 검색

### 이름 고르기

| 후보 | 연 비용(대략) | 평 |
|---|---|---|
| `stepup.run` | 2만원대 | `.run` 이 러닝 앱과 맞아떨어짐. 제일 좋음 |
| `stepup.app` | 2만원대 | 앱이라는 게 바로 읽힘. `.app`은 HTTPS 강제라 안전 |
| `getstepup.com` | 1.5만원 | `.com`은 무난. 앞에 get을 붙여 확보 |
| `stepup.gg` | 4만원대 | 게임 느낌. 비쌈 |
| `stepup.kr` | 2만원 | 국내 심사에는 나쁘지 않음 |

> `stepup.com` 같은 짧은 `.com`은 이미 팔렸고 재판매가가 수천만 원입니다.
> **`.run` 이나 `.app`을 권합니다** — 뜻이 맞고 값이 정상입니다.

3. **Purchase** → 카드 정보 입력 → 결제
4. 결제하면 도메인이 **자동으로 Cloudflare DNS에 등록**됩니다.
   (다른 곳에서 산 도메인이라면 네임서버를 Cloudflare 것으로 바꿔야 합니다 —
   3단계 아래 "다른 곳에서 산 경우" 참고)

---

# 3단계 · Cloudflare Pages에 배포 (10분)

## 3-1. 저장소 연결

1. 대시보드 왼쪽 → **Workers & Pages** → **Create** → **Pages** 탭
2. **Connect to Git** 클릭
3. **GitHub** 선택 → 인증 → **mycyi1994-hash/GIWASTEPN** 선택 → **Begin setup**

> ⚠️ **저장소가 Public이어야 목록에 뜹니다.** 안 보이면 아직 비공개인 것이니
> [TODO-USER.md](TODO-USER.md) 3단계를 먼저 하세요.

## 3-2. 빌드 설정 — 여기가 중요합니다

| 항목 | 넣을 값 |
|---|---|
| **Project name** | `stepup` (주소가 `stepup.pages.dev`가 됩니다) |
| **Production branch** | `claude/work-history-pjm57c` |
| **Framework preset** | **None** |
| **Build command** | *(비워 둡니다)* |
| **Build output directory** | **`web`** ← 이걸 꼭 넣으세요 |

> 정적 HTML이라 빌드가 필요 없습니다. **Build command를 비워 두고**
> output directory만 `web`으로 지정하면 됩니다.

3. **Save and Deploy** → 1분쯤 기다리면 `https://stepup.pages.dev` 가 열립니다

이 시점에서 **이미 제출 가능한 링크**가 생겼습니다. 도메인 연결이 늦어져도
`pages.dev` 주소로 8/12를 채울 수 있습니다.

## 3-3. 커스텀 도메인 연결

1. 방금 만든 Pages 프로젝트 → **Custom domains** 탭 → **Set up a custom domain**
2. 도메인 입력 (예: `stepup.run`) → **Continue** → **Activate domain**
3. DNS 레코드가 **자동으로 만들어집니다.** 직접 입력할 필요 없습니다.
4. 1~5분 뒤 `https://stepup.run` 이 열립니다. SSL 인증서도 자동입니다.

### `www` 도 함께 잡으려면

같은 화면에서 **Set up a custom domain**을 한 번 더 눌러 `www.stepup.run` 을
추가하면 됩니다. Cloudflare가 알아서 리다이렉트를 붙입니다.

## 다른 곳에서 산 경우 (가비아·후이즈 등)

1. Cloudflare 대시보드 → **Add a domain** → 도메인 입력 → **Free** 플랜 선택
2. Cloudflare가 알려주는 **네임서버 2개**를 복사
   (예: `abby.ns.cloudflare.com`, `rick.ns.cloudflare.com`)
3. 산 곳의 관리 페이지에서 네임서버를 그 둘로 교체
4. 전파에 보통 몇 분~몇 시간. Cloudflare가 완료되면 메일로 알려줍니다
5. 그다음 3-3을 그대로 진행

---

# 4단계 · 확인 (2분)

**시크릿 모드**로 아래를 확인하세요.

- [ ] `https://stepup.run` 이 로그인 없이 열린다
- [ ] 주소창에 **자물쇠(🔒)** 가 보인다
- [ ] **Download the APK** 버튼을 누르면 실제로 받아진다
- [ ] 폰에서도 열어본다 (레이아웃이 세로에서 깨지지 않는지)

---

# 5단계 · 8/12 답변 교체

도메인이 생기면 링크가 짧아져 본문에 쓸 글자가 넉넉해집니다.

```
Working MVP: https://stepup.run

Install the APK from that page and try it yourself. StepUp is a Move-to-Earn
running app: GPS course tracking with laps and speed verification, 44 sneaker
NFTs, crews and party runs, in 4 languages. 76 Kotlin files, 60+ screens,
CI-built APK on every push.

Demo video (2m30s): <VIDEO_URL>

Source, docs and contracts: github.com/mycyi1994-hash/GIWASTEPN — four GIWA
contracts written, 60 tests passing. Sepolia deployment is next.
```

> 붙여넣은 뒤 폼의 글자 수 표시가 500을 넘지 않는지 확인하세요.
> `stepup.run` 이 아닌 다른 도메인이면 길이가 달라집니다.

**10/12(문서 링크)도** `https://stepup.run/#economy` 처럼 앵커를 걸어 쓸 수
있지만, 그 문항은 원문 문서를 보여주는 게 나으므로 GitHub 링크를 유지하시길
권합니다.

---

# 이후 관리

- **페이지 수정** — `web/index.html`을 고쳐 push하면 Cloudflare가 자동 재배포합니다.
  따로 할 일이 없습니다.
- **APK를 도메인에서 직접 받게 하려면** — `web/` 안에 APK를 넣으면 됩니다.
  다만 Pages는 파일 하나에 25MB 제한이 있고 현재 APK가 19MB라 가능은 합니다.
  지금은 GitHub 링크를 쓰고 있어 저장소 용량을 아낍니다.
- **비용** — Pages는 무료 유지. 도메인만 매년 자동 갱신됩니다.

---

# 막혔을 때

| 증상 | 원인 / 해결 |
|---|---|
| Pages에서 저장소가 안 보임 | 저장소가 비공개 → 공개 전환 후 Cloudflare 앱 권한 재확인 |
| 배포는 됐는데 404 | **Build output directory**가 `web`이 아님 → 프로젝트 Settings → Builds에서 수정 후 재배포 |
| 스크린샷이 안 보임 | `web/assets/` 가 push 안 됨 → `git status`로 확인 |
| 도메인이 안 열림 | DNS 전파 대기 중. 최대 몇 시간. `dig stepup.run` 으로 확인 |
| SSL 오류 | 인증서 발급 대기(보통 5분 이내). 그 뒤에도 나면 Custom domains에서 도메인 제거 후 재추가 |
| 갱신 요금이 걱정 | Cloudflare Registrar는 원가 판매라 갱신가가 뛰지 않습니다 |

---

<div align="center">

**결론:** 도메인은 선택이지만, `pages.dev` 배포는 **10분이고 공짜**라
그것만이라도 하시길 권합니다.

</div>
