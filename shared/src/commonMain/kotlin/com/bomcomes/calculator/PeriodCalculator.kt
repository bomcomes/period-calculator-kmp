package com.bomcomes.calculator

import com.bomcomes.calculator.models.*
import kotlinx.datetime.*

/**
 * Period Calculator
 *
 * 생리 주기, 배란일, 가임기 계산
 * iOS PredictCalculator 로직 기반
 */
object PeriodCalculator {

    // MARK: - 공개 API

    /**
     * 생리 주기 정보 계산 (메인 함수)
     *
     * @param input 모든 필요한 데이터
     * @param fromDate 검색 시작 날짜
     * @param toDate 검색 종료 날짜
     * @return 생리 주기 리스트
     */
    fun calculateMenstrualCycles(
        input: PeriodCycleInput,
        fromDate: LocalDate,
        toDate: LocalDate
    ): List<PeriodCycle> {
        // 생리 기록이 없으면 빈 리스트 반환
        if (input.periods.isEmpty()) return emptyList()

        val sortedPeriods = input.periods.sortedByDescending { it.startDate }
        val lastPeriod = sortedPeriods.first()
        val averageCycle = input.periodSettings.getAverageCycle()

        // 피임약 복용 중인지 확인
        val isOnPill = input.pillSettings.isCalculatingWithPill && input.pillPackages.isNotEmpty()

        return listOf(
            setupResult(
                input = input,
                theDay = lastPeriod,
                fromDate = fromDate,
                toDate = toDate,
                todayOnly = Clock.System.todayIn(TimeZone.UTC),
                period = averageCycle,
                isThePill = isOnPill
            )
        )
    }

    /**
     * 특정 날짜의 달력 상태 계산
     *
     * @param input 모든 필요한 데이터
     * @param date 확인할 날짜
     * @return 달력 상태
     */
    fun calculateCalendarStatus(
        input: PeriodCycleInput,
        date: LocalDate
    ): CalendarStatus {
        if (input.periods.isEmpty()) {
            return CalendarStatus(
                calendarType = CalendarStatus.CalendarType.NONE,
                gap = 0,
                probability = CalendarStatus.ProbabilityOfPregnancy.INPUT_THE_DAY,
                period = input.periodSettings.getAverageCycle()
            )
        }

        val cycles = calculateMenstrualCycles(input, date, date)
        if (cycles.isEmpty()) {
            return CalendarStatus(
                calendarType = CalendarStatus.CalendarType.NONE,
                gap = 0,
                probability = CalendarStatus.ProbabilityOfPregnancy.NO_THE_DAY,
                period = input.periodSettings.getAverageCycle()
            )
        }

        val cycle = cycles.first()
        val lastPeriod = input.periods.maxByOrNull { it.startDate }!!

        // 임신 중
        if (cycle.pregnancyStartDate != null) {
            return CalendarStatus(
                calendarType = CalendarStatus.CalendarType.NONE,
                gap = lastPeriod.startDate.daysUntil(date),
                probability = CalendarStatus.ProbabilityOfPregnancy.PREGNANCY,
                period = cycle.period
            )
        }

        // 생리 중
        if (cycle.theDay?.contains(date) == true) {
            return CalendarStatus(
                calendarType = CalendarStatus.CalendarType.THE_DAY,
                gap = lastPeriod.startDate.daysUntil(date),
                probability = CalendarStatus.ProbabilityOfPregnancy.LOW,
                period = cycle.period
            )
        }

        // 지연 기간
        if (cycle.delayDay?.contains(date) == true) {
            val delayDays = cycle.delayTheDays
            val probability = when {
                delayDays >= 8 -> CalendarStatus.ProbabilityOfPregnancy.HOSPITAL_OVER_DELAY_8
                else -> CalendarStatus.ProbabilityOfPregnancy.HIGH
            }
            return CalendarStatus(
                calendarType = CalendarStatus.CalendarType.DELAY,
                gap = lastPeriod.startDate.daysUntil(date),
                probability = probability,
                period = cycle.period
            )
        }

        // 생리 예정일
        if (cycle.predictDays.any { it.contains(date) }) {
            return CalendarStatus(
                calendarType = CalendarStatus.CalendarType.PREDICT,
                gap = lastPeriod.startDate.daysUntil(date),
                probability = CalendarStatus.ProbabilityOfPregnancy.LOW,
                period = cycle.period
            )
        }

        // 배란일
        if (cycle.ovulationDays.any { it.contains(date) }) {
            return CalendarStatus(
                calendarType = CalendarStatus.CalendarType.OVULATION_DAY,
                gap = lastPeriod.startDate.daysUntil(date),
                probability = CalendarStatus.ProbabilityOfPregnancy.HIGH,
                period = cycle.period
            )
        }

        // 가임기
        if (cycle.childbearingAges.any { it.contains(date) }) {
            return CalendarStatus(
                calendarType = CalendarStatus.CalendarType.CHILDBEARING_AGE,
                gap = lastPeriod.startDate.daysUntil(date),
                probability = CalendarStatus.ProbabilityOfPregnancy.MIDDLE,
                period = cycle.period
            )
        }

        // 기본 (일반일)
        return CalendarStatus(
            calendarType = CalendarStatus.CalendarType.NONE,
            gap = lastPeriod.startDate.daysUntil(date),
            probability = CalendarStatus.ProbabilityOfPregnancy.NORMAL,
            period = cycle.period
        )
    }

