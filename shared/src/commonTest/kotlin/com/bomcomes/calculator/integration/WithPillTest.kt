package com.bomcomes.calculator.integration

import com.bomcomes.calculator.PeriodCalculator
import com.bomcomes.calculator.models.*
import com.bomcomes.calculator.repository.InMemoryPeriodRepository
import com.bomcomes.calculator.utils.DateUtils
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.*

/**
 * 피임약 복용 검증 테스트 (데이터 기반)
 *
 * 문서 참조: test-cases/docs/04-with-pill.md
 *
 * TypeScript의 TEST_CASES 배열 방식과 동일한 구조로 작성
 */
class WithPillTest {

    // ============================================
    // 데이터 클래스 정의
    // ============================================

    data class TestCase(
        val id: String,
        val name: String,
        val fromDate: LocalDate,
        val toDate: LocalDate,
        val today: LocalDate,
        val pillSettings: PillSettingsData,
        val pillPackages: List<LocalDate>,
        val expectedCycles: List<ExpectedCycle>
    )

    data class PillSettingsData(
        val isCalculatingWithPill: Boolean,
        val pillCount: Int = 21,
        val restPill: Int = 7
    )

    data class ExpectedCycle(
        val pk: String,
        val actualPeriod: DateRange?,
        val delayDays: Int,
        val delayPeriod: DateRange?,
        val predictDays: List<DateRange>,
        val fertileDays: List<DateRange>,
        val ovulationDays: List<DateRange>,
        val period: Int,
        val thePillPeriod: Int?,
        val isContinuousPillUsage: Boolean
    )

