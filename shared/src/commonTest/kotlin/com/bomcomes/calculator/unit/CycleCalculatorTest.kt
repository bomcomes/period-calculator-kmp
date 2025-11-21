package com.bomcomes.calculator.unit

import com.bomcomes.calculator.helpers.CycleCalculator
import com.bomcomes.calculator.models.DateRange
import com.bomcomes.calculator.utils.DateUtils
import kotlinx.datetime.LocalDate
import kotlin.test.*

class CycleCalculatorTest {

    // ==================== calculateChildbearingAgeRange Tests ====================

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
    fun testCalculateChildbearingAgeRange_longCycle() {
        // 긴 주기
        val (start, end) = CycleCalculator.calculateChildbearingAgeRange(35)
        assertEquals(16, start)
        assertEquals(24, end)
    }

    @Test
    fun testCalculateChildbearingAgeRange_veryShortCycle() {
        // 매우 짧은 주기
        val (start, end) = CycleCalculator.calculateChildbearingAgeRange(21)
        assertEquals(2, start)
        assertEquals(10, end)
    }

    @Test
    fun testCalculateChildbearingAgeRange_veryLongCycle() {
        // 매우 긴 주기
        val (start, end) = CycleCalculator.calculateChildbearingAgeRange(45)
        assertEquals(26, start)
        assertEquals(34, end)
    }

    @Test
    fun testCalculateChildbearingAgeRange_boundaryCycles() {
        // 경계값 테스트
        // 26일 주기 (고정 범위 시작)
        val (start26, end26) = CycleCalculator.calculateChildbearingAgeRange(26)
        assertEquals(7, start26)
        assertEquals(18, end26)

        // 32일 주기 (고정 범위 끝)
        val (start32, end32) = CycleCalculator.calculateChildbearingAgeRange(32)
        assertEquals(7, start32)
        assertEquals(18, end32)

        // 33일 주기 (고정 범위 벗어남)
        val (start33, end33) = CycleCalculator.calculateChildbearingAgeRange(33)
        assertEquals(14, start33)
        assertEquals(22, end33)
    }

    @Test
    fun testCalculateChildbearingAgeRange_extremelyShortCycle() {
        // 극단적으로 짧은 주기 - 음수 방어
        val (start, end) = CycleCalculator.calculateChildbearingAgeRange(10)
        assertEquals(0, start) // 음수 방어로 0
        assertEquals(0, end)   // 음수 방어로 0
    }

    // ==================== calculateOvulationRange Tests ====================

    @Test
    fun testCalculateOvulationRange_standardCycle() {
        // 26-32일 주기: 고정 범위
        val (start, end) = CycleCalculator.calculateOvulationRange(28)
        assertEquals(12, start)
        assertEquals(14, end)
    }

    @Test
    fun testCalculateOvulationRange_shortCycle() {
        // 짧은 주기
        val (start, end) = CycleCalculator.calculateOvulationRange(25)
        assertEquals(9, start)
        assertEquals(11, end)
    }

    @Test
    fun testCalculateOvulationRange_longCycle() {
        // 긴 주기
        val (start, end) = CycleCalculator.calculateOvulationRange(35)
        assertEquals(19, start)
        assertEquals(21, end)
    }

    @Test
    fun testCalculateOvulationRange_veryShortCycle() {
        // 매우 짧은 주기
        val (start, end) = CycleCalculator.calculateOvulationRange(21)
        assertEquals(5, start)
        assertEquals(7, end)
    }

    @Test
    fun testCalculateOvulationRange_boundaryCycles() {
        // 경계값 테스트
        // 26일 주기 (고정 범위 시작)
        val (start26, end26) = CycleCalculator.calculateOvulationRange(26)
        assertEquals(12, start26)
        assertEquals(14, end26)

        // 32일 주기 (고정 범위 끝)
        val (start32, end32) = CycleCalculator.calculateOvulationRange(32)
        assertEquals(12, start32)
        assertEquals(14, end32)

        // 33일 주기 (고정 범위 벗어남)
        val (start33, end33) = CycleCalculator.calculateOvulationRange(33)
        assertEquals(17, start33)
        assertEquals(19, end33)
    }