    // MARK: - 내부 계산 함수

    /**
     * 생리 주기 정보 설정
     */
    private fun setupResult(
        input: PeriodCycleInput,
        theDay: PeriodRecord,
        fromDate: LocalDate,
        toDate: LocalDate,
        todayOnly: LocalDate,
        period: Int,
        isThePill: Boolean
    ): PeriodCycle {
        // 임신 중이면 임신 정보만 반환
        if (input.pregnancy?.isActive() == true) {
            return PeriodCycle(
                pk = theDay.pk,
                theDay = DateRange(theDay.startDate, theDay.endDate),
                period = period,
                pregnancyStartDate = input.pregnancy.startsDate
            )
        }

        // 지연 일수 계산
        val delayTheDays = calculateDelayDays(
            lastTheDayStart = theDay.startDate,
            fromDate = fromDate,
            toDate = toDate,
            todayOnly = todayOnly,
            period = period
        )

        // 지연 기간
        val delayDay = calculateDelayPeriod(
            lastTheDayStart = theDay.startDate,
            fromDate = fromDate,
            toDate = toDate,
            period = period,
            delayTheDays = delayTheDays
        )

        // 생리 예정일 계산
        val predictDays = setupPredict(
            input = input,
            theDay = theDay,
            fromDate = fromDate,
            toDate = toDate,
            period = period,
            delayTheDays = delayTheDays,
            isThePill = isThePill
        )

        // 배란일 계산
        val ovulationDays = calculateOvulationDays(
            input = input,
            lastTheDayStart = theDay.startDate,
            fromDate = fromDate,
            toDate = toDate,
            period = period,
            delayTheDays = delayTheDays
        )

        // 가임기 계산
        val childbearingAges = calculateChildbearingAges(
            input = input,
            lastTheDayStart = theDay.startDate,
            fromDate = fromDate,
            toDate = toDate,
            period = period,
            delayTheDays = delayTheDays,
            ovulationDays = ovulationDays
        )

        return PeriodCycle(
            pk = theDay.pk,
            theDay = DateRange(theDay.startDate, theDay.endDate),
            predictDays = predictDays,
            ovulationDays = ovulationDays,
            childbearingAges = childbearingAges,
            delayDay = delayDay,
            delayTheDays = delayTheDays,
            period = period
        )
    }