    companion object {
        // 공통 생리 기록
        val PERIOD_1_START = LocalDate(2025, 1, 1)
        val PERIOD_1_END = LocalDate(2025, 1, 5)
        val PERIOD_2_START = LocalDate(2025, 2, 1)
        val PERIOD_2_END = LocalDate(2025, 2, 5)
        val PERIOD_3_START = LocalDate(2025, 3, 1)
        val PERIOD_3_END = LocalDate(2025, 3, 5)

        // 조회 기준일
        val DEFAULT_TODAY = LocalDate(2025, 3, 15)

        // 주기 설정
        const val MANUAL_AVERAGE_CYCLE = 28
        const val MANUAL_AVERAGE_DAY = 5
        const val AUTO_AVERAGE_CYCLE = 30  // (31 + 29) / 2 = 30
        const val AUTO_AVERAGE_DAY = 5
        const val IS_AUTO_CALC = true

        // ============================================
        // 테스트 케이스 배열
        // ============================================

        val TEST_CASES = listOf(
            // TC-04-01-01: 기본 복용 (1개월)
            TestCase(
                id = "TC-04-01-01",
                name = "기본 복용 (1개월)",
                fromDate = LocalDate(2025, 3, 1),
                toDate = LocalDate(2025, 3, 30),
                today = DEFAULT_TODAY,
                pillSettings = PillSettingsData(
                    isCalculatingWithPill = true,
                    pillCount = 21,
                    restPill = 7
                ),
                pillPackages = listOf(LocalDate(2025, 3, 1)),
                expectedCycles = listOf(
                    ExpectedCycle(
                        pk = "3",
                        actualPeriod = DateRange(
                            startDate = DateUtils.toJulianDay(PERIOD_3_START),
                            endDate = DateUtils.toJulianDay(PERIOD_3_END)
                        ),
                        delayDays = 0,
                        delayPeriod = null,
                        predictDays = listOf(
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 3, 24)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 3, 28))
                            )
                        ),
                        fertileDays = emptyList(),
                        ovulationDays = emptyList(),
                        period = 28,
                        thePillPeriod = 23,
                        isContinuousPillUsage = false
                    )
                )
            ),

            // TC-04-01-02: 휴약기 0일 (연속 복용)
            TestCase(
                id = "TC-04-01-02",
                name = "휴약기 0일 (연속 복용)",
                fromDate = LocalDate(2025, 3, 1),
                toDate = LocalDate(2025, 3, 30),
                today = DEFAULT_TODAY,
                pillSettings = PillSettingsData(
                    isCalculatingWithPill = true,
                    pillCount = 21,
                    restPill = 0
                ),
                pillPackages = listOf(LocalDate(2025, 3, 1)),
                expectedCycles = listOf(
                    ExpectedCycle(
                        pk = "3",
                        actualPeriod = DateRange(
                            startDate = DateUtils.toJulianDay(PERIOD_3_START),
                            endDate = DateUtils.toJulianDay(PERIOD_3_END)
                        ),
                        delayDays = 0,
                        delayPeriod = null,
                        predictDays = emptyList(),
                        fertileDays = emptyList(),
                        ovulationDays = emptyList(),
                        period = 30,
                        thePillPeriod = null,
                        isContinuousPillUsage = true
                    )
                )
            ),

            // TC-04-01-03: 여러 패키지
            TestCase(
                id = "TC-04-01-03",
                name = "여러 패키지",
                fromDate = LocalDate(2025, 3, 1),
                toDate = LocalDate(2025, 3, 30),
                today = DEFAULT_TODAY,
                pillSettings = PillSettingsData(
                    isCalculatingWithPill = true,
                    pillCount = 21,
                    restPill = 7
                ),
                pillPackages = listOf(
                    LocalDate(2025, 1, 1),
                    LocalDate(2025, 2, 1),
                    LocalDate(2025, 3, 1)
                ),
                expectedCycles = listOf(
                    ExpectedCycle(
                        pk = "3",
                        actualPeriod = DateRange(
                            startDate = DateUtils.toJulianDay(PERIOD_3_START),
                            endDate = DateUtils.toJulianDay(PERIOD_3_END)
                        ),
                        delayDays = 0,
                        delayPeriod = null,
                        predictDays = listOf(
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 3, 24)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 3, 28))
                            )
                        ),
                        fertileDays = emptyList(),
                        ovulationDays = emptyList(),
                        period = 28,
                        thePillPeriod = 23,
                        isContinuousPillUsage = false
                    )
                )
            ),

            // TC-04-01-04: 지연 1-7일
            TestCase(
                id = "TC-04-01-04",
                name = "지연 1-7일",
                fromDate = LocalDate(2025, 3, 30),
                toDate = LocalDate(2025, 4, 4),
                today = LocalDate(2025, 3, 30),
                pillSettings = PillSettingsData(
                    isCalculatingWithPill = true,
                    pillCount = 21,
                    restPill = 7
                ),
                pillPackages = listOf(LocalDate(2025, 3, 1)),
                expectedCycles = listOf(
                    ExpectedCycle(
                        pk = "3",
                        actualPeriod = DateRange(
                            startDate = DateUtils.toJulianDay(PERIOD_3_START),
                            endDate = DateUtils.toJulianDay(PERIOD_3_END)
                        ),
                        delayDays = 7,
                        delayPeriod = DateRange(
                            startDate = DateUtils.toJulianDay(LocalDate(2025, 3, 24)),
                            endDate = DateUtils.toJulianDay(LocalDate(2025, 3, 30))
                        ),
                        predictDays = listOf(
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 3, 31)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 4, 4))
                            )
                        ),
                        fertileDays = emptyList(),
                        ovulationDays = emptyList(),
                        period = 28,
                        thePillPeriod = 23,
                        isContinuousPillUsage = false
                    )
                )
            ),

            // TC-04-01-05: 지연 8일 이상
            TestCase(
                id = "TC-04-01-05",
                name = "지연 8일 이상",
                fromDate = LocalDate(2025, 3, 30),
                toDate = LocalDate(2025, 4, 4),
                today = LocalDate(2025, 3, 31),
                pillSettings = PillSettingsData(
                    isCalculatingWithPill = true,
                    pillCount = 21,
                    restPill = 7
                ),
                pillPackages = listOf(LocalDate(2025, 3, 1)),
                expectedCycles = listOf(
                    ExpectedCycle(
                        pk = "3",
                        actualPeriod = DateRange(
                            startDate = DateUtils.toJulianDay(PERIOD_3_START),
                            endDate = DateUtils.toJulianDay(PERIOD_3_END)
                        ),
                        delayDays = 8,
                        delayPeriod = DateRange(
                            startDate = DateUtils.toJulianDay(LocalDate(2025, 3, 24)),
                            endDate = DateUtils.toJulianDay(LocalDate(2025, 3, 31))
                        ),
                        predictDays = emptyList(),
                        fertileDays = emptyList(),
                        ovulationDays = emptyList(),
                        period = 28,
                        thePillPeriod = 23,
                        isContinuousPillUsage = false
                    )
                )
            ),

            // TC-04-01-06: 5일 전 복용 (배란기 숨김)
            TestCase(
                id = "TC-04-01-06",
                name = "5일 전 복용 (배란기 숨김)",
                fromDate = LocalDate(2025, 3, 1),
                toDate = LocalDate(2025, 4, 30),
                today = LocalDate(2025, 3, 27),
                pillSettings = PillSettingsData(
                    isCalculatingWithPill = true,
                    pillCount = 21,
                    restPill = 7
                ),
                pillPackages = listOf(LocalDate(2025, 3, 26)),
                expectedCycles = listOf(
                    ExpectedCycle(
                        pk = "3",
                        actualPeriod = DateRange(
                            startDate = DateUtils.toJulianDay(PERIOD_3_START),
                            endDate = DateUtils.toJulianDay(PERIOD_3_END)
                        ),
                        delayDays = 0,
                        delayPeriod = null,
                        predictDays = listOf(
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 4, 18)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 4, 22))
                            )
                        ),
                        fertileDays = emptyList(),
                        ovulationDays = emptyList(),
                        period = 28,
                        thePillPeriod = 48,
                        isContinuousPillUsage = false
                    )
                )
            ),

            // TC-04-01-07: 5일 이후 복용 (배란기 노출)
            TestCase(
                id = "TC-04-01-07",
                name = "5일 이후 복용 (배란기 노출)",
                fromDate = LocalDate(2025, 3, 1),
                toDate = LocalDate(2025, 4, 30),
                today = LocalDate(2025, 3, 27),
                pillSettings = PillSettingsData(
                    isCalculatingWithPill = true,
                    pillCount = 21,
                    restPill = 7
                ),
                pillPackages = listOf(LocalDate(2025, 3, 27)),
                expectedCycles = listOf(
                    ExpectedCycle(
                        pk = "3",
                        actualPeriod = DateRange(
                            startDate = DateUtils.toJulianDay(PERIOD_3_START),
                            endDate = DateUtils.toJulianDay(PERIOD_3_END)
                        ),
                        delayDays = 0,
                        delayPeriod = null,
                        predictDays = listOf(
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 3, 31)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 4, 4))
                            ),
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 4, 30)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 5, 4))
                            )
                        ),
                        fertileDays = listOf(
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 3, 8)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 3, 19))
                            ),
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 4, 7)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 4, 18))
                            )
                        ),
                        ovulationDays = listOf(
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 3, 13)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 3, 15))
                            ),
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 4, 12)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 4, 14))
                            )
                        ),
                        period = 30,
                        thePillPeriod = null,
                        isContinuousPillUsage = false
                    )
                )
            ),

            // TC-04-01-08: 생리 기간 사이 - 5일 전 복용 (배란기 숨김)
            TestCase(
                id = "TC-04-01-08",
                name = "생리 기간 사이 - 5일 전 복용 (배란기 숨김)",
                fromDate = LocalDate(2025, 1, 1),
                toDate = LocalDate(2025, 1, 31),
                today = DEFAULT_TODAY,
                pillSettings = PillSettingsData(
                    isCalculatingWithPill = true,
                    pillCount = 21,
                    restPill = 7
                ),
                pillPackages = listOf(LocalDate(2025, 1, 27)),
                expectedCycles = listOf(
                    ExpectedCycle(
                        pk = "1",
                        actualPeriod = DateRange(
                            startDate = DateUtils.toJulianDay(PERIOD_1_START),
                            endDate = DateUtils.toJulianDay(PERIOD_1_END)
                        ),
                        delayDays = 0,
                        delayPeriod = null,
                        predictDays = emptyList(),
                        fertileDays = emptyList(),
                        ovulationDays = emptyList(),
                        period = 31,
                        thePillPeriod = null,
                        isContinuousPillUsage = false
                    )
                )
            ),

            // TC-04-01-09: 생리 기간 사이 - 5일 이후 복용 (배란기 노출)
            TestCase(
                id = "TC-04-01-09",
                name = "생리 기간 사이 - 5일 이후 복용 (배란기 노출)",
                fromDate = LocalDate(2025, 1, 1),
                toDate = LocalDate(2025, 1, 31),
                today = DEFAULT_TODAY,
                pillSettings = PillSettingsData(
                    isCalculatingWithPill = true,
                    pillCount = 21,
                    restPill = 7
                ),
                pillPackages = listOf(LocalDate(2025, 1, 28)),
                expectedCycles = listOf(
                    ExpectedCycle(
                        pk = "1",
                        actualPeriod = DateRange(
                            startDate = DateUtils.toJulianDay(PERIOD_1_START),
                            endDate = DateUtils.toJulianDay(PERIOD_1_END)
                        ),
                        delayDays = 0,
                        delayPeriod = null,
                        predictDays = emptyList(),
                        fertileDays = listOf(
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 1, 8)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 1, 19))
                            )
                        ),
                        ovulationDays = listOf(
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 1, 13)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 1, 15))
                            )
                        ),
                        period = 31,
                        thePillPeriod = null,
                        isContinuousPillUsage = false
                    )
                )
            ),

            // TC-04-01-10: 피임약 중단 - 마지막 생리 기록 이후
            TestCase(
                id = "TC-04-01-10",
                name = "피임약 중단 - 마지막 생리 기록 이후",
                fromDate = LocalDate(2025, 3, 1),
                toDate = LocalDate(2025, 3, 31),
                today = DEFAULT_TODAY,
                pillSettings = PillSettingsData(
                    isCalculatingWithPill = false,
                    pillCount = 21,
                    restPill = 7
                ),
                pillPackages = listOf(LocalDate(2025, 3, 1)),
                expectedCycles = listOf(
                    ExpectedCycle(
                        pk = "3",
                        actualPeriod = DateRange(
                            startDate = DateUtils.toJulianDay(PERIOD_3_START),
                            endDate = DateUtils.toJulianDay(PERIOD_3_END)
                        ),
                        delayDays = 0,
                        delayPeriod = null,
                        predictDays = listOf(
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 3, 31)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 4, 4))
                            )
                        ),
                        fertileDays = listOf(
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 3, 8)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 3, 19))
                            )
                        ),
                        ovulationDays = listOf(
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 3, 13)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 3, 15))
                            )
                        ),
                        period = 30,
                        thePillPeriod = null,
                        isContinuousPillUsage = false
                    )
                )
            ),

            // TC-04-01-11: 피임약 중단 - 생리 기록 사이
            TestCase(
                id = "TC-04-01-11",
                name = "피임약 중단 - 생리 기록 사이",
                fromDate = LocalDate(2025, 1, 1),
                toDate = LocalDate(2025, 1, 31),
                today = DEFAULT_TODAY,
                pillSettings = PillSettingsData(
                    isCalculatingWithPill = false,
                    pillCount = 21,
                    restPill = 7
                ),
                pillPackages = listOf(LocalDate(2025, 1, 1)),
                expectedCycles = listOf(
                    ExpectedCycle(
                        pk = "1",
                        actualPeriod = DateRange(
                            startDate = DateUtils.toJulianDay(PERIOD_1_START),
                            endDate = DateUtils.toJulianDay(PERIOD_1_END)
                        ),
                        delayDays = 0,
                        delayPeriod = null,
                        predictDays = emptyList(),
                        fertileDays = listOf(
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 1, 8)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 1, 19))
                            )
                        ),
                        ovulationDays = listOf(
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 1, 13)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 1, 15))
                            )
                        ),
                        period = 31,
                        thePillPeriod = null,
                        isContinuousPillUsage = false
                    )
                )
            )
        )
    }

    // ============================================
    // 공통 설정 함수
    // ============================================

    private fun setupCommonData(repository: InMemoryPeriodRepository, testCase: TestCase) {
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

        repository.setPeriodSettings(PeriodSettings(
            manualAverageCycle = MANUAL_AVERAGE_CYCLE,
            manualAverageDay = MANUAL_AVERAGE_DAY,
            autoAverageCycle = AUTO_AVERAGE_CYCLE,
            autoAverageDay = AUTO_AVERAGE_DAY,
            isAutoCalc = IS_AUTO_CALC
        ))

        repository.setPillSettings(PillSettings(
            isCalculatingWithPill = testCase.pillSettings.isCalculatingWithPill,
            pillCount = testCase.pillSettings.pillCount,
            restPill = testCase.pillSettings.restPill
        ))

        testCase.pillPackages.forEach { packageStart ->
            repository.addPillPackage(PillPackage(
                packageStart = DateUtils.toJulianDay(packageStart)
            ))
        }
    }

    // ============================================
    // 검증 헬퍼 함수
    // ============================================

    private fun assertCycleInfo(
        testCase: TestCase,
        actualCycles: List<CycleInfo>
    ) {
        assertEquals(
            testCase.expectedCycles.size,
            actualCycles.size,
            "[${testCase.id}] 주기 개수 불일치"
        )

        testCase.expectedCycles.forEachIndexed { index, expected ->
            val actual = actualCycles[index]

            assertEquals(expected.pk, actual.pk, "[${testCase.id}] Cycle $index: pk 불일치")

            if (expected.actualPeriod != null) {
                assertNotNull(actual.actualPeriod, "[${testCase.id}] Cycle $index: actualPeriod null")
                assertEquals(
                    expected.actualPeriod.startDate,
                    actual.actualPeriod!!.startDate,
                    "[${testCase.id}] Cycle $index: actualPeriod.startDate 불일치"
                )
                assertEquals(
                    expected.actualPeriod.endDate,
                    actual.actualPeriod!!.endDate,
                    "[${testCase.id}] Cycle $index: actualPeriod.endDate 불일치"
                )
            }

            assertEquals(
                expected.delayDays,
                actual.delayTheDays,
                "[${testCase.id}] Cycle $index: delayDays 불일치"
            )

            if (expected.delayPeriod != null) {
                assertNotNull(actual.delayDay, "[${testCase.id}] Cycle $index: delayPeriod null")
                assertEquals(
                    expected.delayPeriod.startDate,
                    actual.delayDay!!.startDate,
                    "[${testCase.id}] Cycle $index: delayPeriod.startDate 불일치"
                )
                assertEquals(
                    expected.delayPeriod.endDate,
                    actual.delayDay!!.endDate,
                    "[${testCase.id}] Cycle $index: delayPeriod.endDate 불일치"
                )
            } else {
                assertNull(actual.delayDay, "[${testCase.id}] Cycle $index: delayPeriod should be null")
            }

            assertEquals(
                expected.predictDays.size,
                actual.predictDays.size,
                "[${testCase.id}] Cycle $index: predictDays 개수 불일치"
            )
            expected.predictDays.forEachIndexed { i, expectedRange ->
                assertEquals(
                    expectedRange.startDate,
                    actual.predictDays[i].startDate,
                    "[${testCase.id}] Cycle $index: predictDays[$i].startDate 불일치"
                )
                assertEquals(
                    expectedRange.endDate,
                    actual.predictDays[i].endDate,
                    "[${testCase.id}] Cycle $index: predictDays[$i].endDate 불일치"
                )
            }

            assertEquals(
                expected.fertileDays.size,
                actual.fertileDays.size,
                "[${testCase.id}] Cycle $index: fertileDays 개수 불일치"
            )
            expected.fertileDays.forEachIndexed { i, expectedRange ->
                assertEquals(
                    expectedRange.startDate,
                    actual.fertileDays[i].startDate,
                    "[${testCase.id}] Cycle $index: fertileDays[$i].startDate 불일치"
                )
                assertEquals(
                    expectedRange.endDate,
                    actual.fertileDays[i].endDate,
                    "[${testCase.id}] Cycle $index: fertileDays[$i].endDate 불일치"
                )
            }

            assertEquals(
                expected.ovulationDays.size,
                actual.ovulationDays.size,
                "[${testCase.id}] Cycle $index: ovulationDays 개수 불일치"
            )
            expected.ovulationDays.forEachIndexed { i, expectedRange ->
                assertEquals(
                    expectedRange.startDate,
                    actual.ovulationDays[i].startDate,
                    "[${testCase.id}] Cycle $index: ovulationDays[$i].startDate 불일치"
                )
                assertEquals(
                    expectedRange.endDate,
                    actual.ovulationDays[i].endDate,
                    "[${testCase.id}] Cycle $index: ovulationDays[$i].endDate 불일치"
                )
            }

            assertEquals(
                expected.period,
                actual.period,
                "[${testCase.id}] Cycle $index: period 불일치"
            )

            assertEquals(
                expected.thePillPeriod,
                actual.thePillPeriod,
                "[${testCase.id}] Cycle $index: thePillPeriod 불일치"
            )

            assertEquals(
                expected.isContinuousPillUsage,
                actual.isContinuousPillUsage,
                "[${testCase.id}] Cycle $index: isContinuousPillUsage 불일치"
            )
        }
    }

    // ============================================
    // 데이터 기반 단일 테스트
    // ============================================

    @Test
    fun runAllTestCases() = runTest {
        var passedCount = 0
        var failedCount = 0
        val failures = mutableListOf<String>()

        TEST_CASES.forEach { testCase ->
            try {
                val repository = InMemoryPeriodRepository()
                setupCommonData(repository, testCase)

                val cycles = PeriodCalculator.calculateCycleInfo(
                    repository = repository,
                    fromDate = DateUtils.toJulianDay(testCase.fromDate),
                    toDate = DateUtils.toJulianDay(testCase.toDate),
                    today = DateUtils.toJulianDay(testCase.today)
                )

                assertCycleInfo(testCase, cycles)

                passedCount++
                println("✓ ${testCase.id}: ${testCase.name}")
            } catch (e: AssertionError) {
                failedCount++
                failures.add("${testCase.id}: ${e.message}")
                println("✗ ${testCase.id}: ${testCase.name} - ${e.message}")
            }
        }

        println("\n========================================")
        println("테스트 결과: $passedCount/${TEST_CASES.size} 통과")
        println("========================================")

        if (failures.isNotEmpty()) {
            println("\n실패한 테스트:")
            failures.forEach { println("  - $it") }
            fail("${failures.size}개 테스트 실패")
        }
    }
}