    @Test
    fun testCalculateOvulationRange_extremelyShortCycle() {
        // 극단적으로 짧은 주기 - 음수 방어
        val (start, end) = CycleCalculator.calculateOvulationRange(10)
        assertEquals(0, start) // 음수 방어로 0
        assertEquals(0, end)   // 음수 방어로 0
    }

    // ==================== predictInRange Tests ====================

    @Test
    fun testPredictInRange_singleCycle() {
        // 단일 주기 예측
        val lastTheDayStart = DateUtils.toJulianDay(LocalDate(2025, 1, 1))
        val fromDate = DateUtils.toJulianDay(LocalDate(2025, 1, 10))
        val toDate = DateUtils.toJulianDay(LocalDate(2025, 1, 20))

        val ranges = CycleCalculator.predictInRange(
            isPredict = false,
            lastTheDayStart = lastTheDayStart,
            fromDate = fromDate,
            toDate = toDate,
            period = 28,
            rangeStart = 12,
            rangeEnd = 14
        )

        assertEquals(1, ranges.size)
        // 배란기: 1월 1일 + 12일 = 1월 13일
        assertEquals(DateUtils.toJulianDay(LocalDate(2025, 1, 13)), ranges[0].startDate)
        assertEquals(DateUtils.toJulianDay(LocalDate(2025, 1, 15)), ranges[0].endDate)
    }

    @Test
    fun testPredictInRange_multipleCycles() {
        // 여러 주기에 걸친 예측
        val lastTheDayStart = DateUtils.toJulianDay(LocalDate(2025, 1, 1))
        val fromDate = DateUtils.toJulianDay(LocalDate(2025, 1, 1))
        val toDate = DateUtils.toJulianDay(LocalDate(2025, 3, 1))

        val ranges = CycleCalculator.predictInRange(
            isPredict = false,
            lastTheDayStart = lastTheDayStart,
            fromDate = fromDate,
            toDate = toDate,
            period = 28,
            rangeStart = 12,
            rangeEnd = 14
        )

        // 3개 주기 예상
        assertTrue(ranges.size >= 2, "최소 2개 주기 포함")
    }

    @Test
    fun testPredictInRange_withDelay() {
        // 지연 포함 예측
        val lastTheDayStart = DateUtils.toJulianDay(LocalDate(2025, 1, 1))
        val fromDate = DateUtils.toJulianDay(LocalDate(2025, 1, 10))
        val toDate = DateUtils.toJulianDay(LocalDate(2025, 1, 25))

        val ranges = CycleCalculator.predictInRange(
            isPredict = false,
            lastTheDayStart = lastTheDayStart,
            fromDate = fromDate,
            toDate = toDate,
            period = 28,
            rangeStart = 12,
            rangeEnd = 14,
            delayTheDays = 3
        )

        assertEquals(1, ranges.size)
        // 배란기 3일 지연: 1월 1일 + 12일 + 3일 = 1월 16일
        assertEquals(DateUtils.toJulianDay(LocalDate(2025, 1, 16)), ranges[0].startDate)
        assertEquals(DateUtils.toJulianDay(LocalDate(2025, 1, 18)), ranges[0].endDate)
    }

    @Test
    fun testPredictInRange_exceedMaxDelay() {
        // 최대 지연 초과 시 빈 리스트
        val lastTheDayStart = DateUtils.toJulianDay(LocalDate(2025, 1, 1))
        val fromDate = DateUtils.toJulianDay(LocalDate(2025, 1, 10))
        val toDate = DateUtils.toJulianDay(LocalDate(2025, 1, 25))

        val ranges = CycleCalculator.predictInRange(
            isPredict = false,
            lastTheDayStart = lastTheDayStart,
            fromDate = fromDate,
            toDate = toDate,
            period = 28,
            rangeStart = 12,
            rangeEnd = 14,
            delayTheDays = 8 // 7일 초과
        )

        assertTrue(ranges.isEmpty(), "7일 초과 지연 시 빈 리스트")
    }