    /**
     * 생리 예정일 계산
     */
    private fun setupPredict(
        input: PeriodCycleInput,
        theDay: PeriodRecord,
        fromDate: LocalDate,
        toDate: LocalDate,
        period: Int,
        delayTheDays: Int,
        isThePill: Boolean
    ): List<DateRange> {
        val days = input.periodSettings.days

        // 피임약 복용 중이면 피임약 기준 예정일 계산
        if (isThePill) {
            val pillBasedDate = calculatePillBasedPredictDate(
                startDate = theDay.startDate,
                pillPackages = input.pillPackages,
                pillSettings = input.pillSettings,
                normalPeriod = period
            )

            if (pillBasedDate != null) {
                val predictEnd = pillBasedDate.plus(days - 1, DateTimeUnit.DAY)
                return listOf(DateRange(pillBasedDate, predictEnd))
            }
        }

        // 일반 예정일 계산
        val predictDays = predictInRange(
            isPredict = true,
            lastTheDayStart = theDay.startDate,
            fromDate = fromDate,
            toDate = toDate,
            period = period,
            rangeStart = 0,
            rangeEnd = days - 1,
            delayTheDays = delayTheDays
        )

        return filterByPregnancy(predictDays, input.pregnancy)
    }

    /**
     * 배란일 계산
     */
    private fun calculateOvulationDays(
        input: PeriodCycleInput,
        lastTheDayStart: LocalDate,
        fromDate: LocalDate,
        toDate: LocalDate,
        period: Int,
        delayTheDays: Int
    ): List<DateRange> {
        // 사용자 입력 또는 테스트 결과가 있는지 확인
        val hasUserInput = input.userOvulationDays.isNotEmpty() ||
                          input.ovulationTests.any { it.result == OvulationTest.TestResult.POSITIVE }

        if (hasUserInput) {
            // 사용자 입력 데이터 기반 배란일
            val combinedDates = combineOvulationDates(
                ovulationTests = input.ovulationTests,
                userOvulationDays = input.userOvulationDays,
                fromDate = fromDate,
                toDate = toDate
            )

            val ovulationRanges = prepareOvulationDayRanges(combinedDates)
            return filterByPregnancy(ovulationRanges, input.pregnancy)
        }

        // 주기 기반 배란일 계산
        val (ovulStart, ovulEnd) = calculateOvulationRange(period)
        val ovulationDays = predictInRange(
            isPredict = false,
            lastTheDayStart = lastTheDayStart,
            fromDate = fromDate,
            toDate = toDate,
            period = period,
            rangeStart = ovulStart,
            rangeEnd = ovulEnd,
            delayTheDays = delayTheDays,
            isMultiple = false
        )

        return filterByPregnancy(ovulationDays, input.pregnancy)
    }

    /**
     * 가임기 계산
     */
    private fun calculateChildbearingAges(
        input: PeriodCycleInput,
        lastTheDayStart: LocalDate,
        fromDate: LocalDate,
        toDate: LocalDate,
        period: Int,
        delayTheDays: Int,
        ovulationDays: List<DateRange>
    ): List<DateRange> {
        // 배란일이 있으면 배란일 기준으로 가임기 계산 (배란일 -2 ~ +1)
        if (ovulationDays.isNotEmpty()) {
            val fertileFromOvulation = calculateFertileWindowFromOvulation(ovulationDays)
            return filterByPregnancy(fertileFromOvulation, input.pregnancy)
        }

        // 주기 기반 가임기 계산
        val (fertileStart, fertileEnd) = calculateChildbearingAgeRange(period)
        val childbearingAges = predictInRange(
            isPredict = false,
            lastTheDayStart = lastTheDayStart,
            fromDate = fromDate,
            toDate = toDate,
            period = period,
            rangeStart = fertileStart,
            rangeEnd = fertileEnd,
            delayTheDays = delayTheDays,
            isMultiple = false
        )

        return filterByPregnancy(childbearingAges, input.pregnancy)
    }

    // MARK: - Core 계산 함수

