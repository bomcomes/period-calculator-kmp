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

## 프로젝트 구조

```
period-calculator-kmp/
├── README.md                    # 프로젝트 메인 문서
├── docs/                        # 문서 파일
├── tests/                       # 테스트 파일
│   └── test-scenario.js        # 시나리오 테스트
├── shared/                      # KMP 공유 코드
│   └── src/
│       ├── commonMain/kotlin/
│       │   ├── PeriodCalculator.kt     # 메인 계산 로직
│       │   ├── PregnancyCalculator.kt  # 임신 계산
│       │   └── models/Models.kt        # 데이터 모델
│       └── jsMain/kotlin/
│           └── JsExports.kt            # JavaScript exports
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

### JavaScript/TypeScript

```javascript
const lib = require('./shared/build/dist/js/productionLibrary/period-calculator-kmp-shared.js');
const calculator = lib.com.bomcomes.calculator;

// 생리 기록
const periods = [
    { startDate: '2025-11-01', endDate: '2025-11-05' }
];

// 생리 주기 계산
const cycles = calculator.calculateMenstrualCycles(
    periods,
    '2025-11-01',  // fromDate
    '2025-12-31',  // toDate
    31,            // averageCycle
    5              // periodDays
);

const cycle = cycles[0];
console.log('생리 기간:', cycle.theDay);
console.log('생리 예정일:', cycle.predictDays[0]);
console.log('배란기:', cycle.ovulationDays[0]);
console.log('가임기:', cycle.childbearingAges[0]);

// 특정 날짜의 상태 확인
const status = calculator.calculateCalendarStatus(
    periods,
    '2025-11-14',  // 확인할 날짜
    31,            // averageCycle
    5              // periodDays
);

console.log('상태:', status.calendarType);      // "OVULATION_DAY"
console.log('임신 가능성:', status.probability); // "HIGH"
console.log('주기 일차:', status.gap);          // 13
```

### Kotlin (Android)

```kotlin
import com.bomcomes.calculator.PeriodCalculator
import com.bomcomes.calculator.models.*
import kotlinx.datetime.LocalDate

val input = PeriodCycleInput(
    periods = listOf(
        PeriodRecord(
            startDate = LocalDate(2025, 11, 1),
            endDate = LocalDate(2025, 11, 5)
        )
    ),
    periodSettings = PeriodSettings(
        period = 31,
        days = 5
    )
)

val cycles = PeriodCalculator.calculateMenstrualCycles(
    input,
    fromDate = LocalDate(2025, 11, 1),
    toDate = LocalDate(2025, 12, 31)
)

val status = PeriodCalculator.calculateCalendarStatus(
    input,
    date = LocalDate(2025, 11, 14)
)
```

## 테스트

```bash
# 빌드
./gradlew clean :shared:jsNodeProductionLibraryDistribution

# npm 패키지 설치
cd shared/build/dist/js/productionLibrary && npm install

# 테스트 실행
cd tests && node test-scenario.js
```

## 문서

- **[BUILD_GUIDE.md](docs/BUILD_GUIDE.md)** - 빌드 가이드
- **[FIREBASE_FUNCTIONS_GUIDE.md](docs/FIREBASE_FUNCTIONS_GUIDE.md)** - Firebase Functions 통합 가이드

## 개발 환경

- Kotlin: 1.9.22
- Gradle: 8.5+
- kotlinx-datetime: 0.5.0
- kotlinx-serialization: 1.6.2

## 라이센스

MIT
