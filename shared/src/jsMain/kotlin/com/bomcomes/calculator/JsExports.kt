@file:JsExport

package com.bomcomes.calculator

import kotlinx.datetime.LocalDate

/**
 * JavaScript/TypeScript에서 사용하기 위한 Export 함수들
 * 
 * 날짜는 ISO 8601 형식 문자열 (예: "2024-01-01")로 주고받습니다.
 */

/**
 * 다음 생리 예정일 계산
 */
@JsName("calculateNextPeriod")
fun calculateNextPeriodJs(
    lastPeriodStartDate: String,
    averageCycleLength: Int
): String {
    val date = LocalDate.parse(lastPeriodStartDate)
    val result = PeriodCalculator.calculateNextPeriod(date, averageCycleLength)
    return result.toString()
}

/**
 * 배란일 계산
 */
@JsName("calculateOvulationDate")
fun calculateOvulationDateJs(
    nextPeriodDate: String
): String {
    val date = LocalDate.parse(nextPeriodDate)
    val result = PeriodCalculator.calculateOvulationDate(date)
    return result.toString()
}

/**
 * 가임기 계산
 */
@JsName("calculateFertileWindow")
fun calculateFertileWindowJs(
    ovulationDate: String
): FertileWindowResult {
    val date = LocalDate.parse(ovulationDate)
    val (start, end) = PeriodCalculator.calculateFertileWindow(date)
    return FertileWindowResult(
        start = start.toString(),
        end = end.toString()
    )
}

/**
 * 배란 테스트 결과를 기반으로 배란일 추정
 */
@JsName("estimateOvulationFromTests")
fun estimateOvulationFromTestsJs(
    testResults: dynamic
): String? {
    val testMap = mutableMapOf<LocalDate, Int>()
    val keys = js("Object.keys(testResults)") as Array<String>
    
    keys.forEach { dateStr ->
        val date = LocalDate.parse(dateStr)
        val result = (testResults[dateStr] as Number).toInt()
        testMap[date] = result
    }
    
    return PeriodCalculator.estimateOvulationFromTests(testMap)?.toString()
}

/**
 * 피임약 복용 상태를 고려한 배란일 계산
 */
@JsName("calculateOvulationWithPill")
fun calculateOvulationWithPillJs(
    isPillActive: Boolean,
    naturalOvulationDate: String
): String? {
    val date = LocalDate.parse(naturalOvulationDate)
    return PeriodCalculator.calculateOvulationWithPill(isPillActive, date)?.toString()
}

/**
 * 여러 데이터를 종합하여 최적의 배란일 추정
 */
@JsName("estimateBestOvulationDate")
fun estimateBestOvulationDateJs(
    nextPeriodDate: String,
    ovulationTestResults: dynamic = null,
    userInputOvulationDate: String? = null,
    isPillActive: Boolean = false
): String? {
    val nextDate = LocalDate.parse(nextPeriodDate)
    
    val testMap = if (ovulationTestResults != null && ovulationTestResults != undefined) {
        val map = mutableMapOf<LocalDate, Int>()
        val keys = js("Object.keys(ovulationTestResults)") as Array<String>
        keys.forEach { dateStr ->
            val date = LocalDate.parse(dateStr)
            val result = (ovulationTestResults[dateStr] as Number).toInt()
            map[date] = result
        }
        map
    } else null
    
    val userDate = userInputOvulationDate?.let { LocalDate.parse(it) }
    
    return PeriodCalculator.estimateBestOvulationDate(
        nextPeriodDate = nextDate,
        ovulationTestResults = testMap,
        userInputOvulationDate = userDate,
        isPillActive = isPillActive
    )?.toString()
}

/**
 * 가임기 결과 객체
 */
data class FertileWindowResult(
    val start: String,
    val end: String
)