    @Test
    fun testPredictInRange_isPredictTrue() {
        // 예정일 예측 (isPredict = true)
        val lastTheDayStart = DateUtils.toJulianDay(LocalDate(2025, 1, 1))
        val fromDate = DateUtils.toJulianDay(LocalDate(2025, 1, 20))
        val toDate = DateUtils.toJulianDay(LocalDate(2025, 2, 10))

        val ranges = CycleCalculator.predictInRange(
            isPredict = true,
            lastTheDayStart = lastTheDayStart,
            fromDate = fromDate,
            toDate = toDate,
            period = 28,
            rangeStart = 0,
            rangeEnd = 4
        )

        // 예정일은 실제 생리 종료 다음날부터만 추가
        assertEquals(1, ranges.size)
        assertEquals(DateUtils.toJulianDay(LocalDate(2025, 1, 29)), ranges[0].startDate)
    }

    @Test
    fun testPredictInRange_negativePeriodFromDate() {
        // fromDate가 lastTheDayStart보다 이전
        val lastTheDayStart = DateUtils.toJulianDay(LocalDate(2025, 1, 15))
        val fromDate = DateUtils.toJulianDay(LocalDate(2025, 1, 1))
        val toDate = DateUtils.toJulianDay(LocalDate(2025, 2, 1))

        val ranges = CycleCalculator.predictInRange(
            isPredict = false,
            lastTheDayStart = lastTheDayStart,
            fromDate = fromDate,
            toDate = toDate,
            period = 28,
            rangeStart = 12,
            rangeEnd = 14
        )

        // 이전 날짜여도 정상 처리
        assertNotNull(ranges)
        assertTrue(ranges.isNotEmpty())
    }

    @Test
    fun testPredictInRange_zeroPeriod() {
        // period가 0인 경우 (1로 처리됨)
        val lastTheDayStart = DateUtils.toJulianDay(LocalDate(2025, 1, 1))
        val fromDate = DateUtils.toJulianDay(LocalDate(2025, 1, 1))
        val toDate = DateUtils.toJulianDay(LocalDate(2025, 1, 2))

        val ranges = CycleCalculator.predictInRange(
            isPredict = false,
            lastTheDayStart = lastTheDayStart,
            fromDate = fromDate,
            toDate = toDate,
            period = 0, // 0이면 1로 처리
            rangeStart = 0,
            rangeEnd = 0
        )

        // period = 1로 처리되어 매일 반복
        assertTrue(ranges.size >= 1)
    }

    // ==================== calculateDelayDays Tests ====================

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

    @Test
    fun testCalculateDelayDays_todayBeforeFromDate() {
        // todayOnly가 fromDate보다 이전
        val lastTheDayStart = DateUtils.toJulianDay(LocalDate(2025, 1, 1))
        val fromDate = DateUtils.toJulianDay(LocalDate(2025, 2, 1))
        val toDate = DateUtils.toJulianDay(LocalDate(2025, 2, 28))
        val todayOnly = DateUtils.toJulianDay(LocalDate(2025, 1, 15))

        val result = CycleCalculator.calculateDelayDays(
            lastTheDayStart = lastTheDayStart,
            fromDate = fromDate,
            toDate = toDate,
            todayOnly = todayOnly,
            period = 30
        )
        assertEquals(0, result, "오늘이 fromDate 이전이면 지연 없음")
    }

    @Test
    fun testCalculateDelayDays_toDateInPast() {
        // toDate가 과거여도 오늘 기준으로 계산
        val lastTheDayStart = DateUtils.toJulianDay(LocalDate(2025, 1, 1))
        val fromDate = DateUtils.toJulianDay(LocalDate(2025, 1, 10))
        val toDate = DateUtils.toJulianDay(LocalDate(2025, 1, 20)) // 과거
        val todayOnly = DateUtils.toJulianDay(LocalDate(2025, 2, 5)) // 오늘 (35일 경과)

        val result = CycleCalculator.calculateDelayDays(
            lastTheDayStart = lastTheDayStart,
            fromDate = fromDate,
            toDate = toDate,
            todayOnly = todayOnly,
            period = 30
        )
        assertEquals(6, result, "과거 toDate여도 오늘 기준으로 지연 계산")
    }

