@file:JsExport

package com.bomcomes.calculator

import com.bomcomes.calculator.models.*
import kotlinx.datetime.LocalDate

/**
 * JavaScript/TypeScript Export
 *
 * 생리 주기 계산을 위한 API
 */

/**
 * 생리 주기 정보 계산 (메인 함수)
 *
 * @param periodsJson 생리 기록 배열 JSON
 * @param fromDate 검색 시작일 (ISO 8601)
 * @param toDate 검색 종료일 (ISO 8601)
 * @param averageCycle 평균 주기 (일)
 * @param periodDays 생리 기간 (일)
 * @return 생리 주기 정보
 */
@JsName("calculateMenstrualCycles")
fun calculateMenstrualCyclesJs(
    periodsJson: Array<JsPeriodRecord>,
    fromDate: String,
    toDate: String,
    averageCycle: Int = 28,
    periodDays: Int = 5
): Array<JsPeriodCycle> {
    val periods = periodsJson.map { jsRecord ->
        PeriodRecord(
            startDate = LocalDate.parse(jsRecord.startDate),
            endDate = LocalDate.parse(jsRecord.endDate)
        )
    }
    val input = PeriodCycleInput(
        periods = periods,
        periodSettings = PeriodSettings(
            period = averageCycle,
            days = periodDays,
            autoPeriod = averageCycle,
            isAutoCalc = false
        )
    )

    val from = LocalDate.parse(fromDate)
    val to = LocalDate.parse(toDate)

    val result = PeriodCalculator.calculateMenstrualCycles(input, from, to)
    return result.map { it.toJs() }.toTypedArray()
}

/**
 * 특정 날짜의 달력 상태 계산
 *
 * @param periodsJson 생리 기록 배열
 * @param targetDate 확인할 날짜 (ISO 8601)
 * @param averageCycle 평균 주기
 * @param periodDays 생리 기간
 * @return 달력 상태
 */
@JsName("calculateCalendarStatus")
fun calculateCalendarStatusJs(
    periodsJson: Array<JsPeriodRecord>,
    targetDate: String,
    averageCycle: Int = 28,
    periodDays: Int = 5
): JsCalendarStatus {
    val periods = periodsJson.map { jsRecord ->
        PeriodRecord(
            startDate = LocalDate.parse(jsRecord.startDate),
            endDate = LocalDate.parse(jsRecord.endDate)
        )
    }
    val input = PeriodCycleInput(
        periods = periods,
        periodSettings = PeriodSettings(
            period = averageCycle,
            days = periodDays,
            autoPeriod = averageCycle,
            isAutoCalc = false
        )
    )

    val date = LocalDate.parse(targetDate)
    val result = PeriodCalculator.calculateCalendarStatus(input, date)
    return result.toJs()
}

// MARK: - JavaScript 데이터 타입

/**
 * JS용 생리 기록
 */
data class JsPeriodRecord(
    val startDate: String,  // ISO 8601
    val endDate: String     // ISO 8601
)

/**
 * JS용 날짜 범위
 */
data class JsDateRange(
    val startDate: String,
    val endDate: String
)

/**
 * JS용 생리 주기 정보
 */
data class JsPeriodCycle(
    val theDay: JsDateRange?,
    val predictDays: Array<JsDateRange>,
    val ovulationDays: Array<JsDateRange>,
    val childbearingAges: Array<JsDateRange>,
    val delayDay: JsDateRange?,
    val delayTheDays: Int,
    val period: Int
)

/**
 * JS용 달력 상태
 */
data class JsCalendarStatus(
    val calendarType: String,       // "NONE", "THE_DAY", "PREDICT", "OVULATION_DAY", "CHILDBEARING_AGE", "DELAY"
    val gap: Int,
    val probability: String,        // "LOW", "MIDDLE", "NORMAL", "HIGH", etc.
    val period: Int
)

// MARK: - 변환 함수

private fun DateRange.toJs(): JsDateRange {
    return JsDateRange(
        startDate = startDate.toString(),
        endDate = endDate.toString()
    )
}

private fun PeriodCycle.toJs(): JsPeriodCycle {
    return JsPeriodCycle(
        theDay = theDay?.toJs(),
        predictDays = predictDays.map { it.toJs() }.toTypedArray(),
        ovulationDays = ovulationDays.map { it.toJs() }.toTypedArray(),
        childbearingAges = childbearingAges.map { it.toJs() }.toTypedArray(),
        delayDay = delayDay?.toJs(),
        delayTheDays = delayTheDays,
        period = period
    )
}

private fun CalendarStatus.toJs(): JsCalendarStatus {
    return JsCalendarStatus(
        calendarType = calendarType.name,
        gap = gap,
        probability = probability.name,
        period = period
    )
}
