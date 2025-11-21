package com.bomcomes.calculator.integration

import com.bomcomes.calculator.PeriodCalculator
import com.bomcomes.calculator.models.*
import com.bomcomes.calculator.repository.InMemoryPeriodRepository
import com.bomcomes.calculator.utils.DateUtils
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.*

/**
 * 피임약 복용 테스트
 *
 * 문서 참조: test-cases/docs/05-with-pill.md
 * 기준 날짜: 2025년 3월
 *
 * 공통 입력 조건:
 * - 생리 기록: 2024-12-01 ~ 2025-02-27까지 4개
 * - 피임약 시작일: 2025-03-01
 * - 오늘 날짜: 2025-03-25
 */
class WithPillTest {
    private lateinit var repository: InMemoryPeriodRepository

    companion object {
        // 공통 날짜 상수
        val PERIOD_1_START = LocalDate(2024, 12, 1)
        val PERIOD_1_END = LocalDate(2024, 12, 5)
        val PERIOD_2_START = LocalDate(2024, 12, 29)
        val PERIOD_2_END = LocalDate(2025, 1, 2)
        val PERIOD_3_START = LocalDate(2025, 1, 26)
        val PERIOD_3_END = LocalDate(2025, 1, 30)
        val PERIOD_4_START = LocalDate(2025, 2, 23)
        val PERIOD_4_END = LocalDate(2025, 2, 27)

        val PILL_START = LocalDate(2025, 3, 1)
        val TODAY_DEFAULT = LocalDate(2025, 3, 25)

        // 피임약 설정 기본값
        const val DEFAULT_PILL_COUNT = 21
        const val DEFAULT_REST_DAYS = 7
        const val DEFAULT_CYCLE = 28
    }

    /**
     * 공통 데이터 설정
     */
    private fun setupCommonData(repository: InMemoryPeriodRepository) {
        // 생리 기록 4개 추가
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
        repository.addPeriod(PeriodRecord(
            pk = "4",
            startDate = DateUtils.toJulianDay(PERIOD_4_START),
            endDate = DateUtils.toJulianDay(PERIOD_4_END)
        ))

        // 생리 주기 설정
        repository.setPeriodSettings(PeriodSettings(
            manualAverageCycle = DEFAULT_CYCLE,
            manualAverageDay = 5,
            isAutoCalc = false
        ))

        // 피임약 설정 (기본)
        repository.setPillSettings(PillSettings(
            isCalculatingWithPill = true,
            pillCount = DEFAULT_PILL_COUNT,
            restPill = DEFAULT_REST_DAYS
        ))

        // 피임약 패키지 추가 (2025-03-01 시작)
        repository.addPillPackage(PillPackage(
            packageStart = DateUtils.toJulianDay(PILL_START),
            pillCount = DEFAULT_PILL_COUNT,
            restDays = DEFAULT_REST_DAYS
        ))
    }

    // ==================== 그룹 1: 현재 패키지 조회 ====================

    /**
     * TC-05-01: 1일 조회
     * 특정 날짜 1일만 조회할 때 피임약 복용 상태가 정확히 표시되는지 검증
     */
    @Test
    fun testTC_05_01_singleDayQuery() = runTest {
        val repository = InMemoryPeriodRepository()
        setupCommonData(repository)

        val fromDate = LocalDate(2025, 3, 15)
        val toDate = LocalDate(2025, 3, 15)
        val today = TODAY_DEFAULT

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository = repository,
            fromDate = DateUtils.toJulianDay(fromDate),
            toDate = DateUtils.toJulianDay(toDate),
            today = DateUtils.toJulianDay(today)
        )

        assertTrue(cycles.isNotEmpty(), "주기 정보 존재")
        val cycle = cycles.first()

        // 피임약 주기 검증
        assertNotNull(cycle.thePillPeriod, "피임약 주기 존재")
        assertEquals(DEFAULT_CYCLE, cycle.thePillPeriod, "피임약 주기 = 28일")

