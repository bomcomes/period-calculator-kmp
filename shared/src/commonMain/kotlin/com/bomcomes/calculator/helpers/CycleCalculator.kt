package com.bomcomes.calculator.helpers

import com.bomcomes.calculator.models.*
import kotlinx.datetime.*

/**
 * Cycle Calculator
 *
 * 생리 주기 관련 핵심 계산 함수들
 */
internal object CycleCalculator {

    /**
     * 가임기 시작/종료 인덱스 계산
     */
    fun calculateChildbearingAgeRange(period: Int): Pair<Int, Int> {
        var start: Int
        var end: Int

        if (period in 26..32) {
            start = 8 - 1  // 7
            end = 19 - 1   // 18
        } else {
            start = period - 19
            end = period - 11
        }

        if (start < 0) start = 0
        if (end < 0) end = 0

        return Pair(start, end)
    }

    /**
     * 배란일 시작/종료 인덱스 계산
     */
    fun calculateOvulationRange(period: Int): Pair<Int, Int> {
        var start: Int
        var end: Int

        if (period in 26..32) {
            start = 13 - 1  // 12
            end = 15 - 1    // 14
        } else {
            start = period - 16
            end = period - 14
        }

        if (start < 0) start = 0
        if (end < 0) end = 0

        return Pair(start, end)
    }

    /**
     * 날짜 범위 내에서 주기적으로 반복되는 날짜 범위 예측
     */
    fun predictInRange(
        isPredict: Boolean,
        lastTheDayStart: LocalDate,
        fromDate: LocalDate,
        toDate: LocalDate,
        period: Int,
        rangeStart: Int,
        rangeEnd: Int,
        delayTheDays: Int = 0,
        isMultiple: Boolean = true
    ): List<DateRange> {
        val actualPeriod = if (period == 0) 1 else period

        // 지연이 7일 초과면 반환하지 않음
        if (delayTheDays > 7) {
            return emptyList()
        }

        val gapStart = lastTheDayStart.daysUntil(fromDate)
        val gapEnd = lastTheDayStart.daysUntil(toDate)

        val quotientStart = gapStart / actualPeriod
        var remainderStart = gapStart % actualPeriod
        val quotientEnd = gapEnd / actualPeriod
        val remainderEnd = gapEnd % actualPeriod

        if (remainderEnd < remainderStart) {
            remainderStart = 0
        }

        val startWithDelay = rangeStart + delayTheDays
        val endWithDelay = rangeEnd + delayTheDays

        val results = mutableListOf<DateRange>()

        // 조건 확인: 범위가 검색 기간과 겹치는지
        val condition1 = remainderStart <= startWithDelay && startWithDelay <= remainderEnd
        val condition2 = remainderStart <= endWithDelay && endWithDelay <= remainderEnd
        val condition3 = startWithDelay <= remainderStart && remainderEnd <= endWithDelay

        if (condition1 || condition2 || condition3) {
            for (index in quotientStart..quotientEnd) {
                val startDate = lastTheDayStart.plus(actualPeriod * index + startWithDelay, DateTimeUnit.DAY)
                val endDate = lastTheDayStart.plus(actualPeriod * index + endWithDelay, DateTimeUnit.DAY)

                // 생리 기간과 생리 예정일 같게 나오는 이슈 방어
                if (endDate < lastTheDayStart) {
                    return emptyList()
                }

                if (startDate < lastTheDayStart) {
                    results.add(DateRange(lastTheDayStart, endDate))
                } else if (!(isPredict && startDate == lastTheDayStart)) {
                    results.add(DateRange(startDate, endDate))
                }

                // 생리 사이의 가임기/배란기는 한번만 나오도록
                if (!isMultiple) {
                    break
                }
            }
        }

        return results
    }

    /**
     * 지연 일수 계산
     */
    fun calculateDelayDays(
        lastTheDayStart: LocalDate,
        fromDate: LocalDate,
        toDate: LocalDate,
        todayOnly: LocalDate,
        period: Int
    ): Int {
        // Only calculate delay if today is within the search range
        // If toDate is in the past (before today), this is a historical calculation, so no delay
        if (todayOnly < fromDate || todayOnly > toDate) {
            return 0
        }

        val gapEnd = lastTheDayStart.daysUntil(toDate) + 1
        if (gapEnd <= period) {
            return 0
        }

        val daysGap = lastTheDayStart.daysUntil(todayOnly)
        if (daysGap >= period - 1) {
            return daysGap - period + 1
        }

        return 0
    }

    /**
     * 지연 기간 계산
     */
    fun calculateDelayPeriod(
        lastTheDayStart: LocalDate,
        fromDate: LocalDate,
        period: Int,
        delayTheDays: Int
    ): DateRange? {
        val todayOnly = Clock.System.todayIn(TimeZone.UTC)

        if (delayTheDays > 0) {
            if (fromDate <= todayOnly) {
                val startDate = lastTheDayStart.plus(period, DateTimeUnit.DAY)
                val endDate = lastTheDayStart.plus(period + delayTheDays - 1, DateTimeUnit.DAY)
                return DateRange(startDate, endDate)
            }
        }

        return null
    }
}
