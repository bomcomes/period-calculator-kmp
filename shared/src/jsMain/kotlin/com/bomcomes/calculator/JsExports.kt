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
 * 주기 정보 계산 (메인 함수)
 *
 * @param periodsJson 생리 기록 배열 JSON
 * @param fromDate 검색 시작일 (ISO 8601)
 * @param toDate 검색 종료일 (ISO 8601)
 * @param averageCycle 평균 주기 (일)
 * @param periodDays 생리 기간 (일)
 * @return 주기 정보
 */
@JsName("calculateCycleInfo")
fun calculateCycleInfoJs(
    periodsJson: Array<JsPeriodRecord>,
    fromDate: String,
    toDate: String,
    averageCycle: Int = 28,
    periodDays: Int = 5
): Array<JsCycleInfo> {
    val periods = periodsJson.map { jsRecord ->
        PeriodRecord(
            startDate = LocalDate.parse(jsRecord.startDate),
            endDate = LocalDate.parse(jsRecord.endDate)
        )
    }
    val input = CycleInput(
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

    val result = PeriodCalculator.calculateCycleInfo(input, from, to)
    return result.map { it.toJs() }.toTypedArray()
}

/**
 * 특정 날짜의 상태 계산
 *
 * @param periodsJson 생리 기록 배열
 * @param targetDate 확인할 날짜 (ISO 8601)
 * @param today 오늘 날짜 (ISO 8601)
 * @param averageCycle 평균 주기
 * @param periodDays 생리 기간
 * @return 날짜 상태
 */
@JsName("getDayStatus")
fun getDayStatusJs(
    periodsJson: Array<JsPeriodRecord>,
    targetDate: String,
    today: String,
    averageCycle: Int = 28,
    periodDays: Int = 5
): JsDayStatus {
    val periods = periodsJson.map { jsRecord ->
        PeriodRecord(
            startDate = LocalDate.parse(jsRecord.startDate),
            endDate = LocalDate.parse(jsRecord.endDate)
        )
    }
    val input = CycleInput(
        periods = periods,
        periodSettings = PeriodSettings(
            period = averageCycle,
            days = periodDays,
            autoPeriod = averageCycle,
            isAutoCalc = false
        )
    )

    val date = LocalDate.parse(targetDate)
    val todayDate = LocalDate.parse(today)
    val result = PeriodCalculator.getDayStatus(input, date, todayDate)
    return result.toJs()
}

/**
 * 여러 날짜의 상태를 한 번에 계산
 *
 * @param periodsJson 생리 기록 배열
 * @param dates 확인할 날짜 배열 (ISO 8601)
 * @param today 오늘 날짜 (ISO 8601)
 * @param averageCycle 평균 주기
 * @param periodDays 생리 기간
 * @return 날짜 상태 배열
 */
@JsName("getDayStatusesForDates")
fun getDayStatusesForDatesJs(
    periodsJson: Array<JsPeriodRecord>,
    dates: Array<String>,
    today: String,
    averageCycle: Int = 28,
    periodDays: Int = 5
): Array<JsDayStatus> {
    val periods = periodsJson.map { jsRecord ->
        PeriodRecord(
            startDate = LocalDate.parse(jsRecord.startDate),
            endDate = LocalDate.parse(jsRecord.endDate)
        )
    }
    val input = CycleInput(
        periods = periods,
        periodSettings = PeriodSettings(
            period = averageCycle,
            days = periodDays,
            autoPeriod = averageCycle,
            isAutoCalc = false
        )
    )

    val dateList = dates.map { LocalDate.parse(it) }
    val todayDate = LocalDate.parse(today)
    val result = PeriodCalculator.getDayStatusesForDates(input, dateList, todayDate)
    return result.map { it.toJs() }.toTypedArray()
}

/**
 * 날짜 범위의 상태를 한 번에 계산
 *
 * @param periodsJson 생리 기록 배열
 * @param fromDate 시작 날짜 (ISO 8601)
 * @param toDate 종료 날짜 (ISO 8601)
 * @param today 오늘 날짜 (ISO 8601)
 * @param averageCycle 평균 주기
 * @param periodDays 생리 기간
 * @return 날짜 상태 배열
 */
@JsName("getDayStatuses")
fun getDayStatusesJs(
    periodsJson: Array<JsPeriodRecord>,
    fromDate: String,
    toDate: String,
    today: String,
    averageCycle: Int = 28,
    periodDays: Int = 5
): Array<JsDayStatus> {
    val periods = periodsJson.map { jsRecord ->
        PeriodRecord(
            startDate = LocalDate.parse(jsRecord.startDate),
            endDate = LocalDate.parse(jsRecord.endDate)
        )
    }
    val input = CycleInput(
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
    val todayDate = LocalDate.parse(today)
    val result = PeriodCalculator.getDayStatuses(input, from, to, todayDate)
    return result.map { it.toJs() }.toTypedArray()
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
 * JS용 주기 정보
 */
data class JsCycleInfo(
    val actualPeriod: JsDateRange?,
    val predictDays: Array<JsDateRange>,
    val ovulationDays: Array<JsDateRange>,
    val fertileDays: Array<JsDateRange>,
    val delayPeriod: JsDateRange?,
    val delayDays: Int,
    val period: Int
)

/**
 * JS용 날짜 상태
 */
data class JsDayStatus(
    val date: String,               // ISO 8601
    val type: String,               // "NONE", "PERIOD_ONGOING", "PERIOD_UPCOMING", "PERIOD_PREDICTED", "PERIOD_DELAYED", "PERIOD_DELAYED_OVER", "OVULATION", "FERTILE", "PREGNANCY", "EMPTY"
    val gap: Int?,                  // 생리 시작일로부터 며칠째 (null 가능)
    val period: Int                 // 주기
)

// MARK: - 변환 함수

private fun DateRange.toJs(): JsDateRange {
    return JsDateRange(
        startDate = startDate.toString(),
        endDate = endDate.toString()
    )
}

private fun CycleInfo.toJs(): JsCycleInfo {
    return JsCycleInfo(
        actualPeriod = actualPeriod?.toJs(),
        predictDays = predictDays.map { it.toJs() }.toTypedArray(),
        ovulationDays = ovulationDays.map { it.toJs() }.toTypedArray(),
        fertileDays = fertileDays.map { it.toJs() }.toTypedArray(),
        delayPeriod = delayDay?.toJs(),
        delayDays = delayTheDays,
        period = period
    )
}

private fun DayStatus.toJs(): JsDayStatus {
    return JsDayStatus(
        date = date.toString(),
        type = type.name,
        gap = gap,
        period = period
    )
}
