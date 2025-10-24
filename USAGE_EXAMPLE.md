# Period Calculator KMP - 사용 예제

## V2 API (완전 이식 버전)

### 1. 기본 사용법

```kotlin
import com.bomcomes.calculator.PeriodCalculatorV2
import com.bomcomes.calculator.models.*
import kotlinx.datetime.LocalDate

// 생리 기록 준비
val periods = listOf(
    PeriodRecord(
        pk = "1",
        startDate = LocalDate(2025, 1, 5),
        endDate = LocalDate(2025, 1, 9)
    ),
    PeriodRecord(
        pk = "2",
        startDate = LocalDate(2024, 12, 6),
        endDate = LocalDate(2024, 12, 10)
    )
)

// 입력 데이터 구성
val input = PeriodCycleInput(
    periods = periods,
    periodSettings = PeriodSettings(
        period = 30,        // 평균 주기
        days = 5,           // 생리 기간
        isAutoCalc = false  // 수동 입력 주기 사용
    )
)

// 특정 날짜의 상태 확인
val status = PeriodCalculatorV2.calculateStatus(
    input = input,
    date = LocalDate(2025, 1, 20)
)

println("상태: ${status.calendarType}")
println("임신 가능성: ${status.probability}")
println("생리 시작 후 일수: ${status.gap}")
```

### 2. 배란 테스트 포함

```kotlin
val ovulationTests = listOf(
    OvulationTest(
        date = LocalDate(2025, 1, 17),
        result = OvulationTest.TestResult.NEGATIVE
    ),
    OvulationTest(
        date = LocalDate(2025, 1, 18),
        result = OvulationTest.TestResult.POSITIVE  // 양성!
    ),
    OvulationTest(
        date = LocalDate(2025, 1, 19),
        result = OvulationTest.TestResult.UNCLEAR
    )
)

val input = PeriodCycleInput(
    periods = periods,
    periodSettings = PeriodSettings(period = 30, days = 5),
    ovulationTests = ovulationTests
)

// 배란일은 테스트 양성 결과를 우선 사용
val cycles = PeriodCalculatorV2.calculateMenstrualCycles(
    input = input,
    from = LocalDate(2025, 1, 1),
    to = LocalDate(2025, 1, 31)
)

cycles.forEach { cycle ->
    println("배란일: ${cycle.ovulationDays}")
    println("가임기: ${cycle.childbearingAges}")
    println("사용자 입력 배란일 사용: ${cycle.isOvulationPeriodUserInput}")
}
```

### 3. 피임약 복용 고려

```kotlin
val pillPackages = listOf(
    PillPackage(
        packageStart = LocalDate(2024, 12, 31),
        pillCount = 21,
        restDays = 7
    )
)

val pillSettings = PillSettings(
    isCalculatingWithPill = true,
    pillCount = 21,
    restPill = 7
)

val input = PeriodCycleInput(
    periods = periods,
    periodSettings = PeriodSettings(period = 30, days = 5),
    pillPackages = pillPackages,
    pillSettings = pillSettings
)

// 피임약 복용 중에는 배란이 억제됨
val status = PeriodCalculatorV2.calculateStatus(
    input = input,
    date = LocalDate(2025, 1, 15)
)

// 피임약 복용 여부 확인
val isPillActive = PeriodCalculatorV2.isPillActive(
    date = LocalDate(2025, 1, 15),
    pillPackages = pillPackages,
    pillSettings = pillSettings
)
println("피임약 복용 중: $isPillActive")
```

### 4. 임신 상태 처리

```kotlin
val pregnancy = PregnancyInfo(
    startsDate = LocalDate(2025, 2, 1),
    dueDate = LocalDate(2025, 11, 1),
    isEnded = false
)

val input = PeriodCycleInput(
    periods = periods,
    periodSettings = PeriodSettings(period = 30, days = 5),
    pregnancy = pregnancy
)

val status = PeriodCalculatorV2.calculateStatus(
    input = input,
    date = LocalDate(2025, 3, 1)
)

// 임신 중이면 probability = PREGNANCY
println("임신 중: ${status.probability == CalendarStatus.ProbabilityOfPregnancy.PREGNANCY}")
```

### 5. 배란일 직접 입력

```kotlin
val userOvulationDays = listOf(
    OvulationDay(date = LocalDate(2025, 1, 19)),
    OvulationDay(date = LocalDate(2024, 12, 20))
)

val input = PeriodCycleInput(
    periods = periods,
    periodSettings = PeriodSettings(period = 30, days = 5),
    userOvulationDays = userOvulationDays
)

// 사용자 직접 입력이 최우선
val cycles = PeriodCalculatorV2.calculateMenstrualCycles(
    input = input,
    from = LocalDate(2025, 1, 1),
    to = LocalDate(2025, 1, 31)
)

println("배란일 (사용자 입력): ${cycles.first().ovulationDays}")
```

### 6. test-data.md 시나리오 1 (피임약 복용 중)

