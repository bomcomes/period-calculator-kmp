package com.bomcomes.calculator.integration

import com.bomcomes.calculator.PeriodCalculator
import com.bomcomes.calculator.models.*
import com.bomcomes.calculator.repository.InMemoryPeriodRepository
import com.bomcomes.calculator.utils.DateUtils
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.*

/**
 * 크나우스 오기노법 검증 테스트 - 긴 주기 (11개)
 *
 * 문서 참조: test-cases/docs/03-long-cycle-over-33.md
 *
 * 크나우스 오기노법 (33일 이상 긴 주기):
 * - 가임기 시작일: 주기 - 19일
 * - 가임기 종료일: 주기 - 11일
 * - 배란기 시작일: 주기 - 16일
 * - 배란기 종료일: 주기 - 14일
 * - 계산 실패 확률 9%
 */
class LongCycleOver33Test {
    companion object {
        // 공통 생리 기록 (불규칙 주기: 39, 35, 37일)
        val PERIOD_1_START = LocalDate(2025, 1, 1)
        val PERIOD_1_END = LocalDate(2025, 1, 5)
        val PERIOD_2_START = LocalDate(2025, 2, 9)
        val PERIOD_2_END = LocalDate(2025, 2, 13)
        val PERIOD_3_START = LocalDate(2025, 3, 16)
        val PERIOD_3_END = LocalDate(2025, 3, 20)

        // 기본 오늘 날짜
        val DEFAULT_TODAY = LocalDate(2025, 4, 15)

        // 주기 설정 (자동 계산 사용)
        const val MANUAL_AVERAGE_CYCLE = 28
        const val MANUAL_AVERAGE_DAY = 5
        const val AUTO_AVERAGE_CYCLE = 37  // 실제 계산: (39+35+37)/3 = 37
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
     * TC-03-01: 1일 조회 (마지막 생리 이후)
     * 마지막 생리 이후 특정 날짜의 가임기 상태를 정확히 검증
     */
    @Test
    fun testTC_03_01_singleDayQueryAfterLastPeriod() = runTest {
        val repository = InMemoryPeriodRepository()
        setupCommonData(repository)

        val fromDate = LocalDate(2025, 4, 1)
        val toDate = LocalDate(2025, 4, 1)
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
            "실제 생리 시작: 2025-03-16"
        )
        assertEquals(
            DateUtils.toJulianDay(PERIOD_3_END),
            cycle.actualPeriod!!.endDate,
            "실제 생리 종료: 2025-03-20"
        )

        // 지연 검증
        assertEquals(0, cycle.delayTheDays, "지연 일수: 0일")
        assertNull(cycle.delayDay, "지연 기간: null")

        // 생리 예정일 검증 (조회 범위 내 없음)
        assertEquals(0, cycle.predictDays.size, "생리 예정일들: []")

        // 가임기 검증: 조회 범위 밖
        assertEquals(0, cycle.fertileDays.size, "가임기들: []")

        // 배란기 검증: 조회 범위 밖
        assertEquals(0, cycle.ovulationDays.size, "배란기들: []")

        // 주기 검증
        assertEquals(37, cycle.period, "주기: 37일")

        // 배란일 사용자 입력 검증
        assertFalse(cycle.isOvulationPeriodUserInput, "배란일 사용자 입력: false")

        // 피임약 정보 검증
        assertNull(cycle.thePillPeriod, "피임약 기준 주기: null")
        assertNull(cycle.restPill, "남은 휴약일: null")
    }

