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
 * 짧은 주기 (25일) 테스트 케이스
 *
 * 테스트 문서: test-cases/docs/02-short-cycle.md
 *
 * 공통 입력 조건:
 * - 생리 기록:
 *   - Period 1: 2025-01-01 ~ 2025-01-05
 *   - Period 2: 2025-01-26 ~ 2025-01-30 (25일 주기)
 *   - Period 3: 2025-02-20 ~ 2025-02-24 (25일 주기)
 * - 배란 테스트 기록: 없음
 * - 배란일 직접 입력: 없음
 * - 피임약 패키지 시작일: 없음
 * - 오늘 날짜: 2025-03-15 (지연 테스트 제외)
 * - 생리 주기 설정: 평균 25일, 평균 기간 5일, 자동 계산 사용: false
 * - 피임약 설정: 사용 안함
 *
 * **주요 차이점**: 25일 주기 (26일 미만)
 * - 배란기: 생리 시작 9-11일차 (period - 16 ~ period - 14)
 * - 가임기: 생리 시작 6-14일차 (period - 19 ~ period - 11)
 */
class ShortCycleTest {

    /**
     * 공통 Repository 생성 함수
     */
    private fun createRepository(): InMemoryPeriodRepository {
        return InMemoryPeriodRepository().apply {
            // Period 1: 2025-01-01 ~ 2025-01-05
            addPeriod(
                PeriodRecord(
                    pk = "1",
                    startDate = DateUtils.toJulianDay(LocalDate(2025, 1, 1)),
                    endDate = DateUtils.toJulianDay(LocalDate(2025, 1, 5))
                )
            )
            // Period 2: 2025-01-26 ~ 2025-01-30 (25일 주기)
            addPeriod(
                PeriodRecord(
                    pk = "2",
                    startDate = DateUtils.toJulianDay(LocalDate(2025, 1, 26)),
                    endDate = DateUtils.toJulianDay(LocalDate(2025, 1, 30))
                )
            )
            // Period 3: 2025-02-20 ~ 2025-02-24 (25일 주기)
            addPeriod(
                PeriodRecord(
                    pk = "3",
                    startDate = DateUtils.toJulianDay(LocalDate(2025, 2, 20)),
                    endDate = DateUtils.toJulianDay(LocalDate(2025, 2, 24))
                )
            )

            setPeriodSettings(
                PeriodSettings(
                    manualAverageCycle = 25,
                    manualAverageDay = 5,
                    autoAverageCycle = 25,
                    autoAverageDay = 5,
                    isAutoCalc = false
                )
            )
        }
    }

    // ====================================================================================
    // 그룹 1: 미래 예측 중심 (마지막 생리 이후 - Period 3)
    // ====================================================================================

