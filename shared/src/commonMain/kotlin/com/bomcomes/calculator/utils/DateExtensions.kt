package com.bomcomes.calculator.utils

import kotlinx.datetime.LocalDate
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.until

/**
 * 두 날짜 사이의 일수를 계산하는 확장 함수
 */
fun LocalDate.daysUntil(other: LocalDate): Int {
    return this.until(other, DateTimeUnit.DAY)
}