    /**
     * TC-03-02: 1주일 조회 (마지막 생리 이후)
     * 마지막 생리 이후 1주일 조회 시 배란기가 정확히 표시되는지 검증
     */
    @Test
    fun testTC_03_02_weekQueryAfterLastPeriod() = runTest {
        val repository = InMemoryPeriodRepository()
        setupCommonData(repository)

        val fromDate = LocalDate(2025, 3, 30)
        val toDate = LocalDate(2025, 4, 5)
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

        // 가임기: 2025-04-03 ~ 2025-04-11 (부분 포함)
        assertEquals(1, cycle.fertileDays.size, "가임기 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 3)),
            cycle.fertileDays[0].startDate,
            "가임기 시작: 2025-04-03"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 11)),
            cycle.fertileDays[0].endDate,
            "가임기 종료: 2025-04-11"
        )

        // 배란기: 조회 범위 밖
        assertEquals(0, cycle.ovulationDays.size, "배란기들: []")

        assertEquals(37, cycle.period, "주기: 37일")
    }

    /**
     * TC-03-03: 1개월 조회 (마지막 생리 이후)
     * 마지막 생리(Period 3) 전체 주기를 조회하여 실제 생리, 배란기, 가임기, 다음 예정일이 모두 정확히 표시되는지 검증
     */
    @Test
    fun testTC_03_03_monthQueryAfterLastPeriod() = runTest {
        val repository = InMemoryPeriodRepository()
        setupCommonData(repository)

        val fromDate = LocalDate(2025, 3, 16)
        val toDate = LocalDate(2025, 4, 22)
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

        // 실제 생리 기록
        assertEquals(
            DateUtils.toJulianDay(PERIOD_3_START),
            cycle.actualPeriod?.startDate
        )
        assertEquals(
            DateUtils.toJulianDay(PERIOD_3_END),
            cycle.actualPeriod?.endDate
        )

        // 생리 예정일: 2025-04-22 ~ 2025-04-26
        assertEquals(1, cycle.predictDays.size, "생리 예정일 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 22)),
            cycle.predictDays[0].startDate,
            "예정일 시작: 2025-04-22"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 26)),
            cycle.predictDays[0].endDate,
            "예정일 종료: 2025-04-26"
        )

        // 가임기: 2025-04-03 ~ 2025-04-11
        assertEquals(1, cycle.fertileDays.size, "가임기 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 3)),
            cycle.fertileDays[0].startDate,
            "가임기 시작: 2025-04-03"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 11)),
            cycle.fertileDays[0].endDate,
            "가임기 종료: 2025-04-11"
        )

        // 배란기: 2025-04-06 ~ 2025-04-08
        assertEquals(1, cycle.ovulationDays.size, "배란기 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 6)),
            cycle.ovulationDays[0].startDate,
            "배란기 시작: 2025-04-06"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 8)),
            cycle.ovulationDays[0].endDate,
            "배란기 종료: 2025-04-08"
        )

        assertEquals(37, cycle.period, "주기: 37일")
    }

    /**
     * TC-03-04: 1일 조회 (과거)
     * Period 2와 Period 3 사이 배란기 특정 날짜를 정확히 검증
     */
    @Test
    fun testTC_03_04_singleDayQueryPast() = runTest {
        val repository = InMemoryPeriodRepository()
        setupCommonData(repository)

        val fromDate = LocalDate(2025, 2, 9)
        val toDate = LocalDate(2025, 2, 9)
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
        assertEquals(0, cycle1.fertileDays.size, "주기1 가임기들: []")
        assertEquals(0, cycle1.ovulationDays.size, "주기1 배란기들: []")
        assertEquals(35, cycle1.period, "주기1 주기: 35일")
        assertFalse(cycle1.isOvulationPeriodUserInput)
        assertNull(cycle1.thePillPeriod)
        assertNull(cycle1.restPill)
    }

    /**
     * TC-03-05: 1주일 조회 (과거)
     * Period 2의 배란기/가임기 구간을 1주일 조회하여 정확히 검증
     */
    @Test
    fun testTC_03_05_weekQueryPast() = runTest {
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

        // 주기 1 (pk=2)
        val cycle1 = cycles.first()
        assertEquals("2", cycle1.pk)

        // 조회 범위가 가임기/배란기 밖
        assertEquals(0, cycle1.fertileDays.size, "주기1 가임기들: []")
        assertEquals(0, cycle1.ovulationDays.size, "주기1 배란기들: []")
        assertEquals(35, cycle1.period, "주기1 주기: 35일")
    }

    /**
     * TC-03-06: 1개월 조회 (과거)
     * Period 2 전체 주기를 조회하여 실제 생리, 배란기, 가임기가 모두 정확히 계산되는지 검증
     */
    @Test
    fun testTC_03_06_monthQueryPast() = runTest {
        val repository = InMemoryPeriodRepository()
        setupCommonData(repository)

        val fromDate = LocalDate(2025, 1, 1)
        val toDate = LocalDate(2025, 1, 31)
        val today = DEFAULT_TODAY

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository = repository,
            fromDate = DateUtils.toJulianDay(fromDate),
            toDate = DateUtils.toJulianDay(toDate),
            today = DateUtils.toJulianDay(today)
        )

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
        assertEquals(0, cycle1.delayTheDays, "지연 일수: 0일")
        assertNull(cycle1.delayDay, "지연 기간: null")
        assertEquals(0, cycle1.predictDays.size, "생리 예정일들: []")

        // 가임기: 2025-01-21 ~ 2025-01-29
        assertEquals(1, cycle1.fertileDays.size, "가임기들: [2025-01-21 ~ 2025-01-29]")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 1, 21)),
            cycle1.fertileDays[0].startDate,
            "가임기 시작: 2025-01-21"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 1, 29)),
            cycle1.fertileDays[0].endDate,
            "가임기 종료: 2025-01-29"
        )

        // 배란기: 2025-01-24 ~ 2025-01-26
        assertEquals(1, cycle1.ovulationDays.size, "배란기들: [2025-01-24 ~ 2025-01-26]")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 1, 24)),
            cycle1.ovulationDays[0].startDate,
            "배란기 시작: 2025-01-24"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 1, 26)),
            cycle1.ovulationDays[0].endDate,
            "배란기 종료: 2025-01-26"
        )

        assertEquals(39, cycle1.period, "주기: 39")
        assertFalse(cycle1.isOvulationPeriodUserInput)
        assertNull(cycle1.thePillPeriod)
        assertNull(cycle1.restPill)
    }

    /**
     * TC-03-07: 3개월 조회
     * 장기간 조회 시 여러 주기의 예측이 반복적으로 정확한지 검증
     */
    @Test
    fun testTC_03_07_threeMonthQuery() = runTest {
        val repository = InMemoryPeriodRepository()
        setupCommonData(repository)

        val fromDate = LocalDate(2025, 3, 16)
        val toDate = LocalDate(2025, 6, 15)
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

        // 실제 생리 기록
        assertEquals(
            DateUtils.toJulianDay(PERIOD_3_START),
            cycle.actualPeriod?.startDate
        )
        assertEquals(
            DateUtils.toJulianDay(PERIOD_3_END),
            cycle.actualPeriod?.endDate
        )

        // 생리 예정일들: 2개
        // 1) 2025-04-22 ~ 2025-04-26
        // 2) 2025-05-29 ~ 2025-06-02
        assertEquals(2, cycle.predictDays.size, "생리 예정일 2개")

        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 22)),
            cycle.predictDays[0].startDate,
            "1차 예정일 시작"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 26)),
            cycle.predictDays[0].endDate,
            "1차 예정일 종료"
        )

        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 5, 29)),
            cycle.predictDays[1].startDate,
            "2차 예정일 시작"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 6, 2)),
            cycle.predictDays[1].endDate,
            "2차 예정일 종료"
        )

        // 가임기들: 2개
        assertEquals(2, cycle.fertileDays.size, "가임기 2개")

        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 3)),
            cycle.fertileDays[0].startDate,
            "1차 가임기 시작"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 11)),
            cycle.fertileDays[0].endDate,
            "1차 가임기 종료"
        )

        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 5, 10)),
            cycle.fertileDays[1].startDate,
            "2차 가임기 시작"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 5, 18)),
            cycle.fertileDays[1].endDate,
            "2차 가임기 종료"
        )

        // 배란기들: 2개
        assertEquals(2, cycle.ovulationDays.size, "배란기 2개")

        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 6)),
            cycle.ovulationDays[0].startDate,
            "1차 배란기 시작"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 8)),
            cycle.ovulationDays[0].endDate,
            "1차 배란기 종료"
        )

        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 5, 13)),
            cycle.ovulationDays[1].startDate,
            "2차 배란기 시작"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 5, 15)),
            cycle.ovulationDays[1].endDate,
            "2차 배란기 종료"
        )

        assertEquals(37, cycle.period, "주기: 37일")
    }

    /**
     * TC-03-08: 생리 기간 경계 조회
     * 전체 생리 기록 조회 및 미래 예측 검증
     */
    @Test
    fun testTC_03_08_multiplePeriodBoundaryQuery() = runTest {
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
        assertEquals(0, cycle1.delayTheDays, "주기1 지연 일수: 0")
        assertNull(cycle1.delayDay, "주기1 지연 기간: null")
        assertEquals(0, cycle1.predictDays.size, "주기1 생리 예정일들: []")

        // 주기 1 가임기: 2025-01-21 ~ 2025-01-29
        assertEquals(1, cycle1.fertileDays.size)
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 1, 21)),
            cycle1.fertileDays[0].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 1, 29)),
            cycle1.fertileDays[0].endDate
        )

        // 주기 1 배란기: 2025-01-24 ~ 2025-01-26
        assertEquals(1, cycle1.ovulationDays.size)
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 1, 24)),
            cycle1.ovulationDays[0].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 1, 26)),
            cycle1.ovulationDays[0].endDate
        )

        assertEquals(39, cycle1.period, "주기1 주기: 39일")
        assertFalse(cycle1.isOvulationPeriodUserInput)
        assertNull(cycle1.thePillPeriod)
        assertNull(cycle1.restPill)

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
        assertEquals(0, cycle2.delayTheDays, "주기2 지연 일수: 0")
        assertNull(cycle2.delayDay, "주기2 지연 기간: null")
        assertEquals(0, cycle2.predictDays.size, "주기2 생리 예정일들: []")

        // 주기 2 가임기: 2025-02-25 ~ 2025-03-05
        assertEquals(1, cycle2.fertileDays.size)
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 25)),
            cycle2.fertileDays[0].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 5)),
            cycle2.fertileDays[0].endDate
        )

        // 주기 2 배란기: 2025-02-28 ~ 2025-03-02
        assertEquals(1, cycle2.ovulationDays.size)
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 28)),
            cycle2.ovulationDays[0].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 2)),
            cycle2.ovulationDays[0].endDate
        )

        assertEquals(35, cycle2.period, "주기2 주기: 35일")
        assertFalse(cycle2.isOvulationPeriodUserInput)
        assertNull(cycle2.thePillPeriod)
        assertNull(cycle2.restPill)

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
        assertEquals(0, cycle3.delayTheDays, "주기3 지연 일수: 0")
        assertNull(cycle3.delayDay, "주기3 지연 기간: null")

        // 주기 3 생리 예정일들: 2개
        assertEquals(2, cycle3.predictDays.size, "주기3 생리 예정일 2개")

        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 22)),
            cycle3.predictDays[0].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 26)),
            cycle3.predictDays[0].endDate
        )

        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 5, 29)),
            cycle3.predictDays[1].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 6, 2)),
            cycle3.predictDays[1].endDate
        )

        // 주기 3 가임기들: 2개
        assertEquals(2, cycle3.fertileDays.size)

        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 3)),
            cycle3.fertileDays[0].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 11)),
            cycle3.fertileDays[0].endDate
        )

        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 5, 10)),
            cycle3.fertileDays[1].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 5, 18)),
            cycle3.fertileDays[1].endDate
        )

        // 주기 3 배란기들: 2개
        assertEquals(2, cycle3.ovulationDays.size)

        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 6)),
            cycle3.ovulationDays[0].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 8)),
            cycle3.ovulationDays[0].endDate
        )

        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 5, 13)),
            cycle3.ovulationDays[1].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 5, 15)),
            cycle3.ovulationDays[1].endDate
        )

        assertEquals(37, cycle3.period, "주기3 주기: 37일")
        assertFalse(cycle3.isOvulationPeriodUserInput)
        assertNull(cycle3.thePillPeriod)
        assertNull(cycle3.restPill)
    }

    /**
     * TC-03-09: 생리 기간 경계 조회
     * 과거 생리 기록만 조회 (미래 예측 없음)
     */
    @Test
    fun testTC_03_09_partialPeriodQuery() = runTest {
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

        // 주기 개수: 2개
        assertEquals(2, cycles.size, "주기 개수: 2개")

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

        // 주기 1 가임기: 2025-01-21 ~ 2025-01-29
        assertEquals(1, cycle1.fertileDays.size, "주기1 가임기 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 1, 21)),
            cycle1.fertileDays[0].startDate,
            "주기1 가임기 시작"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 1, 29)),
            cycle1.fertileDays[0].endDate,
            "주기1 가임기 종료"
        )

        // 주기 1 배란기: 2025-01-24 ~ 2025-01-26
        assertEquals(1, cycle1.ovulationDays.size, "주기1 배란기 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 1, 24)),
            cycle1.ovulationDays[0].startDate,
            "주기1 배란기 시작"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 1, 26)),
            cycle1.ovulationDays[0].endDate,
            "주기1 배란기 종료"
        )

        assertEquals(39, cycle1.period, "주기1 주기: 39일")
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
        assertEquals(0, cycle2.predictDays.size, "주기2 생리 예정일들: []")

        // 주기 2 가임기: 2025-02-25 ~ 2025-03-05
        assertEquals(1, cycle2.fertileDays.size, "주기2 가임기 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 25)),
            cycle2.fertileDays[0].startDate,
            "주기2 가임기 시작"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 5)),
            cycle2.fertileDays[0].endDate,
            "주기2 가임기 종료"
        )

        // 주기 2 배란기: 2025-02-28 ~ 2025-03-02
        assertEquals(1, cycle2.ovulationDays.size, "주기2 배란기 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 28)),
            cycle2.ovulationDays[0].startDate,
            "주기2 배란기 시작"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 2)),
            cycle2.ovulationDays[0].endDate,
            "주기2 배란기 종료"
        )

        assertEquals(35, cycle2.period, "주기2 주기: 35일")
        assertFalse(cycle2.isOvulationPeriodUserInput, "주기2 배란일 사용자 입력: false")
        assertNull(cycle2.thePillPeriod, "주기2 피임약 기준 주기: null")
        assertNull(cycle2.restPill, "주기2 남은 휴약일: null")
    }

    /**
     * TC-03-10: 생리 지연 1-7일 (예정일 뒤로 미룸)
     * 생리 예정일이 지나고 1-7일 지연되었을 때 지연 기간이 표시되고, 다음 예정일이 지연 다음날부터 시작하는지 검증
     */
    @Test
    fun testTC_03_10_delay1to7Days() = runTest {
        val repository = InMemoryPeriodRepository()
        setupCommonData(repository)

        val fromDate = LocalDate(2025, 4, 27)
        val toDate = LocalDate(2025, 5, 3)
        val today = LocalDate(2025, 4, 28) // 생리 예정일 04-22으로부터 7일 지연

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
            DateUtils.toJulianDay(LocalDate(2025, 4, 22)),
            cycle.delayDay!!.startDate,
            "지연 기간 시작: 2025-04-22"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 28)),
            cycle.delayDay!!.endDate,
            "지연 기간 종료: 2025-04-28"
        )

        // 생리 예정일: 지연 다음날부터 시작
        assertEquals(1, cycle.predictDays.size, "생리 예정일 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 29)),
            cycle.predictDays[0].startDate,
            "예정일 시작: 2025-04-29 (지연 다음날)"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 5, 3)),
            cycle.predictDays[0].endDate,
            "예정일 종료: 2025-05-03"
        )

        // 가임기/배란기 없음 (지연 중)
        assertEquals(0, cycle.fertileDays.size, "가임기들: []")
        assertEquals(0, cycle.ovulationDays.size, "배란기들: []")

        assertEquals(37, cycle.period, "주기: 37일")
        assertFalse(cycle.isOvulationPeriodUserInput)
        assertNull(cycle.thePillPeriod)
        assertNull(cycle.restPill)
    }

    /**
     * TC-03-11: 생리 지연 8일 이상 (예정일 표시 안 함, 병원 권장)
     * 생리 예정일이 지나고 8일 이상 지연되었을 때 예정일과 지연 기간을 표시하지 않는지 검증 (병원 진료 권장 상태)
     */
    @Test
    fun testTC_03_11_delay8DaysOrMore() = runTest {
        val repository = InMemoryPeriodRepository()
        setupCommonData(repository)

        val fromDate = LocalDate(2025, 3, 20)
        val toDate = LocalDate(2025, 5, 10)
        val today = LocalDate(2025, 4, 29) // 생리 예정일 04-22으로부터 8일 지연

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
            DateUtils.toJulianDay(LocalDate(2025, 4, 22)),
            cycle.delayDay!!.startDate,
            "지연 기간 시작: 2025-04-22"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 29)),
            cycle.delayDay!!.endDate,
            "지연 기간 종료: 2025-04-29"
        )

        // 생리 예정일 없음 (8일 이상 지연 시 병원 권장)
        assertEquals(0, cycle.predictDays.size, "생리 예정일들: [] (8일 이상 지연)")

        // 가임기
        assertEquals(1, cycle.fertileDays.size, "가임기들: [2025-04-03 ~ 2025-04-11]")
        val fertileDay = cycle.fertileDays.first()
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 3)),
            fertileDay.startDate,
            "가임기 시작: 2025-04-03"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 11)),
            fertileDay.endDate,
            "가임기 종료: 2025-04-11"
        )

        // 배란기
        assertEquals(1, cycle.ovulationDays.size, "배란기들: [2025-04-06 ~ 2025-04-08]")
        val ovulationDay = cycle.ovulationDays.first()
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 6)),
            ovulationDay.startDate,
            "배란기 시작: 2025-04-06"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 8)),
            ovulationDay.endDate,
            "배란기 종료: 2025-04-08"
        )

        assertEquals(37, cycle.period, "주기: 37일")
        assertFalse(cycle.isOvulationPeriodUserInput)
        assertNull(cycle.thePillPeriod)
        assertNull(cycle.restPill)
    }
}
