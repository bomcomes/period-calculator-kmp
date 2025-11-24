package com.bomcomes.calculator

import com.bomcomes.calculator.helpers.*
import com.bomcomes.calculator.models.*
import com.bomcomes.calculator.repository.PeriodDataRepository
import com.bomcomes.calculator.utils.DateUtils
import kotlinx.datetime.*

/**
 * Period Calculator
 *
 * 생리 주기, 배란기, 가임기 계산
 * iOS PredictCalculator 로직 기반
 */
object PeriodCalculator {

    init {
        println("[PeriodCalculator] ${Version.getVersionString()}")
    }

    /**
     * 주기 정보 계산 (Repository 패턴 사용)
     *
     * 각 플랫폼이 자신의 로컬 DB에서 데이터를 가져옵니다.
     * - iOS: Realm
     * - Android: Room
     * - Firebase: Firestore
     *
     * @param repository 데이터 제공자 (각 플랫폼이 구현)
     * @param fromDate 검색 시작 날짜 (julianDay)
     * @param toDate 검색 종료 날짜 (julianDay)
     * @param today 오늘 날짜 (선택, 없으면 현재 시스템 시간 사용) (julianDay)
     * @return 주기 정보 리스트
     */
    suspend fun calculateCycleInfo(
        repository: PeriodDataRepository,
        fromDate: Double,
        toDate: Double,
        today: Double? = null
    ): List<CycleInfo> {
        val pregnancy = repository.getActivePregnancy()

        // 임신 필터링을 위한 날짜 계산
        val excludeBeforeDate = pregnancy?.dueDate
        val excludeAfterDate = pregnancy?.startsDate

        // Repository에서 범위 내 생리 가져오기
        val periodsInRange = repository.getPeriods(fromDate, toDate).toMutableList()

        // iOS 패턴: 조건부로 이전/다음 생리 추가
        if (periodsInRange.isNotEmpty()) {
            val firstPeriod = periodsInRange.first()
            val lastPeriod = periodsInRange.last()

            // fromDate가 첫 번째 생리보다 이전이거나 같으면 이전 생리 추가
            // (첫 번째 생리의 이전 주기 정보도 필요)
            if (fromDate < firstPeriod.startDate) {
                val previousPeriod = repository.getLastPeriodBefore(firstPeriod.startDate - 1, excludeBeforeDate)
                if (previousPeriod != null && previousPeriod.startDate != firstPeriod.startDate) {
                    periodsInRange.add(0, previousPeriod)
                }
            }

            // toDate가 마지막 생리보다 이후면 다음 생리 추가
            if (toDate > lastPeriod.endDate) {
                val nextPeriod = repository.getFirstPeriodAfter(toDate, excludeAfterDate)
                if (nextPeriod != null) {
                    periodsInRange.add(nextPeriod)
                }
            }
        } else {
            // 범위 내 생리가 없을 때
            val previousPeriod = repository.getLastPeriodBefore(fromDate, excludeBeforeDate)

            // 이전 생리가 있을 때만 처리 (주기 계산 기준이 필요)
            if (previousPeriod != null) {
                periodsInRange.add(previousPeriod)

                // 이전 생리가 있을 때만 이후 생리도 가져오기
                val nextPeriod = repository.getFirstPeriodAfter(toDate, excludeAfterDate)
                if (nextPeriod != null) {
                    periodsInRange.add(nextPeriod)
                }
            }
            // 이전 생리가 없으면 이후 생리도 가져오지 않음 (범위에 표시할 주기 정보가 없음)
        }

        val periodSettings = repository.getPeriodSettings()

        // 배란기 데이터는 항상 가져옴 (사용자가 입력했을 수 있음)
        val ovulationTests = repository.getOvulationTests(fromDate, toDate)
        val userOvulationDays = repository.getUserOvulationDays(fromDate, toDate)

        // 피임약 설정 확인 후 필요할 때만 패키지 데이터 가져오기
        val pillSettings = repository.getPillSettings()
        val pillPackages = if (pillSettings.isCalculatingWithPill) {
            repository.getPillPackages()
        } else {
            emptyList()
        }

        // CycleInput 생성
        val input = CycleInput(
            periods = periodsInRange,
            periodSettings = periodSettings,
            ovulationTests = ovulationTests,
            userOvulationDays = userOvulationDays,
            pillPackages = pillPackages,
            pillSettings = pillSettings,
            pregnancy = pregnancy
        )

        // 계산 로직 호출 및 반환
        // 필터링은 이미 CycleCalculator에서 수행되므로 모두 반환
        return calculateCycleInfo(input, fromDate, toDate, today)
    }

