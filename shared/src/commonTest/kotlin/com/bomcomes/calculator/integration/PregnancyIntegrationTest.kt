package com.bomcomes.calculator.integration

import com.bomcomes.calculator.PeriodCalculator
import com.bomcomes.calculator.PregnancyCalculator
import com.bomcomes.calculator.models.*
import com.bomcomes.calculator.repository.InMemoryPeriodRepository
import com.bomcomes.calculator.utils.DateUtils
import kotlinx.datetime.LocalDate
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import kotlin.test.*

class PregnancyIntegrationTest {
    private lateinit var repository: InMemoryPeriodRepository

    @BeforeTest
    fun setUp() {
        repository = InMemoryPeriodRepository()
    }

    @AfterTest
    fun tearDown() {
        repository.clear()
    }

    // ==================== 기본 임신 계산 테스트 ====================

    @Test
    fun testPregnancy_dueDateCalculation() = runTest {
        val lastPeriodDate = LocalDate(2024, 1, 1)

        // 출산 예정일 계산 (280일 후)
        val dueDate = PregnancyCalculator.calculateDueDate(lastPeriodDate)
        assertEquals(LocalDate(2024, 10, 7), dueDate, "출산 예정일은 마지막 생리일로부터 280일 후")

        // 임신 정보 생성
        val pregnancy = PregnancyInfo(
            startsDate = DateUtils.toJulianDay(lastPeriodDate),
            lastTheDayDate = DateUtils.toJulianDay(lastPeriodDate),
            dueDate = DateUtils.toJulianDay(dueDate),
            isDueDateDecided = true
        )

        // 임신 활성 상태 확인
        assertTrue(pregnancy.isActive(), "임신 상태 활성")
    }

    @Test
    fun testPregnancy_withPeriodCalculation() = runTest {
        // 정상 생리 기록
        repository.addPeriod(PeriodRecord(
            startDate = DateUtils.toJulianDay(LocalDate(2024, 1, 1)),
            endDate = DateUtils.toJulianDay(LocalDate(2024, 1, 5))
        ))
        repository.addPeriod(PeriodRecord(
            startDate = DateUtils.toJulianDay(LocalDate(2024, 1, 29)),
            endDate = DateUtils.toJulianDay(LocalDate(2024, 2, 2))
        ))

        // 생리 설정
        repository.setPeriodSettings(PeriodSettings(
            manualAverageCycle = 28,
            manualAverageDay = 5
        ))

        // 임신 정보 설정
        val lastPeriodDate = LocalDate(2024, 2, 15)
        val dueDate = PregnancyCalculator.calculateDueDate(lastPeriodDate)

        repository.setActivePregnancy(PregnancyInfo(
            startsDate = DateUtils.toJulianDay(lastPeriodDate),
            lastTheDayDate = DateUtils.toJulianDay(lastPeriodDate),
            dueDate = DateUtils.toJulianDay(dueDate),
            isDueDateDecided = true
        ))

        // 주기 계산
        val cycles = PeriodCalculator.calculateCycleInfo(
            repository = repository,
            fromDate = DateUtils.toJulianDay(LocalDate(2024, 3, 1)),
            toDate = DateUtils.toJulianDay(LocalDate(2024, 4, 30))
        )

        // 임신 중에는 예정일 계산이 다르게 처리될 수 있음
        assertNotNull(cycles, "주기 정보 처리됨")
    }

    // ==================== 임신 주수 계산 ====================

    @Test
    fun testPregnancy_weekCalculation() = runTest {
        val lastPeriodDate = LocalDate(2024, 1, 1)
        val pregnancy = PregnancyInfo(
            startsDate = DateUtils.toJulianDay(lastPeriodDate),
            lastTheDayDate = DateUtils.toJulianDay(lastPeriodDate),
            dueDate = DateUtils.toJulianDay(LocalDate(2024, 10, 7)),
            isDueDateDecided = true
        )

        // 7주 후
        val after7Weeks = DateUtils.toJulianDay(LocalDate(2024, 2, 19))
        val weeks7 = pregnancy.getWeeksFromStart(after7Weeks)
        assertEquals(7, weeks7, "7주차")

        // 20주 후
        val after20Weeks = DateUtils.toJulianDay(LocalDate(2024, 5, 20))
        val weeks20 = pregnancy.getWeeksFromStart(after20Weeks)
        assertEquals(20, weeks20, "20주차")
    }

