package com.bomcomes.calculator.utils

import kotlinx.datetime.LocalDate
import kotlin.math.floor

actual object DateUtils {
    /**
     * Julian day를 LocalDate로 변환
     * Julian day는 기원전 4713년 1월 1일부터의 일수
     */
    actual fun fromJulianDay(julianDay: Double): LocalDate {
        val jd = julianDay + 0.5
        val z = floor(jd).toInt()

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
    actual fun toJulianDay(date: LocalDate): Double {
        val year = date.year
        val month = date.monthNumber
        val day = date.dayOfMonth

        val a = (14 - month) / 12
        val y = year + 4800 - a
        val m = month + 12 * a - 3

        val jdn = day + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045

        return jdn.toDouble()
    }
}