    /**
     * 주기 정보 계산 (직접 데이터 전달)
     *
     * @param input 모든 필요한 데이터
     * @param fromDate 검색 시작 날짜 (julianDay)
     * @param toDate 검색 종료 날짜 (julianDay)
     * @param today 오늘 날짜 (선택, 없으면 현재 시스템 시간 사용) (julianDay)
     * @return 주기 정보 리스트
     */
    fun calculateCycleInfo(
        input: CycleInput,
        fromDate: Double,
        toDate: Double,
        today: Double? = null
    ): List<CycleInfo> {
        // 생리 기록이 없으면 빈 리스트 반환
        if (input.periods.isEmpty()) return emptyList()

        val sortedPeriods = input.periods.sortedBy { it.startDate }
        val averageCycle = input.periodSettings.getAverageCycle()

        // 피임약 복용 중인지 확인
        val isOnPill = input.pillSettings.isCalculatingWithPill && input.pillPackages.isNotEmpty()

        val actualToday = today ?: DateUtils.toJulianDay(Clock.System.todayIn(TimeZone.UTC))

        val cycles = mutableListOf<CycleInfo>()

        // iOS 패턴: 연속된 생리 쌍에 대해서만 주기 계산
        if (sortedPeriods.size >= 2) {
            // 마지막 제외하고 연속된 쌍 계산
            for (i in 0 until sortedPeriods.size - 1) {
                val currentPeriod = sortedPeriods[i]
                val nextPeriod = sortedPeriods[i + 1]

                // 실제 주기 계산
                val actualPeriod = (nextPeriod.startDate - currentPeriod.startDate).toInt()

                // 이 주기의 계산 범위: 현재 생리 시작 ~ 다음 생리 시작 전날
                // 배란기/가임기는 이 범위 내에서만 계산
                val cycleEndDate = nextPeriod.startDate - 1
                val cycleToDate = if (cycleEndDate < toDate) cycleEndDate else toDate

                val cycle = setupResult(
                    input = input,
                    periodRecord = currentPeriod,
                    nextPeriod = nextPeriod,
                    fromDate = fromDate,
                    toDate = cycleToDate,
                    today = actualToday,
                    period = actualPeriod,
                    isThePill = isOnPill
                )

                cycles.add(cycle)
            }

            // 생리가 정확히 2개면 마지막 것도 추가 (평균 주기 사용)
            if (sortedPeriods.size == 2) {
                val lastPeriod = sortedPeriods.last()

                val cycle = setupResult(
                    input = input,
                    periodRecord = lastPeriod,
                    nextPeriod = null,
                    fromDate = fromDate,
                    toDate = toDate,
                    today = actualToday,
                    period = averageCycle,
                    isThePill = isOnPill
                )
                cycles.add(cycle)
            }

        } else if (sortedPeriods.size == 1) {
            // 생리가 1개만 있으면 평균 주기로 예측
            val singlePeriod = sortedPeriods.first()
            val cycle = setupResult(
                input = input,
                periodRecord = singlePeriod,
                nextPeriod = null,
                fromDate = fromDate,
                toDate = toDate,
                today = actualToday,
                period = averageCycle,
                isThePill = isOnPill
            )
            cycles.add(cycle)
        } else {
            // 생리가 0개면 기본 배란기 정보 제공 (첫 사용자)
            val ovulationCycle = setupOvulation(
                input = input,
                fromDate = fromDate,
                toDate = toDate + 2  // iOS: toDate + 2일
            )
            if (ovulationCycle != null) {
                cycles.add(ovulationCycle)
            }
        }

        return cycles
    }



    /**
     * 날짜 범위의 상태를 한 번에 계산
     *
     * @param input 모든 필요한 데이터
     * @param fromDate 시작 날짜 (julianDay)
     * @param toDate 종료 날짜 (julianDay)
     * @param today 오늘 날짜 (과거/미래 구분용) (julianDay)
     * @return 각 날짜의 상태 리스트
     */
    fun getDayStatuses(
        input: CycleInput,
        fromDate: Double,
        toDate: Double,
        today: Double
    ): List<DayStatus> {
        // 날짜 범위를 리스트로 변환
        val dates = mutableListOf<Double>()
        var current = fromDate
        while (current <= toDate) {
            dates.add(current)
            current = current + 1
        }

        return getDayStatusesForDates(input, dates, today)
    }

