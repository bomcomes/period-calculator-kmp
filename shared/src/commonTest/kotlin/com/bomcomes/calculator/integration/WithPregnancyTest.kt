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
 * 임신 모드 검증 테스트 (데이터 기반)
 *
 * 문서 참조: test-cases/docs/07-with-pregnancy.md
 *
 * TC ID 형식: TC-07-GG-CC (07: 파일번호, GG: 그룹번호, CC: 케이스번호)
 *
 * 테스트 그룹:
 * - 그룹 1: 임신 모드 (출산 예정일 기준, startsDate = dueDate - 280일)
 * - 그룹 2: 임신 모드 (마지막 생리일 기준, startsDate = lastTheDayDate)
 * - 그룹 3: 출산 후 복귀 (주기 1개 기록)
 * - 그룹 4: 출산 후 복귀 (주기 2개 기록)
 */
class WithPregnancyTest {

    companion object {
        // 공통 생리 기록 (그룹 1, 2)
        val PERIOD_1_START = LocalDate(2025, 1, 1)
        val PERIOD_1_END = LocalDate(2025, 1, 5)
        val PERIOD_2_START = LocalDate(2025, 2, 1)
        val PERIOD_2_END = LocalDate(2025, 2, 5)

        // 출산 후 생리 기록 (그룹 3)
        val PERIOD_3_START = LocalDate(2025, 12, 1)
        val PERIOD_3_END = LocalDate(2025, 12, 5)

        // 출산 후 두 번째 생리 기록 (그룹 4)
        val PERIOD_4_START = LocalDate(2026, 1, 5)
        val PERIOD_4_END = LocalDate(2026, 1, 9)

        // 주기 설정
        const val MANUAL_AVERAGE_CYCLE = 28
        const val MANUAL_AVERAGE_DAY = 5
        const val AUTO_AVERAGE_CYCLE_GROUP1_2 = 28  // 임신 중 기본값 28일
        const val AUTO_AVERAGE_CYCLE_GROUP3 = 28     // 출산 후 무조건 28일
        const val AUTO_AVERAGE_CYCLE_GROUP4 = 35     // 출산 후 기록 기준
        const val AUTO_AVERAGE_DAY = 5
        const val IS_AUTO_CALC = true

        // 임신 정보 (그룹 1: 출산 예정일 기준)
        val PREGNANCY_DUE_DATE = LocalDate(2025, 11, 25)
        val PREGNANCY_START_DATE_GROUP1 = LocalDate(2025, 2, 18)  // 출산 예정일 - 280일

        // 임신 정보 (그룹 2: 마지막 생리일 기준)
        val LAST_PERIOD_START_GROUP2 = LocalDate(2025, 2, 1)
        val PREGNANCY_START_DATE_GROUP2 = LocalDate(2025, 2, 1)   // 마지막 생리 시작일
        val PREGNANCY_DUE_DATE_GROUP2 = LocalDate(2025, 11, 8)    // 마지막 생리 시작일 + 280일
    }

    // ============================================
    // 그룹 1: 임신 모드 (출산 예정일 기준)
    // ============================================

