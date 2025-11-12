package com.bomcomes.calculator

import com.bomcomes.calculator.helpers.OvulationCalculator
import com.bomcomes.calculator.models.*
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OvulationCalculatorTest {

    @Test
    fun testPrepareOvulationDayRanges_consecutiveDates() {
        // 연속된 날짜들
        val dates = listOf(
            LocalDate(2025, 1, 18),
            LocalDate(2025, 1, 19),
            LocalDate(2025, 1, 20)
        )

        val result = OvulationCalculator.prepareOvulationDayRanges(dates)

        assertEquals(1, result.size)
        assertEquals(LocalDate(2025, 1, 18), result[0].startDate)
        assertEquals(LocalDate(2025, 1, 20), result[0].endDate)
    }

    @Test
    fun testPrepareOvulationDayRanges_gappedDates() {
        // 중간에 빈 날짜가 있는 경우
        val dates = listOf(
            LocalDate(2025, 1, 18),
            LocalDate(2025, 1, 19),
            LocalDate(2025, 1, 21)  // 20일이 없음
        )

        val result = OvulationCalculator.prepareOvulationDayRanges(dates)

        assertEquals(2, result.size)
        assertEquals(LocalDate(2025, 1, 18), result[0].startDate)
        assertEquals(LocalDate(2025, 1, 19), result[0].endDate)
        assertEquals(LocalDate(2025, 1, 21), result[1].startDate)
        assertEquals(LocalDate(2025, 1, 21), result[1].endDate)
    }

    @Test
    fun testPrepareOvulationDayRanges_empty() {
        val result = OvulationCalculator.prepareOvulationDayRanges(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun testCombineOvulationDates_userInputPriority() {
        // 사용자 입력이 우선순위가 높음
        val ovulationTests = listOf(
            OvulationTest(LocalDate(2025, 1, 15), TestResult.POSITIVE),
            OvulationTest(LocalDate(2025, 1, 16), TestResult.NEGATIVE)
        )

        val userOvulationDays = listOf(
            OvulationDay(LocalDate(2025, 1, 18))
        )

        val result = OvulationCalculator.combineOvulationDates(
            ovulationTests = ovulationTests,
            userOvulationDays = userOvulationDays,
            fromDate = LocalDate(2025, 1, 1),
            toDate = LocalDate(2025, 1, 31)
        )

        assertEquals(2, result.size)
        assertTrue(result.contains(LocalDate(2025, 1, 15))) // 양성 테스트
        assertTrue(result.contains(LocalDate(2025, 1, 18))) // 사용자 입력
    }

    @Test
    fun testCombineOvulationDates_onlyPositiveTests() {
        // 양성 테스트만 포함
        val ovulationTests = listOf(
            OvulationTest(LocalDate(2025, 1, 15), TestResult.POSITIVE),
            OvulationTest(LocalDate(2025, 1, 16), TestResult.NEGATIVE),
            OvulationTest(LocalDate(2025, 1, 17), TestResult.UNCLEAR)
        )

        val result = OvulationCalculator.combineOvulationDates(
            ovulationTests = ovulationTests,
            userOvulationDays = emptyList(),
            fromDate = LocalDate(2025, 1, 1),
            toDate = LocalDate(2025, 1, 31)
        )

        assertEquals(1, result.size)
        assertEquals(LocalDate(2025, 1, 15), result[0])
    }

    @Test
    fun testCalculateFertileWindowFromOvulation() {
        // 배란일 기준 가임기: 배란일 -2일 ~ +1일
        val ovulationRanges = listOf(
            DateRange(LocalDate(2025, 1, 14), LocalDate(2025, 1, 14))
        )

        val result = OvulationCalculator.calculateFertileWindowFromOvulation(ovulationRanges)

        assertEquals(1, result.size)
        assertEquals(LocalDate(2025, 1, 12), result[0].startDate) // 14 - 2
        assertEquals(LocalDate(2025, 1, 15), result[0].endDate)   // 14 + 1
    }

    @Test
    fun testCalculateFertileWindowFromOvulation_multipleRanges() {
        val ovulationRanges = listOf(
            DateRange(LocalDate(2025, 1, 14), LocalDate(2025, 1, 15))
        )

        val result = OvulationCalculator.calculateFertileWindowFromOvulation(ovulationRanges)

        assertEquals(1, result.size)
        assertEquals(LocalDate(2025, 1, 12), result[0].startDate) // 14 - 2
        assertEquals(LocalDate(2025, 1, 16), result[0].endDate)   // 15 + 1
    }

    @Test
    fun testFilterByPregnancy_noPregnancy() {
        // 임신 정보가 없으면 그대로 반환
        val ranges = listOf(
            DateRange(LocalDate(2025, 1, 1), LocalDate(2025, 1, 5))
        )

        val result = OvulationCalculator.filterByPregnancy(ranges, null)

        assertEquals(1, result.size)
        assertEquals(ranges[0], result[0])
    }

    @Test
    fun testFilterByPregnancy_beforePregnancy() {
        // 임신 시작 전의 날짜는 유지
        val pregnancy = PregnancyInfo(
            startsDate = LocalDate(2025, 2, 1),
            isEnded = false,
            isMiscarriage = false,
            isDeleted = false
        )

        val ranges = listOf(
            DateRange(LocalDate(2025, 1, 10), LocalDate(2025, 1, 15))
        )

        val result = OvulationCalculator.filterByPregnancy(ranges, pregnancy)

        assertEquals(1, result.size)
        assertEquals(LocalDate(2025, 1, 10), result[0].startDate)
        assertEquals(LocalDate(2025, 1, 15), result[0].endDate)
    }

    @Test
    fun testFilterByPregnancy_overlappingPregnancy() {
        // 임신 기간과 겹치는 경우: 임신 시작 전까지만
        val pregnancy = PregnancyInfo(
            startsDate = LocalDate(2025, 1, 15),
            isEnded = false,
            isMiscarriage = false,
            isDeleted = false
        )

        val ranges = listOf(
            DateRange(LocalDate(2025, 1, 10), LocalDate(2025, 1, 20))
        )

        val result = OvulationCalculator.filterByPregnancy(ranges, pregnancy)

        assertEquals(1, result.size)
        assertEquals(LocalDate(2025, 1, 10), result[0].startDate)
        assertEquals(LocalDate(2025, 1, 14), result[0].endDate) // 15일 전날까지
    }

    @Test
    fun testFilterByPregnancy_afterPregnancy() {
        // 임신 시작 이후의 날짜는 필터링됨
        val pregnancy = PregnancyInfo(
            startsDate = LocalDate(2025, 1, 1),
            isEnded = false,
            isMiscarriage = false,
            isDeleted = false
        )

        val ranges = listOf(
            DateRange(LocalDate(2025, 1, 10), LocalDate(2025, 1, 15))
        )

        val result = OvulationCalculator.filterByPregnancy(ranges, pregnancy)

        assertTrue(result.isEmpty())
    }
}
