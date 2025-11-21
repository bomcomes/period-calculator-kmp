package com.bomcomes.calculator.unit

import com.bomcomes.calculator.utils.DateUtils
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.math.abs

class DateUtilsTest {

    // ==================== Basic Conversion Tests ====================

    @Test
    fun testToJulianDay_epochDate() {
        // 2000년 1월 1일 (J2000.0 epoch) = JD 2451545
        val date = LocalDate(2000, 1, 1)
        val julianDay = DateUtils.toJulianDay(date)
        assertEquals(2451545.0, julianDay, 0.5, "2000-01-01은 JD 2451545")
    }

    @Test
    fun testFromJulianDay_epochDate() {
        // JD 2451545 = 2000년 1월 1일
        val julianDay = 2451545.0
        val date = DateUtils.fromJulianDay(julianDay)
        assertEquals(LocalDate(2000, 1, 1), date, "JD 2451545는 2000-01-01")
    }

    @Test
    fun testRoundTripConversion_variousDates() {
        // 다양한 날짜에서 왕복 변환 테스트
        val testDates = listOf(
            LocalDate(1900, 1, 1),
            LocalDate(1970, 1, 1),  // Unix epoch
            LocalDate(2000, 1, 1),  // J2000 epoch
            LocalDate(2024, 1, 1),
            LocalDate(2024, 12, 31),
            LocalDate(2025, 6, 15),
            LocalDate(2100, 12, 31)
        )

        for (originalDate in testDates) {
            val julianDay = DateUtils.toJulianDay(originalDate)
            val convertedDate = DateUtils.fromJulianDay(julianDay)
            assertEquals(originalDate, convertedDate,
                "왕복 변환: $originalDate -> JD $julianDay -> $convertedDate")
        }
    }

    // ==================== Historical Dates Tests ====================

    @Test
    fun testToJulianDay_historicalDates() {
        // 역사적으로 중요한 날짜들의 Julian Day 검증
        data class HistoricalDate(val date: LocalDate, val expectedJD: Double, val description: String)

        val historicalDates = listOf(
            // Gregorian calendar adoption (1582-10-15)
            HistoricalDate(LocalDate(1582, 10, 15), 2299161.0, "그레고리력 시작"),

            // Unix epoch
            HistoricalDate(LocalDate(1970, 1, 1), 2440588.0, "Unix epoch"),

            // 21st century start
            HistoricalDate(LocalDate(2000, 1, 1), 2451545.0, "21세기 시작"),

            // Y2K
            HistoricalDate(LocalDate(1999, 12, 31), 2451544.0, "Y2K 전날")
        )

        for ((date, expectedJD, description) in historicalDates) {
            val actualJD = DateUtils.toJulianDay(date)
            assertEquals(expectedJD, actualJD, 0.5,
                "$description: $date should be JD $expectedJD")
        }
    }

    @Test
    fun testFromJulianDay_historicalDates() {
        // 역사적 Julian Day 값들의 날짜 변환 검증
        data class HistoricalJD(val jd: Double, val expectedDate: LocalDate, val description: String)

        val historicalJDs = listOf(
            HistoricalJD(2299161.0, LocalDate(1582, 10, 15), "그레고리력 시작"),
            HistoricalJD(2440588.0, LocalDate(1970, 1, 1), "Unix epoch"),
            HistoricalJD(2451545.0, LocalDate(2000, 1, 1), "21세기 시작"),
            HistoricalJD(2451544.0, LocalDate(1999, 12, 31), "Y2K 전날")
        )

        for ((jd, expectedDate, description) in historicalJDs) {
            val actualDate = DateUtils.fromJulianDay(jd)
            assertEquals(expectedDate, actualDate,
                "$description: JD $jd should be $expectedDate")
        }
    }

    // ==================== Leap Year Tests ====================

