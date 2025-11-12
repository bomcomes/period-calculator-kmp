package com.bomcomes.calculator.utils

import com.bomcomes.calculator.dto.DateInput
import kotlinx.datetime.LocalDate
import kotlin.js.JsExport
import kotlin.math.floor

@JsExport
object DateUtils {
    /**
     * ISO-8601 문자열을 LocalDate로 변환
     * 지원 형식:
     * - "2020-03-26"
     * - "2020-03-26T00:00:00.000Z"
     */
    fun parseIso8601(dateString: String): LocalDate {
        // ISO-8601 문자열에서 날짜 부분만 추출 (YYYY-MM-DD)
        val datePart = if (dateString.contains('T')) {
            dateString.substringBefore('T')
        } else {
            dateString
        }

        // LocalDate.parse() 사용 (kotlinx-datetime 표준 방식)
        return LocalDate.parse(datePart)
    }

    /**
     * Julian day를 LocalDate로 변환
     * Julian day는 기원전 4713년 1월 1일부터의 일수
     */
    fun fromJulianDay(julianDay: Double): LocalDate {
        val jd = julianDay + 0.5
        val z = floor(jd).toInt()
        val f = jd - floor(jd)

        val a = if (z < 2299161) {
            z
        } else {
            val alpha = floor((z - 1867216.25) / 36524.25).toInt()
            z + 1 + alpha - floor(alpha / 4.0).toInt()
        }

        val b = a + 1524
        val c = floor((b - 122.1) / 365.25).toInt()
        val d = floor(365.25 * c).toInt()
        val e = floor((b - d) / 30.6001).toInt()

        val day = b - d - floor(30.6001 * e).toInt()
        val month = if (e < 14) e - 1 else e - 13
        val year = if (month > 2) c - 4716 else c - 4715

        // YYYY-MM-DD 형식 문자열로 변환 후 parse
        val yearStr = year.toString().padStart(4, '0')
        val monthStr = month.toString().padStart(2, '0')
        val dayStr = day.toString().padStart(2, '0')
        val dateString = "$yearStr-$monthStr-$dayStr"
        return LocalDate.parse(dateString)
    }

    /**
     * LocalDate를 Julian day로 변환
     */
    fun toJulianDay(date: LocalDate): Double {
        val year = date.year
        val month = date.monthNumber
        val day = date.dayOfMonth

        val a = (14 - month) / 12
        val y = year + 4800 - a
        val m = month + 12 * a - 3

        val jdn = day + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045

        return jdn.toDouble()
    }

    /**
     * DateInput을 LocalDate로 변환
     * 우선순위: localDate > iso8601 > julianDay
     */
    fun toLocalDate(dateInput: DateInput): LocalDate {
        return when {
            dateInput.localDate != null -> dateInput.localDate
            dateInput.iso8601 != null -> parseIso8601(dateInput.iso8601)
            dateInput.julianDay != null -> fromJulianDay(dateInput.julianDay)
            else -> throw IllegalArgumentException("DateInput must have at least one non-null field")
        }
    }
}
