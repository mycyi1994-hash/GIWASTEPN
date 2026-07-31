<div align="center">

# 지금 직접 하셔야 하는 것 — 순서대로

**2026-07-31 기준 · 앱 v1.13.0**

앞의 것을 끝내야 뒤의 것이 됩니다. **위에서부터 하나씩** 내려오세요.

</div>

---

## 한눈에 보기

| 순서 | 할 일 | 소요 | 이걸 안 하면 |
|:--:|---|:--:|---|
| **1** | GitHub 로그인 | 3분 | 아무것도 못 함 |
| **2** | 기본 브랜치 변경 | 1분 | 심사자가 **11일 전 옛 앱**을 봄 |
| **3** | 저장소 공개 전환 | 2분 | **3·7·8·9·10번이 전부 404** |
| **4** | 공개 확인 (시크릿 모드) | 1분 | 깨진 링크를 제출하게 됨 |
| **5** | 답변 복붙 (3·4·7·8·9·10번) | 10분 | — |
| ~~**6**~~ | ~~컨트랙트 배포~~ ✅ **2026-07-31 완료** · 남은 건 소스 검증 | 5분 | 9번 링크에 `#code`가 안 붙음 |
| **7** | 데모 영상 촬영 | 2~3시간 | 8번이 APK 링크만 남음 |
| **8** | 나머지 문항(1·2·5·6·11·12) 캡처 전달 | 5분 | 제출 완료 불가 |

> **1~5번(약 17분)만 끝내면 6개 문항을 제출할 수 있습니다.**
> 6·7번은 점수를 올리는 작업이고, 8번은 저에게 넘기시면 됩니다.

---

# 1단계 · GitHub 로그인

전에 404가 떴던 건 저장소가 없어서가 아니라, **비공개 저장소를 로그인 없이 열어서**입니다.
GitHub은 권한 없는 사람에게 "권한 없음" 대신 일부러 404를 보여줍니다.

1. 주소창에 **`github.com/login`** 입력
2. **Username or email**: `mycyi1994-hash` 또는 가입 이메일
3. **Password** 입력 → **Sign in**
4. 화면 **오른쪽 맨 위 동그란 아바타** 클릭 → 맨 위에 **`mycyi1994-hash`** 가 보이는지 확인

**비밀번호를 모르시면** → 로그인 화면의 **Forgot password?** → 이메일 입력 → 메일로 온 링크에서 재설정

**다른 아이디로 로그인되어 있으면** → 그 계정에서 **Sign out** 하고 다시 1번부터

✅ **확인:** `https://github.com/mycyi1994-hash/GIWASTEPN` 이 열리면 성공

---

# 2단계 · 기본 브랜치 변경

> ⚠️ **이게 3단계보다 먼저입니다.** 지금 기본 브랜치가
> `claude/android-app-development-rq17u9` 로 잡혀 있는데, **7월 20일에 멈춘 옛날 코드**입니다.
> 이대로 공개하면 심사자가 저장소를 열었을 때 옛 README와 옛 앱을 봅니다.

1. `https://github.com/mycyi1994-hash/GIWASTEPN` 접속
2. 상단 **⚙ Settings** 탭 클릭
3. 왼쪽 메뉴 **General** (기본 선택되어 있음)
4. 스크롤 내려 **Default branch** 항목 찾기
5. 오른쪽 **⇄** (두 화살표) 아이콘 클릭
6. 드롭다운에서 **`claude/work-history-pjm57c`** 선택
7. **Update** → 경고창에서 **I understand, update the default branch**

✅ **확인:** 저장소 첫 화면 좌측 상단 브랜치 버튼이 `claude/work-history-pjm57c` 로 바뀜

---

# 3단계 · 저장소 공개 전환

1. 같은 **Settings → General** 화면에서 **맨 아래까지** 스크롤
2. 빨간 테두리 **Danger Zone** 박스
3. **Change repository visibility** 줄의 **Change visibility** 버튼
4. **Make public** 선택 → **I want to make this repository public**
5. 확인창에 **`mycyi1994-hash/GIWASTEPN`** 을 그대로 입력
6. **I understand, make this repository public**

### 공개해도 위험하지 않은 이유

| 걱정 | 실제 |
|---|---|
| `app/debug.keystore` 가 커밋되어 있는데? | 안드로이드 **표준 디버그 키**(비밀번호 `android`)입니다. 배포 권한이 없고, README에 그 이유를 적어놨습니다 |
| API 키·개인키가 들어있나? | **하나도 없습니다.** 배포용 개인키는 `.env`에만 들어가고 `.gitignore` 처리되어 있습니다 |
| 코드를 베껴가면? | 그랜트 심사는 **공개 저장소를 요구합니다.** 비공개면 심사 자체가 안 됩니다 |

---

# 4단계 · 공개 확인 (건너뛰지 마세요)

