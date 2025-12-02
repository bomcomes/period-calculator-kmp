package com.bomcomes.calculator.integration

import com.bomcomes.calculator.PeriodCalculator
import com.bomcomes.calculator.models.*
import com.bomcomes.calculator.repository.InMemoryPeriodRepository
import com.bomcomes.calculator.utils.DateUtils
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.*

/**
 * 크나우스 오기노법 검증 테스트 - 긴 주기 (데이터 기반)
 *
 * 문서 참조: test-cases/docs/03-long-cycle-over-33.md
 *
 * TypeScript의 TEST_CASES 배열 방식과 동일한 구조로 작성
 */
class LongCycleOver33Test {

    // ============================================
    // 데이터 클래스 정의
    // ============================================

    data class TestCase(
        val id: String,
        val name: String,
        val fromDate: LocalDate,
        val toDate: LocalDate,
        val today: LocalDate,
        val expectedCycles: List<ExpectedCycle>
    )

    data class ExpectedCycle(
        val pk: String,
        val actualPeriod: DateRange?,
        val delayDays: Int,
        val delayPeriod: DateRange?,
        val predictDays: List<DateRange>,
        val fertileDays: List<DateRange>,
        val ovulationDays: List<DateRange>,
        val period: Int
    )

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

        // ============================================
        // 테스트 케이스 배열
        // ============================================

