# StrideUp — GIWA STEPN (M2E 걷기 리워드 앱)

걸을수록 **SUP 포인트**가 쌓이는 Move-to-Earn(M2E) 안드로이드 앱입니다.
StepN 스타일의 에너지/스니커즈 경제 모델을 채택했으며, 적립된 SUP는 추후
**GIWA 체인** 온체인 토큰으로 전환하는 것을 목표로 합니다.

> 이 저장소는 StrideUp 데모(HTML 시안)와 M2E 기획 문서를 참고해 만든
> 네이티브 안드로이드 구현입니다.

## 디자인 — Obsidian & Champagne

네온 게이밍 톤을 걷어내고 하이엔드 워치·주얼리 브랜드의 문법으로 재구성한
프리미엄 테마입니다. 규칙은 세 가지입니다.

| 축 | 규칙 |
|---|---|
| **색** | 흑요석(`#08090D`) 베이스 + 샴페인 골드 3단(`deep / base / light`). 금색은 항상 그라데이션으로만 쓰고 단색으로 칠하지 않는다 |
| **획** | 큰 숫자·제목은 `FontWeight.Light` + 음수 자간. 작은 라벨은 넓은 자간(1.2~2.4sp)의 대문자 |
| **면** | 카드는 유리 표면(옅은 수직 그라데이션) + 1px 엣지 라이팅 테두리. 발광(glow) 대신 얕은 앰비언트 확산 |

주요 표현 기법

- **브러시드 메탈 스윕** — 진행 링·엠블럼에 `sweepGradient`로 금속 광택을 두 번 스치게 한다 (`MetalSweep`)
- **흐르는 광택(sheen)** — 멤버십 카드와 주 CTA 위를 대각선 하이라이트 띠가 주기적으로 지나간다
- **기요셰(guilloché) 각인** — 지폐·시계 다이얼의 동심원 무늬를 멤버십 카드에 새긴다
- **앰비언트 배경** — 흑요석 그라데이션 위에 아주 옅은 금빛 광원 두 개로 깊이를 만든다
- **숫자 롤업** — 걸음 수·진행률이 툭 바뀌지 않고 굴러 올라간다 (`animatedInt`, `animatedFloat`)
- **그라데이션 텍스트** — `TextStyle(brush = ...)`로 워드마크와 SUP 수치를 금속으로 채운다

디자인 토큰과 공용 컴포넌트는 각각 `ui/theme/`와 `ui/components/`에 모여 있어,
색 하나만 바꿔도 화면 전체의 광택 방향이 함께 움직입니다.

## 주요 기능

| 탭 | 기능 |
|---|---|
| 홈 | 오늘 걸음 수(금속 링 게이지), 거리/칼로리/스트릭, SUP 잔액·에너지, 루트 카드, 최근 7일 차트(목표선 포함) |
| 워킹 | 포그라운드 워킹 세션(시작/일시정지/종료), 실시간 거리·걸음·시간·칼로리, 예상 적립, 세션 정산 카드, 화면 잠금 |
| 리워드 | SUP 멤버십 카드(등급/잔액), 누적 적립·사용 요약, 적립 원장, GIWA 지갑 연동(준비 중) |
| 프로필 | 회원 카드, 스니커즈 레벨·능력치·업그레이드 진행률, 일일 목표 슬라이더, 앱 정보 |

## M2E 경제 모델 (`RewardEconomy`)

- **적립**: 워킹 세션 중 걸음에 대해서만 1보당 `0.01 SUP × 스니커즈 배율` 적립
- **에너지**: 에너지 1칸 = 600보 적립 가능. 매일 자정 최대치로 리필. 에너지 소진 시 적립 중단
- **스니커즈**: 레벨업 시 적립 배율 +0.15, 최대 에너지 +2 (업그레이드 비용 = 레벨 × 100 SUP)
- **일일 목표 보너스**: 목표 달성 시 20 SUP + 연속 달성일당 10% 가산(최대 7일)
- **멤버십 등급**: 스니커즈 레벨에 따라 CLASSIC → SILVER → GOLD → PLATINUM → OBSIDIAN

## 기술 스택

- **UI**: Jetpack Compose + Material 3 (Obsidian & Champagne 다크 테마), Navigation Compose
- **걸음 측정**: `TYPE_STEP_COUNTER` 센서 (자정/재부팅 기준점 보정)
- **세션 추적**: 포그라운드 서비스(`health` 타입) + 알림
- **저장소**: Room(일별 기록·세션·리워드 원장), DataStore(목표·에너지·스트릭)
- **아키텍처**: MVVM + Repository, 수동 DI(ServiceLocator)

```
app/src/main/java/com/giwa/strideup/
├── core/ServiceLocator.kt          # 수동 DI
├── data/
│   ├── local/                      # Room (Entities, DAOs, DB)
│   ├── prefs/UserPrefs.kt          # DataStore 설정/상태
│   └── repo/                       # Step/Reward 리포지토리
├── domain/RewardEconomy.kt         # M2E 경제 모델 (순수 Kotlin, 단위 테스트 포함)
├── sensor/StepTracker.kt           # 걸음 센서 래퍼
├── service/WalkSessionService.kt   # 워킹 세션 포그라운드 서비스
└── ui/
    ├── theme/                      # Color · Brushes · Type · Shape · Theme
    ├── components/                 # LuxeCard, MetalRing, GoldButton, Motion 등 공용 요소
    └── screens/                    # 홈 · 워킹 · 리워드 · 프로필
```

## 빌드 방법

1. Android Studio (Ladybug 이상 권장)에서 프로젝트 열기
2. 자동으로 Gradle 동기화 (AGP 8.7.3 / Kotlin 2.0.21 / compileSdk 35)
3. 실기기 또는 API 26+ 에뮬레이터에서 실행

```bash
./gradlew :app:assembleDebug        # 디버그 APK 빌드
./gradlew :app:testDebugUnitTest    # 경제 모델 단위 테스트
```

### 에뮬레이터에서 체험하기

에뮬레이터에는 걸음 센서가 없는 경우가 많습니다. 디버그 빌드의 **워킹 탭 →
"+100 걸음 시뮬레이션"** 버튼으로 적립 흐름을 체험할 수 있습니다.
(실기기에서는 `ACTIVITY_RECOGNITION` 권한 허용 후 실제 걸음이 집계됩니다.)

## 로드맵

- [ ] GIWA 체인 지갑 연동 및 SUP 온체인 전환/출금
- [ ] 스니커즈 NFT (희귀도/속성) 시스템
- [ ] 소셜 리더보드·챌린지
- [ ] Health Connect 연동
