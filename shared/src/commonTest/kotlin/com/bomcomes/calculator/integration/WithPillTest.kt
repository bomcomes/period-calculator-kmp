package com.bomcomes.calculator.integration

import com.bomcomes.calculator.PeriodCalculator
import com.bomcomes.calculator.models.*
import com.bomcomes.calculator.repository.InMemoryPeriodRepository
import com.bomcomes.calculator.utils.DateUtils
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.*

/**
 * 피임약 복용 테스트 (10개)
 *
 * 문서 참조: test-cases/docs/05-with-pill.md
 *
 * 5일 규칙:
 * - 피임약을 예정일 5일 전에 복용해야 피임약 기반 계산 적용
 * - 5일 이후 복용 시 일반 주기 계산 유지, 배란기/가임기 노출
 *
 * 예정일 계산: 피임약 시작일 + 복용일 + 2 (휴약 3일째부터 생리 예정)
 */
class WithPillTest {
    companion object {
        // 기본 생리 기록: 2025-02-23 ~ 02-27
        val LAST_PERIOD_START = LocalDate(2025, 2, 23)
        val LAST_PERIOD_END = LocalDate(2025, 2, 27)

        // 피임약 시작일: 2025-03-01
        val PILL_START = LocalDate(2025, 3, 1)

        // 피임약 설정
        const val DEFAULT_PILL_COUNT = 21
        const val DEFAULT_REST_DAYS = 7
        const val DEFAULT_CYCLE = 28
    }

    private fun setupBasicData(repository: InMemoryPeriodRepository) {
        repository.addPeriod(PeriodRecord(
            pk = "1",
            startDate = DateUtils.toJulianDay(LAST_PERIOD_START),
            endDate = DateUtils.toJulianDay(LAST_PERIOD_END)
        ))

        repository.setPeriodSettings(PeriodSettings(
            manualAverageCycle = DEFAULT_CYCLE,
            manualAverageDay = 5,
            isAutoCalc = false
        ))

        repository.setPillSettings(PillSettings(
            isCalculatingWithPill = true,
            pillCount = DEFAULT_PILL_COUNT,
            restPill = DEFAULT_REST_DAYS
        ))

        repository.addPillPackage(PillPackage(
            packageStart = DateUtils.toJulianDay(PILL_START),
            pillCount = DEFAULT_PILL_COUNT,
            restDays = DEFAULT_REST_DAYS
        ))
    }

    /**
     * TC-05-01: 기본 복용 (1개월)
     * 피임약 패키지 전체 주기 조회
     */
    @Test
    fun testTC_05_01_basicMonthlyView() = runTest {
        val repository = InMemoryPeriodRepository()
        setupBasicData(repository)

        val fromDate = LocalDate(2025, 3, 1)
        val toDate = LocalDate(2025, 3, 28)
        val today = LocalDate(2025, 3, 23)

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository = repository,
            fromDate = DateUtils.toJulianDay(fromDate),
            toDate = DateUtils.toJulianDay(toDate),
            today = DateUtils.toJulianDay(today)
        )

        assertEquals(1, cycles.size)
        val cycle = cycles.first()

