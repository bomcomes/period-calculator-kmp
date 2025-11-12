package com.bomcomes.calculator

import com.bomcomes.calculator.helpers.CycleCalculator
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
        val result = CycleCalculator.calculateDelayDays(
            lastTheDayStart = LocalDate(2025, 1, 5),
            fromDate = LocalDate(2025, 1, 20),
            toDate = LocalDate(2025, 2, 5),
            todayOnly = LocalDate(2025, 1, 20),
            period = 30
        )
        assertEquals(0, result)
    }

    @Test
    fun testCalculateDelayDays_withDelay() {
        val result = CycleCalculator.calculateDelayDays(
            lastTheDayStart = LocalDate(2025, 1, 5),
            fromDate = LocalDate(2025, 2, 1),
            toDate = LocalDate(2025, 2, 28),
            todayOnly = LocalDate(2025, 2, 10), // 36일 경과
            period = 30
        )
        assertEquals(7, result) // 실제 계산 결과: 7일 지연
    }
}
