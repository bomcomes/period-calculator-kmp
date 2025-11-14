package com.bomcomes.calculator.helpers

import com.bomcomes.calculator.models.*
import com.bomcomes.calculator.utils.DateUtils
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
        startDate: Double,
        nextDate: Double,
        pillPackages: List<PillPackage>
    ): Boolean {
        val pillsInRange = pillPackages.filter { pill ->
            pill.packageStart >= startDate && pill.packageStart < nextDate
        }

        if (pillsInRange.isEmpty()) return false

        val firstPill = pillsInRange.first()
        val daysFromPillToNext = (nextDate - firstPill.packageStart).toInt()

        return daysFromPillToNext >= 5
    }

    /**
     * 피임약 기반 예정일 계산
     */
    fun calculatePillBasedPredictDate(
        startDate: Double,
        pillPackages: List<PillPackage>,
        pillSettings: PillSettings,
        normalPeriod: Int
    ): Double? {
        val pillsAfterStart = pillPackages.filter {
            it.packageStart >= startDate
        }
        if (pillsAfterStart.isEmpty()) return null

        val normalPredictDate = startDate + normalPeriod
        val firstPill = pillsAfterStart.first()
        val daysFromPillToPredict = (normalPredictDate - firstPill.packageStart).toInt()

        return if (daysFromPillToPredict >= 5) {
            val lastPill = pillsAfterStart.last()
            lastPill.packageStart + pillSettings.pillCount + 2
        } else {
            null
        }
    }

    /**
     * 피임약 복용 중인지 확인
     */
    fun isPillActiveOnDate(
        date: Double,
        pillPackages: List<PillPackage>,
        pillSettings: PillSettings
    ): Boolean {
        if (!pillSettings.isCalculatingWithPill) return false
        if (pillPackages.isEmpty()) return false

        for (pillPackage in pillPackages) {
            val packageEnd = pillPackage.packageStart + pillPackage.pillCount - 1
            if (date >= pillPackage.packageStart && date <= packageEnd) {
                return true
            }
        }

        return false
    }
}