        assertEquals("1", cycle.pk)
        // 예정일: 03-01 + 21 + 2 = 03-24 시작
        assertEquals(1, cycle.predictDays.size, "예정일 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 24)),
            cycle.predictDays[0].startDate,
            "예정일 시작: 03-24"
        )
        assertEquals(0, cycle.delayTheDays, "지연 없음")
        assertEquals(0, cycle.ovulationDays.size, "배란기 없음")
        assertEquals(0, cycle.fertileDays.size, "가임기 없음")
    }

    /**
     * TC-05-02: 휴약기 0일 (연속 복용)
     * 휴약 없이 연속 복용 시 예정일 없음
     */
    @Test
    fun testTC_05_02_continuousUse() = runTest {
        val repository = InMemoryPeriodRepository()

        repository.addPeriod(PeriodRecord(
            pk = "1",
            startDate = DateUtils.toJulianDay(LAST_PERIOD_START),
            endDate = DateUtils.toJulianDay(LAST_PERIOD_END)
        ))

        repository.setPeriodSettings(PeriodSettings(
            manualAverageCycle = 21,
            manualAverageDay = 5,
            isAutoCalc = false
        ))

        // 휴약기 0일 설정
        repository.setPillSettings(PillSettings(
            isCalculatingWithPill = true,
            pillCount = 21,
            restPill = 0
        ))

        repository.addPillPackage(PillPackage(
            packageStart = DateUtils.toJulianDay(PILL_START),
            pillCount = 21,
            restDays = 0
        ))

        val fromDate = LocalDate(2025, 3, 15)
        val toDate = LocalDate(2025, 3, 21)
        val today = LocalDate(2025, 3, 20)

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository = repository,
            fromDate = DateUtils.toJulianDay(fromDate),
            toDate = DateUtils.toJulianDay(toDate),
            today = DateUtils.toJulianDay(today)
        )

        assertEquals(1, cycles.size)
        val cycle = cycles.first()

        assertEquals(0, cycle.predictDays.size, "예정일 없음 (휴약기 0일)")
        assertEquals(0, cycle.delayTheDays, "지연 없음")
        assertEquals(0, cycle.ovulationDays.size, "배란기 없음")
        assertEquals(0, cycle.fertileDays.size, "가임기 없음")
    }

    /**
     * TC-05-03: 여러 패키지
     * 3개월 연속 패키지 - 마지막 패키지 예정일만 반환
     */
    @Test
    fun testTC_05_03_multiplePackages() = runTest {
        val repository = InMemoryPeriodRepository()
        setupBasicData(repository)

        // 추가 패키지
        repository.addPillPackage(PillPackage(
            packageStart = DateUtils.toJulianDay(LocalDate(2025, 3, 29)),
            pillCount = DEFAULT_PILL_COUNT,
            restDays = DEFAULT_REST_DAYS
        ))
        repository.addPillPackage(PillPackage(
            packageStart = DateUtils.toJulianDay(LocalDate(2025, 4, 26)),
            pillCount = DEFAULT_PILL_COUNT,
            restDays = DEFAULT_REST_DAYS
        ))

        val fromDate = LocalDate(2025, 3, 1)
        val toDate = LocalDate(2025, 5, 25)
        val today = LocalDate(2025, 5, 18)

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository = repository,
            fromDate = DateUtils.toJulianDay(fromDate),
            toDate = DateUtils.toJulianDay(toDate),
            today = DateUtils.toJulianDay(today)
        )

        val cycle = cycles.find { it.pk == "1" }!!

        // 마지막 패키지 예정일만: 04-26 + 21 + 2 = 05-19
        assertEquals(1, cycle.predictDays.size, "예정일 1개 (마지막 패키지)")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 5, 19)),
            cycle.predictDays[0].startDate,
            "3차 예정일: 05-19"
        )
        assertEquals(0, cycle.ovulationDays.size, "배란기 없음")
        assertEquals(0, cycle.fertileDays.size, "가임기 없음")
    }

    /**
     * TC-05-04: 지연 1-7일
     * 피임약 복용 중 생리 지연
     */
    @Test
    fun testTC_05_04_delay1to7Days() = runTest {
        val repository = InMemoryPeriodRepository()
        setupBasicData(repository)

        val fromDate = LocalDate(2025, 3, 20)
        val toDate = LocalDate(2025, 4, 5)
        val today = LocalDate(2025, 3, 29) // 예정일(03-24) 이후 6일

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository = repository,
            fromDate = DateUtils.toJulianDay(fromDate),
            toDate = DateUtils.toJulianDay(toDate),
            today = DateUtils.toJulianDay(today)
        )

        assertEquals(1, cycles.size)
        val cycle = cycles.first()

        assertEquals(6, cycle.delayTheDays, "지연 6일")
        assertNotNull(cycle.delayDay, "지연 기간 있음")
        assertEquals(1, cycle.predictDays.size, "예정일 있음")
    }

    /**
     * TC-05-05: 지연 8일 이상
     * 8일 이상 지연 시 예정일 표시 안 함
     */
    @Test
    fun testTC_05_05_delay8DaysOrMore() = runTest {
        val repository = InMemoryPeriodRepository()
        setupBasicData(repository)

        val fromDate = LocalDate(2025, 3, 20)
        val toDate = LocalDate(2025, 4, 10)
        val today = LocalDate(2025, 4, 2) // 예정일(03-24) 이후 10일

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository = repository,
            fromDate = DateUtils.toJulianDay(fromDate),
            toDate = DateUtils.toJulianDay(toDate),
            today = DateUtils.toJulianDay(today)
        )

        assertEquals(1, cycles.size)
        val cycle = cycles.first()

        assertEquals(10, cycle.delayTheDays, "지연 10일")
        assertEquals(0, cycle.predictDays.size, "예정일 없음 (8일 이상 지연)")
    }

    /**
     * TC-05-06: 5일 전 복용 (배란기 숨김)
     * 피임약을 예정일 5일 전에 복용 → 피임약 기반 계산, 배란기/가임기 숨김
     */
    @Test
    fun testTC_05_06_pillStart5DaysBefore() = runTest {
        val repository = InMemoryPeriodRepository()

        repository.addPeriod(PeriodRecord(
            pk = "1",
            startDate = DateUtils.toJulianDay(LAST_PERIOD_START),
            endDate = DateUtils.toJulianDay(LAST_PERIOD_END)
        ))

        repository.setPeriodSettings(PeriodSettings(
            manualAverageCycle = DEFAULT_CYCLE,
            manualAverageDay = 5,
            isAutoCalc = false
        ))

        repository.setPillSettings(PillSettings(
            isCalculatingWithPill = true,
            pillCount = DEFAULT_PILL_COUNT,
            restPill = DEFAULT_REST_DAYS
        ))

        // 일반 예정일: 02-23 + 28 = 03-23
        // 피임약: 예정일(03-23) 5일 전인 03-18 시작
        val pillStart = LocalDate(2025, 3, 18)
        repository.addPillPackage(PillPackage(
            packageStart = DateUtils.toJulianDay(pillStart),
            pillCount = DEFAULT_PILL_COUNT,
            restDays = DEFAULT_REST_DAYS
        ))

        val fromDate = LocalDate(2025, 3, 1)
        val toDate = LocalDate(2025, 4, 20)
        val today = LocalDate(2025, 3, 17)

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository = repository,
            fromDate = DateUtils.toJulianDay(fromDate),
            toDate = DateUtils.toJulianDay(toDate),
            today = DateUtils.toJulianDay(today)
        )

        assertEquals(1, cycles.size)
        val cycle = cycles.first()

        // 피임약 기반 예정일: 03-18 + 21 + 2 = 04-10
        assertEquals(1, cycle.predictDays.size, "예정일 1개 (피임약 기반)")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 10)),
            cycle.predictDays[0].startDate,
            "예정일: 04-10"
        )
        assertEquals(0, cycle.ovulationDays.size, "배란기 없음 (5일 규칙 만족)")
        assertEquals(0, cycle.fertileDays.size, "가임기 없음 (5일 규칙 만족)")
    }

    /**
     * TC-05-07: 5일 이후 복용 (배란기 노출)
     * 피임약을 예정일 5일 전 이후에 복용 → 일반 주기 계산, 배란기/가임기 노출
     */
    @Test
    fun testTC_05_07_pillStartLessThan5Days() = runTest {
        val repository = InMemoryPeriodRepository()

        repository.addPeriod(PeriodRecord(
            pk = "1",
            startDate = DateUtils.toJulianDay(LAST_PERIOD_START),
            endDate = DateUtils.toJulianDay(LAST_PERIOD_END)
        ))

        repository.setPeriodSettings(PeriodSettings(
            manualAverageCycle = DEFAULT_CYCLE,
            manualAverageDay = 5,
            isAutoCalc = false
        ))

        repository.setPillSettings(PillSettings(
            isCalculatingWithPill = true,
            pillCount = DEFAULT_PILL_COUNT,
            restPill = DEFAULT_REST_DAYS
        ))

        // 일반 예정일: 02-23 + 28 = 03-23
        // 피임약: 예정일(03-23) 3일 전인 03-20 시작 (5일 이전 아님)
        val pillStart = LocalDate(2025, 3, 20)
        repository.addPillPackage(PillPackage(
            packageStart = DateUtils.toJulianDay(pillStart),
            pillCount = DEFAULT_PILL_COUNT,
            restDays = DEFAULT_REST_DAYS
        ))

        val fromDate = LocalDate(2025, 3, 1)
        val toDate = LocalDate(2025, 4, 5)
        val today = LocalDate(2025, 3, 15)

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository = repository,
            fromDate = DateUtils.toJulianDay(fromDate),
            toDate = DateUtils.toJulianDay(toDate),
            today = DateUtils.toJulianDay(today)
        )

        assertEquals(1, cycles.size)
        val cycle = cycles.first()

        // 일반 예정일: 02-23 + 28 = 03-23 ~ 03-27
        assertEquals(1, cycle.predictDays.size, "예정일 1개 (일반 주기)")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 23)),
            cycle.predictDays[0].startDate,
            "예정일: 03-23 (일반 주기)"
        )
        assertTrue(cycle.ovulationDays.isNotEmpty(), "배란기 있음 (5일 규칙 미만족)")
        assertTrue(cycle.fertileDays.isNotEmpty(), "가임기 있음 (5일 규칙 미만족)")
    }

    /**
     * TC-05-08: 생리 기간 사이 - 5일 전 복용 (배란기 숨김)
     * 첫 생리 후 피임약 복용, 두 번째 생리 기간에서 5일 규칙 검증
     */
    @Test
    fun testTC_05_08_betweenPeriods_5DaysBefore() = runTest {
        val repository = InMemoryPeriodRepository()

        // 생리 기록 2개
        repository.addPeriod(PeriodRecord(
            pk = "1",
            startDate = DateUtils.toJulianDay(LAST_PERIOD_START), // 02-23
            endDate = DateUtils.toJulianDay(LAST_PERIOD_END)      // 02-27
        ))
        repository.addPeriod(PeriodRecord(
            pk = "2",
            startDate = DateUtils.toJulianDay(LocalDate(2025, 3, 24)), // 1차 휴약기에 생리
            endDate = DateUtils.toJulianDay(LocalDate(2025, 3, 28))
        ))

        repository.setPeriodSettings(PeriodSettings(
            manualAverageCycle = DEFAULT_CYCLE,
            manualAverageDay = 5,
            isAutoCalc = false
        ))

        repository.setPillSettings(PillSettings(
            isCalculatingWithPill = true,
            pillCount = DEFAULT_PILL_COUNT,
            restPill = DEFAULT_REST_DAYS
        ))

        // 1차 패키지: 03-01 (첫 생리 이후)
        // 2차 패키지: 03-29 (두 번째 생리 이후, 예정일 04-21 기준 23일 전 = 5일 규칙 만족)
        repository.addPillPackage(PillPackage(
            packageStart = DateUtils.toJulianDay(PILL_START), // 03-01
            pillCount = DEFAULT_PILL_COUNT,
            restDays = DEFAULT_REST_DAYS
        ))
        repository.addPillPackage(PillPackage(
            packageStart = DateUtils.toJulianDay(LocalDate(2025, 3, 29)),
            pillCount = DEFAULT_PILL_COUNT,
            restDays = DEFAULT_REST_DAYS
        ))

        val fromDate = LocalDate(2025, 4, 1)
        val toDate = LocalDate(2025, 4, 30)
        val today = LocalDate(2025, 4, 15)

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository = repository,
            fromDate = DateUtils.toJulianDay(fromDate),
            toDate = DateUtils.toJulianDay(toDate),
            today = DateUtils.toJulianDay(today)
        )

        val cycle = cycles.find { it.pk == "2" }!!

        // 2차 예정일: 03-29 + 21 + 2 = 04-21
        assertEquals(1, cycle.predictDays.size, "예정일 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 21)),
            cycle.predictDays[0].startDate,
            "2차 예정일: 04-21"
        )
        assertEquals(0, cycle.ovulationDays.size, "배란기 없음 (5일 규칙 만족)")
        assertEquals(0, cycle.fertileDays.size, "가임기 없음 (5일 규칙 만족)")
    }

    /**
     * TC-05-09: 생리 기간 사이 - 5일 이후 복용 (배란기 노출)
     * 생리 후 늦게 피임약 시작 시 배란기/가임기 노출
     */
    @Test
    fun testTC_05_09_betweenPeriods_lessThan5Days() = runTest {
        val repository = InMemoryPeriodRepository()

        // 생리 기록
        repository.addPeriod(PeriodRecord(
            pk = "1",
            startDate = DateUtils.toJulianDay(LocalDate(2025, 3, 24)),
            endDate = DateUtils.toJulianDay(LocalDate(2025, 3, 28))
        ))

        repository.setPeriodSettings(PeriodSettings(
            manualAverageCycle = DEFAULT_CYCLE,
            manualAverageDay = 5,
            isAutoCalc = false
        ))

        repository.setPillSettings(PillSettings(
            isCalculatingWithPill = true,
            pillCount = DEFAULT_PILL_COUNT,
            restPill = DEFAULT_REST_DAYS
        ))

        // 일반 예정일: 03-24 + 28 = 04-21
        // 피임약 시작: 04-18 (예정일 3일 전, 5일 이전 아님)
        repository.addPillPackage(PillPackage(
            packageStart = DateUtils.toJulianDay(LocalDate(2025, 4, 18)),
            pillCount = DEFAULT_PILL_COUNT,
            restDays = DEFAULT_REST_DAYS
        ))

        val fromDate = LocalDate(2025, 4, 1)
        val toDate = LocalDate(2025, 4, 30)
        val today = LocalDate(2025, 4, 15)

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository = repository,
            fromDate = DateUtils.toJulianDay(fromDate),
            toDate = DateUtils.toJulianDay(toDate),
            today = DateUtils.toJulianDay(today)
        )

        assertEquals(1, cycles.size)
        val cycle = cycles.first()

        // 일반 예정일 적용: 03-24 + 28 = 04-21
        assertEquals(1, cycle.predictDays.size, "예정일 1개 (일반 주기)")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 21)),
            cycle.predictDays[0].startDate,
            "예정일: 04-21 (일반 주기)"
        )
        assertTrue(cycle.ovulationDays.isNotEmpty(), "배란기 있음 (5일 규칙 미만족)")
        assertTrue(cycle.fertileDays.isNotEmpty(), "가임기 있음 (5일 규칙 미만족)")
    }

    /**
     * TC-05-10: 피임약 중단
     * 피임약 중단 후 일반 주기로 복귀
     */
    @Test
    fun testTC_05_10_pillDiscontinued() = runTest {
        val repository = InMemoryPeriodRepository()

        // 피임약 중단 후 새 생리 기록
        repository.addPeriod(PeriodRecord(
            pk = "5",
            startDate = DateUtils.toJulianDay(LocalDate(2025, 3, 23)),
            endDate = DateUtils.toJulianDay(LocalDate(2025, 3, 27))
        ))

        repository.setPeriodSettings(PeriodSettings(
            manualAverageCycle = DEFAULT_CYCLE,
            manualAverageDay = 5,
            isAutoCalc = false
        ))

        // 피임약 중단
        repository.setPillSettings(PillSettings(
            isCalculatingWithPill = false,
            pillCount = DEFAULT_PILL_COUNT,
            restPill = DEFAULT_REST_DAYS
        ))

        val fromDate = LocalDate(2025, 4, 1)
        val toDate = LocalDate(2025, 4, 30)
        val today = LocalDate(2025, 4, 20)

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository = repository,
            fromDate = DateUtils.toJulianDay(fromDate),
            toDate = DateUtils.toJulianDay(toDate),
            today = DateUtils.toJulianDay(today)
        )

        assertEquals(1, cycles.size)
        val cycle = cycles.first()

        assertEquals("5", cycle.pk)
        // 일반 예정일: 03-23 + 28 = 04-20
        assertEquals(1, cycle.predictDays.size, "예정일 있음")
        // 피임약 중단 후 배란기/가임기 노출 가능
        assertTrue(
            cycle.ovulationDays.isNotEmpty() || cycle.fertileDays.isNotEmpty(),
            "배란기/가임기 있음 (피임약 중단)"
        )
    }
}
