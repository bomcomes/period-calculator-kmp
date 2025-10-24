package com.bomcomes.calculator.core

import com.bomcomes.calculator.models.DateRange
import kotlinx.datetime.*

/**
 * 생리 주기 예측 계산기 (iOS PredictCalculator 이식)
 */
internal class PredictCalculator {

    /**
     * 가임기 시작/종료 일수 계산
     *
     * @param period 생리 주기
     * @return Pair<시작일수, 종료일수> (생리 시작일 기준)
     */
    fun childbearingAgeStartEnd(period: Int): Pair<Int, Int> {
        var start: Int
        var end: Int

        if (period in 26..32) {
            start = 8 - 1  // 8일차 (0-based: 7)
            end = 19 - 1   // 19일차 (0-based: 18)
        } else {
            start = period - 19
            end = period - 11
        }

        if (start < 0) start = 0
        if (end < 0) end = 0

        return Pair(start, end)
    }

    /**
     * 배란일 시작/종료 일수 계산
     *
     * @param period 생리 주기
     * @return Pair<시작일수, 종료일수> (생리 시작일 기준)
     */
    fun ovulationStartEnd(period: Int): Pair<Int, Int> {
        var start: Int
        var end: Int

        if (period in 26..32) {
            start = 13 - 1  // 13일차 (0-based: 12)
            end = 15 - 1    // 15일차 (0-based: 14)
        } else {
            start = period - 16
            end = period - 14
        }

        if (start < 0) start = 0
        if (end < 0) end = 0

        return Pair(start, end)
    }

    /**
     * 예정일/가임기/배란일 예측
     *
     * @param isPredict 예정일 여부 (true: 생리 예정일, false: 가임기/배란일)
     * @param lastTheDayStart 마지막 생리 시작일
     * @param fromDate 검색 시작일
     * @param toDate 검색 종료일
     * @param period 생리 주기
     * @param start 시작 offset (일)
     * @param end 종료 offset (일)
     * @param delayTheDays 지연 일수
     * @param isMultiple 여러 개 반환 여부
     * @return 예측된 날짜 범위 리스트
     */
    fun predict(
        isPredict: Boolean,
        lastTheDayStart: LocalDate,
        fromDate: LocalDate,
        toDate: LocalDate,
        period: Int,
        start: Int,
        end: Int,
        delayTheDays: Int = 0,
        isMultiple: Boolean = true
    ): List<DateRange> {
        val safePeriod = if (period == 0) 1 else period

        if (delayTheDays > 7) {
            return emptyList()
        }

        val gapStart = daysBetween(lastTheDayStart, fromDate)
        val gapEnd = daysBetween(lastTheDayStart, toDate)

        val quotientStart = gapStart / safePeriod
        var remainderStart = gapStart % safePeriod
        val quotientEnd = gapEnd / safePeriod
        val remainderEnd = gapEnd % safePeriod

        if (remainderEnd < remainderStart) {
            remainderStart = 0
        }

        val startWithDelay = start + delayTheDays
        val endWithDelay = end + delayTheDays

        val results = mutableListOf<DateRange>()

        if ((remainderStart <= startWithDelay && startWithDelay <= remainderEnd) ||
            (remainderStart <= endWithDelay && endWithDelay <= remainderEnd) ||
            (startWithDelay <= remainderStart && remainderEnd <= endWithDelay)) {

            for (index in quotientStart..quotientEnd) {
                val startDate = lastTheDayStart.plus(safePeriod * index + startWithDelay, DateTimeUnit.DAY)
                val endDate = lastTheDayStart.plus(safePeriod * index + endWithDelay, DateTimeUnit.DAY)

                // 생리 기간과 생리 예정일 같게 나오는 이슈 방어
                if (endDate < lastTheDayStart) {
                    return emptyList()
                }

                if (startDate < lastTheDayStart) {
                    results.add(DateRange(lastTheDayStart, endDate))
                } else if (!(isPredict && startDate == lastTheDayStart)) {
                    results.add(DateRange(startDate, endDate))
                }

                // 생리 사이의 가임기/배란기는 한번만
                if (!isMultiple) break
            }
        }

        return results
    }

    /**
     * 지연 기간 계산
     *
     * @param lastTheDayStart 마지막 생리 시작일
     * @param fromDate 검색 시작일
     * @param toDate 검색 종료일
     * @param period 생리 주기
     * @param delayTheDays 지연 일수
     * @return 지연 기간 (있으면 DateRange, 없으면 null)
     */
    fun delayPeriod(
        lastTheDayStart: LocalDate,
        fromDate: LocalDate,
        toDate: LocalDate,
        period: Int,
        delayTheDays: Int
    ): DateRange? {
        val today = Clock.System.now().toLocalDateTime(TimeZone.UTC).date

        if (delayTheDays > 0) {
            if (fromDate <= today) {
                val startDate = lastTheDayStart.plus(period, DateTimeUnit.DAY)
                val endDate = lastTheDayStart.plus(period + delayTheDays - 1, DateTimeUnit.DAY)
                return DateRange(startDate, endDate)
            }
        }
        return null
    }

    /**
     * 지연 일수 계산
     *
     * @param lastTheDayStart 마지막 생리 시작일
     * @param fromDate 검색 시작일
     * @param toDate 검색 종료일
     * @param todayOnly 오늘 날짜 (기본값: 현재 날짜)
     * @param period 생리 주기
     * @return 지연 일수
     */
    fun delayTheDays(
        lastTheDayStart: LocalDate,
        fromDate: LocalDate,
        toDate: LocalDate,
        todayOnly: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.UTC).date,
        period: Int
    ): Int {
        val gapEnd = daysBetween(lastTheDayStart, toDate) + 1
        if (gapEnd <= period) {
            return 0
        }

        val daysGap = daysBetween(lastTheDayStart, todayOnly)
        if (daysGap >= period - 1) {
            return daysGap - period + 1
        }
        return 0
    }

    /**
     * 두 날짜 사이의 일수 계산
     */
    private fun daysBetween(from: LocalDate, to: LocalDate): Int {
        return (to.toEpochDays() - from.toEpochDays()).toInt()
    }
}
