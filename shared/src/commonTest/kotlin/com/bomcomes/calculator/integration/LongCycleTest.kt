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
 * 긴 주기 (35일) 테스트 케이스
 *
 * 테스트 문서: test-cases/docs/03-long-cycle.md
 *
 * 공통 입력 조건:
 * - 생리 기록:
 *   - Period 1: 2025-01-01 ~ 2025-01-05
 *   - Period 2: 2025-02-05 ~ 2025-02-09 (35일 주기)
 *   - Period 3: 2025-03-12 ~ 2025-03-16 (35일 주기)
 * - 배란 테스트 기록: 없음
 * - 배란일 직접 입력: 없음
 * - 피임약 패키지 시작일: 없음
 * - 오늘 날짜: 2025-04-10 (지연 테스트 제외)
 * - 생리 주기 설정: 평균 35일, 평균 기간 5일, 자동 계산 사용: false
 * - 피임약 설정: 사용 안함
 *
 * **주요 차이점**: 35일 주기 (26일 이상)
 * - 배란기: 생리 시작 19-21일차 (period - 16 ~ period - 14)
 * - 가임기: 생리 시작 16-24일차 (period - 19 ~ period - 11)
 */
class LongCycleTest {

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
            // Period 2: 2025-02-05 ~ 2025-02-09 (35일 주기)
            addPeriod(
                PeriodRecord(
                    pk = "2",
                    startDate = DateUtils.toJulianDay(LocalDate(2025, 2, 5)),
                    endDate = DateUtils.toJulianDay(LocalDate(2025, 2, 9))
                )
            )
            // Period 3: 2025-03-12 ~ 2025-03-16 (35일 주기)
            addPeriod(
                PeriodRecord(
                    pk = "3",
                    startDate = DateUtils.toJulianDay(LocalDate(2025, 3, 12)),
                    endDate = DateUtils.toJulianDay(LocalDate(2025, 3, 16))
                )
            )

            setPeriodSettings(
                PeriodSettings(
                    manualAverageCycle = 35,
                    manualAverageDay = 5,
                    autoAverageCycle = 35,
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
     * TC-03-01: 1일 조회 (미래)
     *
     * 테스트 목적: Period 3 이후 특정 날짜 1일만 조회할 때 해당 날짜의 상태가 정확한지 검증
     *
     * 조회 기간: 2025-04-10 ~ 2025-04-10 (1일)
     *
     * 예상 결과:
     * - 실제 생리 기록: 2025-03-12 ~ 2025-03-16 (pk=3, 마지막 생리)
     * - 생리 예정일: 없음 (조회 기간 내 생리 예정일 없음, 다음 예정일은 2025-04-16)
     * - 생리 지연일: 없음
     * - 배란기: 없음 (배란기는 2025-03-30 ~ 2025-04-01, 조회일 이후)
     * - 가임기: 없음 (가임기는 2025-03-27 ~ 2025-04-04, 조회일 이후)
     * - 주기: 35일
     */
    @Test
    fun testTC_03_01_singleDayQuery() = runTest {
        val repository = createRepository()

        val searchFrom = LocalDate(2025, 4, 10)
        val searchTo = LocalDate(2025, 4, 10)
        val today = LocalDate(2025, 4, 10)

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
            DateUtils.toJulianDay(LocalDate(2025, 3, 12)),
            cycle.actualPeriod?.startDate,
            "실제 생리 시작: 2025-03-12"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 16)),
            cycle.actualPeriod?.endDate,
            "실제 생리 종료: 2025-03-16"
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
        assertEquals(35, cycle.period, "주기: 35일")
    }

