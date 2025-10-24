package com.bomcomes.calculator

import com.bomcomes.calculator.core.PredictCalculatorUseCase
import com.bomcomes.calculator.models.*
import kotlinx.datetime.LocalDate

/**
 * 생리 주기 계산기 V2 (완전 이식 버전)
 *
 * iOS PredictCalculatorUseCase를 완전히 이식한 버전입니다.
 * 생리 주기, 배란일, 가임기, 피임약, 임신 등을 종합적으로 고려하여 계산합니다.
 */
object PeriodCalculatorV2 {

    /**
     * 특정 날짜의 달력 상태 계산
     *
     * @param input 생리 주기 입력 데이터
     * @param date 조회할 날짜
     * @return 달력 상태 정보
     */
    fun calculateStatus(
        input: PeriodCycleInput,
        date: LocalDate
    ): CalendarStatus {
        val useCase = PredictCalculatorUseCase(input)
        return useCase.status(date)
    }

    /**
     * 기간 내의 생리 주기 정보 계산
     *
     * @param input 생리 주기 입력 데이터
     * @param from 시작 날짜
     * @param to 종료 날짜
     * @return 생리 주기 정보 리스트
     */
    fun calculateMenstrualCycles(
        input: PeriodCycleInput,
        from: LocalDate,
        to: LocalDate
    ): List<PeriodCycle> {
        val useCase = PredictCalculatorUseCase(input)
        return useCase.menstrualCycles(from, to)
    }

    /**
     * 간단한 다음 생리 예정일 계산 (단일 생리 기록 기준)
     *
     * @param lastPeriodStartDate 마지막 생리 시작일
     * @param lastPeriodEndDate 마지막 생리 종료일
     * @param periodSettings 생리 주기 설정
     * @return 다음 생리 예정일 범위
     */
    fun calculateNextPeriod(
        lastPeriodStartDate: LocalDate,
        lastPeriodEndDate: LocalDate,
        periodSettings: PeriodSettings = PeriodSettings()
    ): DateRange {
        val period = periodSettings.getAverageCycle()
        val days = periodSettings.days

        val predictStart = lastPeriodStartDate.plus(period, kotlinx.datetime.DateTimeUnit.DAY)
        val predictEnd = predictStart.plus(days - 1, kotlinx.datetime.DateTimeUnit.DAY)

        return DateRange(predictStart, predictEnd)
    }

    /**
     * 배란일 계산 (생리 예정일 14일 전)
     *
     * @param nextPeriodStartDate 다음 생리 예정일
     * @return 배란일 범위 (12~14일차, 3일간)
     */
    fun calculateOvulation(
        nextPeriodStartDate: LocalDate
    ): DateRange {
        val ovulationEnd = nextPeriodStartDate.minus(14, kotlinx.datetime.DateTimeUnit.DAY)
        val ovulationStart = ovulationEnd.minus(2, kotlinx.datetime.DateTimeUnit.DAY)

        return DateRange(ovulationStart, ovulationEnd)
    }

    /**
     * 가임기 계산 (배란일 -5일 ~ 배란일 +1일)
     *
     * @param ovulationDate 배란 예정일
     * @return 가임기 범위
     */
    fun calculateFertileWindow(
        ovulationDate: DateRange
    ): DateRange {
        val start = ovulationDate.startDate.minus(5, kotlinx.datetime.DateTimeUnit.DAY)
        val end = ovulationDate.endDate.plus(1, kotlinx.datetime.DateTimeUnit.DAY)

        return DateRange(start, end)
    }

    /**
     * 배란 테스트 결과로 배란일 추정
     *
     * @param ovulationTests 배란 테스트 결과 리스트
     * @return 가장 최근 양성 결과 날짜 (없으면 null)
     */
    fun estimateOvulationFromTests(
        ovulationTests: List<OvulationTest>
    ): LocalDate? {
        return ovulationTests
            .filter { it.result == OvulationTest.TestResult.POSITIVE }
            .maxByOrNull { it.date }
            ?.date
    }

    /**
     * 피임약 복용 여부 확인
     *
     * @param date 확인할 날짜
     * @param pillPackages 피임약 패키지 리스트
     * @param pillSettings 피임약 설정
     * @return true: 피임약 복용 중, false: 휴약기 또는 미복용
     */
    fun isPillActive(
        date: LocalDate,
        pillPackages: List<PillPackage>,
        pillSettings: PillSettings
    ): Boolean {
        if (!pillSettings.isCalculatingWithPill) return false

        for (pillPackage in pillPackages) {
            val packageEnd = pillPackage.packageStart.plus(
                pillPackage.totalDays - 1,
                kotlinx.datetime.DateTimeUnit.DAY
            )

            if (date in pillPackage.packageStart..packageEnd) {
                val daysSinceStart = (date.toEpochDays() - pillPackage.packageStart.toEpochDays()).toInt()
                return daysSinceStart < pillPackage.pillCount
            }
        }

        return false
    }
}
