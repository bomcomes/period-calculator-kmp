package com.bomcomes.calculator

import com.bomcomes.calculator.models.*
import com.bomcomes.calculator.repository.InMemoryPeriodRepository
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Repository를 사용한 통합 테스트
 *
 * 실제 사용 시나리오:
 * 1. Repository에 데이터 저장
 * 2. Calculator가 Repository를 직접 사용해서 계산
 */
class PeriodCalculatorWithRepositoryTest {

    @Test
    fun testWithRepository_singlePeriod() = runBlocking {
        // 1. Repository 생성 및 데이터 저장
        val repository = InMemoryPeriodRepository()
        repository.addPeriod(
            PeriodRecord(
                startDate = LocalDate(2025, 1, 5),
                endDate = LocalDate(2025, 1, 9)
            )
        )
        repository.setPeriodSettings(PeriodSettings(period = 28, days = 5))

        // 2. Calculator에 Repository를 직접 넘겨서 계산
        val fromDate = LocalDate(2025, 1, 1)
        val toDate = LocalDate(2025, 2, 28)

        val result = PeriodCalculator.calculateCycleInfo(repository, fromDate, toDate)

        // 3. 검증
        assertEquals(1, result.size)
        val cycle = result[0]

        assertNotNull(cycle.actualPeriod)
        assertEquals(LocalDate(2025, 1, 5), cycle.actualPeriod!!.startDate)
        assertEquals(28, cycle.period)

        // 배란일 확인
        assertTrue(cycle.ovulationDays.isNotEmpty())

        // 가임기 확인
        assertTrue(cycle.fertileDays.isNotEmpty())

        // 생리 예정일 확인
        assertTrue(cycle.predictDays.isNotEmpty())
    }

    @Test
    fun testWithRepository_multiplePeriods() = runBlocking {
        // 1. Repository에 여러 생리 기록 저장
        val repository = InMemoryPeriodRepository()
        repository.addPeriod(
            PeriodRecord(
                startDate = LocalDate(2025, 1, 5),
                endDate = LocalDate(2025, 1, 9)
            )
        )
        repository.addPeriod(
            PeriodRecord(
                startDate = LocalDate(2025, 2, 2),
                endDate = LocalDate(2025, 2, 6)
            )
        )
        repository.setPeriodSettings(PeriodSettings(period = 28, days = 5))

        // 2. Calculator에 Repository 넘겨서 계산
        val fromDate = LocalDate(2025, 1, 5)
        val toDate = LocalDate(2025, 2, 1)

        val result = PeriodCalculator.calculateCycleInfo(repository, fromDate, toDate)

        // 3. 검증
        assertEquals(2, result.size)

        // 각 주기의 실제 주기 확인
        assertEquals(28, result[0].period) // 1/5 ~ 2/2 = 28일
        assertEquals(28, result[1].period) // 마지막은 평균 주기
    }

    @Test
    fun testWithRepository_withOvulationTest() = runBlocking {
        // 1. Repository에 데이터 저장
        val repository = InMemoryPeriodRepository()
        repository.addPeriod(
            PeriodRecord(
                startDate = LocalDate(2025, 1, 5),
                endDate = LocalDate(2025, 1, 9)
            )
        )
        repository.setPeriodSettings(PeriodSettings(period = 28, days = 5))

        // 배란 테스트 양성 결과 추가
        repository.addOvulationTest(
            OvulationTest(
                date = LocalDate(2025, 1, 18),
                result = TestResult.POSITIVE
            )
        )

        // 2. Calculator에 Repository 넘겨서 계산
        val fromDate = LocalDate(2025, 1, 1)
        val toDate = LocalDate(2025, 2, 28)

        val result = PeriodCalculator.calculateCycleInfo(repository, fromDate, toDate)

        // 3. 검증 - 배란 테스트 결과가 반영되어야 함
        val cycle = result[0]
        assertTrue(cycle.ovulationDays.isNotEmpty())

        // 배란 테스트 양성일(1/18)이 배란일에 포함되어야 함
        val hasOvulationTestDate = cycle.ovulationDays.any { range ->
            LocalDate(2025, 1, 18) in range.startDate..range.endDate
        }
        assertTrue(hasOvulationTestDate, "Ovulation test positive date should be in ovulation days")
    }

    @Test
    fun testWithRepository_withPregnancy() = runBlocking {
        // 1. Repository에 데이터 저장
        val repository = InMemoryPeriodRepository()
        repository.addPeriod(
            PeriodRecord(
                startDate = LocalDate(2025, 1, 5),
                endDate = LocalDate(2025, 1, 9)
            )
        )
        repository.setPeriodSettings(PeriodSettings(period = 28, days = 5))
        repository.setActivePregnancy(
            PregnancyInfo(
                startsDate = LocalDate(2025, 1, 20),
                isEnded = false,
                isMiscarriage = false,
                isDeleted = false
            )
        )

        // 2. Calculator에 Repository 넘겨서 계산
        val fromDate = LocalDate(2025, 1, 1)
        val toDate = LocalDate(2025, 3, 31)

        val result = PeriodCalculator.calculateCycleInfo(repository, fromDate, toDate)

        // 3. 검증 - 임신 시작일 이후 배란일/가임기가 없어야 함
        val cycle = result[0]
        assertEquals(LocalDate(2025, 1, 20), cycle.pregnancyStartDate)

        // 배란일/가임기가 임신 시작일 이전에만 있어야 함
        cycle.fertileDays.forEach { range ->
            assertTrue(range.endDate < LocalDate(2025, 1, 20),
                "Fertile days should end before pregnancy starts")
        }
    }