**시크릿 모드**(Ctrl+Shift+N / Mac은 Cmd+Shift+N)로 아래 3개를 엽니다.
로그인 안 된 상태에서 열려야 심사자도 볼 수 있습니다.

| 확인 | 주소 | 성공 기준 |
|---|---|---|
| 저장소 | `github.com/mycyi1994-hash/GIWASTEPN` | **StepUp** 제목의 README + 스크린샷 6장 |
| APK | `github.com/mycyi1994-hash/GIWASTEPN/raw/apk-dist/StepUp-debug.apk` | 다운로드 시작 |
| 문서 | `github.com/mycyi1994-hash/GIWASTEPN/blob/claude/work-history-pjm57c/docs/ONEPAGER.md` | 문서가 보임 |

하나라도 404면 **2~3단계를 다시** 하세요.

---

# 5단계 · 답변 복붙

**[`docs/APPLICATION.md`](APPLICATION.md)** 를 열어두고 회색 블록을 그대로 복사하시면 됩니다.

| 문항 | 어떤 답 | 비고 |
|---|---|---|
| **3/12** Team Introduction | **직접 만든 팀 소개 파일 링크** (구글 드라이브 등) | 실명은 저장소에 안 올립니다 |
| **4/12** Motivation for Applying | 지원 동기 본문 | 영어 499자 |
| **7/12** Pitch Deck | `PITCH.md` 링크 | |
| **8/12** Project Link | APK 링크 (+영상) | **영상 없으면 그 줄만 지우세요** |
| **9/12** Verified Contract | 소스 저장소 + 43 테스트 | 6단계 후 교체 |
| **10/12** Docs / One-Pager | `ONEPAGER.md` + `TOKENOMICS.md` | |

**영어판과 한국어판이 둘 다 있습니다. 영어판을 권합니다** — 폼 자체가 영어이고 심사자가 한국어를 못 읽을 수 있습니다.

> 💡 붙여넣은 뒤 폼 우측 아래 **글자 수 표시(예: 496/500)** 가 빨갛지 않은지 확인하세요.
> 전부 500자 안에 맞춰뒀지만, 임의로 문장을 더하시면 넘칠 수 있습니다.

---

# 6단계 · 컨트랙트 배포 + 검증

> 여기서부터는 **PC에서 터미널**을 쓰셔야 합니다. 9번 문항 점수를 크게 올립니다.
> 자세한 안내: **[`contracts/README.md`](../contracts/README.md)**

### 6-1. 배포 전용 지갑 만들기

**기존 지갑을 쓰지 마세요.** MetaMask에서 새 계정을 만듭니다.

1. MetaMask → 계정 목록 → **계정 추가** → **새 계정 추가**
2. 이름을 "StepUp 배포용" 으로 지정
3. 계정 메뉴 **⋮** → **계정 세부 정보** → **개인 키 표시** → 비밀번호 입력 → 복사

> 🔐 **이 개인키를 저에게 보내지 마세요.** 채팅·스크린샷·이슈 전부 포함입니다.
> 다음 단계에서 만들 `.env` 파일에만 들어갑니다.

### 6-2. `.env` 만들기

> ⚠️ **`-b` 를 빼먹으면 안 됩니다.** 저장소 기본 브랜치는 아직 옛 버전이라
> `contracts/` 폴더가 없습니다. 그냥 `git clone` 하면 다음 줄에서
> "지정된 경로를 찾을 수 없습니다" 가 납니다.
> (0단계에서 기본 브랜치를 바꾸면 이 주의사항은 없어집니다.)

**Windows (명령 프롬프트)**

```bat
cd C:\stepup
git clone -b claude/work-history-pjm57c https://github.com/mycyi1994-hash/GIWASTEPN.git work
cd C:\stepup\work\contracts
copy .env.example .env
notepad .env
```

**macOS · Linux**

```bash
git clone -b claude/work-history-pjm57c https://github.com/mycyi1994-hash/GIWASTEPN.git work
cd work/contracts
cp .env.example .env
```

`.env` 를 메모장으로 열어 `DEPLOYER_PRIVATE_KEY=` 뒤에 복사한 키를 붙여넣고 저장합니다.
나머지 항목은 그대로 두세요.

### 6-3. 가스비 받기

1. **네트워크 추가** — `https://chainlist.org/chain/91342` 접속 →
   **Connect Wallet** → **Add to MetaMask**
2. **테스트 ETH 받기** — `https://faucet.giwa.io` 에서 6-1의 지갑 주소 입력
3. MetaMask에서 잔액이 **0보다 큰지** 확인

> 파우셋이 Ethereum Sepolia ETH를 요구하면, 공개 Sepolia 파우셋에서 먼저 받은 뒤
> `https://bridge.giwa.io` 로 GIWA Sepolia에 브리지하세요.

### 6-4. 배포

