# 임신 정보 사용 예제

## PregnancyInfo 모델 구조

```kotlin
data class PregnancyInfo(
    val id: String = "",                            // primaryKey (uuid)
    val babyName: String = "",                      // 아기 이름 (태명)
    val isDueDateDecided: Boolean = false,          // 출산 예정일 결정 여부
    val lastTheDayDate: LocalDate? = null,          // 마지막 생리 시작일
    val dueDate: LocalDate? = null,                 // 출산 예정일
    val beforePregnancyWeight: Float? = null,       // 임신 전 체중
    val weightUnit: WeightUnit = WeightUnit.KG,     // 체중 단위
    val isMultipleBirth: Boolean = false,           // 다태아 여부
    val isMiscarriage: Boolean = false,             // 유산 여부
    val startsDate: LocalDate,                      // 임신 시작일 (필수)
    val isEnded: Boolean = false,                   // 출산 완료 여부
    val modifyDate: Long = 0,                       // 수정일 (timestamp)
    val regDate: Long = 0,                          // 등록일 (timestamp)
    val isDeleted: Boolean = false                  // 삭제 여부
)
```

## 1. 기본 임신 정보 생성

### 케이스 1: 마지막 생리일로 임신 시작

```kotlin
import com.bomcomes.calculator.PregnancyCalculator
import com.bomcomes.calculator.models.PregnancyInfo
import kotlinx.datetime.LocalDate

// 마지막 생리일
val lastPeriodDate = LocalDate(2024, 10, 1)

// 출산 예정일 자동 계산 (마지막 생리일 + 280일)
val dueDate = PregnancyCalculator.calculateDueDate(lastPeriodDate)

val pregnancy = PregnancyInfo(
    id = "pregnancy-001",
    babyName = "콩이",
    isDueDateDecided = true,
    lastTheDayDate = lastPeriodDate,
    dueDate = dueDate,  // 2025-07-08
    startsDate = lastPeriodDate,
    beforePregnancyWeight = 55.5f,
    weightUnit = PregnancyInfo.WeightUnit.KG
)

println("출산 예정일: ${pregnancy.dueDate}")  // 2025-07-08
```

### 케이스 2: 출산 예정일만 알고 있는 경우

```kotlin
val dueDate = LocalDate(2025, 7, 15)

// 마지막 생리일 역계산 (출산 예정일 - 280일)
val lastPeriodDate = PregnancyCalculator.calculateLastPeriodDate(dueDate)

val pregnancy = PregnancyInfo(
    id = "pregnancy-002",
    babyName = "별이",
    isDueDateDecided = true,
    lastTheDayDate = lastPeriodDate,  // 자동 계산됨
    dueDate = dueDate,
    startsDate = lastPeriodDate
)
```

### 케이스 3: 출산 예정일 미정

```kotlin
val pregnancy = PregnancyInfo(
    id = "pregnancy-003",
    babyName = "아기",
    isDueDateDecided = false,
    lastTheDayDate = LocalDate(2024, 11, 1),
    dueDate = null,  // 아직 결정 안 됨
    startsDate = LocalDate(2024, 11, 1)
)
```

## 2. 임신 주차 계산

```kotlin
val pregnancy = PregnancyInfo(
    startsDate = LocalDate(2024, 10, 1),
    lastTheDayDate = LocalDate(2024, 10, 1),
    dueDate = PregnancyCalculator.calculateDueDate(LocalDate(2024, 10, 1))
)

// 현재 임신 주차
val weeks = PregnancyCalculator.calculateWeeksFromLastPeriod(
    lastPeriodStartDate = pregnancy.lastTheDayDate!!,
    currentDate = LocalDate(2024, 12, 24)
)
println("임신 ${weeks}주")  // 임신 12주

// 주차와 일차
val (weeksNum, daysNum) = PregnancyCalculator.calculateWeeksAndDays(
    lastPeriodStartDate = pregnancy.lastTheDayDate!!,
    currentDate = LocalDate(2024, 12, 24)
)
println("임신 ${weeksNum}주 ${daysNum}일")  // 임신 12주 0일
```

## 3. 임신 삼분기 (Trimester)

```kotlin
val weeks = 15

val trimester = PregnancyCalculator.calculateTrimester(weeks)
when (trimester) {
    1 -> println("첫 번째 삼분기 (1-13주)")
    2 -> println("두 번째 삼분기 (14-27주)")  // ← 15주는 여기
    3 -> println("세 번째 삼분기 (28-40주)")
}
```

