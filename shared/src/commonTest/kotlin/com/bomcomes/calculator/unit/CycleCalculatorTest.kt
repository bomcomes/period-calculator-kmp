package com.bomcomes.calculator

import com.bomcomes.calculator.helpers.CycleCalculator
import com.bomcomes.calculator.utils.DateUtils
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class CycleCalculatorTest {

    @Test
    fun testCalculateChildbearingAgeRange_standardCycle() {
        // 26-32일 주기: 고정 범위
        val (start, end) = CycleCalculator.calculateChildbearingAgeRange(28)
        assertEquals(7, start)
        assertEquals(18, end)
    }

    @Test
    fun testCalculateChildbearingAgeRange_shortCycle() {
        // 짧은 주기
        val (start, end) = CycleCalculator.calculateChildbearingAgeRange(25)
        assertEquals(6, start)
        assertEquals(14, end)
    }

    @Test
    fun testCalculateOvulationRange_standardCycle() {
        // 26-32일 주기: 고정 범위
        val (start, end) = CycleCalculator.calculateOvulationRange(28)
        assertEquals(12, start)
        assertEquals(14, end)
    }

    @Test
    fun testCalculateDelayDays_noDelay() {
        val lastTheDayStart = DateUtils.toJulianDay(LocalDate(2025, 1, 5))
        val fromDate = DateUtils.toJulianDay(LocalDate(2025, 1, 20))
        val toDate = DateUtils.toJulianDay(LocalDate(2025, 2, 5))
        val todayOnly = DateUtils.toJulianDay(LocalDate(2025, 1, 20))

        val result = CycleCalculator.calculateDelayDays(
            lastTheDayStart = lastTheDayStart,
            fromDate = fromDate,
            toDate = toDate,
            todayOnly = todayOnly,
            period = 30
        )
        assertEquals(0, result)
    }

    @Test
    fun testCalculateDelayDays_withDelay() {
        val lastTheDayStart = DateUtils.toJulianDay(LocalDate(2025, 1, 5))
        val fromDate = DateUtils.toJulianDay(LocalDate(2025, 2, 1))
        val toDate = DateUtils.toJulianDay(LocalDate(2025, 2, 28))
        val todayOnly = DateUtils.toJulianDay(LocalDate(2025, 2, 10)) // 36일 경과

        val result = CycleCalculator.calculateDelayDays(
            lastTheDayStart = lastTheDayStart,
            fromDate = fromDate,
            toDate = toDate,
            todayOnly = todayOnly,
            period = 30
        )
        assertEquals(7, result) // 실제 계산 결과: 7일 지연
    }
}
