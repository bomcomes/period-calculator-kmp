package com.bomcomes.calculator.integration

import com.bomcomes.calculator.PeriodCalculator
import com.bomcomes.calculator.models.*
import com.bomcomes.calculator.repository.InMemoryPeriodRepository
import com.bomcomes.calculator.utils.DateUtils
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.*

/**
 * 표준일 피임법 검증 테스트 (11개)
 *
 * 문서 참조: test-cases/docs/01-standard-days-method.md
 *
 * 표준일 피임법 (26~32일 주기):
 * - 생리 주기의 처음 7일 동안은 안전기
 * - 8일부터 19일까지는 가임기
 * - 20일부터 다시 안전기
 * - 계산 실패 확률 5%
 */
class StandardDaysMethodTest {
    companion object {
        // 공통 생리 기록
        val PERIOD_1_START = LocalDate(2025, 1, 1)
        val PERIOD_1_END = LocalDate(2025, 1, 5)
        val PERIOD_2_START = LocalDate(2025, 2, 1)
        val PERIOD_2_END = LocalDate(2025, 2, 5)
        val PERIOD_3_START = LocalDate(2025, 3, 1)
        val PERIOD_3_END = LocalDate(2025, 3, 5)

        // 기본 오늘 날짜
        val DEFAULT_TODAY = LocalDate(2025, 3, 15)

        // 주기 설정 (자동 계산 사용)
        const val MANUAL_AVERAGE_CYCLE = 28
        const val MANUAL_AVERAGE_DAY = 5
        const val AUTO_AVERAGE_CYCLE = 30
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
        repository.setPillSettings(PillSettings(
            isCalculatingWithPill = false,
            pillCount = 21,
            restPill = 7
        ))
    }

    /**
     * TC-01-01: 1일 조회 (마지막 생리 이후)
     * 마지막 생리 이후 특정 날짜의 가임기 상태를 정확히 검증
     */
    @Test
    fun testTC_01_01_singleDayQueryAfterLastPeriod() = runTest {
        val repository = InMemoryPeriodRepository()
        setupCommonData(repository)

        val fromDate = LocalDate(2025, 3, 15)
        val toDate = LocalDate(2025, 3, 15)
        val today = DEFAULT_TODAY

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository = repository,
            fromDate = DateUtils.toJulianDay(fromDate),
            toDate = DateUtils.toJulianDay(toDate),
            today = DateUtils.toJulianDay(today)
        )

        // 주기 개수 검증
        assertEquals(1, cycles.size, "주기 개수: 1개")

        val cycle = cycles.first()

        // pk 검증
        assertEquals("3", cycle.pk, "pk=3")

        // 실제 생리 기록 검증
        assertNotNull(cycle.actualPeriod, "실제 생리 기록 존재")
        assertEquals(
            DateUtils.toJulianDay(PERIOD_3_START),
            cycle.actualPeriod!!.startDate,
            "실제 생리 시작: 2025-03-01"
        )
        assertEquals(
            DateUtils.toJulianDay(PERIOD_3_END),
            cycle.actualPeriod!!.endDate,
            "실제 생리 종료: 2025-03-05"
        )

        // 지연 검증
        assertEquals(0, cycle.delayTheDays, "지연 일수: 0일")
        assertNull(cycle.delayDay, "지연 기간: null")

        // 생리 예정일 검증 (조회 범위 내 없음)
        assertEquals(0, cycle.predictDays.size, "생리 예정일들: []")

