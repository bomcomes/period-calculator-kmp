package com.bomcomes.calculator.integration

import com.bomcomes.calculator.PeriodCalculator
import com.bomcomes.calculator.models.*
import com.bomcomes.calculator.repository.InMemoryPeriodRepository
import com.bomcomes.calculator.utils.DateUtils
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 피임약 복용 테스트
 * - 20~35개 복용, 0~7일 휴약 가능
 * - 다양한 팩 구성과 시나리오 테스트
 *
 * 테스트 문서: test-cases/docs/05-with-pill.md
 */
class WithPillTest {

    /**
     * TC-05-01: 현재 팩 - 복용 시작
     * - 팩 구성: 21일 복용 / 7일 휴약
     * - 첫 팩 복용 시작
     */
    @Test
    fun testTC_05_01_currentPackageStart() = runTest {
        val repository = InMemoryPeriodRepository().apply {
            // 생리 기록: 12/1~12/5
            addPeriod(
                PeriodRecord(
                    pk = "1",
                    startDate = DateUtils.toJulianDay(LocalDate(2024, 12, 1)),
                    endDate = DateUtils.toJulianDay(LocalDate(2024, 12, 5))
                )
            )

            // 피임약 패키지: 12/6 시작
            addPillPackage(
                PillPackage(
                    packageStart = DateUtils.toJulianDay(LocalDate(2024, 12, 6)),
                    pillCount = 21,
                    restDays = 7
                )
            )

            // 피임약 설정
            setPillSettings(
                PillSettings(
                    isCalculatingWithPill = true,
                    pillCount = 21,
                    restPill = 7
                )
            )

            // 생리 주기 설정
            setPeriodSettings(
                PeriodSettings(
                    manualAverageCycle = 28,
                    manualAverageDay = 5,
                    isAutoCalc = false
                )
            )
        }

        val searchFrom = LocalDate(2024, 11, 1)
        val searchTo = LocalDate(2025, 1, 31)
        val today = LocalDate(2024, 12, 10)

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository,
            DateUtils.toJulianDay(searchFrom),
            DateUtils.toJulianDay(searchTo),
            DateUtils.toJulianDay(today)
        )

        // 주기 확인
        assertTrue(cycles.isNotEmpty(), "주기 정보 존재")
        val cycle = cycles.first()

        // 피임약 주기 확인
        assertNotNull(cycle.thePillPeriod, "피임약 주기 존재")
        assertEquals(28, cycle.thePillPeriod ?: 0, "피임약 주기 28일 (21+7)")