```bash
npm install
npm run deploy:giwa
```

성공하면 컨트랙트 주소 4개가 나오고, 리워드 풀에 5,000만 SUP가 들어갑니다.

**가스가 없으면** 이렇게 멈춥니다 → 6-3으로 돌아가세요.
```
Error: 배포 계정에 가스가 없습니다. https://faucet.giwa.io 에서 테스트 ETH를 받으세요.
```

### 6-5. 소스 검증

```bash
npm run verify:giwa
```

마지막에 **9번 문항에 붙여넣을 링크 4개**가 출력됩니다.
GIWA 익스플로러는 Blockscout이라 **API 키가 필요 없습니다.**

### 6-6. 배포 기록 커밋

```bash
cd ..
git add contracts/deployments/giwaSepolia.json
git commit -m "chore: record GIWA Sepolia deployment"
git push
```

### 6-7. 9번 답변 교체

`docs/APPLICATION.md` 맨 아래 **"배포가 끝나면 9/12를 이걸로 교체하세요"** 템플릿에
주소 4개를 넣어 다시 붙여넣습니다.

> ⚠️ 주소 4개만 약 270자를 먹습니다. **글자 수를 꼭 다시 확인**하세요.

**막히면 에러 메시지만 복사해서 보내주세요.** 개인키는 빼고요.

---

# 7단계 · 데모 영상 촬영

전체 대본: **[`docs/DEMO.md`](DEMO.md)** — 컷 9개, 2분 30초, 자막만(내레이션 불필요)

### 핵심만 요약

| | 내용 |
|---|---|
| **가장 중요한 것** | **실외에서 걸으면서** GPS 경로가 그려지는 장면. 이게 목업으로 위조 불가능한 유일한 증거입니다 |
| 촬영 방법 | 안드로이드 **화면 녹화** (빠른 설정 패널 → 화면 녹화 타일) |
| 나눠 찍기 | **A세션**: 실외 5~8분 걷기 / **B세션**: 실내에서 나머지 화면 |
| 편집 | CapCut 또는 VLLO (무료). 자막만 얹으면 됩니다 |
| 업로드 | YouTube → **공개 상태 `일부 공개(Unlisted)`** ← `비공개`는 절대 안 됩니다 |

### 촬영 전 반드시

- [ ] **방해 금지 모드 ON** (알림이 뜨면 다시 찍어야 합니다)
- [ ] **스니커즈 1개 민팅 + 코스 1개 저장** (빈 계정이면 전부 0이라 초라합니다)
- [ ] 화면 밝기 최대, 배터리 50% 이상
- [ ] 개활지에서 촬영 (건물 사이 ❌ — GPS가 안 잡힙니다)

### 마지막 카드 문구 (그대로 쓰세요)

```
✅ App: shipped, installable APK
✅ Contracts: written, 43 tests passing
⏳ Next: deploy on GIWA Sepolia (chain 91342)
```

> ⚠️ **"Live on GIWA" 같은 문구는 쓰지 마세요.** 심사자가 익스플로러에서
> 확인하는 순간 프로젝트 전체 신뢰가 무너집니다.

**시간이 없으면 60초 축약본**도 `DEMO.md`에 있습니다. 축약본으로도 8번은 통과합니다.

---

# 8단계 · 나머지 문항 캡처 보내주기

아직 **1, 2, 5, 6, 11, 12번**은 화면을 못 봐서 못 썼습니다.
폼에서 그 화면들을 캡처해서 보내주시면, 지금까지와 같은 형식으로 써 드립니다.

- 영어판 + 한국어판
- 글자 수 검증 (제한의 75~100%)

---

# ❓ 막혔을 때

| 증상 | 원인 / 해결 |
|---|---|
| GitHub 404 | 로그인 안 됨 또는 아직 비공개 → 1~3단계 |
| 시크릿 모드에서만 404 | 아직 비공개 → 3단계 |
| 저장소는 열리는데 옛날 화면 | 기본 브랜치 안 바꿈 → 2단계 |
| 문서 링크 404 | 브랜치 이름이 바뀜 → 링크의 `claude/work-history-pjm57c` 부분 확인 |
| `npm run deploy:giwa` 가스 에러 | 파우셋에서 ETH 못 받음 → 6-3 |
| APK "앱이 설치되지 않았습니다" | 다른 서명 버전이 이미 설치됨 → 기존 StepUp 삭제 후 재설치 |
| 폼 글자 수 초과 | 문장을 더하셨을 가능성 → `APPLICATION.md` 원문 그대로 사용 |

**에러가 나면 메시지를 그대로 복사해서 보내주세요.** 단, **개인키가 들어간 줄은 빼고** 보내주셔야 합니다.

---

<div align="center">

**1~5단계 = 약 17분.** 거기까지만 하셔도 6개 문항이 제출 가능합니다.

</div>
