package com.bomcomes.calculator.core

import com.bomcomes.calculator.models.*
import kotlinx.datetime.*

/**
 * PredictCalculatorUseCase의 헬퍼 메서드들
 */

/**
 * 생리 사이의 가임기/배란기 구하기
 */
internal fun PredictCalculatorUseCase.setupResultImpl(
    theDay: PeriodRecord,
    nextTheDay: PeriodRecord,
    from: LocalDate,
    to: LocalDate,
    period: Int,
    input: PeriodCycleInput,
    predictCalculator: PredictCalculator
): PeriodCycle? {
    var result = PeriodCycle(
        pk = theDay.pk,
        theDay = DateRange(theDay.startDate, theDay.endDate),
        period = period
    )

    val combineOvulationDays = mutableListOf<LocalDate>()

    // 배란 테스트 양성 결과 찾기
    val positiveTests = input.ovulationTests.filter { test ->
        test.result == OvulationTest.TestResult.POSITIVE &&
        test.date >= theDay.startDate &&
        (test.date <= to || test.date < nextTheDay.startDate)
    }
    combineOvulationDays.addAll(positiveTests.map { it.date })

    // 배란일 직접 입력 찾기
    val userOvulationDays = input.userOvulationDays.filter { ovDay ->
        ovDay.date >= theDay.startDate &&
        (ovDay.date <= to || ovDay.date < nextTheDay.startDate)
    }
    combineOvulationDays.addAll(userOvulationDays.map { it.date })

    // 사용자가 직접 입력한 배란일이 있으면 우선 사용
    if (combineOvulationDays.isNotEmpty()) {
        combineOvulationDays.sort()
        val ovulationDays = prepareOvulationDay(combineOvulationDays)

        val childbearingAges = ovulationDays.map { ovDay ->
            val start = ovDay.startDate.minus(2, DateTimeUnit.DAY)
            val end = ovDay.endDate.plus(1, DateTimeUnit.DAY)
            DateRange(start, end)
        }

        result = result.copy(
            childbearingAges = childbearingAges,
            ovulationDays = ovulationDays,
            isOvulationPeriodUserInput = true
        )
        return result
    }

    // 피임약 확인
    if (input.pillSettings.isCalculatingWithPill &&
        checkThePill(theDay.startDate, nextTheDay.startDate, input.pillPackages)) {
        // 피임약 복용 중이면 배란/가임기 없음
        return result
    }

    // 자동 계산으로 가임기/배란일 구하기
    val childbearingAgeStartEnd = predictCalculator.childbearingAgeStartEnd(period)
    val childbearingAges = predictCalculator.predict(
        isPredict = false,
        lastTheDayStart = theDay.startDate,
        fromDate = from,
        toDate = to,
        period = period,
        start = childbearingAgeStartEnd.first,
        end = childbearingAgeStartEnd.second,
        isMultiple = false
    )

    val ovulationDayStartEnd = predictCalculator.ovulationStartEnd(period)
    val ovulationDays = predictCalculator.predict(
        isPredict = false,
        lastTheDayStart = theDay.startDate,
        fromDate = from,
        toDate = to,
        period = period,
        start = ovulationDayStartEnd.first,
        end = ovulationDayStartEnd.second,
        isMultiple = false
    )

    result = result.copy(
        childbearingAges = childbearingAges,
        ovulationDays = ovulationDays
    )

    return result
}

/**
 * 마지막 생리일 이후의 예정일 예측
 */
