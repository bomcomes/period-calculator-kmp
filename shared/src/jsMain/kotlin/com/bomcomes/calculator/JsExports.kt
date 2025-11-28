@file:JsExport

package com.bomcomes.calculator

import com.bomcomes.calculator.models.*

/**
 * Get library version string
 */
@JsName("getLibraryVersion")
fun getLibraryVersion(): String = Version.getVersionString()

/**
 * JavaScript/TypeScript Export
 *
 * 생리 주기 계산을 위한 API
 */

/**
 * 주기 정보 계산 (메인 함수)
 *
 * @param periodsJson 생리 기록 배열 JSON
 * @param fromDate 검색 시작일 (julianDay)
 * @param toDate 검색 종료일 (julianDay)
 * @param today 오늘 날짜 (julianDay, 선택사항)
 * @param averageCycle 평균 주기 (일)
 * @param periodDays 생리 기간 (일)
 * @return 주기 정보
 */
@JsName("calculateCycleInfo")
fun calculateCycleInfoJs(
    periodsJson: Array<JsPeriodRecord>,
    fromDate: Double,
    toDate: Double,
    today: Double? = null,
    averageCycle: Int = 28,
    periodDays: Int = 5
): Array<JsCycleInfo> {
    val periods = periodsJson.map { jsRecord ->
        PeriodRecord(
            pk = jsRecord.pk,
            startDate = jsRecord.startDate,
            endDate = jsRecord.endDate
        )
    }
    val input = CycleInput(
        periods = periods,
        periodSettings = PeriodSettings(
            manualAverageCycle = averageCycle,
            manualAverageDay = periodDays,
            autoAverageCycle = averageCycle,
            autoAverageDay = periodDays,
            isAutoCalc = false
        )
    )

    val allCycles = PeriodCalculator.calculateCycleInfo(input, fromDate, toDate, today)

    // 조회 기간 내에 관련 데이터가 있는 주기만 반환
    val result = allCycles.filter { cycle ->
        // actualPeriod이 조회 기간과 겹치는지 확인
        val hasActualPeriod = cycle.actualPeriod?.let { period ->
            period.startDate <= toDate && period.endDate >= fromDate
        } ?: false

        // predictDays가 조회 기간과 겹치는지 확인
        val hasPredictDays = cycle.predictDays.any { predict ->
            predict.startDate <= toDate && predict.endDate >= fromDate
        }

        // fertileDays가 조회 기간과 겹치는지 확인
        val hasFertileDays = cycle.fertileDays.any { fertile ->
            fertile.startDate <= toDate && fertile.endDate >= fromDate
        }

        // ovulationDays가 조회 기간과 겹치는지 확인
        val hasOvulationDays = cycle.ovulationDays.any { ovulation ->
            ovulation.startDate <= toDate && ovulation.endDate >= fromDate
        }

        // delayDay가 조회 기간과 겹치는지 확인
        val hasDelayDay = cycle.delayDay?.let { delay ->
            delay.startDate <= toDate && delay.endDate >= fromDate
        } ?: false

        // actualPeriod이 있고 조회 시작일이 생리 시작일 이후인 경우도 포함
        // (해당 주기 내의 안전기 등을 조회할 수 있도록)
        val isWithinCyclePeriod = cycle.actualPeriod?.let { period ->
            fromDate >= period.startDate && fromDate < period.startDate + cycle.period
        } ?: false

        // 하나라도 조회 기간과 겹치거나 주기 내에 있으면 포함
        hasActualPeriod || hasPredictDays || hasFertileDays || hasOvulationDays || hasDelayDay || isWithinCyclePeriod
    }

    return result.map { cycleInfo ->
        val actualPeriodJs = cycleInfo.actualPeriod?.let {
            JsDateRange(startDate = it.startDate, endDate = it.endDate)
        }
        val predictDaysJs = cycleInfo.predictDays.map {
            JsDateRange(startDate = it.startDate, endDate = it.endDate)
        }.toTypedArray()
        val ovulationDaysJs = cycleInfo.ovulationDays.map {
            JsDateRange(startDate = it.startDate, endDate = it.endDate)
        }.toTypedArray()
        val fertileDaysJs = cycleInfo.fertileDays.map {
            JsDateRange(startDate = it.startDate, endDate = it.endDate)
        }.toTypedArray()
        val delayPeriodJs = cycleInfo.delayDay?.let {
            JsDateRange(startDate = it.startDate, endDate = it.endDate)
        }

        JsCycleInfo(
            pk = cycleInfo.pk,
            actualPeriod = actualPeriodJs,
            predictDays = predictDaysJs,
            ovulationDays = ovulationDaysJs,
            fertileDays = fertileDaysJs,
            delayPeriod = delayPeriodJs,
            delayDays = cycleInfo.delayTheDays,
            period = cycleInfo.period,
            pregnancyStartDate = cycleInfo.pregnancyStartDate
        )
    }.toTypedArray()
}