        // 휴약기 예정일 확인
        assertTrue(cycle.predictDays.isNotEmpty(), "예정일 존재")
    }

    /**
     * TC-05-02: 현재 팩 - 복용 중간
     * - 팩 구성: 21일 복용 / 7일 휴약
     * - 복용 10일째
     */
    @Test
    fun testTC_05_02_currentPackageMiddle() = runTest {
        val repository = InMemoryPeriodRepository().apply {
            // 이전 생리: 11/29~12/3
            addPeriod(
                PeriodRecord(
                    pk = "1",
                    startDate = DateUtils.toJulianDay(LocalDate(2024, 11, 29)),
                    endDate = DateUtils.toJulianDay(LocalDate(2024, 12, 3))
                )
            )

            // 피임약 패키지: 12/6 시작
            addPillPackage(
                PillPackage(
                    packageStart = DateUtils.toJulianDay(LocalDate(2024, 12, 6)),
                    pillCount = 21,
                    restDays = 7
                )
            )

            setPillSettings(
                PillSettings(
                    isCalculatingWithPill = true,
                    pillCount = 21,
                    restPill = 7
                )
            )

            setPeriodSettings(
                PeriodSettings(
                    manualAverageCycle = 28,
                    manualAverageDay = 5,
                    isAutoCalc = false
                )
            )
        }

        val today = LocalDate(2024, 12, 15) // 복용 10일째

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository,
            DateUtils.toJulianDay(LocalDate(2024, 11, 1)),
            DateUtils.toJulianDay(LocalDate(2025, 1, 31)),
            DateUtils.toJulianDay(today)
        )

        assertTrue(cycles.isNotEmpty(), "주기 정보 존재")
        val cycle = cycles.first()

        // 피임약 주기 확인
        assertNotNull(cycle.thePillPeriod, "피임약 주기 존재")
        assertEquals(28, cycle.thePillPeriod ?: 0, "피임약 주기 28일")

        // 남은 휴약일 확인
        val restPill = cycle.restPill
        assertNotNull(restPill, "남은 복용일 정보 존재")
    }

    /**
     * TC-05-03: 현재 팩 - 휴약 기간
     * - 팩 구성: 24일 복용 / 4일 휴약
     * - 휴약 2일째
     */
    @Test
    fun testTC_05_03_currentPackagePlacebo() = runTest {
        val repository = InMemoryPeriodRepository().apply {
            addPeriod(
                PeriodRecord(
                    pk = "1",
                    startDate = DateUtils.toJulianDay(LocalDate(2024, 11, 25)),
                    endDate = DateUtils.toJulianDay(LocalDate(2024, 11, 29))
                )
            )

            // 24일 복용 패키지
            addPillPackage(
                PillPackage(
                    packageStart = DateUtils.toJulianDay(LocalDate(2024, 12, 1)),
                    pillCount = 24,
                    restDays = 4
                )
            )

            // 휴약기 생리
            addPeriod(
                PeriodRecord(
                    pk = "2",
                    startDate = DateUtils.toJulianDay(LocalDate(2024, 12, 25)),
                    endDate = DateUtils.toJulianDay(LocalDate(2024, 12, 28))
                )
            )

            setPillSettings(
                PillSettings(
                    isCalculatingWithPill = true,
                    pillCount = 24,
                    restPill = 4
                )
            )

            setPeriodSettings(
                PeriodSettings(
                    manualAverageCycle = 28,
                    manualAverageDay = 5,
                    isAutoCalc = false
                )
            )
        }

        val today = LocalDate(2024, 12, 26) // 휴약 2일째

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository,
            DateUtils.toJulianDay(LocalDate(2024, 11, 1)),
            DateUtils.toJulianDay(LocalDate(2025, 1, 31)),
            DateUtils.toJulianDay(today)
        )

        assertTrue(cycles.isNotEmpty(), "주기 정보 존재")
        val cycle = cycles.first()

        // 피임약 주기 확인
        assertNotNull(cycle.thePillPeriod, "피임약 주기 존재")
        assertEquals(28, cycle.thePillPeriod ?: 0, "피임약 주기 28일 (24+4)")
    }

    /**
     * TC-05-04: 과거 팩 - 완료된 팩
     * - 팩 구성: 21일 복용 / 7일 휴약
     * - 2개월 전 완료된 팩
     */
    @Test
    fun testTC_05_04_pastPackageCompleted() = runTest {
        val repository = InMemoryPeriodRepository().apply {
            // 10월 생리
            addPeriod(
                PeriodRecord(
                    pk = "1",
                    startDate = DateUtils.toJulianDay(LocalDate(2024, 10, 1)),
                    endDate = DateUtils.toJulianDay(LocalDate(2024, 10, 5))
                )
            )

            // 10월 피임약 패키지
            addPillPackage(
                PillPackage(
                    packageStart = DateUtils.toJulianDay(LocalDate(2024, 10, 6)),
                    pillCount = 21,
                    restDays = 7
                )
            )

            // 10월 휴약기 생리
            addPeriod(
                PeriodRecord(
                    pk = "2",
                    startDate = DateUtils.toJulianDay(LocalDate(2024, 10, 29)),
                    endDate = DateUtils.toJulianDay(LocalDate(2024, 11, 2))
                )
            )

            setPillSettings(
                PillSettings(
                    isCalculatingWithPill = true,
                    pillCount = 21,
                    restPill = 7
                )
            )

            setPeriodSettings(
                PeriodSettings(
                    manualAverageCycle = 28,
                    manualAverageDay = 5,
                    isAutoCalc = false
                )
            )
        }

        val today = LocalDate(2024, 12, 15)

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository,
            DateUtils.toJulianDay(LocalDate(2024, 9, 1)),
            DateUtils.toJulianDay(LocalDate(2024, 12, 31)),
            DateUtils.toJulianDay(today)
        )

        // 10월 피임약 주기 확인
        assertTrue(cycles.isNotEmpty(), "주기 정보 존재")
        val cycle = cycles.first()
        assertNotNull(cycle.thePillPeriod, "피임약 주기 존재")
    }

    /**
     * TC-05-05: 과거 팩 - 중단된 팩
     * - 팩 구성: 21일 복용 / 7일 휴약
     * - 복용 중간에 중단
     */
    @Test
    fun testTC_05_05_pastPackageInterrupted() = runTest {
        val repository = InMemoryPeriodRepository().apply {
            addPeriod(
                PeriodRecord(
                    pk = "1",
                    startDate = DateUtils.toJulianDay(LocalDate(2024, 11, 1)),
                    endDate = DateUtils.toJulianDay(LocalDate(2024, 11, 5))
                )
            )

            // 15일만 복용 (11/6~11/20)
            addPillPackage(
                PillPackage(
                    packageStart = DateUtils.toJulianDay(LocalDate(2024, 11, 6)),
                    pillCount = 15, // 중간 중단
                    restDays = 0
                )
            )

            // 조기 생리
            addPeriod(
                PeriodRecord(
                    pk = "2",
                    startDate = DateUtils.toJulianDay(LocalDate(2024, 11, 25)),
                    endDate = DateUtils.toJulianDay(LocalDate(2024, 11, 29))
                )
            )

            setPillSettings(
                PillSettings(
                    isCalculatingWithPill = true,
                    pillCount = 21,
                    restPill = 7
                )
            )

            setPeriodSettings(
                PeriodSettings(
                    manualAverageCycle = 28,
                    manualAverageDay = 5,
                    isAutoCalc = false
                )
            )
        }

        val today = LocalDate(2024, 12, 15)

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository,
            DateUtils.toJulianDay(LocalDate(2024, 10, 1)),
            DateUtils.toJulianDay(LocalDate(2024, 12, 31)),
            DateUtils.toJulianDay(today)
        )

        // 실제 생리 기록 확인 (조기 생리)
        assertTrue(cycles.isNotEmpty(), "주기 정보 존재")
        val cycle = cycles.first()
        assertTrue(cycle.actualPeriod != null || cycle.predictDays.isNotEmpty(), "생리 기록 존재")
    }

    /**
     * TC-05-06: 과거 팩 - 지연 복용
     * - 팩 구성: 21일 복용 / 2일 휴약
     * - 다음 팩 시작 지연
     */
    @Test
    fun testTC_05_06_pastPackageDelayed() = runTest {
        val repository = InMemoryPeriodRepository().apply {
            addPeriod(
                PeriodRecord(
                    pk = "1",
                    startDate = DateUtils.toJulianDay(LocalDate(2024, 10, 1)),
                    endDate = DateUtils.toJulianDay(LocalDate(2024, 10, 5))
                )
            )

            // 첫 팩
            addPillPackage(
                PillPackage(
                    packageStart = DateUtils.toJulianDay(LocalDate(2024, 10, 6)),
                    pillCount = 21,
                    restDays = 2
                )
            )

            // 휴약기 생리
            addPeriod(
                PeriodRecord(
                    pk = "2",
                    startDate = DateUtils.toJulianDay(LocalDate(2024, 10, 27)),
                    endDate = DateUtils.toJulianDay(LocalDate(2024, 10, 30))
                )
            )

            // 다음 팩 (5일 지연)
            addPillPackage(
                PillPackage(
                    packageStart = DateUtils.toJulianDay(LocalDate(2024, 11, 3)), // 10/29 예정 → 11/3 시작
                    pillCount = 21,
                    restDays = 2
                )
            )

            setPillSettings(
                PillSettings(
                    isCalculatingWithPill = true,
                    pillCount = 21,
                    restPill = 2
                )
            )

            setPeriodSettings(
                PeriodSettings(
                    manualAverageCycle = 28,
                    manualAverageDay = 5,
                    isAutoCalc = false
                )
            )
        }

        val today = LocalDate(2024, 12, 1)

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository,
            DateUtils.toJulianDay(LocalDate(2024, 9, 1)),
            DateUtils.toJulianDay(LocalDate(2024, 12, 31)),
            DateUtils.toJulianDay(today)
        )

        assertTrue(cycles.isNotEmpty(), "주기 정보 존재")
        val cycle = cycles.first()

        // 피임약 주기 확인
        assertNotNull(cycle.thePillPeriod, "피임약 주기 존재")
    }

    /**
     * TC-05-07: 장기 복용 - 연속 6개월
     * - 팩 구성: 28일 복용 / 0일 휴약 (연속 복용)
     * - 6개월 연속 복용
     */
    @Test
    fun testTC_05_07_longTermContinuous() = runTest {
        val repository = InMemoryPeriodRepository().apply {
            addPeriod(
                PeriodRecord(
                    pk = "1",
                    startDate = DateUtils.toJulianDay(LocalDate(2024, 6, 1)),
                    endDate = DateUtils.toJulianDay(LocalDate(2024, 6, 5))
                )
            )

            // 6개월 연속 복용 패키지들
            for (month in 6..11) {
                val startDay = if (month == 6) 6 else 1
                addPillPackage(
                    PillPackage(
                        packageStart = DateUtils.toJulianDay(LocalDate(2024, month, startDay)),
                        pillCount = 28,
                        restDays = 0
                    )
                )
            }

            setPillSettings(
                PillSettings(
                    isCalculatingWithPill = true,
                    pillCount = 28,
                    restPill = 0
                )
            )

            setPeriodSettings(
                PeriodSettings(
                    manualAverageCycle = 28,
                    manualAverageDay = 5,
                    isAutoCalc = false
                )
            )
        }

        val today = LocalDate(2024, 12, 1)

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository,
            DateUtils.toJulianDay(LocalDate(2024, 5, 1)),
            DateUtils.toJulianDay(LocalDate(2024, 12, 31)),
            DateUtils.toJulianDay(today)
        )

        // 연속 복용 확인 (휴약 없음)
        assertTrue(cycles.isNotEmpty(), "주기 정보 존재")
        val cycle = cycles.first()
        assertNotNull(cycle.thePillPeriod, "피임약 주기 존재")
        assertEquals(28, cycle.thePillPeriod ?: 0, "피임약 주기 28일 (연속 복용)")
    }

    /**
     * TC-05-08: 장기 복용 - 1년 이상
     * - 팩 구성: 21일 복용 / 7일 휴약
     * - 13개월 복용
     */
    @Test
    fun testTC_05_08_longTermOneYear() = runTest {
        val repository = InMemoryPeriodRepository().apply {
            addPeriod(
                PeriodRecord(
                    pk = "1",
                    startDate = DateUtils.toJulianDay(LocalDate(2023, 11, 1)),
                    endDate = DateUtils.toJulianDay(LocalDate(2023, 11, 5))
                )
            )

            // 13개월 복용 기록 (간소화)
            addPillPackage(
                PillPackage(
                    packageStart = DateUtils.toJulianDay(LocalDate(2023, 11, 6)),
                    pillCount = 21,
                    restDays = 7
                )
            )

            setPillSettings(
                PillSettings(
                    isCalculatingWithPill = true,
                    pillCount = 21,
                    restPill = 7
                )
            )

            setPeriodSettings(
                PeriodSettings(
                    manualAverageCycle = 28,
                    manualAverageDay = 5,
                    isAutoCalc = false
                )
            )
        }

        val today = LocalDate(2024, 12, 15)

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository,
            DateUtils.toJulianDay(LocalDate(2023, 10, 1)),
            DateUtils.toJulianDay(LocalDate(2025, 1, 31)),
            DateUtils.toJulianDay(today)
        )

        assertTrue(cycles.isNotEmpty(), "주기 정보 존재")
        val cycle = cycles.first()

        // 피임약 주기 확인
        assertNotNull(cycle.thePillPeriod, "피임약 주기 존재")
        assertEquals(28, cycle.thePillPeriod ?: 0, "피임약 주기 28일")
    }

    /**
     * TC-05-09: 경계 케이스 - 최소 복용일 (20일)
     * - 팩 구성: 20일 복용 / 7일 휴약
     * - 최소 복용일 테스트
     */
    @Test
    fun testTC_05_09_boundaryMinPills() = runTest {
        val repository = InMemoryPeriodRepository().apply {
            addPeriod(
                PeriodRecord(
                    pk = "1",
                    startDate = DateUtils.toJulianDay(LocalDate(2024, 12, 1)),
                    endDate = DateUtils.toJulianDay(LocalDate(2024, 12, 5))
                )
            )

            // 20일 복용 패키지
            addPillPackage(
                PillPackage(
                    packageStart = DateUtils.toJulianDay(LocalDate(2024, 12, 6)),
                    pillCount = 20,
                    restDays = 7
                )
            )

            setPillSettings(
                PillSettings(
                    isCalculatingWithPill = true,
                    pillCount = 20,
                    restPill = 7
                )
            )

            setPeriodSettings(
                PeriodSettings(
                    manualAverageCycle = 28,
                    manualAverageDay = 5,
                    isAutoCalc = false
                )
            )
        }

        val today = LocalDate(2024, 12, 20)

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository,
            DateUtils.toJulianDay(LocalDate(2024, 11, 1)),
            DateUtils.toJulianDay(LocalDate(2025, 1, 31)),
            DateUtils.toJulianDay(today)
        )

        assertTrue(cycles.isNotEmpty(), "주기 정보 존재")
        val cycle = cycles.first()

        // 피임약 주기 확인
        assertNotNull(cycle.thePillPeriod, "피임약 주기 존재")
        assertEquals(27, cycle.thePillPeriod ?: 0, "피임약 주기 27일 (20+7)")
    }

    /**
     * TC-05-10: 경계 케이스 - 최대 복용일 (35일)
     * - 팩 구성: 35일 복용 / 7일 휴약
     * - 최대 복용일 테스트
     */
    @Test
    fun testTC_05_10_boundaryMaxPills() = runTest {
        val repository = InMemoryPeriodRepository().apply {
            addPeriod(
                PeriodRecord(
                    pk = "1",
                    startDate = DateUtils.toJulianDay(LocalDate(2024, 11, 25)),
                    endDate = DateUtils.toJulianDay(LocalDate(2024, 11, 29))
                )
            )

            // 35일 복용 패키지
            addPillPackage(
                PillPackage(
                    packageStart = DateUtils.toJulianDay(LocalDate(2024, 12, 1)),
                    pillCount = 35,
                    restDays = 7
                )
            )

            setPillSettings(
                PillSettings(
                    isCalculatingWithPill = true,
                    pillCount = 35,
                    restPill = 7
                )
            )

            setPeriodSettings(
                PeriodSettings(
                    manualAverageCycle = 28,
                    manualAverageDay = 5,
                    isAutoCalc = false
                )
            )
        }

        val today = LocalDate(2024, 12, 25)

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository,
            DateUtils.toJulianDay(LocalDate(2024, 11, 1)),
            DateUtils.toJulianDay(LocalDate(2025, 2, 28)),
            DateUtils.toJulianDay(today)
        )

        assertTrue(cycles.isNotEmpty(), "주기 정보 존재")
        val cycle = cycles.first()

        // 피임약 주기 확인
        assertNotNull(cycle.thePillPeriod, "피임약 주기 존재")
        assertEquals(42, cycle.thePillPeriod ?: 0, "피임약 주기 42일 (35+7)")
    }

    /**
     * TC-05-11: 경계 케이스 - 휴약 없음 (0일)
     * - 팩 구성: 28일 복용 / 0일 휴약
     * - 연속 복용 테스트
     */
    @Test
    fun testTC_05_11_boundaryNoPlacebo() = runTest {
        val repository = InMemoryPeriodRepository().apply {
            addPeriod(
                PeriodRecord(
                    pk = "1",
                    startDate = DateUtils.toJulianDay(LocalDate(2024, 11, 28)),
                    endDate = DateUtils.toJulianDay(LocalDate(2024, 12, 2))
                )
            )

            // 첫 팩
            addPillPackage(
                PillPackage(
                    packageStart = DateUtils.toJulianDay(LocalDate(2024, 12, 3)),
                    pillCount = 28,
                    restDays = 0
                )
            )

            // 둘째 팩 (연속)
            addPillPackage(
                PillPackage(
                    packageStart = DateUtils.toJulianDay(LocalDate(2024, 12, 31)),
                    pillCount = 28,
                    restDays = 0
                )
            )

            setPillSettings(
                PillSettings(
                    isCalculatingWithPill = true,
                    pillCount = 28,
                    restPill = 0
                )
            )

            setPeriodSettings(
                PeriodSettings(
                    manualAverageCycle = 28,
                    manualAverageDay = 5,
                    isAutoCalc = false
                )
            )
        }

        val today = LocalDate(2025, 1, 15)

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository,
            DateUtils.toJulianDay(LocalDate(2024, 11, 1)),
            DateUtils.toJulianDay(LocalDate(2025, 2, 28)),
            DateUtils.toJulianDay(today)
        )

        // 연속 복용 확인
        assertTrue(cycles.isNotEmpty(), "주기 정보 존재")
        val cycle = cycles.first()
        assertNotNull(cycle.thePillPeriod, "피임약 주기 존재")
        assertEquals(28, cycle.thePillPeriod ?: 0, "피임약 주기 28일 (연속)")
    }

    /**
     * TC-05-12: 생리 유형 - 휴약기 정상 생리
     * - 팩 구성: 21일 복용 / 7일 휴약
     * - 휴약 3일째 시작, 5일간
     */
    @Test
    fun testTC_05_12_periodTypeNormalPlacebo() = runTest {
        val repository = InMemoryPeriodRepository().apply {
            addPeriod(
                PeriodRecord(
                    pk = "1",
                    startDate = DateUtils.toJulianDay(LocalDate(2024, 12, 1)),
                    endDate = DateUtils.toJulianDay(LocalDate(2024, 12, 5))
                )
            )

            // 21일 복용 패키지
            addPillPackage(
                PillPackage(
                    packageStart = DateUtils.toJulianDay(LocalDate(2024, 12, 6)),
                    pillCount = 21,
                    restDays = 7
                )
            )

            // 휴약기 생리
            addPeriod(
                PeriodRecord(
                    pk = "2",
                    startDate = DateUtils.toJulianDay(LocalDate(2024, 12, 29)),
                    endDate = DateUtils.toJulianDay(LocalDate(2025, 1, 2))
                )
            )

            setPillSettings(
                PillSettings(
                    isCalculatingWithPill = true,
                    pillCount = 21,
                    restPill = 7
                )
            )

            setPeriodSettings(
                PeriodSettings(
                    manualAverageCycle = 28,
                    manualAverageDay = 5,
                    isAutoCalc = false
                )
            )
        }

        val today = LocalDate(2025, 1, 10)

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository,
            DateUtils.toJulianDay(LocalDate(2024, 11, 1)),
            DateUtils.toJulianDay(LocalDate(2025, 2, 28)),
            DateUtils.toJulianDay(today)
        )

        // 휴약기 생리 확인
        assertTrue(cycles.isNotEmpty(), "주기 정보 존재")
        val cycle = cycles.first()
        assertNotNull(cycle.actualPeriod, "휴약기 생리 존재")
    }

    /**
     * TC-05-13: 생리 유형 - 돌발 출혈 (복용 중)
     * - 팩 구성: 21일 복용 / 7일 휴약
     * - 복용 중 돌발 출혈
     */
    @Test
    fun testTC_05_13_periodTypeBreakthrough() = runTest {
        val repository = InMemoryPeriodRepository().apply {
            addPeriod(
                PeriodRecord(
                    pk = "1",
                    startDate = DateUtils.toJulianDay(LocalDate(2024, 12, 1)),
                    endDate = DateUtils.toJulianDay(LocalDate(2024, 12, 5))
                )
            )

            // 21일 복용 패키지
            addPillPackage(
                PillPackage(
                    packageStart = DateUtils.toJulianDay(LocalDate(2024, 12, 6)),
                    pillCount = 21,
                    restDays = 7
                )
            )

            // 돌발 출혈 (복용 중)
            addPeriod(
                PeriodRecord(
                    pk = "2",
                    startDate = DateUtils.toJulianDay(LocalDate(2024, 12, 15)),
                    endDate = DateUtils.toJulianDay(LocalDate(2024, 12, 17))
                )
            )

            setPillSettings(
                PillSettings(
                    isCalculatingWithPill = true,
                    pillCount = 21,
                    restPill = 7
                )
            )

            setPeriodSettings(
                PeriodSettings(
                    manualAverageCycle = 28,
                    manualAverageDay = 5,
                    isAutoCalc = false
                )
            )
        }

        val today = LocalDate(2024, 12, 20)

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository,
            DateUtils.toJulianDay(LocalDate(2024, 11, 1)),
            DateUtils.toJulianDay(LocalDate(2025, 1, 31)),
            DateUtils.toJulianDay(today)
        )

        // 돌발 출혈 확인
        assertTrue(cycles.isNotEmpty(), "주기 정보 존재")
        val cycle = cycles.first()
        assertNotNull(cycle.actualPeriod, "돌발 출혈 기록")
    }

    /**
     * TC-05-14: 생리 유형 - 지연/무월경
     * - 팩 구성: 21일 복용 / 7일 휴약
     * - 휴약기에 생리 없음
     */
    @Test
    fun testTC_05_14_periodTypeAmenorrhea() = runTest {
        val repository = InMemoryPeriodRepository().apply {
            addPeriod(
                PeriodRecord(
                    pk = "1",
                    startDate = DateUtils.toJulianDay(LocalDate(2024, 11, 25)),
                    endDate = DateUtils.toJulianDay(LocalDate(2024, 11, 29))
                )
            )

            // 21일 복용 패키지
            addPillPackage(
                PillPackage(
                    packageStart = DateUtils.toJulianDay(LocalDate(2024, 12, 1)),
                    pillCount = 21,
                    restDays = 7
                )
            )

            // 휴약기 (12/22~12/28)에 생리 없음

            // 다음 팩
            addPillPackage(
                PillPackage(
                    packageStart = DateUtils.toJulianDay(LocalDate(2024, 12, 29)),
                    pillCount = 21,
                    restDays = 7
                )
            )

            setPillSettings(
                PillSettings(
                    isCalculatingWithPill = true,
                    pillCount = 21,
                    restPill = 7
                )
            )

            setPeriodSettings(
                PeriodSettings(
                    manualAverageCycle = 28,
                    manualAverageDay = 5,
                    isAutoCalc = false
                )
            )
        }

        val today = LocalDate(2025, 1, 10)

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository,
            DateUtils.toJulianDay(LocalDate(2024, 11, 1)),
            DateUtils.toJulianDay(LocalDate(2025, 2, 28)),
            DateUtils.toJulianDay(today)
        )

        // 피임약 주기는 존재
        assertTrue(cycles.isNotEmpty(), "주기 정보 존재")
        val cycle = cycles.first()
        assertNotNull(cycle.thePillPeriod, "피임약 주기 존재")
    }

    /**
     * TC-05-15: 팩 지연 - 다음 팩 1일 지연
     * - 팩 구성: 21일 복용 / 7일 휴약
     * - 다음 팩 1일 늦게 시작
     */
    @Test
    fun testTC_05_15_delayOneDayStart() = runTest {
        val repository = InMemoryPeriodRepository().apply {
            addPeriod(
                PeriodRecord(
                    pk = "1",
                    startDate = DateUtils.toJulianDay(LocalDate(2024, 12, 1)),
                    endDate = DateUtils.toJulianDay(LocalDate(2024, 12, 5))
                )
            )

            // 첫 팩
            addPillPackage(
                PillPackage(
                    packageStart = DateUtils.toJulianDay(LocalDate(2024, 12, 6)),
                    pillCount = 21,
                    restDays = 7
                )
            )

            // 휴약기 생리
            addPeriod(
                PeriodRecord(
                    pk = "2",
                    startDate = DateUtils.toJulianDay(LocalDate(2024, 12, 29)),
                    endDate = DateUtils.toJulianDay(LocalDate(2025, 1, 2))
                )
            )

            // 다음 팩 (1일 지연)
            addPillPackage(
                PillPackage(
                    packageStart = DateUtils.toJulianDay(LocalDate(2025, 1, 4)), // 1/3 예정 → 1/4 시작
                    pillCount = 21,
                    restDays = 7
                )
            )

            setPillSettings(
                PillSettings(
                    isCalculatingWithPill = true,
                    pillCount = 21,
                    restPill = 7
                )
            )

            setPeriodSettings(
                PeriodSettings(
                    manualAverageCycle = 28,
                    manualAverageDay = 5,
                    isAutoCalc = false
                )
            )
        }

        val today = LocalDate(2025, 1, 15)

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository,
            DateUtils.toJulianDay(LocalDate(2024, 11, 1)),
            DateUtils.toJulianDay(LocalDate(2025, 2, 28)),
            DateUtils.toJulianDay(today)
        )

        // 지연 후에도 주기 계산
        assertTrue(cycles.isNotEmpty(), "주기 정보 존재")
        val cycle = cycles.first()
        assertNotNull(cycle.thePillPeriod, "피임약 주기 존재")
    }

    /**
     * TC-05-16: 팩 지연 - 다음 팩 7일 지연
     * - 팩 구성: 21일 복용 / 7일 휴약
     * - 다음 팩 일주일 늦게 시작
     */
    @Test
    fun testTC_05_16_delaySevenDayStart() = runTest {
        val repository = InMemoryPeriodRepository().apply {
            addPeriod(
                PeriodRecord(
                    pk = "1",
                    startDate = DateUtils.toJulianDay(LocalDate(2024, 12, 1)),
                    endDate = DateUtils.toJulianDay(LocalDate(2024, 12, 5))
                )
            )

            // 첫 팩
            addPillPackage(
                PillPackage(
                    packageStart = DateUtils.toJulianDay(LocalDate(2024, 12, 6)),
                    pillCount = 21,
                    restDays = 7
                )
            )

            // 휴약기 생리
            addPeriod(
                PeriodRecord(
                    pk = "2",
                    startDate = DateUtils.toJulianDay(LocalDate(2024, 12, 29)),
                    endDate = DateUtils.toJulianDay(LocalDate(2025, 1, 2))
                )
            )

            // 다음 팩 (7일 지연)
            addPillPackage(
                PillPackage(
                    packageStart = DateUtils.toJulianDay(LocalDate(2025, 1, 10)), // 1/3 예정 → 1/10 시작
                    pillCount = 21,
                    restDays = 7
                )
            )

            setPillSettings(
                PillSettings(
                    isCalculatingWithPill = true,
                    pillCount = 21,
                    restPill = 7
                )
            )

            setPeriodSettings(
                PeriodSettings(
                    manualAverageCycle = 28,
                    manualAverageDay = 5,
                    isAutoCalc = false
                )
            )
        }

        val today = LocalDate(2025, 1, 20)

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository,
            DateUtils.toJulianDay(LocalDate(2024, 11, 1)),
            DateUtils.toJulianDay(LocalDate(2025, 2, 28)),
            DateUtils.toJulianDay(today)
        )

        // 지연 후 주기 확인
        assertTrue(cycles.isNotEmpty(), "주기 정보 존재")
        val cycle = cycles.first()
        assertNotNull(cycle.thePillPeriod, "피임약 주기 존재")
    }

    /**
     * TC-05-17: 팩 지연 - 복용 중 누락
     * - 팩 구성: 21일 복용 / 7일 휴약
     * - 복용 중간에 3일 누락
     */
    @Test
    fun testTC_05_17_delayMissedMiddle() = runTest {
        val repository = InMemoryPeriodRepository().apply {
            addPeriod(
                PeriodRecord(
                    pk = "1",
                    startDate = DateUtils.toJulianDay(LocalDate(2024, 12, 1)),
                    endDate = DateUtils.toJulianDay(LocalDate(2024, 12, 5))
                )
            )

            // 첫 부분 복용 (10일)
            addPillPackage(
                PillPackage(
                    packageStart = DateUtils.toJulianDay(LocalDate(2024, 12, 6)),
                    pillCount = 10,
                    restDays = 0
                )
            )

            // 3일 누락 후 나머지 복용 (11일)
            addPillPackage(
                PillPackage(
                    packageStart = DateUtils.toJulianDay(LocalDate(2024, 12, 19)),
                    pillCount = 11,
                    restDays = 7
                )
            )

            setPillSettings(
                PillSettings(
                    isCalculatingWithPill = true,
                    pillCount = 21,
                    restPill = 7
                )
            )

            setPeriodSettings(
                PeriodSettings(
                    manualAverageCycle = 28,
                    manualAverageDay = 5,
                    isAutoCalc = false
                )
            )
        }

        val today = LocalDate(2024, 12, 25)

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository,
            DateUtils.toJulianDay(LocalDate(2024, 11, 1)),
            DateUtils.toJulianDay(LocalDate(2025, 1, 31)),
            DateUtils.toJulianDay(today)
        )

        // 누락이 있어도 주기 계산
        assertTrue(cycles.isNotEmpty(), "주기 정보 존재")
        val cycle = cycles.first()
        assertNotNull(cycle.thePillPeriod, "피임약 주기 존재")
    }

    /**
     * TC-05-18: 다양한 구성 - 24/4 구성
     * - 팩 구성: 24일 복용 / 4일 휴약
     * - 표준과 다른 구성 테스트
     */
    @Test
    fun testTC_05_18_config24_4() = runTest {
        val repository = InMemoryPeriodRepository().apply {
            addPeriod(
                PeriodRecord(
                    pk = "1",
                    startDate = DateUtils.toJulianDay(LocalDate(2024, 12, 1)),
                    endDate = DateUtils.toJulianDay(LocalDate(2024, 12, 5))
                )
            )

            // 24/4 구성 패키지
            addPillPackage(
                PillPackage(
                    packageStart = DateUtils.toJulianDay(LocalDate(2024, 12, 6)),
                    pillCount = 24,
                    restDays = 4
                )
            )

            setPillSettings(
                PillSettings(
                    isCalculatingWithPill = true,
                    pillCount = 24,
                    restPill = 4
                )
            )

            setPeriodSettings(
                PeriodSettings(
                    manualAverageCycle = 28,
                    manualAverageDay = 5,
                    isAutoCalc = false
                )
            )
        }

        val today = LocalDate(2024, 12, 25)

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository,
            DateUtils.toJulianDay(LocalDate(2024, 11, 1)),
            DateUtils.toJulianDay(LocalDate(2025, 1, 31)),
            DateUtils.toJulianDay(today)
        )

        // 24/4 구성 주기 확인
        assertTrue(cycles.isNotEmpty(), "주기 정보 존재")
        val cycle = cycles.first()
        assertNotNull(cycle.thePillPeriod, "피임약 주기 존재")
        assertEquals(28, cycle.thePillPeriod ?: 0, "피임약 주기 28일 (24+4)")
    }

    /**
     * TC-05-19: 다양한 구성 - 21/2 구성
     * - 팩 구성: 21일 복용 / 2일 휴약
     * - 짧은 휴약기 테스트
     */
    @Test
    fun testTC_05_19_config21_2() = runTest {
        val repository = InMemoryPeriodRepository().apply {
            addPeriod(
                PeriodRecord(
                    pk = "1",
                    startDate = DateUtils.toJulianDay(LocalDate(2024, 12, 1)),
                    endDate = DateUtils.toJulianDay(LocalDate(2024, 12, 5))
                )
            )

            // 21/2 구성 패키지
            addPillPackage(
                PillPackage(
                    packageStart = DateUtils.toJulianDay(LocalDate(2024, 12, 6)),
                    pillCount = 21,
                    restDays = 2
                )
            )

            // 짧은 휴약기 생리
            addPeriod(
                PeriodRecord(
                    pk = "2",
                    startDate = DateUtils.toJulianDay(LocalDate(2024, 12, 28)),
                    endDate = DateUtils.toJulianDay(LocalDate(2024, 12, 29))
                )
            )

            setPillSettings(
                PillSettings(
                    isCalculatingWithPill = true,
                    pillCount = 21,
                    restPill = 2
                )
            )

            setPeriodSettings(
                PeriodSettings(
                    manualAverageCycle = 28,
                    manualAverageDay = 5,
                    isAutoCalc = false
                )
            )
        }

        val today = LocalDate(2025, 1, 5)

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository,
            DateUtils.toJulianDay(LocalDate(2024, 11, 1)),
            DateUtils.toJulianDay(LocalDate(2025, 1, 31)),
            DateUtils.toJulianDay(today)
        )

        // 21/2 구성 주기 확인
        assertTrue(cycles.isNotEmpty(), "주기 정보 존재")
        val cycle = cycles.first()
        assertNotNull(cycle.thePillPeriod, "피임약 주기 존재")
        assertEquals(23, cycle.thePillPeriod ?: 0, "피임약 주기 23일 (21+2)")
    }

    /**
     * TC-05-20: 전환 시나리오 - 일반 → 피임약
     * - 일반 주기에서 피임약 복용 시작
     * - 주기 변화 추적
     */
    @Test
    fun testTC_05_20_transitionNormalToPill() = runTest {
        val repository = InMemoryPeriodRepository().apply {
            // 일반 생리 기록들
            addPeriod(
                PeriodRecord(
                    pk = "1",
                    startDate = DateUtils.toJulianDay(LocalDate(2024, 10, 1)),
                    endDate = DateUtils.toJulianDay(LocalDate(2024, 10, 5))
                )
            )

            addPeriod(
                PeriodRecord(
                    pk = "2",
                    startDate = DateUtils.toJulianDay(LocalDate(2024, 10, 29)),
                    endDate = DateUtils.toJulianDay(LocalDate(2024, 11, 2))
                )
            )

            addPeriod(
                PeriodRecord(
                    pk = "3",
                    startDate = DateUtils.toJulianDay(LocalDate(2024, 11, 26)),
                    endDate = DateUtils.toJulianDay(LocalDate(2024, 11, 30))
                )
            )

            // 피임약 시작
            addPillPackage(
                PillPackage(
                    packageStart = DateUtils.toJulianDay(LocalDate(2024, 12, 1)),
                    pillCount = 21,
                    restDays = 7
                )
            )

            setPillSettings(
                PillSettings(
                    isCalculatingWithPill = true,
                    pillCount = 21,
                    restPill = 7
                )
            )

            setPeriodSettings(
                PeriodSettings(
                    manualAverageCycle = 28,
                    manualAverageDay = 5,
                    isAutoCalc = false
                )
            )
        }

        val today = LocalDate(2024, 12, 25)

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository,
            DateUtils.toJulianDay(LocalDate(2024, 9, 1)),
            DateUtils.toJulianDay(LocalDate(2025, 1, 31)),
            DateUtils.toJulianDay(today)
        )

        // 피임약 주기로 전환 확인
        assertTrue(cycles.isNotEmpty(), "주기 정보 존재")
        val cycle = cycles.first()
        assertNotNull(cycle.thePillPeriod, "피임약 주기 존재")
        assertEquals(28, cycle.thePillPeriod ?: 0, "피임약으로 규칙적 주기")
    }
}