internal fun PredictCalculatorUseCase.setupPredictImpl(
    theDay: PeriodRecord,
    from: LocalDate,
    to: LocalDate,
    period: Int,
    days: Int,
    input: PeriodCycleInput,
    predictCalculator: PredictCalculator
): PeriodCycle? {
    if (theDay.startDate > to) {
        return null
    }

    var predictDate = theDay.startDate
    var result = PeriodCycle(
        pk = theDay.pk,
        theDay = DateRange(theDay.startDate, theDay.endDate),
        period = period
    )

    val combineOvulationDays = mutableListOf<LocalDate>()

    // 기간 시작과 생리일 사이의 배란일 구하기
    if (theDay.startDate > from) {
        val testsBeforePeriod = input.ovulationTests.filter { test ->
            test.result == OvulationTest.TestResult.POSITIVE &&
            test.date >= from && test.date < theDay.startDate
        }
        combineOvulationDays.addAll(testsBeforePeriod.map { it.date })

        val userDaysBeforePeriod = input.userOvulationDays.filter { ovDay ->
            ovDay.date >= from && ovDay.date < theDay.startDate
        }
        combineOvulationDays.addAll(userDaysBeforePeriod.map { it.date })
    }

    // 마지막 생리일 이후의 배란일 구하기
    val testsAfterPeriod = input.ovulationTests.filter { test ->
        test.result == OvulationTest.TestResult.POSITIVE &&
        test.date >= theDay.startDate
    }
    combineOvulationDays.addAll(testsAfterPeriod.map { it.date })

    val userDaysAfterPeriod = input.userOvulationDays.filter { ovDay ->
        ovDay.date >= theDay.startDate
    }
    combineOvulationDays.addAll(userDaysAfterPeriod.map { it.date })

    // 사용자 입력 배란일로 가임기/배란기 구하기
    if (combineOvulationDays.isNotEmpty()) {
        combineOvulationDays.sort()
        val ovulationDays = prepareOvulationDay(combineOvulationDays)

        val childbearingAges = ovulationDays.map { ovDay ->
            val start = ovDay.startDate.minus(2, DateTimeUnit.DAY)
            val end = ovDay.endDate.plus(1, DateTimeUnit.DAY)
            DateRange(start, end)
        }

        result = result.copy(
            childbearingAges = childbearingAges,
            ovulationDays = ovulationDays,
            isOvulationPeriodUserInput = true
        )
    }

    // 피임약 확인
    if (input.pillSettings.isCalculatingWithPill) {
        val pillPackagesAfter = input.pillPackages.filter { it.packageStart >= theDay.startDate }

        if (pillPackagesAfter.isNotEmpty() && input.pillSettings.restPill == 0) {
            result = result.copy(restPill = 0)
            return result
        }

        checkThePillForPredict(theDay.startDate, input.pillPackages, input.pillSettings)?.let { thePillPredictDate ->
            val thePillPeriod = daysBetween(theDay.startDate, thePillPredictDate)
            val tmpPeriod = input.pillSettings.pillCount + input.pillSettings.restPill

            val delayTheDays = predictCalculator.delayTheDays(
                theDay.startDate, from, to, period = thePillPeriod
            )

            val predictStart = thePillPredictDate.plus(delayTheDays, DateTimeUnit.DAY)
            val predictEnd = predictStart.plus(4, DateTimeUnit.DAY)

            val today = Clock.System.now().toLocalDateTime(TimeZone.UTC).date
            val delayDay = predictCalculator.delayPeriod(
                theDay.startDate, from, to, thePillPeriod, delayTheDays
            )

            val predictDays = if (delayTheDays < 8) {
                val list = mutableListOf(DateRange(predictStart, predictEnd))
                list.addAll(
                    predictCalculator.predict(
                        isPredict = true,
                        lastTheDayStart = predictStart,
                        fromDate = from,
                        toDate = to,
                        period = tmpPeriod,
                        start = 0,
                        end = 4,
                        delayTheDays = 0
                    )
                )
                list
            } else {
                emptyList()
            }

            result = result.copy(
                thePillPeriod = thePillPeriod,
                period = tmpPeriod,
                delayDay = delayDay,
                delayTheDays = delayTheDays,
                predictDays = predictDays
            )

            return result
        }
    }

    // 배란일 기준 예정일 계산
    val delayTheDays: Int
    if (combineOvulationDays.isNotEmpty()) {
        val lastOvulationDate = combineOvulationDays.last()
        val predictStart = lastOvulationDate.plus(14, DateTimeUnit.DAY)
        val predictEnd = predictStart.plus(days - 1, DateTimeUnit.DAY)

        val ovulationDayPeriod = daysBetween(theDay.startDate, predictStart)
        delayTheDays = predictCalculator.delayTheDays(
            theDay.startDate, from, to, period = ovulationDayPeriod
        )

        val delayDay = predictCalculator.delayPeriod(
            theDay.startDate, from, to, ovulationDayPeriod, delayTheDays
        )

        val predictDays = if (delayTheDays < 8) {
            val list = mutableListOf(DateRange(predictStart, predictEnd))
            list.addAll(
                predictCalculator.predict(
                    isPredict = true,
                    lastTheDayStart = predictStart,
                    fromDate = from,
                    toDate = to,
                    period = period,
                    start = 0,
                    end = days - 1,
                    delayTheDays = delayTheDays
                )
            )
            list
        } else {
            emptyList()
        }

        predictDate = predictStart

        result = result.copy(
            ovulationDayPeriod = ovulationDayPeriod,
            delayDay = delayDay,
            delayTheDays = delayTheDays,
            predictDays = predictDays
        )
    } else {
        // 일반 주기 계산
        delayTheDays = predictCalculator.delayTheDays(
            predictDate, from, to, period = period
        )

        val delayDay = predictCalculator.delayPeriod(
            predictDate, from, to, period, delayTheDays
        )

        val predictDays = if (delayTheDays < 8) {
            predictCalculator.predict(
                isPredict = true,
                lastTheDayStart = predictDate,
                fromDate = from,
                toDate = to,
                period = period,
                start = 0,
                end = days - 1,
                delayTheDays = delayTheDays
            )
        } else {
            emptyList()
        }

        result = result.copy(
            delayDay = delayDay,
            delayTheDays = delayTheDays,
            predictDays = predictDays
        )
    }

    // 배란일/가임기 자동 계산
    if (combineOvulationDays.isEmpty()) {
        val childbearingAgeStartEnd = predictCalculator.childbearingAgeStartEnd(period)
        val childbearingAges = checkPregnancy(
            predictCalculator.predict(
                isPredict = false,
                lastTheDayStart = predictDate,
                fromDate = from,
                toDate = to,
                period = period,
                start = childbearingAgeStartEnd.first,
                end = childbearingAgeStartEnd.second,
                delayTheDays = delayTheDays
            ),
            input.pregnancy
        )

        val ovulationDayStartEnd = predictCalculator.ovulationStartEnd(period)
        val ovulationDays = checkPregnancy(
            predictCalculator.predict(
                isPredict = false,
                lastTheDayStart = predictDate,
                fromDate = from,
                toDate = to,
                period = period,
                start = ovulationDayStartEnd.first,
                end = ovulationDayStartEnd.second,
                delayTheDays = delayTheDays
            ),
            input.pregnancy
        )

        result = result.copy(
            childbearingAges = result.childbearingAges + childbearingAges,
            ovulationDays = result.ovulationDays + ovulationDays
        )
    }

    result = result.copy(delayTheDays = delayTheDays)

    return result
}