    /**
     * 여러 날짜의 상태를 한 번에 계산
     *
     * 캘린더 월별 표시 시 성능 최적화
     *
     * @param input 모든 필요한 데이터
     * @param dates 확인할 날짜 리스트 (julianDay)
     * @param today 오늘 날짜 (과거/미래 구분용) (julianDay)
     * @return 각 날짜의 상태 리스트
     */
    fun getDayStatusesForDates(
        input: CycleInput,
        dates: List<Double>,
        today: Double
    ): List<DayStatus> {
        if (dates.isEmpty()) return emptyList()
        if (input.periods.isEmpty()) {
            return dates.map { date ->
                DayStatus(
                    date = date,
                    type = DayType.EMPTY,
                    gap = null,
                    period = input.periodSettings.getAverageCycle()
                )
            }
        }

        // 범위 내 주기를 한 번만 계산
        val firstDate = dates.minOrNull() ?: return emptyList()
        val lastDate = dates.maxOrNull() ?: return emptyList()
        val cycles = calculateCycleInfo(input, firstDate, lastDate, today)

        if (cycles.isEmpty()) {
            return dates.map { date ->
                DayStatus(
                    date = date,
                    type = DayType.NONE,
                    gap = null,
                    period = input.periodSettings.getAverageCycle()
                )
            }
        }

        val lastPeriod = input.periods.maxByOrNull { it.startDate }!!

        // 각 날짜의 상태 계산
        return dates.map { date ->
            val type = findDayType(cycles, date, today)
            val gap = if (type != DayType.EMPTY && type != DayType.NONE) {
                (date - lastPeriod.startDate).toInt()
            } else null

            DayStatus(
                date = date,
                type = type,
                gap = gap,
                period = cycles.firstOrNull()?.period ?: input.periodSettings.getAverageCycle()
            )
        }
    }

    /**
     * 단일 날짜의 상태 계산 (간편 함수)
     *
     * @param input 모든 필요한 데이터
     * @param date 확인할 날짜 (julianDay)
     * @param today 오늘 날짜 (ING/NEXT 구분용) (julianDay)
     * @return 날짜 상태
     */
    fun getDayStatus(
        input: CycleInput,
        date: Double,
        today: Double
    ): DayStatus {
        return getDayStatusesForDates(input, listOf(date), today).first()
    }

    /**
     * 주기 리스트에서 특정 날짜의 타입 찾기
     */
    private fun findDayType(cycles: List<CycleInfo>, date: Double, today: Double): DayType {
        if (cycles.isEmpty()) return DayType.NONE

        val cycle = cycles.firstOrNull() ?: return DayType.NONE

        // 임신 중
        if (cycle.pregnancyStartDate != null) {
            return DayType.PREGNANCY
        }

        // 실제 생리일 (과거/현재 vs 미래 구분)
        if (cycle.actualPeriod?.contains(date) == true) {
            return if (date <= today) {
                DayType.PERIOD_ONGOING
            } else {
                DayType.PERIOD_UPCOMING
            }
        }

        // 지연 (8일+ 구분)
        if (cycle.delayDay?.contains(date) == true) {
            return if (cycle.delayTheDays >= 8) {
                DayType.PERIOD_DELAYED_OVER
            } else {
                DayType.PERIOD_DELAYED
            }
        }

        // 생리 예측일
        if (cycle.predictDays.any { it.contains(date) }) {
            return DayType.PERIOD_PREDICTED
        }

        // 배란기
        if (cycle.ovulationDays.any { it.contains(date) }) {
            return DayType.OVULATION
        }

        // 가임기
        if (cycle.fertileDays.any { it.contains(date) }) {
            return DayType.FERTILE
        }

        return DayType.NONE
    }

    // MARK: - 내부 계산 함수