    /**
     * 가임기 시작/종료 인덱스 계산
     */
    fun calculateChildbearingAgeRange(period: Int): Pair<Int, Int> {
        var start: Int
        var end: Int

        if (period in 26..32) {
            start = 8 - 1  // 7
            end = 19 - 1   // 18
        } else {
            start = period - 19
            end = period - 11
        }

        if (start < 0) start = 0
        if (end < 0) end = 0

        return Pair(start, end)
    }

    /**
     * 배란일 시작/종료 인덱스 계산
     */
    fun calculateOvulationRange(period: Int): Pair<Int, Int> {
        var start: Int
        var end: Int

        if (period in 26..32) {
            start = 13 - 1  // 12
            end = 15 - 1    // 14
        } else {
            start = period - 16
            end = period - 14
        }

        if (start < 0) start = 0
        if (end < 0) end = 0

        return Pair(start, end)
    }

    /**
     * 날짜 범위 내에서 주기적으로 반복되는 날짜 범위 예측
     */
    fun predictInRange(
        isPredict: Boolean,
        lastTheDayStart: LocalDate,
        fromDate: LocalDate,
        toDate: LocalDate,
        period: Int,
        rangeStart: Int,
        rangeEnd: Int,
        delayTheDays: Int = 0,
        isMultiple: Boolean = true
    ): List<DateRange> {
        val actualPeriod = if (period == 0) 1 else period

        // 지연이 7일 초과면 반환하지 않음
        if (delayTheDays > 7) {
            return emptyList()
        }

        val gapStart = lastTheDayStart.daysUntil(fromDate)
        val gapEnd = lastTheDayStart.daysUntil(toDate)

        val quotientStart = gapStart / actualPeriod
        var remainderStart = gapStart % actualPeriod
        val quotientEnd = gapEnd / actualPeriod
        val remainderEnd = gapEnd % actualPeriod

        if (remainderEnd < remainderStart) {
            remainderStart = 0
        }

        val startWithDelay = rangeStart + delayTheDays
        val endWithDelay = rangeEnd + delayTheDays

        val results = mutableListOf<DateRange>()

        // 조건 확인: 범위가 검색 기간과 겹치는지
        val condition1 = remainderStart <= startWithDelay && startWithDelay <= remainderEnd
        val condition2 = remainderStart <= endWithDelay && endWithDelay <= remainderEnd
        val condition3 = startWithDelay <= remainderStart && remainderEnd <= endWithDelay

        if (condition1 || condition2 || condition3) {
            for (index in quotientStart..quotientEnd) {
                val startDate = lastTheDayStart.plus(actualPeriod * index + startWithDelay, DateTimeUnit.DAY)
                val endDate = lastTheDayStart.plus(actualPeriod * index + endWithDelay, DateTimeUnit.DAY)

                // 생리 기간과 생리 예정일 같게 나오는 이슈 방어
                if (endDate < lastTheDayStart) {
                    return emptyList()
                }

                if (startDate < lastTheDayStart) {
                    results.add(DateRange(lastTheDayStart, endDate))
                } else if (!(isPredict && startDate == lastTheDayStart)) {
                    results.add(DateRange(startDate, endDate))
                }

                // 생리 사이의 가임기/배란기는 한번만 나오도록
                if (!isMultiple) {
                    break
                }
            }
        }

        return results
    }

    /**
     * 지연 일수 계산
     */
    fun calculateDelayDays(
        lastTheDayStart: LocalDate,
        fromDate: LocalDate,
        toDate: LocalDate,
        todayOnly: LocalDate,
        period: Int
    ): Int {
        val gapEnd = lastTheDayStart.daysUntil(toDate) + 1
        if (gapEnd <= period) {
            return 0
        }

        val daysGap = lastTheDayStart.daysUntil(todayOnly)
        if (daysGap >= period - 1) {
            return daysGap - period + 1
        }

        return 0
    }