    /**
     * TC-07-01-01: 임신 중 생리 예측 비활성화
     *
     * 임신 모드 활성화 시 생리 예정일, 가임기, 배란기가 모두 비활성화되는지 검증
     * 조회 범위(3월)에 생리 기록이 없지만, 임신 중이므로 마지막 생리 반환 (pregnancyStartDate 전달용)
     */
    @Test
    fun testTC_07_01_01_PregnancyModeDisablesPrediction() = runTest {
        val testCase = TestCase(
            id = "TC-07-01-01",
            name = "임신 중 생리 예측 비활성화",
            fromDate = LocalDate(2025, 3, 1),
            toDate = LocalDate(2025, 3, 31),
            today = LocalDate(2025, 3, 15),
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
                    // 가임기/배란기는 조회 범위(3월) 밖이므로 빈 배열
                    fertileDays = emptyList(),
                    ovulationDays = emptyList(),
                    period = 28,
                    isOvulationUserInput = false,
                    ovulationBasedPeriod = null,
                    pillBasedPeriod = null,
                    remainingPlaceboDay = null,
                    pregnancyStartDate = DateUtils.toJulianDay(PREGNANCY_START_DATE_GROUP1)
                )
            )
        )

        val repository = setupGroup1Data()
        runTestCase(testCase, repository)
    }

    @Test
    fun testTC_07_01_02_FertileDaysBeforePregnancy() = runTest {
        val testCase = TestCase(
            id = "TC-07-01-02",
            name = "임신 시작 전 가임기는 유지",
            fromDate = LocalDate(2025, 2, 1),
            toDate = LocalDate(2025, 2, 28),
            today = LocalDate(2025, 3, 15),
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
                    // KMP 로직 (31일 주기, 26~32 범위): 가임기 = start+7 ~ start+18 = 2/8 ~ 2/19
                    // filterByPregnancy 적용 (임신시작일 2/18 전날까지): 2/8 ~ 2/17
                    fertileDays = listOf(
                        DateRange(
                            startDate = DateUtils.toJulianDay(LocalDate(2025, 2, 8)),
                            endDate = DateUtils.toJulianDay(LocalDate(2025, 2, 17))
                        )
                    ),
                    // 배란기 (31일 주기, 26~32 범위): start+12 ~ start+14 = 2/13 ~ 2/15
                    // filterByPregnancy: 2/15 < 임신시작일(2/18) 전날(2/17) → 그대로
                    ovulationDays = listOf(
                        DateRange(
                            startDate = DateUtils.toJulianDay(LocalDate(2025, 2, 13)),
                            endDate = DateUtils.toJulianDay(LocalDate(2025, 2, 15))
                        )
                    ),
                    // 문서 기준: 임신 중 기본값 28일
                    period = 28,
                    isOvulationUserInput = false,
                    ovulationBasedPeriod = null,
                    pillBasedPeriod = null,
                    remainingPlaceboDay = null,
                    pregnancyStartDate = DateUtils.toJulianDay(PREGNANCY_START_DATE_GROUP1)
                )
            )
        )

        val repository = setupGroup1Data()
        runTestCase(testCase, repository)
    }

    // ============================================
    // 그룹 2: 임신 모드 (마지막 생리일 기준)
    // ============================================

    @Test
    fun testTC_07_02_01_LastPeriodBasedPregnancy() = runTest {
        val testCase = TestCase(
            id = "TC-07-02-01",
            name = "마지막 생리일 기준 임신 - 생리 예측 비활성화",
            fromDate = LocalDate(2025, 3, 1),
            toDate = LocalDate(2025, 3, 31),
            today = LocalDate(2025, 3, 15),
            // 조회 범위(3월)에 생리 없지만, 임신 중이므로 마지막 생리 반환
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
                    // 그룹 2: 임신 시작일(2/1)이 생리 시작일과 같으므로 가임기/배란기 빈 배열
                    fertileDays = emptyList(),
                    ovulationDays = emptyList(),
                    period = 28,
                    isOvulationUserInput = false,
                    ovulationBasedPeriod = null,
                    pillBasedPeriod = null,
                    remainingPlaceboDay = null,
                    pregnancyStartDate = DateUtils.toJulianDay(PREGNANCY_START_DATE_GROUP2)
                )
            )
        )

        val repository = setupGroup2Data()
        runTestCase(testCase, repository)
    }

    @Test
    fun testTC_07_02_02_FertileDaysBeforePregnancyLastPeriod() = runTest {
        val testCase = TestCase(
            id = "TC-07-02-02",
            name = "임신 시작 전 가임기는 유지 (마지막 생리일 기준)",
            fromDate = LocalDate(2025, 2, 1),
            toDate = LocalDate(2025, 2, 28),
            today = LocalDate(2025, 3, 15),
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
                    // 문서 기준: 임신 시작일(2/1)이 생리 시작일과 같으므로 가임기/배란기 빈 배열
                    fertileDays = emptyList(),
                    ovulationDays = emptyList(),
                    // 문서 기준: 임신 중 기본값 28일
                    period = 28,
                    isOvulationUserInput = false,
                    ovulationBasedPeriod = null,
                    pillBasedPeriod = null,
                    remainingPlaceboDay = null,
                    pregnancyStartDate = DateUtils.toJulianDay(PREGNANCY_START_DATE_GROUP2)
                )
            )
        )

        val repository = setupGroup2Data()
        runTestCase(testCase, repository)
    }

    // ============================================
    // 그룹 3: 출산 후 복귀 (주기 1개 기록)
    // ============================================

    @Test
    fun testTC_07_03_01_PostBirthCycleRecalculation() = runTest {
        val testCase = TestCase(
            id = "TC-07-03-01",
            name = "출산 후 생리 주기 재계산",
            fromDate = LocalDate(2025, 12, 1),
            toDate = LocalDate(2026, 1, 31),
            today = LocalDate(2025, 12, 15),
            expectedCycles = listOf(
                ExpectedCycle(
                    pk = "3",
                    actualPeriod = DateRange(
                        startDate = DateUtils.toJulianDay(PERIOD_3_START),
                        endDate = DateUtils.toJulianDay(PERIOD_3_END)
                    ),
                    delayDays = 0,
                    delayPeriod = null,
                    // 문서 기준: 출산 후 정상 예측 재개
                    predictDays = listOf(
                        DateRange(
                            startDate = DateUtils.toJulianDay(LocalDate(2025, 12, 29)),
                            endDate = DateUtils.toJulianDay(LocalDate(2026, 1, 2))
                        ),
                        DateRange(
                            startDate = DateUtils.toJulianDay(LocalDate(2026, 1, 26)),
                            endDate = DateUtils.toJulianDay(LocalDate(2026, 1, 30))
                        )
                    ),
                    fertileDays = listOf(
                        DateRange(
                            startDate = DateUtils.toJulianDay(LocalDate(2025, 12, 8)),
                            endDate = DateUtils.toJulianDay(LocalDate(2025, 12, 19))
                        ),
                        DateRange(
                            startDate = DateUtils.toJulianDay(LocalDate(2026, 1, 5)),
                            endDate = DateUtils.toJulianDay(LocalDate(2026, 1, 16))
                        )
                    ),
                    ovulationDays = listOf(
                        DateRange(
                            startDate = DateUtils.toJulianDay(LocalDate(2025, 12, 13)),
                            endDate = DateUtils.toJulianDay(LocalDate(2025, 12, 15))
                        ),
                        DateRange(
                            startDate = DateUtils.toJulianDay(LocalDate(2026, 1, 10)),
                            endDate = DateUtils.toJulianDay(LocalDate(2026, 1, 12))
                        )
                    ),
                    period = 28,
                    isOvulationUserInput = false,
                    ovulationBasedPeriod = null,
                    pillBasedPeriod = null,
                    remainingPlaceboDay = null,
                    pregnancyStartDate = null
                )
            )
        )

        val repository = setupGroup3Data()
        runTestCase(testCase, repository)
    }

    // ============================================
    // 그룹 4: 출산 후 복귀 (주기 2개 기록)
    // ============================================

    @Test
    fun testTC_07_04_01_ExcludePrePregnancyRecords() = runTest {
        val testCase = TestCase(
            id = "TC-07-04-01",
            name = "임신 전 생리 기록 제외",
            fromDate = LocalDate(2026, 1, 5),  // 문서 기준: 2026-01-05 시작
            toDate = LocalDate(2026, 2, 28),
            today = LocalDate(2026, 1, 15),
            expectedCycles = listOf(
                ExpectedCycle(
                    pk = "4",
                    actualPeriod = DateRange(
                        startDate = DateUtils.toJulianDay(PERIOD_4_START),
                        endDate = DateUtils.toJulianDay(PERIOD_4_END)
                    ),
                    delayDays = 0,
                    delayPeriod = null,
                    predictDays = listOf(
                        DateRange(
                            startDate = DateUtils.toJulianDay(LocalDate(2026, 2, 9)),
                            endDate = DateUtils.toJulianDay(LocalDate(2026, 2, 13))
                        )
                    ),
                    fertileDays = listOf(
                        DateRange(
                            startDate = DateUtils.toJulianDay(LocalDate(2026, 1, 21)),
                            endDate = DateUtils.toJulianDay(LocalDate(2026, 1, 29))
                        ),
                        DateRange(
                            startDate = DateUtils.toJulianDay(LocalDate(2026, 2, 25)),
                            endDate = DateUtils.toJulianDay(LocalDate(2026, 3, 5))
                        )
                    ),
                    ovulationDays = listOf(
                        DateRange(
                            startDate = DateUtils.toJulianDay(LocalDate(2026, 1, 24)),
                            endDate = DateUtils.toJulianDay(LocalDate(2026, 1, 26))
                        ),
                        DateRange(
                            startDate = DateUtils.toJulianDay(LocalDate(2026, 2, 28)),
                            endDate = DateUtils.toJulianDay(LocalDate(2026, 3, 2))
                        )
                    ),
                    period = 35,
                    isOvulationUserInput = false,
                    ovulationBasedPeriod = null,
                    pillBasedPeriod = null,
                    remainingPlaceboDay = null,
                    pregnancyStartDate = null
                )
            )
        )

        val repository = setupGroup4Data()
        runTestCase(testCase, repository)
    }

    // ============================================
    // 데이터 설정 함수
    // ============================================

    private fun setupGroup1Data(): InMemoryPeriodRepository {
        val repository = InMemoryPeriodRepository()

        // 생리 기록
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

        // 주기 설정
        repository.setPeriodSettings(PeriodSettings(
            manualAverageCycle = MANUAL_AVERAGE_CYCLE,
            manualAverageDay = MANUAL_AVERAGE_DAY,
            autoAverageCycle = AUTO_AVERAGE_CYCLE_GROUP1_2,
            autoAverageDay = AUTO_AVERAGE_DAY,
            isAutoCalc = IS_AUTO_CALC
        ))

        // 피임약 비활성화
        repository.setPillSettings(PillSettings(isCalculatingWithPill = false))

        // 임신 정보 (출산 예정일 기준)
        repository.setActivePregnancy(PregnancyInfo(
            id = "pregnancy-1",
            isDueDateDecided = true,
            dueDate = DateUtils.toJulianDay(PREGNANCY_DUE_DATE),
            startsDate = DateUtils.toJulianDay(PREGNANCY_START_DATE_GROUP1),
            lastTheDayDate = null,
            isEnded = false,
            isMiscarriage = false
        ))

        return repository
    }

    private fun setupGroup2Data(): InMemoryPeriodRepository {
        val repository = InMemoryPeriodRepository()

        // 생리 기록
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

        // 주기 설정
        repository.setPeriodSettings(PeriodSettings(
            manualAverageCycle = MANUAL_AVERAGE_CYCLE,
            manualAverageDay = MANUAL_AVERAGE_DAY,
            autoAverageCycle = AUTO_AVERAGE_CYCLE_GROUP1_2,
            autoAverageDay = AUTO_AVERAGE_DAY,
            isAutoCalc = IS_AUTO_CALC
        ))

        // 피임약 비활성화
        repository.setPillSettings(PillSettings(isCalculatingWithPill = false))

        // 임신 정보 (마지막 생리일 기준)
        // iOS에서는 isDueDateDecided=false일 때 dueDate를 자동 계산 (lastTheDayDate + 280일)
        repository.setActivePregnancy(PregnancyInfo(
            id = "pregnancy-2",
            isDueDateDecided = false,
            dueDate = DateUtils.toJulianDay(PREGNANCY_DUE_DATE_GROUP2),  // 자동 계산: 마지막 생리 시작일 + 280일 = 2025-11-08
            startsDate = DateUtils.toJulianDay(PREGNANCY_START_DATE_GROUP2),
            lastTheDayDate = DateUtils.toJulianDay(LAST_PERIOD_START_GROUP2),
            isEnded = false,
            isMiscarriage = false
        ))

        return repository
    }

    private fun setupGroup3Data(): InMemoryPeriodRepository {
        val repository = InMemoryPeriodRepository()

        // 임신 전 생리 기록
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

        // 출산 후 생리 기록
        repository.addPeriod(PeriodRecord(
            pk = "3",
            startDate = DateUtils.toJulianDay(PERIOD_3_START),
            endDate = DateUtils.toJulianDay(PERIOD_3_END)
        ))

        // 주기 설정 (출산 후 무조건 28일)
        repository.setPeriodSettings(PeriodSettings(
            manualAverageCycle = MANUAL_AVERAGE_CYCLE,
            manualAverageDay = MANUAL_AVERAGE_DAY,
            autoAverageCycle = AUTO_AVERAGE_CYCLE_GROUP3,
            autoAverageDay = AUTO_AVERAGE_DAY,
            isAutoCalc = IS_AUTO_CALC
        ))

        // 피임약 비활성화
        repository.setPillSettings(PillSettings(isCalculatingWithPill = false))

        // 임신 정보 (출산 완료)
        repository.setActivePregnancy(PregnancyInfo(
            id = "pregnancy-3",
            isDueDateDecided = true,
            dueDate = DateUtils.toJulianDay(PREGNANCY_DUE_DATE),
            startsDate = DateUtils.toJulianDay(PREGNANCY_START_DATE_GROUP1),
            lastTheDayDate = null,
            isEnded = true,  // 출산 완료
            isMiscarriage = false
        ))

        return repository
    }

    private fun setupGroup4Data(): InMemoryPeriodRepository {
        val repository = InMemoryPeriodRepository()

        // 임신 전 생리 기록
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

        // 출산 후 생리 기록
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

        // 주기 설정 (출산 후 기록 기준 35일)
        repository.setPeriodSettings(PeriodSettings(
            manualAverageCycle = MANUAL_AVERAGE_CYCLE,
            manualAverageDay = MANUAL_AVERAGE_DAY,
            autoAverageCycle = AUTO_AVERAGE_CYCLE_GROUP4,
            autoAverageDay = AUTO_AVERAGE_DAY,
            isAutoCalc = IS_AUTO_CALC
        ))

        // 피임약 비활성화
        repository.setPillSettings(PillSettings(isCalculatingWithPill = false))

        // 임신 정보 (출산 완료)
        repository.setActivePregnancy(PregnancyInfo(
            id = "pregnancy-4",
            isDueDateDecided = true,
            dueDate = DateUtils.toJulianDay(PREGNANCY_DUE_DATE),
            startsDate = DateUtils.toJulianDay(PREGNANCY_START_DATE_GROUP1),
            lastTheDayDate = null,
            isEnded = true,  // 출산 완료
            isMiscarriage = false
        ))

        return repository
    }

    // ============================================
    // 테스트 실행 및 검증
    // ============================================

    private suspend fun runTestCase(testCase: TestCase, repository: InMemoryPeriodRepository) {
        val cycles = PeriodCalculator.calculateCycleInfo(
            repository = repository,
            fromDate = DateUtils.toJulianDay(testCase.fromDate),
            toDate = DateUtils.toJulianDay(testCase.toDate),
            today = DateUtils.toJulianDay(testCase.today)
        )

        assertEquals(
            testCase.expectedCycles.size,
            cycles.size,
            "[${testCase.id}] 주기 개수 불일치"
        )

        testCase.expectedCycles.forEachIndexed { index, expected ->
            val actual = cycles[index]

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
                expected.isOvulationUserInput,
                actual.isOvulationPeriodUserInput,
                "[${testCase.id}] Cycle $index: isOvulationUserInput 불일치"
            )

            assertEquals(
                expected.ovulationBasedPeriod,
                actual.ovulationDayPeriod,
                "[${testCase.id}] Cycle $index: ovulationBasedPeriod 불일치"
            )

            assertEquals(
                expected.pillBasedPeriod,
                actual.thePillPeriod,
                "[${testCase.id}] Cycle $index: pillBasedPeriod 불일치"
            )

            assertEquals(
                expected.pregnancyStartDate,
                actual.pregnancyStartDate,
                "[${testCase.id}] Cycle $index: pregnancyStartDate 불일치"
            )
        }

        println("${testCase.id}: ${testCase.name} - PASSED")
    }
}
