package com.bomcomes.calculator.integration

import com.bomcomes.calculator.PeriodCalculator
import com.bomcomes.calculator.models.*
import com.bomcomes.calculator.repository.InMemoryPeriodRepository
import com.bomcomes.calculator.utils.DateUtils
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.*

/**
 * 피임약 복용 검증 테스트 (11개)
 *
 * 문서 참조: test-cases/docs/04-with-pill.md
 *
 * 피임약 예정일 계산:
 * - 공식: 피임약 시작일 + 복용일 + 2 (휴약 3일째부터 생리 예정)
 * - 5일 규칙: 예정일 5일 전 이전 복용 시 피임약 기반 계산 적용
 * - 연속 복용 (휴약 0일): 예정일 없음, 배란기/가임기 숨김
 */
class WithPillTest {
    companion object {
        // 공통 생리 기록
        val PERIOD_1_START = LocalDate(2025, 1, 1)
        val PERIOD_1_END = LocalDate(2025, 1, 5)
        val PERIOD_2_START = LocalDate(2025, 2, 1)
        val PERIOD_2_END = LocalDate(2025, 2, 5)
        val PERIOD_3_START = LocalDate(2025, 3, 1)
        val PERIOD_3_END = LocalDate(2025, 3, 5)

        // 조회 기준일
        val DEFAULT_TODAY = LocalDate(2025, 3, 15)

        // 주기 설정
        const val MANUAL_AVERAGE_CYCLE = 28
        const val MANUAL_AVERAGE_DAY = 5
        const val AUTO_AVERAGE_CYCLE = 30  // (31 + 29) / 2 = 30
        const val AUTO_AVERAGE_DAY = 5
        const val IS_AUTO_CALC = true

        // 피임약 기본 설정
        const val DEFAULT_PILL_COUNT = 21
        const val DEFAULT_REST_DAYS = 7
    }

    /**
     * 공통 데이터 설정
     * - Period 1, 2, 3 추가
     * - 자동 계산 활성화
     * - 피임약 기본 비활성화
     */
    private fun setupCommonData(repository: InMemoryPeriodRepository) {
        // 생리 기록 3개 추가
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

        // 생리 주기 설정 (자동 계산 사용)
        repository.setPeriodSettings(PeriodSettings(
            manualAverageCycle = MANUAL_AVERAGE_CYCLE,
            manualAverageDay = MANUAL_AVERAGE_DAY,
            autoAverageCycle = AUTO_AVERAGE_CYCLE,
            autoAverageDay = AUTO_AVERAGE_DAY,
            isAutoCalc = IS_AUTO_CALC
        ))

        // 피임약 설정 (기본 비활성화)
        repository.setPillSettings(PillSettings(
            isCalculatingWithPill = false,
            pillCount = DEFAULT_PILL_COUNT,
            restPill = DEFAULT_REST_DAYS
        ))
    }

    /**
     * TC-04-01: 기본 복용 (1개월)
     * 피임약 패키지 전체 주기 조회 시 정확히 계산되는지 검증
     */
    @Test
    fun testTC_04_01_basicPillUsage() = runTest {
        val repository = InMemoryPeriodRepository()
        setupCommonData(repository)

        // 피임약 설정 활성화
        repository.setPillSettings(PillSettings(
            isCalculatingWithPill = true,
            pillCount = 21,
            restPill = 7
        ))

        // 피임약 패키지 추가
        repository.addPillPackage(PillPackage(
            packageStart = DateUtils.toJulianDay(LocalDate(2025, 3, 1))
        ))

        val fromDate = LocalDate(2025, 3, 1)
        val toDate = LocalDate(2025, 3, 30)
        val today = DEFAULT_TODAY

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository = repository,
            fromDate = DateUtils.toJulianDay(fromDate),
            toDate = DateUtils.toJulianDay(toDate),
            today = DateUtils.toJulianDay(today)
        )

        // 주기 개수: 1개
        assertEquals(1, cycles.size, "주기 개수: 1개")

        // 주기 1 (pk=3)
        val cycle1 = cycles.first()
        assertEquals("3", cycle1.pk, "pk: 3")
        assertEquals(
            DateUtils.toJulianDay(PERIOD_3_START),
            cycle1.actualPeriod?.startDate,
            "실제 생리 시작일: 2025-03-01"
        )
        assertEquals(
            DateUtils.toJulianDay(PERIOD_3_END),
            cycle1.actualPeriod?.endDate,
            "실제 생리 종료일: 2025-03-05"
        )