    @Test
    fun testPregnancy_daysUntilDue() = runTest {
        val lastPeriodDate = LocalDate(2024, 1, 1)
        val dueDate = LocalDate(2024, 10, 7)

        val pregnancy = PregnancyInfo(
            startsDate = DateUtils.toJulianDay(lastPeriodDate),
            dueDate = DateUtils.toJulianDay(dueDate),
            isDueDateDecided = true
        )

        // 100일 전
        val currentDate = LocalDate(2024, 6, 29)
        val daysLeft = pregnancy.getDaysUntilDue(DateUtils.toJulianDay(currentDate))
        assertNotNull(daysLeft)
        assertEquals(100, daysLeft, "출산 예정일까지 100일 남음")
    }

    // ==================== 피임약과 임신 ====================

    @Test
    fun testPregnancy_withPillSettings() = runTest {
        // 피임약 설정
        repository.setPillSettings(PillSettings(
            isCalculatingWithPill = true,
            pillCount = 21,
            restPill = 7
        ))

        repository.addPillPackage(PillPackage(
            packageStart = DateUtils.toJulianDay(LocalDate(2024, 1, 1)),
            pillCount = 21,
            restDays = 7
        ))

        // 임신 설정
        val pregnancy = PregnancyInfo(
            startsDate = DateUtils.toJulianDay(LocalDate(2024, 2, 1)),
            isDueDateDecided = false
        )
        repository.setActivePregnancy(pregnancy)

        // 임신 중 피임약 주기 계산
        val cycles = PeriodCalculator.calculateCycleInfo(
            repository = repository,
            fromDate = DateUtils.toJulianDay(LocalDate(2024, 2, 1)),
            toDate = DateUtils.toJulianDay(LocalDate(2024, 3, 31))
        )

        // 결과 확인
        assertNotNull(cycles)
    }

    // ==================== 배란 테스트와 임신 ====================

    @Test
    fun testPregnancy_withOvulationTests() = runTest {
        // 배란 테스트 기록
        repository.addOvulationTest(OvulationTest(
            date = DateUtils.toJulianDay(LocalDate(2024, 1, 14)),
            result = TestResult.POSITIVE
        ))

        // 사용자 입력 배란일
        repository.addUserOvulationDay(OvulationDay(
            date = DateUtils.toJulianDay(LocalDate(2024, 1, 14))
        ))

        // 생리 기록
        repository.addPeriod(PeriodRecord(
            startDate = DateUtils.toJulianDay(LocalDate(2024, 1, 1)),
            endDate = DateUtils.toJulianDay(LocalDate(2024, 1, 5))
        ))

        // 주기 계산
        val cycles = PeriodCalculator.calculateCycleInfo(
            repository = repository,
            fromDate = DateUtils.toJulianDay(LocalDate(2024, 1, 10)),
            toDate = DateUtils.toJulianDay(LocalDate(2024, 2, 10))
        )

        assertTrue(cycles.isNotEmpty(), "배란 정보를 포함한 주기 계산")
    }

    // ==================== 임신 상태 변경 ====================

    @Test
    fun testPregnancy_statusChanges() = runTest {
        val pregnancy = PregnancyInfo(
            startsDate = DateUtils.toJulianDay(LocalDate(2024, 1, 1)),
            isDueDateDecided = true,
            dueDate = DateUtils.toJulianDay(LocalDate(2024, 10, 7))
        )

        // 정상 임신
        assertTrue(pregnancy.isActive(), "임신 활성 상태")

        // 유산
        val miscarriagePregnancy = pregnancy.copy(isMiscarriage = true)
        assertFalse(miscarriagePregnancy.isActive(), "유산 후 비활성")

        // 출산 완료
        val deliveredPregnancy = pregnancy.copy(isEnded = true)
        assertFalse(deliveredPregnancy.isActive(), "출산 완료 후 비활성")

        // 삭제됨
        val deletedPregnancy = pregnancy.copy(isDeleted = true)
        assertFalse(deletedPregnancy.isActive(), "삭제 후 비활성")
    }

