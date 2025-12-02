package com.bomcomes.calculator.integration

import com.bomcomes.calculator.PeriodCalculator
import com.bomcomes.calculator.models.*
import com.bomcomes.calculator.repository.InMemoryPeriodRepository
import com.bomcomes.calculator.utils.DateUtils
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.*

/**
 * 수동 배란일 입력 검증 테스트 (데이터 기반)
 *
 * 문서 참조: test-cases/docs/06-with-manual-ovulation.md
 *
 * 사용자가 배란일을 직접 입력했을 때의 계산 로직 검증
 * (배란 테스트 기록이 아닌 userOvulationDays 사용)
 */
class WithManualOvulationTest {

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
        val period: Int,
        val isOvulationPeriodUserInput: Boolean,
        val ovulationDayPeriod: Int?,
        val thePillPeriod: Int?
    )

    companion object {
        // 공통 생리 기록
        val PERIOD_1_START = LocalDate(2025, 1, 1)
        val PERIOD_1_END = LocalDate(2025, 1, 5)
        val PERIOD_2_START = LocalDate(2025, 2, 1)
        val PERIOD_2_END = LocalDate(2025, 2, 5)
        val PERIOD_3_START = LocalDate(2025, 3, 1)
        val PERIOD_3_END = LocalDate(2025, 3, 5)

        // 수동 배란일 입력 (userOvulationDays)
        val MANUAL_OVULATION_1 = LocalDate(2025, 1, 15)
        val MANUAL_OVULATION_2 = LocalDate(2025, 2, 11)
        val MANUAL_OVULATION_3 = LocalDate(2025, 2, 12)
        val MANUAL_OVULATION_4 = LocalDate(2025, 3, 18)

        // 조회 기준일
        val DEFAULT_TODAY = LocalDate(2025, 3, 18)

        // 주기 설정
        const val MANUAL_AVERAGE_CYCLE = 28
        const val MANUAL_AVERAGE_DAY = 5
        const val AUTO_AVERAGE_CYCLE = 30  // (31 + 28) / 2 ≈ 30
        const val AUTO_AVERAGE_DAY = 5
        const val IS_AUTO_CALC = true

        // ============================================
        // 테스트 케이스 배열
        // ============================================

        val TEST_CASES = listOf(
            // TC-06-01: 1일 조회 (마지막 생리 이후)
            TestCase(
                id = "TC-06-01",
                name = "1일 조회 (마지막 생리 이후)",
                fromDate = LocalDate(2025, 3, 18),
                toDate = LocalDate(2025, 3, 18),
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
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 3, 16)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 3, 19))
                            )
                        ),
                        ovulationDays = listOf(
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 3, 18)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 3, 18))
                            )
                        ),
                        period = 30,
                        isOvulationPeriodUserInput = true,
                        ovulationDayPeriod = 31,
                        thePillPeriod = null
                    )
                )
            ),

            // TC-06-02: 1주일 조회 (마지막 생리 이후)
            TestCase(
                id = "TC-06-02",
                name = "1주일 조회 (마지막 생리 이후)",
                fromDate = LocalDate(2025, 3, 16),
                toDate = LocalDate(2025, 3, 22),
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
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 3, 16)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 3, 19))
                            )
                        ),
                        ovulationDays = listOf(
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 3, 18)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 3, 18))
                            )
                        ),
                        period = 30,
                        isOvulationPeriodUserInput = true,
                        ovulationDayPeriod = 31,
                        thePillPeriod = null
                    )
                )
            ),

            // TC-06-03: 1개월 조회 (마지막 생리 이후)
            TestCase(
                id = "TC-06-03",
                name = "1개월 조회 (마지막 생리 이후)",
                fromDate = LocalDate(2025, 3, 1),
                toDate = LocalDate(2025, 3, 31),
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
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 3, 16)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 3, 19))
                            )
                        ),
                        ovulationDays = listOf(
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 3, 18)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 3, 18))
                            )
                        ),
                        period = 30,
                        isOvulationPeriodUserInput = true,
                        ovulationDayPeriod = 31,
                        thePillPeriod = null
                    )
                )
            ),

            // TC-06-04: 1일 조회 (과거)
            TestCase(
                id = "TC-06-04",
                name = "1일 조회 (과거)",
                fromDate = LocalDate(2025, 2, 7),
                toDate = LocalDate(2025, 2, 7),
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
                        period = 28,
                        isOvulationPeriodUserInput = true,
                        ovulationDayPeriod = null,
                        thePillPeriod = null
                    )
                )
            ),

            // TC-06-05: 1주일 조회 (과거)
            TestCase(
                id = "TC-06-05",
                name = "1주일 조회 (과거)",
                fromDate = LocalDate(2025, 2, 9),
                toDate = LocalDate(2025, 2, 15),
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
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 2, 9)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 2, 13))
                            )
                        ),
                        ovulationDays = listOf(
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 2, 11)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 2, 12))
                            )
                        ),
                        period = 28,
                        isOvulationPeriodUserInput = true,
                        ovulationDayPeriod = null,
                        thePillPeriod = null
                    )
                )
            ),

            // TC-06-06: 1개월 조회 (과거)
            TestCase(
                id = "TC-06-06",
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
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 1, 13)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 1, 16))
                            )
                        ),
                        ovulationDays = listOf(
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 1, 15)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 1, 15))
                            )
                        ),
                        period = 31,
                        isOvulationPeriodUserInput = true,
                        ovulationDayPeriod = null,
                        thePillPeriod = null
                    )
                )
            ),

            // TC-06-07: 3개월 조회
            TestCase(
                id = "TC-06-07",
                name = "3개월 조회",
                fromDate = LocalDate(2025, 3, 1),
                toDate = LocalDate(2025, 5, 31),
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
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 4, 1)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 4, 5))
                            ),
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 5, 1)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 5, 5))
                            ),
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 5, 31)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 6, 4))
                            )
                        ),
                        fertileDays = listOf(
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 3, 16)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 3, 19))
                            ),
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 4, 8)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 4, 19))
                            ),
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 5, 8)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 5, 19))
                            )
                        ),
                        ovulationDays = listOf(
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 3, 18)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 3, 18))
                            ),
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 4, 13)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 4, 15))
                            ),
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 5, 13)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 5, 15))
                            )
                        ),
                        period = 30,
                        isOvulationPeriodUserInput = true,
                        ovulationDayPeriod = 31,
                        thePillPeriod = null
                    )
                )
            ),

            // TC-06-08: 생리 기간 경계 조회 (전체)
            TestCase(
                id = "TC-06-08",
                name = "생리 기간 경계 조회 (전체)",
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
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 1, 13)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 1, 16))
                            )
                        ),
                        ovulationDays = listOf(
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 1, 15)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 1, 15))
                            )
                        ),
                        period = 31,
                        isOvulationPeriodUserInput = true,
                        ovulationDayPeriod = null,
                        thePillPeriod = null
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
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 2, 9)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 2, 13))
                            )
                        ),
                        ovulationDays = listOf(
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 2, 11)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 2, 12))
                            )
                        ),
                        period = 28,
                        isOvulationPeriodUserInput = true,
                        ovulationDayPeriod = null,
                        thePillPeriod = null
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
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 4, 1)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 4, 5))
                            ),
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 5, 1)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 5, 5))
                            ),
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 5, 31)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 6, 4))
                            )
                        ),
                        fertileDays = listOf(
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 3, 16)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 3, 19))
                            ),
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 4, 8)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 4, 19))
                            ),
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 5, 8)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 5, 19))
                            )
                        ),
                        ovulationDays = listOf(
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 3, 18)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 3, 18))
                            ),
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 4, 13)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 4, 15))
                            ),
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 5, 13)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 5, 15))
                            )
                        ),
                        period = 30,
                        isOvulationPeriodUserInput = true,
                        ovulationDayPeriod = 31,
                        thePillPeriod = null
                    )
                )
            ),

            // TC-06-09: 생리 기간 경계 조회 (과거만)
            TestCase(
                id = "TC-06-09",
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
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 1, 13)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 1, 16))
                            )
                        ),
                        ovulationDays = listOf(
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 1, 15)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 1, 15))
                            )
                        ),
                        period = 31,
                        isOvulationPeriodUserInput = true,
                        ovulationDayPeriod = null,
                        thePillPeriod = null
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
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 2, 9)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 2, 13))
                            )
                        ),
                        ovulationDays = listOf(
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 2, 11)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 2, 12))
                            )
                        ),
                        period = 28,
                        isOvulationPeriodUserInput = true,
                        ovulationDayPeriod = null,
                        thePillPeriod = null
                    )
                )
            ),

            // TC-06-10: 생리 지연 1-7일
            TestCase(
                id = "TC-06-10",
                name = "생리 지연 1-7일",
                fromDate = LocalDate(2025, 3, 20),
                toDate = LocalDate(2025, 4, 10),
                today = LocalDate(2025, 4, 7),
                expectedCycles = listOf(
                    ExpectedCycle(
                        pk = "3",
                        actualPeriod = DateRange(
                            startDate = DateUtils.toJulianDay(PERIOD_3_START),
                            endDate = DateUtils.toJulianDay(PERIOD_3_END)
                        ),
                        delayDays = 7,
                        delayPeriod = DateRange(
                            startDate = DateUtils.toJulianDay(LocalDate(2025, 4, 1)),
                            endDate = DateUtils.toJulianDay(LocalDate(2025, 4, 7))
                        ),
                        predictDays = listOf(
                            DateRange(
                                startDate = DateUtils.toJulianDay(LocalDate(2025, 4, 8)),
                                endDate = DateUtils.toJulianDay(LocalDate(2025, 4, 12))
                            )
                        ),
                        fertileDays = emptyList(),
                        ovulationDays = emptyList(),
                        period = 30,
                        isOvulationPeriodUserInput = true,
                        ovulationDayPeriod = 31,
                        thePillPeriod = null
                    )
                )
            ),

            // TC-06-11: 생리 지연 8일 이상
            TestCase(
                id = "TC-06-11",
                name = "생리 지연 8일 이상",
                fromDate = LocalDate(2025, 3, 20),
                toDate = LocalDate(2025, 5, 10),
                today = LocalDate(2025, 4, 8),
                expectedCycles = listOf(
                    ExpectedCycle(
                        pk = "3",
                        actualPeriod = DateRange(
                            startDate = DateUtils.toJulianDay(PERIOD_3_START),
                            endDate = DateUtils.toJulianDay(PERIOD_3_END)
                        ),
                        delayDays = 8,
                        delayPeriod = DateRange(
                            startDate = DateUtils.toJulianDay(LocalDate(2025, 4, 1)),
                            endDate = DateUtils.toJulianDay(LocalDate(2025, 4, 8))
                        ),
                        predictDays = emptyList(),
                        fertileDays = emptyList(),
                        ovulationDays = emptyList(),
                        period = 30,
                        isOvulationPeriodUserInput = true,
                        ovulationDayPeriod = 31,
                        thePillPeriod = null
                    )
                )
            )
        )
    }

    // ============================================
    // 공통 설정 함수
    // ============================================

    private fun setupCommonData(repository: InMemoryPeriodRepository) {
        // 생리 기록 추가
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

        // 주기 설정
        repository.setPeriodSettings(PeriodSettings(
            manualAverageCycle = MANUAL_AVERAGE_CYCLE,
            manualAverageDay = MANUAL_AVERAGE_DAY,
            autoAverageCycle = AUTO_AVERAGE_CYCLE,
            autoAverageDay = AUTO_AVERAGE_DAY,
            isAutoCalc = IS_AUTO_CALC
        ))

        // 수동 배란일 입력 (userOvulationDays)
        repository.addUserOvulationDay(OvulationDay(
            date = DateUtils.toJulianDay(MANUAL_OVULATION_1)
        ))
        repository.addUserOvulationDay(OvulationDay(
            date = DateUtils.toJulianDay(MANUAL_OVULATION_2)
        ))
        repository.addUserOvulationDay(OvulationDay(
            date = DateUtils.toJulianDay(MANUAL_OVULATION_3)
        ))
        repository.addUserOvulationDay(OvulationDay(
            date = DateUtils.toJulianDay(MANUAL_OVULATION_4)
        ))

        // 피임약 비활성화
        repository.setPillSettings(PillSettings(
            isCalculatingWithPill = false
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

            assertEquals(
                expected.isOvulationPeriodUserInput,
                actual.isOvulationPeriodUserInput,
                "[${testCase.id}] Cycle $index: isOvulationPeriodUserInput 불일치"
            )

            assertEquals(
                expected.ovulationDayPeriod,
                actual.ovulationDayPeriod,
                "[${testCase.id}] Cycle $index: ovulationDayPeriod 불일치"
            )

            assertEquals(
                expected.thePillPeriod,
                actual.thePillPeriod,
                "[${testCase.id}] Cycle $index: thePillPeriod 불일치"
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
