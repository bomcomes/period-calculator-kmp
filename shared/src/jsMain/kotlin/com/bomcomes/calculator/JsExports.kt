@file:JsExport

package com.bomcomes.calculator

import com.bomcomes.calculator.models.*
import com.bomcomes.calculator.repository.JsRepositoryWrapper
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.promise

// =============================================================================
// Library Info
// =============================================================================

/**
 * Get library version string
 */
@JsName("getLibraryVersion")
fun getLibraryVersion(): String = Version.getVersionString()

// =============================================================================
// Cycle Calculation API
// =============================================================================

/**
 * Calculate cycle information for a date range
 *
 * Note: JavaScript doesn't support suspend functions with @JsExport.
 * This function returns a Promise in JavaScript.
 */
@JsName("calculateCycleInfo")
fun calculateCycleInfoJs(
    repository: JsRepositoryWrapper,
    fromDate: Double,
    toDate: Double,
    today: Double? = null
): dynamic = MainScope().promise {
    val cycles = PeriodCalculator.calculateCycleInfo(repository, fromDate, toDate, today)

    cycles.map { cycleInfo ->
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

// =============================================================================
// Day Status API
// =============================================================================

/**
 * Get status of a specific date
 */
@JsName("getDayStatus")
fun getDayStatusJs(
    repository: JsRepositoryWrapper,
    targetDate: Double,
    today: Double
): dynamic = MainScope().promise {
    // Get all periods from repository (getDayStatus doesn't have a repository version)
    val allPeriods = repository.getPeriods(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY)
    val periodSettings = repository.getPeriodSettings()
    val pillSettings = repository.getPillSettings()

    // Create CycleInput for getDayStatus
    val input = CycleInput(
        periods = allPeriods,
        periodSettings = periodSettings,
        pillSettings = pillSettings
    )

    val result = PeriodCalculator.getDayStatus(input, targetDate, today)

    JsDayStatus(
        date = result.date,
        type = result.type.name,
        gap = result.gap,
        period = result.period
    )
}

/**
 * Get statuses for multiple specific dates
 */
@JsName("getDayStatusesForDates")
fun getDayStatusesForDatesJs(
    repository: JsRepositoryWrapper,
    dates: Array<Double>,
    today: Double
): dynamic = MainScope().promise {
    // Get all periods from repository (getDayStatusesForDates doesn't have a repository version)
    val allPeriods = repository.getPeriods(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY)
    val periodSettings = repository.getPeriodSettings()
    val pillSettings = repository.getPillSettings()

    // Create CycleInput for getDayStatusesForDates
    val input = CycleInput(
        periods = allPeriods,
        periodSettings = periodSettings,
        pillSettings = pillSettings
    )

    val result = PeriodCalculator.getDayStatusesForDates(input, dates.toList(), today)

    result.map { dayStatus ->
        JsDayStatus(
            date = dayStatus.date,
            type = dayStatus.type.name,
            gap = dayStatus.gap,
            period = dayStatus.period
        )
    }.toTypedArray()
}

/**
 * Get statuses for a date range
 */
@JsName("getDayStatuses")
fun getDayStatusesJs(
    repository: JsRepositoryWrapper,
    fromDate: Double,
    toDate: Double,
    today: Double
): dynamic = MainScope().promise {
    // Get all periods from repository (getDayStatuses doesn't have a repository version)
    val allPeriods = repository.getPeriods(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY)
    val periodSettings = repository.getPeriodSettings()
    val pillSettings = repository.getPillSettings()

    // Create CycleInput for getDayStatuses
    val input = CycleInput(
        periods = allPeriods,
        periodSettings = periodSettings,
        pillSettings = pillSettings
    )

    val result = PeriodCalculator.getDayStatuses(input, fromDate, toDate, today)

    result.map { dayStatus ->
        JsDayStatus(
            date = dayStatus.date,
            type = dayStatus.type.name,
            gap = dayStatus.gap,
            period = dayStatus.period
        )
    }.toTypedArray()
}

// =============================================================================
// Data Types (JavaScript/TypeScript)
// =============================================================================

/**
 * Period record
 */
data class JsPeriodRecord(
    val pk: String = "",
    val startDate: Double,  // julianDay
    val endDate: Double     // julianDay
)

/**
 * Date range
 */
data class JsDateRange(
    val startDate: Double,  // julianDay
    val endDate: Double     // julianDay
)

/**
 * Cycle information
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
 * Day status
 */
data class JsDayStatus(
    val date: Double,   // julianDay
    val type: String,   // "NONE", "PERIOD_ONGOING", "PERIOD_UPCOMING", etc.
    val gap: Int?,      // Days from period start (nullable)
    val period: Int     // Cycle length
)

// =============================================================================
// Date Utilities
// =============================================================================

/**
 * Convert ISO-8601 string to julianDay
 * Supports: "2024-03-05" or "2024-03-05T00:00:00.000Z"
 */
@JsName("stringToJulianDay")
fun stringToJulianDayJs(dateString: String): Double {
    return com.bomcomes.calculator.utils.DateUtils.stringToJulianDay(dateString)
}

/**
 * Convert julianDay to ISO-8601 string
 * Returns: "2024-03-05"
 */
@JsName("julianDayToString")
fun julianDayToStringJs(julianDay: Double): String {
    return com.bomcomes.calculator.utils.DateUtils.julianDayToString(julianDay)
}