    /**
     * TC-03-02: 1주일 조회 (미래)
     *
     * 테스트 목적: Period 3 이후 1주일 기간 조회 시 35일 주기의 상태 변화가 정확히 표시되는지 검증
     *
     * 조회 기간: 2025-04-05 ~ 2025-04-11 (7일)
     *
     * 예상 결과:
     * - 실제 생리 기록: 2025-03-12 ~ 2025-03-16 (pk=3, 마지막 생리)
     * - 생리 예정일: 없음 (다음 예정일은 2025-04-16부터, 조회 범위 밖)
     * - 생리 지연일: 없음
     * - 배란기: 없음 (배란기는 2025-03-30 ~ 2025-04-01, 조회 범위 밖)
     * - 가임기: 2025-03-27 ~ 2025-04-04 (조회 범위와 부분 겹침)
     * - 주기: 35일
     */
    @Test
    fun testTC_03_02_oneWeekQuery() = runTest {
        val repository = createRepository()

        val searchFrom = LocalDate(2025, 4, 5)
        val searchTo = LocalDate(2025, 4, 11)
        val today = LocalDate(2025, 4, 10)

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
            DateUtils.toJulianDay(LocalDate(2025, 3, 12)),
            cycle.actualPeriod?.startDate,
            "실제 생리 시작: 2025-03-12"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 16)),
            cycle.actualPeriod?.endDate,
            "실제 생리 종료: 2025-03-16"
        )

        // 생리 예정일 검증 (조회 범위 밖)
        assertEquals(0, cycle.predictDays.size, "생리 예정일 없음 (조회 범위 밖)")

        // 생리 지연일 검증
        assertEquals(0, cycle.delayTheDays, "생리 지연일: 0일")

        // 배란기 검증 (조회 범위 밖)
        assertEquals(0, cycle.ovulationDays.size, "배란기 없음 (조회 범위 밖)")

        // 가임기 검증 (조회 범위와 부분 겹침)
        assertEquals(1, cycle.fertileDays.size, "가임기 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 28)),
            cycle.fertileDays[0]?.startDate,
            "가임기 시작: 2025-03-28 (Period 3 시작 + 16)"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 5)),
            cycle.fertileDays[0]?.endDate,
            "가임기 종료: 2025-04-05"
        )

        // 주기 검증
        assertEquals(35, cycle.period, "주기: 35일")
    }

    /**
     * TC-03-03: 1개월 조회 (미래)
     *
     * 테스트 목적: 마지막 생리(Period 3) 전체 주기를 조회하여 실제 생리, 배란기, 가임기가 모두 정확히 표시되는지 검증
     *
     * 조회 기간: 2025-03-12 ~ 2025-04-15 (Period 3 전체 주기, 35일)
     *
     * 예상 결과:
     * - 실제 생리 기록: 2025-03-12 ~ 2025-03-16 (pk=3, 마지막 생리)
     * - 생리 예정일: 없음 (다음 예정일 2025-04-16은 조회 범위 밖)
     * - 생리 지연일: 없음
     * - 배란기: 2025-03-30 ~ 2025-04-01 (생리 시작 19-21일차, period-16 ~ period-14)
     * - 가임기: 2025-03-27 ~ 2025-04-04 (생리 시작 16-24일차, period-19 ~ period-11)
     * - 주기: 35일
     */
    @Test
    fun testTC_03_03_oneMonthQuery() = runTest {
        val repository = createRepository()

        val searchFrom = LocalDate(2025, 3, 12)
        val searchTo = LocalDate(2025, 4, 15)
        val today = LocalDate(2025, 4, 10)

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
            DateUtils.toJulianDay(LocalDate(2025, 3, 12)),
            cycle.actualPeriod?.startDate,
            "실제 생리 시작: 2025-03-12"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 16)),
            cycle.actualPeriod?.endDate,
            "실제 생리 종료: 2025-03-16"
        )

        // 생리 예정일 검증
        assertEquals(0, cycle.predictDays.size, "생리 예정일 없음 (조회 범위 밖)")

        // 생리 지연일 검증
        assertEquals(0, cycle.delayTheDays, "생리 지연일: 0일")

        // 배란기 검증 (인덱스 18-20)
        assertEquals(1, cycle.ovulationDays.size, "배란기 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 31)),
            cycle.ovulationDays[0]?.startDate,
            "배란기 시작: 2025-03-31 (Period 3 시작 + 19)"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 2)),
            cycle.ovulationDays[0]?.endDate,
            "배란기 종료: 2025-04-02"
        )

        // 가임기 검증 (인덱스 15-23)
        assertEquals(1, cycle.fertileDays.size, "가임기 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 28)),
            cycle.fertileDays[0]?.startDate,
            "가임기 시작: 2025-03-28 (Period 3 시작 + 16)"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 5)),
            cycle.fertileDays[0]?.endDate,
            "가임기 종료: 2025-04-05"
        )

        // 주기 검증
        assertEquals(35, cycle.period, "주기: 35일")
    }

    // ====================================================================================
    // 그룹 2: 과거 기록 중심 (생리 주기 사이 - Period 2)
    // ====================================================================================

    /**
     * TC-03-04: 1일 조회 (과거)
     *
     * 테스트 목적: Period 2의 배란기 중 특정 날짜 1일만 조회할 때 해당 날짜의 상태가 정확한지 검증
     *
     * 조회 기간: 2025-02-24 ~ 2025-02-24 (1일, Period 2의 배란기)
     *
     * 예상 결과: 주기 개수 2개
     *
     * 주기 1 (pk=2):
     * - 실제 생리 기록: 2025-02-05 ~ 2025-02-09 (두 번째 생리)
     * - 생리 예정일: 없음 (Period 3는 실제 기록)
     * - 생리 지연일: 없음
     * - 배란기: 2025-02-24 ~ 2025-02-26 (조회일 포함)
     * - 가임기: 2025-02-21 ~ 2025-03-01 (조회일 포함)
     * - 주기: 35일
     *
     * 주기 2 (pk=3):
     * - 실제 생리 기록: 2025-03-12 ~ 2025-03-16 (세 번째 생리)
     * - 생리 예정일: 없음
     * - 생리 지연일: 없음
     * - 배란기: 없음 (조회 범위 밖)
     * - 가임기: 없음 (조회 범위 밖)
     * - 주기: 35일
     */
    @Test
    fun testTC_03_04_singleDayQueryPast() = runTest {
        val repository = createRepository()

        val searchFrom = LocalDate(2025, 2, 24)
        val searchTo = LocalDate(2025, 2, 24)
        val today = LocalDate(2025, 4, 10)

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
            DateUtils.toJulianDay(LocalDate(2025, 2, 5)),
            cycle2.actualPeriod?.startDate,
            "실제 생리 시작: 2025-02-05"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 9)),
            cycle2.actualPeriod?.endDate,
            "실제 생리 종료: 2025-02-09"
        )

        assertEquals(0, cycle2.predictDays.size, "생리 예정일 없음 (Period 3는 실제 기록)")
        assertEquals(0, cycle2.delayTheDays, "생리 지연일: 0일")

        // 배란기 검증 (인덱스 18-20)
        assertEquals(1, cycle2.ovulationDays.size, "배란기 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 24)),
            cycle2.ovulationDays[0]?.startDate,
            "배란기 시작: 2025-02-24 (Period 2 시작 + 19)"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 26)),
            cycle2.ovulationDays[0]?.endDate,
            "배란기 종료: 2025-02-26"
        )

        // 가임기 검증 (인덱스 15-23)
        assertEquals(1, cycle2.fertileDays.size, "가임기 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 21)),
            cycle2.fertileDays[0]?.startDate,
            "가임기 시작: 2025-02-21 (Period 2 시작 + 16)"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 1)),
            cycle2.fertileDays[0]?.endDate,
            "가임기 종료: 2025-03-01"
        )

        assertEquals(35, cycle2.period, "주기: 35일")

        // pk=3 주기 (Period 3): 배란기/가임기 모두 조회 범위 밖
        val cycle3 = result.find { it.pk == "3" }!!

        assertEquals("3", cycle3.pk, "pk=3 (세 번째 생리)")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 12)),
            cycle3.actualPeriod?.startDate,
            "실제 생리 시작: 2025-03-12"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 16)),
            cycle3.actualPeriod?.endDate,
            "실제 생리 종료: 2025-03-16"
        )

        assertEquals(0, cycle3.predictDays.size, "생리 예정일 없음")
        assertEquals(0, cycle3.delayTheDays, "생리 지연일: 0일")
        assertEquals(0, cycle3.ovulationDays.size, "배란기 없음 (조회 범위 밖)")
        assertEquals(0, cycle3.fertileDays.size, "가임기 없음 (조회 범위 밖)")
        assertEquals(35, cycle3.period, "주기: 35일")
    }

    /**
     * TC-03-05: 1주일 조회 (과거)
     *
     * 테스트 목적: Period 2의 배란기/가임기가 포함된 1주일 기간 조회 시 해당 주의 상태 변화가 정확히 표시되는지 검증
     *
     * 조회 기간: 2025-02-21 ~ 2025-02-27 (7일, Period 2의 배란기/가임기 포함)
     *
     * 예상 결과: 주기 개수 2개
     *
     * 주기 1 (pk=2):
     * - 실제 생리 기록: 2025-02-05 ~ 2025-02-09 (두 번째 생리)
     * - 생리 예정일: 없음 (Period 3는 실제 기록)
     * - 생리 지연일: 없음
     * - 배란기: 2025-02-23 ~ 2025-02-25 (조회 범위와 부분 겹침)
     * - 가임기: 2025-02-20 ~ 2025-02-28 (조회 범위와 부분 겹침)
     * - 주기: 35일
     *
     * 주기 2 (pk=3):
     * - 실제 생리 기록: 2025-03-12 ~ 2025-03-16 (세 번째 생리)
     * - 생리 예정일: 없음
     * - 생리 지연일: 없음
     * - 배란기: 없음 (조회 범위 밖)
     * - 가임기: 없음 (조회 범위 밖)
     * - 주기: 35일
     */
    @Test
    fun testTC_03_05_oneWeekQueryPast() = runTest {
        val repository = createRepository()

        val searchFrom = LocalDate(2025, 2, 21)
        val searchTo = LocalDate(2025, 2, 27)
        val today = LocalDate(2025, 4, 10)

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
            DateUtils.toJulianDay(LocalDate(2025, 2, 5)),
            cycle2.actualPeriod?.startDate,
            "실제 생리 시작: 2025-02-05"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 9)),
            cycle2.actualPeriod?.endDate,
            "실제 생리 종료: 2025-02-09"
        )

        assertEquals(0, cycle2.predictDays.size, "생리 예정일 없음 (Period 3는 실제 기록)")
        assertEquals(0, cycle2.delayTheDays, "생리 지연일: 0일")

        // 배란기 검증 (인덱스 18-20)
        assertEquals(1, cycle2.ovulationDays.size, "배란기 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 24)),
            cycle2.ovulationDays[0]?.startDate,
            "배란기 시작: 2025-02-24 (Period 2 시작 + 19)"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 26)),
            cycle2.ovulationDays[0]?.endDate,
            "배란기 종료: 2025-02-26"
        )

        // 가임기 검증 (인덱스 15-23)
        assertEquals(1, cycle2.fertileDays.size, "가임기 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 21)),
            cycle2.fertileDays[0]?.startDate,
            "가임기 시작: 2025-02-21 (Period 2 시작 + 16)"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 1)),
            cycle2.fertileDays[0]?.endDate,
            "가임기 종료: 2025-03-01"
        )

        assertEquals(35, cycle2.period, "주기: 35일")

        // pk=3 주기 (Period 3): 배란기/가임기 모두 조회 범위 밖
        val cycle3 = result.find { it.pk == "3" }!!

        assertEquals("3", cycle3.pk, "pk=3 (세 번째 생리)")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 12)),
            cycle3.actualPeriod?.startDate,
            "실제 생리 시작: 2025-03-12"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 16)),
            cycle3.actualPeriod?.endDate,
            "실제 생리 종료: 2025-03-16"
        )

        assertEquals(0, cycle3.predictDays.size, "생리 예정일 없음")
        assertEquals(0, cycle3.delayTheDays, "생리 지연일: 0일")
        assertEquals(0, cycle3.ovulationDays.size, "배란기 없음 (조회 범위 밖)")
        assertEquals(0, cycle3.fertileDays.size, "가임기 없음 (조회 범위 밖)")
        assertEquals(35, cycle3.period, "주기: 35일")
    }

    /**
     * TC-03-06: 1개월 조회 (과거)
     *
     * 테스트 목적: Period 2 전체 주기를 조회하여 실제 생리, 배란기, 가임기가 모두 정확히 표시되는지 검증
     *
     * 조회 기간: 2025-02-05 ~ 2025-03-11 (Period 2 전체 주기, 35일)
     *
     * 예상 결과: 주기 개수 2개
     *
     * 주기 1 (pk=2):
     * - 실제 생리 기록: 2025-02-05 ~ 2025-02-09 (두 번째 생리)
     * - 생리 예정일: 없음 (Period 3는 실제 기록)
     * - 생리 지연일: 없음
     * - 배란기: 2025-02-23 ~ 2025-02-25 (생리 시작 19-21일차)
     * - 가임기: 2025-02-20 ~ 2025-02-28 (생리 시작 16-24일차)
     * - 주기: 35일
     *
     * 주기 2 (pk=3):
     * - 실제 생리 기록: 2025-03-12 ~ 2025-03-16 (세 번째 생리)
     * - 생리 예정일: 없음
     * - 생리 지연일: 없음
     * - 배란기: 없음 (조회 범위 밖)
     * - 가임기: 없음 (조회 범위 밖)
     * - 주기: 35일
     */
    @Test
    fun testTC_03_06_oneMonthQueryPast() = runTest {
        val repository = createRepository()

        val searchFrom = LocalDate(2025, 2, 5)
        val searchTo = LocalDate(2025, 3, 11)
        val today = LocalDate(2025, 4, 10)

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
            DateUtils.toJulianDay(LocalDate(2025, 2, 5)),
            cycle2.actualPeriod?.startDate,
            "실제 생리 시작: 2025-02-05"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 9)),
            cycle2.actualPeriod?.endDate,
            "실제 생리 종료: 2025-02-09"
        )

        assertEquals(0, cycle2.predictDays.size, "생리 예정일 없음 (Period 3는 실제 기록)")
        assertEquals(0, cycle2.delayTheDays, "생리 지연일: 0일")

        // 배란기 검증 (인덱스 18-20)
        assertEquals(1, cycle2.ovulationDays.size, "배란기 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 24)),
            cycle2.ovulationDays[0]?.startDate,
            "배란기 시작: 2025-02-24 (Period 2 시작 + 19)"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 26)),
            cycle2.ovulationDays[0]?.endDate,
            "배란기 종료: 2025-02-26"
        )

        // 가임기 검증 (인덱스 15-23)
        assertEquals(1, cycle2.fertileDays.size, "가임기 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 21)),
            cycle2.fertileDays[0]?.startDate,
            "가임기 시작: 2025-02-21 (Period 2 시작 + 16)"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 1)),
            cycle2.fertileDays[0]?.endDate,
            "가임기 종료: 2025-03-01"
        )

        assertEquals(35, cycle2.period, "주기: 35일")

        // pk=3 주기 (Period 3): 다음 생리 정보
        val cycle3 = result.find { it.pk == "3" }!!

        assertEquals("3", cycle3.pk, "pk=3 (세 번째 생리)")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 12)),
            cycle3.actualPeriod?.startDate,
            "실제 생리 시작: 2025-03-12"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 16)),
            cycle3.actualPeriod?.endDate,
            "실제 생리 종료: 2025-03-16"
        )

        assertEquals(0, cycle3.predictDays.size, "생리 예정일 없음")
        assertEquals(0, cycle3.delayTheDays, "생리 지연일: 0일")
        assertEquals(0, cycle3.ovulationDays.size, "배란기 없음 (조회 범위 밖)")
        assertEquals(0, cycle3.fertileDays.size, "가임기 없음 (조회 범위 밖)")
        assertEquals(35, cycle3.period, "주기: 35일")
    }

    // ====================================================================================
    // 그룹 3: 장기 조회 및 특수 경계
    // ====================================================================================

    /**
     * TC-03-07: 생리 기간 경계 조회
     *
     * 테스트 목적: 조회 기간이 생리 종료일과 다음 생리 시작일에 걸쳐있을 때 두 주기가 모두 반환되는지 검증
     *
     * 조회 기간: 2025-03-10 ~ 2025-03-14 (Period 2 종료 후 ~ Period 3 진행 중)
     *
     * 예상 결과: 주기 개수 2개
     *
     * 주기 1 (pk=2):
     * - 실제 생리 기록: 2025-02-05 ~ 2025-02-09 (두 번째 생리)
     * - 생리 예정일: 없음 (Period 3는 실제 기록)
     * - 생리 지연일: 없음
     * - 배란기: 없음 (조회 범위 밖)
     * - 가임기: 없음 (조회 범위 밖)
     * - 주기: 35일
     *
     * 주기 2 (pk=3):
     * - 실제 생리 기록: 2025-03-12 ~ 2025-03-16 (세 번째 생리, 조회 범위와 부분 겹침)
     * - 생리 예정일: 없음
     * - 생리 지연일: 없음
     * - 배란기: 없음 (조회 범위 밖)
     * - 가임기: 없음 (조회 범위 밖)
     * - 주기: 35일
     */
    @Test
    fun testTC_03_07_periodBoundaryQuery() = runTest {
        val repository = createRepository()

        val searchFrom = LocalDate(2025, 3, 10)
        val searchTo = LocalDate(2025, 3, 14)
        val today = LocalDate(2025, 4, 10)

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
            DateUtils.toJulianDay(LocalDate(2025, 2, 5)),
            cycle2.actualPeriod?.startDate,
            "실제 생리 시작: 2025-02-05"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 9)),
            cycle2.actualPeriod?.endDate,
            "실제 생리 종료: 2025-02-09"
        )

        assertEquals(0, cycle2.predictDays.size, "생리 예정일 없음 (Period 3는 실제 기록)")
        assertEquals(0, cycle2.delayTheDays, "생리 지연일: 0일")
        assertEquals(0, cycle2.ovulationDays.size, "배란기 없음 (조회 범위 밖)")
        assertEquals(0, cycle2.fertileDays.size, "가임기 없음 (조회 범위 밖)")
        assertEquals(35, cycle2.period, "주기: 35일")

        // pk=3 주기 (Period 3): 생리 진행 중, 조회 범위와 부분 겹침
        val cycle3 = result.find { it.pk == "3" }!!

        assertEquals("3", cycle3.pk, "pk=3 (세 번째 생리)")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 12)),
            cycle3.actualPeriod?.startDate,
            "실제 생리 시작: 2025-03-12"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 16)),
            cycle3.actualPeriod?.endDate,
            "실제 생리 종료: 2025-03-16"
        )

        assertEquals(0, cycle3.predictDays.size, "생리 예정일 없음")
        assertEquals(0, cycle3.delayTheDays, "생리 지연일: 0일")
        assertEquals(0, cycle3.ovulationDays.size, "배란기 없음 (조회 범위 밖)")
        assertEquals(0, cycle3.fertileDays.size, "가임기 없음 (조회 범위 밖)")
        assertEquals(35, cycle3.period, "주기: 35일")
    }

    /**
     * TC-03-08: 3개월 조회
     *
     * 테스트 목적: 장기간 조회 시 35일 주기의 예측이 반복적으로 정확한지 검증
     *
     * 조회 기간: 2025-03-27 ~ 2025-06-30 (3개월)
     *
     * 예상 결과:
     * - 실제 생리 기록: 2025-03-12 ~ 2025-03-16 (pk=3, 마지막 생리)
     * - 생리 예정일들:
     *   - 2025-04-16 ~ 2025-04-20
     *   - 2025-05-21 ~ 2025-05-25
     *   - 2025-06-25 ~ 2025-06-29
     * - 배란기들:
     *   - 2025-03-30 ~ 2025-04-01 (첫 번째 주기)
     *   - 2025-05-04 ~ 2025-05-06 (두 번째 주기)
     *   - 2025-06-08 ~ 2025-06-10 (세 번째 주기)
     * - 가임기들:
     *   - 2025-03-27 ~ 2025-04-04 (부분 포함)
     *   - 2025-05-01 ~ 2025-05-09
     *   - 2025-06-05 ~ 2025-06-13
     * - 주기: 35일
     */
    @Test
    fun testTC_03_08_threeMonthQuery() = runTest {
        val repository = createRepository()

        val searchFrom = LocalDate(2025, 3, 27)
        val searchTo = LocalDate(2025, 6, 30)
        val today = LocalDate(2025, 4, 10)

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
            DateUtils.toJulianDay(LocalDate(2025, 3, 12)),
            cycle.actualPeriod?.startDate,
            "실제 생리 시작: 2025-03-12"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 16)),
            cycle.actualPeriod?.endDate,
            "실제 생리 종료: 2025-03-16"
        )

        // 생리 예정일 검증 (3개)
        assertEquals(3, cycle.predictDays.size, "생리 예정일 3개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 16)),
            cycle.predictDays[0].startDate,
            "첫 번째 예정일 시작: 2025-04-16"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 20)),
            cycle.predictDays[0].endDate,
            "첫 번째 예정일 종료: 2025-04-20"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 5, 21)),
            cycle.predictDays[1].startDate,
            "두 번째 예정일 시작: 2025-05-21"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 5, 25)),
            cycle.predictDays[1].endDate,
            "두 번째 예정일 종료: 2025-05-25"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 6, 25)),
            cycle.predictDays[2].startDate,
            "세 번째 예정일 시작: 2025-06-25"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 6, 29)),
            cycle.predictDays[2].endDate,
            "세 번째 예정일 종료: 2025-06-29"
        )

        // 배란기 검증 (3개)
        assertEquals(3, cycle.ovulationDays.size, "배란기 3개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 31)),
            cycle.ovulationDays[0]?.startDate,
            "첫 번째 배란기 시작: 2025-03-31"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 2)),
            cycle.ovulationDays[0]?.endDate,
            "첫 번째 배란기 종료: 2025-04-02"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 5, 5)),
            cycle.ovulationDays[1]?.startDate,
            "두 번째 배란기 시작: 2025-05-05"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 5, 7)),
            cycle.ovulationDays[1]?.endDate,
            "두 번째 배란기 종료: 2025-05-07"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 6, 9)),
            cycle.ovulationDays[2]?.startDate,
            "세 번째 배란기 시작: 2025-06-09"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 6, 11)),
            cycle.ovulationDays[2]?.endDate,
            "세 번째 배란기 종료: 2025-06-11"
        )

        // 가임기 검증 (3개)
        assertEquals(3, cycle.fertileDays.size, "가임기 3개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 28)),
            cycle.fertileDays[0]?.startDate,
            "첫 번째 가임기 시작: 2025-03-28 (조회 범위와 부분 겹침)"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 5)),
            cycle.fertileDays[0]?.endDate,
            "첫 번째 가임기 종료: 2025-04-05"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 5, 2)),
            cycle.fertileDays[1]?.startDate,
            "두 번째 가임기 시작: 2025-05-02"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 5, 10)),
            cycle.fertileDays[1]?.endDate,
            "두 번째 가임기 종료: 2025-05-10"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 6, 6)),
            cycle.fertileDays[2]?.startDate,
            "세 번째 가임기 시작: 2025-06-06"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 6, 14)),
            cycle.fertileDays[2]?.endDate,
            "세 번째 가임기 종료: 2025-06-14"
        )

        // 주기 검증
        assertEquals(35, cycle.period, "주기: 35일")
    }

    // ====================================================================================
    // 그룹 4: 특정 기간 타입 조회
    // ====================================================================================

    /**
     * TC-03-09: 생리 예정 기간 중 조회
     *
     * 테스트 목적: 조회 기간이 생리 예정 기간을 포함할 때 정확히 표시되는지 검증
     *
     * 조회 기간: 2025-04-16 ~ 2025-04-20
     *
     * 예상 결과:
     * - 실제 생리 기록: 2025-03-12 ~ 2025-03-16 (pk=3, 마지막 생리)
     * - 생리 예정일: 2025-04-16 ~ 2025-04-20
     * - 생리 지연일: 없음
     * - 배란기: 없음 (조회 기간 내 배란기 없음)
     * - 가임기: 없음 (조회 기간 내 가임기 없음)
     * - 주기: 35일
     */
    @Test
    fun testTC_03_09_predictedPeriodQuery() = runTest {
        val repository = createRepository()

        val searchFrom = LocalDate(2025, 4, 16)
        val searchTo = LocalDate(2025, 4, 20)
        val today = LocalDate(2025, 4, 10)

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
            DateUtils.toJulianDay(LocalDate(2025, 3, 12)),
            cycle.actualPeriod?.startDate,
            "실제 생리 시작: 2025-03-12"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 16)),
            cycle.actualPeriod?.endDate,
            "실제 생리 종료: 2025-03-16"
        )

        // 생리 예정일 검증
        assertEquals(1, cycle.predictDays.size, "생리 예정일 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 16)),
            cycle.predictDays[0].startDate,
            "예정일 시작: 2025-04-16"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 20)),
            cycle.predictDays[0].endDate,
            "예정일 종료: 2025-04-20"
        )

        // 생리 지연일 검증
        assertEquals(0, cycle.delayTheDays, "생리 지연일: 0일")

        // 배란기 검증
        assertEquals(0, cycle.ovulationDays.size, "배란기 없음 (조회 기간 내 없음)")

        // 가임기 검증
        assertEquals(0, cycle.fertileDays.size, "가임기 없음 (조회 기간 내 없음)")

        // 주기 검증
        assertEquals(35, cycle.period, "주기: 35일")
    }

    /**
     * TC-03-10: 배란기 중 조회
     *
     * 테스트 목적: 조회 기간이 배란기를 포함할 때 정확히 표시되는지 검증 (35일 주기 공식 적용)
     *
     * 조회 기간: 2025-03-31 ~ 2025-04-02
     *
     * 예상 결과:
     * - 실제 생리 기록: 2025-03-12 ~ 2025-03-16 (pk=3, 마지막 생리)
     * - 생리 예정일: 없음 (조회 기간 내 생리 없음)
     * - 생리 지연일: 없음
     * - 배란기: 2025-03-31 ~ 2025-04-02
     * - 가임기: 2025-03-28 ~ 2025-04-05 (조회 범위와 부분 겹침)
     * - 주기: 35일
     */
    @Test
    fun testTC_03_10_ovulationPeriodQuery() = runTest {
        val repository = createRepository()

        val searchFrom = LocalDate(2025, 3, 31)
        val searchTo = LocalDate(2025, 4, 2)
        val today = LocalDate(2025, 4, 10)

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
            DateUtils.toJulianDay(LocalDate(2025, 3, 12)),
            cycle.actualPeriod?.startDate,
            "실제 생리 시작: 2025-03-12"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 16)),
            cycle.actualPeriod?.endDate,
            "실제 생리 종료: 2025-03-16"
        )

        // 생리 예정일 검증
        assertEquals(0, cycle.predictDays.size, "생리 예정일 없음 (조회 기간 내 없음)")

        // 생리 지연일 검증
        assertEquals(0, cycle.delayTheDays, "생리 지연일: 0일")

        // 배란기 검증 (인덱스 18-20)
        assertEquals(1, cycle.ovulationDays.size, "배란기 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 31)),
            cycle.ovulationDays[0]?.startDate,
            "배란기 시작: 2025-03-31"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 2)),
            cycle.ovulationDays[0]?.endDate,
            "배란기 종료: 2025-04-02"
        )

        // 가임기 검증 (조회 범위와 부분 겹침)
        assertEquals(1, cycle.fertileDays.size, "가임기 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 28)),
            cycle.fertileDays[0]?.startDate,
            "가임기 시작: 2025-03-28 (전체 범위 반환)"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 5)),
            cycle.fertileDays[0]?.endDate,
            "가임기 종료: 2025-04-05"
        )

        // 주기 검증
        assertEquals(35, cycle.period, "주기: 35일")
    }

    /**
     * TC-03-11: 가임기 중 조회
     *
     * 테스트 목적: 조회 기간이 가임기를 포함할 때 정확히 표시되는지 검증 (35일 주기 공식 적용)
     *
     * 조회 기간: 2025-03-28 ~ 2025-03-30
     *
     * 예상 결과:
     * - 실제 생리 기록: 2025-03-12 ~ 2025-03-16 (pk=3, 마지막 생리)
     * - 생리 예정일: 없음 (조회 기간 내 생리 없음)
     * - 생리 지연일: 없음
     * - 배란기: 없음 (배란기는 2025-03-31부터 시작)
     * - 가임기: 2025-03-28 ~ 2025-04-05 (조회 범위와 부분 겹침)
     * - 주기: 35일
     */
    @Test
    fun testTC_03_11_fertilePeriodQuery() = runTest {
        val repository = createRepository()

        val searchFrom = LocalDate(2025, 3, 28)
        val searchTo = LocalDate(2025, 3, 30)
        val today = LocalDate(2025, 4, 10)

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
            DateUtils.toJulianDay(LocalDate(2025, 3, 12)),
            cycle.actualPeriod?.startDate,
            "실제 생리 시작: 2025-03-12"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 16)),
            cycle.actualPeriod?.endDate,
            "실제 생리 종료: 2025-03-16"
        )

        // 생리 예정일 검증
        assertEquals(0, cycle.predictDays.size, "생리 예정일 없음 (조회 기간 내 없음)")

        // 생리 지연일 검증
        assertEquals(0, cycle.delayTheDays, "생리 지연일: 0일")

        // 배란기 검증 (조회 기간 내 없음)
        assertEquals(0, cycle.ovulationDays.size, "배란기 없음 (배란기는 03-31부터 시작)")

        // 가임기 검증 (조회 범위와 부분 겹침)
        assertEquals(1, cycle.fertileDays.size, "가임기 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 28)),
            cycle.fertileDays[0]?.startDate,
            "가임기 시작: 2025-03-28 (전체 범위 반환)"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 5)),
            cycle.fertileDays[0]?.endDate,
            "가임기 종료: 2025-04-05"
        )

        // 주기 검증
        assertEquals(35, cycle.period, "주기: 35일")
    }

    // ====================================================================================
    // 그룹 5: 생리 지연 케이스
    // ====================================================================================

    /**
     * TC-03-12: 생리 지연 1-7일 (예정일 뒤로 미룸)
     *
     * 테스트 목적: 생리 예정일이 지나고 1-7일 지연되었을 때 지연 기간이 표시되고,
     *             다음 예정일이 지연 다음날부터 시작하는지 검증 (35일 주기)
     *
     * 조회 기간: 2025-04-05 ~ 2025-05-31
     * 오늘 날짜: 2025-04-22 (생리 예정일 04-16로부터 7일 지연)
     *
     * 예상 결과:
     * - 실제 생리 기록: 2025-03-12 ~ 2025-03-16 (pk=3, 마지막 생리)
     * - 생리 지연일: 7일
     * - 지연 기간: 2025-04-16 ~ 2025-04-22 (예정일부터 오늘까지)
     * - 생리 예정일: 2025-04-23 ~ 2025-04-27 (지연 다음날부터 5일간)
     * - 배란기: 2025-05-08 ~ 2025-05-10 (지연 적용된 미래 주기)
     * - 가임기: 2025-05-05 ~ 2025-05-13 (지연 적용된 미래 주기)
     * - 주기: 35일
     */
    @Test
    fun testTC_03_12_delayOneToSevenDays() = runTest {
        val repository = createRepository()

        val searchFrom = LocalDate(2025, 4, 5)
        val searchTo = LocalDate(2025, 5, 31)
        val today = LocalDate(2025, 4, 22) // 7일 지연

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
            DateUtils.toJulianDay(LocalDate(2025, 3, 12)),
            cycle.actualPeriod?.startDate,
            "실제 생리 시작: 2025-03-12"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 16)),
            cycle.actualPeriod?.endDate,
            "실제 생리 종료: 2025-03-16"
        )

        // 생리 지연일 검증
        assertEquals(7, cycle.delayTheDays, "생리 지연일: 7일")

        // 지연 기간 검증 (2025-04-16 ~ 2025-04-22)
        assertTrue(cycle.delayDay != null, "지연 기간 있음")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 16)),
            cycle.delayDay?.startDate,
            "지연 시작: 2025-04-16"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 22)),
            cycle.delayDay?.endDate,
            "지연 종료: 2025-04-22"
        )

        // 생리 예정일 검증 (지연 다음날부터)
        assertEquals(2, cycle.predictDays.size, "생리 예정일 2개")

        // 첫 번째 예정일 (지연 적용)
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 23)),
            cycle.predictDays[0].startDate,
            "첫 번째 예정일 시작: 2025-04-23 (지연 다음날)"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 27)),
            cycle.predictDays[0].endDate,
            "첫 번째 예정일 종료: 2025-04-27"
        )

        // 두 번째 예정일
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 5, 28)),
            cycle.predictDays[1].startDate,
            "두 번째 예정일 시작: 2025-05-28"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 6, 1)),
            cycle.predictDays[1].endDate,
            "두 번째 예정일 종료: 2025-06-01"
        )

        // 배란기 검증 (조회 범위 내 1개만)
        assertEquals(1, cycle.ovulationDays.size, "배란기 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 5, 12)),
            cycle.ovulationDays[0].startDate,
            "배란기 시작: 2025-05-12 (첫 번째 예정일 배란기, 지연 7일 적용)"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 5, 14)),
            cycle.ovulationDays[0].endDate,
            "배란기 종료: 2025-05-14"
        )

        // 가임기 검증 (2개: Period 3 + 첫 번째 예정일)
        assertEquals(2, cycle.fertileDays.size, "가임기 2개")

        // 첫 번째 가임기 (Period 3)
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 28)),
            cycle.fertileDays[0].startDate,
            "첫 번째 가임기 시작: 2025-03-28 (Period 3의 가임기)"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 5)),
            cycle.fertileDays[0].endDate,
            "첫 번째 가임기 종료: 2025-04-05 (조회 시작일과 겹침)"
        )

        // 두 번째 가임기 (첫 번째 예정일)
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 5, 9)),
            cycle.fertileDays[1].startDate,
            "두 번째 가임기 시작: 2025-05-09 (첫 번째 예정일 가임기, 지연 7일 적용)"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 5, 17)),
            cycle.fertileDays[1].endDate,
            "두 번째 가임기 종료: 2025-05-17"
        )

        // 주기 검증
        assertEquals(35, cycle.period, "주기: 35일")
    }

    /**
     * TC-03-13: 생리 지연 8일 이상 (예정일 표시 안 함, 병원 권장)
     *
     * 테스트 목적: 생리 예정일이 지나고 8일 이상 지연되었을 때 예정일과 지연 기간을
     *             표시하지 않는지 검증 (병원 진료 권장 상태, 35일 주기)
     *
     * 조회 기간: 2025-04-05 ~ 2025-06-30
     * 오늘 날짜: 2025-04-23 (생리 예정일 04-16로부터 8일 지연)
     *
     * 예상 결과:
     * - 실제 생리 기록: 2025-03-12 ~ 2025-03-16 (pk=3, 마지막 생리)
     * - 생리 지연일: 8일
     * - 지연 기간: 없음 (8일 이상이면 지연 기간 표시 안 함)
     * - 생리 예정일: 없음 (8일 이상 지연 시 예정일 표시 안 함)
     * - 배란기: 없음 (조회 범위 밖)
     * - 가임기: 없음 (조회 범위 밖)
     * - 주기: 35일
     */
    @Test
    fun testTC_03_13_delayEightDaysOrMore() = runTest {
        val repository = createRepository()

        val searchFrom = LocalDate(2025, 4, 5)
        val searchTo = LocalDate(2025, 6, 30)
        val today = LocalDate(2025, 4, 23) // 8일 지연

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
            DateUtils.toJulianDay(LocalDate(2025, 3, 12)),
            cycle.actualPeriod?.startDate,
            "실제 생리 시작: 2025-03-12"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 16)),
            cycle.actualPeriod?.endDate,
            "실제 생리 종료: 2025-03-16"
        )

        // 생리 지연일 검증 (8일로 계산됨)
        assertEquals(8, cycle.delayTheDays, "생리 지연일: 8일")

        // 지연 기간 검증 (2025-04-16 ~ 2025-04-23)
        assertTrue(cycle.delayDay != null, "지연 기간 있음")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 16)),
            cycle.delayDay?.startDate,
            "지연 시작: 2025-04-16"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 23)),
            cycle.delayDay?.endDate,
            "지연 종료: 2025-04-23"
        )

        // 생리 예정일 검증 (8일 이상은 표시하지 않음)
        assertEquals(0, cycle.predictDays.size, "생리 예정일 없음 (8일 이상 지연 시 표시 안 함)")

        // 배란기 검증
        assertEquals(0, cycle.ovulationDays.size, "배란기 없음 (조회 범위 밖)")

        // 가임기 검증 (Period 3의 가임기가 조회 시작일과 겹침)
        assertEquals(1, cycle.fertileDays.size, "가임기 1개 (Period 3)")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 28)),
            cycle.fertileDays[0].startDate,
            "가임기 시작: 2025-03-28 (Period 3의 가임기)"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 5)),
            cycle.fertileDays[0].endDate,
            "가임기 종료: 2025-04-05 (조회 시작일과 겹침)"
        )

        // 주기 검증
        assertEquals(35, cycle.period, "주기: 35일")
    }
}