/**
 * 생리 주기가 없는 상태에서 배란일 직접 입력이나 테스트만 있는 경우
 */
internal fun PredictCalculatorUseCase.setupOvulationImpl(
    from: LocalDate,
    to: LocalDate,
    input: PeriodCycleInput
): PeriodCycle? {
    val combineOvulationDays = mutableListOf<LocalDate>()

    // 배란 테스트 양성 결과
    val positiveTests = input.ovulationTests.filter { test ->
        test.result == OvulationTest.TestResult.POSITIVE &&
        test.date >= from && test.date <= to
    }
    combineOvulationDays.addAll(positiveTests.map { it.date })

    // 배란일 직접 입력
    val userDays = input.userOvulationDays.filter { ovDay ->
        ovDay.date >= from && ovDay.date <= to
    }
    combineOvulationDays.addAll(userDays.map { it.date })

    if (combineOvulationDays.isEmpty()) {
        return null
    }

    combineOvulationDays.sort()
    val ovulationDays = prepareOvulationDay(combineOvulationDays)

    val childbearingAges = ovulationDays.map { ovDay ->
        val start = ovDay.startDate.minus(2, DateTimeUnit.DAY)
        val end = ovDay.endDate.plus(1, DateTimeUnit.DAY)
        DateRange(start, end)
    }

    return PeriodCycle(
        childbearingAges = childbearingAges,
        ovulationDays = ovulationDays,
        isOvulationPeriodUserInput = true
    )
}