```kotlin
// 시나리오: 주기 1 - 피임약 복용 중
val periods = listOf(
    PeriodRecord(
        pk = "1",
        startDate = LocalDate(2025, 1, 5),
        endDate = LocalDate(2025, 1, 9)
    )
)

val ovulationTests = listOf(
    OvulationTest(
        date = LocalDate(2025, 1, 17),
        result = OvulationTest.TestResult.NEGATIVE
    ),
    OvulationTest(
        date = LocalDate(2025, 1, 18),
        result = OvulationTest.TestResult.POSITIVE
    ),
    OvulationTest(
        date = LocalDate(2025, 1, 19),
        result = OvulationTest.TestResult.UNCLEAR
    )
)

val userOvulationDays = listOf(
    OvulationDay(date = LocalDate(2025, 1, 19))
)

val pillPackages = listOf(
    PillPackage(
        packageStart = LocalDate(2024, 12, 31),
        pillCount = 26,  // 26일 복용
        restDays = 7
    )
)

val pillSettings = PillSettings(
    isCalculatingWithPill = true,
    pillCount = 26,
    restPill = 7
)

val input = PeriodCycleInput(
    periods = periods,
    periodSettings = PeriodSettings(period = 30, days = 5),
    ovulationTests = ovulationTests,
    userOvulationDays = userOvulationDays,
    pillPackages = pillPackages,
    pillSettings = pillSettings
)

// 예상 결과:
// - 다음 생리 예정일: 2025-02-04
// - 배란일: null (피임약 복용 중)
// - 가임기: null (배란 없음)

val cycles = PeriodCalculatorV2.calculateMenstrualCycles(
    input = input,
    from = LocalDate(2025, 1, 1),
    to = LocalDate(2025, 2, 28)
)

cycles.forEach { cycle ->
    println("생리일: ${cycle.theDay}")
    println("예정일: ${cycle.predictDays}")
    println("배란일: ${cycle.ovulationDays}") // 피임약으로 억제
    println("가임기: ${cycle.childbearingAges}") // 피임약으로 억제
}
```

### 7. test-data.md 시나리오 2 (자연 주기)

```kotlin
// 시나리오: 주기 2 - 자연 주기
val periods = listOf(
    PeriodRecord(
        pk = "1",
        startDate = LocalDate(2024, 12, 6),
        endDate = LocalDate(2024, 12, 10)
    )
)

val ovulationTests = listOf(
    OvulationTest(
        date = LocalDate(2024, 12, 17),
        result = OvulationTest.TestResult.NEGATIVE
    ),
    OvulationTest(
        date = LocalDate(2024, 12, 19),
        result = OvulationTest.TestResult.POSITIVE
    )
)

val userOvulationDays = listOf(
    OvulationDay(date = LocalDate(2024, 12, 20))
)

val input = PeriodCycleInput(
    periods = periods,
    periodSettings = PeriodSettings(period = 30, days = 5),
    ovulationTests = ovulationTests,
    userOvulationDays = userOvulationDays,
    pillSettings = PillSettings(isCalculatingWithPill = false) // 피임약 없음
)

// 예상 결과:
// - 다음 생리 예정일: 2025-01-05
// - 배란일: 2024-12-20 (사용자 입력 우선)
// - 가임기: 2024-12-15 ~ 2024-12-20

val cycles = PeriodCalculatorV2.calculateMenstrualCycles(
    input = input,
    from = LocalDate(2024, 12, 1),
    to = LocalDate(2025, 1, 15)
)

cycles.forEach { cycle ->
    println("생리일: ${cycle.theDay}")
    println("예정일: ${cycle.predictDays}")
    println("배란일: ${cycle.ovulationDays}") // 사용자 입력: 2024-12-20
    println("가임기: ${cycle.childbearingAges}") // 2024-12-15 ~ 2024-12-21
}
```

### 8. 간단한 계산 (단일 메서드)

```kotlin
// 다음 생리 예정일만 빠르게 계산
val nextPeriod = PeriodCalculatorV2.calculateNextPeriod(
    lastPeriodStartDate = LocalDate(2025, 1, 5),
    lastPeriodEndDate = LocalDate(2025, 1, 9),
    periodSettings = PeriodSettings(period = 30, days = 5)
)
println("다음 생리 예정일: ${nextPeriod.startDate} ~ ${nextPeriod.endDate}")

// 배란일 계산
val ovulation = PeriodCalculatorV2.calculateOvulation(
    nextPeriodStartDate = nextPeriod.startDate
)
println("배란일: ${ovulation.startDate} ~ ${ovulation.endDate}")

// 가임기 계산
val fertile = PeriodCalculatorV2.calculateFertileWindow(
    ovulationDate = ovulation
)
println("가임기: ${fertile.startDate} ~ ${fertile.endDate}")
```

## 주요 데이터 구조

### CalendarStatus.CalendarType
- `NONE`: 없음
- `THE_DAY`: 생리 중
- `PREDICT`: 생리 예정일
- `OVULATION_DAY`: 배란일
- `CHILDBEARING_AGE`: 가임기
- `DELAY`: 생리 지연

### CalendarStatus.ProbabilityOfPregnancy
- `LOW`: 낮음
- `MIDDLE`: 중간
- `NORMAL`: 보통
- `HIGH`: 높음 (배란기/가임기)
- `PREGNANCY`: 임신 중
- `RECOVERY_AFTER_CHILDBIRTH`: 출산 후 회복기
- `NO_THE_DAY`: 생리일 없음
- `INPUT_THE_DAY`: 생리일 입력 필요
- `HOSPITAL_OVER_DELAY_8`: 8일 이상 지연 (병원 권장)

### OvulationTest.TestResult
- `NEGATIVE`: 음성 (0)
- `POSITIVE`: 양성 (1)
- `UNCLEAR`: 불명확 (2)

## 우선순위

### 배란일 추정 우선순위
1. **사용자 직접 입력** (`userOvulationDays`)
2. **배란 테스트 양성 결과** (`ovulationTests` 중 POSITIVE)
3. **자동 계산** (생리 예정일 14일 전)

### 피임약 복용 시
- 배란 억제 (배란일/가임기 없음)
- 피임약 패키지 시작일 기준으로 예정일 재계산
- 복용일/휴약일 자동 추적

### 임신 중
- 모든 생리/배란 예측 중단
- 임신 기간 표시
- 출산 후 회복기 표시
