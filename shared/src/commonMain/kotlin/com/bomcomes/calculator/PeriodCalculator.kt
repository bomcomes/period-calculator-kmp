package com.bomcomes.calculator

import kotlinx.datetime.*

/**
 * 생리 주기 계산기
 */
object PeriodCalculator {
    
    /**
     * 다음 생리 예정일 계산
     * 
     * @param lastPeriodStartDate 마지막 생리 시작일
     * @param averageCycleLength 평균 주기 길이 (일)
     * @return 다음 생리 예정일
     */
    fun calculateNextPeriod(
        lastPeriodStartDate: LocalDate,
        averageCycleLength: Int
    ): LocalDate {
        return lastPeriodStartDate.plus(averageCycleLength, DateTimeUnit.DAY)
    }
    
    /**
     * 다음 생리 예정일 계산 (PeriodSettings 사용)
     * 
     * @param lastPeriodStartDate 마지막 생리 시작일
     * @param settings 생리 주기 설정 (isAutoCalc에 따라 자동/수동 평균 주기 선택)
     * @return 다음 생리 예정일
     */
    fun calculateNextPeriod(
        lastPeriodStartDate: LocalDate,
        settings: PeriodSettings
    ): LocalDate {
        val averageCycle = settings.getAverageCycle()
        return calculateNextPeriod(lastPeriodStartDate, averageCycle)
    }
    
    /**
     * 배란일 계산 (생리 예정일 14일 전)
     * 
     * @param nextPeriodDate 다음 생리 예정일
     * @return 배란 예정일
     */
    fun calculateOvulationDate(
        nextPeriodDate: LocalDate
    ): LocalDate {
        return nextPeriodDate.minus(14, DateTimeUnit.DAY)
    }
    
    /**
     * 가임기 계산 (배란일 -5일 ~ 배란일)
     * 
     * @param ovulationDate 배란 예정일
     * @return Pair<가임기 시작일, 가임기 종료일>
     */
    fun calculateFertileWindow(
        ovulationDate: LocalDate
    ): Pair<LocalDate, LocalDate> {
        val start = ovulationDate.minus(5, DateTimeUnit.DAY)
        val end = ovulationDate
        return Pair(start, end)
    }
    
    /**
     * 배란 테스트 결과를 기반으로 배란일 추정
     * 
     * @param testResults Map<테스트 날짜, 결과(0=음성, 1=양성, 2=불명확)>
     * @return 추정 배란일 (양성 결과가 있는 경우)
     */
    fun estimateOvulationFromTests(
        testResults: Map<LocalDate, Int>
    ): LocalDate? {
        // 양성(1) 결과가 있는 날짜 찾기
        val positiveResults = testResults
            .filter { it.value == 1 }
            .keys
            .sortedDescending()
        
        // 가장 최근 양성 결과 반환
        return positiveResults.firstOrNull()
    }
    
    /**
     * 피임약 복용 상태를 고려한 배란일 계산
     * 
     * @param isPillActive 피임약 복용 중인지 여부
     * @param naturalOvulationDate 자연 배란일 (피임약 미복용 시)
     * @return 배란 예정일 (피임약 복용 중이면 null)
     */
    fun calculateOvulationWithPill(
        isPillActive: Boolean,
        naturalOvulationDate: LocalDate
    ): LocalDate? {
        // 피임약 복용 중에는 배란 억제
        return if (isPillActive) null else naturalOvulationDate
    }
    
    /**
     * 여러 데이터를 종합하여 최적의 배란일 추정
     * 
     * @param nextPeriodDate 다음 생리 예정일
     * @param ovulationTestResults 배란 테스트 결과
     * @param userInputOvulationDate 사용자 직접 입력 배란일
     * @param isPillActive 피임약 복용 중인지 여부
     * @return 추정된 배란일
     */
    fun estimateBestOvulationDate(
        nextPeriodDate: LocalDate,
        ovulationTestResults: Map<LocalDate, Int>? = null,
        userInputOvulationDate: LocalDate? = null,
        isPillActive: Boolean = false
    ): LocalDate? {
        // 피임약 복용 중이면 배란 없음
        if (isPillActive) return null
        
        // 우선순위: 사용자 직접 입력 > 배란 테스트 > 계산
        return userInputOvulationDate
            ?: ovulationTestResults?.let { estimateOvulationFromTests(it) }
            ?: calculateOvulationDate(nextPeriodDate)
    }
}
