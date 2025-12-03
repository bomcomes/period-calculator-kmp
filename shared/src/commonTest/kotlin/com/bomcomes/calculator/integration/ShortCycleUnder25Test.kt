package com.bomcomes.calculator.integration

import com.bomcomes.calculator.PeriodCalculator
import com.bomcomes.calculator.integration.common.ExpectedCycle
import com.bomcomes.calculator.integration.common.TestCase
import com.bomcomes.calculator.models.*
import com.bomcomes.calculator.repository.InMemoryPeriodRepository
import com.bomcomes.calculator.utils.DateUtils
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.*

/**
 * 크나우스 오기노법 검증 테스트 - 짧은 주기 (데이터 기반)
 *
 * 문서 참조: test-cases/docs/02-short-cycle-under-25.md
 *
 * TC ID 형식: TC-02-GG-CC (02: 파일번호, GG: 그룹번호, CC: 케이스번호)
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

        // ============================================
        // 테스트 케이스 배열
        // ============================================

        val TEST_CASES = listOf(
            // 그룹 1: 마지막 생리 이후 조회 (Period 3 기준)
            TestCase(
                id = "TC-02-01-01",
                name = "1일 조회 (마지막 생리 이후)",
                fromDate = LocalDate(2025, 2, 20),
                toDate = LocalDate(2025, 2, 20),
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
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 2, 13)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 2, 21))
                            )
                        ),
                        ovulationDays = emptyList(),
                        period = 22
                    )
                )
            ),

            TestCase(
                id = "TC-02-01-02",
                name = "1주일 조회 (마지막 생리 이후)",
                fromDate = LocalDate(2025, 2, 16),
                toDate = LocalDate(2025, 2, 22),
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
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 2, 13)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 2, 21))
                            )
                        ),
                        ovulationDays = listOf(
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 2, 16)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 2, 18))
                            )
                        ),
                        period = 22
                    )
                )
            ),

            TestCase(
                id = "TC-02-01-03",
                name = "1개월 조회 (마지막 생리 이후)",
                fromDate = LocalDate(2025, 2, 10),
                toDate = LocalDate(2025, 3, 9),
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
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 3, 4)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 3, 8))
                            )
                        ),
                        fertileDays = listOf(
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 2, 13)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 2, 21))
                            ),
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 3, 7)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 3, 15))
                            )
                        ),
                        ovulationDays = listOf(
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 2, 16)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 2, 18))
                            )
                        ),
                        period = 22
                    )
                )
            ),

            // 그룹 2: 과거 기록 중심 (Period 2)
            TestCase(
                id = "TC-02-02-01",
                name = "1일 조회 (과거)",
                fromDate = LocalDate(2025, 1, 23),
                toDate = LocalDate(2025, 1, 23),
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
                        fertileDays = listOf(
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 1, 23)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 1, 30))
                            )
                        ),
                        ovulationDays = emptyList(),
                        period = 18
                    )
                )
            ),

            TestCase(
                id = "TC-02-02-02",
                name = "1주일 조회 (과거) - 음수 방어 로직 검증",
                fromDate = LocalDate(2025, 1, 26),
                toDate = LocalDate(2025, 2, 1),
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
                        fertileDays = listOf(
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 1, 23)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 1, 30))
                            )
                        ),
                        ovulationDays = listOf(
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 1, 25)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 1, 27))
                            )
                        ),
                        period = 18
                    )
                )
            ),

            TestCase(
                id = "TC-02-02-03",
                name = "1개월 조회 (과거)",
                fromDate = LocalDate(2025, 1, 1),
                toDate = LocalDate(2025, 1, 22),
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
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 1, 4)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 1, 12))
                            )
                        ),
                        ovulationDays = listOf(
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 1, 7)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 1, 9))
                            )
                        ),
                        period = 22
                    )
                )
            ),

            // 그룹 3: 장기 조회 & 특수 구간
            TestCase(
                id = "TC-02-03-01",
                name = "3개월 조회",
                fromDate = LocalDate(2025, 2, 10),
                toDate = LocalDate(2025, 5, 9),
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
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 3, 4)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 3, 8))
                            ),
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 3, 26)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 3, 30))
                            ),
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 4, 17)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 4, 21))
                            ),
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 5, 9)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 5, 13))
                            )
                        ),
                        fertileDays = listOf(
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 2, 13)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 2, 21))
                            ),
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 3, 7)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 3, 15))
                            ),
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 3, 29)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 4, 6))
                            ),
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 4, 20)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 4, 28))
                            )
                        ),
                        ovulationDays = listOf(
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 2, 16)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 2, 18))
                            ),
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 3, 10)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 3, 12))
                            ),
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 4, 1)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 4, 3))
                            ),
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 4, 23)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 4, 25))
                            )
                        ),
                        period = 22
                    )
                )
            ),

            TestCase(
                id = "TC-02-03-02",
                name = "생리 기간 경계 조회 (3개 주기)",
                fromDate = LocalDate(2025, 1, 7),
                toDate = LocalDate(2025, 4, 6),
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
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 1, 4)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 1, 12))
                            )
                        ),
                        ovulationDays = listOf(
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 1, 7)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 1, 9))
                            )
                        ),
                        period = 22
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
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 1, 23)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 1, 30))
                            )
                        ),
                        ovulationDays = listOf(
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 1, 25)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 1, 27))
                            )
                        ),
                        period = 18
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
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 3, 4)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 3, 8))
                            ),
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 3, 26)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 3, 30))
                            )
                        ),
                        fertileDays = listOf(
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 2, 13)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 2, 21))
                            ),
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 3, 7)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 3, 15))
                            ),
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 3, 29)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 4, 6))
                            )
                        ),
                        ovulationDays = listOf(
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 2, 16)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 2, 18))
                            ),
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 3, 10)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 3, 12))
                            ),
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 4, 1)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 4, 3))
                            )
                        ),
                        period = 22
                    )
                )
            ),

            TestCase(
                id = "TC-02-03-03",
                name = "생리 기간 경계 조회 (2개 주기)",
                fromDate = LocalDate(2025, 1, 1),
                toDate = LocalDate(2025, 2, 9),
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
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 1, 4)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 1, 12))
                            )
                        ),
                        ovulationDays = listOf(
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 1, 7)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 1, 9))
                            )
                        ),
                        period = 22
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
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 1, 23)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 1, 30))
                            )
                        ),
                        ovulationDays = listOf(
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 1, 25)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 1, 27))
                            )
                        ),
                        period = 18
                    )
                )
            ),

            // 그룹 4: 생리 지연 케이스
            TestCase(
                id = "TC-02-04-01",
                name = "생리 지연 1-7일 (예정일 뒤로 미룸)",
                fromDate = LocalDate(2025, 3, 9),
                toDate = LocalDate(2025, 3, 15),
                today = LocalDate(2025, 3, 10),
                expectedCycles = listOf(
                    ExpectedCycle(
                        pk = "3",
                        actualPeriod = DateRange(
                            startDate = DateUtils.toJulianDay(PERIOD_3_START),
                            endDate = DateUtils.toJulianDay(PERIOD_3_END)
                        ),
                        delayDays = 7,
                        delayPeriod = DateRange(
                            startDate = DateUtils.toJulianDay(LocalDate(2025, 3, 4)),
                            endDate = DateUtils.toJulianDay(LocalDate(2025, 3, 10))
                        ),
                        predictDays = listOf(
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 3, 11)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 3, 15))
                            )
                        ),
                        fertileDays = listOf(
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 3, 14)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 3, 22))
                            )
                        ),
                        ovulationDays = emptyList(),
                        period = 22
                    )
                )
            ),

            TestCase(
                id = "TC-02-04-02",
                name = "생리 지연 8일 이상 (병원 권장)",
                fromDate = LocalDate(2025, 3, 9),
                toDate = LocalDate(2025, 3, 15),
                today = LocalDate(2025, 3, 11),
                expectedCycles = listOf(
                    ExpectedCycle(
                        pk = "3",
                        actualPeriod = DateRange(
                            startDate = DateUtils.toJulianDay(PERIOD_3_START),
                            endDate = DateUtils.toJulianDay(PERIOD_3_END)
                        ),
                        delayDays = 8,
                        delayPeriod = DateRange(
                            startDate = DateUtils.toJulianDay(LocalDate(2025, 3, 4)),
                            endDate = DateUtils.toJulianDay(LocalDate(2025, 3, 11))
                        ),
                        predictDays = emptyList(),
                        fertileDays = emptyList(),
                        ovulationDays = emptyList(),
                        period = 22
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

        repository.setPillSettings(PillSettings(isCalculatingWithPill = false))
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