    /**
     * TC-02-01: 1일 조회 (미래)
     *
     * 테스트 목적: Period 3 이후 특정 날짜 1일만 조회할 때 해당 날짜의 상태가 정확한지 검증
     *
     * 조회 기간: 2025-03-15 ~ 2025-03-15 (1일)
     *
     * 예상 결과:
     * - 실제 생리 기록: 2025-02-20 ~ 2025-02-24 (pk=3, 마지막 생리)
     * - 생리 예정일: 없음 (조회 기간 내 생리 예정일 없음, 다음 예정일은 2025-03-17)
     * - 생리 지연일: 없음
     * - 배란기: 없음 (배란기는 2025-03-01 ~ 2025-03-03, 조회일 이전)
     * - 가임기: 없음 (가임기는 2025-02-26 ~ 2025-03-06, 조회일 이전)
     * - 주기: 25일
     */
    @Test
    fun testTC_02_01_singleDayQuery() = runTest {
        val repository = createRepository()

        val searchFrom = LocalDate(2025, 3, 15)
        val searchTo = LocalDate(2025, 3, 15)
        val today = LocalDate(2025, 3, 15)

        val result = PeriodCalculator.calculateCycleInfo(
            repository,
            DateUtils.toJulianDay(searchFrom),
            DateUtils.toJulianDay(searchTo),
            DateUtils.toJulianDay(today)
        )

        // 검증
        assertEquals(1, result.size, "주기 개수: 1개")

        val cycle = result[0]

        // 실제 생리 기록 검증
        assertEquals("3", cycle.pk, "pk=3 (마지막 생리)")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 20)),
            cycle.actualPeriod?.startDate,
            "실제 생리 시작: 2025-02-20"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 24)),
            cycle.actualPeriod?.endDate,
            "실제 생리 종료: 2025-02-24"
        )

        // 생리 예정일 검증
        assertEquals(0, cycle.predictDays.size, "생리 예정일 없음 (조회 기간 내 없음)")

        // 생리 지연일 검증
        assertEquals(0, cycle.delayTheDays, "생리 지연일: 0일")

        // 배란기 검증
        assertEquals(0, cycle.ovulationDays.size, "배란기 없음 (조회 기간 내 없음)")

        // 가임기 검증
        assertEquals(0, cycle.fertileDays.size, "가임기 없음 (조회 기간 내 없음)")

        // 주기 검증
        assertEquals(25, cycle.period, "주기: 25일")
    }

    /**
     * TC-02-02: 1주일 조회 (미래)
     *
     * 테스트 목적: Period 3 이후 1주일 기간 조회 시 25일 주기의 상태 변화가 정확히 표시되는지 검증
     *
     * 조회 기간: 2025-03-10 ~ 2025-03-16 (7일)
     *
     * 예상 결과:
     * - 실제 생리 기록: 2025-02-20 ~ 2025-02-24 (pk=3, 마지막 생리)
     * - 생리 예정일: 없음 (다음 예정일은 2025-03-17부터, 조회 범위 밖)
     * - 생리 지연일: 없음
     * - 배란기: 없음 (배란기는 2025-03-01 ~ 2025-03-03, 조회 범위 밖)
     * - 가임기: 없음 (가임기는 2025-02-26 ~ 2025-03-06, 조회 범위 밖)
     * - 주기: 25일
     */
    @Test
    fun testTC_02_02_oneWeekQuery() = runTest {
        val repository = createRepository()

        val searchFrom = LocalDate(2025, 3, 10)
        val searchTo = LocalDate(2025, 3, 16)
        val today = LocalDate(2025, 3, 15)

        val result = PeriodCalculator.calculateCycleInfo(
            repository,
            DateUtils.toJulianDay(searchFrom),
            DateUtils.toJulianDay(searchTo),
            DateUtils.toJulianDay(today)
        )

        // 검증
        assertEquals(1, result.size, "주기 개수: 1개")

        val cycle = result[0]

        // 실제 생리 기록 검증
        assertEquals("3", cycle.pk, "pk=3 (마지막 생리)")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 20)),
            cycle.actualPeriod?.startDate,
            "실제 생리 시작: 2025-02-20"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 24)),
            cycle.actualPeriod?.endDate,
            "실제 생리 종료: 2025-02-24"
        )

        // 생리 예정일 검증 (조회 범위 밖)
        assertEquals(0, cycle.predictDays.size, "생리 예정일 없음 (조회 범위 밖)")

        // 생리 지연일 검증
        assertEquals(0, cycle.delayTheDays, "생리 지연일: 0일")

        // 배란기 검증 (조회 범위 밖)
        assertEquals(0, cycle.ovulationDays.size, "배란기 없음 (조회 범위 밖)")

        // 가임기 검증 (조회 범위 밖)
        assertEquals(0, cycle.fertileDays.size, "가임기 없음 (조회 범위 밖)")

        // 주기 검증
        assertEquals(25, cycle.period, "주기: 25일")
    }

    /**
     * TC-02-03: 1개월 조회 (미래)
     *
     * 테스트 목적: 마지막 생리(Period 3) 전체 주기를 조회하여 실제 생리, 배란기, 가임기가 모두 정확히 표시되는지 검증
     *
     * 조회 기간: 2025-02-20 ~ 2025-03-16 (Period 3 전체 주기, 25일)
     *
     * 예상 결과:
     * - 실제 생리 기록: 2025-02-20 ~ 2025-02-24 (pk=3, 마지막 생리)
     * - 생리 예정일: 없음 (다음 예정일 2025-03-17은 조회 범위 밖)
     * - 생리 지연일: 없음
     * - 배란기: 2025-03-01 ~ 2025-03-03 (생리 시작 10-12일차, period-16 ~ period-14)
     * - 가임기: 2025-02-26 ~ 2025-03-06 (생리 시작 7-15일차, period-19 ~ period-11)
     * - 주기: 25일
     */
    @Test
    fun testTC_02_03_oneMonthQuery() = runTest {
        val repository = createRepository()

        val searchFrom = LocalDate(2025, 2, 20)
        val searchTo = LocalDate(2025, 3, 16)
        val today = LocalDate(2025, 3, 15)

        val result = PeriodCalculator.calculateCycleInfo(
            repository,
            DateUtils.toJulianDay(searchFrom),
            DateUtils.toJulianDay(searchTo),
            DateUtils.toJulianDay(today)
        )

        // 검증
        assertEquals(1, result.size, "주기 개수: 1개")

        val cycle = result[0]

        // 실제 생리 기록 검증
        assertEquals("3", cycle.pk, "pk=3 (마지막 생리)")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 20)),
            cycle.actualPeriod?.startDate,
            "실제 생리 시작: 2025-02-20"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 24)),
            cycle.actualPeriod?.endDate,
            "실제 생리 종료: 2025-02-24"
        )

        // 생리 예정일 검증
        assertEquals(0, cycle.predictDays.size, "생리 예정일 없음 (조회 범위 밖)")

        // 생리 지연일 검증
        assertEquals(0, cycle.delayTheDays, "생리 지연일: 0일")

        // 배란기 검증 (인덱스 9-11)
        assertEquals(1, cycle.ovulationDays.size, "배란기 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 1)),
            cycle.ovulationDays[0]?.startDate,
            "배란기 시작: 2025-03-01 (Period 3 시작 + 9)"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 3)),
            cycle.ovulationDays[0]?.endDate,
            "배란기 종료: 2025-03-03"
        )

        // 가임기 검증 (인덱스 6-14)
        assertEquals(1, cycle.fertileDays.size, "가임기 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 26)),
            cycle.fertileDays[0]?.startDate,
            "가임기 시작: 2025-02-26 (Period 3 시작 + 6)"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 6)),
            cycle.fertileDays[0]?.endDate,
            "가임기 종료: 2025-03-06"
        )

        // 주기 검증
        assertEquals(25, cycle.period, "주기: 25일")
    }

    // ====================================================================================
    // 그룹 2: 과거 기록 중심 (생리 주기 사이 - Period 2)
    // ====================================================================================

    /**
     * TC-02-04: 1일 조회 (과거)
     *
     * 테스트 목적: Period 2의 배란기 중 특정 날짜 1일만 조회할 때 해당 날짜의 상태가 정확한지 검증
     *
     * 조회 기간: 2025-02-04 ~ 2025-02-04 (1일, Period 2의 배란기)
     *
     * 예상 결과: 주기 개수 2개
     *
     * 주기 1 (pk=2):
     * - 실제 생리 기록: 2025-01-26 ~ 2025-01-30 (두 번째 생리)
     * - 생리 예정일: 없음 (Period 3는 실제 기록)
     * - 생리 지연일: 없음
     * - 배란기: 2025-02-04 ~ 2025-02-06 (조회일 포함)
     * - 가임기: 2025-02-01 ~ 2025-02-09 (조회일 포함)
     * - 주기: 25일
     *
     * 주기 2 (pk=3):
     * - 실제 생리 기록: 2025-02-20 ~ 2025-02-24 (세 번째 생리)
     * - 생리 예정일: 없음
     * - 생리 지연일: 없음
     * - 배란기: 없음 (조회 범위 밖)
     * - 가임기: 없음 (조회 범위 밖)
     * - 주기: 25일
     */
    @Test
    fun testTC_02_04_singleDayQueryPast() = runTest {
        val repository = createRepository()

        val searchFrom = LocalDate(2025, 2, 4)
        val searchTo = LocalDate(2025, 2, 4)
        val today = LocalDate(2025, 3, 15)

        val result = PeriodCalculator.calculateCycleInfo(
            repository,
            DateUtils.toJulianDay(searchFrom),
            DateUtils.toJulianDay(searchTo),
            DateUtils.toJulianDay(today)
        )

        // 검증
        assertEquals(2, result.size, "주기 개수: 2개")

        // pk=2 주기 (Period 2): 배란기/가임기가 조회일 포함
        val cycle2 = result.find { it.pk == "2" }!!

        assertEquals("2", cycle2.pk, "pk=2 (두 번째 생리)")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 1, 26)),
            cycle2.actualPeriod?.startDate,
            "실제 생리 시작: 2025-01-26"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 1, 30)),
            cycle2.actualPeriod?.endDate,
            "실제 생리 종료: 2025-01-30"
        )

        assertEquals(0, cycle2.predictDays.size, "생리 예정일 없음 (Period 3는 실제 기록)")
        assertEquals(0, cycle2.delayTheDays, "생리 지연일: 0일")

        // 배란기 검증 (인덱스 9-11)
        assertEquals(1, cycle2.ovulationDays.size, "배란기 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 4)),
            cycle2.ovulationDays[0]?.startDate,
            "배란기 시작: 2025-02-04 (Period 2 시작 + 9)"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 6)),
            cycle2.ovulationDays[0]?.endDate,
            "배란기 종료: 2025-02-06"
        )

        // 가임기 검증 (인덱스 6-14)
        assertEquals(1, cycle2.fertileDays.size, "가임기 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 1)),
            cycle2.fertileDays[0]?.startDate,
            "가임기 시작: 2025-02-01 (Period 2 시작 + 6)"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 9)),
            cycle2.fertileDays[0]?.endDate,
            "가임기 종료: 2025-02-09"
        )

        assertEquals(25, cycle2.period, "주기: 25일")

        // pk=3 주기 (Period 3): 배란기/가임기 모두 조회 범위 밖
        val cycle3 = result.find { it.pk == "3" }!!

        assertEquals("3", cycle3.pk, "pk=3 (세 번째 생리)")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 20)),
            cycle3.actualPeriod?.startDate,
            "실제 생리 시작: 2025-02-20"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 24)),
            cycle3.actualPeriod?.endDate,
            "실제 생리 종료: 2025-02-24"
        )

        assertEquals(0, cycle3.predictDays.size, "생리 예정일 없음")
        assertEquals(0, cycle3.delayTheDays, "생리 지연일: 0일")
        assertEquals(0, cycle3.ovulationDays.size, "배란기 없음 (조회 범위 밖)")
        assertEquals(0, cycle3.fertileDays.size, "가임기 없음 (조회 범위 밖)")
        assertEquals(25, cycle3.period, "주기: 25일")
    }

    /**
     * TC-02-05: 1주일 조회 (과거)
     *
     * 테스트 목적: Period 2의 배란기/가임기가 포함된 1주일 기간 조회 시 해당 주의 상태 변화가 정확히 표시되는지 검증
     *
     * 조회 기간: 2025-02-02 ~ 2025-02-08 (7일, Period 2의 배란기/가임기 포함)
     *
     * 예상 결과: 주기 개수 2개
     *
     * 주기 1 (pk=2):
     * - 실제 생리 기록: 2025-01-26 ~ 2025-01-30 (두 번째 생리)
     * - 생리 예정일: 없음 (Period 3는 실제 기록)
     * - 생리 지연일: 없음
     * - 배란기: 2025-02-04 ~ 2025-02-06 (조회 범위와 부분 겹침)
     * - 가임기: 2025-02-01 ~ 2025-02-09 (조회 범위와 부분 겹침)
     * - 주기: 25일
     *
     * 주기 2 (pk=3):
     * - 실제 생리 기록: 2025-02-20 ~ 2025-02-24 (세 번째 생리)
     * - 생리 예정일: 없음
     * - 생리 지연일: 없음
     * - 배란기: 없음 (조회 범위 밖)
     * - 가임기: 없음 (조회 범위 밖)
     * - 주기: 25일
     */
    @Test
    fun testTC_02_05_oneWeekQueryPast() = runTest {
        val repository = createRepository()

        val searchFrom = LocalDate(2025, 2, 2)
        val searchTo = LocalDate(2025, 2, 8)
        val today = LocalDate(2025, 3, 15)

        val result = PeriodCalculator.calculateCycleInfo(
            repository,
            DateUtils.toJulianDay(searchFrom),
            DateUtils.toJulianDay(searchTo),
            DateUtils.toJulianDay(today)
        )

        // 검증
        assertEquals(2, result.size, "주기 개수: 2개")

        // pk=2 주기 (Period 2): 배란기/가임기가 조회 범위 내
        val cycle2 = result.find { it.pk == "2" }!!

        assertEquals("2", cycle2.pk, "pk=2 (두 번째 생리)")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 1, 26)),
            cycle2.actualPeriod?.startDate,
            "실제 생리 시작: 2025-01-26"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 1, 30)),
            cycle2.actualPeriod?.endDate,
            "실제 생리 종료: 2025-01-30"
        )

        assertEquals(0, cycle2.predictDays.size, "생리 예정일 없음 (Period 3는 실제 기록)")
        assertEquals(0, cycle2.delayTheDays, "생리 지연일: 0일")

        // 배란기 검증 (인덱스 9-11)
        assertEquals(1, cycle2.ovulationDays.size, "배란기 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 4)),
            cycle2.ovulationDays[0]?.startDate,
            "배란기 시작: 2025-02-04 (Period 2 시작 + 9)"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 6)),
            cycle2.ovulationDays[0]?.endDate,
            "배란기 종료: 2025-02-06"
        )

        // 가임기 검증 (인덱스 6-14)
        assertEquals(1, cycle2.fertileDays.size, "가임기 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 1)),
            cycle2.fertileDays[0]?.startDate,
            "가임기 시작: 2025-02-01 (Period 2 시작 + 6)"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 9)),
            cycle2.fertileDays[0]?.endDate,
            "가임기 종료: 2025-02-09"
        )

        assertEquals(25, cycle2.period, "주기: 25일")

        // pk=3 주기 (Period 3): 배란기/가임기 모두 조회 범위 밖
        val cycle3 = result.find { it.pk == "3" }!!

        assertEquals("3", cycle3.pk, "pk=3 (세 번째 생리)")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 20)),
            cycle3.actualPeriod?.startDate,
            "실제 생리 시작: 2025-02-20"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 24)),
            cycle3.actualPeriod?.endDate,
            "실제 생리 종료: 2025-02-24"
        )

        assertEquals(0, cycle3.predictDays.size, "생리 예정일 없음")
        assertEquals(0, cycle3.delayTheDays, "생리 지연일: 0일")
        assertEquals(0, cycle3.ovulationDays.size, "배란기 없음 (조회 범위 밖)")
        assertEquals(0, cycle3.fertileDays.size, "가임기 없음 (조회 범위 밖)")
        assertEquals(25, cycle3.period, "주기: 25일")
    }

    /**
     * TC-02-06: 1개월 조회 (과거)
     *
     * 테스트 목적: Period 2 전체 주기를 조회하여 실제 생리, 배란기, 가임기가 모두 정확히 표시되는지 검증
     *
     * 조회 기간: 2025-01-26 ~ 2025-02-19 (Period 2 전체 주기, 25일)
     *
     * 예상 결과: 주기 개수 2개
     *
     * 주기 1 (pk=2):
     * - 실제 생리 기록: 2025-01-26 ~ 2025-01-30 (두 번째 생리)
     * - 생리 예정일: 없음 (Period 3는 실제 기록)
     * - 생리 지연일: 없음
     * - 배란기: 2025-02-04 ~ 2025-02-06 (생리 시작 10-12일차)
     * - 가임기: 2025-02-01 ~ 2025-02-09 (생리 시작 7-15일차)
     * - 주기: 25일
     *
     * 주기 2 (pk=3):
     * - 실제 생리 기록: 2025-02-20 ~ 2025-02-24 (세 번째 생리)
     * - 생리 예정일: 없음
     * - 생리 지연일: 없음
     * - 배란기: 없음 (조회 범위 밖)
     * - 가임기: 없음 (조회 범위 밖)
     * - 주기: 25일
     */
    @Test
    fun testTC_02_06_oneMonthQueryPast() = runTest {
        val repository = createRepository()

        val searchFrom = LocalDate(2025, 1, 26)
        val searchTo = LocalDate(2025, 2, 19)
        val today = LocalDate(2025, 3, 15)

        val result = PeriodCalculator.calculateCycleInfo(
            repository,
            DateUtils.toJulianDay(searchFrom),
            DateUtils.toJulianDay(searchTo),
            DateUtils.toJulianDay(today)
        )

        // 검증
        assertEquals(2, result.size, "주기 개수: 2개")

        // pk=2 주기 (Period 2): 전체 주기 포함
        val cycle2 = result.find { it.pk == "2" }!!

        assertEquals("2", cycle2.pk, "pk=2 (두 번째 생리)")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 1, 26)),
            cycle2.actualPeriod?.startDate,
            "실제 생리 시작: 2025-01-26"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 1, 30)),
            cycle2.actualPeriod?.endDate,
            "실제 생리 종료: 2025-01-30"
        )

        assertEquals(0, cycle2.predictDays.size, "생리 예정일 없음 (Period 3는 실제 기록)")
        assertEquals(0, cycle2.delayTheDays, "생리 지연일: 0일")

        // 배란기 검증 (인덱스 9-11)
        assertEquals(1, cycle2.ovulationDays.size, "배란기 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 4)),
            cycle2.ovulationDays[0]?.startDate,
            "배란기 시작: 2025-02-04 (Period 2 시작 + 9)"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 6)),
            cycle2.ovulationDays[0]?.endDate,
            "배란기 종료: 2025-02-06"
        )

        // 가임기 검증 (인덱스 6-14)
        assertEquals(1, cycle2.fertileDays.size, "가임기 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 1)),
            cycle2.fertileDays[0]?.startDate,
            "가임기 시작: 2025-02-01 (Period 2 시작 + 6)"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 9)),
            cycle2.fertileDays[0]?.endDate,
            "가임기 종료: 2025-02-09"
        )

        assertEquals(25, cycle2.period, "주기: 25일")

        // pk=3 주기 (Period 3): 다음 생리 정보
        val cycle3 = result.find { it.pk == "3" }!!

        assertEquals("3", cycle3.pk, "pk=3 (세 번째 생리)")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 20)),
            cycle3.actualPeriod?.startDate,
            "실제 생리 시작: 2025-02-20"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 24)),
            cycle3.actualPeriod?.endDate,
            "실제 생리 종료: 2025-02-24"
        )

        assertEquals(0, cycle3.predictDays.size, "생리 예정일 없음")
        assertEquals(0, cycle3.delayTheDays, "생리 지연일: 0일")
        assertEquals(0, cycle3.ovulationDays.size, "배란기 없음 (조회 범위 밖)")
        assertEquals(0, cycle3.fertileDays.size, "가임기 없음 (조회 범위 밖)")
        assertEquals(25, cycle3.period, "주기: 25일")
    }

    // ====================================================================================
    // 그룹 3: 장기 조회 및 특수 경계
    // ====================================================================================

    /**
     * TC-02-07: 생리 기간 경계 조회
     *
     * 테스트 목적: 조회 기간이 생리 종료일과 다음 생리 시작일에 걸쳐있을 때 두 주기가 모두 반환되는지 검증
     *
     * 조회 기간: 2025-02-18 ~ 2025-02-22 (Period 2 종료 후 ~ Period 3 진행 중)
     *
     * 예상 결과: 주기 개수 2개
     *
     * 주기 1 (pk=2):
     * - 실제 생리 기록: 2025-01-26 ~ 2025-01-30 (두 번째 생리)
     * - 생리 예정일: 없음 (Period 3는 실제 기록)
     * - 생리 지연일: 없음
     * - 배란기: 없음 (조회 범위 밖)
     * - 가임기: 없음 (조회 범위 밖)
     * - 주기: 25일
     *
     * 주기 2 (pk=3):
     * - 실제 생리 기록: 2025-02-20 ~ 2025-02-24 (세 번째 생리, 조회 범위와 부분 겹침)
     * - 생리 예정일: 없음
     * - 생리 지연일: 없음
     * - 배란기: 없음 (조회 범위 밖)
     * - 가임기: 없음 (조회 범위 밖)
     * - 주기: 25일
     */
    @Test
    fun testTC_02_07_periodBoundaryQuery() = runTest {
        val repository = createRepository()

        val searchFrom = LocalDate(2025, 2, 18)
        val searchTo = LocalDate(2025, 2, 22)
        val today = LocalDate(2025, 3, 15)

        val result = PeriodCalculator.calculateCycleInfo(
            repository,
            DateUtils.toJulianDay(searchFrom),
            DateUtils.toJulianDay(searchTo),
            DateUtils.toJulianDay(today)
        )

        // 검증
        assertEquals(2, result.size, "주기 개수: 2개")

        // pk=2 주기 (Period 2): 생리 종료 후, 조회 범위 밖
        val cycle2 = result.find { it.pk == "2" }!!

        assertEquals("2", cycle2.pk, "pk=2 (두 번째 생리)")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 1, 26)),
            cycle2.actualPeriod?.startDate,
            "실제 생리 시작: 2025-01-26"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 1, 30)),
            cycle2.actualPeriod?.endDate,
            "실제 생리 종료: 2025-01-30"
        )

        assertEquals(0, cycle2.predictDays.size, "생리 예정일 없음 (Period 3는 실제 기록)")
        assertEquals(0, cycle2.delayTheDays, "생리 지연일: 0일")
        assertEquals(0, cycle2.ovulationDays.size, "배란기 없음 (조회 범위 밖)")
        assertEquals(0, cycle2.fertileDays.size, "가임기 없음 (조회 범위 밖)")
        assertEquals(25, cycle2.period, "주기: 25일")

        // pk=3 주기 (Period 3): 생리 진행 중, 조회 범위와 부분 겹침
        val cycle3 = result.find { it.pk == "3" }!!

        assertEquals("3", cycle3.pk, "pk=3 (세 번째 생리)")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 20)),
            cycle3.actualPeriod?.startDate,
            "실제 생리 시작: 2025-02-20"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 24)),
            cycle3.actualPeriod?.endDate,
            "실제 생리 종료: 2025-02-24"
        )

        assertEquals(0, cycle3.predictDays.size, "생리 예정일 없음")
        assertEquals(0, cycle3.delayTheDays, "생리 지연일: 0일")
        assertEquals(0, cycle3.ovulationDays.size, "배란기 없음 (조회 범위 밖)")
        assertEquals(0, cycle3.fertileDays.size, "가임기 없음 (조회 범위 밖)")
        assertEquals(25, cycle3.period, "주기: 25일")
    }

    /**
     * TC-02-08: 3개월 조회
     *
     * 테스트 목적: 장기간 조회 시 25일 주기의 예측이 반복적으로 정확한지 검증
     *
     * 조회 기간: 2025-03-01 ~ 2025-05-31 (3개월)
     *
     * 예상 결과:
     * - 실제 생리 기록: 2025-02-20 ~ 2025-02-24 (pk=3, 마지막 생리)
     * - 생리 예정일들:
     *   - 2025-03-17 ~ 2025-03-21
     *   - 2025-04-11 ~ 2025-04-15
     *   - 2025-05-06 ~ 2025-05-10
     *   - 2025-05-31 ~ 2025-06-04 (부분 포함)
     * - 배란기들:
     *   - 2025-03-01 ~ 2025-03-03 (첫 번째 주기)
     *   - 2025-03-26 ~ 2025-03-28 (두 번째 주기)
     *   - 2025-04-20 ~ 2025-04-22 (세 번째 주기)
     *   - 2025-05-15 ~ 2025-05-17 (네 번째 주기)
     * - 가임기들:
     *   - 2025-02-26 ~ 2025-03-06 (부분 포함)
     *   - 2025-03-23 ~ 2025-03-31
     *   - 2025-04-17 ~ 2025-04-25
     *   - 2025-05-12 ~ 2025-05-20
     * - 주기: 25일
     */
    @Test
    fun testTC_02_08_threeMonthQuery() = runTest {
        val repository = createRepository()

        val searchFrom = LocalDate(2025, 3, 1)
        val searchTo = LocalDate(2025, 5, 31)
        val today = LocalDate(2025, 3, 15)

        val result = PeriodCalculator.calculateCycleInfo(
            repository,
            DateUtils.toJulianDay(searchFrom),
            DateUtils.toJulianDay(searchTo),
            DateUtils.toJulianDay(today)
        )

        // 검증
        assertEquals(1, result.size, "주기 개수: 1개")

        val cycle = result[0]

        // 실제 생리 기록 검증
        assertEquals("3", cycle.pk, "pk=3 (마지막 생리)")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 20)),
            cycle.actualPeriod?.startDate,
            "실제 생리 시작: 2025-02-20"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 24)),
            cycle.actualPeriod?.endDate,
            "실제 생리 종료: 2025-02-24"
        )

        // 생리 예정일 검증 (4개)
        assertEquals(4, cycle.predictDays.size, "생리 예정일 4개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 17)),
            cycle.predictDays[0].startDate,
            "첫 번째 예정일 시작: 2025-03-17"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 21)),
            cycle.predictDays[0].endDate,
            "첫 번째 예정일 종료: 2025-03-21"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 11)),
            cycle.predictDays[1].startDate,
            "두 번째 예정일 시작: 2025-04-11"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 15)),
            cycle.predictDays[1].endDate,
            "두 번째 예정일 종료: 2025-04-15"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 5, 6)),
            cycle.predictDays[2].startDate,
            "세 번째 예정일 시작: 2025-05-06"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 5, 10)),
            cycle.predictDays[2].endDate,
            "세 번째 예정일 종료: 2025-05-10"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 5, 31)),
            cycle.predictDays[3].startDate,
            "네 번째 예정일 시작: 2025-05-31 (부분 포함)"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 6, 4)),
            cycle.predictDays[3].endDate,
            "네 번째 예정일 종료: 2025-06-04"
        )

        // 배란기 검증 (4개)
        assertEquals(4, cycle.ovulationDays.size, "배란기 4개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 1)),
            cycle.ovulationDays[0]?.startDate,
            "첫 번째 배란기 시작: 2025-03-01"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 3)),
            cycle.ovulationDays[0]?.endDate,
            "첫 번째 배란기 종료: 2025-03-03"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 26)),
            cycle.ovulationDays[1]?.startDate,
            "두 번째 배란기 시작: 2025-03-26"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 28)),
            cycle.ovulationDays[1]?.endDate,
            "두 번째 배란기 종료: 2025-03-28"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 20)),
            cycle.ovulationDays[2]?.startDate,
            "세 번째 배란기 시작: 2025-04-20"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 22)),
            cycle.ovulationDays[2]?.endDate,
            "세 번째 배란기 종료: 2025-04-22"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 5, 15)),
            cycle.ovulationDays[3]?.startDate,
            "네 번째 배란기 시작: 2025-05-15"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 5, 17)),
            cycle.ovulationDays[3]?.endDate,
            "네 번째 배란기 종료: 2025-05-17"
        )

        // 가임기 검증 (4개)
        assertEquals(4, cycle.fertileDays.size, "가임기 4개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 26)),
            cycle.fertileDays[0]?.startDate,
            "첫 번째 가임기 시작: 2025-02-26 (조회 범위와 부분 겹침)"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 6)),
            cycle.fertileDays[0]?.endDate,
            "첫 번째 가임기 종료: 2025-03-06"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 23)),
            cycle.fertileDays[1]?.startDate,
            "두 번째 가임기 시작: 2025-03-23"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 31)),
            cycle.fertileDays[1]?.endDate,
            "두 번째 가임기 종료: 2025-03-31"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 17)),
            cycle.fertileDays[2]?.startDate,
            "세 번째 가임기 시작: 2025-04-17"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 25)),
            cycle.fertileDays[2]?.endDate,
            "세 번째 가임기 종료: 2025-04-25"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 5, 12)),
            cycle.fertileDays[3]?.startDate,
            "네 번째 가임기 시작: 2025-05-12"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 5, 20)),
            cycle.fertileDays[3]?.endDate,
            "네 번째 가임기 종료: 2025-05-20"
        )

        // 주기 검증
        assertEquals(25, cycle.period, "주기: 25일")
    }

    // ====================================================================================
    // 그룹 4: 특정 기간 타입 조회
    // ====================================================================================

    /**
     * TC-02-09: 생리 예정 기간 중 조회
     *
     * 테스트 목적: 조회 기간이 생리 예정 기간을 포함할 때 정확히 표시되는지 검증
     *
     * 조회 기간: 2025-03-17 ~ 2025-03-21
     *
     * 예상 결과:
     * - 실제 생리 기록: 2025-02-20 ~ 2025-02-24 (pk=3, 마지막 생리)
     * - 생리 예정일: 2025-03-17 ~ 2025-03-21
     * - 생리 지연일: 없음
     * - 배란기: 없음 (조회 기간 내 배란기 없음)
     * - 가임기: 없음 (조회 기간 내 가임기 없음)
     * - 주기: 25일
     */
    @Test
    fun testTC_02_09_duringPredictedPeriod() = runTest {
        val repository = createRepository()

        val searchFrom = LocalDate(2025, 3, 17)
        val searchTo = LocalDate(2025, 3, 21)
        val today = LocalDate(2025, 3, 15)

        val result = PeriodCalculator.calculateCycleInfo(
            repository,
            DateUtils.toJulianDay(searchFrom),
            DateUtils.toJulianDay(searchTo),
            DateUtils.toJulianDay(today)
        )

        // 검증
        assertEquals(1, result.size, "주기 개수: 1개")

        val cycle = result[0]

        // 실제 생리 기록 검증
        assertEquals("3", cycle.pk, "pk=3 (마지막 생리)")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 20)),
            cycle.actualPeriod?.startDate,
            "실제 생리 시작: 2025-02-20"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 24)),
            cycle.actualPeriod?.endDate,
            "실제 생리 종료: 2025-02-24"
        )

        // 생리 예정일 검증
        assertEquals(1, cycle.predictDays.size, "생리 예정일 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 17)),
            cycle.predictDays[0].startDate,
            "예정일 시작: 2025-03-17"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 21)),
            cycle.predictDays[0].endDate,
            "예정일 종료: 2025-03-21"
        )

        // 생리 지연일 검증
        assertEquals(0, cycle.delayTheDays, "생리 지연일: 0일")

        // 배란기 검증
        assertEquals(0, cycle.ovulationDays.size, "배란기 없음 (조회 기간 내 없음)")

        // 가임기 검증
        assertEquals(0, cycle.fertileDays.size, "가임기 없음 (조회 기간 내 없음)")

        // 주기 검증
        assertEquals(25, cycle.period, "주기: 25일")
    }

    /**
     * TC-02-10: 배란기 중 조회
     *
     * 테스트 목적: 조회 기간이 배란기를 포함할 때 정확히 표시되는지 검증 (25일 주기 공식 적용)
     *
     * 조회 기간: 2025-03-01 ~ 2025-03-03
     *
     * 예상 결과:
     * - 실제 생리 기록: 2025-02-20 ~ 2025-02-24 (pk=3, 마지막 생리)
     * - 생리 예정일: 없음 (조회 기간 내 생리 없음)
     * - 생리 지연일: 없음
     * - 배란기: 2025-03-01 ~ 2025-03-03
     * - 가임기: 2025-02-26 ~ 2025-03-06 (조회 범위와 부분 겹침)
     * - 주기: 25일
     */
    @Test
    fun testTC_02_10_duringOvulation() = runTest {
        val repository = createRepository()

        val searchFrom = LocalDate(2025, 3, 1)
        val searchTo = LocalDate(2025, 3, 3)
        val today = LocalDate(2025, 3, 15)

        val result = PeriodCalculator.calculateCycleInfo(
            repository,
            DateUtils.toJulianDay(searchFrom),
            DateUtils.toJulianDay(searchTo),
            DateUtils.toJulianDay(today)
        )

        // 검증
        assertEquals(1, result.size, "주기 개수: 1개")

        val cycle = result[0]

        // 실제 생리 기록 검증
        assertEquals("3", cycle.pk, "pk=3 (마지막 생리)")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 20)),
            cycle.actualPeriod?.startDate,
            "실제 생리 시작: 2025-02-20"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 24)),
            cycle.actualPeriod?.endDate,
            "실제 생리 종료: 2025-02-24"
        )

        // 생리 예정일 검증
        assertEquals(0, cycle.predictDays.size, "생리 예정일 없음 (조회 기간 내 없음)")

        // 생리 지연일 검증
        assertEquals(0, cycle.delayTheDays, "생리 지연일: 0일")

        // 배란기 검증 (인덱스 9-11)
        assertEquals(1, cycle.ovulationDays.size, "배란기 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 1)),
            cycle.ovulationDays[0]?.startDate,
            "배란기 시작: 2025-03-01"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 3)),
            cycle.ovulationDays[0]?.endDate,
            "배란기 종료: 2025-03-03"
        )

        // 가임기 검증 (조회 범위와 부분 겹침)
        assertEquals(1, cycle.fertileDays.size, "가임기 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 26)),
            cycle.fertileDays[0]?.startDate,
            "가임기 시작: 2025-02-26 (전체 범위 반환)"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 6)),
            cycle.fertileDays[0]?.endDate,
            "가임기 종료: 2025-03-06"
        )

        // 주기 검증
        assertEquals(25, cycle.period, "주기: 25일")
    }

    /**
     * TC-02-11: 가임기 중 조회
     *
     * 테스트 목적: 조회 기간이 가임기를 포함할 때 정확히 표시되는지 검증 (25일 주기 공식 적용)
     *
     * 조회 기간: 2025-02-25 ~ 2025-02-28
     *
     * 예상 결과:
     * - 실제 생리 기록: 2025-02-20 ~ 2025-02-24 (pk=3, 마지막 생리)
     * - 생리 예정일: 없음 (조회 기간 내 생리 없음)
     * - 생리 지연일: 없음
     * - 배란기: 없음 (배란기는 2025-03-01부터 시작)
     * - 가임기: 2025-02-26 ~ 2025-03-06 (조회 범위와 부분 겹침)
     * - 주기: 25일
     */
    @Test
    fun testTC_02_11_duringFertile() = runTest {
        val repository = createRepository()

        val searchFrom = LocalDate(2025, 2, 25)
        val searchTo = LocalDate(2025, 2, 28)
        val today = LocalDate(2025, 3, 15)

        val result = PeriodCalculator.calculateCycleInfo(
            repository,
            DateUtils.toJulianDay(searchFrom),
            DateUtils.toJulianDay(searchTo),
            DateUtils.toJulianDay(today)
        )

        // 검증
        assertEquals(1, result.size, "주기 개수: 1개")

        val cycle = result[0]

        // 실제 생리 기록 검증
        assertEquals("3", cycle.pk, "pk=3 (마지막 생리)")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 20)),
            cycle.actualPeriod?.startDate,
            "실제 생리 시작: 2025-02-20"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 24)),
            cycle.actualPeriod?.endDate,
            "실제 생리 종료: 2025-02-24"
        )

        // 생리 예정일 검증
        assertEquals(0, cycle.predictDays.size, "생리 예정일 없음 (조회 기간 내 없음)")

        // 생리 지연일 검증
        assertEquals(0, cycle.delayTheDays, "생리 지연일: 0일")

        // 배란기 검증 (조회 기간 내 없음)
        assertEquals(0, cycle.ovulationDays.size, "배란기 없음 (배란기는 03-01부터 시작)")

        // 가임기 검증 (조회 범위와 부분 겹침)
        assertEquals(1, cycle.fertileDays.size, "가임기 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 26)),
            cycle.fertileDays[0]?.startDate,
            "가임기 시작: 2025-02-26 (전체 범위 반환)"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 6)),
            cycle.fertileDays[0]?.endDate,
            "가임기 종료: 2025-03-06"
        )

        // 주기 검증
        assertEquals(25, cycle.period, "주기: 25일")
    }

    // ====================================================================================
    // 그룹 5: 생리 지연 케이스
    // ====================================================================================

    /**
     * TC-02-12: 생리 지연 1-7일 (예정일 뒤로 미룸)
     *
     * 테스트 목적: 생리 예정일이 지나고 1-7일 지연되었을 때 지연 기간이 표시되고,
     *             다음 예정일이 지연 다음날부터 시작하는지 검증 (25일 주기)
     *
     * 조회 기간: 2025-03-10 ~ 2025-03-31
     * 오늘 날짜: 2025-03-23 (생리 예정일 03-17로부터 7일 지연)
     *
     * 예상 결과:
     * - 실제 생리 기록: 2025-02-20 ~ 2025-02-24 (pk=3, 마지막 생리)
     * - 생리 지연일: 7일
     * - 지연 기간: 2025-03-17 ~ 2025-03-23 (예정일부터 오늘까지)
     * - 생리 예정일: 2025-03-24 ~ 2025-03-28 (지연 다음날부터 5일간)
     * - 배란기: 없음 (조회 범위 밖)
     * - 가임기: 없음 (조회 범위 밖)
     * - 주기: 25일
     */
    @Test
    fun testTC_02_12_delayOneToSevenDays() = runTest {
        val repository = createRepository()

        val searchFrom = LocalDate(2025, 3, 10)
        val searchTo = LocalDate(2025, 3, 31)
        val today = LocalDate(2025, 3, 23) // 7일 지연

        val result = PeriodCalculator.calculateCycleInfo(
            repository,
            DateUtils.toJulianDay(searchFrom),
            DateUtils.toJulianDay(searchTo),
            DateUtils.toJulianDay(today)
        )

        // 검증
        assertEquals(1, result.size, "주기 개수: 1개")

        val cycle = result[0]

        // 실제 생리 기록 검증
        assertEquals("3", cycle.pk, "pk=3 (마지막 생리)")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 20)),
            cycle.actualPeriod?.startDate,
            "실제 생리 시작: 2025-02-20"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 24)),
            cycle.actualPeriod?.endDate,
            "실제 생리 종료: 2025-02-24"
        )

        // 생리 지연일 검증
        assertEquals(7, cycle.delayTheDays, "생리 지연일: 7일")

        // 지연 기간 검증 (2025-03-17 ~ 2025-03-23)
        assertTrue(cycle.delayDay != null, "지연 기간 있음")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 17)),
            cycle.delayDay?.startDate,
            "지연 시작: 2025-03-17"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 23)),
            cycle.delayDay?.endDate,
            "지연 종료: 2025-03-23"
        )

        // 생리 예정일 검증 (지연 다음날부터)
        assertEquals(1, cycle.predictDays.size, "생리 예정일 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 24)),
            cycle.predictDays[0].startDate,
            "예정일 시작: 2025-03-24 (지연 다음날)"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 28)),
            cycle.predictDays[0].endDate,
            "예정일 종료: 2025-03-28"
        )

        // 배란기 검증 (조회 범위 밖)
        assertEquals(0, cycle.ovulationDays.size, "배란기 없음 (조회 범위 밖)")

        // 가임기 검증 (지연 적용된 미래 주기)
        assertEquals(1, cycle.fertileDays.size, "가임기 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 30)),
            cycle.fertileDays[0].startDate,
            "가임기 시작: 2025-03-30"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 7)),
            cycle.fertileDays[0].endDate,
            "가임기 종료: 2025-04-07"
        )

        // 주기 검증
        assertEquals(25, cycle.period, "주기: 25일")
    }

    /**
     * TC-02-13: 생리 지연 8일 이상 (예정일 표시 안 함, 병원 권장)
     *
     * 테스트 목적: 생리 예정일이 지나고 8일 이상 지연되었을 때 예정일과 지연 기간을
     *             표시하지 않는지 검증 (병원 진료 권장 상태, 25일 주기)
     *
     * 조회 기간: 2025-03-10 ~ 2025-04-30
     * 오늘 날짜: 2025-03-24 (생리 예정일 03-17로부터 8일 지연)
     *
     * 예상 결과:
     * - 실제 생리 기록: 2025-02-20 ~ 2025-02-24 (pk=3, 마지막 생리)
     * - 생리 지연일: 8일
     * - 지연 기간: 없음 (8일 이상이면 지연 기간 표시 안 함)
     * - 생리 예정일: 없음 (8일 이상 지연 시 예정일 표시 안 함)
     * - 배란기: 없음 (조회 범위 밖)
     * - 가임기: 없음 (조회 범위 밖)
     * - 주기: 25일
     */
    @Test
    fun testTC_02_13_delayEightDaysOrMore() = runTest {
        val repository = createRepository()

        val searchFrom = LocalDate(2025, 3, 10)
        val searchTo = LocalDate(2025, 4, 30)
        val today = LocalDate(2025, 3, 24) // 8일 지연

        val result = PeriodCalculator.calculateCycleInfo(
            repository,
            DateUtils.toJulianDay(searchFrom),
            DateUtils.toJulianDay(searchTo),
            DateUtils.toJulianDay(today)
        )

        // 검증
        assertEquals(1, result.size, "주기 개수: 1개")

        val cycle = result[0]

        // 실제 생리 기록 검증
        assertEquals("3", cycle.pk, "pk=3 (마지막 생리)")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 20)),
            cycle.actualPeriod?.startDate,
            "실제 생리 시작: 2025-02-20"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 24)),
            cycle.actualPeriod?.endDate,
            "실제 생리 종료: 2025-02-24"
        )

        // 생리 지연일 검증 (8일로 계산됨)
        assertEquals(8, cycle.delayTheDays, "생리 지연일: 8일")

        // 지연 기간 검증 (2025-03-17 ~ 2025-03-24)
        assertTrue(cycle.delayDay != null, "지연 기간 있음")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 17)),
            cycle.delayDay?.startDate,
            "지연 시작: 2025-03-17"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 24)),
            cycle.delayDay?.endDate,
            "지연 종료: 2025-03-24"
        )

        // 생리 예정일 검증 (8일 이상은 표시하지 않음)
        assertEquals(0, cycle.predictDays.size, "생리 예정일 없음 (8일 이상 지연 시 표시 안 함)")

        // 배란기 검증
        assertEquals(0, cycle.ovulationDays.size, "배란기 없음 (조회 범위 밖)")

        // 가임기 검증
        assertEquals(0, cycle.fertileDays.size, "가임기 없음 (조회 범위 밖)")

        // 주기 검증
        assertEquals(25, cycle.period, "주기: 25일")
    }
}