        // 가임기 검증: 2025-03-08 ~ 2025-03-19
        assertEquals(1, cycle.fertileDays.size, "가임기 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 8)),
            cycle.fertileDays[0].startDate,
            "가임기 시작: 2025-03-08"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 19)),
            cycle.fertileDays[0].endDate,
            "가임기 종료: 2025-03-19"
        )

        // 배란기 검증: 2025-03-13 ~ 2025-03-15
        assertEquals(1, cycle.ovulationDays.size, "배란기 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 13)),
            cycle.ovulationDays[0].startDate,
            "배란기 시작: 2025-03-13"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 15)),
            cycle.ovulationDays[0].endDate,
            "배란기 종료: 2025-03-15"
        )

        // 주기 검증
        assertEquals(30, cycle.period, "주기: 30일")

        // 배란일 사용자 입력 검증
        assertFalse(cycle.isOvulationPeriodUserInput, "배란일 사용자 입력: false")

        // 피임약 정보 검증
        assertNull(cycle.thePillPeriod, "피임약 기준 주기: null")
        assertNull(cycle.restPill, "남은 휴약일: null")
    }

    /**
     * TC-01-02: 1주일 조회 (마지막 생리 이후)
     * 마지막 생리 이후 1주일 조회 시 배란기가 정확히 표시되는지 검증
     */
    @Test
    fun testTC_01_02_weekQueryAfterLastPeriod() = runTest {
        val repository = InMemoryPeriodRepository()
        setupCommonData(repository)

        val fromDate = LocalDate(2025, 3, 9)
        val toDate = LocalDate(2025, 3, 15)
        val today = DEFAULT_TODAY

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository = repository,
            fromDate = DateUtils.toJulianDay(fromDate),
            toDate = DateUtils.toJulianDay(toDate),
            today = DateUtils.toJulianDay(today)
        )

        assertEquals(1, cycles.size, "주기 개수: 1개")

        val cycle = cycles.first()

        assertEquals("3", cycle.pk)
        assertEquals(0, cycle.predictDays.size, "생리 예정일들: []")

        // 가임기: 2025-03-08 ~ 2025-03-19 (전체 반환)
        assertEquals(1, cycle.fertileDays.size)
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 8)),
            cycle.fertileDays[0].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 19)),
            cycle.fertileDays[0].endDate
        )

        // 배란기: 2025-03-13 ~ 2025-03-15 (전체 반환)
        assertEquals(1, cycle.ovulationDays.size)
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 13)),
            cycle.ovulationDays[0].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 15)),
            cycle.ovulationDays[0].endDate
        )
    }

    /**
     * TC-01-03: 1개월 조회 (마지막 생리 이후)
     * 마지막 생리(Period 3) 전체 주기를 조회하여 실제 생리, 배란기, 가임기, 다음 예정일이 모두 정확히 표시되는지 검증
     */
    @Test
    fun testTC_01_03_monthQueryAfterLastPeriod() = runTest {
        val repository = InMemoryPeriodRepository()
        setupCommonData(repository)

        val fromDate = LocalDate(2025, 3, 1)
        val toDate = LocalDate(2025, 3, 31)
        val today = DEFAULT_TODAY

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository = repository,
            fromDate = DateUtils.toJulianDay(fromDate),
            toDate = DateUtils.toJulianDay(toDate),
            today = DateUtils.toJulianDay(today)
        )

        assertEquals(1, cycles.size, "주기 개수: 1개")

        val cycle = cycles.first()

        assertEquals("3", cycle.pk)

        // 생리 예정일: 2025-03-31 ~ 2025-04-04
        assertEquals(1, cycle.predictDays.size, "생리 예정일 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 31)),
            cycle.predictDays[0].startDate,
            "예정일 시작: 2025-03-31"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 4)),
            cycle.predictDays[0].endDate,
            "예정일 종료: 2025-04-04"
        )

        // 가임기: 2025-03-08 ~ 2025-03-19
        assertEquals(1, cycle.fertileDays.size)
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 8)),
            cycle.fertileDays[0].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 19)),
            cycle.fertileDays[0].endDate
        )

        // 배란기: 2025-03-13 ~ 2025-03-15
        assertEquals(1, cycle.ovulationDays.size)
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 13)),
            cycle.ovulationDays[0].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 15)),
            cycle.ovulationDays[0].endDate
        )

        assertEquals(30, cycle.period, "주기: 30일")
    }

    /**
     * TC-01-04: 1일 조회 (과거)
     * Period 2와 Period 3 사이 배란기 특정 날짜를 정확히 검증
     */
    @Test
    fun testTC_01_04_singleDayQueryPast() = runTest {
        val repository = InMemoryPeriodRepository()
        setupCommonData(repository)

        val fromDate = LocalDate(2025, 2, 7)
        val toDate = LocalDate(2025, 2, 7)
        val today = DEFAULT_TODAY

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository = repository,
            fromDate = DateUtils.toJulianDay(fromDate),
            toDate = DateUtils.toJulianDay(toDate),
            today = DateUtils.toJulianDay(today)
        )

        // 주기 개수: 2개 (Period 2, Period 3 주기)
        assertEquals(2, cycles.size, "주기 개수: 2개")

        // 주기 1 (pk=2)
        val cycle1 = cycles[0]
        assertEquals("2", cycle1.pk, "주기1 pk=2")
        assertEquals(
            DateUtils.toJulianDay(PERIOD_2_START),
            cycle1.actualPeriod?.startDate,
            "주기1 실제 생리 시작"
        )
        assertEquals(
            DateUtils.toJulianDay(PERIOD_2_END),
            cycle1.actualPeriod?.endDate,
            "주기1 실제 생리 종료"
        )
        assertEquals(0, cycle1.delayTheDays, "주기1 지연 일수: 0")
        assertNull(cycle1.delayDay, "주기1 지연 기간: null")
        assertEquals(0, cycle1.predictDays.size, "주기1 생리 예정일들: []")
        assertEquals(0, cycle1.fertileDays.size, "주기1 가임기들: [] (02-07은 안전기)")
        assertEquals(0, cycle1.ovulationDays.size, "주기1 배란기들: []")
        assertEquals(28, cycle1.period, "주기1 주기: 28일")
        assertFalse(cycle1.isOvulationPeriodUserInput)
        assertNull(cycle1.thePillPeriod)
        assertNull(cycle1.restPill)

        // 주기 2 (pk=3)
        val cycle2 = cycles[1]
        assertEquals("3", cycle2.pk, "주기2 pk=3")
        assertEquals(
            DateUtils.toJulianDay(PERIOD_3_START),
            cycle2.actualPeriod?.startDate,
            "주기2 실제 생리 시작"
        )
        assertEquals(
            DateUtils.toJulianDay(PERIOD_3_END),
            cycle2.actualPeriod?.endDate,
            "주기2 실제 생리 종료"
        )
        assertEquals(0, cycle2.delayTheDays, "주기2 지연 일수: 0")
        assertNull(cycle2.delayDay, "주기2 지연 기간: null")
        assertEquals(0, cycle2.predictDays.size, "주기2 생리 예정일들: []")
        assertEquals(0, cycle2.fertileDays.size, "주기2 가임기들: []")
        assertEquals(0, cycle2.ovulationDays.size, "주기2 배란기들: []")
        assertEquals(30, cycle2.period, "주기2 주기: 30일")
        assertFalse(cycle2.isOvulationPeriodUserInput)
        assertNull(cycle2.thePillPeriod)
        assertNull(cycle2.restPill)
    }

    /**
     * TC-01-05: 1주일 조회 (과거)
     * Period 2의 배란기/가임기 구간을 1주일 조회하여 정확히 검증
     */
    @Test
    fun testTC_01_05_weekQueryPast() = runTest {
        val repository = InMemoryPeriodRepository()
        setupCommonData(repository)

        val fromDate = LocalDate(2025, 2, 9)
        val toDate = LocalDate(2025, 2, 15)
        val today = DEFAULT_TODAY

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository = repository,
            fromDate = DateUtils.toJulianDay(fromDate),
            toDate = DateUtils.toJulianDay(toDate),
            today = DateUtils.toJulianDay(today)
        )

        assertEquals(2, cycles.size, "주기 개수: 2개")

        // 주기 1 (pk=2) - 가임기/배란기 포함
        val cycle1 = cycles[0]
        assertEquals("2", cycle1.pk)

        // 가임기: 2025-02-08 ~ 2025-02-19 (조회 범위와 겹침)
        assertEquals(1, cycle1.fertileDays.size, "주기1 가임기 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 8)),
            cycle1.fertileDays[0].startDate,
            "주기1 가임기 시작"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 19)),
            cycle1.fertileDays[0].endDate,
            "주기1 가임기 종료"
        )

        // 배란기: 2025-02-13 ~ 2025-02-15 (조회 범위와 겹침)
        assertEquals(1, cycle1.ovulationDays.size, "주기1 배란기 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 13)),
            cycle1.ovulationDays[0].startDate,
            "주기1 배란기 시작"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 15)),
            cycle1.ovulationDays[0].endDate,
            "주기1 배란기 종료"
        )

        assertEquals(28, cycle1.period, "주기1 주기: 28일")

        // 주기 2 (pk=3) - 가임기/배란기 없음
        val cycle2 = cycles[1]
        assertEquals("3", cycle2.pk)
        assertEquals(0, cycle2.fertileDays.size, "주기2 가임기들: []")
        assertEquals(0, cycle2.ovulationDays.size, "주기2 배란기들: []")
        assertEquals(30, cycle2.period, "주기2 주기: 30일")
    }

    /**
     * TC-01-06: 1개월 조회 (과거)
     * Period 2 전체 주기를 조회하여 실제 생리, 배란기, 가임기가 모두 정확히 계산되는지 검증
     */
    @Test
    fun testTC_01_06_monthQueryPast() = runTest {
        val repository = InMemoryPeriodRepository()
        setupCommonData(repository)

        val fromDate = LocalDate(2025, 2, 1)
        val toDate = LocalDate(2025, 2, 28)
        val today = DEFAULT_TODAY

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository = repository,
            fromDate = DateUtils.toJulianDay(fromDate),
            toDate = DateUtils.toJulianDay(toDate),
            today = DateUtils.toJulianDay(today)
        )

        assertEquals(2, cycles.size, "주기 개수: 2개")

        // 주기 1 (pk=2)
        val cycle1 = cycles[0]
        assertEquals("2", cycle1.pk)
        assertEquals(
            DateUtils.toJulianDay(PERIOD_2_START),
            cycle1.actualPeriod?.startDate
        )
        assertEquals(
            DateUtils.toJulianDay(PERIOD_2_END),
            cycle1.actualPeriod?.endDate
        )
        assertEquals(0, cycle1.predictDays.size, "주기1 생리 예정일들: []")

        // 가임기: 2025-02-08 ~ 2025-02-19
        assertEquals(1, cycle1.fertileDays.size)
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 8)),
            cycle1.fertileDays[0].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 19)),
            cycle1.fertileDays[0].endDate
        )

        // 배란기: 2025-02-13 ~ 2025-02-15
        assertEquals(1, cycle1.ovulationDays.size)
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 13)),
            cycle1.ovulationDays[0].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 15)),
            cycle1.ovulationDays[0].endDate
        )

        assertEquals(28, cycle1.period)

        // 주기 2 (pk=3)
        val cycle2 = cycles[1]
        assertEquals("3", cycle2.pk)
        assertEquals(0, cycle2.fertileDays.size)
        assertEquals(0, cycle2.ovulationDays.size)
        assertEquals(30, cycle2.period)
    }

    /**
     * TC-01-07: 3개월 조회
     * 장기간 조회 시 여러 주기의 예측이 반복적으로 정확한지 검증
     */
    @Test
    fun testTC_01_07_threeMonthQuery() = runTest {
        val repository = InMemoryPeriodRepository()
        setupCommonData(repository)

        val fromDate = LocalDate(2025, 3, 1)
        val toDate = LocalDate(2025, 5, 31)
        val today = DEFAULT_TODAY

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository = repository,
            fromDate = DateUtils.toJulianDay(fromDate),
            toDate = DateUtils.toJulianDay(toDate),
            today = DateUtils.toJulianDay(today)
        )

        assertEquals(1, cycles.size, "주기 개수: 1개")

        val cycle = cycles.first()
        assertEquals("3", cycle.pk)

        // 생리 예정일들: 3개
        // 1) 2025-03-31 ~ 2025-04-04
        // 2) 2025-04-30 ~ 2025-05-04
        // 3) 2025-05-30 ~ 2025-06-03
        assertEquals(3, cycle.predictDays.size, "생리 예정일 3개")

        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 31)),
            cycle.predictDays[0].startDate,
            "1차 예정일 시작"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 4)),
            cycle.predictDays[0].endDate,
            "1차 예정일 종료"
        )

        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 30)),
            cycle.predictDays[1].startDate,
            "2차 예정일 시작"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 5, 4)),
            cycle.predictDays[1].endDate,
            "2차 예정일 종료"
        )

        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 5, 30)),
            cycle.predictDays[2].startDate,
            "3차 예정일 시작"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 6, 3)),
            cycle.predictDays[2].endDate,
            "3차 예정일 종료"
        )

        // 가임기들: 3개
        assertEquals(3, cycle.fertileDays.size, "가임기 3개")

        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 8)),
            cycle.fertileDays[0].startDate,
            "1차 가임기 시작"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 19)),
            cycle.fertileDays[0].endDate,
            "1차 가임기 종료"
        )

        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 7)),
            cycle.fertileDays[1].startDate,
            "2차 가임기 시작"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 18)),
            cycle.fertileDays[1].endDate,
            "2차 가임기 종료"
        )

        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 5, 7)),
            cycle.fertileDays[2].startDate,
            "3차 가임기 시작"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 5, 18)),
            cycle.fertileDays[2].endDate,
            "3차 가임기 종료"
        )

        // 배란기들: 3개
        assertEquals(3, cycle.ovulationDays.size, "배란기 3개")

        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 13)),
            cycle.ovulationDays[0].startDate,
            "1차 배란기 시작"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 15)),
            cycle.ovulationDays[0].endDate,
            "1차 배란기 종료"
        )

        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 12)),
            cycle.ovulationDays[1].startDate,
            "2차 배란기 시작"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 14)),
            cycle.ovulationDays[1].endDate,
            "2차 배란기 종료"
        )

        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 5, 12)),
            cycle.ovulationDays[2].startDate,
            "3차 배란기 시작"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 5, 14)),
            cycle.ovulationDays[2].endDate,
            "3차 배란기 종료"
        )

        assertEquals(30, cycle.period, "주기: 30일")
    }

    /**
     * TC-01-08: 생리 기간 경계 조회
     * 조회 기간이 여러 생리 기간을 포함하고 있을때 검증
     */
    @Test
    fun testTC_01_08_multiplePeriodBoundaryQuery() = runTest {
        val repository = InMemoryPeriodRepository()
        setupCommonData(repository)

        val fromDate = LocalDate(2025, 1, 1)
        val toDate = LocalDate(2025, 5, 31)
        val today = DEFAULT_TODAY

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository = repository,
            fromDate = DateUtils.toJulianDay(fromDate),
            toDate = DateUtils.toJulianDay(toDate),
            today = DateUtils.toJulianDay(today)
        )

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
        assertEquals(0, cycle1.predictDays.size, "주기1 생리 예정일들: []")

        // 주기 1 가임기: 2025-01-08 ~ 2025-01-19
        assertEquals(1, cycle1.fertileDays.size)
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 1, 8)),
            cycle1.fertileDays[0].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 1, 19)),
            cycle1.fertileDays[0].endDate
        )

        // 주기 1 배란기: 2025-01-13 ~ 2025-01-15
        assertEquals(1, cycle1.ovulationDays.size)
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 1, 13)),
            cycle1.ovulationDays[0].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 1, 15)),
            cycle1.ovulationDays[0].endDate
        )

        assertEquals(31, cycle1.period, "주기1 주기: 31일")

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
        assertEquals(0, cycle2.predictDays.size, "주기2 생리 예정일들: []")

        // 주기 2 가임기: 2025-02-08 ~ 2025-02-19
        assertEquals(1, cycle2.fertileDays.size)
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 8)),
            cycle2.fertileDays[0].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 19)),
            cycle2.fertileDays[0].endDate
        )

        // 주기 2 배란기: 2025-02-13 ~ 2025-02-15
        assertEquals(1, cycle2.ovulationDays.size)
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 13)),
            cycle2.ovulationDays[0].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 15)),
            cycle2.ovulationDays[0].endDate
        )

        assertEquals(28, cycle2.period, "주기2 주기: 28일")

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

        // 주기 3 생리 예정일들: 3개
        assertEquals(3, cycle3.predictDays.size, "주기3 생리 예정일 3개")

        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 31)),
            cycle3.predictDays[0].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 4)),
            cycle3.predictDays[0].endDate
        )

        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 30)),
            cycle3.predictDays[1].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 5, 4)),
            cycle3.predictDays[1].endDate
        )

        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 5, 30)),
            cycle3.predictDays[2].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 6, 3)),
            cycle3.predictDays[2].endDate
        )

        // 주기 3 가임기들: 3개
        assertEquals(3, cycle3.fertileDays.size)

        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 8)),
            cycle3.fertileDays[0].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 19)),
            cycle3.fertileDays[0].endDate
        )

        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 7)),
            cycle3.fertileDays[1].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 18)),
            cycle3.fertileDays[1].endDate
        )

        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 5, 7)),
            cycle3.fertileDays[2].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 5, 18)),
            cycle3.fertileDays[2].endDate
        )

        // 주기 3 배란기들: 3개
        assertEquals(3, cycle3.ovulationDays.size)

        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 13)),
            cycle3.ovulationDays[0].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 15)),
            cycle3.ovulationDays[0].endDate
        )

        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 12)),
            cycle3.ovulationDays[1].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 14)),
            cycle3.ovulationDays[1].endDate
        )

        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 5, 12)),
            cycle3.ovulationDays[2].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 5, 14)),
            cycle3.ovulationDays[2].endDate
        )

        assertEquals(30, cycle3.period, "주기3 주기: 30일")
    }

    /**
     * TC-01-09: 생리 기간 경계 조회
     * 조회 기간이 마지막 생리 기간을 제외한 여러 생리 기간을 포함하고 있을때 검증
     */
    @Test
    fun testTC_01_09_partialPeriodQuery() = runTest {
        val repository = InMemoryPeriodRepository()
        setupCommonData(repository)

        val fromDate = LocalDate(2025, 1, 1)
        val toDate = LocalDate(2025, 2, 28)
        val today = DEFAULT_TODAY

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository = repository,
            fromDate = DateUtils.toJulianDay(fromDate),
            toDate = DateUtils.toJulianDay(toDate),
            today = DateUtils.toJulianDay(today)
        )

        assertEquals(3, cycles.size, "주기 개수: 3개")

        // 주기 1 (pk=1)
        val cycle1 = cycles[0]
        assertEquals("1", cycle1.pk)
        assertEquals(
            DateUtils.toJulianDay(PERIOD_1_START),
            cycle1.actualPeriod?.startDate,
            "주기1 실제 생리 시작"
        )
        assertEquals(
            DateUtils.toJulianDay(PERIOD_1_END),
            cycle1.actualPeriod?.endDate,
            "주기1 실제 생리 종료"
        )
        assertEquals(0, cycle1.delayTheDays, "주기1 지연 일수: 0")
        assertNull(cycle1.delayDay, "주기1 지연 기간: null")
        assertEquals(0, cycle1.predictDays.size, "주기1 생리 예정일들: []")

        // 주기 1 가임기: 2025-01-08 ~ 2025-01-19
        assertEquals(1, cycle1.fertileDays.size, "주기1 가임기 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 1, 8)),
            cycle1.fertileDays[0].startDate,
            "주기1 가임기 시작"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 1, 19)),
            cycle1.fertileDays[0].endDate,
            "주기1 가임기 종료"
        )

        // 주기 1 배란기: 2025-01-13 ~ 2025-01-15
        assertEquals(1, cycle1.ovulationDays.size, "주기1 배란기 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 1, 13)),
            cycle1.ovulationDays[0].startDate,
            "주기1 배란기 시작"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 1, 15)),
            cycle1.ovulationDays[0].endDate,
            "주기1 배란기 종료"
        )

        assertEquals(31, cycle1.period, "주기1 주기: 31일")
        assertFalse(cycle1.isOvulationPeriodUserInput, "주기1 배란일 사용자 입력: false")
        assertNull(cycle1.thePillPeriod, "주기1 피임약 기준 주기: null")
        assertNull(cycle1.restPill, "주기1 남은 휴약일: null")

        // 주기 2 (pk=2)
        val cycle2 = cycles[1]
        assertEquals("2", cycle2.pk)
        assertEquals(
            DateUtils.toJulianDay(PERIOD_2_START),
            cycle2.actualPeriod?.startDate,
            "주기2 실제 생리 시작"
        )
        assertEquals(
            DateUtils.toJulianDay(PERIOD_2_END),
            cycle2.actualPeriod?.endDate,
            "주기2 실제 생리 종료"
        )
        assertEquals(0, cycle2.delayTheDays, "주기2 지연 일수: 0")
        assertNull(cycle2.delayDay, "주기2 지연 기간: null")
        assertEquals(0, cycle2.predictDays.size, "주기2 생리 예정일들: [] (조회 범위가 Period 3 이전에 종료)")

        // 주기 2 가임기: 2025-02-08 ~ 2025-02-19
        assertEquals(1, cycle2.fertileDays.size, "주기2 가임기 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 8)),
            cycle2.fertileDays[0].startDate,
            "주기2 가임기 시작"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 19)),
            cycle2.fertileDays[0].endDate,
            "주기2 가임기 종료"
        )

        // 주기 2 배란기: 2025-02-13 ~ 2025-02-15
        assertEquals(1, cycle2.ovulationDays.size, "주기2 배란기 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 13)),
            cycle2.ovulationDays[0].startDate,
            "주기2 배란기 시작"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 15)),
            cycle2.ovulationDays[0].endDate,
            "주기2 배란기 종료"
        )

        assertEquals(28, cycle2.period, "주기2 주기: 28일")
        assertFalse(cycle2.isOvulationPeriodUserInput, "주기2 배란일 사용자 입력: false")
        assertNull(cycle2.thePillPeriod, "주기2 피임약 기준 주기: null")
        assertNull(cycle2.restPill, "주기2 남은 휴약일: null")

        // 주기 3 (pk=3)
        val cycle3 = cycles[2]
        assertEquals("3", cycle3.pk)
        assertEquals(
            DateUtils.toJulianDay(PERIOD_3_START),
            cycle3.actualPeriod?.startDate,
            "주기3 실제 생리 시작"
        )
        assertEquals(
            DateUtils.toJulianDay(PERIOD_3_END),
            cycle3.actualPeriod?.endDate,
            "주기3 실제 생리 종료"
        )
        assertEquals(0, cycle3.delayTheDays, "주기3 지연 일수: 0")
        assertNull(cycle3.delayDay, "주기3 지연 기간: null")
        assertEquals(0, cycle3.predictDays.size, "주기3 생리 예정일들: [] (조회 범위 밖)")
        assertEquals(0, cycle3.fertileDays.size, "주기3 가임기들: [] (조회 범위 밖)")
        assertEquals(0, cycle3.ovulationDays.size, "주기3 배란기들: [] (조회 범위 밖)")
        assertEquals(30, cycle3.period, "주기3 주기: 30일")
        assertFalse(cycle3.isOvulationPeriodUserInput, "주기3 배란일 사용자 입력: false")
        assertNull(cycle3.thePillPeriod, "주기3 피임약 기준 주기: null")
        assertNull(cycle3.restPill, "주기3 남은 휴약일: null")
    }

    /**
     * TC-01-10: 생리 지연 1-7일 (예정일 뒤로 미룸)
     * 생리 예정일이 지나고 1-7일 지연되었을 때 지연 기간이 표시되고, 다음 예정일이 지연 다음날부터 시작하는지 검증
     */
    @Test
    fun testTC_01_10_delay1to7Days() = runTest {
        val repository = InMemoryPeriodRepository()
        setupCommonData(repository)

        val fromDate = LocalDate(2025, 3, 20)
        val toDate = LocalDate(2025, 4, 10)
        val today = LocalDate(2025, 4, 6) // 예정일 03-31으로부터 7일 지연

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository = repository,
            fromDate = DateUtils.toJulianDay(fromDate),
            toDate = DateUtils.toJulianDay(toDate),
            today = DateUtils.toJulianDay(today)
        )

        assertEquals(1, cycles.size, "주기 개수: 1개")

        val cycle = cycles.first()
        assertEquals("3", cycle.pk)

        // 실제 생리 기록
        assertEquals(
            DateUtils.toJulianDay(PERIOD_3_START),
            cycle.actualPeriod?.startDate
        )
        assertEquals(
            DateUtils.toJulianDay(PERIOD_3_END),
            cycle.actualPeriod?.endDate
        )

        // 지연 검증
        assertEquals(7, cycle.delayTheDays, "지연 일수: 7일")

        assertNotNull(cycle.delayDay, "지연 기간 존재")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 31)),
            cycle.delayDay!!.startDate,
            "지연 기간 시작: 2025-03-31"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 6)),
            cycle.delayDay!!.endDate,
            "지연 기간 종료: 2025-04-06"
        )

        // 생리 예정일: 지연 다음날부터 시작
        assertEquals(1, cycle.predictDays.size, "생리 예정일 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 7)),
            cycle.predictDays[0].startDate,
            "예정일 시작: 2025-04-07 (지연 다음날)"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 11)),
            cycle.predictDays[0].endDate,
            "예정일 종료: 2025-04-11"
        )

        // 가임기/배란기 없음 (지연 중)
        assertEquals(0, cycle.fertileDays.size, "가임기들: []")
        assertEquals(0, cycle.ovulationDays.size, "배란기들: []")

        assertEquals(30, cycle.period, "주기: 30일")
    }

    /**
     * TC-01-11: 생리 지연 8일 이상 (예정일 표시 안 함, 병원 권장)
     * 생리 예정일이 지나고 8일 이상 지연되었을 때 예정일과 지연 기간을 표시하지 않는지 검증 (병원 진료 권장 상태)
     */
    @Test
    fun testTC_01_11_delay8DaysOrMore() = runTest {
        val repository = InMemoryPeriodRepository()
        setupCommonData(repository)

        val fromDate = LocalDate(2025, 3, 20)
        val toDate = LocalDate(2025, 5, 10)
        val today = LocalDate(2025, 4, 7) // 예정일 03-31으로부터 8일 지연

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository = repository,
            fromDate = DateUtils.toJulianDay(fromDate),
            toDate = DateUtils.toJulianDay(toDate),
            today = DateUtils.toJulianDay(today)
        )

        assertEquals(1, cycles.size, "주기 개수: 1개")

        val cycle = cycles.first()
        assertEquals("3", cycle.pk)

        // 실제 생리 기록
        assertEquals(
            DateUtils.toJulianDay(PERIOD_3_START),
            cycle.actualPeriod?.startDate
        )
        assertEquals(
            DateUtils.toJulianDay(PERIOD_3_END),
            cycle.actualPeriod?.endDate
        )

        // 지연 검증
        assertEquals(8, cycle.delayTheDays, "지연 일수: 8일")

        assertNotNull(cycle.delayDay, "지연 기간 존재")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 31)),
            cycle.delayDay!!.startDate,
            "지연 기간 시작: 2025-03-31"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 7)),
            cycle.delayDay!!.endDate,
            "지연 기간 종료: 2025-04-07"
        )

        // 생리 예정일 없음 (8일 이상 지연 시 병원 권장)
        assertEquals(0, cycle.predictDays.size, "생리 예정일들: [] (8일 이상 지연)")

        // 가임기/배란기 없음
        assertEquals(0, cycle.fertileDays.size, "가임기들: []")
        assertEquals(0, cycle.ovulationDays.size, "배란기들: []")

        assertEquals(30, cycle.period, "주기: 30일")
    }
}