## 4. 출산까지 남은 기간

```kotlin
val pregnancy = PregnancyInfo(
    startsDate = LocalDate(2024, 10, 1),
    dueDate = LocalDate(2025, 7, 8)
)

// 남은 일수
val daysLeft = PregnancyCalculator.calculateDaysUntilDue(
    dueDate = pregnancy.dueDate!!,
    currentDate = LocalDate(2024, 12, 24)
)
println("출산까지 ${daysLeft}일 남음")  // 196일

// 또는 PregnancyInfo의 메서드 사용
val daysLeft2 = pregnancy.getDaysUntilDue(LocalDate(2024, 12, 24))
println("출산까지 ${daysLeft2}일 남음")
```

## 5. 임신 진행률

```kotlin
val progress = PregnancyCalculator.calculateProgress(
    lastPeriodStartDate = LocalDate(2024, 10, 1),
    currentDate = LocalDate(2024, 12, 24)
)
println("임신 진행률: ${progress.toInt()}%")  // 30%
```

## 6. 체중 관리

```kotlin
// kg로 임신 전 체중 기록
val pregnancy = PregnancyInfo(
    startsDate = LocalDate(2024, 10, 1),
    beforePregnancyWeight = 55.5f,
    weightUnit = PregnancyInfo.WeightUnit.KG
)

// 다른 단위로 변환
val weightInLbs = PregnancyCalculator.kgToLbs(pregnancy.beforePregnancyWeight!!)
println("임신 전 체중: ${weightInLbs.toInt()} lbs")  // 122 lbs

val weightInStone = PregnancyCalculator.kgToStone(pregnancy.beforePregnancyWeight!!)
println("임신 전 체중: ${"%.1f".format(weightInStone)} st")  // 8.7 st

// 정규화 (모든 단위를 kg로)
val normalizedWeight = PregnancyCalculator.normalizeWeightToKg(
    weight = 122f,
    unit = PregnancyInfo.WeightUnit.LBS
)
println("정규화된 체중: ${"%.1f".format(normalizedWeight)} kg")  // 55.3 kg
```

## 7. 다태아 임신

```kotlin
val pregnancy = PregnancyInfo(
    id = "pregnancy-twins",
    babyName = "쌍둥이",
    startsDate = LocalDate(2024, 10, 1),
    lastTheDayDate = LocalDate(2024, 10, 1),
    dueDate = PregnancyCalculator.calculateDueDate(LocalDate(2024, 10, 1)),
    isMultipleBirth = true,  // 다태아
    beforePregnancyWeight = 58.0f
)

if (pregnancy.isMultipleBirth) {
    println("다태아 임신입니다")
}
```

## 8. PeriodCalculatorV2와 함께 사용

```kotlin
import com.bomcomes.calculator.PeriodCalculatorV2
import com.bomcomes.calculator.models.*

// 임신 정보
val pregnancy = PregnancyInfo(
    startsDate = LocalDate(2025, 2, 1),
    dueDate = LocalDate(2025, 11, 1),
    lastTheDayDate = LocalDate(2025, 1, 5)
)

// 생리 기록
val periods = listOf(
    PeriodRecord(
        startDate = LocalDate(2025, 1, 5),
        endDate = LocalDate(2025, 1, 9)
    )
)

// 입력 데이터
val input = PeriodCycleInput(
    periods = periods,
    pregnancy = pregnancy  // 임신 정보 포함
)

// 임신 중 날짜 확인
val status = PeriodCalculatorV2.calculateStatus(
    input = input,
    date = LocalDate(2025, 3, 1)
)

println("상태: ${status.probability}")  // PREGNANCY
println("임신 ${status.gap}일차")
```

## 9. 출산 후 회복기

```kotlin
val pregnancy = PregnancyInfo(
    startsDate = LocalDate(2024, 2, 1),
    dueDate = LocalDate(2024, 11, 1),
    isEnded = true  // 출산 완료
)

val input = PeriodCycleInput(
    periods = emptyList(),
    pregnancy = pregnancy
)

val status = PeriodCalculatorV2.calculateStatus(
    input = input,
    date = LocalDate(2024, 12, 1)
)

// 출산 후 회복기 상태
println(status.probability)  // RECOVERY_AFTER_CHILDBIRTH
```

