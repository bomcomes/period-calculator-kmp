package com.bomcomes.calculator.models

import kotlinx.datetime.LocalDate

/**
 * 생리 기록
 */
data class PeriodRecord(
    val pk: String = "",
    val startDate: LocalDate,
    val endDate: LocalDate
) {
    val durationDays: Int
        get() = (endDate.toEpochDays() - startDate.toEpochDays() + 1).toInt()
}

/**
 * 배란 테스트 결과
 */
data class OvulationTest(
    val date: LocalDate,
    val result: TestResult
) {
    enum class TestResult {
        NEGATIVE,   // 음성 (0)
        POSITIVE,   // 양성 (1)
        UNCLEAR     // 불명확 (2)
    }
}

/**
 * 배란일 직접 입력
 */
data class OvulationDay(
    val date: LocalDate
)

/**
 * 피임약 패키지 정보
 */
data class PillPackage(
    val packageStart: LocalDate,
    val pillCount: Int = 21,
    val restDays: Int = 7
) {
    val totalDays: Int
        get() = pillCount + restDays
}

/**
 * 피임약 설정
 */
data class PillSettings(
    val isCalculatingWithPill: Boolean = false,  // 피임약 계산 활성화 여부
    val pillCount: Int = 21,                     // 복용일 수
    val restPill: Int = 7                        // 휴약일 수
)

/**
 * 임신 정보 (iOS ThePregnancy 테이블)
 */
data class PregnancyInfo(
    val id: String = "",                            // primaryKey (uuid)
    val babyName: String = "",                      // 아기 이름 (태명)
    val isDueDateDecided: Boolean = false,          // 출산 예정일 결정 여부
    val lastTheDayDate: LocalDate? = null,          // 마지막 생리 시작일 (출산일 계산용)
    val dueDate: LocalDate? = null,                 // 출산 예정일 (계산 또는 직접 입력)
    val beforePregnancyWeight: Float? = null,       // 임신 전 체중
    val weightUnit: WeightUnit = WeightUnit.KG,     // 체중 단위
    val isMultipleBirth: Boolean = false,           // 다태아 여부
    val isMiscarriage: Boolean = false,             // 유산 여부
    val startsDate: LocalDate,                      // 임신 시작일 (필수)
    val isEnded: Boolean = false,                   // 출산 완료 여부
    val modifyDate: Long = 0,                       // 수정일 (timestamp)
    val regDate: Long = 0,                          // 등록일 (timestamp)
    val isDeleted: Boolean = false                  // 삭제 여부
) {
    /**
     * 체중 단위
     */
    enum class WeightUnit {
        KG,     // 킬로그램
        LBS,    // 파운드
        ST      // 스톤 (영국)
    }

    /**
     * 임신 주차 계산 (시작일 기준)
     */
    fun getWeeksFromStart(currentDate: LocalDate): Int {
        val days = (currentDate.toEpochDays() - startsDate.toEpochDays()).toInt()
        return days / 7
    }

    /**
     * 출산 예정일까지 남은 일수
     */
    fun getDaysUntilDue(currentDate: LocalDate): Int? {
        return dueDate?.let {
            (it.toEpochDays() - currentDate.toEpochDays()).toInt()
        }
    }

    /**
     * 임신 진행 중 여부
     */
    fun isActive(): Boolean {
        return !isEnded && !isMiscarriage && !isDeleted
    }
}

/**
 * 생리 주기 설정
 */
data class PeriodSettings(
    val period: Int = 30,              // 수동 입력 평균 주기
    val days: Int = 5,                 // 생리 기간 (일)
    val autoPeriod: Int = 30,          // 자동 계산된 평균 주기
    val isAutoCalc: Boolean = false    // 자동 계산 사용 여부
) {
    fun getAverageCycle(): Int = if (isAutoCalc) autoPeriod else period
}

/**
 * 날짜 범위
 */
data class DateRange(
    val startDate: LocalDate,
    val endDate: LocalDate
) {
    fun contains(date: LocalDate): Boolean {
        return date >= startDate && date <= endDate
    }

    val durationDays: Int
        get() = (endDate.toEpochDays() - startDate.toEpochDays() + 1).toInt()
}

/**
 * 생리 주기 계산 입력 데이터
 */
data class PeriodCycleInput(
    val periods: List<PeriodRecord>,                      // 생리 기록들
    val periodSettings: PeriodSettings = PeriodSettings(), // 생리 주기 설정
    val ovulationTests: List<OvulationTest> = emptyList(), // 배란 테스트 결과들
    val userOvulationDays: List<OvulationDay> = emptyList(), // 사용자 직접 입력 배란일들
    val pillPackages: List<PillPackage> = emptyList(),    // 피임약 패키지들
    val pillSettings: PillSettings = PillSettings(),       // 피임약 설정
    val pregnancy: PregnancyInfo? = null                   // 임신 정보
)

/**
 * 생리 주기 정보
 */
data class PeriodCycle(
    val pk: String = "",
    val theDay: DateRange? = null,                        // 생리 기간
    val predictDays: List<DateRange> = emptyList(),        // 생리 예정일들
    val ovulationDays: List<DateRange> = emptyList(),      // 배란일들
    val childbearingAges: List<DateRange> = emptyList(),   // 가임기들
    val delayDay: DateRange? = null,                       // 지연 기간
    val delayTheDays: Int = 0,                             // 지연 일수
    val period: Int = 0,                                   // 주기
    val thePillPeriod: Int? = null,                        // 피임약 기준 주기
    val ovulationDayPeriod: Int? = null,                   // 배란일 기준 주기
    val isOvulationPeriodUserInput: Boolean = false,       // 배란일 사용자 입력 여부
    val pregnancyStartDate: LocalDate? = null,             // 임신 시작일
    val restPill: Int? = null                              // 남은 휴약일
)

/**
 * 달력 상태 정보
 */
data class CalendarStatus(
    val calendarType: CalendarType,
    val gap: Int,                                          // 생리 시작일로부터 일수
    val probability: ProbabilityOfPregnancy,               // 임신 가능성
    val period: Int                                        // 주기
) {
    enum class CalendarType {
        NONE,                // 없음
        THE_DAY,            // 생리 중
        PREDICT,            // 생리 예정일
        OVULATION_DAY,      // 배란일
        CHILDBEARING_AGE,   // 가임기
        DELAY               // 생리 지연
    }

    enum class ProbabilityOfPregnancy {
        LOW,                        // 낮음
        MIDDLE,                     // 중간
        NORMAL,                     // 보통
        HIGH,                       // 높음
        PREGNANCY,                  // 임신 중
        RECOVERY_AFTER_CHILDBIRTH,  // 출산 후 회복기
        NO_THE_DAY,                 // 생리일 없음
        INPUT_THE_DAY,              // 생리일 입력 필요
        HOSPITAL_OVER_DELAY_8       // 8일 이상 지연 (병원 권장)
    }
}
