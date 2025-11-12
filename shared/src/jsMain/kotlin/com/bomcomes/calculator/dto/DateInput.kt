package com.bomcomes.calculator.dto

import kotlinx.datetime.LocalDate
import kotlin.js.JsExport

/**
 * JavaScript에서 날짜를 여러 형식으로 전달할 수 있도록 하는 클래스
 * - ISO-8601 문자열: "2020-03-26T00:00:00.000Z"
 * - Julian day: 정수 값
 * - LocalDate: kotlinx-datetime LocalDate 객체
 */
@JsExport
data class DateInput(
    val iso8601: String? = null,
    val julianDay: Double? = null,  // JavaScript Number
    val localDate: LocalDate? = null
)
