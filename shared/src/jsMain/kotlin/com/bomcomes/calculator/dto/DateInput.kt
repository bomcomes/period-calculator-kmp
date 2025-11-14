package com.bomcomes.calculator.dto

import com.bomcomes.calculator.utils.DateUtils
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
) {
    /**
     * DateInput을 julianDay로 변환
     */
    fun toJulianDay(): Double {
        return when {
            julianDay != null -> julianDay
            localDate != null -> DateUtils.toJulianDay(localDate)
            iso8601 != null -> {
                // ISO-8601 형식에서 날짜 부분만 추출 (YYYY-MM-DD)
                val datePart = iso8601.substringBefore('T')
                val date = LocalDate.parse(datePart)
                DateUtils.toJulianDay(date)
            }
            else -> throw IllegalArgumentException("DateInput must have at least one non-null field")
        }
    }
}