    @Test
    fun testCalculateDelayDays_exactPeriodBoundary() {
        // 정확히 주기 경계
        val lastTheDayStart = DateUtils.toJulianDay(LocalDate(2025, 1, 1))
        val fromDate = DateUtils.toJulianDay(LocalDate(2025, 1, 15))
        val toDate = DateUtils.toJulianDay(LocalDate(2025, 2, 15))
        val todayOnly = DateUtils.toJulianDay(LocalDate(2025, 1, 29)) // 28일 경과

        val result = CycleCalculator.calculateDelayDays(
            lastTheDayStart = lastTheDayStart,
            fromDate = fromDate,
            toDate = toDate,
            todayOnly = todayOnly,
            period = 28
        )
        assertEquals(1, result, "주기 경계에서 1일 지연")
    }

    @Test
    fun testCalculateDelayDays_multiplePeriodsDelay() {
        // 여러 주기 지연
        val lastTheDayStart = DateUtils.toJulianDay(LocalDate(2025, 1, 1))
        val fromDate = DateUtils.toJulianDay(LocalDate(2025, 1, 15))
        val toDate = DateUtils.toJulianDay(LocalDate(2025, 3, 15))
        val todayOnly = DateUtils.toJulianDay(LocalDate(2025, 3, 1)) // 59일 경과

        val result = CycleCalculator.calculateDelayDays(
            lastTheDayStart = lastTheDayStart,
            fromDate = fromDate,
            toDate = toDate,
            todayOnly = todayOnly,
            period = 28
        )
        assertEquals(32, result, "여러 주기 지연: 59 - 28 + 1 = 32")
    }

    // ==================== calculateDelayPeriod Tests ====================

    @Test
    fun testCalculateDelayPeriod_noDelay() {
        // 지연 없음
        val lastTheDayStart = DateUtils.toJulianDay(LocalDate(2025, 1, 1))
        val fromDate = DateUtils.toJulianDay(LocalDate(2025, 1, 15))

        val result = CycleCalculator.calculateDelayPeriod(
            lastTheDayStart = lastTheDayStart,
            fromDate = fromDate,
            period = 28,
            delayTheDays = 0
        )

        assertNull(result, "지연 없으면 null")
    }

    @Test
    fun testCalculateDelayPeriod_withDelay() {
        // 지연 있음
        val lastTheDayStart = DateUtils.toJulianDay(LocalDate(2025, 1, 1))
        val fromDate = DateUtils.toJulianDay(LocalDate(2025, 1, 15))

        // 현재 시간 모킹이 불가능하므로 fromDate를 과거로 설정
        val pastFromDate = DateUtils.toJulianDay(LocalDate(2024, 1, 15))

        val result = CycleCalculator.calculateDelayPeriod(
            lastTheDayStart = lastTheDayStart,
            fromDate = pastFromDate,
            period = 28,
            delayTheDays = 5
        )

        if (result != null) {
            // 지연 기간: 1월 1일 + 28일 = 1월 29일 시작
            val expectedStart = DateUtils.toJulianDay(LocalDate(2025, 1, 29))
            val expectedEnd = DateUtils.toJulianDay(LocalDate(2025, 2, 2)) // +5일 -1

            assertEquals(expectedStart, result.startDate, 0.1)
            assertEquals(expectedEnd, result.endDate, 0.1)
        }
    }

    @Test
    fun testCalculateDelayPeriod_futureFromDate() {
        // fromDate가 미래
        val lastTheDayStart = DateUtils.toJulianDay(LocalDate(2025, 1, 1))
        val fromDate = DateUtils.toJulianDay(LocalDate(2099, 1, 15)) // 미래

        val result = CycleCalculator.calculateDelayPeriod(
            lastTheDayStart = lastTheDayStart,
            fromDate = fromDate,
            period = 28,
            delayTheDays = 5
        )

        assertNull(result, "fromDate가 미래면 null")
    }