/**
 * 특정 날짜의 상태 계산
 *
 * @param periodsJson 생리 기록 배열
 * @param targetDate 확인할 날짜 (julianDay)
 * @param today 오늘 날짜 (julianDay)
 * @param averageCycle 평균 주기
 * @param periodDays 생리 기간
 * @return 날짜 상태
 */
@JsName("getDayStatus")
fun getDayStatusJs(
    periodsJson: Array<JsPeriodRecord>,
    targetDate: Double,
    today: Double,
    averageCycle: Int = 28,
    periodDays: Int = 5
): JsDayStatus {
    val periods = periodsJson.map { jsRecord ->
        PeriodRecord(
            pk = jsRecord.pk,
            startDate = jsRecord.startDate,
            endDate = jsRecord.endDate
        )
    }
    val input = CycleInput(
        periods = periods,
        periodSettings = PeriodSettings(
            manualAverageCycle = averageCycle,
            manualAverageDay = periodDays,
            autoAverageCycle = averageCycle,
            autoAverageDay = periodDays,
            isAutoCalc = false
        )
    )

    val result = PeriodCalculator.getDayStatus(input, targetDate, today)
    return JsDayStatus(
        date = result.date,
        type = result.type.name,
        gap = result.gap,
        period = result.period
    )
}

/**
 * 여러 날짜의 상태를 한 번에 계산
 *
 * @param periodsJson 생리 기록 배열
 * @param dates 확인할 날짜 배열 (julianDay)
 * @param today 오늘 날짜 (julianDay)
 * @param averageCycle 평균 주기
 * @param periodDays 생리 기간
 * @return 날짜 상태 배열
 */
@JsName("getDayStatusesForDates")
fun getDayStatusesForDatesJs(
    periodsJson: Array<JsPeriodRecord>,
    dates: Array<Double>,
    today: Double,
    averageCycle: Int = 28,
    periodDays: Int = 5
): Array<JsDayStatus> {
    val periods = periodsJson.map { jsRecord ->
        PeriodRecord(
            pk = jsRecord.pk,
            startDate = jsRecord.startDate,
            endDate = jsRecord.endDate
        )
    }
    val input = CycleInput(
        periods = periods,
        periodSettings = PeriodSettings(
            manualAverageCycle = averageCycle,
            manualAverageDay = periodDays,
            autoAverageCycle = averageCycle,
            autoAverageDay = periodDays,
            isAutoCalc = false
        )
    )

    val result = PeriodCalculator.getDayStatusesForDates(input, dates.toList(), today)
    return result.map { dayStatus ->
        JsDayStatus(
            date = dayStatus.date,
            type = dayStatus.type.name,
            gap = dayStatus.gap,
            period = dayStatus.period
        )
    }.toTypedArray()
}

/**
 * 날짜 범위의 상태를 한 번에 계산
 *
 * @param periodsJson 생리 기록 배열
 * @param fromDate 시작 날짜 (julianDay)
 * @param toDate 종료 날짜 (julianDay)
 * @param today 오늘 날짜 (julianDay)
 * @param averageCycle 평균 주기
 * @param periodDays 생리 기간
 * @return 날짜 상태 배열
 */
