package com.bomcomes.calculator.helpers

import com.bomcomes.calculator.models.*
import kotlinx.datetime.*

/**
 * Pill Calculator
 *
 * 피임약 관련 계산 함수들
 */
internal object PillCalculator {

    /**
     * 생리 주기 사이에 피임약 복용이 있는지 확인
     */
    fun checkPillBetweenPeriods(
        startDate: LocalDate,
        nextDate: LocalDate,
        pillPackages: List<PillPackage>
    ): Boolean {
        val pillsInRange = pillPackages.filter { pill ->
            pill.packageStart >= startDate && pill.packageStart < nextDate
        }

        if (pillsInRange.isEmpty()) return false

        val firstPill = pillsInRange.first()
        val daysFromPillToNext = firstPill.packageStart.daysUntil(nextDate)

        return daysFromPillToNext >= 5
    }

    /**
     * 피임약 기반 예정일 계산
     */
    fun calculatePillBasedPredictDate(
        startDate: LocalDate,
        pillPackages: List<PillPackage>,
        pillSettings: PillSettings,
        normalPeriod: Int
    ): LocalDate? {
        val pillsAfterStart = pillPackages.filter { it.packageStart >= startDate }
        if (pillsAfterStart.isEmpty()) return null

        val normalPredictDate = startDate.plus(normalPeriod, DateTimeUnit.DAY)
        val firstPill = pillsAfterStart.first()
        val daysFromPillToPredict = firstPill.packageStart.daysUntil(normalPredictDate)

        return if (daysFromPillToPredict >= 5) {
            val lastPill = pillsAfterStart.last()
            lastPill.packageStart.plus(pillSettings.pillCount + 2, DateTimeUnit.DAY)
        } else {
            null
        }
    }

    /**
     * 피임약 복용 중인지 확인
     */
    fun isPillActiveOnDate(
        date: LocalDate,
        pillPackages: List<PillPackage>,
        pillSettings: PillSettings
    ): Boolean {
        if (!pillSettings.isCalculatingWithPill) return false
        if (pillPackages.isEmpty()) return false

        for (pillPackage in pillPackages) {
            val packageEnd = pillPackage.packageStart.plus(pillPackage.pillCount - 1, DateTimeUnit.DAY)
            if (date >= pillPackage.packageStart && date <= packageEnd) {
                return true
            }
        }

        return false
    }
}
