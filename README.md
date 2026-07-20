# StrideUp — GIWA STEPN (M2E 걷기 리워드 앱)

걸을수록 **SUP 포인트**가 쌓이는 Move-to-Earn(M2E) 안드로이드 앱입니다.
StepN 스타일의 에너지/스니커즈 경제 모델을 채택했으며, 적립된 SUP는 추후
**GIWA 체인** 온체인 토큰으로 전환하는 것을 목표로 합니다.

> 이 저장소는 StrideUp 데모(HTML 시안)와 M2E 기획 문서를 참고해 만든
> 네이티브 안드로이드 구현입니다.

## 주요 기능

| 탭 | 기능 |
|---|---|
| 홈 | 오늘 걸음 수 링 게이지, 거리/칼로리/달성률, 에너지 게이지, SUP 잔액, 최근 7일 차트, 연속 달성 스트릭 |
| 워킹 | 포그라운드 워킹 세션(시작/일시정지/종료), 실시간 걸음·시간·예상 적립, 세션 종료 정산 카드 |
| 리워드 | SUP 잔액 카드, 적립/사용 원장 내역, GIWA 지갑 연동(준비 중) |
| 프로필 | 스니커즈 레벨/업그레이드, 일일 목표 설정 슬라이더, 스트릭 |

## M2E 경제 모델 (`RewardEconomy`)

- **적립**: 워킹 세션 중 걸음에 대해서만 1보당 `0.01 SUP × 스니커즈 배율` 적립
- **에너지**: 에너지 1칸 = 600보 적립 가능. 매일 자정 최대치로 리필. 에너지 소진 시 적립 중단
- **스니커즈**: 레벨업 시 적립 배율 +0.15, 최대 에너지 +2 (업그레이드 비용 = 레벨 × 100 SUP)
- **일일 목표 보너스**: 목표 달성 시 20 SUP + 연속 달성일당 10% 가산(최대 7일)

## 기술 스택

- **UI**: Jetpack Compose + Material 3 (다크 네온 테마), Navigation Compose
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
└── ui/                             # Compose 화면 4개 + 테마 + 컴포넌트
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
