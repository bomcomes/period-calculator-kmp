package com.period.calculator

import com.period.calculator.model.PeriodRecord
import com.period.calculator.model.PeriodSettings
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class TestCase01Verification {

    @Test
    fun `TC-01 규칙적인 주기 정확한 결과 확인`() {
        // 입력 조건
        val records = listOf(
            PeriodRecord(
                startDate = LocalDate(2025, 1, 1),
                endDate = LocalDate(2025, 1, 5)
            ),
            PeriodRecord(
                startDate = LocalDate(2025, 1, 29),
                endDate = LocalDate(2025, 2, 2)
            ),
            PeriodRecord(
                startDate = LocalDate(2025, 2, 26),
                endDate = LocalDate(2025, 3, 2)
            )
        )

        val settings = PeriodSettings(
            cycleLength = 28,
            periodLength = 5,
            ovulationDaysBefore = 14,
            fertileWindowDays = 5,
            lutealPhaseDays = 14
        )

        val calculator = PeriodCalculator()
        val currentDate = LocalDate(2025, 3, 15)

        // 계산
        val result = calculator.calculate(records, currentDate, settings)

        // 결과 출력
        println("=== TC-01 실제 계산 결과 ===")
        println("다음 생리 예정일: ${result.nextPeriodDate}")
        println("배란 예정일: ${result.nextOvulationDate}")
        println("가임기: ${result.fertilePeriod?.start} ~ ${result.fertilePeriod?.endInclusive}")
        println("현재 상태: ${result.currentPhase}")
        println("D-Day: ${result.daysUntilNextPeriod?.let { "D-$it" } ?: "N/A"}")

        // 실제 주기 계산 확인
        println("\n=== 주기 분석 ===")
        if (records.size >= 2) {
            for (i in 1 until records.size) {
                val cycle = calculator.calculateCycleDays(records[i - 1].startDate, records[i].startDate)
                println("주기 ${i}: ${cycle}일 (${records[i - 1].startDate} ~ ${records[i].startDate})")
            }
        }

        // 평균 주기
        val avgCycle = calculator.calculateAverageCycle(records)
        println("평균 주기: ${avgCycle}일")

        // 마지막 생리 시작일로부터 경과일
        val lastPeriodStart = records.last().startDate
        val daysSinceLastPeriod = calculator.calculateDaysBetween(lastPeriodStart, currentDate)
        println("\n마지막 생리(${lastPeriodStart})로부터 경과일: ${daysSinceLastPeriod}일")

        // 예상 결과와 비교
        println("\n=== 문서의 예상 결과와 비교 ===")
        println("문서 예상 - 다음 생리: 2025-03-26")
        println("실제 계산 - 다음 생리: ${result.nextPeriodDate}")
        println("일치 여부: ${result.nextPeriodDate == LocalDate(2025, 3, 26)}")
    }
}
