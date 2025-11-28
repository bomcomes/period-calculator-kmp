package com.bomcomes.calculator.integration

import com.bomcomes.calculator.PeriodCalculator
import com.bomcomes.calculator.models.*
import com.bomcomes.calculator.repository.InMemoryPeriodRepository
import com.bomcomes.calculator.utils.DateUtils
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.*

/**
 * 크나우스 오기노법 검증 테스트 - 짧은 주기 (11개)
 *
 * 문서 참조: test-cases/docs/02-short-cycle-under-25.md
 *
 * 크나우스 오기노법 (25일 이하 짧은 주기):
 * - 가임기 시작일: 주기 - 19일 (음수 시 0)
 * - 가임기 종료일: 주기 - 11일
 * - 배란기 시작일: 주기 - 16일 (음수 시 0)
 * - 배란기 종료일: 주기 - 14일
 * - 계산 실패 확률 9%
 */
class ShortCycleUnder25Test {
    companion object {
        // 공통 생리 기록 (짧은 주기: 22, 18일)
        val PERIOD_1_START = LocalDate(2025, 1, 1)
        val PERIOD_1_END = LocalDate(2025, 1, 5)
        val PERIOD_2_START = LocalDate(2025, 1, 23)
        val PERIOD_2_END = LocalDate(2025, 1, 27)
        val PERIOD_3_START = LocalDate(2025, 2, 10)
        val PERIOD_3_END = LocalDate(2025, 2, 14)

        // 조회 기준일
        val DEFAULT_TODAY = LocalDate(2025, 2, 20)

        // 주기 설정 (자동 계산 사용)
        const val MANUAL_AVERAGE_CYCLE = 28
        const val MANUAL_AVERAGE_DAY = 5
        const val AUTO_AVERAGE_CYCLE = 22  // 18일 주기는 < 20이므로 제외, 평균 = 22일
        const val AUTO_AVERAGE_DAY = 5
        const val IS_AUTO_CALC = true
    }

    /**
     * 공통 데이터 설정
     * - Period 1, 2, 3 추가
     * - 자동 계산 활성화
     * - 피임약 비활성화
     */
    private fun setupCommonData(repository: InMemoryPeriodRepository) {
        // 생리 기록 3개 추가
        repository.addPeriod(PeriodRecord(
            pk = "1",
            startDate = DateUtils.toJulianDay(PERIOD_1_START),
            endDate = DateUtils.toJulianDay(PERIOD_1_END)
        ))

        repository.addPeriod(PeriodRecord(
            pk = "2",
            startDate = DateUtils.toJulianDay(PERIOD_2_START),
            endDate = DateUtils.toJulianDay(PERIOD_2_END)
        ))

        repository.addPeriod(PeriodRecord(
            pk = "3",
            startDate = DateUtils.toJulianDay(PERIOD_3_START),
            endDate = DateUtils.toJulianDay(PERIOD_3_END)
        ))

        // 생리 주기 설정 (자동 계산 사용)
        repository.setPeriodSettings(PeriodSettings(
            manualAverageCycle = MANUAL_AVERAGE_CYCLE,
            manualAverageDay = MANUAL_AVERAGE_DAY,
            autoAverageCycle = AUTO_AVERAGE_CYCLE,
            autoAverageDay = AUTO_AVERAGE_DAY,
            isAutoCalc = IS_AUTO_CALC
        ))

        // 피임약 설정 (비활성화)
        repository.setPillSettings(PillSettings(isCalculatingWithPill = false))
    }

    /**
     * TC-02-01: 1일 조회 (마지막 생리 이후)
     * 마지막 생리 이후 특정 날짜의 가임기 상태를 정확히 검증
     */
    @Test
    fun testTC_02_01_singleDayQueryAfterLastPeriod() = runTest {
        val repository = InMemoryPeriodRepository()
        setupCommonData(repository)

        val fromDate = LocalDate(2025, 2, 20)
        val toDate = LocalDate(2025, 2, 20)
        val today = DEFAULT_TODAY

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository = repository,
            fromDate = DateUtils.toJulianDay(fromDate),
            toDate = DateUtils.toJulianDay(toDate),
            today = DateUtils.toJulianDay(today)
        )

