package com.bomcomes.calculator.helpers

import com.bomcomes.calculator.models.*
import kotlinx.datetime.*

/**
 * Ovulation Calculator
 *
 * 배란일 관련 계산 함수들
 */
internal object OvulationCalculator {

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
            test.result == TestResult.POSITIVE &&
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
}