@JsName("getDayStatuses")
fun getDayStatusesJs(
    periodsJson: Array<JsPeriodRecord>,
    fromDate: Double,
    toDate: Double,
    today: Double,
    averageCycle: Int = 28,
    periodDays: Int = 5
): Array<JsDayStatus> {
    val periods = periodsJson.map { jsRecord ->
        PeriodRecord(
            pk = jsRecord.pk,
            startDate = jsRecord.startDate,
            endDate = jsRecord.endDate
        )
    }
    val input = CycleInput(
        periods = periods,
        periodSettings = PeriodSettings(
            manualAverageCycle = averageCycle,
            manualAverageDay = periodDays,
            autoAverageCycle = averageCycle,
            autoAverageDay = periodDays,
            isAutoCalc = false
        )
    )

    val result = PeriodCalculator.getDayStatuses(input, fromDate, toDate, today)
    return result.map { dayStatus ->
        JsDayStatus(
            date = dayStatus.date,
            type = dayStatus.type.name,
            gap = dayStatus.gap,
            period = dayStatus.period
        )
    }.toTypedArray()
}

// MARK: - JavaScript 데이터 타입

/**
 * JS용 생리 기록
 */
data class JsPeriodRecord(
    val pk: String = "",
    val startDate: Double,  // julianDay
    val endDate: Double     // julianDay
)

/**
 * JS용 날짜 범위
 */
data class JsDateRange(
    val startDate: Double,  // julianDay
    val endDate: Double     // julianDay
)

/**
 * JS용 주기 정보
 */
class JsCycleInfo(
    val pk: String = "",
    val actualPeriod: JsDateRange?,
    val predictDays: Array<JsDateRange>,
    val ovulationDays: Array<JsDateRange>,
    val fertileDays: Array<JsDateRange>,
    val delayPeriod: JsDateRange?,
    val delayDays: Int,
    val period: Int,
    val pregnancyStartDate: Double? = null  // julianDay
)

/**
 * JS용 배란 테스트
 */
data class JsOvulationTest(
    val date: Double,  // julianDay
    val result: String     // "POSITIVE", "NEGATIVE", "UNCLEAR"
)

/**
 * JS용 사용자 배란일
 */
data class JsOvulationDay(
    val date: Double  // julianDay
)

/**
 * JS용 피임약 패키지
 */
data class JsPillPackage(
    val packageStart: Double  // julianDay
)

/**
 * JS용 임신 정보
 */
data class JsPregnancyInfo(
    val id: String = "",
    val babyName: String = "",
    val isDueDateDecided: Boolean = false,
    val lastTheDayDate: Double? = null,  // julianDay
    val dueDate: Double? = null,         // julianDay
    val beforePregnancyWeight: Float? = null,
    val weightUnit: String = "KG",  // "KG", "LBS", "ST"
    val isMultipleBirth: Boolean = false,
    val isMiscarriage: Boolean = false,
    val startsDate: Double,  // julianDay
    val isEnded: Boolean = false,
    val modifyDate: Double = 0.0,
    val regDate: Double = 0.0,
    val isDeleted: Boolean = false
)

/**
 * JS용 날짜 상태
 */
data class JsDayStatus(
    val date: Double,               // julianDay
    val type: String,               // "NONE", "PERIOD_ONGOING", "PERIOD_UPCOMING", "PERIOD_PREDICTED", "PERIOD_DELAYED", "PERIOD_DELAYED_OVER", "OVULATION", "FERTILE", "PREGNANCY", "EMPTY"
    val gap: Int?,                  // 생리 시작일로부터 며칠째 (null 가능)
    val period: Int                 // 주기
)

// =============================================================================
// DateUtils 편의 함수 (JavaScript에서 쉽게 사용하기 위한 래퍼)
// =============================================================================

/**
 * ISO-8601 문자열을 julianDay로 변환
 *
 * @param dateString ISO-8601 형식의 날짜 문자열 (예: "2024-03-05" 또는 "2024-03-05T00:00:00.000Z")
 * @return julianDay (Double)
 */
@JsName("stringToJulianDay")
fun stringToJulianDayJs(dateString: String): Double {
    return com.bomcomes.calculator.utils.DateUtils.stringToJulianDay(dateString)
}

/**
 * julianDay를 ISO-8601 날짜 문자열로 변환
 *
 * @param julianDay julianDay (Double)
 * @return ISO-8601 형식의 날짜 문자열 (예: "2024-03-05")
 */
@JsName("julianDayToString")
fun julianDayToStringJs(julianDay: Double): String {
    return com.bomcomes.calculator.utils.DateUtils.julianDayToString(julianDay)
}