    /**
     * 주기 정보 설정
     */
    private fun setupResult(
        input: CycleInput,
        periodRecord: PeriodRecord,
        nextPeriod: PeriodRecord?,
        fromDate: Double,
        toDate: Double,
        today: Double,
        period: Int,
        isThePill: Boolean
    ): CycleInfo {
        // 임신 중이면 임신 정보만 반환
        if (input.pregnancy?.isActive() == true) {
            return CycleInfo(
                pk = periodRecord.pk,
                actualPeriod = DateRange(periodRecord.startDate, periodRecord.endDate),
                period = period,
                pregnancyStartDate = input.pregnancy.startsDate
            )
        }

        // 피임약 사용 시 thePillPeriod를 먼저 계산 (iOS와 동일하게 동적 계산)
        val thePillPeriod = if (isThePill && input.pillSettings.isCalculatingWithPill) {
            PillCalculator.calculatePillBasedPredictDate(
                startDate = periodRecord.startDate,
                pillPackages = input.pillPackages,
                pillSettings = input.pillSettings,
                normalPeriod = period
            )?.let { pillPredictDate ->
                (pillPredictDate - periodRecord.startDate).toInt()
            } ?: (input.pillSettings.pillCount + input.pillSettings.restPill)
        } else {
            null
        }

        // 피임약 사용 시 예정일 시작까지를 기준으로 delay 계산
        val effectivePeriod = if (thePillPeriod != null) {
            thePillPeriod
        } else {
            period
        }

        // 지연 일수 계산
        val delayDays = CycleCalculator.calculateDelayDays(
            lastTheDayStart = periodRecord.startDate,
            fromDate = fromDate,
            toDate = toDate,
            todayOnly = today,
            period = effectivePeriod
        )

        // 지연 기간
        val delayPeriodRaw = CycleCalculator.calculateDelayPeriod(
            lastTheDayStart = periodRecord.startDate,
            fromDate = fromDate,
            period = effectivePeriod,
            delayTheDays = delayDays
        )

        // 지연 기간도 범위 필터링: startDate > toDate이면 제외
        val delayPeriod = delayPeriodRaw?.let {
            if (it.startDate <= toDate) it else null
        }

        // delayPeriod가 필터링되면 delayTheDays도 0으로
        val filteredDelayDays = if (delayPeriod != null) delayDays else 0

        // 생리 예정일 계산
        val predictDays = setupPredict(
            input = input,
            periodRecord = periodRecord,
            fromDate = fromDate,
            toDate = toDate,
            period = period,
            delayDays = delayDays,
            isThePill = isThePill
        )

        // 배란기 계산 (피임약 복용 중에는 계산하지 않음)
        val ovulationDays = if (isThePill && input.pillSettings.isCalculatingWithPill) {
            emptyList()
        } else {
            calculateOvulationDays(
                input = input,
                lastPeriodStart = periodRecord.startDate,
                fromDate = fromDate,
                toDate = toDate,
                period = period,
                delayDays = delayDays
            )
        }

        // 가임기 계산 (피임약 복용 중에는 계산하지 않음)
        val fertileDays = if (isThePill && input.pillSettings.isCalculatingWithPill) {
            emptyList()
        } else {
            calculateFertileDays(
                input = input,
                lastPeriodStart = periodRecord.startDate,
                fromDate = fromDate,
                toDate = toDate,
                period = period,
                delayDays = delayDays,
                ovulationDays = ovulationDays
            )
        }

        // 현재 남은 휴약일 계산
        val restPillDays = if (isThePill && input.pillSettings.isCalculatingWithPill) {
            calculateRestPillDays(today, input.pillPackages, input.pillSettings)
        } else {
            null
        }

        return CycleInfo(
            pk = periodRecord.pk,
            actualPeriod = DateRange(periodRecord.startDate, periodRecord.endDate),
            predictDays = predictDays,
            ovulationDays = ovulationDays,
            fertileDays = fertileDays,
            delayDay = delayPeriod,
            delayTheDays = filteredDelayDays,
            period = period,
            thePillPeriod = thePillPeriod,
            restPill = restPillDays
        )
    }

    /**
     * 남은 휴약일 계산
     */
    private fun calculateRestPillDays(
        today: Double,
        pillPackages: List<PillPackage>,
        pillSettings: PillSettings
    ): Int? {
        if (pillPackages.isEmpty()) return null

        // 현재 날짜가 속한 피임약 패키지 찾기
        for (pillPackage in pillPackages) {
            val packageStart = pillPackage.packageStart
            val packageEnd = packageStart + pillPackage.pillCount + pillPackage.restDays - 1

            if (today >= packageStart && today <= packageEnd) {
                // 현재 날짜가 이 패키지 내에 있음
                val dayInPackage = (today - packageStart).toInt() + 1

                if (dayInPackage > pillPackage.pillCount) {
                    // 휴약 기간
                    val restDayNumber = dayInPackage - pillPackage.pillCount
                    return pillPackage.restDays - restDayNumber + 1
                } else {
                    // 복용 기간
                    return 0
                }
            }
        }

        return null
    }