    @Test
    fun testLeapYearDates() {
        // 윤년 처리 테스트
        val leapYearDates = listOf(
            LocalDate(2000, 2, 29),  // 2000년은 400으로 나누어떨어지는 윤년
            LocalDate(2004, 2, 29),  // 일반적인 윤년
            LocalDate(2020, 2, 29),  // 최근 윤년
            LocalDate(2024, 2, 29)   // 현재 윤년
        )

        for (date in leapYearDates) {
            val jd = DateUtils.toJulianDay(date)
            val converted = DateUtils.fromJulianDay(jd)
            assertEquals(date, converted, "윤년 날짜 변환: $date")
        }
    }

    @Test
    fun testNonLeapYearFebruary() {
        // 평년 2월 마지막 날 테스트
        val nonLeapYearDates = listOf(
            LocalDate(1900, 2, 28),  // 1900년은 100으로 나누어떨어지지만 400으로 안 나누어떨어지는 평년
            LocalDate(2001, 2, 28),
            LocalDate(2023, 2, 28)
        )

        for (date in nonLeapYearDates) {
            val jd = DateUtils.toJulianDay(date)
            val converted = DateUtils.fromJulianDay(jd)
            assertEquals(date, converted, "평년 2월 마지막 날: $date")
        }
    }

    // ==================== Month Boundary Tests ====================

    @Test
    fun testMonthBoundaries() {
        // 각 월의 첫날과 마지막날 테스트
        val year = 2024
        val monthBoundaries = listOf(
            Pair(LocalDate(year, 1, 1), LocalDate(year, 1, 31)),    // January
            Pair(LocalDate(year, 2, 1), LocalDate(year, 2, 29)),    // February (leap year)
            Pair(LocalDate(year, 3, 1), LocalDate(year, 3, 31)),    // March
            Pair(LocalDate(year, 4, 1), LocalDate(year, 4, 30)),    // April
            Pair(LocalDate(year, 5, 1), LocalDate(year, 5, 31)),    // May
            Pair(LocalDate(year, 6, 1), LocalDate(year, 6, 30)),    // June
            Pair(LocalDate(year, 7, 1), LocalDate(year, 7, 31)),    // July
            Pair(LocalDate(year, 8, 1), LocalDate(year, 8, 31)),    // August
            Pair(LocalDate(year, 9, 1), LocalDate(year, 9, 30)),    // September
            Pair(LocalDate(year, 10, 1), LocalDate(year, 10, 31)),  // October
            Pair(LocalDate(year, 11, 1), LocalDate(year, 11, 30)),  // November
            Pair(LocalDate(year, 12, 1), LocalDate(year, 12, 31))   // December
        )

        for ((firstDay, lastDay) in monthBoundaries) {
            // 첫날 테스트
            val jdFirst = DateUtils.toJulianDay(firstDay)
            val convertedFirst = DateUtils.fromJulianDay(jdFirst)
            assertEquals(firstDay, convertedFirst,
                "월 첫날: ${firstDay.month} 1일")

            // 마지막날 테스트
            val jdLast = DateUtils.toJulianDay(lastDay)
            val convertedLast = DateUtils.fromJulianDay(jdLast)
            assertEquals(lastDay, convertedLast,
                "월 마지막날: ${lastDay.month} ${lastDay.dayOfMonth}일")
        }
    }

    // ==================== Year Boundary Tests ====================

    @Test
    fun testYearBoundaries() {
        // 년도 경계 테스트 (년말/년초)
        val yearTransitions = listOf(
            Pair(LocalDate(1999, 12, 31), LocalDate(2000, 1, 1)),  // Y2K
            Pair(LocalDate(2023, 12, 31), LocalDate(2024, 1, 1)),  // Recent
            Pair(LocalDate(2024, 12, 31), LocalDate(2025, 1, 1)),  // Current
            Pair(LocalDate(2099, 12, 31), LocalDate(2100, 1, 1))   // Future
        )

        for ((lastDay, firstDay) in yearTransitions) {
            val jdLast = DateUtils.toJulianDay(lastDay)
            val jdFirst = DateUtils.toJulianDay(firstDay)

            // 연속된 날짜는 JD가 1 차이
            assertEquals(1.0, jdFirst - jdLast, 0.01,
                "연속된 날짜의 JD 차이: $lastDay -> $firstDay")

            // 변환 정확성
            assertEquals(lastDay, DateUtils.fromJulianDay(jdLast))
            assertEquals(firstDay, DateUtils.fromJulianDay(jdFirst))
        }
    }

