package com.bomcomes.calculator

/**
 * 생리 주기 설정
 * Firebase의 theDaySettings/ONE_DOCUMENT 구조
 */
data class PeriodSettings(
    val isAutoCalc: Boolean,           // true: 자동 계산 사용, false: 수동 입력 사용
    val autoAverageCycle: Int,         // 자동 계산된 평균 주기 (일)
    val autoAverageDay: Int,           // 자동 계산된 평균 생리 기간 (일)
    val manualAverageCycle: Int,       // 수동 입력한 평균 주기 (일)
    val manualAverageDay: Int          // 수동 입력한 평균 생리 기간 (일)
) {
    /**
     * 실제 사용할 평균 주기 반환
     * isAutoCalc에 따라 자동/수동 값 선택
     */
    fun getAverageCycle(): Int {
        return if (isAutoCalc) autoAverageCycle else manualAverageCycle
    }
    
    /**
     * 실제 사용할 평균 생리 기간 반환
     * isAutoCalc에 따라 자동/수동 값 선택
     */
    fun getAverageDay(): Int {
        return if (isAutoCalc) autoAverageDay else manualAverageDay
    }
}
