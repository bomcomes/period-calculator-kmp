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
 * 불규칙한 주기 테스트 케이스
 *
 * 테스트 문서: test-cases/docs/04-irregular-cycle.md
 *
 * 공통 입력 조건:
 * - 생리 기록:
 *   - Period 1: 2025-01-01 ~ 2025-01-05
 *   - Period 2: 2025-01-26 ~ 2025-01-30 (25일 주기)
 *   - Period 3: 2025-03-01 ~ 2025-03-05 (34일 주기)
 *   - Period 4: 2025-04-04 ~ 2025-04-08 (34일 주기)
 * - 배란 테스트 기록: 없음
 * - 배란일 직접 입력: 없음
 * - 피임약 패키지 시작일: 없음
 * - 오늘 날짜: 2025-04-20 (지연 테스트 제외)
 * - 생리 주기 설정:
 *   - 평균 주기: 31일 (실제 계산: (25+34+34)/3 = 31)
 *   - 평균 기간: 5일
 *   - 자동 계산 사용: true
 * - 피임약 설정: 사용 안함
 *
 * **주요 차이점**: 불규칙한 주기로 평균 31일은 26-32일 범위 밖
 * - 34일 주기는 26-32일 범위를 벗어나므로 다른 공식 사용
 * - 배란기: 주기 - 14~16일차 (31일 기준: 15-17일차)
 * - 가임기: 주기 - 11~19일차 (31일 기준: 12-20일차)
 * - 자동 계산 모드 사용
 */
class IrregularCycleTest {

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
            // Period 3: 2025-03-01 ~ 2025-03-05 (34일 주기)
            addPeriod(
                PeriodRecord(
                    pk = "3",
                    startDate = DateUtils.toJulianDay(LocalDate(2025, 3, 1)),
                    endDate = DateUtils.toJulianDay(LocalDate(2025, 3, 5))
                )
            )
            // Period 4: 2025-04-04 ~ 2025-04-08 (34일 주기)
            addPeriod(
                PeriodRecord(
                    pk = "4",
                    startDate = DateUtils.toJulianDay(LocalDate(2025, 4, 4)),
                    endDate = DateUtils.toJulianDay(LocalDate(2025, 4, 8))
                )
            )

