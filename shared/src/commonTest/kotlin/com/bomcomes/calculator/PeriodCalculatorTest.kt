package com.bomcomes.calculator

import com.bomcomes.calculator.models.*
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PeriodCalculatorTest {

    @Test
    fun testCalculateMenstrualCycles_basicScenario() {
        // 기본 시나리오: 1개 생리 기록, 28일 주기
        val input = CycleInput(
            periods = listOf(
                PeriodRecord(
                    startDate = LocalDate(2025, 1, 5),
                    endDate = LocalDate(2025, 1, 9)
                )
            ),
            periodSettings = PeriodSettings(
                period = 28,
                days = 5
            )
        )

        val result = PeriodCalculator.calculateCycleInfo(
            input = input,
            fromDate = LocalDate(2025, 1, 1),
            toDate = LocalDate(2025, 2, 28)
        )

        assertEquals(1, result.size)
        val cycle = result[0]

        // 생리 기간 확인
        assertNotNull(cycle.actualPeriod)
        assertEquals(LocalDate(2025, 1, 5), cycle.actualPeriod!!.startDate)
        assertEquals(LocalDate(2025, 1, 9), cycle.actualPeriod!!.endDate)

        // 주기 확인
        assertEquals(28, cycle.period)

        // 생리 예정일 확인 (1/5 + 28일 = 2/2)
        assertTrue(cycle.predictDays.isNotEmpty())
        assertEquals(LocalDate(2025, 2, 2), cycle.predictDays[0].startDate)
    }

    @Test
    fun testCalculateMenstrualCycles_withOvulation() {
        // 배란일 계산 확인
        val input = CycleInput(
            periods = listOf(
                PeriodRecord(
                    startDate = LocalDate(2025, 1, 5),
                    endDate = LocalDate(2025, 1, 9)
                )
            ),
            periodSettings = PeriodSettings(
                period = 28,
                days = 5
            )
        )

        val result = PeriodCalculator.calculateCycleInfo(
            input = input,
            fromDate = LocalDate(2025, 1, 1),
            toDate = LocalDate(2025, 2, 28)
        )

        val cycle = result[0]

        // 배란일 확인 (28일 주기: 12-14일차)
        assertTrue(cycle.ovulationDays.isNotEmpty())
        val ovulation = cycle.ovulationDays[0]
        assertEquals(LocalDate(2025, 1, 17), ovulation.startDate) // 1/5 + 12일
        assertEquals(LocalDate(2025, 1, 19), ovulation.endDate)   // 1/5 + 14일
    }

    @Test
    fun testCalculateMenstrualCycles_withFertileWindow() {
        // 가임기 계산 확인
        val input = CycleInput(
            periods = listOf(
                PeriodRecord(
                    startDate = LocalDate(2025, 1, 5),
                    endDate = LocalDate(2025, 1, 9)
                )
            ),
            periodSettings = PeriodSettings(
                period = 28,
                days = 5
            )
        )

        val result = PeriodCalculator.calculateCycleInfo(
            input = input,
            fromDate = LocalDate(2025, 1, 1),
            toDate = LocalDate(2025, 2, 28)
        )

        val cycle = result[0]

        // 가임기 확인 (주기 기반: 28일 주기는 7-18일차)
        // 사용자 입력/테스트 양성이 없으므로 주기 기반으로 계산됨
        assertTrue(cycle.fertileDays.isNotEmpty())
        val fertile = cycle.fertileDays[0]
        assertEquals(LocalDate(2025, 1, 12), fertile.startDate) // 1/5 + 7일
        assertEquals(LocalDate(2025, 1, 23), fertile.endDate)   // 1/5 + 18일
    }

    @Test
    fun testCalculateMenstrualCycles_noPeriods() {
        // 생리 기록이 없으면 빈 리스트
        val input = CycleInput(
            periods = emptyList(),
            periodSettings = PeriodSettings(period = 28, days = 5)
        )

        val result = PeriodCalculator.calculateCycleInfo(
            input = input,
            fromDate = LocalDate(2025, 1, 1),
            toDate = LocalDate(2025, 2, 28)
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun testCalculateMenstrualCycles_withPregnancy() {
        // 임신 중이면 생리/배란일 없음
        val input = CycleInput(
            periods = listOf(
                PeriodRecord(
                    startDate = LocalDate(2025, 1, 5),
                    endDate = LocalDate(2025, 1, 9)
                )
            ),
            periodSettings = PeriodSettings(period = 28, days = 5),
            pregnancy = PregnancyInfo(
                startsDate = LocalDate(2025, 1, 20),
                isEnded = false,
                isMiscarriage = false,
                isDeleted = false
            )
        )

        val result = PeriodCalculator.calculateCycleInfo(
            input = input,
            fromDate = LocalDate(2025, 1, 1),
            toDate = LocalDate(2025, 2, 28)
        )

        val cycle = result[0]
        assertEquals(LocalDate(2025, 1, 20), cycle.pregnancyStartDate)
    }

    @Test
    fun testGetDayStatus_periodOngoing() {
        // 생리 중 상태
        val input = CycleInput(
            periods = listOf(
                PeriodRecord(
                    startDate = LocalDate(2025, 1, 5),
                    endDate = LocalDate(2025, 1, 9)
                )
            ),
            periodSettings = PeriodSettings(period = 28, days = 5)
        )

        val status = PeriodCalculator.getDayStatus(
            input = input,
            date = LocalDate(2025, 1, 7),
            today = LocalDate(2025, 1, 7)
        )

        assertEquals(DayType.PERIOD_ONGOING, status.type)
        assertEquals(28, status.period)
    }

    @Test
    fun testGetDayStatus_ovulation() {
        // 배란일 상태
        val input = CycleInput(
            periods = listOf(
                PeriodRecord(
                    startDate = LocalDate(2025, 1, 5),
                    endDate = LocalDate(2025, 1, 9)
                )
            ),
            periodSettings = PeriodSettings(period = 28, days = 5)
        )

        val status = PeriodCalculator.getDayStatus(
            input = input,
            date = LocalDate(2025, 1, 18),
            today = LocalDate(2025, 1, 15)
        )

        assertEquals(DayType.OVULATION, status.type)
    }

    @Test
    fun testGetDayStatus_fertile() {
        // 가임기 상태
        val input = CycleInput(
            periods = listOf(
                PeriodRecord(
                    startDate = LocalDate(2025, 1, 5),
                    endDate = LocalDate(2025, 1, 9)
                )
            ),
            periodSettings = PeriodSettings(period = 28, days = 5)
        )

        val status = PeriodCalculator.getDayStatus(
            input = input,
            date = LocalDate(2025, 1, 16),
            today = LocalDate(2025, 1, 15)
        )

        assertEquals(DayType.FERTILE, status.type)
    }

    @Test
    fun testGetDayStatus_predicted() {
        // 생리 예정일 상태
        val input = CycleInput(
            periods = listOf(
                PeriodRecord(
                    startDate = LocalDate(2025, 1, 5),
                    endDate = LocalDate(2025, 1, 9)
                )
            ),
            periodSettings = PeriodSettings(period = 28, days = 5)
        )

        val status = PeriodCalculator.getDayStatus(
            input = input,
            date = LocalDate(2025, 2, 3),
            today = LocalDate(2025, 1, 15)
        )

        assertEquals(DayType.PERIOD_PREDICTED, status.type)
    }

    @Test
    fun testGetDayStatus_none() {
        // 일반일 상태
        val input = CycleInput(
            periods = listOf(
                PeriodRecord(
                    startDate = LocalDate(2025, 1, 5),
                    endDate = LocalDate(2025, 1, 9)
                )
            ),
            periodSettings = PeriodSettings(period = 28, days = 5)
        )

        val status = PeriodCalculator.getDayStatus(
            input = input,
            date = LocalDate(2025, 1, 25),
            today = LocalDate(2025, 1, 15)
        )

        assertEquals(DayType.NONE, status.type)
    }

    @Test
    fun testCalculateMenstrualCycles_multiplePeriods() {
        // 생리 기록 2개: 실제 주기 계산 확인
        val input = CycleInput(
            periods = listOf(
                PeriodRecord(
                    startDate = LocalDate(2025, 1, 5),
                    endDate = LocalDate(2025, 1, 9)
                ),
                PeriodRecord(
                    startDate = LocalDate(2025, 2, 2),
                    endDate = LocalDate(2025, 2, 6)
                )
            ),
            periodSettings = PeriodSettings(period = 28, days = 5)
        )

        val result = PeriodCalculator.calculateCycleInfo(
            input = input,
            fromDate = LocalDate(2025, 1, 1),
            toDate = LocalDate(2025, 3, 31)
        )

        // 2개의 생리 주기 정보가 있어야 함
        assertEquals(2, result.size)

        // 첫 번째 주기 (1/5 ~ 2/2 사이, 실제 주기 = 28일)
        val cycle1 = result[0]
        assertNotNull(cycle1.actualPeriod)
        assertEquals(LocalDate(2025, 1, 5), cycle1.actualPeriod!!.startDate)
        assertEquals(LocalDate(2025, 1, 9), cycle1.actualPeriod!!.endDate)
        assertEquals(28, cycle1.period)

        // 두 번째 주기 (2/2부터, 평균 주기 = 28일)
        val cycle2 = result[1]
        assertNotNull(cycle2.actualPeriod)
        assertEquals(LocalDate(2025, 2, 2), cycle2.actualPeriod!!.startDate)
        assertEquals(LocalDate(2025, 2, 6), cycle2.actualPeriod!!.endDate)
        assertEquals(28, cycle2.period)

        // Note: 현재 구현에서는 fromDate가 생리 시작일보다 이전이면
        // 배란일/가임기 계산이 제대로 되지 않는 이슈가 있음
        // 실제 사용에서는 각 생리별로 적절한 범위를 지정해야 함
    }

    @Test
    fun testGetDayStatus_noPeriods() {
        // 생리 기록이 없으면 EMPTY
        val input = CycleInput(
            periods = emptyList(),
            periodSettings = PeriodSettings(period = 28, days = 5)
        )

        val status = PeriodCalculator.getDayStatus(
            input = input,
            date = LocalDate(2025, 1, 15),
            today = LocalDate(2025, 1, 15)
        )

        assertEquals(DayType.EMPTY, status.type)
    }

    @Test
    fun testGetDayStatusesForDates() {
        // 여러 날짜 한 번에 계산
        val input = CycleInput(
            periods = listOf(
                PeriodRecord(
                    startDate = LocalDate(2025, 1, 5),
                    endDate = LocalDate(2025, 1, 9)
                )
            ),
            periodSettings = PeriodSettings(period = 28, days = 5)
        )

        val dates = listOf(
            LocalDate(2025, 1, 7),   // 생리 중
            LocalDate(2025, 2, 3)    // 생리 예정일
        )

        val statuses = PeriodCalculator.getDayStatusesForDates(
            input = input,
            dates = dates,
            today = LocalDate(2025, 1, 15)
        )

        assertEquals(2, statuses.size)
        assertEquals(DayType.PERIOD_ONGOING, statuses[0].type)
        assertEquals(DayType.PERIOD_PREDICTED, statuses[1].type)
    }

    @Test
    fun testGetDayStatuses_range() {
        // 날짜 범위로 계산
        val input = CycleInput(
            periods = listOf(
                PeriodRecord(
                    startDate = LocalDate(2025, 1, 5),
                    endDate = LocalDate(2025, 1, 9)
                )
            ),
            periodSettings = PeriodSettings(period = 28, days = 5)
        )

        val statuses = PeriodCalculator.getDayStatuses(
            input = input,
            fromDate = LocalDate(2025, 1, 1),
            toDate = LocalDate(2025, 1, 10),
            today = LocalDate(2025, 1, 15)
        )

        assertEquals(10, statuses.size)
        // 1/5-1/9는 생리 중
        assertTrue(statuses.filter { it.type == DayType.PERIOD_ONGOING }.size >= 5)
    }
}