        val TEST_CASES = listOf(
            // TC-03-01: 1일 조회 (마지막 생리 이후)
            TestCase(
                id = "TC-03-01",
                name = "1일 조회 (마지막 생리 이후)",
                fromDate = LocalDate(2025, 4, 1),
                toDate = LocalDate(2025, 4, 1),
                today = DEFAULT_TODAY,
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
                        period = 37
                    )
                )
            ),

            // TC-03-02: 1주일 조회 (마지막 생리 이후)
            TestCase(
                id = "TC-03-02",
                name = "1주일 조회 (마지막 생리 이후)",
                fromDate = LocalDate(2025, 3, 30),
                toDate = LocalDate(2025, 4, 5),
                today = DEFAULT_TODAY,
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
                        fertileDays = listOf(
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 4, 3)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 4, 11))
                            )
                        ),
                        ovulationDays = emptyList(),
                        period = 37
                    )
                )
            ),

            // TC-03-03: 1개월 조회 (마지막 생리 이후)
            TestCase(
                id = "TC-03-03",
                name = "1개월 조회 (마지막 생리 이후)",
                fromDate = LocalDate(2025, 3, 16),
                toDate = LocalDate(2025, 4, 22),
                today = DEFAULT_TODAY,
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
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 4, 22)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 4, 26))
                            )
                        ),
                        fertileDays = listOf(
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 4, 3)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 4, 11))
                            )
                        ),
                        ovulationDays = listOf(
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 4, 6)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 4, 8))
                            )
                        ),
                        period = 37
                    )
                )
            ),

            // TC-03-04: 1일 조회 (과거)
            TestCase(
                id = "TC-03-04",
                name = "1일 조회 (과거)",
                fromDate = LocalDate(2025, 2, 9),
                toDate = LocalDate(2025, 2, 9),
                today = DEFAULT_TODAY,
                expectedCycles = listOf(
                    ExpectedCycle(
                        pk = "2",
                        actualPeriod = DateRange(
                            startDate = DateUtils.toJulianDay(PERIOD_2_START),
                            endDate = DateUtils.toJulianDay(PERIOD_2_END)
                        ),
                        delayDays = 0,
                        delayPeriod = null,
                        predictDays = emptyList(),
                        fertileDays = emptyList(),
                        ovulationDays = emptyList(),
                        period = 35
                    )
                )
            ),

            // TC-03-05: 1주일 조회 (과거)
            TestCase(
                id = "TC-03-05",
                name = "1주일 조회 (과거)",
                fromDate = LocalDate(2025, 2, 16),
                toDate = LocalDate(2025, 2, 22),
                today = DEFAULT_TODAY,
                expectedCycles = listOf(
                    ExpectedCycle(
                        pk = "2",
                        actualPeriod = DateRange(
                            startDate = DateUtils.toJulianDay(PERIOD_2_START),
                            endDate = DateUtils.toJulianDay(PERIOD_2_END)
                        ),
                        delayDays = 0,
                        delayPeriod = null,
                        predictDays = emptyList(),
                        fertileDays = emptyList(),
                        ovulationDays = emptyList(),
                        period = 35
                    )
                )
            ),

            // TC-03-06: 1개월 조회 (과거)
            TestCase(
                id = "TC-03-06",
                name = "1개월 조회 (과거)",
                fromDate = LocalDate(2025, 1, 1),
                toDate = LocalDate(2025, 1, 31),
                today = DEFAULT_TODAY,
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
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 1, 21)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 1, 29))
                            )
                        ),
                        ovulationDays = listOf(
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 1, 24)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 1, 26))
                            )
                        ),
                        period = 39
                    )
                )
            ),

            // TC-03-07: 3개월 조회
            TestCase(
                id = "TC-03-07",
                name = "3개월 조회",
                fromDate = LocalDate(2025, 3, 16),
                toDate = LocalDate(2025, 6, 15),
                today = DEFAULT_TODAY,
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
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 4, 22)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 4, 26))
                            ),
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 5, 29)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 6, 2))
                            )
                        ),
                        fertileDays = listOf(
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 4, 3)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 4, 11))
                            ),
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 5, 10)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 5, 18))
                            )
                        ),
                        ovulationDays = listOf(
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 4, 6)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 4, 8))
                            ),
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 5, 13)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 5, 15))
                            )
                        ),
                        period = 37
                    )
                )
            ),

            // TC-03-08: 생리 기간 경계 조회 (전체 5개월)
            TestCase(
                id = "TC-03-08",
                name = "생리 기간 경계 조회 (전체 5개월)",
                fromDate = LocalDate(2025, 1, 1),
                toDate = LocalDate(2025, 5, 31),
                today = DEFAULT_TODAY,
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
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 1, 21)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 1, 29))
                            )
                        ),
                        ovulationDays = listOf(
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 1, 24)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 1, 26))
                            )
                        ),
                        period = 39
                    ),
                    ExpectedCycle(
                        pk = "2",
                        actualPeriod = DateRange(
                            startDate = DateUtils.toJulianDay(PERIOD_2_START),
                            endDate = DateUtils.toJulianDay(PERIOD_2_END)
                        ),
                        delayDays = 0,
                        delayPeriod = null,
                        predictDays = emptyList(),
                        fertileDays = listOf(
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 2, 25)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 3, 5))
                            )
                        ),
                        ovulationDays = listOf(
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 2, 28)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 3, 2))
                            )
                        ),
                        period = 35
                    ),
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
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 4, 22)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 4, 26))
                            ),
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 5, 29)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 6, 2))
                            )
                        ),
                        fertileDays = listOf(
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 4, 3)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 4, 11))
                            ),
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 5, 10)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 5, 18))
                            )
                        ),
                        ovulationDays = listOf(
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 4, 6)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 4, 8))
                            ),
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 5, 13)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 5, 15))
                            )
                        ),
                        period = 37
                    )
                )
            ),

            // TC-03-09: 생리 기간 경계 조회 (과거만)
            TestCase(
                id = "TC-03-09",
                name = "생리 기간 경계 조회 (과거만)",
                fromDate = LocalDate(2025, 1, 1),
                toDate = LocalDate(2025, 2, 28),
                today = DEFAULT_TODAY,
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
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 1, 21)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 1, 29))
                            )
                        ),
                        ovulationDays = listOf(
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 1, 24)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 1, 26))
                            )
                        ),
                        period = 39
                    ),
                    ExpectedCycle(
                        pk = "2",
                        actualPeriod = DateRange(
                            startDate = DateUtils.toJulianDay(PERIOD_2_START),
                            endDate = DateUtils.toJulianDay(PERIOD_2_END)
                        ),
                        delayDays = 0,
                        delayPeriod = null,
                        predictDays = emptyList(),
                        fertileDays = listOf(
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 2, 25)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 3, 5))
                            )
                        ),
                        ovulationDays = listOf(
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 2, 28)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 3, 2))
                            )
                        ),
                        period = 35
                    )
                )
            ),

            // TC-03-10: 생리 지연 1-7일
            TestCase(
                id = "TC-03-10",
                name = "생리 지연 1-7일 (예정일 뒤로 미룸)",
                fromDate = LocalDate(2025, 4, 27),
                toDate = LocalDate(2025, 5, 3),
                today = LocalDate(2025, 4, 28),
                expectedCycles = listOf(
                    ExpectedCycle(
                        pk = "3",
                        actualPeriod = DateRange(
                            startDate = DateUtils.toJulianDay(PERIOD_3_START),
                            endDate = DateUtils.toJulianDay(PERIOD_3_END)
                        ),
                        delayDays = 7,
                        delayPeriod = DateRange(
                            startDate = DateUtils.toJulianDay(LocalDate(2025, 4, 22)),
                            endDate = DateUtils.toJulianDay(LocalDate(2025, 4, 28))
                        ),
                        predictDays = listOf(
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 4, 29)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 5, 3))
                            )
                        ),
                        fertileDays = emptyList(),
                        ovulationDays = emptyList(),
                        period = 37
                    )
                )
            ),

            // TC-03-11: 생리 지연 8일 이상
            TestCase(
                id = "TC-03-11",
                name = "생리 지연 8일 이상 (병원 권장)",
                fromDate = LocalDate(2025, 3, 20),
                toDate = LocalDate(2025, 5, 10),
                today = LocalDate(2025, 4, 29),
                expectedCycles = listOf(
                    ExpectedCycle(
                        pk = "3",
                        actualPeriod = DateRange(
                            startDate = DateUtils.toJulianDay(PERIOD_3_START),
                            endDate = DateUtils.toJulianDay(PERIOD_3_END)
                        ),
                        delayDays = 8,
                        delayPeriod = DateRange(
                            startDate = DateUtils.toJulianDay(LocalDate(2025, 4, 22)),
                            endDate = DateUtils.toJulianDay(LocalDate(2025, 4, 29))
                        ),
                        predictDays = emptyList(),
                        fertileDays = listOf(
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 4, 3)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 4, 11))
                            )
                        ),
                        ovulationDays = listOf(
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 4, 6)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 4, 8))
                            )
                        ),
                        period = 37
                    )
                )
            )
        )
    }

    // ============================================
    // 공통 설정 함수
    // ============================================

    private fun setupCommonData(repository: InMemoryPeriodRepository) {
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
            isCalculatingWithPill = false,
            pillCount = 21,
            restPill = 7
        ))
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
                setupCommonData(repository)

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
