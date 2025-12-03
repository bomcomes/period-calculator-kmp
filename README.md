# Period Calculator KMP

생리 주기, 배란일, 가임기를 계산하는 Kotlin Multiplatform 라이브러리

## 지원 플랫폼

- **Android** (`.aar`)
- **iOS** (`.framework`)
- **JavaScript/Node.js** (`.js` + `.d.ts`)

## 주요 기능

- **생리 주기 계산**: 생리 기록 기반 정확한 주기 계산
- **배란일/가임기 자동 계산**: 주기 기반 자동 계산
- **배란 테스트 결과 반영**: 양성 결과 우선 반영
- **사용자 직접 입력**: 배란일 직접 입력 최우선 처리
- **피임약 복용 고려**: 피임약 복용 시 계산 조정
- **임신 기간 필터링**: 임신 중 생리/배란 제외
- **생리 지연 계산**: 예정일 대비 지연 일수 계산
- **달력 상태 분석**: 생리 중/예정일/배란기/가임기/지연 상태 구분

## 계산 정책 (Priority Rules)

### 생리 예정일 우선순위

생리 예정일 계산 시 다음 우선순위로 적용됩니다:

1. **피임약 기준** (최우선)
   - 조건: `isCalculatingWithPill = true` AND 피임약 패키지 존재 AND 5일 규칙 충족
   - 5일 규칙: 생리 예정일 5일 전까지 피임약 복용 시작해야 피임약 기준 적용
   - 예정일 = 피임약 패키지 시작일 + 복용일(pillCount) + 휴약일(restPill)

2. **배란일 직접 입력 기준**
   - 조건: 사용자가 배란일 직접 입력 OR 배란 테스트기 양성
   - 예정일 = 배란일 + 14일 (황체기)
   - `isOvulationPeriodUserInput = true`로 설정됨

3. **일반 주기 기준** (기본)
   - 조건: 위 조건에 해당하지 않는 경우
   - 예정일 = 마지막 생리 시작일 + 평균 주기

### 배란기/가임기 표시 우선순위

1. **사용자 입력 우선**
   - 배란일 직접 입력 또는 배란 테스트기 양성이 있으면 해당 날짜 기준
   - 가임기 = 배란일 -2일 ~ +1일

2. **주기 기반 자동 계산**
   - 사용자 입력 없을 시 주기 기반 자동 계산
   - 배란기 = 주기 -14일 ±1일
   - 가임기 = 주기 -18일 ~ 주기 -11일

3. **피임약 복용 시**
   - 5일 규칙 충족 시 배란기/가임기 숨김 (피임 효과)
   - 연속 복용(휴약기 0일) 시에도 숨김

### CycleInfo 필드 설명

| 필드 | 타입 | 설명 |
|------|------|------|
| `isOvulationPeriodUserInput` | Boolean | 배란일이 사용자 입력 기반인지 여부 |
| `ovulationDayPeriod` | Int? | 배란일 기준 주기 (배란일 + 14일) |
| `thePillPeriod` | Int? | 피임약 기준 주기 |
| `isContinuousPillUsage` | Boolean | 연속 복용 여부 (휴약기 0일) |

### 지연 처리

- 지연 1-7일: 생리 지연 상태 표시
- 지연 8일 이상: 예정일 숨김, 병원 진료 권장 상태

## 프로젝트 구조

```
period-calculator-kmp/
├── README.md                              # 프로젝트 메인 문서
├── docs/                                  # 문서 파일
├── test-cases/docs/                       # 테스트 케이스 문서
├── shared/                                # KMP 공유 코드
│   └── src/
│       ├── commonMain/kotlin/
│       │   ├── PeriodCalculator.kt       # 메인 계산 로직
│       │   ├── PregnancyCalculator.kt    # 임신 계산
│       │   ├── models/Models.kt          # 데이터 모델
│       │   └── utils/DateUtils.kt        # 날짜 유틸리티
│       ├── jsMain/kotlin/
│       │   ├── JsExports.kt              # JavaScript exports
│       │   └── utils/DateUtils.kt        # JS용 날짜 변환
│       └── commonTest/kotlin/            # Kotlin 테스트
│           ├── integration/              # 통합 테스트 (Data-Driven)
│           ├── unit/                     # 단위 테스트
│           └── repository/               # Repository 테스트
└── build.gradle.kts
```

## 빌드 방법

```bash
# JavaScript 빌드 (권장)
./gradlew clean :shared:jsNodeProductionLibraryDistribution

# 빌드 결과 위치
shared/build/dist/js/productionLibrary/

# Android AAR 빌드 (Android SDK 필요)
./gradlew :shared:assembleRelease

# iOS Framework 빌드 (macOS + Xcode 필요)
./gradlew :shared:linkReleaseFrameworkIosArm64
```

## 사용 예시

### JavaScript/TypeScript (Firebase Functions)

