package com.bomcomes.calculator.integration

import com.bomcomes.calculator.PeriodCalculator
import com.bomcomes.calculator.models.PeriodRecord
import com.bomcomes.calculator.models.PeriodSettings
import com.bomcomes.calculator.repository.InMemoryPeriodRepository
import com.bomcomes.calculator.utils.DateUtils
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 통합 테스트: 다양한 시나리오별 테스트
 * Repository를 통해 실제 사용 시나리오를 검증
 * JavaScript 테스트(test-cases/01~08.js)는 이 테스트와 동일한 시나리오로 JS export 계층을 검증
 */
class PeriodCalculatorScenarioTest {

    @Test
    fun test01_basicSinglePeriod() = runTest {
        // 시나리오: 생리 1개, 28일 주기, 범위 내 예측
        val repository = InMemoryPeriodRepository().apply {
            addPeriod(
                PeriodRecord(
                    pk = "1",
                    startDate = DateUtils.toJulianDay(LocalDate(2024, 3, 5)),
                    endDate = DateUtils.toJulianDay(LocalDate(2024, 3, 9))  // 5일 (3/5~3/9)
                )
            )
            setPeriodSettings(
                PeriodSettings(
                    manualAverageCycle = 28,
                    manualAverageDay = 5,
                    autoAverageCycle = 28,
                    autoAverageDay = 5,
                    isAutoCalc = false
                )
            )
        }

        val searchFrom = LocalDate(2024, 3, 1)
        val searchTo = LocalDate(2024, 4, 30)
        val today = LocalDate(2024, 3, 10)

        val result = PeriodCalculator.calculateCycleInfo(
            repository,
            DateUtils.toJulianDay(searchFrom),
            DateUtils.toJulianDay(searchTo),
            DateUtils.toJulianDay(today)
        )

        // 검증
        assertEquals(1, result.size, "주기 개수는 1개")

        val cycle = result[0]

        // 예측 생리일 검증: [0] 4/2~4/6, [1] 4/30~5/4
        // 두 번째 예측이 searchTo(4/30)를 넘어 5/4까지 이어지지만, 시작일(4/30)이 범위 내이므로 포함
        assertEquals(2, cycle.predictDays.size, "예측 생리일 2개")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 4, 2)), cycle.predictDays[0].startDate, "첫 예측 시작: 4/2")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 4, 6)), cycle.predictDays[0].endDate, "첫 예측 종료: 4/6 (5일간)")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 4, 30)), cycle.predictDays[1].startDate, "두 번째 예측 시작: 4/30")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 5, 4)), cycle.predictDays[1].endDate, "두 번째 예측 종료: 5/4")

        // 배란기 검증: [0] 3/17~3/19, [1] 4/14~4/16
        // 5/12~5/14 배란기는 시작일이 searchTo(4/30)를 초과하므로 필터링됨
        assertEquals(2, cycle.ovulationDays.size, "배란기 2개 (범위 내)")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 3, 17)), cycle.ovulationDays[0].startDate, "첫 배란기 시작: 3/17")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 3, 19)), cycle.ovulationDays[0].endDate, "첫 배란기 종료: 3/19")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 4, 14)), cycle.ovulationDays[1].startDate, "두 번째 배란기 시작: 4/14")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 4, 16)), cycle.ovulationDays[1].endDate, "두 번째 배란기 종료: 4/16")

        // 가임기 검증: [0] 3/12~3/23, [1] 4/9~4/20
        // 5/7~5/18 가임기는 시작일이 searchTo(4/30)를 초과하므로 필터링됨
        assertEquals(2, cycle.fertileDays.size, "가임기 2개 (범위 내)")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 3, 12)), cycle.fertileDays[0].startDate, "첫 가임기 시작: 3/12")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 3, 23)), cycle.fertileDays[0].endDate, "첫 가임기 종료: 3/23")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 4, 9)), cycle.fertileDays[1].startDate, "두 번째 가임기 시작: 4/9")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 4, 20)), cycle.fertileDays[1].endDate, "두 번째 가임기 종료: 4/20")

        assertEquals(0, cycle.delayTheDays, "지연 없음")
        assertEquals(28, cycle.period, "평균 주기 28일")
    }

    @Test
    fun test02_delayedPeriod() = runTest {
        // 시나리오: 생리 지연 (예정일 지나서 테스트)
        val repository = InMemoryPeriodRepository().apply {
            addPeriod(
                PeriodRecord(
                    pk = "1",
                    startDate = DateUtils.toJulianDay(LocalDate(2024, 3, 5)),
                    endDate = DateUtils.toJulianDay(LocalDate(2024, 3, 9))  // 5일 (3/5~3/9)
                )
            )
            setPeriodSettings(
                PeriodSettings(
                    manualAverageCycle = 28,
                    manualAverageDay = 5,
                    autoAverageCycle = 28,
                    autoAverageDay = 5,
                    isAutoCalc = false
                )
            )
        }

        val searchFrom = LocalDate(2024, 3, 1)
        val searchTo = LocalDate(2024, 4, 30)
        val today = LocalDate(2024, 4, 5) // 예정일(4/2) 지난 날

        val result = PeriodCalculator.calculateCycleInfo(
            repository,
            DateUtils.toJulianDay(searchFrom),
            DateUtils.toJulianDay(searchTo),
            DateUtils.toJulianDay(today)
        )

        // 검증
        assertEquals(1, result.size)

        val cycle = result[0]

        // 지연 검증: 예정일 4/2, 오늘 4/5 → 4일 지연 (4/2, 4/3, 4/4, 4/5)
        assertEquals(4, cycle.delayTheDays, "지연 4일 (4/2~4/5)")
        assertTrue(cycle.delayDay != null, "지연 기간 존재")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 4, 2)), cycle.delayDay!!.startDate, "지연 시작: 4/2 (예정일)")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 4, 5)), cycle.delayDay!!.endDate, "지연 종료: 4/5 (오늘)")

        // 예측 생리일: [0] 2024-03-09, [1] 2024-04-06 (지연 다음날부터), [2] 2024-05-04
        assertTrue(cycle.predictDays.size >= 2, "예측 생리일 존재")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 4, 6)), cycle.predictDays[1].startDate, "지연 후 다음 예측: 4/6")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 4, 10)), cycle.predictDays[1].endDate, "지연 후 다음 예측 종료: 4/10")
    }

    @Test
    fun test03_multiplePeriods() = runTest {
        // 시나리오: 생리 2개, 실제 주기 계산
        val repository = InMemoryPeriodRepository().apply {
            addPeriod(
                PeriodRecord(
                    pk = "1",
                    startDate = DateUtils.toJulianDay(LocalDate(2024, 2, 5)),
                    endDate = DateUtils.toJulianDay(LocalDate(2024, 2, 9))  // 5일 (2/5~2/9)
                )
            )
            addPeriod(
                PeriodRecord(
                    pk = "2",
                    startDate = DateUtils.toJulianDay(LocalDate(2024, 3, 5)),
                    endDate = DateUtils.toJulianDay(LocalDate(2024, 3, 9))  // 5일 (3/5~3/9)
                )
            )
            setPeriodSettings(
                PeriodSettings(
                    manualAverageCycle = 28,  // 수동 설정 평균 (isAutoCalc=false일 때 사용)
                    manualAverageDay = 5,
                    autoAverageCycle = 29,  // 자동 계산 평균: 2/5 → 3/5 = 29일
                    autoAverageDay = 5,
                    isAutoCalc = false  // 수동 모드: manualAverageCycle(28일) 사용
                )
            )
        }

        val searchFrom = LocalDate(2024, 2, 1)
        val searchTo = LocalDate(2024, 4, 30)
        val today = LocalDate(2024, 3, 10)

        val result = PeriodCalculator.calculateCycleInfo(
            repository,
            DateUtils.toJulianDay(searchFrom),
            DateUtils.toJulianDay(searchTo),
            DateUtils.toJulianDay(today)
        )

        // 검증
        assertEquals(2, result.size, "주기 2개")

        // 첫 번째 주기: 2/5 → 3/5 = 29일
        val firstCycle = result[0]
        assertEquals(29, firstCycle.period, "첫 번째 주기 29일")
        assertEquals("1", firstCycle.pk, "첫 주기 pk")

        // 첫 번째 주기의 배란기: 2/17~2/19 (2/5 + 12~14일)
        assertTrue(firstCycle.ovulationDays.size >= 1, "첫 주기 배란기 존재")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 2, 17)), firstCycle.ovulationDays[0].startDate, "첫 주기 배란기 시작: 2/17")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 2, 19)), firstCycle.ovulationDays[0].endDate, "첫 주기 배란기 종료: 2/19")

        // 첫 번째 주기의 가임기: 2/12~2/23
        assertTrue(firstCycle.fertileDays.size >= 1, "첫 주기 가임기 존재")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 2, 12)), firstCycle.fertileDays[0].startDate, "첫 주기 가임기 시작: 2/12")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 2, 23)), firstCycle.fertileDays[0].endDate, "첫 주기 가임기 종료: 2/23")

        // 두 번째 주기: 3/5 시작, 수동 평균 28일 사용 (isAutoCalc = false)
        val secondCycle = result[1]
        assertEquals(28, secondCycle.period, "두 번째 주기 28일 (수동 설정)")
        assertEquals("2", secondCycle.pk, "두 번째 주기 pk")

        // 두 번째 주기의 예측: 4/2 (3/5 + 28일), 4/30 (4/2 + 28일)
        assertEquals(2, secondCycle.predictDays.size, "다음 예측 2개")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 4, 2)), secondCycle.predictDays[0].startDate, "첫 예측 시작: 4/2 (3/5 + 28일)")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 4, 6)), secondCycle.predictDays[0].endDate, "첫 예측 종료: 4/6 (5일간)")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 4, 30)), secondCycle.predictDays[1].startDate, "두 번째 예측 시작: 4/30 (4/2 + 28일)")

        // 두 번째 주기의 배란기: 3/17~3/19 (3/5 + 12~14일), 4/14~4/16 (4/2 + 12~14일)
        assertEquals(2, secondCycle.ovulationDays.size, "배란기 2개 (범위 내)")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 3, 17)), secondCycle.ovulationDays[0].startDate, "첫 배란기 시작: 3/17")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 3, 19)), secondCycle.ovulationDays[0].endDate, "첫 배란기 종료: 3/19")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 4, 14)), secondCycle.ovulationDays[1].startDate, "두 번째 배란기 시작: 4/14")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 4, 16)), secondCycle.ovulationDays[1].endDate, "두 번째 배란기 종료: 4/16")

        // 두 번째 주기의 가임기: 3/12~3/23, 4/9~4/20
        assertEquals(2, secondCycle.fertileDays.size, "가임기 2개 (범위 내)")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 3, 12)), secondCycle.fertileDays[0].startDate, "첫 가임기 시작: 3/12")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 3, 23)), secondCycle.fertileDays[0].endDate, "첫 가임기 종료: 3/23")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 4, 9)), secondCycle.fertileDays[1].startDate, "두 번째 가임기 시작: 4/9")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 4, 20)), secondCycle.fertileDays[1].endDate, "두 번째 가임기 종료: 4/20")
    }

    @Test
    fun test04_pastSearch() = runTest {
        // 시나리오: 생리 기록보다 완전히 이전 범위 검색 (역산 예측 확인)
        // 실제 생리(3/5)보다 이전인 2월 전체를 검색
        // 필터링 규칙: startDate > searchTo(2/28)인 모든 데이터는 제외됨
        val repository = InMemoryPeriodRepository().apply {
            addPeriod(
                PeriodRecord(
                    pk = "1",
                    startDate = DateUtils.toJulianDay(LocalDate(2024, 3, 5)),
                    endDate = DateUtils.toJulianDay(LocalDate(2024, 3, 9))  // 5일 (3/5~3/9)
                )
            )
            setPeriodSettings(
                PeriodSettings(
                    manualAverageCycle = 28,
                    manualAverageDay = 5,
                    autoAverageCycle = 28,
                    autoAverageDay = 5,
                    isAutoCalc = false
                )
            )
        }

        val searchFrom = LocalDate(2024, 2, 1)  // 생리 기록(3/5)보다 한참 이전
        val searchTo = LocalDate(2024, 2, 28)   // 생리 시작 전까지
        val today = LocalDate(2024, 4, 15)      // 현재 (과거 데이터 조회)

        val result = PeriodCalculator.calculateCycleInfo(
            repository,
            DateUtils.toJulianDay(searchFrom),
            DateUtils.toJulianDay(searchTo),
            DateUtils.toJulianDay(today)
        )

        // 검증: 범위 내 생리 없음 + 이전 생리 없음 → 빈 배열 반환
        assertEquals(0, result.size, "주기 정보 없음 (범위 내/이전 생리 모두 없음)")
    }

    @Test
    fun test05_longRange() = runTest {
        // 시나리오: 4개월 범위에서 여러 주기 확인
        val repository = InMemoryPeriodRepository().apply {
            addPeriod(
                PeriodRecord(
                    pk = "1",
                    startDate = DateUtils.toJulianDay(LocalDate(2024, 3, 5)),
                    endDate = DateUtils.toJulianDay(LocalDate(2024, 3, 9))  // 5일 (3/5~3/9)
                )
            )
            setPeriodSettings(
                PeriodSettings(
                    manualAverageCycle = 28,
                    manualAverageDay = 5,
                    autoAverageCycle = 28,
                    autoAverageDay = 5,
                    isAutoCalc = false
                )
            )
        }

        val searchFrom = LocalDate(2024, 3, 1)
        val searchTo = LocalDate(2024, 6, 30) // 4개월
        val today = LocalDate(2024, 3, 10)      // 생리 직후

        val result = PeriodCalculator.calculateCycleInfo(
            repository,
            DateUtils.toJulianDay(searchFrom),
            DateUtils.toJulianDay(searchTo),
            DateUtils.toJulianDay(today)
        )

        // 검증
        assertEquals(1, result.size)

        val cycle = result[0]

        // 예측 생리일: [0] 2024-04-02, [1] 2024-04-30, [2] 2024-05-28, [3] 2024-06-25
        assertEquals(4, cycle.predictDays.size, "예측 생리일 4개 (4개월 범위)")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 4, 2)), cycle.predictDays[0].startDate, "첫 예측: 4/2")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 4, 6)), cycle.predictDays[0].endDate, "첫 예측 종료: 4/6")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 4, 30)), cycle.predictDays[1].startDate, "두 번째 예측: 4/30")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 5, 4)), cycle.predictDays[1].endDate, "두 번째 예측 종료: 5/4")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 5, 28)), cycle.predictDays[2].startDate, "세 번째 예측: 5/28")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 6, 1)), cycle.predictDays[2].endDate, "세 번째 예측 종료: 6/1")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 6, 25)), cycle.predictDays[3].startDate, "네 번째 예측: 6/25")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 6, 29)), cycle.predictDays[3].endDate, "네 번째 예측 종료: 6/29")

        // 배란기: [0] 3/17~19, [1] 4/14~16, [2] 5/12~14, [3] 6/09~11
        // [4] 7/7~7/9는 searchTo(6/30)를 초과하므로 필터링됨
        assertEquals(4, cycle.ovulationDays.size, "배란기 4개 (범위 내)")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 3, 17)), cycle.ovulationDays[0].startDate, "첫 배란기 시작: 3/17")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 3, 19)), cycle.ovulationDays[0].endDate, "첫 배란기 종료: 3/19")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 4, 14)), cycle.ovulationDays[1].startDate, "두 번째 배란기 시작: 4/14")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 4, 16)), cycle.ovulationDays[1].endDate, "두 번째 배란기 종료: 4/16")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 5, 12)), cycle.ovulationDays[2].startDate, "세 번째 배란기 시작: 5/12")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 5, 14)), cycle.ovulationDays[2].endDate, "세 번째 배란기 종료: 5/14")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 6, 9)), cycle.ovulationDays[3].startDate, "네 번째 배란기 시작: 6/9")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 6, 11)), cycle.ovulationDays[3].endDate, "네 번째 배란기 종료: 6/11")

        // 가임기: [0] 3/12~23, [1] 4/9~20, [2] 5/7~18, [3] 6/4~15
        assertEquals(4, cycle.fertileDays.size, "가임기 4개 (범위 내)")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 3, 12)), cycle.fertileDays[0].startDate, "첫 가임기 시작: 3/12")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 3, 23)), cycle.fertileDays[0].endDate, "첫 가임기 종료: 3/23")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 4, 9)), cycle.fertileDays[1].startDate, "두 번째 가임기 시작: 4/9")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 4, 20)), cycle.fertileDays[1].endDate, "두 번째 가임기 종료: 4/20")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 5, 7)), cycle.fertileDays[2].startDate, "세 번째 가임기 시작: 5/7")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 5, 18)), cycle.fertileDays[2].endDate, "세 번째 가임기 종료: 5/18")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 6, 4)), cycle.fertileDays[3].startDate, "네 번째 가임기 시작: 6/4")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 6, 15)), cycle.fertileDays[3].endDate, "네 번째 가임기 종료: 6/15")
    }

    @Test
    fun test06_rangeAfterLastPeriod() = runTest {
        // 시나리오: 생리 기록 3개, 검색 범위가 마지막 주기 포함
        val repository = InMemoryPeriodRepository().apply {
            addPeriod(PeriodRecord(pk = "1", startDate = DateUtils.toJulianDay(LocalDate(2024, 1, 5)), endDate = DateUtils.toJulianDay(LocalDate(2024, 1, 10))))  // 6일
            addPeriod(PeriodRecord(pk = "2", startDate = DateUtils.toJulianDay(LocalDate(2024, 2, 3)), endDate = DateUtils.toJulianDay(LocalDate(2024, 2, 8))))   // 6일
            addPeriod(PeriodRecord(pk = "3", startDate = DateUtils.toJulianDay(LocalDate(2024, 3, 2)), endDate = DateUtils.toJulianDay(LocalDate(2024, 3, 7))))   // 6일
            setPeriodSettings(
                PeriodSettings(
                    manualAverageCycle = 28,
                    manualAverageDay = 5,
                    autoAverageCycle = 29,  // 실제 평균: (1/5→2/3=29일, 2/3→3/2=28일) → 29일
                    autoAverageDay = 6,     // 실제 평균: 6일
                    isAutoCalc = false
                )
            )
        }

        val searchFrom = LocalDate(2024, 3, 1)
        val searchTo = LocalDate(2024, 5, 31)
        val today = LocalDate(2024, 3, 15)      // 3월 중순

        val result = PeriodCalculator.calculateCycleInfo(
            repository,
            DateUtils.toJulianDay(searchFrom),
            DateUtils.toJulianDay(searchTo),
            DateUtils.toJulianDay(today)
        )

        // 검증
        // 검색 범위(3/1~5/31)에는 2/3 생리의 예측/배란/가임도 포함됨
        assertEquals(2, result.size, "주기 정보 2개")

        // pk=2 (2/3 생리): 예측/배란/가임이 3월 이후
        val cycle2 = result.find { it.pk == "2" }!!
        assertEquals("2", cycle2.pk, "pk=2 (두 번째 생리)")
        assertEquals(28, cycle2.period, "주기 28일 (2/3→3/2)")

        // pk=3 (3/2 생리)
        val cycle = result.find { it.pk == "3" }!!
        assertEquals("3", cycle.pk, "pk=3 (세 번째 생리)")
        assertEquals(28, cycle.period, "주기 28일 (수동 설정값)")

        // 실제 생리일: 3/2~3/7
        assertTrue(cycle.actualPeriod != null, "실제 생리일 존재")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 3, 2)), cycle.actualPeriod!!.startDate, "생리 시작: 3/2")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 3, 7)), cycle.actualPeriod!!.endDate, "생리 종료: 3/7")

        // 예측 생리일: 3/2 + 28일 = 3/30, 4/27, 5/25
        assertEquals(3, cycle.predictDays.size, "예측 생리일 3개")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 3, 30)), cycle.predictDays[0].startDate, "첫 예측: 3/30")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 4, 27)), cycle.predictDays[1].startDate, "두 번째 예측: 4/27")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 5, 25)), cycle.predictDays[2].startDate, "세 번째 예측: 5/25")

        // 배란기: 3/2 기준 3/14~16, 4/11~13, 5/9~11
        assertEquals(3, cycle.ovulationDays.size, "배란기 3개")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 3, 14)), cycle.ovulationDays[0].startDate, "첫 배란기 시작: 3/14")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 3, 16)), cycle.ovulationDays[0].endDate, "첫 배란기 종료: 3/16")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 4, 11)), cycle.ovulationDays[1].startDate, "두 번째 배란기 시작: 4/11")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 4, 13)), cycle.ovulationDays[1].endDate, "두 번째 배란기 종료: 4/13")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 5, 9)), cycle.ovulationDays[2].startDate, "세 번째 배란기 시작: 5/9")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 5, 11)), cycle.ovulationDays[2].endDate, "세 번째 배란기 종료: 5/11")

        // 가임기: 3/9~20, 4/6~17, 5/4~15
        assertEquals(3, cycle.fertileDays.size, "가임기 3개")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 3, 9)), cycle.fertileDays[0].startDate, "첫 가임기 시작: 3/9")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 3, 20)), cycle.fertileDays[0].endDate, "첫 가임기 종료: 3/20")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 4, 6)), cycle.fertileDays[1].startDate, "두 번째 가임기 시작: 4/6")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 4, 17)), cycle.fertileDays[1].endDate, "두 번째 가임기 종료: 4/17")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 5, 4)), cycle.fertileDays[2].startDate, "세 번째 가임기 시작: 5/4")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 5, 15)), cycle.fertileDays[2].endDate, "세 번째 가임기 종료: 5/15")
    }

    @Test
    fun test07_rangeBetweenPeriods() = runTest {
        // 시나리오: 생리 기록 3개, 검색 범위가 생리 기록 사이
        val repository = InMemoryPeriodRepository().apply {
            addPeriod(PeriodRecord(pk = "1", startDate = DateUtils.toJulianDay(LocalDate(2024, 1, 5)), endDate = DateUtils.toJulianDay(LocalDate(2024, 1, 10))))  // 6일
            addPeriod(PeriodRecord(pk = "2", startDate = DateUtils.toJulianDay(LocalDate(2024, 2, 3)), endDate = DateUtils.toJulianDay(LocalDate(2024, 2, 8))))   // 6일
            addPeriod(PeriodRecord(pk = "3", startDate = DateUtils.toJulianDay(LocalDate(2024, 3, 2)), endDate = DateUtils.toJulianDay(LocalDate(2024, 3, 7))))   // 6일
            setPeriodSettings(
                PeriodSettings(
                    manualAverageCycle = 28,
                    manualAverageDay = 5,
                    autoAverageCycle = 29,  // 실제 평균: (1/5→2/3=29일, 2/3→3/2=28일) → 29일
                    autoAverageDay = 6,     // 실제 평균: 6일
                    isAutoCalc = false
                )
            )
        }

        val searchFrom = LocalDate(2024, 2, 10) // 두 번째 생리 종료 직후
        val searchTo = LocalDate(2024, 2, 29) // 세 번째 생리 시작 전
        val today = LocalDate(2024, 3, 10)      // 3월 중순 (과거 데이터 조회)

        val result = PeriodCalculator.calculateCycleInfo(
            repository,
            DateUtils.toJulianDay(searchFrom),
            DateUtils.toJulianDay(searchTo),
            DateUtils.toJulianDay(today)
        )

        // 검증
        // 검색 범위(2/10~2/29)는 2/3 생리와 3/2 생리 사이
        // 2/3 생리의 배란기/가임기가 2월 중순에 있으므로 pk=2도 반환됨
        assertEquals(2, result.size, "주기 정보 2개")

        // pk=2 (2/3 생리): 배란기/가임기가 범위 내
        val cycle2 = result.find { it.pk == "2" }!!
        assertEquals("2", cycle2.pk, "pk=2 (두 번째 생리)")
        assertEquals(28, cycle2.period, "주기 28일 (2/3→3/2 = 28일)")

        // 배란기: 2/3 + 12~14일 = 2/15~17
        assertEquals(1, cycle2.ovulationDays.size, "배란기 1개")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 2, 15)), cycle2.ovulationDays[0].startDate, "배란기 시작: 2/15")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 2, 17)), cycle2.ovulationDays[0].endDate, "배란기 종료: 2/17")

        // 가임기: 2/3 + 7~18일 = 2/10~21
        assertEquals(1, cycle2.fertileDays.size, "가임기 1개")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 2, 10)), cycle2.fertileDays[0].startDate, "가임기 시작: 2/10")
        assertEquals(DateUtils.toJulianDay(LocalDate(2024, 2, 21)), cycle2.fertileDays[0].endDate, "가임기 종료: 2/21")

        // 예측일은 3/2 (다음 생리)이므로 범위 밖
        assertEquals(0, cycle2.predictDays.size, "예측 생리일 없음 (범위 밖)")

        // pk=3 (3/2 생리): 배란기/가임기 모두 3월 이후라 범위 밖
        val cycle3 = result.find { it.pk == "3" }!!
        assertEquals("3", cycle3.pk, "pk=3 (세 번째 생리)")
        assertEquals(0, cycle3.ovulationDays.size, "배란기 없음 (범위 밖)")
        assertEquals(0, cycle3.fertileDays.size, "가임기 없음 (범위 밖)")
        assertEquals(0, cycle3.predictDays.size, "예측 생리일 없음 (범위 밖)")
    }

    @Test
    fun test08_rangeBeforePeriods() = runTest {
        // 시나리오: 생리 기록 3개, 검색 범위가 모든 생리 기록 이전
        val repository = InMemoryPeriodRepository().apply {
            addPeriod(PeriodRecord(pk = "1", startDate = DateUtils.toJulianDay(LocalDate(2024, 2, 5)), endDate = DateUtils.toJulianDay(LocalDate(2024, 2, 10))))   // 6일
            addPeriod(PeriodRecord(pk = "2", startDate = DateUtils.toJulianDay(LocalDate(2024, 3, 3)), endDate = DateUtils.toJulianDay(LocalDate(2024, 3, 8))))    // 6일
            addPeriod(PeriodRecord(pk = "3", startDate = DateUtils.toJulianDay(LocalDate(2024, 3, 31)), endDate = DateUtils.toJulianDay(LocalDate(2024, 4, 5))))  // 6일
            setPeriodSettings(
                PeriodSettings(
                    manualAverageCycle = 28,
                    manualAverageDay = 5,
                    autoAverageCycle = 28,  // 실제 평균: (2/5→3/3=27일, 3/3→3/31=28일) → 28일
                    autoAverageDay = 6,     // 실제 평균: 6일
                    isAutoCalc = false
                )
            )
        }

        val searchFrom = LocalDate(2024, 1, 1)
        val searchTo = LocalDate(2024, 2, 4) // 첫 번째 생리 시작 전날까지
        val today = LocalDate(2024, 4, 10)      // 4월 중순 (과거 데이터 조회)

        val result = PeriodCalculator.calculateCycleInfo(
            repository,
            DateUtils.toJulianDay(searchFrom),
            DateUtils.toJulianDay(searchTo),
            DateUtils.toJulianDay(today)
        )

        // 검증: 결과가 없거나, 있다면 실제 생리일이 범위 밖에 있어야 함
        if (result.isNotEmpty()) {
            result.forEach { cycle ->
                if (cycle.actualPeriod != null) {
                    val rangeEnd = DateUtils.toJulianDay(searchTo)
                    assertTrue(
                        cycle.actualPeriod!!.startDate > rangeEnd,
                        "실제 생리일이 검색 범위 밖"
                    )
                }
            }
        }
    }
}