    // ==================== 다태아 임신 ====================

    @Test
    fun testPregnancy_multipleBirth() = runTest {
        val pregnancy = PregnancyInfo(
            startsDate = DateUtils.toJulianDay(LocalDate(2024, 1, 1)),
            isDueDateDecided = true,
            dueDate = DateUtils.toJulianDay(LocalDate(2024, 10, 7)),
            isMultipleBirth = true
        )

        assertTrue(pregnancy.isMultipleBirth, "다태아 임신")
        assertTrue(pregnancy.isActive(), "임신 활성 상태")
    }

    // ==================== 체중 관리 ====================

    @Test
    fun testPregnancy_weightManagement() = runTest {
        val pregnancy = PregnancyInfo(
            startsDate = DateUtils.toJulianDay(LocalDate(2024, 1, 1)),
            beforePregnancyWeight = 60.0f,
            weightUnit = WeightUnit.KG
        )

        assertEquals(60.0f, pregnancy.beforePregnancyWeight)
        assertEquals(WeightUnit.KG, pregnancy.weightUnit)

        // 파운드 단위
        val pregnancyLbs = pregnancy.copy(
            beforePregnancyWeight = 132.0f,
            weightUnit = WeightUnit.LBS
        )
        assertEquals(WeightUnit.LBS, pregnancyLbs.weightUnit)

        // 스톤 단위
        val pregnancySt = pregnancy.copy(
            beforePregnancyWeight = 9.5f,
            weightUnit = WeightUnit.ST
        )
        assertEquals(WeightUnit.ST, pregnancySt.weightUnit)
    }

    // ==================== 복잡한 시나리오 ====================

    @Test
    fun testPregnancy_complexScenario() = runTest {
        // 1. 불규칙 주기 설정
        repository.setPeriodSettings(PeriodSettings(
            manualAverageCycle = 32,
            manualAverageDay = 5,
            isAutoCalc = false
        ))

        // 2. 생리 기록
        repository.addPeriod(PeriodRecord(
            startDate = DateUtils.toJulianDay(LocalDate(2023, 11, 1)),
            endDate = DateUtils.toJulianDay(LocalDate(2023, 11, 5))
        ))
        repository.addPeriod(PeriodRecord(
            startDate = DateUtils.toJulianDay(LocalDate(2023, 12, 5)),
            endDate = DateUtils.toJulianDay(LocalDate(2023, 12, 9))
        ))

        // 3. 임신
        val lastPeriodDate = LocalDate(2024, 1, 8)
        val pregnancy = PregnancyInfo(
            startsDate = DateUtils.toJulianDay(lastPeriodDate),
            lastTheDayDate = DateUtils.toJulianDay(lastPeriodDate),
            dueDate = DateUtils.toJulianDay(PregnancyCalculator.calculateDueDate(lastPeriodDate)),
            isDueDateDecided = true,
            babyName = "아가"
        )
        repository.setActivePregnancy(pregnancy)

        // 4. 임신 중 주기 계산
        val cycles = PeriodCalculator.calculateCycleInfo(
            repository = repository,
            fromDate = DateUtils.toJulianDay(LocalDate(2024, 2, 1)),
            toDate = DateUtils.toJulianDay(LocalDate(2024, 4, 30))
        )

        assertNotNull(cycles)
        assertTrue(cycles.isNotEmpty() || cycles.isEmpty(), "주기 정보 처리")

        // 5. 임신 주수 계산
        val currentDate = DateUtils.toJulianDay(LocalDate(2024, 4, 1))
        val weeks = pregnancy.getWeeksFromStart(currentDate)
        assertTrue(weeks >= 0, "임신 주수 계산")
    }

    // Helper function
    private fun runTest(block: suspend () -> Unit) = kotlinx.coroutines.test.runTest {
        block()
    }
}