```javascript
const kmp = require('./shared/build/dist/js/productionLibrary/period-calculator-kmp-shared');
const {
  calculateCycleInfo,
  getDayStatus,
  stringToJulianDay,
  julianDayToString,
  JsPeriodRecord,
  getLibraryVersion
} = kmp.com.bomcomes.calculator;

// 라이브러리 버전 확인
console.log(getLibraryVersion()); // "period-calculator-kmp v1.0.0 (build XXX)"

// 생리 기록 (Firebase에서 가져온 데이터)
const periods = [
  new JsPeriodRecord(
    "1",                              // pk
    stringToJulianDay("2025-01-01"), // startDate (julianDay)
    stringToJulianDay("2025-01-05")  // endDate (julianDay)
  ),
  new JsPeriodRecord(
    "2",
    stringToJulianDay("2025-02-01"),
    stringToJulianDay("2025-02-05")
  ),
  new JsPeriodRecord(
    "3",
    stringToJulianDay("2025-03-01"),
    stringToJulianDay("2025-03-05")
  )
];

// 주기 정보 계산
const cycles = calculateCycleInfo(
  periods,
  stringToJulianDay("2025-03-01"),  // fromDate
  stringToJulianDay("2025-03-31"),  // toDate
  stringToJulianDay("2025-03-15"),  // today
  30,  // averageCycle
  5    // periodDays
);

// 결과 확인
const cycle = cycles[0];
console.log('pk:', cycle.pk);
console.log('실제 생리:', julianDayToString(cycle.actualPeriod.startDate), '~', julianDayToString(cycle.actualPeriod.endDate));
console.log('생리 예정일:', cycle.predictDays.map(d => `${julianDayToString(d.startDate)}~${julianDayToString(d.endDate)}`));
console.log('배란기:', cycle.ovulationDays.map(d => `${julianDayToString(d.startDate)}~${julianDayToString(d.endDate)}`));
console.log('가임기:', cycle.fertileDays.map(d => `${julianDayToString(d.startDate)}~${julianDayToString(d.endDate)}`));
console.log('지연 일수:', cycle.delayDays);

// 특정 날짜의 상태 확인
const status = getDayStatus(
  periods,
  stringToJulianDay("2025-03-14"),  // targetDate
  stringToJulianDay("2025-03-15"),  // today
  30,  // averageCycle
  5    // periodDays
);

console.log('날짜:', julianDayToString(status.date));
console.log('상태:', status.type);  // "OVULATION", "FERTILE", "PERIOD_ONGOING" 등
console.log('주기 일차:', status.gap);
console.log('주기:', status.period);
```

### Kotlin (Android/JVM)

```kotlin
import com.bomcomes.calculator.PeriodCalculator
import com.bomcomes.calculator.models.*
import com.bomcomes.calculator.utils.DateUtils

val periods = listOf(
    PeriodRecord(
        pk = "1",
        startDate = DateUtils.toJulianDay(LocalDate(2025, 1, 1)),
        endDate = DateUtils.toJulianDay(LocalDate(2025, 1, 5))
    ),
    PeriodRecord(
        pk = "2",
        startDate = DateUtils.toJulianDay(LocalDate(2025, 2, 1)),
        endDate = DateUtils.toJulianDay(LocalDate(2025, 2, 5))
    ),
    PeriodRecord(
        pk = "3",
        startDate = DateUtils.toJulianDay(LocalDate(2025, 3, 1)),
        endDate = DateUtils.toJulianDay(LocalDate(2025, 3, 5))
    )
)

val input = CycleInput(
    periods = periods,
    periodSettings = PeriodSettings(
        manualAverageCycle = 30,
        manualAverageDay = 5,
        autoAverageCycle = 30,
        autoAverageDay = 5,
        isAutoCalc = false
    )
)

val cycles = PeriodCalculator.calculateCycleInfo(
    input,
    fromDate = DateUtils.toJulianDay(LocalDate(2025, 3, 1)),
    toDate = DateUtils.toJulianDay(LocalDate(2025, 3, 31)),
    today = DateUtils.toJulianDay(LocalDate(2025, 3, 15))
)

val status = PeriodCalculator.getDayStatus(
    input,
    targetDate = DateUtils.toJulianDay(LocalDate(2025, 3, 14)),
    today = DateUtils.toJulianDay(LocalDate(2025, 3, 15))
)
```

## 테스트

### 테스트 케이스 문서

| 문서 | 설명 |
|------|------|
| `01-standard-days-26-32.md` | 표준 주기 (26-32일) |
| `02-short-cycle-under-25.md` | 짧은 주기 (25일 미만) |
| `03-long-cycle-over-33.md` | 긴 주기 (33일 이상) |
| `04-with-pill.md` | 피임약 복용 |
| `05-with-ovulation-test.md` | 배란 테스트기 |
| `06-with-manual-ovulation.md` | 수동 배란일 입력 |
| `07-with-pregnancy.md` | 임신 모드 |

### 테스트 실행

```bash
# 전체 테스트
./gradlew :shared:jvmTest

# 특정 테스트 클래스
./gradlew :shared:jvmTest --tests "com.bomcomes.calculator.integration.WithPillTest"

# 특정 테스트 메서드
./gradlew :shared:jvmTest --tests "com.bomcomes.calculator.integration.WithPillTest.testTC_04_01*"
```

## 문서

- **[BUILD_GUIDE.md](docs/BUILD_GUIDE.md)** - 빌드 가이드
- **[FIREBASE_FUNCTIONS_GUIDE.md](docs/FIREBASE_FUNCTIONS_GUIDE.md)** - Firebase Functions 통합 가이드
- **[REPOSITORY_PATTERN.md](docs/REPOSITORY_PATTERN.md)** - Repository 패턴 가이드

## 개발 환경

- Kotlin: 1.9.22
- Gradle: 8.5+
- kotlinx-datetime: 0.5.0
- kotlinx-serialization: 1.6.2

## 라이센스

MIT