/**
 * 피임약 복용 확인 (생리 사이)
 */
private fun checkThePill(
    startDate: LocalDate,
    nextDate: LocalDate,
    pillPackages: List<PillPackage>
): Boolean {
    val pills = pillPackages.filter { pill ->
        pill.packageStart >= startDate && pill.packageStart < nextDate
    }

    if (pills.isNotEmpty()) {
        val firstPill = pills.first()
        val gap = daysBetween(firstPill.packageStart, nextDate)
        if (gap >= 5) {
            return true
        }
    }
    return false
}

/**
 * 피임약 기준 예정일 확인
 */
private fun checkThePillForPredict(
    startDate: LocalDate,
    pillPackages: List<PillPackage>,
    pillSettings: PillSettings
): LocalDate? {
    val pills = pillPackages.filter { it.packageStart >= startDate }
    if (pills.isEmpty()) return null

    val period = pillSettings.pillCount + pillSettings.restPill
    var predictDate = startDate.plus(period, DateTimeUnit.DAY)

    val gap = daysBetween(pills.first().packageStart, predictDate)
    if (gap >= 5) {
        predictDate = pills.last().packageStart.plus(pillSettings.pillCount + 2, DateTimeUnit.DAY)
        return predictDate
    }
    return null
}

/**
 * 연속된 배란일들을 범위로 묶기
 */
private fun prepareOvulationDay(theOvulationDays: List<LocalDate>): List<DateRange> {
    val results = mutableListOf<DateRange>()

    for (theOvulationDay in theOvulationDays) {
        if (results.isNotEmpty()) {
            val last = results.last()
            val nextDate = last.endDate.plus(1, DateTimeUnit.DAY)
            if (nextDate == theOvulationDay) {
                results[results.lastIndex] = DateRange(last.startDate, theOvulationDay)
                continue
            }
        }
        results.add(DateRange(theOvulationDay, theOvulationDay))
    }

    return results
}

/**
 * 임신 기간과 겹치는 예측 제거
 */
private fun checkPregnancy(
    periods: List<DateRange>,
    pregnancy: PregnancyInfo?
): List<DateRange> {
    if (pregnancy == null || !pregnancy.isActive()) return periods

    val results = mutableListOf<DateRange>()
    val dueDate = pregnancy.dueDate

    for (period in periods) {
        val isOverlapping = if (dueDate != null) {
            // 출산 예정일이 있으면 임신 기간과 겹치는지 확인
            pregnancy.startsDate <= period.endDate && dueDate >= period.startDate
        } else {
            // 출산 예정일이 없으면 임신 시작일 이후는 모두 제외
            pregnancy.startsDate <= period.endDate
        }

        if (isOverlapping) {
            // 임신 시작 전까지만 포함
            val previousDate = pregnancy.startsDate.minus(1, DateTimeUnit.DAY)
            if (period.startDate < previousDate) {
                results.add(DateRange(period.startDate, previousDate))
            }
        } else {
            results.add(period)
        }
    }

    return results
}

private fun daysBetween(from: LocalDate, to: LocalDate): Int {
    return (to.toEpochDays() - from.toEpochDays()).toInt()
}