    /**
     * 생리 예정일 계산
     */
    private fun setupPredict(
        input: CycleInput,
        periodRecord: PeriodRecord,
        fromDate: Double,
        toDate: Double,
        period: Int,
        delayDays: Int,
        isThePill: Boolean
    ): List<DateRange> {
        // 8일 이상 지연 시 예정일 표시 안 함 (병원 진료 권장)
        if (delayDays >= 8) {
            return emptyList()
        }

        val days = input.periodSettings.getAverageDays()

        // 피임약 복용 중이면 피임약 기준 예정일 계산
        if (isThePill) {
            val pillBasedDate = PillCalculator.calculatePillBasedPredictDate(
                startDate = periodRecord.startDate,
                pillPackages = input.pillPackages,
                pillSettings = input.pillSettings,
                normalPeriod = period
            )

            if (pillBasedDate != null) {
                // 지연일 적용 (일반 주기와 동일하게)
                val adjustedPillDate = pillBasedDate + delayDays
                val predictEnd = adjustedPillDate + days - 1
                // 쿼리 범위와 겹치는 경우에만 반환
                if (predictEnd >= fromDate && adjustedPillDate <= toDate) {
                    return listOf(DateRange(adjustedPillDate, predictEnd))
                }
                return emptyList()
            }
        }

        // 일반 예정일 계산
        val predictDays = CycleCalculator.predictInRange(
            isPredict = true,
            lastTheDayStart = periodRecord.startDate,
            fromDate = fromDate,
            toDate = toDate,
            period = period,
            rangeStart = 0,
            rangeEnd = days - 1,
            delayTheDays = delayDays
        )

        return OvulationCalculator.filterByPregnancy(predictDays, input.pregnancy)
    }

    /**
     * 배란기 계산
     */
    private fun calculateOvulationDays(
        input: CycleInput,
        lastPeriodStart: Double,
        fromDate: Double,
        toDate: Double,
        period: Int,
        delayDays: Int
    ): List<DateRange> {
        // 사용자 입력 또는 테스트 결과가 있는지 확인
        val hasUserInput = input.userOvulationDays.isNotEmpty() ||
                          input.ovulationTests.any { it.result == TestResult.POSITIVE }

        if (hasUserInput) {
            // 사용자 입력 데이터 기반 배란기
            val combinedDates = OvulationCalculator.combineOvulationDates(
                ovulationTests = input.ovulationTests,
                userOvulationDays = input.userOvulationDays,
                fromDate = fromDate,
                toDate = toDate
            )

            val ovulationRanges = OvulationCalculator.prepareOvulationDayRanges(combinedDates)
            return OvulationCalculator.filterByPregnancy(ovulationRanges, input.pregnancy)
        }

        // 주기 기반 배란기 계산
        // 배란기는 생리 시작일 이후부터 계산
        val effectiveFromDate = if (fromDate < lastPeriodStart) lastPeriodStart else fromDate

        val (ovulStart, ovulEnd) = CycleCalculator.calculateOvulationRange(period)

        // 첫 번째 주기의 끝 날짜 계산
        val firstCycleEnd = lastPeriodStart + period - 1

        // 첫 번째 주기 배란기 (실제 생리 기록, delay 미적용)
        val firstCycleOvulation = CycleCalculator.predictInRange(
            isPredict = false,
            lastTheDayStart = lastPeriodStart,
            fromDate = effectiveFromDate,
            toDate = minOf(toDate, firstCycleEnd),  // 첫 주기 범위로 제한
            period = period,
            rangeStart = ovulStart,
            rangeEnd = ovulEnd,
            delayTheDays = 0  // 실제 생리 기록은 delay 미적용
        )

        // 미래 주기 배란기 (예정일, delay 적용)
        val futureCyclesOvulation = CycleCalculator.predictInRange(
            isPredict = false,
            lastTheDayStart = lastPeriodStart + period,  // 다음 주기부터 시작
            fromDate = fromDate,  // 원래 조회 시작일 사용
            toDate = toDate,
            period = period,
            rangeStart = ovulStart,
            rangeEnd = ovulEnd,
            delayTheDays = delayDays  // 미래 주기는 delay 적용
        )

        val ovulationDays = firstCycleOvulation + futureCyclesOvulation

        return OvulationCalculator.filterByPregnancy(ovulationDays, input.pregnancy)
    }

