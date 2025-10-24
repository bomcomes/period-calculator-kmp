package com.bomcomes.calculator.service

import com.bomcomes.calculator.PeriodCalculatorV2
import com.bomcomes.calculator.models.*
import com.bomcomes.calculator.repository.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.*

/**
 * 생리 주기 계산 서비스
 *
 * Repository에서 데이터를 가져와 PeriodCalculatorV2에 전달하는 중간 레이어
 */
class PeriodCalculatorService(
    private val periodRepo: PeriodRepository,
    private val ovulationTestRepo: OvulationTestRepository,
    private val ovulationDayRepo: OvulationDayRepository,
    private val pillRepo: PillRepository,
    private val pregnancyRepo: PregnancyRepository,
    private val settingsRepo: PeriodSettingsRepository
) {

    /**
     * 특정 날짜의 달력 상태 계산
     *
     * @param date 조회할 날짜
     * @return 달력 상태 정보
     */
    suspend fun getStatus(date: LocalDate): CalendarStatus {
        val input = buildInput(
            from = date.minus(90, DateTimeUnit.DAY),  // 최근 3개월
            to = date.plus(90, DateTimeUnit.DAY)       // 향후 3개월
        )

        return PeriodCalculatorV2.calculateStatus(input, date)
    }

    /**
     * 기간 내의 생리 주기 정보 계산
     *
     * @param from 시작 날짜
     * @param to 종료 날짜
     * @return 생리 주기 정보 리스트
     */
    suspend fun getMenstrualCycles(
        from: LocalDate,
        to: LocalDate
    ): List<PeriodCycle> {
        val input = buildInput(from, to)
        return PeriodCalculatorV2.calculateMenstrualCycles(input, from, to)
    }

    /**
     * 한 달치 데이터 일괄 조회 (성능 최적화)
     *
     * @param year 년도
     * @param month 월 (1-12)
     * @return 해당 월의 모든 주기 정보와 일별 상태
     */
    suspend fun getMonthData(year: Int, month: Int): MonthData {
        val from = LocalDate(year, month, 1)
        val to = from.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)

        val cycles = getMenstrualCycles(from, to)

        // 각 날짜의 상태를 미리 계산 (배치 처리)
        val dailyStatus = mutableMapOf<LocalDate, CalendarStatus>()
        var currentDate = from
        while (currentDate <= to) {
            dailyStatus[currentDate] = getStatus(currentDate)
            currentDate = currentDate.plus(1, DateTimeUnit.DAY)
        }

        return MonthData(
            year = year,
            month = month,
            cycles = cycles,
            dailyStatus = dailyStatus
        )
    }

    /**
     * 다음 생리 예정일 간단 계산
     */
    suspend fun getNextPeriodDate(): DateRange? {
        val lastPeriod = periodRepo.getLatestPeriod() ?: return null
        val settings = settingsRepo.getSettings()

        return PeriodCalculatorV2.calculateNextPeriod(
            lastPeriodStartDate = lastPeriod.startDate,
            lastPeriodEndDate = lastPeriod.endDate,
            periodSettings = settings
        )
    }

    /**
     * 현재 임신 정보 조회
     */
    suspend fun getActivePregnancy(): PregnancyInfo? {
        return pregnancyRepo.getActivePregnancy()
    }

    /**
     * Repository에서 데이터를 모아서 PeriodCycleInput 생성
     *
     * @param from 시작 날짜
     * @param to 종료 날짜
     * @return 계산에 필요한 모든 입력 데이터
     */
    private suspend fun buildInput(
        from: LocalDate,
        to: LocalDate
    ): PeriodCycleInput = coroutineScope {
        // 병렬로 데이터 로딩 (성능 최적화)
        val periodsDeferred = async { periodRepo.getPeriods(from, to) }
        val ovulationTestsDeferred = async { ovulationTestRepo.getTests(from, to) }
        val ovulationDaysDeferred = async { ovulationDayRepo.getOvulationDays(from, to) }
        val pillPackagesDeferred = async { pillRepo.getPillPackages(from) }
        val pillSettingsDeferred = async { pillRepo.getPillSettings() }
        val pregnancyDeferred = async { pregnancyRepo.getActivePregnancy() }
        val settingsDeferred = async { settingsRepo.getSettings() }

        PeriodCycleInput(
            periods = periodsDeferred.await(),
            periodSettings = settingsDeferred.await(),
            ovulationTests = ovulationTestsDeferred.await(),
            userOvulationDays = ovulationDaysDeferred.await(),
            pillPackages = pillPackagesDeferred.await(),
            pillSettings = pillSettingsDeferred.await(),
            pregnancy = pregnancyDeferred.await()
        )
    }
}

/**
 * 한 달치 데이터
 */
data class MonthData(
    val year: Int,
    val month: Int,
    val cycles: List<PeriodCycle>,
    val dailyStatus: Map<LocalDate, CalendarStatus>
) {
    /**
     * 특정 날짜의 상태 조회
     */
    fun getStatusForDate(date: LocalDate): CalendarStatus? {
        return dailyStatus[date]
    }

    /**
     * 생리일 리스트
     */
    fun getPeriodDates(): List<LocalDate> {
        return dailyStatus.filter { (_, status) ->
            status.calendarType == CalendarStatus.CalendarType.THE_DAY
        }.keys.toList()
    }

    /**
     * 배란일 리스트
     */
    fun getOvulationDates(): List<LocalDate> {
        return dailyStatus.filter { (_, status) ->
            status.calendarType == CalendarStatus.CalendarType.OVULATION_DAY
        }.keys.toList()
    }

    /**
     * 가임기 리스트
     */
    fun getFertileDates(): List<LocalDate> {
        return dailyStatus.filter { (_, status) ->
            status.calendarType == CalendarStatus.CalendarType.CHILDBEARING_AGE
        }.keys.toList()
    }
}
