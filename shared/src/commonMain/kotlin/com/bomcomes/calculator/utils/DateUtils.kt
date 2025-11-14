package com.bomcomes.calculator.utils

import kotlinx.datetime.LocalDate

/**
 * Date utility functions for converting between LocalDate and Julian Day
 * Platform-specific implementations provided via expect/actual
 */
expect object DateUtils {
    /**
     * Julian day를 LocalDate로 변환
     * Julian day는 기원전 4713년 1월 1일부터의 일수
     */
    fun fromJulianDay(julianDay: Double): LocalDate

    /**
     * LocalDate를 Julian day로 변환
     */
    fun toJulianDay(date: LocalDate): Double
}