        // 지연 정보
        assertEquals(0, cycle1.delayTheDays, "지연 일수: 0일")
        assertNull(cycle1.delayDay, "지연 기간: null")

        // 생리 예정일: [2025-03-24 ~ 2025-03-28]
        assertEquals(1, cycle1.predictDays.size, "생리 예정일 개수: 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 24)),
            cycle1.predictDays[0].startDate,
            "예정일 시작: 2025-03-24"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 28)),
            cycle1.predictDays[0].endDate,
            "예정일 종료: 2025-03-28"
        )

        // 가임기: 없음
        assertEquals(0, cycle1.fertileDays.size, "가임기 개수: 0개")

        // 배란기: 없음
        assertEquals(0, cycle1.ovulationDays.size, "배란기 개수: 0개")

        // 주기 정보
        assertEquals(28, cycle1.period, "주기: 28일")
        assertFalse(cycle1.isOvulationPeriodUserInput, "배란일 사용자 입력: false")
        assertEquals(23, cycle1.thePillPeriod, "피임약 기준 주기: 23")
        assertFalse(cycle1.isContinuousPillUsage, "연속 복용: false")
    }

    /**
     * TC-04-02: 휴약기 0일 (연속 복용)
     * 휴약 없이 연속 복용 시 예정일이 없는지 검증
     */
    @Test
    fun testTC_04_02_continuousPillUsage() = runTest {
        val repository = InMemoryPeriodRepository()
        setupCommonData(repository)

        // 피임약 설정 활성화 (휴약 0일)
        repository.setPillSettings(PillSettings(
            isCalculatingWithPill = true,
            pillCount = 21,
            restPill = 0
        ))

        // 피임약 패키지 추가
        repository.addPillPackage(PillPackage(
            packageStart = DateUtils.toJulianDay(LocalDate(2025, 3, 1))
        ))

        val fromDate = LocalDate(2025, 3, 1)
        val toDate = LocalDate(2025, 3, 30)
        val today = DEFAULT_TODAY

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository = repository,
            fromDate = DateUtils.toJulianDay(fromDate),
            toDate = DateUtils.toJulianDay(toDate),
            today = DateUtils.toJulianDay(today)
        )

        // 주기 개수: 1개
        assertEquals(1, cycles.size, "주기 개수: 1개")

        // 주기 1 (pk=3)
        val cycle1 = cycles.first()
        assertEquals("3", cycle1.pk, "pk: 3")
        assertEquals(
            DateUtils.toJulianDay(PERIOD_3_START),
            cycle1.actualPeriod?.startDate,
            "실제 생리 시작일: 2025-03-01"
        )
        assertEquals(
            DateUtils.toJulianDay(PERIOD_3_END),
            cycle1.actualPeriod?.endDate,
            "실제 생리 종료일: 2025-03-05"
        )

        // 지연 정보
        assertEquals(0, cycle1.delayTheDays, "지연 일수: 0일")
        assertNull(cycle1.delayDay, "지연 기간: null")

        // 생리 예정일: 없음 (연속 복용)
        assertEquals(0, cycle1.predictDays.size, "생리 예정일 개수: 0개")

        // 가임기: 없음
        assertEquals(0, cycle1.fertileDays.size, "가임기 개수: 0개")

        // 배란기: 없음
        assertEquals(0, cycle1.ovulationDays.size, "배란기 개수: 0개")

        // 주기 정보
        assertEquals(30, cycle1.period, "주기: 30일")
        assertFalse(cycle1.isOvulationPeriodUserInput, "배란일 사용자 입력: false")
        assertNull(cycle1.thePillPeriod, "피임약 기준 주기: null (연속 복용)")
        assertTrue(cycle1.isContinuousPillUsage, "연속 복용: true")
    }

    /**
     * TC-04-03: 여러 패키지
     * 3개월 연속 패키지 - 마지막 패키지 예정일만 반환되는지 검증
     */
    @Test
    fun testTC_04_03_multiplePackages() = runTest {
        val repository = InMemoryPeriodRepository()
        setupCommonData(repository)

        // 피임약 설정 활성화
        repository.setPillSettings(PillSettings(
            isCalculatingWithPill = true,
            pillCount = 21,
            restPill = 7
        ))

        // 피임약 패키지 3개 추가
        repository.addPillPackage(PillPackage(
            packageStart = DateUtils.toJulianDay(LocalDate(2025, 1, 1))
        ))
        repository.addPillPackage(PillPackage(
            packageStart = DateUtils.toJulianDay(LocalDate(2025, 2, 1))
        ))
        repository.addPillPackage(PillPackage(
            packageStart = DateUtils.toJulianDay(LocalDate(2025, 3, 1))
        ))

        val fromDate = LocalDate(2025, 3, 1)
        val toDate = LocalDate(2025, 3, 30)
        val today = DEFAULT_TODAY

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository = repository,
            fromDate = DateUtils.toJulianDay(fromDate),
            toDate = DateUtils.toJulianDay(toDate),
            today = DateUtils.toJulianDay(today)
        )

        // 주기 개수: 1개
        assertEquals(1, cycles.size, "주기 개수: 1개")

        // 주기 1 (pk=3)
        val cycle1 = cycles.first()
        assertEquals("3", cycle1.pk, "pk: 3")
        assertEquals(DateUtils.toJulianDay(PERIOD_3_START), cycle1.actualPeriod?.startDate, "실제 생리 시작일: 2025-03-01")
        assertEquals(DateUtils.toJulianDay(PERIOD_3_END), cycle1.actualPeriod?.endDate, "실제 생리 종료일: 2025-03-05")

        // 지연 정보
        assertEquals(0, cycle1.delayTheDays, "지연 일수: 0일")
        assertNull(cycle1.delayDay, "지연 기간: null")

        // 생리 예정일: [2025-03-24 ~ 2025-03-28]
        assertEquals(1, cycle1.predictDays.size, "생리 예정일 개수: 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 24)),
            cycle1.predictDays[0].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 28)),
            cycle1.predictDays[0].endDate
        )

        // 가임기: 없음
        assertEquals(0, cycle1.fertileDays.size, "가임기 개수: 0개")

        // 배란기: 없음
        assertEquals(0, cycle1.ovulationDays.size, "배란기 개수: 0개")

        // 주기 정보 (피임약 복용으로 pk=3가 평균 계산에서 제외되어 28일)
        assertEquals(28, cycle1.period, "주기: 28일")
        assertFalse(cycle1.isOvulationPeriodUserInput, "배란일 사용자 입력: false")
        assertEquals(23, cycle1.thePillPeriod, "피임약 기준 주기: 23")
        assertFalse(cycle1.isContinuousPillUsage, "연속 복용: false")

    }

    /**
     * TC-04-04: 지연 1-7일
     * 피임약 복용 중 생리 지연 시 정확히 계산되는지 검증
     */
    @Test
    fun testTC_04_04_delay1To7Days() = runTest {
        val repository = InMemoryPeriodRepository()
        setupCommonData(repository)

        // 피임약 설정 활성화
        repository.setPillSettings(PillSettings(
            isCalculatingWithPill = true,
            pillCount = 21,
            restPill = 7
        ))

        // 피임약 패키지 추가
        repository.addPillPackage(PillPackage(
            packageStart = DateUtils.toJulianDay(LocalDate(2025, 3, 1))
        ))

        val fromDate = LocalDate(2025, 3, 30)
        val toDate = LocalDate(2025, 4, 4)
        val today = LocalDate(2025, 3, 30) // 생리 예정일 03-24으로부터 7일 지연

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository = repository,
            fromDate = DateUtils.toJulianDay(fromDate),
            toDate = DateUtils.toJulianDay(toDate),
            today = DateUtils.toJulianDay(today)
        )

        // 주기 개수: 1개
        assertEquals(1, cycles.size, "주기 개수: 1개")

        // 주기 1 (pk=3)
        val cycle1 = cycles.first()
        assertEquals("3", cycle1.pk, "pk: 3")
        assertEquals(DateUtils.toJulianDay(PERIOD_3_START), cycle1.actualPeriod?.startDate, "실제 생리 시작일: 2025-03-01")
        assertEquals(DateUtils.toJulianDay(PERIOD_3_END), cycle1.actualPeriod?.endDate, "실제 생리 종료일: 2025-03-05")

        // 지연 정보: 7일
        assertEquals(7, cycle1.delayTheDays, "지연 일수: 7일")
        assertNotNull(cycle1.delayDay, "지연 기간: not null")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 24)),
            cycle1.delayDay?.startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 30)),
            cycle1.delayDay?.endDate
        )

        // 생리 예정일: [2025-03-31 ~ 2025-04-04] (지연 다음날부터)
        assertEquals(1, cycle1.predictDays.size, "생리 예정일 개수: 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 31)),
            cycle1.predictDays[0].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 4)),
            cycle1.predictDays[0].endDate
        )

        // 가임기: 없음
        assertEquals(0, cycle1.fertileDays.size, "가임기 개수: 0개")

        // 배란기: 없음
        assertEquals(0, cycle1.ovulationDays.size, "배란기 개수: 0개")

        // 주기 정보
        assertEquals(28, cycle1.period, "주기: 28일")
        assertFalse(cycle1.isOvulationPeriodUserInput, "배란일 사용자 입력: false")
        assertEquals(23, cycle1.thePillPeriod, "피임약 기준 주기: 23")
        assertFalse(cycle1.isContinuousPillUsage, "연속 복용: false")

    }

    /**
     * TC-04-05: 지연 8일 이상
     * 8일 이상 지연 시 예정일 표시 안 함 검증 (병원 진료 권장)
     */
    @Test
    fun testTC_04_05_delay8OrMoreDays() = runTest {
        val repository = InMemoryPeriodRepository()
        setupCommonData(repository)

        // 피임약 설정 활성화
        repository.setPillSettings(PillSettings(
            isCalculatingWithPill = true,
            pillCount = 21,
            restPill = 7
        ))

        // 피임약 패키지 추가
        repository.addPillPackage(PillPackage(
            packageStart = DateUtils.toJulianDay(LocalDate(2025, 3, 1))
        ))

        val fromDate = LocalDate(2025, 3, 30)
        val toDate = LocalDate(2025, 4, 4)
        val today = LocalDate(2025, 3, 31) // 생리 예정일 03-24으로부터 8일 지연

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository = repository,
            fromDate = DateUtils.toJulianDay(fromDate),
            toDate = DateUtils.toJulianDay(toDate),
            today = DateUtils.toJulianDay(today)
        )

        // 주기 개수: 1개
        assertEquals(1, cycles.size, "주기 개수: 1개")

        // 주기 1 (pk=3)
        val cycle1 = cycles.first()
        assertEquals("3", cycle1.pk, "pk: 3")
        assertEquals(DateUtils.toJulianDay(PERIOD_3_START), cycle1.actualPeriod?.startDate, "실제 생리 시작일: 2025-03-01")
        assertEquals(DateUtils.toJulianDay(PERIOD_3_END), cycle1.actualPeriod?.endDate, "실제 생리 종료일: 2025-03-05")

        // 지연 정보: 8일
        assertEquals(8, cycle1.delayTheDays, "지연 일수: 8일")
        assertNotNull(cycle1.delayDay, "지연 기간: not null")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 24)),
            cycle1.delayDay?.startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 31)),
            cycle1.delayDay?.endDate
        )

        // 생리 예정일: 없음 (8일 이상 지연 시 표시 안 함)
        assertEquals(0, cycle1.predictDays.size, "생리 예정일 개수: 0개")

        // 가임기: 없음
        assertEquals(0, cycle1.fertileDays.size, "가임기 개수: 0개")

        // 배란기: 없음
        assertEquals(0, cycle1.ovulationDays.size, "배란기 개수: 0개")

        // 주기 정보
        assertEquals(28, cycle1.period, "주기: 28일")
        assertFalse(cycle1.isOvulationPeriodUserInput, "배란일 사용자 입력: false")
        assertEquals(23, cycle1.thePillPeriod, "피임약 기준 주기: 23")
        assertFalse(cycle1.isContinuousPillUsage, "연속 복용: false")
    }

    /**
     * TC-04-06: 5일 전 복용 (배란기 숨김)
     * 피임약을 예정일 5일 전에 복용 시 피임약 기반 계산 적용 검증
     */
    @Test
    fun testTC_04_06_fiveDaysBeforePeriod() = runTest {
        val repository = InMemoryPeriodRepository()
        setupCommonData(repository)

        // 피임약 설정 활성화
        repository.setPillSettings(PillSettings(
            isCalculatingWithPill = true,
            pillCount = 21,
            restPill = 7
        ))

        // 피임약 패키지 추가 (예정일 5일 전)
        repository.addPillPackage(PillPackage(
            packageStart = DateUtils.toJulianDay(LocalDate(2025, 3, 26))
        ))

        val fromDate = LocalDate(2025, 3, 1)
        val toDate = LocalDate(2025, 4, 30)
        val today = LocalDate(2025, 3, 27)

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository = repository,
            fromDate = DateUtils.toJulianDay(fromDate),
            toDate = DateUtils.toJulianDay(toDate),
            today = DateUtils.toJulianDay(today)
        )

        // 주기 개수: 1개
        assertEquals(1, cycles.size, "주기 개수: 1개")

        // 주기 1 (pk=3)
        val cycle1 = cycles.first()
        assertEquals("3", cycle1.pk, "pk: 3")
        assertEquals(DateUtils.toJulianDay(PERIOD_3_START), cycle1.actualPeriod?.startDate, "실제 생리 시작일: 2025-03-01")
        assertEquals(DateUtils.toJulianDay(PERIOD_3_END), cycle1.actualPeriod?.endDate, "실제 생리 종료일: 2025-03-05")

        // 지연 정보
        assertEquals(0, cycle1.delayTheDays, "지연 일수: 0일")
        assertNull(cycle1.delayDay, "지연 기간: null")

        // 생리 예정일: [2025-04-18 ~ 2025-04-22]
        assertEquals(1, cycle1.predictDays.size, "생리 예정일 개수: 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 18)),
            cycle1.predictDays[0].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 22)),
            cycle1.predictDays[0].endDate
        )

        // 가임기: 없음
        assertEquals(0, cycle1.fertileDays.size, "가임기 개수: 0개")

        // 배란기: 없음
        assertEquals(0, cycle1.ovulationDays.size, "배란기 개수: 0개")

        // 주기 정보
        assertEquals(28, cycle1.period, "주기: 30일")
        assertFalse(cycle1.isOvulationPeriodUserInput, "배란일 사용자 입력: false")
        assertEquals(48, cycle1.thePillPeriod, "피임약 기준 주기: 48")
        assertFalse(cycle1.isContinuousPillUsage, "연속 복용: false")
    }

    /**
     * TC-04-07: 5일 이후 복용 (배란기 노출)
     * 피임약을 예정일 5일 전 이후에 복용 시 일반 주기 계산 유지 검증
     */
    @Test
    fun testTC_04_07_afterFiveDays() = runTest {
        val repository = InMemoryPeriodRepository()
        setupCommonData(repository)

        // 피임약 설정 활성화
        repository.setPillSettings(PillSettings(
            isCalculatingWithPill = true,
            pillCount = 21,
            restPill = 7
        ))

        // 피임약 패키지 추가 (예정일 4일 전)
        repository.addPillPackage(PillPackage(
            packageStart = DateUtils.toJulianDay(LocalDate(2025, 3, 27))
        ))

        val fromDate = LocalDate(2025, 3, 1)
        val toDate = LocalDate(2025, 4, 30)
        val today = LocalDate(2025, 3, 27)

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository = repository,
            fromDate = DateUtils.toJulianDay(fromDate),
            toDate = DateUtils.toJulianDay(toDate),
            today = DateUtils.toJulianDay(today)
        )

        // 주기 개수: 1개
        assertEquals(1, cycles.size, "주기 개수: 1개")

        // 주기 1 (pk=3)
        val cycle1 = cycles.first()
        assertEquals("3", cycle1.pk, "pk: 3")
        assertEquals(DateUtils.toJulianDay(PERIOD_3_START), cycle1.actualPeriod?.startDate, "실제 생리 시작일: 2025-03-01")
        assertEquals(DateUtils.toJulianDay(PERIOD_3_END), cycle1.actualPeriod?.endDate, "실제 생리 종료일: 2025-03-05")

        // 지연 정보
        assertEquals(0, cycle1.delayTheDays, "지연 일수: 0일")
        assertNull(cycle1.delayDay, "지연 기간: null")

        // 생리 예정일: [2025-03-31 ~ 2025-04-04, 2025-04-30 ~ 2025-05-04]
        assertEquals(2, cycle1.predictDays.size, "생리 예정일 개수: 2개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 31)),
            cycle1.predictDays[0].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 4)),
            cycle1.predictDays[0].endDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 30)),
            cycle1.predictDays[1].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 5, 4)),
            cycle1.predictDays[1].endDate
        )

        // 가임기: [2025-03-08 ~ 2025-03-19, 2025-04-07 ~ 2025-04-18]
        assertEquals(2, cycle1.fertileDays.size, "가임기 개수: 2개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 8)),
            cycle1.fertileDays[0].startDate,
            "첫 번째 가임기 시작: 2025-03-08"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 19)),
            cycle1.fertileDays[0].endDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 7)),
            cycle1.fertileDays[1].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 18)),
            cycle1.fertileDays[1].endDate
        )

        // 배란기: [2025-03-13 ~ 2025-03-15, 2025-04-12 ~ 2025-04-14]
        assertEquals(2, cycle1.ovulationDays.size)
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 13)),
            cycle1.ovulationDays[0].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 15)),
            cycle1.ovulationDays[0].endDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 12)),
            cycle1.ovulationDays[1].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 14)),
            cycle1.ovulationDays[1].endDate
        )

        // 주기 정보
        assertEquals(30, cycle1.period, "주기: 30일")
        assertFalse(cycle1.isOvulationPeriodUserInput, "배란일 사용자 입력: false")
        assertNull(cycle1.thePillPeriod, "피임약 기준 주기: null")
        assertFalse(cycle1.isContinuousPillUsage, "연속 복용: false")

    }

    /**
     * TC-04-08: 생리 기간 사이 - 5일 전 복용 (배란기 숨김)
     * 첫 생리 후 피임약 복용, 두 번째 생리 기간에서 5일 규칙 검증
     */
    @Test
    fun testTC_04_08_betweenPeriodsFiveDaysBefore() = runTest {
        val repository = InMemoryPeriodRepository()
        setupCommonData(repository)

        // 피임약 설정 활성화
        repository.setPillSettings(PillSettings(
            isCalculatingWithPill = true,
            pillCount = 21,
            restPill = 7
        ))

        // 피임약 패키지 추가 (다음 생리 5일 전)
        repository.addPillPackage(PillPackage(
            packageStart = DateUtils.toJulianDay(LocalDate(2025, 1, 27))
        ))

        val fromDate = LocalDate(2025, 1, 1)
        val toDate = LocalDate(2025, 1, 31)
        val today = DEFAULT_TODAY

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository = repository,
            fromDate = DateUtils.toJulianDay(fromDate),
            toDate = DateUtils.toJulianDay(toDate),
            today = DateUtils.toJulianDay(today)
        )

        // 주기 개수: 1개
        assertEquals(1, cycles.size, "주기 개수: 1개")

        // 주기 1 (pk=1)
        val cycle1 = cycles.first()
        assertEquals("1", cycle1.pk, "pk: 1")
        assertEquals(DateUtils.toJulianDay(PERIOD_1_START), cycle1.actualPeriod?.startDate, "실제 생리 시작일: 2025-01-01")
        assertEquals(DateUtils.toJulianDay(PERIOD_1_END), cycle1.actualPeriod?.endDate, "실제 생리 종료일: 2025-01-05")

        // 지연 정보
        assertEquals(0, cycle1.delayTheDays, "지연 일수: 0일")
        assertNull(cycle1.delayDay, "지연 기간: null")

        // 생리 예정일: 없음
        assertEquals(0, cycle1.predictDays.size, "생리 예정일 개수: 0개")

        // 가임기: 없음
        assertEquals(0, cycle1.fertileDays.size, "가임기 개수: 0개")

        // 배란기: 없음
        assertEquals(0, cycle1.ovulationDays.size, "배란기 개수: 0개")

        // 주기 정보
        assertEquals(31, cycle1.period, "주기: 31일")
        assertFalse(cycle1.isOvulationPeriodUserInput, "배란일 사용자 입력: false")
        assertNull(cycle1.thePillPeriod, "피임약 기준 주기: null")
        assertFalse(cycle1.isContinuousPillUsage, "연속 복용: false")

    }

    /**
     * TC-04-09: 생리 기간 사이 - 5일 이후 복용 (배란기 노출)
     * 생리 후 늦게 피임약 시작 시 배란기/가임기 노출 검증
     */
    @Test
    fun testTC_04_09_betweenPeriodsAfterFiveDays() = runTest {
        val repository = InMemoryPeriodRepository()
        setupCommonData(repository)

        // 피임약 설정 활성화
        repository.setPillSettings(PillSettings(
            isCalculatingWithPill = true,
            pillCount = 21,
            restPill = 7
        ))

        // 피임약 패키지 추가 (다음 생리 4일 전)
        repository.addPillPackage(PillPackage(
            packageStart = DateUtils.toJulianDay(LocalDate(2025, 1, 28))
        ))

        val fromDate = LocalDate(2025, 1, 1)
        val toDate = LocalDate(2025, 1, 31)
        val today = DEFAULT_TODAY

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository = repository,
            fromDate = DateUtils.toJulianDay(fromDate),
            toDate = DateUtils.toJulianDay(toDate),
            today = DateUtils.toJulianDay(today)
        )

        // 주기 개수: 1개
        assertEquals(1, cycles.size, "주기 개수: 1개")

        // 주기 1 (pk=1)
        val cycle1 = cycles.first()
        assertEquals("1", cycle1.pk, "pk: 1")
        assertEquals(DateUtils.toJulianDay(PERIOD_1_START), cycle1.actualPeriod?.startDate, "실제 생리 시작일: 2025-01-01")
        assertEquals(DateUtils.toJulianDay(PERIOD_1_END), cycle1.actualPeriod?.endDate, "실제 생리 종료일: 2025-01-05")

        // 지연 정보
        assertEquals(0, cycle1.delayTheDays, "지연 일수: 0일")
        assertNull(cycle1.delayDay, "지연 기간: null")

        // 생리 예정일: 없음
        assertEquals(0, cycle1.predictDays.size, "생리 예정일 개수: 0개")

        // 가임기: [2025-01-08 ~ 2025-01-19]
        assertEquals(1, cycle1.fertileDays.size, "가임기 개수: 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 1, 8)),
            cycle1.fertileDays[0].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 1, 19)),
            cycle1.fertileDays[0].endDate
        )

        // 배란기: [2025-01-13 ~ 2025-01-15]
        assertEquals(1, cycle1.ovulationDays.size, "배란기 개수: 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 1, 13)),
            cycle1.ovulationDays[0].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 1, 15)),
            cycle1.ovulationDays[0].endDate
        )

        // 주기 정보
        assertEquals(31, cycle1.period, "주기: 31일")
        assertFalse(cycle1.isOvulationPeriodUserInput, "배란일 사용자 입력: false")
        assertNull(cycle1.thePillPeriod, "피임약 기준 주기: null")
        assertFalse(cycle1.isContinuousPillUsage, "연속 복용: false")

    }

    /**
     * TC-04-10: 피임약 중단 - 마지막 생리 기록 이후
     * 피임약 계산 적용 무시 설정 시 일반 주기 복귀 검증
     */
    @Test
    fun testTC_04_10_pillDiscontinuationAfterLastPeriod() = runTest {
        val repository = InMemoryPeriodRepository()
        setupCommonData(repository)

        // 피임약 비활성화
        repository.setPillSettings(PillSettings(
            isCalculatingWithPill = false,
            pillCount = 21,
            restPill = 7
        ))

        // 피임약 패키지 추가 (비활성화되었으므로 무시됨)
        repository.addPillPackage(PillPackage(
            packageStart = DateUtils.toJulianDay(LocalDate(2025, 3, 1))
        ))

        val fromDate = LocalDate(2025, 3, 1)
        val toDate = LocalDate(2025, 3, 31)
        val today = DEFAULT_TODAY

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository = repository,
            fromDate = DateUtils.toJulianDay(fromDate),
            toDate = DateUtils.toJulianDay(toDate),
            today = DateUtils.toJulianDay(today)
        )

        // 주기 개수: 1개
        assertEquals(1, cycles.size, "주기 개수: 1개")

        // 주기 1 (pk=3)
        val cycle1 = cycles.first()
        assertEquals("3", cycle1.pk, "pk: 3")
        assertEquals(DateUtils.toJulianDay(PERIOD_3_START), cycle1.actualPeriod?.startDate, "실제 생리 시작일: 2025-03-01")
        assertEquals(DateUtils.toJulianDay(PERIOD_3_END), cycle1.actualPeriod?.endDate, "실제 생리 종료일: 2025-03-05")

        // 지연 정보
        assertEquals(0, cycle1.delayTheDays, "지연 일수: 0일")
        assertNull(cycle1.delayDay, "지연 기간: null")

        // 생리 예정일: [2025-03-24 ~ 2025-03-28]
        assertEquals(1, cycle1.predictDays.size, "생리 예정일 개수: 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 31)),
            cycle1.predictDays[0].startDate,
            "일반 주기 계산으로 예정일 표시"
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 4, 4)),
            cycle1.predictDays[0].endDate
        )

        // 가임기: [2025-03-08 ~ 2025-03-19]
        assertEquals(1, cycle1.fertileDays.size, "가임기 개수: 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 8)),
            cycle1.fertileDays[0].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 19)),
            cycle1.fertileDays[0].endDate
        )

        // 배란기: [2025-03-13 ~ 2025-03-15]
        assertEquals(1, cycle1.ovulationDays.size, "배란기 개수: 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 13)),
            cycle1.ovulationDays[0].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 3, 15)),
            cycle1.ovulationDays[0].endDate
        )

        // 주기 정보
        assertEquals(30, cycle1.period, "주기: 30일")
        assertFalse(cycle1.isOvulationPeriodUserInput, "배란일 사용자 입력: false")
        assertNull(cycle1.thePillPeriod, "피임약 기준 주기: null")
        assertFalse(cycle1.isContinuousPillUsage, "연속 복용: false")

    }

    /**
     * TC-04-11: 피임약 중단 - 생리 기록 사이
     * 피임약 계산 적용 무시 설정 시 일반 주기 복귀 검증
     */
    @Test
    fun testTC_04_11_pillDiscontinuationBetweenPeriods() = runTest {
        val repository = InMemoryPeriodRepository()
        setupCommonData(repository)

        // 피임약 비활성화
        repository.setPillSettings(PillSettings(
            isCalculatingWithPill = false,
            pillCount = 21,
            restPill = 7
        ))

        // 피임약 패키지 추가 (비활성화되었으므로 무시됨)
        repository.addPillPackage(PillPackage(
            packageStart = DateUtils.toJulianDay(LocalDate(2025, 1, 1))
        ))

        val fromDate = LocalDate(2025, 1, 1)
        val toDate = LocalDate(2025, 1, 31)
        val today = DEFAULT_TODAY

        val cycles = PeriodCalculator.calculateCycleInfo(
            repository = repository,
            fromDate = DateUtils.toJulianDay(fromDate),
            toDate = DateUtils.toJulianDay(toDate),
            today = DateUtils.toJulianDay(today)
        )

        // 주기 개수: 1개
        assertEquals(1, cycles.size, "주기 개수: 1개")

        // 주기 1 (pk=1)
        val cycle1 = cycles.first()
        assertEquals("1", cycle1.pk, "pk: 1")
        assertEquals(DateUtils.toJulianDay(PERIOD_1_START), cycle1.actualPeriod?.startDate, "실제 생리 시작일: 2025-01-01")
        assertEquals(DateUtils.toJulianDay(PERIOD_1_END), cycle1.actualPeriod?.endDate, "실제 생리 종료일: 2025-01-05")

        // 지연 정보
        assertEquals(0, cycle1.delayTheDays, "지연 일수: 0일")
        assertNull(cycle1.delayDay, "지연 기간: null")

        // 생리 예정일: 없음
        assertEquals(0, cycle1.predictDays.size, "생리 예정일 개수: 0개")

        // 가임기: [2025-01-08 ~ 2025-01-19]
        assertEquals(1, cycle1.fertileDays.size, "가임기 개수: 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 1, 8)),
            cycle1.fertileDays[0].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 1, 19)),
            cycle1.fertileDays[0].endDate
        )

        // 배란기: [2025-01-13 ~ 2025-01-15]
        assertEquals(1, cycle1.ovulationDays.size, "배란기 개수: 1개")
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 1, 13)),
            cycle1.ovulationDays[0].startDate
        )
        assertEquals(
            DateUtils.toJulianDay(LocalDate(2025, 1, 15)),
            cycle1.ovulationDays[0].endDate
        )

        // 주기 정보
        assertEquals(31, cycle1.period, "주기: 31일")
        assertFalse(cycle1.isOvulationPeriodUserInput, "배란일 사용자 입력: false")
        assertNull(cycle1.thePillPeriod, "피임약 기준 주기: null")
        assertFalse(cycle1.isContinuousPillUsage, "연속 복용: false")

    }
}
