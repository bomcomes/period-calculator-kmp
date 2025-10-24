package com.bomcomes.calculator.core

import com.bomcomes.calculator.models.*
import kotlinx.datetime.*

/**
 * 생리 주기 예측 계산 유스케이스 (iOS PredictCalculatorUseCase 이식)
 */
class PredictCalculatorUseCase(
    private val input: PeriodCycleInput
) {
    private val predictCalculator = PredictCalculator()

    /**
     * 달력 하단 상태바에 표시할 값 구하기
     *
     * @param date 조회할 날짜
     * @return 달력 상태 정보
     */
    fun status(date: LocalDate): CalendarStatus {
        val from = date.minus(1, DateTimeUnit.DAY)
        val to = date.plus(2, DateTimeUnit.DAY)
        val today = Clock.System.now().toLocalDateTime(TimeZone.UTC).date

        // 임신 중인지 확인
        input.pregnancy?.let { pregnancy ->
            // 임신 진행 중이고 삭제/유산되지 않은 경우
            if (pregnancy.isActive()) {
                val dueDate = pregnancy.dueDate

                // 출산 예정일이 있고 그 범위 내인 경우
                if (dueDate != null && date in pregnancy.startsDate..dueDate) {
                    val gap = daysBetween(pregnancy.startsDate, date)
                    return CalendarStatus(
                        calendarType = CalendarStatus.CalendarType.NONE,
                        gap = gap,
                        probability = CalendarStatus.ProbabilityOfPregnancy.PREGNANCY,
                        period = gap
                    )
                }
                // 출산 예정일이 없거나 과거 날짜를 확인하는 경우
                else if (date >= pregnancy.startsDate && pregnancy.startsDate <= today) {
                    val gap = daysBetween(pregnancy.startsDate, date)
                    return CalendarStatus(
                        calendarType = CalendarStatus.CalendarType.NONE,
                        gap = gap,
                        probability = CalendarStatus.ProbabilityOfPregnancy.PREGNANCY,
                        period = gap
                    )
                }
            }
        }

        val menstrualCycles = menstrualCycles(from, to).filter { cycle ->
            cycle.theDay?.startDate?.let { it <= date } ?: false
        }

        val menstrualCycle = menstrualCycles.lastOrNull() ?: run {
            // 생리 기록이 없는 경우
            input.pregnancy?.let { pregnancy ->
                val dueDate = pregnancy.dueDate

                if (pregnancy.startsDate <= date) {
                    val gap = daysBetween(pregnancy.startsDate, date)
                    return CalendarStatus(
                        calendarType = CalendarStatus.CalendarType.NONE,
                        gap = gap,
                        probability = CalendarStatus.ProbabilityOfPregnancy.RECOVERY_AFTER_CHILDBIRTH,
                        period = gap
                    )
                } else if (dueDate != null && dueDate >= date) {
                    val gap = daysBetween(pregnancy.startsDate, date)
                    return CalendarStatus(
                        calendarType = CalendarStatus.CalendarType.NONE,
                        gap = gap,
                        probability = CalendarStatus.ProbabilityOfPregnancy.NO_THE_DAY,
                        period = gap
                    )
                }
            }
            return CalendarStatus(
                calendarType = CalendarStatus.CalendarType.NONE,
                gap = 0,
                probability = CalendarStatus.ProbabilityOfPregnancy.INPUT_THE_DAY,
                period = 0
            )
        }

        var status = CalendarStatus.CalendarType.NONE
        var gap = 0
        var probability = CalendarStatus.ProbabilityOfPregnancy.LOW

        // 생리 중인지 확인
        menstrualCycle.theDay?.let { theDay ->
            gap = daysBetween(theDay.startDate, date)
            if (date in theDay.startDate..theDay.endDate) {
                return CalendarStatus(
                    calendarType = CalendarStatus.CalendarType.THE_DAY,
                    gap = gap,
                    probability = CalendarStatus.ProbabilityOfPregnancy.LOW,
                    period = menstrualCycle.period
                )
            }
        }

        // 피임약 또는 배란일 기준 주기 조정
        var periodGap = 0
        var period = menstrualCycle.period
        menstrualCycle.thePillPeriod?.let { thePillPeriod ->
            if (gap < menstrualCycle.delayTheDays + thePillPeriod) {
                period = thePillPeriod
            } else {
                periodGap = menstrualCycle.period - thePillPeriod
            }
        } ?: menstrualCycle.ovulationDayPeriod?.let { ovulationDayPeriod ->
            if (gap < menstrualCycle.delayTheDays + ovulationDayPeriod) {
                period = ovulationDayPeriod
            } else {
                periodGap = menstrualCycle.period - ovulationDayPeriod
            }
        }

        gap = (gap + periodGap) % (if (period > 0) period else 1)

        // 지연 고려
        menstrualCycle.delayDay?.let { delayDay ->
            if (delayDay.endDate < date) {
                gap -= menstrualCycle.delayTheDays
            }
        } ?: run {
            gap -= menstrualCycle.delayTheDays
        }

        // 지연 기간인지 확인
        menstrualCycle.delayDay?.let { delayDay ->
            if (date in delayDay.startDate..delayDay.endDate) {
                gap = daysBetween(delayDay.startDate, date)
                probability = if (gap < 7) {
                    CalendarStatus.ProbabilityOfPregnancy.NORMAL
                } else {
                    CalendarStatus.ProbabilityOfPregnancy.HOSPITAL_OVER_DELAY_8
                }
                return CalendarStatus(
                    calendarType = CalendarStatus.CalendarType.DELAY,
                    gap = gap,
                    probability = probability,
                    period = menstrualCycle.period
                )
            }
        }

        // 8일 이상 지연 시 병원 권장
        if (menstrualCycle.delayTheDays >= 8) {
            val checkPeriod = menstrualCycle.thePillPeriod
                ?: menstrualCycle.ovulationDayPeriod
                ?: menstrualCycle.period

            menstrualCycle.theDay?.startDate?.let { startDate ->
                val days = daysBetween(startDate, date)
                if (days + 1 - checkPeriod >= 8) {
                    return CalendarStatus(
                        calendarType = CalendarStatus.CalendarType.NONE,
                        gap = 0,
                        probability = CalendarStatus.ProbabilityOfPregnancy.HOSPITAL_OVER_DELAY_8,
                        period = menstrualCycle.period
                    )
                }
            }
        }

        // 생리 예정일인지 확인
        if (menstrualCycle.predictDays.isNotEmpty()) {
            if (menstrualCycle.predictDays.any { date in it.startDate..it.endDate }) {
                status = CalendarStatus.CalendarType.PREDICT
                probability = CalendarStatus.ProbabilityOfPregnancy.LOW
                gap %= if (period > 0) period else 1
                return CalendarStatus(
                    calendarType = status,
                    gap = gap,
                    probability = probability,
                    period = menstrualCycle.period
                )
            }
        }

        // 배란일인지 확인
        if (menstrualCycle.ovulationDays.isNotEmpty()) {
            if (menstrualCycle.ovulationDays.any { date in it.startDate..it.endDate }) {
                return CalendarStatus(
                    calendarType = CalendarStatus.CalendarType.OVULATION_DAY,
                    gap = gap,
                    probability = CalendarStatus.ProbabilityOfPregnancy.HIGH,
                    period = menstrualCycle.period
                )
            }
        }

        // 가임기인지 확인
        if (menstrualCycle.childbearingAges.isNotEmpty()) {
            if (menstrualCycle.childbearingAges.any { date in it.startDate..it.endDate }) {
                probability = if (menstrualCycle.ovulationDays.isNotEmpty()) {
                    CalendarStatus.ProbabilityOfPregnancy.HIGH
                } else {
                    CalendarStatus.ProbabilityOfPregnancy.MIDDLE
                }
                return CalendarStatus(
                    calendarType = CalendarStatus.CalendarType.CHILDBEARING_AGE,
                    gap = gap,
                    probability = probability,
                    period = menstrualCycle.period
                )
            }
        }

        return CalendarStatus(
            calendarType = status,
            gap = gap,
            probability = probability,
            period = menstrualCycle.period
        )
    }

    /**
     * 생리 주기 정보 계산
     *
     * @param from 시작 날짜
     * @param to 종료 날짜
     * @return 생리 주기 정보 리스트
     */
    fun menstrualCycles(from: LocalDate, to: LocalDate): List<PeriodCycle> {
        val results = mutableListOf<PeriodCycle>()

        // 임신 정보로 기간 필터링
        val filteredPeriods = filterPeriodsByPregnancy(from, to)

        if (filteredPeriods.isEmpty()) {
            // 배란일 직접 입력이나 테스트가 있으면 처리
            setupOvulation(from, to)?.let { results.add(it) }
            return results
        }

        // 기간에 포함되는 생리 기록들 찾기
        var theDays = filteredPeriods.filter { period ->
            period.startDate <= to && period.endDate >= from
        }.toMutableList()

        // 앞뒤로 한 개씩 더 가져오기
        if (theDays.isNotEmpty()) {
            val firstTheDay = theDays.first()
            val lastTheDay = theDays.last()

            if (from < firstTheDay.startDate) {
                filteredPeriods
                    .filter { it.startDate <= from }
                    .maxByOrNull { it.startDate }
                    ?.let { theDays.add(0, it) }
            }

            if (to > lastTheDay.endDate) {
                filteredPeriods
                    .filter { it.startDate >= to }
                    .minByOrNull { it.startDate }
                    ?.let { theDays.add(it) }
            }
        } else {
            // 앞뒤로 하나씩 찾기
            filteredPeriods
                .filter { it.startDate <= from }
                .maxByOrNull { it.startDate }
                ?.let { theDays.add(0, it) }

            filteredPeriods
                .filter { it.startDate >= to }
                .minByOrNull { it.startDate }
                ?.let { theDays.add(it) }
        }

        if (theDays.size >= 2) {
            // 연속된 생리 기록들 사이의 주기 계산
            for (index in 0 until theDays.size - 1) {
                val currentTheDay = theDays[index]
                val nextTheDay = theDays[index + 1]
                val period = daysBetween(currentTheDay.startDate, nextTheDay.startDate)

                setupResult(currentTheDay, nextTheDay, from, to, period)?.let {
                    results.add(it)
                }
            }

            // 마지막 생리 이후 예측
            if (theDays.size == 2) {
                theDays.last().let { lastTheDay ->
                    results.add(PeriodCycle(
                        pk = lastTheDay.pk,
                        theDay = DateRange(lastTheDay.startDate, lastTheDay.endDate)
                    ))
                }
            }
        } else if (theDays.size == 1) {
            // 평균 주기로 예정일 구하기
            val currentTheDay = theDays[0]
            val period = input.periodSettings.getAverageCycle()
            val days = input.periodSettings.days

            setupPredict(currentTheDay, from, to, period, days)?.let {
                results.add(it)
            } ?: run {
                val twoDays = to.plus(2, DateTimeUnit.DAY)
                setupOvulation(from, twoDays)?.let { results.add(it) }
            }
        } else {
            val twoDays = to.plus(2, DateTimeUnit.DAY)
            setupOvulation(from, twoDays)?.let { results.add(it) }
        }

        return results
    }

    private fun daysBetween(from: LocalDate, to: LocalDate): Int {
        return (to.toEpochDays() - from.toEpochDays()).toInt()
    }

    private fun filterPeriodsByPregnancy(from: LocalDate, to: LocalDate): List<PeriodRecord> {
        input.pregnancy?.let { pregnancy ->
            return input.periods.filter { period ->
                // 임신 기간과 겹치지 않는 생리 기록만
                if (pregnancy.dueDate != null) {
                    period.startDate < pregnancy.startsDate || period.startDate > pregnancy.dueDate
                } else {
                    period.startDate < pregnancy.startsDate
                }
            }
        }
        return input.periods
    }

    // Helper methods will be added in the next file
    internal fun setupResult(
        theDay: PeriodRecord,
        nextTheDay: PeriodRecord,
        from: LocalDate,
        to: LocalDate,
        period: Int
    ): PeriodCycle? {
        return setupResultImpl(theDay, nextTheDay, from, to, period, input, predictCalculator)
    }

    internal fun setupPredict(
        theDay: PeriodRecord,
        from: LocalDate,
        to: LocalDate,
        period: Int,
        days: Int
    ): PeriodCycle? {
        return setupPredictImpl(theDay, from, to, period, days, input, predictCalculator)
    }

    internal fun setupOvulation(from: LocalDate, to: LocalDate): PeriodCycle? {
        return setupOvulationImpl(from, to, input)
    }
}