            setPeriodSettings(
                PeriodSettings(
                    manualAverageCycle = 28,
                    manualAverageDay = 5,
                    autoAverageCycle = 31,
                    autoAverageDay = 5,
                    isAutoCalc = true
                )
            )
        }
    }

    // ====================================================================================
    // 그룹 1: 마지막 생리 이후 (Period 4)
    // ====================================================================================

    /**
     * TC-04-01: 1일 조회 (마지막 생리 이후)
     *
     * 테스트 목적: 마지막 생리 이후 특정 날짜 1일만 조회할 때 불규칙 주기의 평균 계산이 정확한지 검증
     *
     * 조회 기간: 2025-04-25 ~ 2025-04-25 (1일)
     *
     * 예상 결과:
     * - 실제 생리 기록: 2025-04-04 ~ 2025-04-08 (pk=4, 마지막 생리)
     * - 생리 예정일들: 없음 (조회 기간 내 생리 없음, 다음 예정일은 2025-05-05)
     * - 생리 지연일: 없음
     * - 배란기들: 없음 (조회 기간 내 배란기 없음, 04-18 ~ 04-20)
     * - 가임기들: 없음 (조회 기간 내 가임기 없음, 04-15 ~ 04-23)
     * - 주기: 31일
     */
    @Test
    fun testTC_04_01_singleDayQuery() = runTest {
        val repository = createRepository()

        val searchFrom = LocalDate(2025, 4, 25)
        val searchTo = LocalDate(2025, 4, 25)
        val today = LocalDate(2025, 4, 25)

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
        assertEquals("4", cycle.pk, "pk=4 (마지막 생리)")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 4)),
            cycle.actualPeriod?.startDate,
            "실제 생리 시작: 2025-04-04"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 8)),
            cycle.actualPeriod?.endDate,
            "실제 생리 종료: 2025-04-08"
        )

        // 생리 예정일 검증 (조회 기간 내 없음)
        assertEquals(0, cycle.predictDays.size, "생리 예정일 없음")

        // 생리 지연일 검증
        assertEquals(0, cycle.delayTheDays, "생리 지연일: 없음")

        // 배란기 검증
        assertEquals(0, cycle.ovulationDays.size, "배란기 없음 (조회 기간 내 배란기 없음)")

        // 가임기 검증
        assertEquals(0, cycle.fertileDays.size, "가임기 없음 (조회 기간 내 가임기 없음)")

        // 주기 검증
        assertEquals(31, cycle.period, "주기: 31일 (자동 계산 평균)")
    }

    /**
     * TC-04-02: 1주일 조회 (마지막 생리 이후)
     *
     * 테스트 목적: 마지막 생리 이후 1주일 기간 조회 시 불규칙 주기의 상태 변화가 정확히 표시되는지 검증
     *
     * 조회 기간: 2025-04-15 ~ 2025-04-21
     *
     * 예상 결과:
     * - 실제 생리 기록: 2025-04-04 ~ 2025-04-08 (pk=4, 마지막 생리)
     * - 생리 예정일들: 없음 (다음 예정일은 2025-05-05부터, 조회 범위 밖)
     * - 생리 지연일: 없음
     * - 배란기들: 2025-04-16 ~ 2025-04-18 (조회 범위와 부분 겹침)
     * - 가임기들: 2025-04-11 ~ 2025-04-22 (조회 범위와 부분 겹침, 04-15 ~ 04-21만 겹침)
     * - 주기: 31일
     */
    @Test
    fun testTC_04_02_oneWeekQuery() = runTest {
        val repository = createRepository()

        val searchFrom = LocalDate(2025, 4, 15)
        val searchTo = LocalDate(2025, 4, 21)
        val today = LocalDate(2025, 4, 20)

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
        assertEquals("4", cycle.pk, "pk=4 (마지막 생리)")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 4)),
            cycle.actualPeriod?.startDate,
            "실제 생리 시작: 2025-04-04"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 8)),
            cycle.actualPeriod?.endDate,
            "실제 생리 종료: 2025-04-08"
        )

        // 생리 예정일 검증 (조회 범위 밖)
        assertEquals(0, cycle.predictDays.size, "생리 예정일 없음")

        // 생리 지연일 검증
        assertEquals(0, cycle.delayTheDays, "생리 지연일: 없음")

        // 배란기 검증 (조회 범위와 겹침)
        assertEquals(1, cycle.ovulationDays.size, "배란기 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 16)),
            cycle.ovulationDays[0].startDate,
            "배란기 시작: 2025-04-16"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 18)),
            cycle.ovulationDays[0].endDate,
            "배란기 종료: 2025-04-18"
        )

        // 가임기 검증 (조회 범위와 부분 겹침)
        assertEquals(1, cycle.fertileDays.size, "가임기 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 11)),
            cycle.fertileDays[0].startDate,
            "가임기 시작: 2025-04-11"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 22)),
            cycle.fertileDays[0].endDate,
            "가임기 종료: 2025-04-22"
        )

        // 주기 검증
        assertEquals(31, cycle.period, "주기: 31일 (자동 계산 평균)")
    }

    /**
     * TC-04-03: 1개월 조회 (마지막 생리 이후)
     *
     * 테스트 목적: 마지막 생리(Period 4) 전체 주기를 조회하여 실제 생리, 배란기, 가임기가 모두 정확히 표시되는지 검증
     *
     * 조회 기간: 2025-04-04 ~ 2025-04-28
     *
     * 예상 결과:
     * - 실제 생리 기록: 2025-04-04 ~ 2025-04-08 (pk=4, 마지막 생리)
     * - 생리 예정일들: 없음 (다음 예정일 2025-05-05는 조회 범위 밖)
     * - 생리 지연일: 없음
     * - 배란기들: 2025-04-16 ~ 2025-04-18 (생리 시작 13-15일차)
     * - 가임기들: 2025-04-11 ~ 2025-04-22 (생리 시작 8-19일차)
     * - 주기: 34일
     */
    @Test
    fun testTC_04_03_oneMonthQuery() = runTest {
        val repository = createRepository()

        val searchFrom = LocalDate(2025, 4, 4)
        val searchTo = LocalDate(2025, 4, 27)
        val today = LocalDate(2025, 4, 20)

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
        assertEquals("4", cycle.pk, "pk=4 (마지막 생리)")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 4)),
            cycle.actualPeriod?.startDate,
            "실제 생리 시작: 2025-04-04"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 8)),
            cycle.actualPeriod?.endDate,
            "실제 생리 종료: 2025-04-08"
        )

        // 생리 예정일 검증
        assertEquals(0, cycle.predictDays.size, "생리 예정일 없음 (조회 범위 밖)")

        // 생리 지연일 검증
        assertEquals(0, cycle.delayTheDays, "생리 지연일: 없음")

        // 배란기 검증 (13-15일차)
        assertEquals(1, cycle.ovulationDays.size, "배란기 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 16)),
            cycle.ovulationDays[0].startDate,
            "배란기 시작: 2025-04-16 (13일차)"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 18)),
            cycle.ovulationDays[0].endDate,
            "배란기 종료: 2025-04-18 (15일차)"
        )

        // 가임기 검증 (8-19일차)
        assertEquals(1, cycle.fertileDays.size, "가임기 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 11)),
            cycle.fertileDays[0].startDate,
            "가임기 시작: 2025-04-11 (8일차)"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 22)),
            cycle.fertileDays[0].endDate,
            "가임기 종료: 2025-04-22 (19일차)"
        )

        // 주기 검증
        assertEquals(31, cycle.period, "주기: 31일 (자동 계산 평균)")
    }

    // ====================================================================================
    // 그룹 2: 과거 기록 (Period 3)
    // ====================================================================================

    /**
     * TC-04-04: 1일 조회 (과거)
     *
     * 테스트 목적: 과거 기록된 주기의 배란기 중 특정 날짜를 조회할 때 배란기/가임기가 정확히 표시되는지 검증 (불규칙 주기)
     *
     * 조회 기간: 2025-03-11 ~ 2025-03-11
     *
     * 예상 결과:
     * 주기 개수: 2개
     *
     * 주기 1 (pk=3):
     * - 실제 생리 기록: 2025-03-01 ~ 2025-03-05 (세 번째 생리)
     * - 생리 예정일들: 없음 (Period 4는 실제 기록)
     * - 생리 지연일: 없음
     * - 배란기들: 없음 (2025-03-19 ~ 2025-03-21, 조회 범위 밖)
     * - 가임기들: 없음 (2025-03-16 ~ 2025-03-24, 조회 범위 밖)
     * - 주기: 34일
     *
     * 주기 2 (pk=4):
     * - 실제 생리 기록: 2025-04-04 ~ 2025-04-08 (네 번째 생리)
     * - 생리 예정일들: 없음
     * - 생리 지연일: 없음
     * - 배란기들: 없음 (조회 범위 밖)
     * - 가임기들: 없음 (조회 범위 밖)
     * - 주기: 31일
     */
    @Test
    fun testTC_04_04_singleDayQueryPast() = runTest {
        val repository = createRepository()

        val searchFrom = LocalDate(2025, 3, 11)
        val searchTo = LocalDate(2025, 3, 11)
        val today = LocalDate(2025, 4, 18)

        val result = PeriodCalculator.calculateCycleInfo(
            repository,
            DateUtils.toJulianDay(searchFrom),
            DateUtils.toJulianDay(searchTo),
            DateUtils.toJulianDay(today)
        )

        // 검증
        assertEquals(2, result.size, "주기 개수: 2개")

        // 주기 1 (pk=3) 검증
        val cycle1 = result[0]
        assertEquals("3", cycle1.pk, "주기 1: pk=3")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 1)),
            cycle1.actualPeriod?.startDate,
            "주기 1 실제 생리 시작: 2025-03-01"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 5)),
            cycle1.actualPeriod?.endDate,
            "주기 1 실제 생리 종료: 2025-03-05"
        )

        // 주기 1 배란기 검증 (조회 범위 밖이므로 없음)
        assertEquals(0, cycle1.ovulationDays.size, "주기 1 배란기 없음 (조회 범위 밖)")

        // 주기 1 가임기 검증 (조회 범위 밖이므로 없음)
        assertEquals(0, cycle1.fertileDays.size, "주기 1 가임기 없음 (조회 범위 밖)")

        // 주기 1 주기 검증
        assertEquals(34, cycle1.period, "주기 1: 34일 (Period 3 실제 주기)")

        // 주기 2 (pk=4) 검증
        val cycle2 = result[1]
        assertEquals("4", cycle2.pk, "주기 2: pk=4")
        assertEquals(0, cycle2.ovulationDays.size, "주기 2 배란기 없음")
        assertEquals(0, cycle2.fertileDays.size, "주기 2 가임기 없음")
        assertEquals(31, cycle2.period, "주기 2: 31일 (자동 계산 평균)")
    }

    /**
     * TC-04-05: 1주일 조회 (과거)
     *
     * 테스트 목적: 과거 기록된 주기의 배란기/가임기가 포함된 1주일 기간 조회 시 해당 주의 상태 변화가 정확히 표시되는지 검증
     *
     * 조회 기간: 2025-03-09 ~ 2025-03-15
     *
     * 예상 결과:
     * 주기 개수: 2개
     *
     * 주기 1 (pk=3):
     * - 실제 생리 기록: 2025-03-01 ~ 2025-03-05 (세 번째 생리)
     * - 생리 예정일들: 없음 (Period 4는 실제 기록)
     * - 생리 지연일: 없음
     * - 배란기들: 없음 (2025-03-19 ~ 2025-03-21, 조회 범위 밖)
     * - 가임기들: 없음 (2025-03-16 ~ 2025-03-24, 조회 범위 밖)
     * - 주기: 34일
     *
     * 주기 2 (pk=4):
     * - 실제 생리 기록: 2025-04-04 ~ 2025-04-08 (네 번째 생리)
     * - 생리 예정일들: 없음
     * - 생리 지연일: 없음
     * - 배란기들: 없음 (조회 범위 밖)
     * - 가임기들: 없음 (조회 범위 밖)
     * - 주기: 31일
     */
    @Test
    fun testTC_04_05_oneWeekQueryPast() = runTest {
        val repository = createRepository()

        val searchFrom = LocalDate(2025, 3, 9)
        val searchTo = LocalDate(2025, 3, 15)
        val today = LocalDate(2025, 4, 20)

        val result = PeriodCalculator.calculateCycleInfo(
            repository,
            DateUtils.toJulianDay(searchFrom),
            DateUtils.toJulianDay(searchTo),
            DateUtils.toJulianDay(today)
        )

        // 검증
        assertEquals(2, result.size, "주기 개수: 2개")

        // 주기 1 (pk=3) 검증
        val cycle1 = result[0]
        assertEquals("3", cycle1.pk, "주기 1: pk=3")

        // 주기 1 배란기 검증
        assertEquals(0, cycle1.ovulationDays.size, "주기 1 배란기 없음 (조회 범위 밖)")

        // 주기 1 가임기 검증
        assertEquals(0, cycle1.fertileDays.size, "주기 1 가임기 없음 (조회 범위 밖)")

        // 주기 1 주기 검증
        assertEquals(34, cycle1.period, "주기 1: 34일 (Period 3 실제 주기)")

        // 주기 2 (pk=4) 검증
        val cycle2 = result[1]
        assertEquals("4", cycle2.pk, "주기 2: pk=4")
        assertEquals(0, cycle2.ovulationDays.size, "주기 2 배란기 없음")
        assertEquals(0, cycle2.fertileDays.size, "주기 2 가임기 없음")
        assertEquals(31, cycle2.period, "주기 2: 29일 (자동 계산 평균)")
    }

    /**
     * TC-04-06: 1개월 조회 (과거)
     *
     * 테스트 목적: 과거 기록된 Period 3 전체 주기를 조회할 때 생리일, 배란기, 가임기가 모두 정확히 표시되는지 검증 (불규칙 주기)
     *
     * 조회 기간: 2025-03-01 ~ 2025-03-29
     *
     * 예상 결과:
     * 주기 개수: 2개
     *
     * 주기 1 (pk=3):
     * - 실제 생리 기록: 2025-03-01 ~ 2025-03-03 (세 번째 생리)
     * - 생리 예정일들: 없음 (Period 4는 실제 기록, 예정일이 아님)
     * - 생리 지연일: 없음
     * - 배란기들: 2025-03-18 ~ 2025-03-20 (생리 시작 15-17일차)
     * - 가임기들: 2025-03-15 ~ 2025-03-23 (생리 시작 12-20일차)
     * - 주기: 34일
     *
     * 주기 2 (pk=4):
     * - 실제 생리 기록: 2025-04-04 ~ 2025-04-08 (네 번째 생리, 다음 주기 정보 제공)
     * - 생리 예정일들: 없음 (조회 범위 밖)
     * - 생리 지연일: 없음
     * - 배란기들: 없음 (조회 범위 밖)
     * - 가임기들: 없음 (조회 범위 밖)
     * - 주기: 31일
     */
    @Test
    fun testTC_04_06_oneMonthQueryPast() = runTest {
        val repository = createRepository()

        val searchFrom = LocalDate(2025, 3, 1)
        val searchTo = LocalDate(2025, 3, 29)
        val today = LocalDate(2025, 4, 18)

        val result = PeriodCalculator.calculateCycleInfo(
            repository,
            DateUtils.toJulianDay(searchFrom),
            DateUtils.toJulianDay(searchTo),
            DateUtils.toJulianDay(today)
        )

        // 검증
        assertEquals(2, result.size, "주기 개수: 2개")

        // 주기 1 (pk=3) 검증
        val cycle1 = result[0]
        assertEquals("3", cycle1.pk, "주기 1: pk=3")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 1)),
            cycle1.actualPeriod?.startDate,
            "주기 1 실제 생리 시작: 2025-03-01"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 5)),
            cycle1.actualPeriod?.endDate,
            "주기 1 실제 생리 종료: 2025-03-03"
        )

        // 주기 1 배란기 검증
        assertEquals(1, cycle1.ovulationDays.size, "주기 1 배란기 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 19)),
            cycle1.ovulationDays[0].startDate,
            "주기 1 배란기 시작: 2025-03-11"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 21)),
            cycle1.ovulationDays[0].endDate,
            "주기 1 배란기 종료: 2025-03-13"
        )

        // 주기 1 가임기 검증
        assertEquals(1, cycle1.fertileDays.size, "주기 1 가임기 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 16)),
            cycle1.fertileDays[0].startDate,
            "주기 1 가임기 시작: 2025-03-06"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 24)),
            cycle1.fertileDays[0].endDate,
            "주기 1 가임기 종료: 2025-03-17"
        )

        // 주기 1 주기 검증
        assertEquals(34, cycle1.period, "주기 1: 34일 (Period 3 실제 주기)")

        // 주기 2 (pk=4) 검증
        val cycle2 = result[1]
        assertEquals("4", cycle2.pk, "주기 2: pk=4")
        assertEquals(0, cycle2.predictDays.size, "주기 2 생리 예정일 없음")
        assertEquals(0, cycle2.ovulationDays.size, "주기 2 배란기 없음")
        assertEquals(0, cycle2.fertileDays.size, "주기 2 가임기 없음")
        assertEquals(31, cycle2.period, "주기 2: 29일 (자동 계산 평균)")
    }

    // ====================================================================================
    // 그룹 3: 장기 조회 및 특수 경계
    // ====================================================================================

    /**
     * TC-04-07: 생리 기간 경계 조회
     *
     * 테스트 목적: 조회 기간이 생리 종료일과 다음 생리 시작일에 걸쳐있을 때 두 주기가 모두 반환되는지 검증
     *
     * 조회 기간: 2025-04-01 ~ 2025-04-06
     *
     * 예상 결과:
     * 주기 개수: 2개
     *
     * 주기 1 (pk=3):
     * - 실제 생리 기록: 2025-03-01 ~ 2025-03-05 (세 번째 생리)
     * - 생리 예정일들: 없음 (Period 4는 실제 기록)
     * - 생리 지연일: 없음
     * - 배란기들: 없음 (조회 범위 밖)
     * - 가임기들: 없음 (조회 범위 밖)
     * - 주기: 34일
     *
     * 주기 2 (pk=4):
     * - 실제 생리 기록: 2025-04-04 ~ 2025-04-08 (네 번째 생리, 조회 범위와 부분 겹침)
     * - 생리 예정일들: 없음
     * - 생리 지연일: 없음
     * - 배란기들: 없음 (조회 범위 밖)
     * - 가임기들: 없음 (조회 범위 밖)
     * - 주기: 31일
     */
    @Test
    fun testTC_04_07_periodBoundaryQuery() = runTest {
        val repository = createRepository()

        val searchFrom = LocalDate(2025, 4, 1)
        val searchTo = LocalDate(2025, 4, 6)
        val today = LocalDate(2025, 4, 18)

        val result = PeriodCalculator.calculateCycleInfo(
            repository,
            DateUtils.toJulianDay(searchFrom),
            DateUtils.toJulianDay(searchTo),
            DateUtils.toJulianDay(today)
        )

        // 검증
        assertEquals(2, result.size, "주기 개수: 2개")

        // 주기 1 (pk=3) 검증
        val cycle1 = result[0]
        assertEquals("3", cycle1.pk, "주기 1: pk=3")
        assertEquals(0, cycle1.ovulationDays.size, "주기 1 배란기 없음 (조회 범위 밖)")
        assertEquals(0, cycle1.fertileDays.size, "주기 1 가임기 없음 (조회 범위 밖)")
        assertEquals(34, cycle1.period, "주기 1: 34일 (Period 3 실제 주기)")

        // 주기 2 (pk=4) 검증
        val cycle2 = result[1]
        assertEquals("4", cycle2.pk, "주기 2: pk=4")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 4)),
            cycle2.actualPeriod?.startDate,
            "주기 2 실제 생리 시작: 2025-04-04 (조회 범위와 겹침)"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 8)),
            cycle2.actualPeriod?.endDate,
            "주기 2 실제 생리 종료: 2025-04-08"
        )
        assertEquals(0, cycle2.ovulationDays.size, "주기 2 배란기 없음 (조회 범위 밖)")
        assertEquals(0, cycle2.fertileDays.size, "주기 2 가임기 없음 (조회 범위 밖)")
        assertEquals(31, cycle2.period, "주기 2: 31일 (자동 계산 평균)")
    }

    /**
     * TC-04-08: 3개월 조회
     *
     * 테스트 목적: 장기간 조회 시 불규칙 주기의 예측이 평균값(31일)으로 반복되는지 검증
     *
     * 조회 기간: 2025-04-09 ~ 2025-06-30
     *
     * 예상 결과:
     * - 실제 생리 기록: 2025-04-04 ~ 2025-04-08 (pk=4, 마지막 생리)
     * - 생리 예정일들:
     *   - 2025-05-05 ~ 2025-05-09 (31일 기준, 5일간)
     *   - 2025-06-05 ~ 2025-06-09
     * - 생리 지연일: 없음
     * - 배란기들:
     *   - 2025-04-18 ~ 2025-04-20 (15-17일차)
     *   - 2025-05-19 ~ 2025-05-21
     *   - 2025-06-19 ~ 2025-06-21
     * - 가임기들:
     *   - 2025-04-15 ~ 2025-04-23 (12-20일차)
     *   - 2025-05-16 ~ 2025-05-24
     *   - 2025-06-16 ~ 2025-06-24
     * - 주기: 31일
     */
    @Test
    fun testTC_04_08_threeMonthQuery() = runTest {
        val repository = createRepository()

        val searchFrom = LocalDate(2025, 4, 9)
        val searchTo = LocalDate(2025, 6, 30)
        val today = LocalDate(2025, 4, 20)

        val result = PeriodCalculator.calculateCycleInfo(
            repository,
            DateUtils.toJulianDay(searchFrom),
            DateUtils.toJulianDay(searchTo),
            DateUtils.toJulianDay(today)
        )

        // 검증
        assertEquals(1, result.size, "주기 개수: 1개 (Period 4만)")

        // Period 4 (주요 검증 대상)
        val cycle = result[0]

        // 실제 생리 기록 검증
        assertEquals("4", cycle.pk, "pk=4 (마지막 생리)")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 4)),
            cycle.actualPeriod?.startDate,
            "실제 생리 시작: 2025-04-04"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 8)),
            cycle.actualPeriod?.endDate,
            "실제 생리 종료: 2025-04-08"
        )

        // 생리 예정일 검증 (2개, 세 번째는 조회 범위 밖)
        assertEquals(2, cycle.predictDays.size, "생리 예정일 2개")

        // 첫 번째 예정일
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 5, 5)),
            cycle.predictDays[0].startDate,
            "첫 번째 예정일 시작: 2025-05-05"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 5, 9)),
            cycle.predictDays[0].endDate,
            "첫 번째 예정일 종료: 2025-05-09"
        )

        // 두 번째 예정일
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 6, 5)),
            cycle.predictDays[1].startDate,
            "두 번째 예정일 시작: 2025-06-05"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 6, 9)),
            cycle.predictDays[1].endDate,
            "두 번째 예정일 종료: 2025-06-09"
        )

        // 배란기 검증 (3개)
        assertEquals(3, cycle.ovulationDays.size, "배란기 3개")

        // 첫 번째 배란기 (Period 4)
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 16)),
            cycle.ovulationDays[0].startDate,
            "첫 번째 배란기 시작: 2025-04-16"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 18)),
            cycle.ovulationDays[0].endDate,
            "첫 번째 배란기 종료: 2025-04-18"
        )

        // 두 번째 배란기 (첫 번째 예정일 05-05 기준)
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 5, 17)),
            cycle.ovulationDays[1].startDate,
            "두 번째 배란기 시작: 2025-05-17"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 5, 19)),
            cycle.ovulationDays[1].endDate,
            "두 번째 배란기 종료: 2025-05-19"
        )

        // 세 번째 배란기 (두 번째 예정일 06-05 기준)
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 6, 17)),
            cycle.ovulationDays[2].startDate,
            "세 번째 배란기 시작: 2025-06-17"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 6, 19)),
            cycle.ovulationDays[2].endDate,
            "세 번째 배란기 종료: 2025-06-19"
        )

        // 가임기 검증 (3개)
        assertEquals(3, cycle.fertileDays.size, "가임기 3개")

        // 첫 번째 가임기 (Period 4)
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 11)),
            cycle.fertileDays[0].startDate,
            "첫 번째 가임기 시작: 2025-04-11"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 22)),
            cycle.fertileDays[0].endDate,
            "첫 번째 가임기 종료: 2025-04-22"
        )

        // 두 번째 가임기 (첫 번째 예정일 05-05 기준)
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 5, 12)),
            cycle.fertileDays[1].startDate,
            "두 번째 가임기 시작: 2025-05-12"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 5, 23)),
            cycle.fertileDays[1].endDate,
            "두 번째 가임기 종료: 2025-05-23"
        )

        // 세 번째 가임기 (두 번째 예정일 06-05 기준)
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 6, 12)),
            cycle.fertileDays[2].startDate,
            "세 번째 가임기 시작: 2025-06-12"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 6, 23)),
            cycle.fertileDays[2].endDate,
            "세 번째 가임기 종료: 2025-06-23"
        )

        // 주기 검증
        assertEquals(31, cycle.period, "주기: 31일 (자동 계산 평균)")
    }

    // ====================================================================================
    // 그룹 4: 특정 기간 타입 조회
    // ====================================================================================

    /**
     * TC-04-09: 생리 예정 기간 중 조회
     *
     * 테스트 목적: 조회 기간이 생리 예정 기간을 포함할 때 정확히 표시되는지 검증
     *
     * 조회 기간: 2025-05-05 ~ 2025-05-09
     *
     * 예상 결과:
     * - 실제 생리 기록: 2025-04-04 ~ 2025-04-08 (pk=4, 마지막 생리)
     * - 생리 예정일들: 2025-05-05 ~ 2025-05-09
     * - 생리 지연일: 없음
     * - 배란기들: 없음 (조회 기간 내 배란기 없음)
     * - 가임기들: 없음 (조회 기간 내 가임기 없음)
     * - 주기: 31일
     */
    @Test
    fun testTC_04_09_duringPredictedPeriod() = runTest {
        val repository = createRepository()

        val searchFrom = LocalDate(2025, 5, 5)
        val searchTo = LocalDate(2025, 5, 9)
        val today = LocalDate(2025, 4, 18)

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
        assertEquals("4", cycle.pk, "pk=4 (마지막 생리)")

        // 생리 예정일 검증
        assertEquals(1, cycle.predictDays.size, "생리 예정일 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 5, 5)),
            cycle.predictDays[0].startDate,
            "예정일 시작: 2025-05-05"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 5, 9)),
            cycle.predictDays[0].endDate,
            "예정일 종료: 2025-05-09"
        )

        // 생리 지연일 검증
        assertEquals(0, cycle.delayTheDays, "생리 지연일: 없음")

        // 배란기/가임기 검증
        assertEquals(0, cycle.ovulationDays.size, "배란기 없음 (조회 기간 내 없음)")
        assertEquals(0, cycle.fertileDays.size, "가임기 없음 (조회 기간 내 없음)")

        // 주기 검증
        assertEquals(31, cycle.period, "주기: 31일 (자동 계산 평균)")
    }

    /**
     * TC-04-10: 배란기 중 조회
     *
     * 테스트 목적: 조회 기간이 배란기를 포함할 때 정확히 표시되는지 검증 (불규칙 주기, 평균 공식 적용)
     *
     * 조회 기간: 2025-04-16 ~ 2025-04-18
     *
     * 예상 결과:
     * - 실제 생리 기록: 2025-04-04 ~ 2025-04-08 (pk=4, 마지막 생리)
     * - 생리 예정일들: 없음 (조회 기간 내 생리 없음)
     * - 생리 지연일: 없음
     * - 배란기들: 2025-04-18 ~ 2025-04-20
     * - 가임기들: 2025-04-15 ~ 2025-04-23 (조회 범위와 부분 겹침)
     * - 주기: 31일
     */
    @Test
    fun testTC_04_10_duringOvulation() = runTest {
        val repository = createRepository()


        val searchFrom = LocalDate(2025, 4, 16)
        val searchTo = LocalDate(2025, 4, 18)
        val today = LocalDate(2025, 4, 18)

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
        assertEquals("4", cycle.pk, "pk=4 (마지막 생리)")

        // 생리 예정일 검증
        assertEquals(0, cycle.predictDays.size, "생리 예정일 없음 (조회 기간 내 없음)")

        // 생리 지연일 검증
        assertEquals(0, cycle.delayTheDays, "생리 지연일: 없음")

        // 배란기 검증
        assertEquals(1, cycle.ovulationDays.size, "배란기 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 16)),
            cycle.ovulationDays[0].startDate,
            "배란기 시작: 2025-04-18 (13일차)"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 18)),
            cycle.ovulationDays[0].endDate,
            "배란기 종료: 2025-04-20 (15일차)"
        )

        // 가임기 검증
        assertEquals(1, cycle.fertileDays.size, "가임기 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 11)),
            cycle.fertileDays[0].startDate,
            "가임기 시작: 2025-04-15 (8일차)"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 22)),
            cycle.fertileDays[0].endDate,
            "가임기 종료: 2025-04-23 (19일차)"
        )

        // 주기 검증
        assertEquals(31, cycle.period, "주기: 31일 (자동 계산 평균)")
    }

    /**
     * TC-04-11: 가임기 중 조회
     *
     * 테스트 목적: 조회 기간이 가임기를 포함할 때 정확히 표시되는지 검증 (불규칙 주기, 평균 공식 적용)
     *
     * 조회 기간: 2025-04-11 ~ 2025-04-16
     *
     * 예상 결과:
     * - 실제 생리 기록: 2025-04-04 ~ 2025-04-08 (pk=4, 마지막 생리)
     * - 생리 예정일들: 없음 (조회 기간 내 생리 없음)
     * - 생리 지연일: 없음
     * - 배란기들: 2025-04-18 ~ 2025-04-20 (조회 범위와 부분 겹침)
     * - 가임기들: 2025-04-15 ~ 2025-04-23 (조회 범위와 부분 겹침)
     * - 주기: 31일
     */
    @Test
    fun testTC_04_11_duringFertile() = runTest {
        val repository = createRepository()

        val searchFrom = LocalDate(2025, 4, 11)
        val searchTo = LocalDate(2025, 4, 18)
        val today = LocalDate(2025, 4, 18)

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
        assertEquals("4", cycle.pk, "pk=4 (마지막 생리)")

        // 생리 예정일 검증
        assertEquals(0, cycle.predictDays.size, "생리 예정일 없음 (조회 기간 내 없음)")

        // 생리 지연일 검증
        assertEquals(0, cycle.delayTheDays, "생리 지연일: 없음")

        // 배란기 검증 (조회 범위와 겹침)
        assertEquals(1, cycle.ovulationDays.size, "배란기 1개 (04-18 ~ 04-20)")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 16)),
            cycle.ovulationDays[0].startDate,
            "배란기 시작: 2025-04-18"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 18)),
            cycle.ovulationDays[0].endDate,
            "배란기 종료: 2025-04-20"
        )

        // 가임기 검증
        assertEquals(1, cycle.fertileDays.size, "가임기 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 11)),
            cycle.fertileDays[0].startDate,
            "가임기 시작: 2025-04-15 (8일차)"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 22)),
            cycle.fertileDays[0].endDate,
            "가임기 종료: 2025-04-23 (19일차)"
        )

        // 주기 검증
        assertEquals(31, cycle.period, "주기: 31일 (자동 계산 평균)")
    }

    // ====================================================================================
    // 그룹 5: 생리 지연
    // ====================================================================================

    /**
     * TC-04-12: 생리 지연 1-7일 (예정일 뒤로 미룸)
     *
     * 테스트 목적: 불규칙 주기에서 생리가 1-7일 지연되었을 때 예정일이 지연일만큼 미뤄지고
     *              배란기/가임기도 함께 조정되는지 검증
     *
     * 조회 기간: 2025-04-05 ~ 2025-05-20
     * 오늘 날짜: 2025-05-08 (생리 예정일 05-05에서 4일 지연)
     *
     * 예상 결과:
     * - 실제 생리 기록: 2025-04-04 ~ 2025-04-08 (pk=4, 마지막 생리, 부분 포함)
     * - 생리 지연일: 4일
     * - 지연 기간: 2025-05-05 ~ 2025-05-08 (예정일부터 오늘까지)
     * - 생리 예정일들: 2025-05-09 ~ 2025-05-13 (지연 다음날부터 5일간, 지연으로 밀린 예정일)
     * - 배란기들: 2025-04-16 ~ 2025-04-18 (Period 4의 배란기)
     * - 가임기들:
     *   - 2025-04-11 ~ 2025-04-22 (Period 4의 가임기, 부분 포함)
     *   - 2025-05-16 ~ 2025-05-27 (지연 4일 적용된 예측 주기의 가임기, 전체 포함)
     * - 주기: 31일
     */
    @Test
    fun testTC_04_12_delayOneToSevenDays() = runTest {
        val repository = createRepository()

        val searchFrom = LocalDate(2025, 4, 5)
        val searchTo = LocalDate(2025, 5, 20)
        val today = LocalDate(2025, 5, 8) // 4일 지연 (예정일 05-05에서)

        val result = PeriodCalculator.calculateCycleInfo(
            repository,
            DateUtils.toJulianDay(searchFrom),
            DateUtils.toJulianDay(searchTo),
            DateUtils.toJulianDay(today)
        )

        // 검증
        assertEquals(1, result.size, "주기 개수: 1개")

        val cycle = result[0]

        // 디버깅: 실제 반환된 예정일 확인
        val periodDetails = cycle.predictDays.mapIndexed { index, period ->
            val startDate = DateUtils.fromJulianDay(period.startDate)
            val endDate = DateUtils.fromJulianDay(period.endDate)
            "Period $index: $startDate ~ $endDate"
        }.joinToString(", ")

        assertEquals(1, cycle.predictDays.size, "생리 예정일 1개. Actual periods: $periodDetails")

        // 실제 생리 기록 검증
        assertEquals("4", cycle.pk, "pk=4 (마지막 생리)")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 4)),
            cycle.actualPeriod?.startDate,
            "실제 생리 시작: 2025-04-04"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 8)),
            cycle.actualPeriod?.endDate,
            "실제 생리 종료: 2025-04-08"
        )

        // 생리 지연일 검증 (05-05 예정일 기준으로 4일 지연: 05-05, 05-06, 05-07, 05-08)
        assertEquals(4, cycle.delayTheDays, "생리 지연일: 4일")

        // 지연 기간 검증 (05-05 ~ 05-08, 4일간)
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 5, 5)),
            cycle.delayDay?.startDate,
            "지연 기간 시작: 2025-05-05"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 5, 8)),
            cycle.delayDay?.endDate,
            "지연 기간 종료: 2025-05-08"
        )

        // 예정일 (4일 지연 반영: 지연 다음날부터)
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 5, 9)),
            cycle.predictDays[0].startDate,
            "예정일 시작: 2025-05-09 (지연 다음날)"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 5, 13)),
            cycle.predictDays[0].endDate,
            "예정일 종료: 2025-05-13"
        )

        // 배란기 검증 (1개: Period 4)
        assertEquals(1, cycle.ovulationDays.size, "배란기 1개")

        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 16)),
            cycle.ovulationDays[0].startDate,
            "배란기 시작: 2025-04-16 (Period 4)"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 18)),
            cycle.ovulationDays[0].endDate,
            "배란기 종료: 2025-04-18"
        )

        // 가임기 검증 (2개: Period 4 + 지연 적용된 예측 주기)
        assertEquals(2, cycle.fertileDays.size, "가임기 2개")

        // 첫 번째 가임기 (Period 4, 부분 포함)
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 11)),
            cycle.fertileDays[0].startDate,
            "첫 번째 가임기 시작: 2025-04-11 (Period 4)"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 22)),
            cycle.fertileDays[0].endDate,
            "첫 번째 가임기 종료: 2025-04-22"
        )

        // 두 번째 가임기 (지연 적용된 예측 주기, 전체 포함)
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 5, 16)),
            cycle.fertileDays[1].startDate,
            "두 번째 가임기 시작: 2025-05-16"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 5, 27)),
            cycle.fertileDays[1].endDate,
            "두 번째 가임기 종료: 2025-05-27 (전체 범위)"
        )

        // 주기 검증
        assertEquals(31, cycle.period, "주기: 31일 (자동 계산 평균)")
    }

    /**
     * TC-04-13: 생리 지연 8일 이상 (병원 추천)
     *
     * 테스트 목적: 불규칙 주기에서 생리가 8일 이상 지연되었을 때 미래 예측을 중단하고
     *              병원 방문을 권장하는지 검증
     *
     * 조회 기간: 2025-04-05 ~ 2025-05-20
     * 오늘 날짜: 2025-05-13 (생리 예정일 05-05에서 9일 지연)
     *
     * 예상 결과:
     * - 실제 생리 기록: 2025-04-04 ~ 2025-04-08 (pk=4, 마지막 생리)
     * - 생리 지연일: 9일
     * - 지연 기간: 2025-05-05 ~ 2025-05-13 (예정일 05-05부터 오늘 05-13까지 9일간)
     * - 생리 예정일: 없음 (8일 이상 지연 시 예정일 표시 안 함)
     * - 배란기들: 2025-04-16 ~ 2025-04-18 (Period 4의 배란기, 조회 범위 내)
     * - 가임기들: 2025-04-11 ~ 2025-04-22 (Period 4의 가임기, 조회 범위 내)
     * - 주기: 31일
     */
    @Test
    fun testTC_04_13_delayEightDaysOrMore() = runTest {
        val repository = createRepository()

        val searchFrom = LocalDate(2025, 4, 5)
        val searchTo = LocalDate(2025, 5, 20)
        val today = LocalDate(2025, 5, 13) // 9일 지연 (예정일 05-05에서)

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
        assertEquals("4", cycle.pk, "pk=4 (마지막 생리)")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 4)),
            cycle.actualPeriod?.startDate,
            "실제 생리 시작: 2025-04-04"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 8)),
            cycle.actualPeriod?.endDate,
            "실제 생리 종료: 2025-04-08"
        )

        // 생리 지연일 검증 (05-05 예정일 기준으로 9일 지연: 05-05부터 05-13까지)
        assertEquals(9, cycle.delayTheDays, "생리 지연일: 9일")

        // 지연 기간 검증 (2025-05-05 ~ 2025-05-13, 9일간)
        assertTrue(cycle.delayDay != null, "지연 기간 있음")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 5, 5)),
            cycle.delayDay?.startDate,
            "지연 시작: 2025-05-05"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 5, 13)),
            cycle.delayDay?.endDate,
            "지연 종료: 2025-05-13"
        )

        // 생리 예정일 검증 (8일 이상 지연 시 예정일 표시 안 함)
        assertEquals(0, cycle.predictDays.size, "생리 예정일 없음 (8일 이상 지연)")

        // 배란기 검증 (Period 4의 배란기만 표시, 조회 범위 내)
        assertEquals(1, cycle.ovulationDays.size, "배란기 1개 (Period 4만)")

        // Period 4 배란기
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 16)),
            cycle.ovulationDays[0].startDate,
            "배란기 시작: 2025-04-16"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 18)),
            cycle.ovulationDays[0].endDate,
            "배란기 종료: 2025-04-18"
        )

        // 가임기 검증 (Period 4의 가임기만 표시, 조회 범위 내)
        assertEquals(1, cycle.fertileDays.size, "가임기 1개 (Period 4만)")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 11)),
            cycle.fertileDays[0].startDate,
            "가임기 시작: 2025-04-11 (Period 4의 가임기)"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 22)),
            cycle.fertileDays[0].endDate,
            "가임기 종료: 2025-04-22"
        )

        // 주기 검증
        assertEquals(31, cycle.period, "주기: 31일 (자동 계산 평균)")
    }
}