    /**
     * 가임기 계산
     */
    private fun calculateFertileDays(
        input: CycleInput,
        lastPeriodStart: Double,
        fromDate: Double,
        toDate: Double,
        period: Int,
        delayDays: Int,
        ovulationDays: List<DateRange>
    ): List<DateRange> {
        // 사용자가 배란일을 직접 입력하거나 테스트 양성인 경우만 배란일 기준으로 가임기 계산 (배란일 -2 ~ +1)
        val hasUserInput = input.userOvulationDays.isNotEmpty() ||
                          input.ovulationTests.any { it.result == TestResult.POSITIVE }

        if (hasUserInput && ovulationDays.isNotEmpty()) {
            val fertileFromOvulation = OvulationCalculator.calculateFertileWindowFromOvulation(ovulationDays)
            return OvulationCalculator.filterByPregnancy(fertileFromOvulation, input.pregnancy)
        }

        // 주기 기반 가임기 계산
        // 가임기는 생리 시작일 이후부터 계산
        val effectiveFromDate = if (fromDate < lastPeriodStart) lastPeriodStart else fromDate

        val (fertileStart, fertileEnd) = CycleCalculator.calculateChildbearingAgeRange(period)

        // 첫 번째 주기의 끝 날짜 계산
        val firstCycleEnd = lastPeriodStart + period - 1

        // 첫 번째 주기 가임기 (실제 생리 기록, delay 미적용)
        val firstCycleFertile = CycleCalculator.predictInRange(
            isPredict = false,
            lastTheDayStart = lastPeriodStart,
            fromDate = effectiveFromDate,
            toDate = minOf(toDate, firstCycleEnd),  // 첫 주기 범위로 제한
            period = period,
            rangeStart = fertileStart,
            rangeEnd = fertileEnd,
            delayTheDays = 0  // 실제 생리 기록은 delay 미적용
        )

        // 미래 주기 가임기 (예정일, delay 적용)
        val futureCyclesFertile = CycleCalculator.predictInRange(
            isPredict = false,
            lastTheDayStart = lastPeriodStart + period,  // 다음 주기부터 시작
            fromDate = fromDate,  // 원래 조회 시작일 사용
            toDate = toDate,
            period = period,
            rangeStart = fertileStart,
            rangeEnd = fertileEnd,
            delayTheDays = delayDays  // 미래 주기는 delay 적용
        )

        val fertileDays = firstCycleFertile + futureCyclesFertile

        return OvulationCalculator.filterByPregnancy(fertileDays, input.pregnancy)
    }

    /**
     * 생리 기록이 없을 때 기본 배란기 정보 제공
     *
     * 첫 사용자를 위한 기본 정보 제공
     * 사용자가 입력한 배란일 데이터가 있으면 그것을 사용
     *
     * @param input 입력 데이터
     * @param fromDate 시작 날짜 (julianDay)
     * @param toDate 종료 날짜 (julianDay)
     * @return 기본 주기 정보 (배란기만 포함)
     */
    private fun setupOvulation(
        input: CycleInput,
        fromDate: Double,
        toDate: Double
    ): CycleInfo? {
        // 사용자 입력 배란일이 있는지 확인
        val hasOvulationData = input.userOvulationDays.isNotEmpty() ||
                              input.ovulationTests.any { it.result == TestResult.POSITIVE }

        if (!hasOvulationData) {
            // 배란일 데이터도 없으면 null 반환 (아무것도 표시하지 않음)
            return null
        }

        // 사용자 입력 배란일만 표시
        val combinedDates = OvulationCalculator.combineOvulationDates(
            ovulationTests = input.ovulationTests,
            userOvulationDays = input.userOvulationDays,
            fromDate = fromDate,
            toDate = toDate
        )

        if (combinedDates.isEmpty()) {
            return null
        }

        val ovulationRanges = OvulationCalculator.prepareOvulationDayRanges(combinedDates)
        val filteredOvulation = OvulationCalculator.filterByPregnancy(ovulationRanges, input.pregnancy)

        if (filteredOvulation.isEmpty()) {
            return null
        }

        // 배란일 기준 가임기 계산
        val childbearingAges = OvulationCalculator.calculateFertileWindowFromOvulation(filteredOvulation)
        val filteredChildbearing = OvulationCalculator.filterByPregnancy(childbearingAges, input.pregnancy)

        return CycleInfo(
            pk = "",
            actualPeriod = null,
            predictDays = emptyList(),
            ovulationDays = filteredOvulation,
            fertileDays = filteredChildbearing,
            delayDay = null,
            delayTheDays = 0,
            period = input.periodSettings.getAverageCycle()
        )
    }
}