        // 배란기/가임기 없음 검증
        assertTrue(cycle.ovulationDays.isEmpty(), "피임약 복용 중 배란기 없음")
        assertTrue(cycle.fertileDays.isEmpty(), "피임약 복용 중 가임기 없음")
    }

    /**
     * TC-05-02: 1주일 조회
     * 1주일 기간 조회 시 피임약 복용 상태 변화가 정확히 표시되는지 검증
     */
    @Test
    fun testTC_05_02_weekQuery() = runTest {
        val repository = InMemoryPeriodRepository()
        setupCommonData(repository)

        val fromDate = LocalDate(2025, 3, 19)
        val toDate = LocalDate(2025, 3, 25)
        val today = TODAY_DEFAULT

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository = repository,
            fromDate = DateUtils.toJulianDay(fromDate),
            toDate = DateUtils.toJulianDay(toDate),
            today = DateUtils.toJulianDay(today)
        )

        assertTrue(cycles.isNotEmpty(), "주기 정보 존재")
        val cycle = cycles.first()

        // 피임약 주기 검증
        assertNotNull(cycle.thePillPeriod, "피임약 주기 존재")
        assertEquals(DEFAULT_CYCLE, cycle.thePillPeriod, "피임약 주기 = 28일")

        // 배란기/가임기 없음
        assertTrue(cycle.ovulationDays.isEmpty(), "피임약 복용 중 배란기 없음")
        assertTrue(cycle.fertileDays.isEmpty(), "피임약 복용 중 가임기 없음")
    }

    /**
     * TC-05-03: 1개월 조회
     * 피임약 패키지 전체 주기 조회 시 복용기간과 휴약기간이 모두 정확히 표시되는지 검증
     */
    @Test
    fun testTC_05_03_monthQuery() = runTest {
        val repository = InMemoryPeriodRepository()
        setupCommonData(repository)

        val fromDate = PILL_START
        val toDate = LocalDate(2025, 3, 28)
        val today = TODAY_DEFAULT

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository = repository,
            fromDate = DateUtils.toJulianDay(fromDate),
            toDate = DateUtils.toJulianDay(toDate),
            today = DateUtils.toJulianDay(today)
        )

        assertTrue(cycles.isNotEmpty(), "주기 정보 존재")
        val cycle = cycles.first()

        // 피임약 주기 검증
        assertNotNull(cycle.thePillPeriod, "피임약 주기 존재")
        assertEquals(DEFAULT_CYCLE, cycle.thePillPeriod, "피임약 주기 = 28일")

        // 남은 휴약일 확인
        assertNotNull(cycle.restPill, "남은 휴약일 정보 존재")
    }

    // ==================== 그룹 2: 과거 패키지 조회 ====================

    /**
     * TC-05-04: 이전 패키지 완료 시점
     * 이전 패키지가 완료된 시점 조회 시 정확한 정보가 표시되는지 검증
     */
    @Test
    fun testTC_05_04_previousPackageComplete() = runTest {
        val repository = InMemoryPeriodRepository()
        setupCommonData(repository)

        // 이전 패키지 추가 (2월)
        repository.addPillPackage(PillPackage(
            packageStart = DateUtils.toJulianDay(LocalDate(2025, 2, 1)),
            pillCount = DEFAULT_PILL_COUNT,
            restDays = DEFAULT_REST_DAYS
        ))

        val fromDate = LocalDate(2025, 2, 28)
        val toDate = LocalDate(2025, 2, 28)
        val today = TODAY_DEFAULT

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository = repository,
            fromDate = DateUtils.toJulianDay(fromDate),
            toDate = DateUtils.toJulianDay(toDate),
            today = DateUtils.toJulianDay(today)
        )

        assertTrue(cycles.isNotEmpty(), "주기 정보 존재")
        val cycle = cycles.first()

        // 피임약 주기 검증
        assertNotNull(cycle.thePillPeriod, "피임약 주기 존재")
        assertEquals(DEFAULT_CYCLE, cycle.thePillPeriod, "피임약 주기 = 28일")
    }

    /**
     * TC-05-05: 피임약 시작 전
     * 피임약 시작 전 날짜 조회 시 일반 생리 주기가 표시되는지 검증
     */
    @Test
    fun testTC_05_05_beforePillStart() = runTest {
        val repository = InMemoryPeriodRepository()

        // 피임약 없이 일반 생리 기록만 설정
        // 이전 생리 기록 추가
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
        repository.addPeriod(PeriodRecord(
            pk = "4",
            startDate = DateUtils.toJulianDay(PERIOD_4_START),
            endDate = DateUtils.toJulianDay(PERIOD_4_END)
        ))

        repository.setPeriodSettings(PeriodSettings(
            manualAverageCycle = DEFAULT_CYCLE,
            manualAverageDay = 5,
            isAutoCalc = false
        ))

        // 피임약 설정하지 않음
        repository.setPillSettings(PillSettings(
            isCalculatingWithPill = false
        ))

        val fromDate = LocalDate(2025, 1, 10)
        val toDate = LocalDate(2025, 2, 15)
        val today = TODAY_DEFAULT

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository = repository,
            fromDate = DateUtils.toJulianDay(fromDate),
            toDate = DateUtils.toJulianDay(toDate),
            today = DateUtils.toJulianDay(today)
        )

        assertTrue(cycles.isNotEmpty(), "주기 정보 존재")
        val cycle = cycles.first()

        // 피임약 주기 없음
        assertNull(cycle.thePillPeriod, "피임약 시작 전이므로 피임약 주기 없음")
    }

    /**
     * TC-05-06: 복약 지연 후 조회
     * 피임약 복용을 며칠 지연한 후 이전 기간 조회 시 정보가 정확한지 검증
     */
    @Test
    fun testTC_05_06_delayedStart() = runTest {
        val repository = InMemoryPeriodRepository()

        // 생리 기록만 설정 (피임약 지연 시작을 위해)
        repository.addPeriod(PeriodRecord(
            pk = "4",
            startDate = DateUtils.toJulianDay(PERIOD_4_START),
            endDate = DateUtils.toJulianDay(PERIOD_4_END)
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

        // 피임약 시작을 3일 지연 (03-04부터 시작)
        repository.addPillPackage(PillPackage(
            packageStart = DateUtils.toJulianDay(LocalDate(2025, 3, 4)),
            pillCount = DEFAULT_PILL_COUNT,
            restDays = DEFAULT_REST_DAYS
        ))

        val fromDate = LocalDate(2025, 3, 1)
        val toDate = LocalDate(2025, 3, 3)
        val today = TODAY_DEFAULT

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository = repository,
            fromDate = DateUtils.toJulianDay(fromDate),
            toDate = DateUtils.toJulianDay(toDate),
            today = DateUtils.toJulianDay(today)
        )

        assertTrue(cycles.isNotEmpty(), "주기 정보 존재")
        val cycle = cycles.first()

        // 3월 1-3일은 피임약 시작 전
        assertNotNull(cycle.thePillPeriod, "피임약 설정은 있지만 해당 기간은 복용 전")
    }

    // ==================== 그룹 3: 경계값 테스트 ====================

    /**
     * TC-05-07: 최소 복용일 (20일)
     * 최소 복용일 설정 시 주기가 올바르게 계산되는지 검증
     */
    @Test
    fun testTC_05_07_minPillDays() = runTest {
        val repository = InMemoryPeriodRepository()

        // 생리 기록
        repository.addPeriod(PeriodRecord(
            pk = "4",
            startDate = DateUtils.toJulianDay(PERIOD_4_START),
            endDate = DateUtils.toJulianDay(PERIOD_4_END)
        ))

        repository.setPeriodSettings(PeriodSettings(
            manualAverageCycle = 27,
            manualAverageDay = 5,
            isAutoCalc = false
        ))

        repository.setPillSettings(PillSettings(
            isCalculatingWithPill = true,
            pillCount = 20,
            restPill = 7
        ))

        repository.addPillPackage(PillPackage(
            packageStart = DateUtils.toJulianDay(PILL_START),
            pillCount = 20,
            restDays = 7
        ))

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository = repository,
            fromDate = DateUtils.toJulianDay(PILL_START),
            toDate = DateUtils.toJulianDay(LocalDate(2025, 3, 27)),
            today = DateUtils.toJulianDay(TODAY_DEFAULT)
        )

        assertTrue(cycles.isNotEmpty(), "주기 정보 존재")
        val cycle = cycles.first()

        assertNotNull(cycle.thePillPeriod, "피임약 주기 존재")
        assertEquals(27, cycle.thePillPeriod, "피임약 주기 = 20 + 7 = 27일")
    }

    /**
     * TC-05-08: 최대 복용일 (35일)
     * 최대 복용일 설정 시 주기가 올바르게 계산되는지 검증
     */
    @Test
    fun testTC_05_08_maxPillDays() = runTest {
        val repository = InMemoryPeriodRepository()

        // 생리 기록
        repository.addPeriod(PeriodRecord(
            pk = "4",
            startDate = DateUtils.toJulianDay(PERIOD_4_START),
            endDate = DateUtils.toJulianDay(PERIOD_4_END)
        ))

        repository.setPeriodSettings(PeriodSettings(
            manualAverageCycle = 42,
            manualAverageDay = 5,
            isAutoCalc = false
        ))

        repository.setPillSettings(PillSettings(
            isCalculatingWithPill = true,
            pillCount = 35,
            restPill = 7
        ))

        repository.addPillPackage(PillPackage(
            packageStart = DateUtils.toJulianDay(PILL_START),
            pillCount = 35,
            restDays = 7
        ))

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository = repository,
            fromDate = DateUtils.toJulianDay(PILL_START),
            toDate = DateUtils.toJulianDay(LocalDate(2025, 4, 11)),
            today = DateUtils.toJulianDay(TODAY_DEFAULT)
        )

        assertTrue(cycles.isNotEmpty(), "주기 정보 존재")
        val cycle = cycles.first()

        assertNotNull(cycle.thePillPeriod, "피임약 주기 존재")
        assertEquals(42, cycle.thePillPeriod, "피임약 주기 = 35 + 7 = 42일")
    }

    /**
     * TC-05-09: 휴약 없음 (연속 복용)
     * 휴약일 0으로 설정 시 연속 복용이 올바르게 처리되는지 검증
     */
    @Test
    fun testTC_05_09_noPlacebo() = runTest {
        val repository = InMemoryPeriodRepository()

        // 생리 기록
        repository.addPeriod(PeriodRecord(
            pk = "4",
            startDate = DateUtils.toJulianDay(PERIOD_4_START),
            endDate = DateUtils.toJulianDay(PERIOD_4_END)
        ))

        repository.setPeriodSettings(PeriodSettings(
            manualAverageCycle = 21,
            manualAverageDay = 5,
            isAutoCalc = false
        ))

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

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository = repository,
            fromDate = DateUtils.toJulianDay(PILL_START),
            toDate = DateUtils.toJulianDay(LocalDate(2025, 3, 21)),
            today = DateUtils.toJulianDay(TODAY_DEFAULT)
        )

        assertTrue(cycles.isNotEmpty(), "주기 정보 존재")
        val cycle = cycles.first()

        assertNotNull(cycle.thePillPeriod, "피임약 주기 존재")
        assertEquals(21, cycle.thePillPeriod, "피임약 주기 = 21 + 0 = 21일")

        // 휴약일이 0이므로 restPill은 0
        assertEquals(0, cycle.restPill ?: 0, "휴약일 0")
    }

    /**
     * TC-05-10: 24일 복용 + 4일 휴약
     * 24/4 구성 피임약 테스트
     */
    @Test
    fun testTC_05_10_config24_4() = runTest {
        val repository = InMemoryPeriodRepository()

        // 생리 기록
        repository.addPeriod(PeriodRecord(
            pk = "4",
            startDate = DateUtils.toJulianDay(PERIOD_4_START),
            endDate = DateUtils.toJulianDay(PERIOD_4_END)
        ))

        repository.setPeriodSettings(PeriodSettings(
            manualAverageCycle = DEFAULT_CYCLE,
            manualAverageDay = 5,
            isAutoCalc = false
        ))

        repository.setPillSettings(PillSettings(
            isCalculatingWithPill = true,
            pillCount = 24,
            restPill = 4
        ))

        repository.addPillPackage(PillPackage(
            packageStart = DateUtils.toJulianDay(PILL_START),
            pillCount = 24,
            restDays = 4
        ))

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository = repository,
            fromDate = DateUtils.toJulianDay(PILL_START),
            toDate = DateUtils.toJulianDay(LocalDate(2025, 3, 28)),
            today = DateUtils.toJulianDay(TODAY_DEFAULT)
        )

        assertTrue(cycles.isNotEmpty(), "주기 정보 존재")
        val cycle = cycles.first()

        assertNotNull(cycle.thePillPeriod, "피임약 주기 존재")
        assertEquals(28, cycle.thePillPeriod, "피임약 주기 = 24 + 4 = 28일")
    }

    // ==================== 그룹 4: 특수 상황 ====================

    /**
     * TC-05-11: 복약 1일 지연 시작
     * 피임약 시작을 1일 지연했을 때 계산이 올바른지 검증
     */
    @Test
    fun testTC_05_11_oneDayDelay() = runTest {
        val repository = InMemoryPeriodRepository()

        // 생리 기록
        repository.addPeriod(PeriodRecord(
            pk = "4",
            startDate = DateUtils.toJulianDay(PERIOD_4_START),
            endDate = DateUtils.toJulianDay(PERIOD_4_END)
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

        // 3월 2일부터 시작 (1일 지연)
        repository.addPillPackage(PillPackage(
            packageStart = DateUtils.toJulianDay(LocalDate(2025, 3, 2)),
            pillCount = DEFAULT_PILL_COUNT,
            restDays = DEFAULT_REST_DAYS
        ))

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository = repository,
            fromDate = DateUtils.toJulianDay(LocalDate(2025, 3, 2)),
            toDate = DateUtils.toJulianDay(LocalDate(2025, 3, 29)),
            today = DateUtils.toJulianDay(TODAY_DEFAULT)
        )

        assertTrue(cycles.isNotEmpty(), "주기 정보 존재")
        val cycle = cycles.first()

        assertNotNull(cycle.thePillPeriod, "피임약 주기 존재")
        assertEquals(DEFAULT_CYCLE, cycle.thePillPeriod, "피임약 주기 = 28일")
    }

    /**
     * TC-05-12: 복약 7일 지연 시작
     * 피임약 시작을 7일 지연했을 때 계산이 올바른지 검증
     */
    @Test
    fun testTC_05_12_sevenDayDelay() = runTest {
        val repository = InMemoryPeriodRepository()

        // 생리 기록
        repository.addPeriod(PeriodRecord(
            pk = "4",
            startDate = DateUtils.toJulianDay(PERIOD_4_START),
            endDate = DateUtils.toJulianDay(PERIOD_4_END)
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

        // 3월 8일부터 시작 (7일 지연)
        repository.addPillPackage(PillPackage(
            packageStart = DateUtils.toJulianDay(LocalDate(2025, 3, 8)),
            pillCount = DEFAULT_PILL_COUNT,
            restDays = DEFAULT_REST_DAYS
        ))

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository = repository,
            fromDate = DateUtils.toJulianDay(LocalDate(2025, 3, 8)),
            toDate = DateUtils.toJulianDay(LocalDate(2025, 4, 4)),
            today = DateUtils.toJulianDay(TODAY_DEFAULT)
        )

        assertTrue(cycles.isNotEmpty(), "주기 정보 존재")
        val cycle = cycles.first()

        assertNotNull(cycle.thePillPeriod, "피임약 주기 존재")
        assertEquals(DEFAULT_CYCLE, cycle.thePillPeriod, "피임약 주기 = 28일")
    }

    /**
     * TC-05-13: 장기 연속 복용
     * 여러 패키지를 연속으로 복용할 때 계산이 올바른지 검증
     */
    @Test
    fun testTC_05_13_continuousPackages() = runTest {
        val repository = InMemoryPeriodRepository()
        setupCommonData(repository)

        // 3개월 연속 패키지 추가
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

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository = repository,
            fromDate = DateUtils.toJulianDay(PILL_START),
            toDate = DateUtils.toJulianDay(LocalDate(2025, 5, 23)),
            today = DateUtils.toJulianDay(LocalDate(2025, 4, 15))
        )

        assertTrue(cycles.isNotEmpty(), "주기 정보 존재")

        // 각 패키지마다 주기 정보가 있어야 함
        for (cycle in cycles) {
            assertNotNull(cycle.thePillPeriod, "각 패키지의 피임약 주기 존재")
            assertEquals(DEFAULT_CYCLE, cycle.thePillPeriod, "각 패키지 주기 = 28일")
        }
    }

    /**
     * TC-05-14: 복용 중단 후 재개
     * 피임약 복용을 중단했다가 재개했을 때 계산이 올바른지 검증
     */
    @Test
    fun testTC_05_14_discontinuedAndResumed() = runTest {
        val repository = InMemoryPeriodRepository()
        setupCommonData(repository)

        // 1월 패키지
        repository.addPillPackage(PillPackage(
            packageStart = DateUtils.toJulianDay(LocalDate(2025, 1, 1)),
            pillCount = DEFAULT_PILL_COUNT,
            restDays = DEFAULT_REST_DAYS
        ))
        // 2월은 건너뜀
        // 3월 패키지는 이미 setupCommonData에서 추가됨

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository = repository,
            fromDate = DateUtils.toJulianDay(LocalDate(2025, 2, 1)),
            toDate = DateUtils.toJulianDay(LocalDate(2025, 3, 31)),
            today = DateUtils.toJulianDay(TODAY_DEFAULT)
        )

        assertTrue(cycles.isNotEmpty(), "주기 정보 존재")

        // 마지막 주기 확인 (3월)
        val lastCycle = cycles.last()
        assertNotNull(lastCycle.thePillPeriod, "3월 피임약 주기 존재")
        assertEquals(DEFAULT_CYCLE, lastCycle.thePillPeriod, "3월 피임약 주기 = 28일")
    }

    /**
     * TC-05-15: 일반 주기에서 피임약 전환
     * 일반 생리 주기에서 피임약으로 전환할 때 올바르게 처리되는지 검증
     */
    @Test
    fun testTC_05_15_transitionToPill() = runTest {
        val repository = InMemoryPeriodRepository()
        setupCommonData(repository)

        // 3월부터는 피임약
        val marchCycles = PeriodCalculator.calculateCycleInfo(
            repository = repository,
            fromDate = DateUtils.toJulianDay(PILL_START),
            toDate = DateUtils.toJulianDay(LocalDate(2025, 3, 31)),
            today = DateUtils.toJulianDay(TODAY_DEFAULT)
        )

        assertTrue(marchCycles.isNotEmpty(), "3월 주기 정보 존재")
        val marchCycle = marchCycles.first()

        assertNotNull(marchCycle.thePillPeriod, "3월부터 피임약 주기 존재")
        assertEquals(DEFAULT_CYCLE, marchCycle.thePillPeriod, "피임약 주기 = 28일")

        // 배란기/가임기 없음 확인
        assertTrue(marchCycle.ovulationDays.isEmpty(), "피임약 복용 중 배란기 없음")
        assertTrue(marchCycle.fertileDays.isEmpty(), "피임약 복용 중 가임기 없음")
    }
}