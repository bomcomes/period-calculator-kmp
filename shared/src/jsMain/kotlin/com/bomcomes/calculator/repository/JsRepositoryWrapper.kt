package com.bomcomes.calculator.repository

import com.bomcomes.calculator.models.*
import kotlin.js.JsExport

/**
 * JavaScript Repository Wrapper
 *
 * JavaScript/Firebase에서 PeriodDataRepository를 구현하기 위한 메모리 기반 래퍼입니다.
 *
 * ## 왜 InMemoryPeriodRepository를 사용하지 않나요?
 *
 * InMemoryPeriodRepository는 테스트 전용입니다.
 * 프로덕션 코드에서 InMemoryPeriodRepository를 노출하면:
 * - iOS/Android에서 실수로 사용할 수 있음 (Realm/Room 대신)
 * - 테스트용 코드와 프로덕션용 코드가 혼재
 *
 * ## 사용 방법 (Firebase Functions):
 *
 * ```javascript
 * // 1. Firestore에서 필요한 범위의 데이터만 가져오기
 * const periods = await firestore.collection('periods')
 *   .where('startDate', '<=', toDate)
 *   .where('endDate', '>=', fromDate)
 *   .get();
 *
 * // 2. Repository 생성 및 데이터 채우기
 * const repository = new JsRepositoryWrapper();
 * periods.forEach(doc => {
 *   repository.addPeriod(new PeriodRecord(...));
 * });
 * repository.setPeriodSettings(...);
 *
 * // 3. 계산 함수에 전달 (Repository에서 메모리 검색)
 * const cycles = await calculateCycleInfo(repository, fromDate, toDate);
 * ```
 *
 * ## 각 플랫폼의 Repository 구현:
 * - iOS: RealmRepository (Realm DB)
 * - Android: RoomRepository (Room DB)
 * - JavaScript: JsRepositoryWrapper (메모리)
 */
@JsExport
class JsRepositoryWrapper : PeriodDataRepository {
    private val periods = mutableListOf<PeriodRecord>()
    private var periodSettings = PeriodSettings()
    private val ovulationTests = mutableListOf<OvulationTest>()
    private val userOvulationDays = mutableListOf<OvulationDay>()
    private val pillPackages = mutableListOf<PillPackage>()
    private var pillSettings = PillSettings()
    private var activePregnancy: PregnancyInfo? = null

    // Helper methods for populating data from JavaScript
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

    fun addPillPackage(pillPackage: PillPackage) {
        pillPackages.add(pillPackage)
    }

    fun setPillSettings(settings: PillSettings) {
        pillSettings = settings
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

    @JsExport.Ignore
    override suspend fun getPeriods(fromDate: Double, toDate: Double): List<PeriodRecord> {
        return periods.filter { period ->
            period.startDate <= toDate && period.endDate >= fromDate
        }.sortedBy { it.startDate }
    }

    @JsExport.Ignore
    override suspend fun getPeriodSettings(): PeriodSettings {
        return periodSettings
    }

    @JsExport.Ignore
    override suspend fun getOvulationTests(fromDate: Double, toDate: Double): List<OvulationTest> {
        return ovulationTests.filter { test ->
            test.date in fromDate..toDate
        }.sortedBy { it.date }
    }

    @JsExport.Ignore
    override suspend fun getUserOvulationDays(fromDate: Double, toDate: Double): List<OvulationDay> {
        return userOvulationDays.filter { day ->
            day.date in fromDate..toDate
        }.sortedBy { it.date }
    }

    @JsExport.Ignore
    override suspend fun getPillPackages(): List<PillPackage> {
        return pillPackages.toList()
    }

    @JsExport.Ignore
    override suspend fun getPillSettings(): PillSettings {
        return pillSettings
    }

    @JsExport.Ignore
    override suspend fun getActivePregnancy(): PregnancyInfo? {
        return activePregnancy?.takeIf { it.isActive() }
    }

    @JsExport.Ignore
    override suspend fun getLastPeriodBefore(date: Double, excludeBeforeDate: Double?): PeriodRecord? {
        return periods
            .filter { it.startDate <= date }
            .filter { excludeBeforeDate == null || it.startDate > excludeBeforeDate }
            .maxByOrNull { it.startDate }
    }

    @JsExport.Ignore
    override suspend fun getFirstPeriodAfter(date: Double, excludeAfterDate: Double?): PeriodRecord? {
        return periods
            .filter { it.startDate >= date }
            .filter { excludeAfterDate == null || it.endDate < excludeAfterDate }
            .minByOrNull { it.startDate }
    }
}