        // 주기 개수: 1개
        assertEquals(1, cycles.size, "주기 개수: 1개")

        // 주기 1 (pk=3)
        val cycle1 = cycles.first()
        assertEquals("3", cycle1.pk)
        assertEquals(
            DateUtils.toJulianDay(PERIOD_3_START),
            cycle1.actualPeriod?.startDate
        )
        assertEquals(
            DateUtils.toJulianDay(PERIOD_3_END),
            cycle1.actualPeriod?.endDate
        )

        // 지연 정보
        assertEquals(0, cycle1.delayTheDays)
        assertNull(cycle1.delayDay)

        // 생리 예정일: 없음
        assertEquals(0, cycle1.predictDays.size)

        // 가임기: [2025-02-13 ~ 2025-02-21]
        assertEquals(1, cycle1.fertileDays.size)
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 13)),
            cycle1.fertileDays[0].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 21)),
            cycle1.fertileDays[0].endDate
        )

        // 배란기: 없음
        assertEquals(0, cycle1.ovulationDays.size)

        // 주기 정보
        assertEquals(22, cycle1.period)
        assertFalse(cycle1.isOvulationPeriodUserInput)
        assertNull(cycle1.thePillPeriod)
        assertFalse(cycle1.isContinuousPillUsage)
    }

    /**
     * TC-02-02: 1주일 조회 (마지막 생리 이후)
     * 마지막 생리 이후 1주일 조회 시 배란기가 정확히 표시되는지 검증
     */
    @Test
    fun testTC_02_02_weekQueryAfterLastPeriod() = runTest {
        val repository = InMemoryPeriodRepository()
        setupCommonData(repository)

        val fromDate = LocalDate(2025, 2, 16)
        val toDate = LocalDate(2025, 2, 22)
        val today = DEFAULT_TODAY

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository = repository,
            fromDate = DateUtils.toJulianDay(fromDate),
            toDate = DateUtils.toJulianDay(toDate),
            today = DateUtils.toJulianDay(today)
        )

        // 주기 개수: 1개
        assertEquals(1, cycles.size, "주기 개수: 1개")

        // 주기 1 (pk=3)
        val cycle1 = cycles.first()
        assertEquals("3", cycle1.pk)
        assertEquals(
            DateUtils.toJulianDay(PERIOD_3_START),
            cycle1.actualPeriod?.startDate
        )
        assertEquals(
            DateUtils.toJulianDay(PERIOD_3_END),
            cycle1.actualPeriod?.endDate
        )

        // 지연 정보
        assertEquals(0, cycle1.delayTheDays)
        assertNull(cycle1.delayDay)

        // 생리 예정일: 없음
        assertEquals(0, cycle1.predictDays.size)

        // 가임기: [2025-02-13 ~ 2025-02-21]
        assertEquals(1, cycle1.fertileDays.size)
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 13)),
            cycle1.fertileDays[0].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 21)),
            cycle1.fertileDays[0].endDate
        )

        // 배란기: [2025-02-16 ~ 2025-02-18]
        assertEquals(1, cycle1.ovulationDays.size)
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 16)),
            cycle1.ovulationDays[0].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 18)),
            cycle1.ovulationDays[0].endDate
        )

        // 주기 정보
        assertEquals(22, cycle1.period)
    }

    /**
     * TC-02-03: 1개월 조회 (마지막 생리 이후)
     * 마지막 생리(Period 3) 전체 주기를 조회하여 실제 생리, 배란기, 가임기, 다음 예정일이 모두 정확히 표시되는지 검증
     */
    @Test
    fun testTC_02_03_monthQueryAfterLastPeriod() = runTest {
        val repository = InMemoryPeriodRepository()
        setupCommonData(repository)

        val fromDate = LocalDate(2025, 2, 10)
        val toDate = LocalDate(2025, 3, 9)
        val today = DEFAULT_TODAY

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository = repository,
            fromDate = DateUtils.toJulianDay(fromDate),
            toDate = DateUtils.toJulianDay(toDate),
            today = DateUtils.toJulianDay(today)
        )

        // 주기 개수: 1개
        assertEquals(1, cycles.size, "주기 개수: 1개")

        // 주기 1 (pk=3)
        val cycle1 = cycles.first()
        assertEquals("3", cycle1.pk)
        assertEquals(
            DateUtils.toJulianDay(PERIOD_3_START),
            cycle1.actualPeriod?.startDate
        )
        assertEquals(
            DateUtils.toJulianDay(PERIOD_3_END),
            cycle1.actualPeriod?.endDate
        )

        // 지연 정보
        assertEquals(0, cycle1.delayTheDays)
        assertNull(cycle1.delayDay)

        // 생리 예정일: [2025-03-04 ~ 2025-03-08]
        assertEquals(1, cycle1.predictDays.size)
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 4)),
            cycle1.predictDays[0].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 8)),
            cycle1.predictDays[0].endDate
        )

        // 가임기: [2025-02-13 ~ 2025-02-21, 2025-03-07 ~ 2025-03-15]
        assertEquals(2, cycle1.fertileDays.size)
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 13)),
            cycle1.fertileDays[0].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 21)),
            cycle1.fertileDays[0].endDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 7)),
            cycle1.fertileDays[1].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 15)),
            cycle1.fertileDays[1].endDate
        )

        // 배란기: [2025-02-16 ~ 2025-02-18]
        assertEquals(1, cycle1.ovulationDays.size)
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 16)),
            cycle1.ovulationDays[0].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 18)),
            cycle1.ovulationDays[0].endDate
        )

        // 주기 정보
        assertEquals(22, cycle1.period)
    }

    /**
     * TC-02-04: 1일 조회 (과거)
     * Period 2와 Period 3 사이 배란기 특정 날짜를 정확히 검증
     */
    @Test
    fun testTC_02_04_singleDayQueryPast() = runTest {
        val repository = InMemoryPeriodRepository()
        setupCommonData(repository)

        val fromDate = LocalDate(2025, 1, 23)
        val toDate = LocalDate(2025, 1, 23)
        val today = DEFAULT_TODAY

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository = repository,
            fromDate = DateUtils.toJulianDay(fromDate),
            toDate = DateUtils.toJulianDay(toDate),
            today = DateUtils.toJulianDay(today)
        )

        // 주기 개수: 1개
        assertEquals(1, cycles.size, "주기 개수: 1개")

        // 주기 1 (pk=2)
        val cycle1 = cycles.first()
        assertEquals("2", cycle1.pk)
        assertEquals(
            DateUtils.toJulianDay(PERIOD_2_START),
            cycle1.actualPeriod?.startDate
        )
        assertEquals(
            DateUtils.toJulianDay(PERIOD_2_END),
            cycle1.actualPeriod?.endDate
        )

        // 지연 정보
        assertEquals(0, cycle1.delayTheDays)
        assertNull(cycle1.delayDay)

        // 생리 예정일: 없음
        assertEquals(0, cycle1.predictDays.size)

        // 가임기: [2025-01-23 ~ 2025-01-30] - 음수 방어로 생리 시작일부터 시작
        // 18일 주기: 시작 = 18-19 = -1 → 0, 종료 = 18-11 = 7
        assertEquals(1, cycle1.fertileDays.size)
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 1, 23)),
            cycle1.fertileDays[0].startDate,
            "가임기 시작일 (음수 방어: 18-19=-1→0)"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 1, 30)),
            cycle1.fertileDays[0].endDate
        )

        // 배란기: 없음 (2025-01-25 ~ 2025-01-27로 조회일과 겹치지 않음)
        // 18일 주기: 시작 = 18-16 = 2, 종료 = 18-14 = 4
        assertEquals(0, cycle1.ovulationDays.size)

        // 주기 정보
        assertEquals(18, cycle1.period)
        assertFalse(cycle1.isOvulationPeriodUserInput)
        assertNull(cycle1.thePillPeriod)
        assertFalse(cycle1.isContinuousPillUsage)
    }

    /**
     * TC-02-05: 1주일 조회 (과거) - 음수 방어 로직 검증
     * Period 2의 배란기/가임기 구간을 1주일 조회하여 정확히 검증
     * 주기 18일: 가임기 시작 = 18 - 19 = -1 → 0 (음수 방어)
     */
    @Test
    fun testTC_02_05_weekQueryPast_NegativeDefense() = runTest {
        val repository = InMemoryPeriodRepository()
        setupCommonData(repository)

        val fromDate = LocalDate(2025, 1, 26)
        val toDate = LocalDate(2025, 2, 1)
        val today = DEFAULT_TODAY

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository = repository,
            fromDate = DateUtils.toJulianDay(fromDate),
            toDate = DateUtils.toJulianDay(toDate),
            today = DateUtils.toJulianDay(today)
        )

        // 주기 개수: 1개
        assertEquals(1, cycles.size, "주기 개수: 1개")

        // 주기 1 (pk=2)
        val cycle1 = cycles.first()
        assertEquals("2", cycle1.pk)
        assertEquals(
            DateUtils.toJulianDay(PERIOD_2_START),
            cycle1.actualPeriod?.startDate
        )
        assertEquals(
            DateUtils.toJulianDay(PERIOD_2_END),
            cycle1.actualPeriod?.endDate
        )

        // 지연 정보
        assertEquals(0, cycle1.delayTheDays)
        assertNull(cycle1.delayDay)

        // 생리 예정일: 없음
        assertEquals(0, cycle1.predictDays.size)

        // 가임기: [2025-01-23 ~ 2025-01-30] - 음수 방어로 생리 시작일부터 시작
        assertEquals(1, cycle1.fertileDays.size)
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 1, 23)),
            cycle1.fertileDays[0].startDate,
            "가임기 시작일이 생리 시작일과 동일 (음수 방어: 18-19=-1→0)"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 1, 30)),
            cycle1.fertileDays[0].endDate
        )

        // 배란기: [2025-01-25 ~ 2025-01-27]
        assertEquals(1, cycle1.ovulationDays.size)
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 1, 25)),
            cycle1.ovulationDays[0].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 1, 27)),
            cycle1.ovulationDays[0].endDate
        )

        // 주기 정보
        assertEquals(18, cycle1.period)
    }

    /**
     * TC-02-06: 1개월 조회 (과거)
     * Period 1 주기를 조회하여 실제 생리, 배란기, 가임기가 모두 정확히 계산되는지 검증
     */
    @Test
    fun testTC_02_06_monthQueryPast() = runTest {
        val repository = InMemoryPeriodRepository()
        setupCommonData(repository)

        val fromDate = LocalDate(2025, 1, 1)
        val toDate = LocalDate(2025, 1, 22)
        val today = DEFAULT_TODAY

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository = repository,
            fromDate = DateUtils.toJulianDay(fromDate),
            toDate = DateUtils.toJulianDay(toDate),
            today = DateUtils.toJulianDay(today)
        )

        // 주기 개수: 1개
        assertEquals(1, cycles.size, "주기 개수: 1개")

        // 주기 1 (pk=1)
        val cycle1 = cycles.first()
        assertEquals("1", cycle1.pk)
        assertEquals(
            DateUtils.toJulianDay(PERIOD_1_START),
            cycle1.actualPeriod?.startDate
        )
        assertEquals(
            DateUtils.toJulianDay(PERIOD_1_END),
            cycle1.actualPeriod?.endDate
        )

        // 지연 정보
        assertEquals(0, cycle1.delayTheDays)
        assertNull(cycle1.delayDay)

        // 생리 예정일: 없음
        assertEquals(0, cycle1.predictDays.size)

        // 가임기: [2025-01-04 ~ 2025-01-12]
        assertEquals(1, cycle1.fertileDays.size)
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 1, 4)),
            cycle1.fertileDays[0].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 1, 12)),
            cycle1.fertileDays[0].endDate
        )

        // 배란기: [2025-01-07 ~ 2025-01-09]
        assertEquals(1, cycle1.ovulationDays.size)
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 1, 7)),
            cycle1.ovulationDays[0].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 1, 9)),
            cycle1.ovulationDays[0].endDate
        )

        // 주기 정보
        assertEquals(22, cycle1.period)
    }

    /**
     * TC-02-07: 3개월 조회
     * 장기간 조회 시 여러 주기의 예측이 반복적으로 정확한지 검증
     */
    @Test
    fun testTC_02_07_threeMonthQuery() = runTest {
        val repository = InMemoryPeriodRepository()
        setupCommonData(repository)

        val fromDate = LocalDate(2025, 2, 10)
        val toDate = LocalDate(2025, 5, 9)
        val today = DEFAULT_TODAY

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository = repository,
            fromDate = DateUtils.toJulianDay(fromDate),
            toDate = DateUtils.toJulianDay(toDate),
            today = DateUtils.toJulianDay(today)
        )

        // 주기 개수: 1개
        assertEquals(1, cycles.size, "주기 개수: 1개")

        // 주기 1 (pk=3)
        val cycle1 = cycles.first()
        assertEquals("3", cycle1.pk)
        assertEquals(
            DateUtils.toJulianDay(PERIOD_3_START),
            cycle1.actualPeriod?.startDate
        )
        assertEquals(
            DateUtils.toJulianDay(PERIOD_3_END),
            cycle1.actualPeriod?.endDate
        )

        // 지연 정보
        assertEquals(0, cycle1.delayTheDays)
        assertNull(cycle1.delayDay)

        // 생리 예정일: 4개
        assertEquals(4, cycle1.predictDays.size)
        // 예정일 1: 2025-03-04 ~ 2025-03-08
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 4)),
            cycle1.predictDays[0].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 8)),
            cycle1.predictDays[0].endDate
        )
        // 예정일 2: 2025-03-26 ~ 2025-03-30
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 26)),
            cycle1.predictDays[1].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 30)),
            cycle1.predictDays[1].endDate
        )
        // 예정일 3: 2025-04-17 ~ 2025-04-21
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 17)),
            cycle1.predictDays[2].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 21)),
            cycle1.predictDays[2].endDate
        )
        // 예정일 4: 2025-05-09 ~ 2025-05-13
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 5, 9)),
            cycle1.predictDays[3].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 5, 13)),
            cycle1.predictDays[3].endDate
        )

        // 가임기: 4개
        assertEquals(4, cycle1.fertileDays.size)
        // 가임기 1: 2025-02-13 ~ 2025-02-21
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 13)),
            cycle1.fertileDays[0].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 21)),
            cycle1.fertileDays[0].endDate
        )
        // 가임기 2: 2025-03-07 ~ 2025-03-15
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 7)),
            cycle1.fertileDays[1].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 15)),
            cycle1.fertileDays[1].endDate
        )
        // 가임기 3: 2025-03-29 ~ 2025-04-06
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 29)),
            cycle1.fertileDays[2].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 6)),
            cycle1.fertileDays[2].endDate
        )
        // 가임기 4: 2025-04-20 ~ 2025-04-28
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 20)),
            cycle1.fertileDays[3].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 28)),
            cycle1.fertileDays[3].endDate
        )

        // 배란기: 4개
        assertEquals(4, cycle1.ovulationDays.size)
        // 배란기 1: 2025-02-16 ~ 2025-02-18
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 16)),
            cycle1.ovulationDays[0].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 18)),
            cycle1.ovulationDays[0].endDate
        )
        // 배란기 2: 2025-03-10 ~ 2025-03-12
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 10)),
            cycle1.ovulationDays[1].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 12)),
            cycle1.ovulationDays[1].endDate
        )
        // 배란기 3: 2025-04-01 ~ 2025-04-03
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 1)),
            cycle1.ovulationDays[2].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 3)),
            cycle1.ovulationDays[2].endDate
        )
        // 배란기 4: 2025-04-23 ~ 2025-04-25
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 23)),
            cycle1.ovulationDays[3].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 25)),
            cycle1.ovulationDays[3].endDate
        )

        // 주기 정보
        assertEquals(22, cycle1.period)
    }

    /**
     * TC-02-08: 생리 기간 경계 조회 (3개 주기)
     * 전체 생리 기록 조회 및 미래 예측 검증
     */
    @Test
    fun testTC_02_08_periodBoundaryQueryThreeCycles() = runTest {
        val repository = InMemoryPeriodRepository()
        setupCommonData(repository)

        val fromDate = LocalDate(2025, 1, 7)
        val toDate = LocalDate(2025, 4, 6)
        val today = DEFAULT_TODAY

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository = repository,
            fromDate = DateUtils.toJulianDay(fromDate),
            toDate = DateUtils.toJulianDay(toDate),
            today = DateUtils.toJulianDay(today)
        )

        // 주기 개수: 3개
        assertEquals(3, cycles.size, "주기 개수: 3개")

        // 주기 1 (pk=1)
        val cycle1 = cycles[0]
        assertEquals("1", cycle1.pk)
        assertEquals(
            DateUtils.toJulianDay(PERIOD_1_START),
            cycle1.actualPeriod?.startDate
        )
        assertEquals(
            DateUtils.toJulianDay(PERIOD_1_END),
            cycle1.actualPeriod?.endDate
        )
        assertEquals(0, cycle1.delayTheDays)
        assertNull(cycle1.delayDay)
        assertEquals(0, cycle1.predictDays.size)
        assertEquals(1, cycle1.fertileDays.size)
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 1, 4)),
            cycle1.fertileDays[0].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 1, 12)),
            cycle1.fertileDays[0].endDate
        )
        assertEquals(1, cycle1.ovulationDays.size)
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 1, 7)),
            cycle1.ovulationDays[0].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 1, 9)),
            cycle1.ovulationDays[0].endDate
        )
        assertEquals(22, cycle1.period)

        // 주기 2 (pk=2)
        val cycle2 = cycles[1]
        assertEquals("2", cycle2.pk)
        assertEquals(
            DateUtils.toJulianDay(PERIOD_2_START),
            cycle2.actualPeriod?.startDate
        )
        assertEquals(
            DateUtils.toJulianDay(PERIOD_2_END),
            cycle2.actualPeriod?.endDate
        )
        assertEquals(0, cycle2.delayTheDays)
        assertNull(cycle2.delayDay)
        assertEquals(0, cycle2.predictDays.size)
        assertEquals(1, cycle2.fertileDays.size)
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 1, 23)),
            cycle2.fertileDays[0].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 1, 30)),
            cycle2.fertileDays[0].endDate
        )
        assertEquals(1, cycle2.ovulationDays.size)
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 1, 25)),
            cycle2.ovulationDays[0].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 1, 27)),
            cycle2.ovulationDays[0].endDate
        )
        assertEquals(18, cycle2.period)

        // 주기 3 (pk=3)
        val cycle3 = cycles[2]
        assertEquals("3", cycle3.pk)
        assertEquals(
            DateUtils.toJulianDay(PERIOD_3_START),
            cycle3.actualPeriod?.startDate
        )
        assertEquals(
            DateUtils.toJulianDay(PERIOD_3_END),
            cycle3.actualPeriod?.endDate
        )
        assertEquals(0, cycle3.delayTheDays)
        assertNull(cycle3.delayDay)
        assertEquals(2, cycle3.predictDays.size)
        // 예정일 1: 2025-03-04 ~ 2025-03-08
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 4)),
            cycle3.predictDays[0].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 8)),
            cycle3.predictDays[0].endDate
        )
        // 예정일 2: 2025-03-26 ~ 2025-03-30
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 26)),
            cycle3.predictDays[1].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 30)),
            cycle3.predictDays[1].endDate
        )
        assertEquals(3, cycle3.fertileDays.size)
        // 가임기 1: 2025-02-13 ~ 2025-02-21
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 13)),
            cycle3.fertileDays[0].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 21)),
            cycle3.fertileDays[0].endDate
        )
        // 가임기 2: 2025-03-07 ~ 2025-03-15
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 7)),
            cycle3.fertileDays[1].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 15)),
            cycle3.fertileDays[1].endDate
        )
        // 가임기 3: 2025-03-29 ~ 2025-04-06
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 29)),
            cycle3.fertileDays[2].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 6)),
            cycle3.fertileDays[2].endDate
        )
        assertEquals(3, cycle3.ovulationDays.size)
        // 배란기 1: 2025-02-16 ~ 2025-02-18
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 16)),
            cycle3.ovulationDays[0].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 18)),
            cycle3.ovulationDays[0].endDate
        )
        // 배란기 2: 2025-03-10 ~ 2025-03-12
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 10)),
            cycle3.ovulationDays[1].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 12)),
            cycle3.ovulationDays[1].endDate
        )
        // 배란기 3: 2025-04-01 ~ 2025-04-03
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 1)),
            cycle3.ovulationDays[2].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 3)),
            cycle3.ovulationDays[2].endDate
        )
        assertEquals(22, cycle3.period)
    }

    /**
     * TC-02-09: 생리 기간 경계 조회 (2개 주기)
     * 과거 생리 기록만 조회 (미래 예측 없음)
     */
    @Test
    fun testTC_02_09_periodBoundaryQueryTwoCycles() = runTest {
        val repository = InMemoryPeriodRepository()
        setupCommonData(repository)

        val fromDate = LocalDate(2025, 1, 1)
        val toDate = LocalDate(2025, 2, 9)
        val today = DEFAULT_TODAY

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository = repository,
            fromDate = DateUtils.toJulianDay(fromDate),
            toDate = DateUtils.toJulianDay(toDate),
            today = DateUtils.toJulianDay(today)
        )

        // 주기 개수: 2개
        assertEquals(2, cycles.size, "주기 개수: 2개")

        // 주기 1 (pk=1)
        val cycle1 = cycles[0]
        assertEquals("1", cycle1.pk)
        assertEquals(
            DateUtils.toJulianDay(PERIOD_1_START),
            cycle1.actualPeriod?.startDate
        )
        assertEquals(
            DateUtils.toJulianDay(PERIOD_1_END),
            cycle1.actualPeriod?.endDate
        )
        assertEquals(0, cycle1.delayTheDays)
        assertNull(cycle1.delayDay)
        assertEquals(0, cycle1.predictDays.size)
        assertEquals(1, cycle1.fertileDays.size)
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 1, 4)),
            cycle1.fertileDays[0].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 1, 12)),
            cycle1.fertileDays[0].endDate
        )
        assertEquals(1, cycle1.ovulationDays.size)
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 1, 7)),
            cycle1.ovulationDays[0].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 1, 9)),
            cycle1.ovulationDays[0].endDate
        )
        assertEquals(22, cycle1.period)

        // 주기 2 (pk=2)
        val cycle2 = cycles[1]
        assertEquals("2", cycle2.pk)
        assertEquals(
            DateUtils.toJulianDay(PERIOD_2_START),
            cycle2.actualPeriod?.startDate
        )
        assertEquals(
            DateUtils.toJulianDay(PERIOD_2_END),
            cycle2.actualPeriod?.endDate
        )
        assertEquals(0, cycle2.delayTheDays)
        assertNull(cycle2.delayDay)
        assertEquals(0, cycle2.predictDays.size)
        assertEquals(1, cycle2.fertileDays.size)
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 1, 23)),
            cycle2.fertileDays[0].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 1, 30)),
            cycle2.fertileDays[0].endDate
        )
        assertEquals(1, cycle2.ovulationDays.size)
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 1, 25)),
            cycle2.ovulationDays[0].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 1, 27)),
            cycle2.ovulationDays[0].endDate
        )
        assertEquals(18, cycle2.period)
    }

    /**
     * TC-02-10: 생리 지연 1-7일 (예정일 뒤로 미룸)
     * 생리 예정일이 지나고 1-7일 지연되었을 때 지연 기간이 표시되고, 다음 예정일이 지연 다음날부터 시작하는지 검증
     */
    @Test
    fun testTC_02_10_delay1To7Days() = runTest {
        val repository = InMemoryPeriodRepository()
        setupCommonData(repository)

        val fromDate = LocalDate(2025, 3, 9)
        val toDate = LocalDate(2025, 3, 15)
        val today = LocalDate(2025, 3, 10) // 생리 예정일 03-04으로부터 7일 지연

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository = repository,
            fromDate = DateUtils.toJulianDay(fromDate),
            toDate = DateUtils.toJulianDay(toDate),
            today = DateUtils.toJulianDay(today)
        )

        // 주기 개수: 1개
        assertEquals(1, cycles.size, "주기 개수: 1개")

        // 주기 1 (pk=3)
        val cycle1 = cycles.first()
        assertEquals("3", cycle1.pk)
        assertEquals(
            DateUtils.toJulianDay(PERIOD_3_START),
            cycle1.actualPeriod?.startDate
        )
        assertEquals(
            DateUtils.toJulianDay(PERIOD_3_END),
            cycle1.actualPeriod?.endDate
        )

        // 지연 정보: 7일
        assertEquals(7, cycle1.delayTheDays)
        assertNotNull(cycle1.delayDay)
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 4)),
            cycle1.delayDay?.startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 10)),
            cycle1.delayDay?.endDate
        )

        // 생리 예정일: [2025-03-11 ~ 2025-03-15] (지연 다음날부터)
        assertEquals(1, cycle1.predictDays.size)
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 11)),
            cycle1.predictDays[0].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 15)),
            cycle1.predictDays[0].endDate
        )

        // 가임기: [2025-03-14 ~ 2025-03-22]
        assertEquals(1, cycle1.fertileDays.size)
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 14)),
            cycle1.fertileDays[0].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 22)),
            cycle1.fertileDays[0].endDate
        )

        // 배란기: 없음
        assertEquals(0, cycle1.ovulationDays.size)

        // 주기 정보
        assertEquals(22, cycle1.period)
    }

    /**
     * TC-02-11: 생리 지연 8일 이상 (예정일 표시 안 함, 병원 권장)
     * 생리 예정일이 지나고 8일 이상 지연되었을 때 예정일과 지연 기간을 표시하지 않는지 검증 (병원 진료 권장 상태)
     */
    @Test
    fun testTC_02_11_delay8OrMoreDays() = runTest {
        val repository = InMemoryPeriodRepository()
        setupCommonData(repository)

        val fromDate = LocalDate(2025, 3, 9)
        val toDate = LocalDate(2025, 3, 15)
        val today = LocalDate(2025, 3, 11) // 생리 예정일 03-04으로부터 8일 지연

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository = repository,
            fromDate = DateUtils.toJulianDay(fromDate),
            toDate = DateUtils.toJulianDay(toDate),
            today = DateUtils.toJulianDay(today)
        )

        // 주기 개수: 1개
        assertEquals(1, cycles.size, "주기 개수: 1개")

        // 주기 1 (pk=3)
        val cycle1 = cycles.first()
        assertEquals("3", cycle1.pk)
        assertEquals(
            DateUtils.toJulianDay(PERIOD_3_START),
            cycle1.actualPeriod?.startDate
        )
        assertEquals(
            DateUtils.toJulianDay(PERIOD_3_END),
            cycle1.actualPeriod?.endDate
        )

        // 지연 정보: 8일
        assertEquals(8, cycle1.delayTheDays)
        assertNotNull(cycle1.delayDay)
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 4)),
            cycle1.delayDay?.startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 11)),
            cycle1.delayDay?.endDate
        )

        // 생리 예정일: 없음 (8일 이상 지연 시 표시 안 함)
        assertEquals(0, cycle1.predictDays.size)

        // 가임기: 없음
        assertEquals(0, cycle1.fertileDays.size)

        // 배란기: 없음
        assertEquals(0, cycle1.ovulationDays.size)

        // 주기 정보
        assertEquals(22, cycle1.period)
    }
}
