package com.bomcomes.calculator.repository

import com.bomcomes.calculator.models.*
import kotlinx.datetime.LocalDate

/**
 * In-Memory Period Repository for Testing
 */
class InMemoryPeriodRepository : PeriodDataRepository {
    private val periods = mutableListOf<PeriodRecord>()
    private var periodSettings = PeriodSettings()
    private val ovulationTests = mutableListOf<OvulationTest>()
    private val userOvulationDays = mutableListOf<OvulationDay>()
    private val pillPackages = mutableListOf<PillPackage>()
    private var pillSettings = PillSettings()
    private var activePregnancy: PregnancyInfo? = null

    // Test helper methods
    fun addPeriod(period: PeriodRecord) {
        periods.add(period)
    }

    fun setPeriodSettings(settings: PeriodSettings) {
        periodSettings = settings
    }

    fun addOvulationTest(test: OvulationTest) {
        ovulationTests.add(test)
    }

    fun addUserOvulationDay(day: OvulationDay) {
        userOvulationDays.add(day)
    }

    fun setActivePregnancy(pregnancy: PregnancyInfo?) {
        activePregnancy = pregnancy
    }

    fun clear() {
        periods.clear()
        ovulationTests.clear()
        userOvulationDays.clear()
        pillPackages.clear()
        periodSettings = PeriodSettings()
        pillSettings = PillSettings()
        activePregnancy = null
    }

    override suspend fun getPeriods(fromDate: LocalDate, toDate: LocalDate): List<PeriodRecord> {
        return periods.filter { period ->
            period.startDate <= toDate && period.endDate >= fromDate
        }.sortedBy { it.startDate }
    }

    override suspend fun getPeriodSettings(): PeriodSettings {
        return periodSettings
    }

    override suspend fun getOvulationTests(fromDate: LocalDate, toDate: LocalDate): List<OvulationTest> {
        return ovulationTests.filter { test ->
            test.date in fromDate..toDate
        }.sortedBy { it.date }
    }

    override suspend fun getUserOvulationDays(fromDate: LocalDate, toDate: LocalDate): List<OvulationDay> {
        return userOvulationDays.filter { day ->
            day.date in fromDate..toDate
        }.sortedBy { it.date }
    }

    override suspend fun getPillPackages(): List<PillPackage> {
        return pillPackages.toList()
    }

    override suspend fun getPillSettings(): PillSettings {
        return pillSettings
    }

    override suspend fun getActivePregnancy(): PregnancyInfo? {
        return activePregnancy?.takeIf { it.isActive() }
    }

    override suspend fun getLastPeriodBefore(date: LocalDate, excludeBeforeDate: LocalDate?): PeriodRecord? {
        return periods
            .filter { it.startDate <= date }
            .filter { excludeBeforeDate == null || it.startDate > excludeBeforeDate }
            .maxByOrNull { it.startDate }
    }

    override suspend fun getFirstPeriodAfter(date: LocalDate, excludeAfterDate: LocalDate?): PeriodRecord? {
        return periods
            .filter { it.startDate >= date }
            .filter { excludeAfterDate == null || it.endDate < excludeAfterDate }
            .minByOrNull { it.startDate }
    }
}
