# StrideUp — GIWA STEPN (M2E 러닝 리워드 앱)

달릴수록 **SUP 포인트(StrideTokens)**가 쌓이는 Move-to-Earn(M2E) 안드로이드 앱입니다.
StepN 스타일의 에너지/스니커즈 경제 모델을 채택했으며, 적립된 SUP는 추후
**GIWA 체인** 온체인 토큰으로 전환하는 것을 목표로 합니다.

## 디자인 — Volt (Black × Neon Lime)

퓨어 블랙 캔버스 + 네온 라임 단일 강조의 테크·크립토 무드.
몽글몽글함(토스 감성)은 색이 아니라 **큰 곡률·소프트 글로우·여백**에서 만듭니다.

| 축 | 규칙 |
|---|---|
| **색** | 딥 블랙(`#060708`) + 볼트 라임(`#C3FF3E`) 단일 강조. 채도 경쟁자를 두지 않는다 |
| **획** | 큰 숫자·제목은 ExtraBold/Black + 타이트한 자간, 소형 라벨은 넓은 자간 대문자 |
| **면** | 카본 카드(12~32dp 곡률) + 헤어라인. 강조 카드만 볼트 외곽선 |

시그니처 요소: 헥사곤 토큰 엠블럼(`HexEmblem`), 네온 진행 링(`NeonRing`),
글로우 루트 맵(`RouteMap`), START RUN 볼트 CTA, 육각 레벨 배지 아바타,
이탤릭 블랙 워드마크(**Stride**+<span>Up</span>).

## 화면 구성 (5탭)

| 탭 | 내용 |
|---|---|
| 홈 | 인사말+레벨 아바타, StrideTokens 카드, 오늘 걸음 히어로(루트 맵+목표 진행), 에너지 링(자정 리필 카운트다운), 주간 거리 차트, 스니커즈(Apex Runner) 능력치, **START RUN** |
| 커뮤니티 | 크루/러너/이벤트 검색, 근처 러닝 크루, 오늘 밤 파티 런(참여 토글), 인기 게시글, 러너 모집, 활동 피드 |
| 아이템 | 내 장비(스니커즈 업그레이드 — 실제 SUP 차감), 스니커즈 보관함(해금 레벨), 부스트 상점 |
| 이벤트 | Neon Horizon 캠페인(실시간 카운트다운), 주간 챌린지 Step Surge(**실제 주간 걸음 연동**), 미션/한정 퀘스트, 유형별 필터 |
| 프로필 | 레벨/XP 진행, 누적 거리·걸음·스트릭·업적(실데이터 기반 해금), 이번 달 요약, 일일 목표 설정, 계정·설정 |

추가 화면: **러닝 세션**(START RUN → 포그라운드 추적, 일시정지/잠금/정산),
**지갑**(SUP 잔액·적립/사용 요약·원장·GIWA 연동 준비).

## 다국어

한국어 · English · 简体中文 · 日本語 · Español — 시스템 언어를 따라가며,
Android 13+에서는 앱별 언어 설정(`localeConfig`)도 지원합니다.

## M2E 경제 모델 (`RewardEconomy`)

- **적립**: 러닝 세션 중 걸음에 대해서만 1보당 `0.01 SUP × 스니커즈 배율`
- **에너지**: 1칸 = 600보 적립 가능, 매일 자정 리필. 소진 시 적립 중단
- **스니커즈**: 레벨업 시 배율 +0.15, 최대 에너지 +2 (비용 = 레벨 × 100 SUP)
- **일일 목표 보너스**: 달성 시 20 SUP + 연속 달성일당 10% 가산(최대 7일)

## 기술 스택

- **UI**: Jetpack Compose + Material 3 (Volt 다크 테마), Navigation Compose
- **걸음 측정**: `TYPE_STEP_COUNTER` 센서 (자정/재부팅 기준점 보정)
- **세션 추적**: 포그라운드 서비스(`health` 타입) + 알림
- **저장소**: Room(일별 기록·세션·리워드 원장), DataStore(목표·에너지·스트릭)
- **아키텍처**: MVVM + Repository, 수동 DI(ServiceLocator)

```
app/src/main/java/com/giwa/strideup/
├── core/ServiceLocator.kt          # 수동 DI
├── data/                           # Room · DataStore · 리포지토리
├── domain/RewardEconomy.kt         # M2E 경제 모델 (단위 테스트 포함)
├── sensor/StepTracker.kt           # 걸음 센서 래퍼
├── service/WalkSessionService.kt   # 러닝 세션 포그라운드 서비스
└── ui/
    ├── theme/                      # Volt 팔레트 · 브러시 · 타이포 · 곡률
    ├── components/                 # HexEmblem, NeonRing, GlowCard, StartRunButton 등
    └── screens/                    # home · community · items · events · profile · walk(run) · rewards(wallet)
```

## 빌드 방법

1. Android Studio (Ladybug 이상 권장)에서 프로젝트 열기
2. 자동으로 Gradle 동기화 (AGP 8.7.3 / Kotlin 2.0.21 / compileSdk 35)
3. 실기기 또는 API 26+ 에뮬레이터에서 실행

```bash
./gradlew :app:assembleDebug        # 디버그 APK 빌드
./gradlew :app:testDebugUnitTest    # 경제 모델 단위 테스트
```

### CI로 빌드된 APK 받기

`claude/work-history-pjm57c` 브랜치에 푸시될 때마다 GitHub Actions(`Build APK`)가
디버그 APK를 빌드해 두 곳에 올립니다.

- **Actions 아티팩트**: 워크플로 실행 페이지의 `StrideUp-debug-apk`
- **`apk-dist` 브랜치**: 최신 `StrideUp-debug.apk` 파일이 커밋됨

### 고정 debug 키스토어 (`app/debug.keystore`)

CI 러너는 실행마다 새로 생성되므로, AGP가 자동 생성하는 debug 키스토어에
맡기면 **빌드마다 APK 서명이 달라집니다.** 그러면 기기에 이미 설치된 앱 위에
새 APK를 덮어쓸 때 `INSTALL_FAILED_UPDATE_INCOMPATIBLE`이 발생해
"앱이 설치되지 않았습니다"로 실패합니다.

이를 막기 위해 고정 키스토어를 저장소에 포함하고 `signingConfigs.debug`에
연결했습니다. 표준 Android debug 키와 동일한 성격의 공개 키이며
(비밀번호 `android`), **배포용 서명 키가 아닙니다.** Play 스토어 출시에는
별도의 release 키스토어를 CI 시크릿으로 주입해야 합니다.

CI는 매 빌드마다 APK 서명 지문이 이 키스토어와 일치하는지 검증합니다.

### 에뮬레이터에서 체험하기

에뮬레이터에는 걸음 센서가 없는 경우가 많습니다. 디버그 빌드의 **러닝 화면 →
"+100 걸음 (디버그)"** 버튼으로 적립 흐름을 체험할 수 있습니다.

## 로드맵

- [ ] GIWA 체인 지갑 연동 및 SUP 온체인 전환/출금
- [ ] 스니커즈 NFT (희귀도/속성) 시스템 및 마켓
- [ ] 커뮤니티 크루·파티 런 실서비스 연동
- [ ] Health Connect 연동