    /**
     * 지연 기간 계산
     */
    fun calculateDelayPeriod(
        lastTheDayStart: LocalDate,
        fromDate: LocalDate,
        toDate: LocalDate,
        period: Int,
        delayTheDays: Int
    ): DateRange? {
        val todayOnly = Clock.System.todayIn(TimeZone.UTC)

        if (delayTheDays > 0) {
            if (fromDate <= todayOnly) {
                val startDate = lastTheDayStart.plus(period, DateTimeUnit.DAY)
                val endDate = lastTheDayStart.plus(period + delayTheDays - 1, DateTimeUnit.DAY)
                return DateRange(startDate, endDate)
            }
        }

        return null
    }

    // MARK: - 배란일 관련 함수

    /**
     * 연속된 배란일 날짜들을 범위로 묶기
     */
    fun prepareOvulationDayRanges(ovulationDates: List<LocalDate>): List<DateRange> {
        if (ovulationDates.isEmpty()) return emptyList()

        val sortedDates = ovulationDates.sorted()
        val results = mutableListOf<DateRange>()

        for (date in sortedDates) {
            val last = results.lastOrNull()

            if (last != null) {
                val nextDate = last.endDate.plus(1, DateTimeUnit.DAY)
                if (nextDate == date) {
                    // 연속된 날짜면 범위 확장
                    results.removeLast()
                    results.add(DateRange(last.startDate, date))
                    continue
                }
            }

            // 새로운 범위 시작
            results.add(DateRange(date, date))
        }

        return results
    }

    /**
     * 배란일 우선순위로 결합
     */
    fun combineOvulationDates(
        ovulationTests: List<OvulationTest>,
        userOvulationDays: List<OvulationDay>,
        fromDate: LocalDate,
        toDate: LocalDate
    ): List<LocalDate> {
        val combined = mutableListOf<LocalDate>()

        // 1. 배란 테스트 양성 결과
        val positiveTests = ovulationTests.filter { test ->
            test.result == OvulationTest.TestResult.POSITIVE &&
            test.date >= fromDate &&
            test.date <= toDate
        }
        combined.addAll(positiveTests.map { it.date })

        // 2. 사용자 직접 입력 (우선순위 높음, 중복 제거)
        val userInputDates = userOvulationDays
            .filter { it.date >= fromDate && it.date <= toDate }
            .map { it.date }

        // 사용자 입력이 있으면 덮어쓰기
        for (userDate in userInputDates) {
            if (userDate !in combined) {
                combined.add(userDate)
            }
        }

        return combined.sorted()
    }

    /**
     * 배란일 기반으로 가임기 계산
     */
    fun calculateFertileWindowFromOvulation(ovulationRanges: List<DateRange>): List<DateRange> {
        return ovulationRanges.map { ovulation ->
            val start = ovulation.startDate.minus(2, DateTimeUnit.DAY)
            val end = ovulation.endDate.plus(1, DateTimeUnit.DAY)
            DateRange(start, end)
        }
    }

    /**
     * 임신 기간과 겹치는 날짜 범위 필터링
     */
    fun filterByPregnancy(
        ranges: List<DateRange>,
        pregnancy: PregnancyInfo?
    ): List<DateRange> {
        if (pregnancy == null) return ranges
        if (!pregnancy.isActive()) return ranges

        val startsDate = pregnancy.startsDate

        return ranges.mapNotNull { range ->
            when {
                // 임신 시작 전에 완전히 끝남
                range.endDate < startsDate -> range

                // 임신 기간과 겹침 - 임신 시작 전까지만
                range.startDate < startsDate -> {
                    val adjustedEnd = startsDate.minus(1, DateTimeUnit.DAY)
                    if (range.startDate < adjustedEnd) {
                        DateRange(range.startDate, adjustedEnd)
                    } else null
                }

                // 임신 시작 이후
                else -> null
            }
        }
    }

    // MARK: - 피임약 관련 함수

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

/**
 * LocalDate 확장 함수
 */
private fun LocalDate.daysUntil(other: LocalDate): Int {
    return this.until(other, DateTimeUnit.DAY)
}