    // ==================== Sequential Days Tests ====================

    @Test
    fun testSequentialDays() {
        // 연속된 날짜들의 Julian Day가 1씩 증가하는지 확인
        val startDate = LocalDate(2024, 1, 1)
        var previousJD = DateUtils.toJulianDay(startDate)

        // 2024년의 각 날짜를 순차적으로 테스트
        for (month in 1..12) {
            val daysInMonth = when (month) {
                1, 3, 5, 7, 8, 10, 12 -> 31
                4, 6, 9, 11 -> 30
                2 -> 29 // 2024는 윤년
                else -> 0
            }

            for (day in 1..daysInMonth) {
                val currentDate = LocalDate(2024, month, day)
                val currentJD = DateUtils.toJulianDay(currentDate)

                if (currentDate != startDate) {
                    // 전날과의 차이가 1인지 확인
                    val diff = currentJD - previousJD
                    assertTrue(abs(diff - 1.0) < 0.001,
                        "연속된 날짜의 JD 차이가 1이어야 함: $currentDate (diff=$diff)")
                }

                previousJD = currentJD
            }
        }
    }

    @Test
    fun testDaysDifference() {
        // 두 날짜 사이의 일수 차이 계산
        data class DatePair(
            val date1: LocalDate,
            val date2: LocalDate,
            val expectedDays: Int
        )

        val datePairs = listOf(
            DatePair(LocalDate(2024, 1, 1), LocalDate(2024, 1, 31), 30),
            DatePair(LocalDate(2024, 1, 1), LocalDate(2024, 2, 1), 31),
            DatePair(LocalDate(2024, 1, 1), LocalDate(2024, 12, 31), 365),  // 2024 is leap year
            DatePair(LocalDate(2023, 1, 1), LocalDate(2023, 12, 31), 364),  // 2023 is not leap year
            DatePair(LocalDate(2024, 2, 28), LocalDate(2024, 3, 1), 2)      // Leap year February
        )

        for ((date1, date2, expectedDays) in datePairs) {
            val jd1 = DateUtils.toJulianDay(date1)
            val jd2 = DateUtils.toJulianDay(date2)
            val actualDays = (jd2 - jd1).toInt()

            assertEquals(expectedDays, actualDays,
                "$date1 와 $date2 사이의 일수는 $expectedDays")
        }
    }

    // ==================== Edge Cases Tests ====================

    @Test
    fun testCenturyYears() {
        // 세기 경계 년도 테스트 (윤년 규칙)
        val centuryYears = listOf(
            LocalDate(1900, 1, 1),  // 100으로 나누어떨어지지만 400으로 안 나누어떨어짐 (평년)
            LocalDate(2000, 1, 1),  // 400으로 나누어떨어짐 (윤년)
            LocalDate(2100, 1, 1)   // 100으로 나누어떨어지지만 400으로 안 나누어떨어짐 (평년)
        )

        for (date in centuryYears) {
            val jd = DateUtils.toJulianDay(date)
            val converted = DateUtils.fromJulianDay(jd)
            assertEquals(date, converted, "세기 경계 년도: $date")
        }
    }

    @Test
    fun testFarFutureDates() {
        // 먼 미래 날짜 테스트
        val futureDates = listOf(
            LocalDate(2100, 1, 1),
            LocalDate(2200, 12, 31),
            LocalDate(2500, 6, 15),
            LocalDate(3000, 1, 1)
        )

        for (date in futureDates) {
            val jd = DateUtils.toJulianDay(date)
            val converted = DateUtils.fromJulianDay(jd)
            assertEquals(date, converted, "미래 날짜 변환: $date")
        }
    }

