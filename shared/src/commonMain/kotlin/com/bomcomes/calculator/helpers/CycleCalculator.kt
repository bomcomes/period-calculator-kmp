package com.bomcomes.calculator.helpers

import com.bomcomes.calculator.models.*
import com.bomcomes.calculator.utils.DateUtils
import kotlinx.datetime.*

/**
 * Cycle Calculator
 *
 * 주기 계산을 위한 유틸리티
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
     * 배란기 시작/종료 인덱스 계산
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
        lastTheDayStart: Double,
        fromDate: Double,
        toDate: Double,
        period: Int,
        rangeStart: Int,
        rangeEnd: Int,
        delayTheDays: Int = 0
    ): List<DateRange> {
        val actualPeriod = if (period == 0) 1 else period

        // 지연이 7일 초과면 반환하지 않음
        if (delayTheDays > 7) {
            return emptyList()
        }

        val gapStart = (fromDate - lastTheDayStart).toInt()
        val gapEnd = (toDate - lastTheDayStart).toInt()

        var quotientStart = gapStart / actualPeriod
        var remainderStart = gapStart % actualPeriod
        val quotientEnd = gapEnd / actualPeriod
        val remainderEnd = gapEnd % actualPeriod

        // fromDate가 lastTheDayStart보다 이전인 경우 조정
        if (gapStart < 0) {
            // 이전 주기에서 시작하므로 현재 주기(0)부터 검색
            quotientStart = 0
            remainderStart = 0
        }

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
        // 여러 주기를 포함하는 경우, 중간 주기들은 전체 범위를 포함하므로 항상 조건 만족
        val condition4 = quotientStart < quotientEnd

        if (condition1 || condition2 || condition3 || condition4) {
            for (index in quotientStart..quotientEnd) {
                val startDate = lastTheDayStart + actualPeriod * index + startWithDelay
                val endDate = lastTheDayStart + actualPeriod * index + endWithDelay

                // 생리 기간과 생리 예정일 같게 나오는 이슈 방어
                if (endDate < lastTheDayStart) {
                    return emptyList()
                }

                // 조회 범위와 겹치는지 확인
                if (!(startDate <= toDate && endDate >= fromDate)) {
                    continue  // 겹치지 않으면 skip
                }

                if (startDate < lastTheDayStart) {
                    results.add(DateRange(lastTheDayStart, endDate))
                } else if (!isPredict) {
                    // 배란기/가임기는 항상 추가
                    results.add(DateRange(startDate, endDate))
                } else if (startDate > lastTheDayStart + rangeEnd) {
                    // 예정일은 실제 생리 종료 다음날부터만 추가
                    results.add(DateRange(startDate, endDate))
                }
            }
        }

        // toDate를 넘어가는 범위 필터링
        // 시작일이 toDate보다 이후인 경우는 제외 (완전히 범위 밖)
        // 시작일이 toDate 이하면 포함 (범위에 걸쳐있거나 범위 내)
        return results.filter { it.startDate <= toDate }
    }

    /**
     * 지연 일수 계산
     */
    fun calculateDelayDays(
        lastTheDayStart: Double,
        fromDate: Double,
        toDate: Double,
        todayOnly: Double,
        period: Int
    ): Int {
        // Only calculate delay if today is after fromDate
        // (과거 검색이더라도 오늘 기준으로 지연 계산)
        if (todayOnly < fromDate) {
            return 0
        }

        // toDate가 과거여도 오늘 기준으로 지연 계산
        val effectiveToDate = if (toDate < todayOnly) todayOnly else toDate

        val gapEnd = (effectiveToDate - lastTheDayStart).toInt() + 1
        if (gapEnd <= period) {
            return 0
        }

        val daysGap = (todayOnly - lastTheDayStart).toInt()
        if (daysGap >= period - 1) {
            return daysGap - period + 1
        }

        return 0
    }

    /**
     * 지연 기간 계산
     */
    fun calculateDelayPeriod(
        lastTheDayStart: Double,
        fromDate: Double,
        period: Int,
        delayTheDays: Int
    ): DateRange? {
        val todayOnly = DateUtils.toJulianDay(Clock.System.todayIn(TimeZone.UTC))

        if (delayTheDays > 0) {
            if (fromDate <= todayOnly) {
                val startDate = lastTheDayStart + period
                val endDate = lastTheDayStart + period + delayTheDays - 1
                return DateRange(startDate, endDate)
            }
        }

        return null
    }
}
