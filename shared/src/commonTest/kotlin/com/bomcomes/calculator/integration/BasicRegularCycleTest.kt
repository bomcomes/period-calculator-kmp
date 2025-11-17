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
 * 기본 규칙적인 28일 주기 테스트 케이스
 *
 * 테스트 문서: test-cases/docs/01-basic-regular-cycle.md
 *
 * 공통 입력 조건:
 * - 생리 기록:
 *   - Period 1: 2025-01-01 ~ 2025-01-05
 *   - Period 2: 2025-01-29 ~ 2025-02-02
 *   - Period 3: 2025-02-26 ~ 2025-03-02
 * - 배란 테스트 기록: 없음
 * - 배란일 직접 입력: 없음
 * - 피임약 패키지 시작일: 없음
 * - 오늘 날짜: 2025-03-15
 * - 생리 주기 설정: 평균 28일, 평균 기간 5일, 자동 계산 사용: false
 * - 피임약 설정: 사용 안함
 */
class BasicRegularCycleTest {

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
            // Period 2: 2025-01-29 ~ 2025-02-02
            addPeriod(
                PeriodRecord(
                    pk = "2",
                    startDate = DateUtils.toJulianDay(LocalDate(2025, 1, 29)),
                    endDate = DateUtils.toJulianDay(LocalDate(2025, 2, 2))
                )
            )
            // Period 3: 2025-02-26 ~ 2025-03-02
            addPeriod(
                PeriodRecord(
                    pk = "3",
                    startDate = DateUtils.toJulianDay(LocalDate(2025, 2, 26)),
                    endDate = DateUtils.toJulianDay(LocalDate(2025, 3, 2))
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
    }

    /**
     * TC-01-01: 1일 조회
     *
     * 테스트 목적: 특정 날짜 1일만 조회할 때 해당 날짜의 상태가 정확한지 검증
     *
     * 조회 기간: 2025-03-15 ~ 2025-03-15 (1일)
     *
     * 예상 결과:
     * - 실제 생리 기록: 2025-02-26 ~ 2025-03-02 (pk=3, 마지막 생리)
     * - 생리 예정일: 없음 (조회 기간 내 생리 예정일 없음, 다음 예정일은 2025-03-26)
     * - 생리 지연일: 없음
     * - 배란기: 없음 (조회 기간 내 배란기 없음, 배란기는 2025-03-10 ~ 2025-03-12)
     * - 가임기: 2025-03-05 ~ 2025-03-16 (조회일 3/15가 가임기 범위 내)
     * - 주기: 28일
     */
    @Test
    fun testTC_01_01_singleDayQuery() = runTest {
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
            DateUtils.toJulianDay(LocalDate(2025, 2, 26)),
            cycle.actualPeriod?.startDate,
            "실제 생리 시작: 2025-02-26"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 2)),
            cycle.actualPeriod?.endDate,
            "실제 생리 종료: 2025-03-02"
        )

        // 생리 예정일 검증
        assertEquals(0, cycle.predictDays.size, "생리 예정일 없음 (조회 기간 내 없음)")

        // 생리 지연일 검증
        assertEquals(0, cycle.delayTheDays, "생리 지연일: 0일")

        // 배란기 검증
        assertEquals(0, cycle.ovulationDays.size, "배란기 없음 (조회 기간 내 없음)")

        // 가임기 검증 (2025-03-05 ~ 2025-03-16, 조회일 포함)
        assertEquals(1, cycle.fertileDays.size, "가임기 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 5)),
            cycle.fertileDays[0]?.startDate,
            "가임기 시작: 2025-03-05"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 16)),
            cycle.fertileDays[0]?.endDate,
            "가임기 종료: 2025-03-16"
        )

        // 주기 검증
        assertEquals(28, cycle.period, "주기: 28일")
    }

    /**
     * TC-01-02: 1주일 조회
     *
     * 테스트 목적: 1주일 기간 조회 시 해당 주의 상태 변화가 정확히 표시되는지 검증
     *
     * 조회 기간: 2025-03-10 ~ 2025-03-16 (7일)
     *
     * 예상 결과:
     * - 실제 생리 기록: 2025-02-26 ~ 2025-03-02 (pk=3, 마지막 생리)
     * - 생리 예정일: 없음 (조회 기간 내 생리 예정일 없음)
     * - 생리 지연일: 없음
     * - 배란기: 2025-03-10 ~ 2025-03-12
     * - 가임기: 2025-03-05 ~ 2025-03-16 (조회 범위와 부분 겹침)
     * - 주기: 28일
     */
    @Test
    fun testTC_01_02_oneWeekQuery() = runTest {
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
            DateUtils.toJulianDay(LocalDate(2025, 2, 26)),
            cycle.actualPeriod?.startDate,
            "실제 생리 시작: 2025-02-26"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 2)),
            cycle.actualPeriod?.endDate,
            "실제 생리 종료: 2025-03-02"
        )

        // 생리 예정일 검증
        assertEquals(0, cycle.predictDays.size, "생리 예정일 없음")

        // 생리 지연일 검증
        assertEquals(0, cycle.delayTheDays, "생리 지연일: 0일")

        // 배란기 검증 (2025-03-10 ~ 2025-03-12, 13-15일차)
        assertEquals(1, cycle.ovulationDays.size, "배란기 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 10)),
            cycle.ovulationDays[0]?.startDate,
            "배란기 시작: 2025-03-10"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 12)),
            cycle.ovulationDays[0]?.endDate,
            "배란기 종료: 2025-03-12"
        )

        // 가임기 검증 (2025-03-05 ~ 2025-03-16, 조회 범위와 부분 겹침)
        assertEquals(1, cycle.fertileDays.size, "가임기 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 5)),
            cycle.fertileDays[0]?.startDate,
            "가임기 시작: 2025-03-05 (전체 범위 반환)"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 16)),
            cycle.fertileDays[0]?.endDate,
            "가임기 종료: 2025-03-16"
        )

        // 주기 검증
        assertEquals(28, cycle.period, "주기: 28일")
    }

    /**
     * TC-01-03: 1개월 조회 (미래)
     *
     * 테스트 목적: 마지막 생리(Period 3) 전체 주기를 조회하여 실제 생리, 배란기, 가임기, 다음 예정일이 모두 정확히 표시되는지 검증
     *
     * 조회 기간: 2025-02-26 ~ 2025-03-25 (Period 3 전체 주기, 28일)
     *
     * 예상 결과:
     * - 실제 생리 기록: 2025-02-26 ~ 2025-03-02 (pk=3, 마지막 생리)
     * - 생리 예정일들: 없음 (다음 예정일 2025-03-26은 조회 범위 밖)
     * - 생리 지연일: 없음
     * - 배란기: 2025-03-10 ~ 2025-03-12 (생리 시작 13-15일차)
     * - 가임기: 2025-03-05 ~ 2025-03-16 (생리 시작 8-19일차)
     * - 주기: 28일
     */
    @Test
    fun testTC_01_03_oneMonthFutureQuery() = runTest {
        val repository = createRepository()

        val searchFrom = LocalDate(2025, 2, 26)
        val searchTo = LocalDate(2025, 3, 25)
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
            DateUtils.toJulianDay(LocalDate(2025, 2, 26)),
            cycle.actualPeriod?.startDate,
            "실제 생리 시작: 2025-02-26"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 2)),
            cycle.actualPeriod?.endDate,
            "실제 생리 종료: 2025-03-02"
        )

        // 생리 예정일 검증 (다음 예정일은 조회 범위 밖)
        assertEquals(0, cycle.predictDays.size, "생리 예정일 없음 (다음 예정일 03-26은 조회 범위 밖)")

        // 생리 지연일 검증
        assertEquals(0, cycle.delayTheDays, "생리 지연일: 0일")

        // 배란기 검증 (생리 시작 13-15일차)
        assertEquals(1, cycle.ovulationDays.size, "배란기 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 10)),
            cycle.ovulationDays[0]?.startDate,
            "배란기 시작: 2025-03-10"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 12)),
            cycle.ovulationDays[0]?.endDate,
            "배란기 종료: 2025-03-12"
        )

        // 가임기 검증 (생리 시작 8-19일차)
        assertEquals(1, cycle.fertileDays.size, "가임기 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 5)),
            cycle.fertileDays[0]?.startDate,
            "가임기 시작: 2025-03-05"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 16)),
            cycle.fertileDays[0]?.endDate,
            "가임기 종료: 2025-03-16"
        )

        // 주기 검증
        assertEquals(28, cycle.period, "주기: 28일")
    }

    /**
     * TC-01-04: 1일 조회 (과거)
     *
     * 테스트 목적: Period 2의 배란기 중 특정 날짜 1일만 조회할 때 해당 날짜의 상태가 정확한지 검증
     *
     * 조회 기간: 2025-02-10 ~ 2025-02-10 (1일, Period 2의 배란기)
     *
     * 예상 결과: 주기 개수 2개
     *
     * 주기 1 (pk=2):
     * - 실제 생리 기록: 2025-01-29 ~ 2025-02-02 (두 번째 생리)
     * - 생리 예정일들: 없음 (Period 3는 실제 기록)
     * - 생리 지연일: 없음
     * - 배란기: 2025-02-10 ~ 2025-02-12 (조회일 포함)
     * - 가임기: 2025-02-05 ~ 2025-02-16 (조회일 포함)
     * - 주기: 28일
     *
     * 주기 2 (pk=3):
     * - 실제 생리 기록: 2025-02-26 ~ 2025-03-02 (세 번째 생리)
     * - 생리 예정일들: 없음
     * - 생리 지연일: 없음
     * - 배란기: 없음 (조회 범위 밖)
     * - 가임기: 없음 (조회 범위 밖)
     * - 주기: 28일
     */
    @Test
    fun testTC_01_04_singleDayPastQuery() = runTest {
        val repository = createRepository()

        val searchFrom = LocalDate(2025, 2, 10)
        val searchTo = LocalDate(2025, 2, 10)
        val today = LocalDate(2025, 3, 15)

        val result = PeriodCalculator.calculateCycleInfo(
            repository,
            DateUtils.toJulianDay(searchFrom),
            DateUtils.toJulianDay(searchTo),
            DateUtils.toJulianDay(today)
        )

        // 검증
        assertEquals(2, result.size, "주기 개수: 2개")

        // pk=2 주기 (Period 2): 배란기/가임기 조회일 포함
        val cycle2 = result.find { it.pk == "2" }!!

        assertEquals("2", cycle2.pk, "pk=2 (두 번째 생리)")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 1, 29)),
            cycle2.actualPeriod?.startDate,
            "실제 생리 시작: 2025-01-29"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 2)),
            cycle2.actualPeriod?.endDate,
            "실제 생리 종료: 2025-02-02"
        )

        assertEquals(0, cycle2.predictDays.size, "생리 예정일 없음 (Period 3는 실제 기록)")
        assertEquals(0, cycle2.delayTheDays, "생리 지연일: 0일")

        // 배란기 검증
        assertEquals(1, cycle2.ovulationDays.size, "배란기 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 10)),
            cycle2.ovulationDays[0]?.startDate,
            "배란기 시작: 2025-02-10"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 12)),
            cycle2.ovulationDays[0]?.endDate,
            "배란기 종료: 2025-02-12"
        )

        // 가임기 검증
        assertEquals(1, cycle2.fertileDays.size, "가임기 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 5)),
            cycle2.fertileDays[0]?.startDate,
            "가임기 시작: 2025-02-05"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 16)),
            cycle2.fertileDays[0]?.endDate,
            "가임기 종료: 2025-02-16"
        )

        assertEquals(28, cycle2.period, "주기: 28일")

        // pk=3 주기 (Period 3): 배란기/가임기 모두 조회 범위 밖
        val cycle3 = result.find { it.pk == "3" }!!

        assertEquals("3", cycle3.pk, "pk=3 (세 번째 생리)")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 26)),
            cycle3.actualPeriod?.startDate,
            "실제 생리 시작: 2025-02-26"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 2)),
            cycle3.actualPeriod?.endDate,
            "실제 생리 종료: 2025-03-02"
        )

        assertEquals(0, cycle3.predictDays.size, "생리 예정일 없음")
        assertEquals(0, cycle3.delayTheDays, "생리 지연일: 0일")
        assertEquals(0, cycle3.ovulationDays.size, "배란기 없음 (조회 범위 밖)")
        assertEquals(0, cycle3.fertileDays.size, "가임기 없음 (조회 범위 밖)")
        assertEquals(28, cycle3.period, "주기: 28일")
    }

    /**
     * TC-01-05: 1주일 조회 (과거)
     *
     * 테스트 목적: Period 2의 배란기/가임기가 포함된 1주일 기간 조회 시 해당 주의 상태 변화가 정확히 표시되는지 검증
     *
     * 조회 기간: 2025-02-08 ~ 2025-02-14 (7일, Period 2의 배란기/가임기 포함)
     *
     * 예상 결과: 주기 개수 2개
     *
     * 주기 1 (pk=2):
     * - 실제 생리 기록: 2025-01-29 ~ 2025-02-02 (두 번째 생리)
     * - 생리 예정일들: 없음 (Period 3는 실제 기록)
     * - 생리 지연일: 없음
     * - 배란기: 2025-02-10 ~ 2025-02-12 (조회 범위와 부분 겹침)
     * - 가임기: 2025-02-05 ~ 2025-02-16 (조회 범위와 부분 겹침)
     * - 주기: 28일
     *
     * 주기 2 (pk=3):
     * - 실제 생리 기록: 2025-02-26 ~ 2025-03-02 (세 번째 생리)
     * - 생리 예정일들: 없음
     * - 생리 지연일: 없음
     * - 배란기: 없음 (조회 범위 밖)
     * - 가임기: 없음 (조회 범위 밖)
     * - 주기: 28일
     */
    @Test
    fun testTC_01_05_oneWeekPastQuery() = runTest {
        val repository = createRepository()

        val searchFrom = LocalDate(2025, 2, 8)
        val searchTo = LocalDate(2025, 2, 14)
        val today = LocalDate(2025, 3, 15)

        val result = PeriodCalculator.calculateCycleInfo(
            repository,
            DateUtils.toJulianDay(searchFrom),
            DateUtils.toJulianDay(searchTo),
            DateUtils.toJulianDay(today)
        )

        // 검증
        assertEquals(2, result.size, "주기 개수: 2개")

        // pk=2 주기 (Period 2): 배란기/가임기가 조회 범위와 부분 겹침
        val cycle2 = result.find { it.pk == "2" }!!

        assertEquals("2", cycle2.pk, "pk=2 (두 번째 생리)")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 1, 29)),
            cycle2.actualPeriod?.startDate,
            "실제 생리 시작: 2025-01-29"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 2)),
            cycle2.actualPeriod?.endDate,
            "실제 생리 종료: 2025-02-02"
        )

        assertEquals(0, cycle2.predictDays.size, "생리 예정일 없음 (Period 3는 실제 기록)")
        assertEquals(0, cycle2.delayTheDays, "생리 지연일: 0일")

        // 배란기 검증
        assertEquals(1, cycle2.ovulationDays.size, "배란기 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 10)),
            cycle2.ovulationDays[0]?.startDate,
            "배란기 시작: 2025-02-10"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 12)),
            cycle2.ovulationDays[0]?.endDate,
            "배란기 종료: 2025-02-12"
        )

        // 가임기 검증
        assertEquals(1, cycle2.fertileDays.size, "가임기 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 5)),
            cycle2.fertileDays[0]?.startDate,
            "가임기 시작: 2025-02-05 (전체 범위 반환)"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 16)),
            cycle2.fertileDays[0]?.endDate,
            "가임기 종료: 2025-02-16"
        )

        assertEquals(28, cycle2.period, "주기: 28일")

        // pk=3 주기 (Period 3): 배란기/가임기 모두 조회 범위 밖
        val cycle3 = result.find { it.pk == "3" }!!

        assertEquals("3", cycle3.pk, "pk=3 (세 번째 생리)")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 26)),
            cycle3.actualPeriod?.startDate,
            "실제 생리 시작: 2025-02-26"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 2)),
            cycle3.actualPeriod?.endDate,
            "실제 생리 종료: 2025-03-02"
        )

        assertEquals(0, cycle3.predictDays.size, "생리 예정일 없음")
        assertEquals(0, cycle3.delayTheDays, "생리 지연일: 0일")
        assertEquals(0, cycle3.ovulationDays.size, "배란기 없음 (조회 범위 밖)")
        assertEquals(0, cycle3.fertileDays.size, "가임기 없음 (조회 범위 밖)")
        assertEquals(28, cycle3.period, "주기: 28일")
    }

    /**
     * TC-01-06: 1개월 조회 (과거)
     *
     * 테스트 목적: Period 2 전체 주기를 조회하여 실제 생리, 배란기, 가임기가 모두 정확히 표시되는지 검증
     *
     * 조회 기간: 2025-01-29 ~ 2025-02-25 (Period 2 전체 주기, 28일)
     *
     * 예상 결과: 주기 개수 2개
     *
     * 주기 1 (pk=2):
     * - 실제 생리 기록: 2025-01-29 ~ 2025-02-02 (두 번째 생리)
     * - 생리 예정일들: 없음 (Period 3는 실제 기록)
     * - 생리 지연일: 없음
     * - 배란기: 2025-02-10 ~ 2025-02-12 (생리 시작 13-15일차)
     * - 가임기: 2025-02-05 ~ 2025-02-16 (생리 시작 8-19일차)
     * - 주기: 28일
     *
     * 주기 2 (pk=3):
     * - 실제 생리 기록: 2025-02-26 ~ 2025-03-02 (세 번째 생리)
     * - 생리 예정일들: 없음
     * - 생리 지연일: 없음
     * - 배란기: 없음 (조회 범위 밖)
     * - 가임기: 없음 (조회 범위 밖)
     * - 주기: 28일
     */
    @Test
    fun testTC_01_06_oneMonthPastQuery() = runTest {
        val repository = createRepository()

        val searchFrom = LocalDate(2025, 1, 29)
        val searchTo = LocalDate(2025, 2, 25)
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

        // 실제 생리 기록 검증 (Period 2)
        assertEquals("2", cycle2.pk, "pk=2 (두 번째 생리)")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 1, 29)),
            cycle2.actualPeriod?.startDate,
            "실제 생리 시작: 2025-01-29"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 2)),
            cycle2.actualPeriod?.endDate,
            "실제 생리 종료: 2025-02-02"
        )

        // 생리 예정일 검증 (Period 3는 실제 기록이므로 예정일에 포함 안됨)
        assertEquals(0, cycle2.predictDays.size, "생리 예정일 없음 (Period 3는 실제 기록)")

        // 생리 지연일 검증
        assertEquals(0, cycle2.delayTheDays, "생리 지연일: 0일")

        // 배란기 검증 (2025-02-10 ~ 2025-02-12, Period 2 기준 13-15일차)
        assertEquals(1, cycle2.ovulationDays.size, "배란기 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 10)),
            cycle2.ovulationDays[0]?.startDate,
            "배란기 시작: 2025-02-10 (Period 2 시작 + 12일)"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 12)),
            cycle2.ovulationDays[0]?.endDate,
            "배란기 종료: 2025-02-12"
        )

        // 가임기 검증 (2025-02-05 ~ 2025-02-16, Period 2 기준 8-19일차)
        assertEquals(1, cycle2.fertileDays.size, "가임기 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 5)),
            cycle2.fertileDays[0]?.startDate,
            "가임기 시작: 2025-02-05 (Period 2 시작 + 7일)"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 16)),
            cycle2.fertileDays[0]?.endDate,
            "가임기 종료: 2025-02-16"
        )

        // 주기 검증
        assertEquals(28, cycle2.period, "주기: 28일")

        // pk=3 주기 (Period 3): 배란기/가임기 모두 조회 범위 밖
        val cycle3 = result.find { it.pk == "3" }!!

        // 실제 생리 기록 검증 (Period 3)
        assertEquals("3", cycle3.pk, "pk=3 (세 번째 생리)")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 26)),
            cycle3.actualPeriod?.startDate,
            "실제 생리 시작: 2025-02-26"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 2)),
            cycle3.actualPeriod?.endDate,
            "실제 생리 종료: 2025-03-02"
        )

        // 생리 예정일 검증 (조회 범위 밖)
        assertEquals(0, cycle3.predictDays.size, "생리 예정일 없음 (조회 범위 밖)")

        // 생리 지연일 검증
        assertEquals(0, cycle3.delayTheDays, "생리 지연일: 0일")

        // 배란기 검증 (조회 범위 밖)
        assertEquals(0, cycle3.ovulationDays.size, "배란기 없음 (조회 범위 밖)")

        // 가임기 검증 (조회 범위 밖)
        assertEquals(0, cycle3.fertileDays.size, "가임기 없음 (조회 범위 밖)")

        // 주기 검증
        assertEquals(28, cycle3.period, "주기: 28일")
    }

    /**
     * TC-01-07: 생리 기간 경계 조회
     *
     * 테스트 목적: 조회 기간이 생리 종료일과 다음 생리 시작일에 걸쳐있을 때 두 주기가 모두 반환되는지 검증
     *
     * 조회 기간: 2025-02-23 ~ 2025-03-01 (Period 2 종료 후 ~ Period 3 진행 중)
     *
     * 예상 결과: 주기 개수 2개
     *
     * 주기 1 (pk=2):
     * - 실제 생리 기록: 2025-01-29 ~ 2025-02-02 (두 번째 생리)
     * - 생리 예정일들: 없음 (Period 3는 실제 기록)
     * - 생리 지연일: 없음
     * - 배란기: 없음 (조회 범위 밖)
     * - 가임기: 없음 (조회 범위 밖)
     * - 주기: 28일
     *
     * 주기 2 (pk=3):
     * - 실제 생리 기록: 2025-02-26 ~ 2025-03-02 (세 번째 생리, 조회 범위와 부분 겹침)
     * - 생리 예정일들: 없음
     * - 생리 지연일: 없음
     * - 배란기: 없음 (조회 범위 밖)
     * - 가임기: 없음 (조회 범위 밖)
     * - 주기: 28일
     */
    @Test
    fun testTC_01_07_periodBoundaryQuery() = runTest {
        val repository = createRepository()

        val searchFrom = LocalDate(2025, 2, 23)
        val searchTo = LocalDate(2025, 3, 1)
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
            DateUtils.toJulianDay(LocalDate(2025, 1, 29)),
            cycle2.actualPeriod?.startDate,
            "실제 생리 시작: 2025-01-29"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 2)),
            cycle2.actualPeriod?.endDate,
            "실제 생리 종료: 2025-02-02"
        )

        assertEquals(0, cycle2.predictDays.size, "생리 예정일 없음 (Period 3는 실제 기록)")
        assertEquals(0, cycle2.delayTheDays, "생리 지연일: 0일")
        assertEquals(0, cycle2.ovulationDays.size, "배란기 없음 (조회 범위 밖)")
        assertEquals(0, cycle2.fertileDays.size, "가임기 없음 (조회 범위 밖)")
        assertEquals(28, cycle2.period, "주기: 28일")

        // pk=3 주기 (Period 3): 생리 진행 중, 조회 범위와 부분 겹침
        val cycle3 = result.find { it.pk == "3" }!!

        assertEquals("3", cycle3.pk, "pk=3 (세 번째 생리)")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 26)),
            cycle3.actualPeriod?.startDate,
            "실제 생리 시작: 2025-02-26"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 2)),
            cycle3.actualPeriod?.endDate,
            "실제 생리 종료: 2025-03-02"
        )

        assertEquals(0, cycle3.predictDays.size, "생리 예정일 없음")
        assertEquals(0, cycle3.delayTheDays, "생리 지연일: 0일")
        assertEquals(0, cycle3.ovulationDays.size, "배란기 없음 (조회 범위 밖)")
        assertEquals(0, cycle3.fertileDays.size, "가임기 없음 (조회 범위 밖)")
        assertEquals(28, cycle3.period, "주기: 28일")
    }

    /**
     * TC-01-08: 3개월 조회
     *
     * 테스트 목적: 장기간 조회 시 여러 주기의 예측이 반복적으로 정확한지 검증
     *
     * 조회 기간: 2025-03-01 ~ 2025-05-31 (3개월)
     *
     * 예상 결과:
     * - 실제 생리 기록: 2025-02-26 ~ 2025-03-02 (pk=3, 마지막 생리, 부분 포함)
     * - 생리 예정일들:
     *   - 2025-03-26 ~ 2025-03-30
     *   - 2025-04-23 ~ 2025-04-27
     *   - 2025-05-21 ~ 2025-05-25
     * - 배란기들:
     *   - 2025-03-10 ~ 2025-03-12 (생리 시작 13-15일차)
     *   - 2025-04-07 ~ 2025-04-09
     *   - 2025-05-05 ~ 2025-05-07
     * - 가임기들:
     *   - 2025-03-05 ~ 2025-03-16 (생리 시작 8-19일차)
     *   - 2025-04-02 ~ 2025-04-13
     *   - 2025-04-30 ~ 2025-05-11
     * - 주기: 28일
     */
    @Test
    fun testTC_01_08_threeMonthQuery() = runTest {
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

        // 실제 생리 기록 검증 (부분 포함)
        assertEquals("3", cycle.pk, "pk=3 (마지막 생리)")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 2, 26)),
            cycle.actualPeriod?.startDate,
            "실제 생리 시작: 2025-02-26"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 2)),
            cycle.actualPeriod?.endDate,
            "실제 생리 종료: 2025-03-02"
        )

        // 생리 예정일 검증 (3개)
        assertEquals(3, cycle.predictDays.size, "생리 예정일 3개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 26)),
            cycle.predictDays[0].startDate,
            "첫 번째 예정일 시작: 2025-03-26"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 30)),
            cycle.predictDays[0].endDate,
            "첫 번째 예정일 종료: 2025-03-30"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 23)),
            cycle.predictDays[1].startDate,
            "두 번째 예정일 시작: 2025-04-23"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 27)),
            cycle.predictDays[1].endDate,
            "두 번째 예정일 종료: 2025-04-27"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 5, 21)),
            cycle.predictDays[2].startDate,
            "세 번째 예정일 시작: 2025-05-21"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 5, 25)),
            cycle.predictDays[2].endDate,
            "세 번째 예정일 종료: 2025-05-25"
        )

        // 배란기 검증 (3개)
        assertEquals(3, cycle.ovulationDays.size, "배란기 3개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 10)),
            cycle.ovulationDays[0]?.startDate,
            "첫 번째 배란기 시작: 2025-03-10"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 12)),
            cycle.ovulationDays[0]?.endDate,
            "첫 번째 배란기 종료: 2025-03-12"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 7)),
            cycle.ovulationDays[1]?.startDate,
            "두 번째 배란기 시작: 2025-04-07"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 9)),
            cycle.ovulationDays[1]?.endDate,
            "두 번째 배란기 종료: 2025-04-09"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 5, 5)),
            cycle.ovulationDays[2]?.startDate,
            "세 번째 배란기 시작: 2025-05-05"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 5, 7)),
            cycle.ovulationDays[2]?.endDate,
            "세 번째 배란기 종료: 2025-05-07"
        )

        // 가임기 검증 (4개, 마지막 가임기가 조회 범위와 부분 겹침)
        assertEquals(4, cycle.fertileDays.size, "가임기 4개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 5)),
            cycle.fertileDays[0]?.startDate,
            "첫 번째 가임기 시작: 2025-03-05"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 16)),
            cycle.fertileDays[0]?.endDate,
            "첫 번째 가임기 종료: 2025-03-16"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 2)),
            cycle.fertileDays[1]?.startDate,
            "두 번째 가임기 시작: 2025-04-02"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 13)),
            cycle.fertileDays[1]?.endDate,
            "두 번째 가임기 종료: 2025-04-13"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 30)),
            cycle.fertileDays[2]?.startDate,
            "세 번째 가임기 시작: 2025-04-30"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 5, 11)),
            cycle.fertileDays[2]?.endDate,
            "세 번째 가임기 종료: 2025-05-11"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 5, 28)),
            cycle.fertileDays[3]?.startDate,
            "네 번째 가임기 시작: 2025-05-28 (조회 범위와 부분 겹침)"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 6, 8)),
            cycle.fertileDays[3]?.endDate,
            "네 번째 가임기 종료: 2025-06-08"
        )

        // 주기 검증
        assertEquals(28, cycle.period, "주기: 28일")
    }

    /**
     * TC-01-09: 생리 예정 기간 중 조회
     *
     * 테스트 목적: 조회 기간이 생리 예정 기간을 포함할 때 정확히 표시되는지 검증
     *
     * 조회 기간: 2025-03-26 ~ 2025-03-30
     *
     * 예상 결과:
     * - 실제 생리 기록: 2025-02-26 ~ 2025-03-02 (pk=3, 마지막 생리)
     * - 생리 예정일들:
     *   - 2025-03-26 ~ 2025-03-30
     * - 생리 지연일: 없음
     * - 배란기들: 없음 (조회 기간 내 배란기 없음)
     * - 가임기들: 없음 (조회 기간 내 가임기 없음)
     * - 주기: 28일
     */
    @Test
    fun testTC_01_09_predictedPeriodQuery() = runTest {
        val repository = createRepository()

        val searchFrom = LocalDate(2025, 3, 26)
        val searchTo = LocalDate(2025, 3, 30)
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
            DateUtils.toJulianDay(LocalDate(2025, 2, 26)),
            cycle.actualPeriod?.startDate,
            "실제 생리 시작: 2025-02-26"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 2)),
            cycle.actualPeriod?.endDate,
            "실제 생리 종료: 2025-03-02"
        )

        // 생리 예정일 검증
        assertEquals(1, cycle.predictDays.size, "생리 예정일 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 26)),
            cycle.predictDays[0].startDate,
            "예정일 시작: 2025-03-26"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 30)),
            cycle.predictDays[0].endDate,
            "예정일 종료: 2025-03-30"
        )

        // 생리 지연일 검증
        assertEquals(0, cycle.delayTheDays, "생리 지연일: 0일")

        // 배란기 검증 (조회 기간 내 없음)
        assertEquals(0, cycle.ovulationDays.size, "배란기 없음 (조회 기간 내 없음)")

        // 가임기 검증 (조회 기간 내 없음)
        assertEquals(0, cycle.fertileDays.size, "가임기 없음 (조회 기간 내 없음)")

        // 주기 검증
        assertEquals(28, cycle.period, "주기: 28일")
    }

    /**
     * TC-01-10: 배란기 중 조회
     *
     * 테스트 목적: 조회 기간이 배란기를 포함할 때 정확히 표시되는지 검증
     *
     * 조회 기간: 2025-03-10 ~ 2025-03-12
     *
     * 예상 결과:
     * - 실제 생리 기록: 2025-02-26 ~ 2025-03-02 (pk=3, 마지막 생리)
     * - 생리 예정일들: 없음 (조회 기간 내 생리 예정일 없음)
     * - 생리 지연일: 없음
     * - 배란기들: 2025-03-10 ~ 2025-03-12
     * - 가임기들: 2025-03-05 ~ 2025-03-16 (조회 범위 03-10 ~ 03-12와 부분 겹침)
     * - 주기: 28일
     */
    @Test
    fun testTC_01_10_ovulationPeriodQuery() = runTest {
        val repository = createRepository()

        val searchFrom = LocalDate(2025, 3, 10)
        val searchTo = LocalDate(2025, 3, 12)
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
            DateUtils.toJulianDay(LocalDate(2025, 2, 26)),
            cycle.actualPeriod?.startDate,
            "실제 생리 시작: 2025-02-26"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 2)),
            cycle.actualPeriod?.endDate,
            "실제 생리 종료: 2025-03-02"
        )

        // 생리 예정일 검증
        assertEquals(0, cycle.predictDays.size, "생리 예정일 없음 (조회 기간 내 없음)")

        // 생리 지연일 검증
        assertEquals(0, cycle.delayTheDays, "생리 지연일: 0일")

        // 배란기 검증
        assertEquals(1, cycle.ovulationDays.size, "배란기 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 10)),
            cycle.ovulationDays[0]?.startDate,
            "배란기 시작: 2025-03-10"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 12)),
            cycle.ovulationDays[0]?.endDate,
            "배란기 종료: 2025-03-12"
        )

        // 가임기 검증 (조회 범위와 부분 겹침)
        assertEquals(1, cycle.fertileDays.size, "가임기 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 5)),
            cycle.fertileDays[0]?.startDate,
            "가임기 시작: 2025-03-05 (전체 범위 반환)"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 16)),
            cycle.fertileDays[0]?.endDate,
            "가임기 종료: 2025-03-16"
        )

        // 주기 검증
        assertEquals(28, cycle.period, "주기: 28일")
    }

    /**
     * TC-01-11: 가임기 중 조회
     *
     * 테스트 목적: 조회 기간이 가임기를 포함할 때 정확히 표시되는지 검증
     *
     * 조회 기간: 2025-03-05 ~ 2025-03-09
     *
     * 예상 결과:
     * - 실제 생리 기록: 2025-02-26 ~ 2025-03-02 (pk=3, 마지막 생리)
     * - 생리 예정일들: 없음 (조회 기간 내 생리 없음)
     * - 생리 지연일: 없음
     * - 배란기들: 없음 (배란기는 2025-03-10부터 시작)
     * - 가임기들: 2025-03-05 ~ 2025-03-16 (조회 범위 03-05 ~ 03-09와 부분 겹침)
     * - 주기: 28일
     */
    @Test
    fun testTC_01_11_fertilePeriodQuery() = runTest {
        val repository = createRepository()

        val searchFrom = LocalDate(2025, 3, 5)
        val searchTo = LocalDate(2025, 3, 9)
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
            DateUtils.toJulianDay(LocalDate(2025, 2, 26)),
            cycle.actualPeriod?.startDate,
            "실제 생리 시작: 2025-02-26"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 2)),
            cycle.actualPeriod?.endDate,
            "실제 생리 종료: 2025-03-02"
        )

        // 생리 예정일 검증
        assertEquals(0, cycle.predictDays.size, "생리 예정일 없음 (조회 기간 내 없음)")

        // 생리 지연일 검증
        assertEquals(0, cycle.delayTheDays, "생리 지연일: 0일")

        // 배란기 검증 (조회 기간 내 없음)
        assertEquals(0, cycle.ovulationDays.size, "배란기 없음 (배란기는 03-10부터 시작)")

        // 가임기 검증 (조회 범위와 부분 겹침)
        assertEquals(1, cycle.fertileDays.size, "가임기 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 5)),
            cycle.fertileDays[0]?.startDate,
            "가임기 시작: 2025-03-05 (전체 범위 반환)"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 16)),
            cycle.fertileDays[0]?.endDate,
            "가임기 종료: 2025-03-16"
        )

        // 주기 검증
        assertEquals(28, cycle.period, "주기: 28일")
    }

    /**
     * TC-01-12: 생리 지연 1-7일 (예정일 뒤로 미룸)
     *
     * 테스트 목적: 생리 예정일이 지나고 1-7일 지연되었을 때 지연 기간이 표시되고, 다음 예정일이 지연 다음날부터 시작하는지 검증
     *
     * 조회 기간: 2025-03-20 ~ 2025-04-10
     * 오늘 날짜: 2025-04-01 (생리 예정일 03-26으로부터 7일 지연)
     *
     * 예상 결과:
     * - 실제 생리 기록: 2025-02-26 ~ 2025-03-02 (pk=3, 마지막 생리, 부분 포함)
     * - 생리 지연일: 7일
     * - 지연 기간: 2025-03-26 ~ 2025-04-01 (예정일부터 오늘까지)
     * - 생리 예정일들: 2025-04-02 ~ 2025-04-06 (지연 다음날부터 5일간, 지연으로 밀린 예정일)
     * - 배란기들: 없음 (조회 범위 밖)
     * - 가임기들: 없음 (조회 범위 밖)
     * - 주기: 28일
     */
    @Test
    fun testTC_01_12_delay1To7Days() = runTest {
        val repository = createRepository()

        val searchFrom = LocalDate(2025, 3, 20)
        val searchTo = LocalDate(2025, 4, 10)
        val today = LocalDate(2025, 4, 1) // 7일 지연

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
            DateUtils.toJulianDay(LocalDate(2025, 2, 26)),
            cycle.actualPeriod?.startDate,
            "실제 생리 시작: 2025-02-26"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 2)),
            cycle.actualPeriod?.endDate,
            "실제 생리 종료: 2025-03-02"
        )

        // 생리 지연일 검증
        assertEquals(7, cycle.delayTheDays, "생리 지연일: 7일")

        // 지연 기간 검증 (2025-03-26 ~ 2025-04-01)
        assertTrue(cycle.delayDay != null, "지연 기간 있음")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 26)),
            cycle.delayDay?.startDate,
            "지연 시작: 2025-03-26"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 1)),
            cycle.delayDay?.endDate,
            "지연 종료: 2025-04-01"
        )

        // 생리 예정일 검증 (지연 다음날부터 시작)
        assertEquals(1, cycle.predictDays.size, "생리 예정일 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 2)),
            cycle.predictDays[0].startDate,
            "예정일 시작: 2025-04-02 (지연 다음날)"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 6)),
            cycle.predictDays[0].endDate,
            "예정일 종료: 2025-04-06"
        )

        // 배란기 검증 (조회 범위 밖)
        assertEquals(0, cycle.ovulationDays.size, "배란기 없음 (조회 범위 밖)")

        // 가임기 검증 (조회 범위 밖)
        assertEquals(0, cycle.fertileDays.size, "가임기 없음 (조회 범위 밖)")

        // 주기 검증
        assertEquals(28, cycle.period, "주기: 28일")
    }

    /**
     * TC-01-13: 생리 지연 8일 이상 (예정일 표시 안 함, 병원 권장)
     *
     * 테스트 목적: 생리 예정일이 지나고 8일 이상 지연되었을 때 예정일과 지연 기간을 표시하지 않는지 검증 (병원 진료 권장 상태)
     *
     * 조회 기간: 2025-03-20 ~ 2025-05-10
     * 오늘 날짜: 2025-04-02 (생리 예정일 03-26으로부터 8일 지연)
     *
     * 예상 결과:
     * - 실제 생리 기록: 2025-02-26 ~ 2025-03-02 (pk=3, 마지막 생리, 부분 포함)
     * - 생리 지연일: 8일
     * - 지연 기간: 없음 (8일 이상이면 지연 기간 표시 안 함)
     * - 생리 예정일들: 없음 (8일 이상 지연 시 예정일 표시 안 함)
     * - 배란기들: 없음 (조회 범위 밖)
     * - 가임기들: 없음 (조회 범위 밖)
     * - 주기: 28일
     */
    @Test
    fun testTC_01_13_delay8OrMoreDays() = runTest {
        val repository = createRepository()

        val searchFrom = LocalDate(2025, 3, 20)
        val searchTo = LocalDate(2025, 5, 10)
        val today = LocalDate(2025, 4, 2) // 8일 지연 (병원 권장)

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
            DateUtils.toJulianDay(LocalDate(2025, 2, 26)),
            cycle.actualPeriod?.startDate,
            "실제 생리 시작: 2025-02-26"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 2)),
            cycle.actualPeriod?.endDate,
            "실제 생리 종료: 2025-03-02"
        )

        // 생리 지연일 검증 (8일로 계산됨)
        assertEquals(8, cycle.delayTheDays, "생리 지연일: 8일")

        // 지연 기간 검증 (8일 이상이면 표시 안 함)
        assertTrue(cycle.delayDay == null, "지연 기간 없음 (8일 이상)")

        // 생리 예정일 검증 (8일 이상이면 표시 안 함)
        assertEquals(0, cycle.predictDays.size, "생리 예정일 없음 (8일 이상 지연)")

        // 배란기 검증 (조회 범위 밖)
        assertEquals(0, cycle.ovulationDays.size, "배란기 없음 (조회 범위 밖)")

        // 가임기 검증 (조회 범위 밖)
        assertEquals(0, cycle.fertileDays.size, "가임기 없음 (조회 범위 밖)")

        // 주기 검증
        assertEquals(28, cycle.period, "주기: 28일")
    }
}