    @Test
    fun testPastDates() {
        // 과거 날짜 테스트
        val pastDates = listOf(
            LocalDate(1800, 1, 1),
            LocalDate(1700, 12, 31),
            LocalDate(1600, 6, 15)
        )

        for (date in pastDates) {
            val jd = DateUtils.toJulianDay(date)
            val converted = DateUtils.fromJulianDay(jd)
            assertEquals(date, converted, "과거 날짜 변환: $date")
        }
    }

    // ==================== Fractional Julian Day Tests ====================

    @Test
    fun testFractionalJulianDay() {
        // 소수점 있는 Julian Day 처리
        val baseDate = LocalDate(2024, 1, 1)
        val baseJD = DateUtils.toJulianDay(baseDate)
        val nextDate = LocalDate(2024, 1, 2)

        // 소수 부분이 있는 JD 처리 테스트
        // 일부 구현에서는 0.5를 더하면 다음 날로 넘어갈 수 있음
        val fractionalJD1 = baseJD + 0.1
        val fractionalJD2 = baseJD + 0.4
        val fractionalJD3 = baseJD + 0.6
        val fractionalJD4 = baseJD + 0.9

        // 변환된 날짜가 baseDate 또는 nextDate 중 하나임을 확인
        val converted1 = DateUtils.fromJulianDay(fractionalJD1)
        val converted2 = DateUtils.fromJulianDay(fractionalJD2)
        val converted3 = DateUtils.fromJulianDay(fractionalJD3)
        val converted4 = DateUtils.fromJulianDay(fractionalJD4)

        // 모든 변환이 유효한 날짜를 반환하는지 확인
        assertTrue(converted1 == baseDate || converted1 == nextDate,
            "JD + 0.1 변환 결과는 같은 날 또는 다음 날")
        assertTrue(converted2 == baseDate || converted2 == nextDate,
            "JD + 0.4 변환 결과는 같은 날 또는 다음 날")
        assertTrue(converted3 == baseDate || converted3 == nextDate,
            "JD + 0.6 변환 결과는 같은 날 또는 다음 날")
        assertTrue(converted4 == baseDate || converted4 == nextDate,
            "JD + 0.9 변환 결과는 같은 날 또는 다음 날")
    }

    // ==================== Special Calendar Events Tests ====================

    @Test
    fun testSpecialDates() {
        // 특별한 날짜들 테스트
        val specialDates = listOf(
            LocalDate(2024, 1, 1),   // 새해 첫날
            LocalDate(2024, 2, 14),  // 발렌타인데이
            LocalDate(2024, 12, 25), // 크리스마스
            LocalDate(2024, 10, 31), // 할로윈
            LocalDate(2024, 4, 1)    // 만우절
        )

        for (date in specialDates) {
            val jd = DateUtils.toJulianDay(date)
            val converted = DateUtils.fromJulianDay(jd)
            assertEquals(date, converted, "특별한 날짜 변환: $date")
        }
    }

    // ==================== Consistency Tests ====================

    @Test
    fun testConsistencyAcrossMultipleConversions() {
        // 여러 번 변환해도 일관성 유지
        val originalDate = LocalDate(2024, 6, 15)
        var currentDate = originalDate

        repeat(10) {
            val jd = DateUtils.toJulianDay(currentDate)
            currentDate = DateUtils.fromJulianDay(jd)
        }

        assertEquals(originalDate, currentDate,
            "10번 왕복 변환 후에도 같은 날짜")
    }

