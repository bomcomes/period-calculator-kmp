package com.bomcomes.calculator

import com.bomcomes.calculator.models.PregnancyInfo
import kotlinx.datetime.*

/**
 * 임신 관련 계산 유틸리티
 */
object PregnancyCalculator {

    /**
     * 표준 임신 기간 (일)
     */
    const val PREGNANCY_DURATION_DAYS = 280  // 40주 = 280일

    /**
     * 마지막 생리 시작일로부터 출산 예정일 계산
     *
     * @param lastPeriodStartDate 마지막 생리 시작일
     * @return 출산 예정일 (마지막 생리일 + 280일)
     */
    fun calculateDueDate(lastPeriodStartDate: LocalDate): LocalDate {
        return lastPeriodStartDate.plus(PREGNANCY_DURATION_DAYS, DateTimeUnit.DAY)
    }

    /**
     * 출산 예정일로부터 마지막 생리 시작일 역계산
     *
     * @param dueDate 출산 예정일
     * @return 마지막 생리 시작일 (출산 예정일 - 280일)
     */
    fun calculateLastPeriodDate(dueDate: LocalDate): LocalDate {
        return dueDate.minus(PREGNANCY_DURATION_DAYS, DateTimeUnit.DAY)
    }

    /**
     * 현재 임신 주차 계산 (마지막 생리일 기준)
     *
     * @param lastPeriodStartDate 마지막 생리 시작일
     * @param currentDate 현재 날짜
     * @return 임신 주차 (예: 12주)
     */
    fun calculateWeeksFromLastPeriod(
        lastPeriodStartDate: LocalDate,
        currentDate: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.UTC).date
    ): Int {
        val days = (currentDate.toEpochDays() - lastPeriodStartDate.toEpochDays()).toInt()
        return days / 7
    }

    /**
     * 현재 임신 주차와 일차 계산
     *
     * @param lastPeriodStartDate 마지막 생리 시작일
     * @param currentDate 현재 날짜
     * @return Pair<주차, 일차> (예: <12, 3> = 12주 3일)
     */
    fun calculateWeeksAndDays(
        lastPeriodStartDate: LocalDate,
        currentDate: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.UTC).date
    ): Pair<Int, Int> {
        val days = (currentDate.toEpochDays() - lastPeriodStartDate.toEpochDays()).toInt()
        val weeks = days / 7
        val remainingDays = days % 7
        return Pair(weeks, remainingDays)
    }

    /**
     * 출산 예정일까지 남은 일수
     *
     * @param dueDate 출산 예정일
     * @param currentDate 현재 날짜
     * @return 남은 일수
     */
    fun calculateDaysUntilDue(
        dueDate: LocalDate,
        currentDate: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.UTC).date
    ): Int {
        return (dueDate.toEpochDays() - currentDate.toEpochDays()).toInt()
    }

    /**
     * 임신 삼분기 계산
     *
     * @param weeks 임신 주차
     * @return 1, 2, 3 (삼분기) 또는 0 (아직 임신 전 또는 출산 후)
     */
    fun calculateTrimester(weeks: Int): Int {
        return when {
            weeks < 0 -> 0
            weeks <= 13 -> 1  // 1-13주: 첫 번째 삼분기
            weeks <= 27 -> 2  // 14-27주: 두 번째 삼분기
            weeks <= 40 -> 3  // 28-40주: 세 번째 삼분기
            else -> 0         // 출산 후
        }
    }

    /**
     * PregnancyInfo에서 출산 예정일 가져오기 또는 계산
     *
     * @param pregnancy 임신 정보
     * @return 출산 예정일 (직접 입력되었거나 마지막 생리일로부터 계산)
     */
    fun getDueDateOrCalculate(pregnancy: PregnancyInfo): LocalDate? {
        // 이미 출산 예정일이 있으면 반환
        if (pregnancy.dueDate != null) {
            return pregnancy.dueDate
        }

        // 마지막 생리일이 있으면 계산
        return pregnancy.lastTheDayDate?.let { calculateDueDate(it) }
    }

    /**
     * 체중 단위 변환: kg -> lbs
     */
    fun kgToLbs(kg: Float): Float = kg * 2.20462f

    /**
     * 체중 단위 변환: lbs -> kg
     */
    fun lbsToKg(lbs: Float): Float = lbs / 2.20462f

    /**
     * 체중 단위 변환: kg -> stone
     */
    fun kgToStone(kg: Float): Float = kg * 0.157473f

    /**
     * 체중 단위 변환: stone -> kg
     */
    fun stoneToKg(stone: Float): Float = stone / 0.157473f

    /**
     * 체중을 kg로 정규화
     */
    fun normalizeWeightToKg(weight: Float, unit: PregnancyInfo.WeightUnit): Float {
        return when (unit) {
            PregnancyInfo.WeightUnit.KG -> weight
            PregnancyInfo.WeightUnit.LBS -> lbsToKg(weight)
            PregnancyInfo.WeightUnit.ST -> stoneToKg(weight)
        }
    }

    /**
     * 임신 진행률 계산 (%)
     *
     * @param lastPeriodStartDate 마지막 생리 시작일
     * @param currentDate 현재 날짜
     * @return 진행률 (0-100%)
     */
    fun calculateProgress(
        lastPeriodStartDate: LocalDate,
        currentDate: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.UTC).date
    ): Float {
        val days = (currentDate.toEpochDays() - lastPeriodStartDate.toEpochDays()).toInt()
        val progress = (days.toFloat() / PREGNANCY_DURATION_DAYS) * 100f
        return progress.coerceIn(0f, 100f)
    }
}