## 10. 유산 처리

```kotlin
val pregnancy = PregnancyInfo(
    id = "pregnancy-004",
    startsDate = LocalDate(2024, 10, 1),
    lastTheDayDate = LocalDate(2024, 10, 1),
    isMiscarriage = true,  // 유산
    isEnded = true
)

// 활성 임신 여부 확인
val isActive = pregnancy.isActive()
println("진행 중인 임신: $isActive")  // false

// 유산된 임신은 주기 계산에서 제외됨
```

## 11. 임신 정보 수정

```kotlin
var pregnancy = PregnancyInfo(
    id = "pregnancy-005",
    babyName = "아기",
    startsDate = LocalDate(2024, 10, 1),
    lastTheDayDate = LocalDate(2024, 10, 1),
    isDueDateDecided = false,
    dueDate = null,
    regDate = Clock.System.now().toEpochMilliseconds()
)

// 나중에 출산 예정일 확정
pregnancy = pregnancy.copy(
    isDueDateDecided = true,
    dueDate = PregnancyCalculator.calculateDueDate(pregnancy.lastTheDayDate!!),
    modifyDate = Clock.System.now().toEpochMilliseconds()
)

println("출산 예정일 확정: ${pregnancy.dueDate}")
```

## 12. 실전 시나리오: 임신 추적 앱

```kotlin
class PregnancyTracker(private val pregnancy: PregnancyInfo) {
    
    fun getCurrentStatus(): String {
        val today = Clock.System.now().toLocalDateTime(TimeZone.UTC).date
        
        if (!pregnancy.isActive()) {
            return "임신이 종료되었습니다"
        }
        
        val weeks = pregnancy.getWeeksFromStart(today)
        val trimester = PregnancyCalculator.calculateTrimester(weeks)
        val daysLeft = pregnancy.getDaysUntilDue(today)
        
        return buildString {
            appendLine("🤰 ${pregnancy.babyName}")
            appendLine("📅 임신 ${weeks}주")
            appendLine("🔢 ${trimester}분기")
            daysLeft?.let {
                appendLine("⏰ D-${it}일")
            }
        }
    }
    
    fun getWeightGainSuggestion(currentWeight: Float): String {
        val beforeWeight = pregnancy.beforePregnancyWeight ?: return "임신 전 체중 정보 없음"
        val gain = currentWeight - beforeWeight
        
        val weeks = pregnancy.lastTheDayDate?.let { lastDay ->
            val today = Clock.System.now().toLocalDateTime(TimeZone.UTC).date
            PregnancyCalculator.calculateWeeksFromLastPeriod(lastDay, today)
        } ?: 0
        
        return when {
            weeks < 13 -> "첫 삼분기: 권장 체중 증가 1-2kg (현재: +${"%.1f".format(gain)}kg)"
            weeks < 28 -> "둘째 삼분기: 권장 체중 증가 5-7kg (현재: +${"%.1f".format(gain)}kg)"
            else -> "셋째 삼분기: 권장 체중 증가 11-16kg (현재: +${"%.1f".format(gain)}kg)"
        }
    }
}

// 사용 예
val pregnancy = PregnancyInfo(
    babyName = "콩이",
    startsDate = LocalDate(2024, 10, 1),
    lastTheDayDate = LocalDate(2024, 10, 1),
    dueDate = PregnancyCalculator.calculateDueDate(LocalDate(2024, 10, 1)),
    beforePregnancyWeight = 55.5f
)

val tracker = PregnancyTracker(pregnancy)
println(tracker.getCurrentStatus())
println(tracker.getWeightGainSuggestion(57.2f))
```

## 주요 포인트

### 필수 필드
- `startsDate`: 임신 시작일 (필수)

### 선택 필드
- `dueDate`: 출산 예정일 (계산 또는 직접 입력)
- `lastTheDayDate`: 마지막 생리일 (있으면 dueDate 자동 계산 가능)

### 상태 관리
- `isEnded`: 출산 완료
- `isMiscarriage`: 유산
- `isDeleted`: 삭제됨
- `isActive()`: 위 3가지 모두 false일 때 진행 중

### 계산 기준
- 임신 기간: 280일 (40주)
- 삼분기: 1-13주, 14-27주, 28-40주
- 주차 계산: 마지막 생리일 기준
