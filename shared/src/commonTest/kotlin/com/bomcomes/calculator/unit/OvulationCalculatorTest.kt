package com.bomcomes.calculator

import com.bomcomes.calculator.helpers.OvulationCalculator
import com.bomcomes.calculator.models.*
import com.bomcomes.calculator.utils.DateUtils
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OvulationCalculatorTest {

    @Test
    fun testPrepareOvulationDayRanges_consecutiveDates() {
        // 연속된 날짜들
        val jan18 = DateUtils.toJulianDay(LocalDate(2025, 1, 18))
        val jan19 = DateUtils.toJulianDay(LocalDate(2025, 1, 19))
        val jan20 = DateUtils.toJulianDay(LocalDate(2025, 1, 20))

        val dates = listOf(jan18, jan19, jan20)

        val result = OvulationCalculator.prepareOvulationDayRanges(dates)

        assertEquals(1, result.size)
        assertEquals(jan18, result[0].startDate)
        assertEquals(jan20, result[0].endDate)
    }

    @Test
    fun testPrepareOvulationDayRanges_gappedDates() {
        // 중간에 빈 날짜가 있는 경우
        val jan18 = DateUtils.toJulianDay(LocalDate(2025, 1, 18))
        val jan19 = DateUtils.toJulianDay(LocalDate(2025, 1, 19))
        val jan21 = DateUtils.toJulianDay(LocalDate(2025, 1, 21))

        val dates = listOf(jan18, jan19, jan21)  // 20일이 없음

        val result = OvulationCalculator.prepareOvulationDayRanges(dates)

        assertEquals(2, result.size)
        assertEquals(jan18, result[0].startDate)
        assertEquals(jan19, result[0].endDate)
        assertEquals(jan21, result[1].startDate)
        assertEquals(jan21, result[1].endDate)
    }

    @Test
    fun testPrepareOvulationDayRanges_empty() {
        val result = OvulationCalculator.prepareOvulationDayRanges(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun testCombineOvulationDates_userInputPriority() {
        // 사용자 입력이 우선순위가 높음
        val jan15 = DateUtils.toJulianDay(LocalDate(2025, 1, 15))
        val jan16 = DateUtils.toJulianDay(LocalDate(2025, 1, 16))
        val jan18 = DateUtils.toJulianDay(LocalDate(2025, 1, 18))

        val ovulationTests = listOf(
            OvulationTest(jan15, TestResult.POSITIVE),
            OvulationTest(jan16, TestResult.NEGATIVE)
        )

        val userOvulationDays = listOf(
            OvulationDay(jan18)
        )

        val fromDate = DateUtils.toJulianDay(LocalDate(2025, 1, 1))
        val toDate = DateUtils.toJulianDay(LocalDate(2025, 1, 31))

        val result = OvulationCalculator.combineOvulationDates(
            ovulationTests = ovulationTests,
            userOvulationDays = userOvulationDays,
            fromDate = fromDate,
            toDate = toDate
        )

        assertEquals(2, result.size)
        assertTrue(result.contains(jan15)) // 양성 테스트
        assertTrue(result.contains(jan18)) // 사용자 입력
    }

    @Test
    fun testCombineOvulationDates_onlyPositiveTests() {
        // 양성 테스트만 포함
        val jan15 = DateUtils.toJulianDay(LocalDate(2025, 1, 15))
        val jan16 = DateUtils.toJulianDay(LocalDate(2025, 1, 16))
        val jan17 = DateUtils.toJulianDay(LocalDate(2025, 1, 17))

        val ovulationTests = listOf(
            OvulationTest(jan15, TestResult.POSITIVE),
            OvulationTest(jan16, TestResult.NEGATIVE),
            OvulationTest(jan17, TestResult.UNCLEAR)
        )

        val fromDate = DateUtils.toJulianDay(LocalDate(2025, 1, 1))
        val toDate = DateUtils.toJulianDay(LocalDate(2025, 1, 31))

        val result = OvulationCalculator.combineOvulationDates(
            ovulationTests = ovulationTests,
            userOvulationDays = emptyList(),
            fromDate = fromDate,
            toDate = toDate
        )

        assertEquals(1, result.size)
        assertEquals(jan15, result[0])
    }

    @Test
    fun testCalculateFertileWindowFromOvulation() {
        // 배란일 기준 가임기: 배란일 -2일 ~ +1일
        val jan14 = DateUtils.toJulianDay(LocalDate(2025, 1, 14))
        val jan12 = DateUtils.toJulianDay(LocalDate(2025, 1, 12))
        val jan15 = DateUtils.toJulianDay(LocalDate(2025, 1, 15))

        val ovulationRanges = listOf(
            DateRange(jan14, jan14)
        )

        val result = OvulationCalculator.calculateFertileWindowFromOvulation(ovulationRanges)

        assertEquals(1, result.size)
        assertEquals(jan12, result[0].startDate) // 14 - 2
        assertEquals(jan15, result[0].endDate)   // 14 + 1
    }

    @Test
    fun testCalculateFertileWindowFromOvulation_multipleRanges() {
        val jan14 = DateUtils.toJulianDay(LocalDate(2025, 1, 14))
        val jan15 = DateUtils.toJulianDay(LocalDate(2025, 1, 15))
        val jan12 = DateUtils.toJulianDay(LocalDate(2025, 1, 12))
        val jan16 = DateUtils.toJulianDay(LocalDate(2025, 1, 16))

        val ovulationRanges = listOf(
            DateRange(jan14, jan15)
        )

        val result = OvulationCalculator.calculateFertileWindowFromOvulation(ovulationRanges)

        assertEquals(1, result.size)
        assertEquals(jan12, result[0].startDate) // 14 - 2
        assertEquals(jan16, result[0].endDate)   // 15 + 1
    }

    @Test
    fun testFilterByPregnancy_noPregnancy() {
        // 임신 정보가 없으면 그대로 반환
        val jan1 = DateUtils.toJulianDay(LocalDate(2025, 1, 1))
        val jan5 = DateUtils.toJulianDay(LocalDate(2025, 1, 5))

        val ranges = listOf(
            DateRange(jan1, jan5)
        )

        val result = OvulationCalculator.filterByPregnancy(ranges, null)

        assertEquals(1, result.size)
        assertEquals(ranges[0], result[0])
    }

    @Test
    fun testFilterByPregnancy_beforePregnancy() {
        // 임신 시작 전의 날짜는 유지
        val feb1 = DateUtils.toJulianDay(LocalDate(2025, 2, 1))
        val jan10 = DateUtils.toJulianDay(LocalDate(2025, 1, 10))
        val jan15 = DateUtils.toJulianDay(LocalDate(2025, 1, 15))

        val pregnancy = PregnancyInfo(
            startsDate = feb1,
            isEnded = false,
            isMiscarriage = false,
            isDeleted = false
        )

        val ranges = listOf(
            DateRange(jan10, jan15)
        )

        val result = OvulationCalculator.filterByPregnancy(ranges, pregnancy)

        assertEquals(1, result.size)
        assertEquals(jan10, result[0].startDate)
        assertEquals(jan15, result[0].endDate)
    }

    @Test
    fun testFilterByPregnancy_overlappingPregnancy() {
        // 임신 기간과 겹치는 경우: 임신 시작 전까지만
        val jan10 = DateUtils.toJulianDay(LocalDate(2025, 1, 10))
        val jan14 = DateUtils.toJulianDay(LocalDate(2025, 1, 14))
        val jan15 = DateUtils.toJulianDay(LocalDate(2025, 1, 15))
        val jan20 = DateUtils.toJulianDay(LocalDate(2025, 1, 20))

        val pregnancy = PregnancyInfo(
            startsDate = jan15,
            isEnded = false,
            isMiscarriage = false,
            isDeleted = false
        )

        val ranges = listOf(
            DateRange(jan10, jan20)
        )

        val result = OvulationCalculator.filterByPregnancy(ranges, pregnancy)

        assertEquals(1, result.size)
        assertEquals(jan10, result[0].startDate)
        assertEquals(jan14, result[0].endDate) // 15일 전날까지
    }

    @Test
    fun testFilterByPregnancy_afterPregnancy() {
        // 임신 시작 이후의 날짜는 필터링됨
        val jan1 = DateUtils.toJulianDay(LocalDate(2025, 1, 1))
        val jan10 = DateUtils.toJulianDay(LocalDate(2025, 1, 10))
        val jan15 = DateUtils.toJulianDay(LocalDate(2025, 1, 15))

        val pregnancy = PregnancyInfo(
            startsDate = jan1,
            isEnded = false,
            isMiscarriage = false,
            isDeleted = false
        )

        val ranges = listOf(
            DateRange(jan10, jan15)
        )

        val result = OvulationCalculator.filterByPregnancy(ranges, pregnancy)

        assertTrue(result.isEmpty())
    }
}