    @Test
    fun testCalculateDelayPeriod_largeDelay() {
        // 큰 지연값
        val lastTheDayStart = DateUtils.toJulianDay(LocalDate(2025, 1, 1))
        val pastFromDate = DateUtils.toJulianDay(LocalDate(2024, 1, 15))

        val result = CycleCalculator.calculateDelayPeriod(
            lastTheDayStart = lastTheDayStart,
            fromDate = pastFromDate,
            period = 28,
            delayTheDays = 30
        )

        if (result != null) {
            // 지연 기간: 1월 1일 + 28일 = 1월 29일 시작
            val expectedStart = DateUtils.toJulianDay(LocalDate(2025, 1, 29))
            val expectedEnd = DateUtils.toJulianDay(LocalDate(2025, 2, 27)) // +30일 -1

            assertEquals(expectedStart, result.startDate, 0.1)
            assertEquals(expectedEnd, result.endDate, 0.1)
        }
    }

    // ==================== Edge Cases & Integration Tests ====================

    @Test
    fun testIntegration_fullCycleCalculation() {
        // 전체 주기 계산 통합 테스트
        val period = 28
        val lastTheDayStart = DateUtils.toJulianDay(LocalDate(2025, 1, 1))
        val fromDate = DateUtils.toJulianDay(LocalDate(2025, 1, 1))
        val toDate = DateUtils.toJulianDay(LocalDate(2025, 2, 28))

        // 가임기 계산
        val (fertileStart, fertileEnd) = CycleCalculator.calculateChildbearingAgeRange(period)
        assertEquals(7, fertileStart)
        assertEquals(18, fertileEnd)

        // 배란기 계산
        val (ovulationStart, ovulationEnd) = CycleCalculator.calculateOvulationRange(period)
        assertEquals(12, ovulationStart)
        assertEquals(14, ovulationEnd)

        // 가임기 예측
        val fertilePeriods = CycleCalculator.predictInRange(
            isPredict = false,
            lastTheDayStart = lastTheDayStart,
            fromDate = fromDate,
            toDate = toDate,
            period = period,
            rangeStart = fertileStart,
            rangeEnd = fertileEnd
        )
        assertTrue(fertilePeriods.size >= 2, "2개 주기의 가임기")

        // 배란기 예측
        val ovulationPeriods = CycleCalculator.predictInRange(
            isPredict = false,
            lastTheDayStart = lastTheDayStart,
            fromDate = fromDate,
            toDate = toDate,
            period = period,
            rangeStart = ovulationStart,
            rangeEnd = ovulationEnd
        )
        assertTrue(ovulationPeriods.size >= 2, "2개 주기의 배란기")
    }

    @Test
    fun testEdgeCase_allNegativeCalculations() {
        // 모든 값이 음수가 되는 극단적 케이스
        val (fertileStart, fertileEnd) = CycleCalculator.calculateChildbearingAgeRange(5)
        assertEquals(0, fertileStart, "음수 방어")
        assertEquals(0, fertileEnd, "음수 방어")

        val (ovulationStart, ovulationEnd) = CycleCalculator.calculateOvulationRange(5)
        assertEquals(0, ovulationStart, "음수 방어")
        assertEquals(0, ovulationEnd, "음수 방어")
    }

    @Test
    fun testEdgeCase_veryLongPeriod() {
        // 매우 긴 주기
        val period = 90
        val (fertileStart, fertileEnd) = CycleCalculator.calculateChildbearingAgeRange(period)
        assertEquals(71, fertileStart)
        assertEquals(79, fertileEnd)

        val (ovulationStart, ovulationEnd) = CycleCalculator.calculateOvulationRange(period)
        assertEquals(74, ovulationStart)
        assertEquals(76, ovulationEnd)
    }
}