    @Test
    fun testWithRepository_getLastPeriodBefore() = runBlocking {
        val repository = InMemoryPeriodRepository()

        // 데이터 저장
        repository.addPeriod(
            PeriodRecord(
                pk = "1",
                startDate = LocalDate(2025, 1, 5),
                endDate = LocalDate(2025, 1, 9)
            )
        )
        repository.addPeriod(
            PeriodRecord(
                pk = "2",
                startDate = LocalDate(2025, 2, 2),
                endDate = LocalDate(2025, 2, 6)
            )
        )

        // 2/10 이전의 가장 최근 생리는 2/2
        val lastPeriod = repository.getLastPeriodBefore(LocalDate(2025, 2, 10))
        assertNotNull(lastPeriod)
        assertEquals("2", lastPeriod.pk)
        assertEquals(LocalDate(2025, 2, 2), lastPeriod.startDate)
    }

    @Test
    fun testWithRepository_realScenario_60days() = runBlocking {
        // 실제 사용 시나리오: 마지막 생리일 기준으로 60일치 계산
        val repository = InMemoryPeriodRepository()

        // 마지막 생리: 3/1 ~ 3/5
        repository.addPeriod(
            PeriodRecord(
                startDate = LocalDate(2024, 3, 1),
                endDate = LocalDate(2024, 3, 5)
            )
        )

        // 이전 생리: 2/2 ~ 2/6 (28일 전)
        repository.addPeriod(
            PeriodRecord(
                startDate = LocalDate(2024, 2, 2),
                endDate = LocalDate(2024, 2, 6)
            )
        )

        repository.setPeriodSettings(PeriodSettings(period = 28, days = 5))

        // 3/1부터 60일치 계산 (~ 4/30)
        val fromDate = LocalDate(2024, 3, 1)
        val toDate = LocalDate(2024, 4, 30)

        val result = PeriodCalculator.calculateCycleInfo(repository, fromDate, toDate)

        println("=== 60일 계산 결과 ===")
        println("주기 개수: ${result.size}")
        result.forEachIndexed { idx, cycle ->
            println("\n[주기 ${idx + 1}]")
            println("  실제 생리일: ${cycle.actualPeriod?.startDate} ~ ${cycle.actualPeriod?.endDate}")
            println("  예측 생리일: ${cycle.predictDays.size}개")
            cycle.predictDays.forEach { println("    - $it") }
            println("  배란일: ${cycle.ovulationDays.size}개")
            cycle.ovulationDays.forEach { println("    - $it") }
            println("  가임기: ${cycle.fertileDays.size}개")
            cycle.fertileDays.forEach { println("    - $it") }
            println("  주기: ${cycle.period}일")
        }

        // 검증
        // fromDate가 3/1이므로 2/2 생리는 제외되고 3/1 생리만 나옴
        assertEquals(1, result.size)

        // 3/1 주기: 다음 예정일이 있어야 함
        val cycle1 = result[0]
        assertEquals(LocalDate(2024, 3, 1), cycle1.actualPeriod?.startDate)
        assertEquals(28, cycle1.period)

        // 다음 예정일: 3/29, 4/26
        assertTrue(cycle1.predictDays.isNotEmpty(), "Should have predicted periods")
        assertEquals(LocalDate(2024, 3, 29), cycle1.predictDays[0].startDate)

        // Note: 배란일/가임기가 비어있는 이슈는 별도 수정 필요
        // adjustedFromDate로 인해 검색 범위가 좁아져서 조건 실패
    }

    @Test
    fun testWithRepository_getFirstPeriodAfter() = runBlocking {
        val repository = InMemoryPeriodRepository()

        // 데이터 저장
        repository.addPeriod(
            PeriodRecord(
                pk = "1",
                startDate = LocalDate(2025, 1, 5),
                endDate = LocalDate(2025, 1, 9)
            )
        )
        repository.addPeriod(
            PeriodRecord(
                pk = "2",
                startDate = LocalDate(2025, 2, 2),
                endDate = LocalDate(2025, 2, 6)
            )
        )

        // 1/20 이후의 가장 가까운 생리는 2/2
        val firstPeriod = repository.getFirstPeriodAfter(LocalDate(2025, 1, 20))
        assertNotNull(firstPeriod)
        assertEquals("2", firstPeriod.pk)
        assertEquals(LocalDate(2025, 2, 2), firstPeriod.startDate)
    }
}
