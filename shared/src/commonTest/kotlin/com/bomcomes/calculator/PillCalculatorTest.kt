package com.bomcomes.calculator

import com.bomcomes.calculator.helpers.PillCalculator
import com.bomcomes.calculator.models.*
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PillCalculatorTest {

    @Test
    fun testCheckPillBetweenPeriods_noPills() {
        // 피임약 복용이 없으면 false
        val result = PillCalculator.checkPillBetweenPeriods(
            startDate = LocalDate(2025, 1, 1),
            nextDate = LocalDate(2025, 1, 31),
            pillPackages = emptyList()
        )

        assertFalse(result)
    }

    @Test
    fun testCheckPillBetweenPeriods_pillTooClose() {
        // 피임약 시작일이 다음 생리일과 5일 미만이면 false
        val pillPackages = listOf(
            PillPackage(
                packageStart = LocalDate(2025, 1, 28),
                pillCount = 21,
                restDays = 7
            )
        )

        val result = PillCalculator.checkPillBetweenPeriods(
            startDate = LocalDate(2025, 1, 1),
            nextDate = LocalDate(2025, 1, 31), // 28일부터 31일까지 3일
            pillPackages = pillPackages
        )

        assertFalse(result)
    }

    @Test
    fun testCheckPillBetweenPeriods_pillValid() {
        // 피임약 시작일이 다음 생리일과 5일 이상이면 true
        val pillPackages = listOf(
            PillPackage(
                packageStart = LocalDate(2025, 1, 15),
                pillCount = 21,
                restDays = 7
            )
        )

        val result = PillCalculator.checkPillBetweenPeriods(
            startDate = LocalDate(2025, 1, 1),
            nextDate = LocalDate(2025, 1, 31), // 15일부터 31일까지 16일
            pillPackages = pillPackages
        )

        assertTrue(result)
    }

    @Test
    fun testCalculatePillBasedPredictDate_noPills() {
        // 피임약이 없으면 null 반환
        val result = PillCalculator.calculatePillBasedPredictDate(
            startDate = LocalDate(2025, 1, 1),
            pillPackages = emptyList(),
            pillSettings = PillSettings(isCalculatingWithPill = true, pillCount = 21, restPill = 7),
            normalPeriod = 30
        )

        assertEquals(null, result)
    }

    @Test
    fun testCalculatePillBasedPredictDate_pillAfterPredictDate() {
        // 피임약 시작이 정상 예정일 이후면 null
        val pillPackages = listOf(
            PillPackage(
                packageStart = LocalDate(2025, 2, 10), // 정상 예정일 이후
                pillCount = 21,
                restDays = 7
            )
        )

        val result = PillCalculator.calculatePillBasedPredictDate(
            startDate = LocalDate(2025, 1, 1),
            pillPackages = pillPackages,
            pillSettings = PillSettings(isCalculatingWithPill = true, pillCount = 21, restPill = 7),
            normalPeriod = 30 // 정상 예정일: 1/31
        )

        assertEquals(null, result)
    }

    @Test
    fun testCalculatePillBasedPredictDate_pillBasedCalculation() {
        // 피임약 기반 예정일 계산: 마지막 피임약 시작일 + pillCount + 2
        val pillPackages = listOf(
            PillPackage(
                packageStart = LocalDate(2025, 1, 10),
                pillCount = 21,
                restDays = 7
            ),
            PillPackage(
                packageStart = LocalDate(2025, 2, 7),
                pillCount = 21,
                restDays = 7
            )
        )

        val result = PillCalculator.calculatePillBasedPredictDate(
            startDate = LocalDate(2025, 1, 1),
            pillPackages = pillPackages,
            pillSettings = PillSettings(isCalculatingWithPill = true, pillCount = 21, restPill = 7),
            normalPeriod = 30
        )

        // 마지막 피임약(2/7) + 21 + 2 = 3/2
        assertEquals(LocalDate(2025, 3, 2), result)
    }

    @Test
    fun testIsPillActiveOnDate_notCalculating() {
        // 피임약 계산이 비활성화되면 false
        val pillPackages = listOf(
            PillPackage(
                packageStart = LocalDate(2025, 1, 10),
                pillCount = 21,
                restDays = 7
            )
        )

        val result = PillCalculator.isPillActiveOnDate(
            date = LocalDate(2025, 1, 15),
            pillPackages = pillPackages,
            pillSettings = PillSettings(isCalculatingWithPill = false) // 비활성화
        )

        assertFalse(result)
    }

    @Test
    fun testIsPillActiveOnDate_noPills() {
        // 피임약이 없으면 false
        val result = PillCalculator.isPillActiveOnDate(
            date = LocalDate(2025, 1, 15),
            pillPackages = emptyList(),
            pillSettings = PillSettings(isCalculatingWithPill = true)
        )

        assertFalse(result)
    }

    @Test
    fun testIsPillActiveOnDate_duringPillPeriod() {
        // 피임약 복용 기간 중이면 true
        val pillPackages = listOf(
            PillPackage(
                packageStart = LocalDate(2025, 1, 10), // 1/10 ~ 1/30 (21일)
                pillCount = 21,
                restDays = 7
            )
        )

        val result = PillCalculator.isPillActiveOnDate(
            date = LocalDate(2025, 1, 20), // 복용 기간 중
            pillPackages = pillPackages,
            pillSettings = PillSettings(isCalculatingWithPill = true, pillCount = 21)
        )

        assertTrue(result)
    }

    @Test
    fun testIsPillActiveOnDate_afterPillPeriod() {
        // 피임약 복용 기간이 끝나면 false
        val pillPackages = listOf(
            PillPackage(
                packageStart = LocalDate(2025, 1, 10), // 1/10 ~ 1/30 (21일)
                pillCount = 21,
                restDays = 7
            )
        )

        val result = PillCalculator.isPillActiveOnDate(
            date = LocalDate(2025, 2, 5), // 복용 기간 이후
            pillPackages = pillPackages,
            pillSettings = PillSettings(isCalculatingWithPill = true, pillCount = 21)
        )

        assertFalse(result)
    }

    @Test
    fun testIsPillActiveOnDate_beforePillPeriod() {
        // 피임약 복용 시작 전이면 false
        val pillPackages = listOf(
            PillPackage(
                packageStart = LocalDate(2025, 1, 10),
                pillCount = 21,
                restDays = 7
            )
        )

        val result = PillCalculator.isPillActiveOnDate(
            date = LocalDate(2025, 1, 5), // 복용 시작 전
            pillPackages = pillPackages,
            pillSettings = PillSettings(isCalculatingWithPill = true, pillCount = 21)
        )

        assertFalse(result)
    }
}