    @Test
    fun testJulianDayOrdering() {
        // Julian Day 순서가 날짜 순서와 일치하는지 확인
        val dates = listOf(
            LocalDate(2023, 12, 31),
            LocalDate(2024, 1, 1),
            LocalDate(2024, 6, 15),
            LocalDate(2024, 12, 31),
            LocalDate(2025, 1, 1)
        )

        val jds = dates.map { DateUtils.toJulianDay(it) }

        // Julian Day가 증가하는 순서인지 확인
        for (i in 0 until jds.size - 1) {
            assertTrue(jds[i] < jds[i + 1],
                "${dates[i]}의 JD는 ${dates[i + 1]}의 JD보다 작아야 함")
        }
    }

    // ==================== Performance Tests ====================

    @Test
    fun testPerformanceForMassConversion() {
        // 대량 변환 성능 테스트 (1년치 날짜)
        val conversions = mutableListOf<Pair<LocalDate, Double>>()

        // 2024년의 모든 날짜를 순차적으로 생성
        for (month in 1..12) {
            val daysInMonth = when (month) {
                1, 3, 5, 7, 8, 10, 12 -> 31
                4, 6, 9, 11 -> 30
                2 -> 29 // 2024는 윤년
                else -> 0
            }

            for (day in 1..daysInMonth) {
                val date = LocalDate(2024, month, day)
                val jd = DateUtils.toJulianDay(date)
                conversions.add(date to jd)
            }
        }

        // 모든 변환이 성공적으로 완료되었는지 확인
        assertEquals(366, conversions.size, "366개 날짜 모두 변환 성공")

        // 역변환 테스트
        for ((originalDate, jd) in conversions) {
            val converted = DateUtils.fromJulianDay(jd)
            assertEquals(originalDate, converted)
        }
    }

    // ==================== Gregorian Calendar Transition Tests ====================

    @Test
    fun testGregorianCalendarTransition() {
        // 그레고리력 전환 시점 (1582년 10월) 테스트
        // 일부 구현에서는 이 기간을 다르게 처리할 수 있음

        // 그레고리력 채택 후의 날짜로 테스트 (안전한 날짜)
        val safeDate1 = LocalDate(1600, 1, 1)
        val safeDate2 = LocalDate(1700, 1, 1)

        val jd1 = DateUtils.toJulianDay(safeDate1)
        val jd2 = DateUtils.toJulianDay(safeDate2)

        // 왕복 변환 테스트
        assertEquals(safeDate1, DateUtils.fromJulianDay(jd1),
            "1600년 날짜 왕복 변환")
        assertEquals(safeDate2, DateUtils.fromJulianDay(jd2),
            "1700년 날짜 왕복 변환")

        // 100년 차이는 약 36525일 (윤년 고려)
        val expectedDaysDiff = 36524.0 // 1600-1700년 사이 일수 (1700년은 평년)
        val actualDiff = jd2 - jd1
        assertTrue(abs(actualDiff - expectedDaysDiff) < 2.0,
            "100년 차이의 JD 계산: expected ~$expectedDaysDiff, got $actualDiff")
    }

    // ==================== Helper Function Tests ====================

    private fun assertDateEquals(expected: LocalDate, actual: LocalDate, message: String = "") {
        assertEquals(expected.year, actual.year, "$message - 년도")
        assertEquals(expected.monthNumber, actual.monthNumber, "$message - 월")
        assertEquals(expected.dayOfMonth, actual.dayOfMonth, "$message - 일")
    }

    @Test
    fun testDateEquality() {
        // 날짜 동등성 테스트
        val date1 = LocalDate(2024, 6, 15)
        val date2 = LocalDate(2024, 6, 15)
        val date3 = LocalDate(2024, 6, 16)

        assertDateEquals(date1, date2, "같은 날짜")

        // 다른 날짜는 실패해야 함 (테스트의 테스트)
        var differentDates = false
        try {
            assertDateEquals(date1, date3, "다른 날짜")
        } catch (e: AssertionError) {
            differentDates = true
        }
        assertTrue(differentDates, "다른 날짜는 assertDateEquals에서 실패해야 함")
    }
}