package com.bomcomes.calculator.unit

import com.bomcomes.calculator.PregnancyCalculator
import com.bomcomes.calculator.models.PregnancyInfo
import com.bomcomes.calculator.models.WeightUnit
import com.bomcomes.calculator.utils.DateUtils
import kotlinx.datetime.LocalDate
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * PregnancyCalculator 유닛 테스트
 *
 * 임신 관련 모든 계산 로직을 검증합니다:
 * - 출산 예정일 계산
 * - 임신 주수 계산
 * - 삼분기 계산
 * - 진행률 계산
 * - 체중 단위 변환
 */
class PregnancyCalculatorTest {

    // ==================== 출산 예정일 계산 테스트 ====================

    @Test
    fun testCalculateDueDate_normalCase() {
        // Given: 일반적인 마지막 생리 시작일
        val lastPeriodDate = LocalDate(2024, 1, 1)

        // When: 출산 예정일 계산 (280일 후)
        val dueDate = PregnancyCalculator.calculateDueDate(lastPeriodDate)

        // Then: 2024년 10월 7일 (1월 1일 + 280일)
        assertEquals(LocalDate(2024, 10, 7), dueDate, "출산 예정일은 마지막 생리일로부터 280일 후")
    }

    @Test
    fun testCalculateDueDate_leapYear() {
        // Given: 윤년의 2월 29일이 포함된 기간
        val lastPeriodDate = LocalDate(2024, 2, 1)

        // When: 출산 예정일 계산
        val dueDate = PregnancyCalculator.calculateDueDate(lastPeriodDate)

        // Then: 윤년 고려한 정확한 날짜
        assertEquals(LocalDate(2024, 11, 7), dueDate, "윤년이 포함된 경우도 정확히 계산")
    }

    @Test
    fun testCalculateDueDate_yearTransition() {
        // Given: 연도가 바뀌는 경우
        val lastPeriodDate = LocalDate(2023, 12, 1)

        // When: 출산 예정일 계산
        val dueDate = PregnancyCalculator.calculateDueDate(lastPeriodDate)

        // Then: 다음 연도로 정확히 이동
        assertEquals(LocalDate(2024, 9, 6), dueDate, "연도 변경이 정확히 처리됨")
    }

    @Test
    fun testCalculateDueDate_edgeCase_veryEarlyDate() {
        // Given: 매우 이른 날짜
        val lastPeriodDate = LocalDate(1900, 1, 1)

        // When: 출산 예정일 계산
        val dueDate = PregnancyCalculator.calculateDueDate(lastPeriodDate)

        // Then: 280일 후 정확히 계산
        assertEquals(LocalDate(1900, 10, 8), dueDate, "과거 날짜도 정확히 계산")
    }

    // ==================== 마지막 생리일 역계산 테스트 ====================

    @Test
    fun testCalculateLastPeriodDate_normalCase() {
        // Given: 일반적인 출산 예정일
        val dueDate = LocalDate(2024, 10, 7)

        // When: 마지막 생리일 역계산
        val lastPeriodDate = PregnancyCalculator.calculateLastPeriodDate(dueDate)

        // Then: 280일 전
        assertEquals(LocalDate(2024, 1, 1), lastPeriodDate, "출산 예정일로부터 280일 전")
    }

    @Test
    fun testCalculateLastPeriodDate_leapYear() {
        // Given: 윤년 포함 출산 예정일
        val dueDate = LocalDate(2024, 11, 7)

        // When: 마지막 생리일 역계산
        val lastPeriodDate = PregnancyCalculator.calculateLastPeriodDate(dueDate)

        // Then: 윤년 고려한 정확한 날짜
        assertEquals(LocalDate(2024, 2, 1), lastPeriodDate, "윤년 고려한 역계산")
    }

    @Test
    fun testCalculateLastPeriodDate_yearTransition() {
        // Given: 연도 초반 출산 예정일
        val dueDate = LocalDate(2024, 1, 15)

        // When: 마지막 생리일 역계산
        val lastPeriodDate = PregnancyCalculator.calculateLastPeriodDate(dueDate)

        // Then: 전년도로 이동
        assertEquals(LocalDate(2023, 4, 10), lastPeriodDate, "연도 변경 역계산")
    }

    // ==================== 임신 주수 계산 테스트 ====================

    @Test
    fun testCalculateWeeksFromLastPeriod_firstTrimester() {
        // Given: 첫 삼분기 (8주차)
        val lastPeriodDate = LocalDate(2024, 1, 1)
        val currentDate = LocalDate(2024, 2, 26) // 56일 후 = 8주

        // When: 임신 주수 계산
        val weeks = PregnancyCalculator.calculateWeeksFromLastPeriod(lastPeriodDate, currentDate)

        // Then: 8주
        assertEquals(8, weeks, "첫 삼분기 주수 계산")
    }

    @Test
    fun testCalculateWeeksFromLastPeriod_secondTrimester() {
        // Given: 둘째 삼분기 (20주차)
        val lastPeriodDate = LocalDate(2024, 1, 1)
        val currentDate = LocalDate(2024, 5, 20) // 140일 후 = 20주

        // When: 임신 주수 계산
        val weeks = PregnancyCalculator.calculateWeeksFromLastPeriod(lastPeriodDate, currentDate)

        // Then: 20주
        assertEquals(20, weeks, "둘째 삼분기 주수 계산")
    }

    @Test
    fun testCalculateWeeksFromLastPeriod_thirdTrimester() {
        // Given: 셋째 삼분기 (35주차)
        val lastPeriodDate = LocalDate(2024, 1, 1)
        val currentDate = LocalDate(2024, 9, 2) // 245일 후 = 35주

        // When: 임신 주수 계산
        val weeks = PregnancyCalculator.calculateWeeksFromLastPeriod(lastPeriodDate, currentDate)

        // Then: 35주
        assertEquals(35, weeks, "셋째 삼분기 주수 계산")
    }

    @Test
    fun testCalculateWeeksFromLastPeriod_exactWeeks() {
        // Given: 정확히 주 단위로 떨어지는 경우
        val lastPeriodDate = LocalDate(2024, 1, 1)
        val currentDate = LocalDate(2024, 1, 8) // 7일 후 = 1주

        // When: 임신 주수 계산
        val weeks = PregnancyCalculator.calculateWeeksFromLastPeriod(lastPeriodDate, currentDate)

        // Then: 1주
        assertEquals(1, weeks, "정확히 1주")
    }

    @Test
    fun testCalculateWeeksFromLastPeriod_partialWeek() {
        // Given: 주 단위로 떨어지지 않는 경우
        val lastPeriodDate = LocalDate(2024, 1, 1)
        val currentDate = LocalDate(2024, 1, 10) // 9일 후 = 1주 2일

        // When: 임신 주수 계산
        val weeks = PregnancyCalculator.calculateWeeksFromLastPeriod(lastPeriodDate, currentDate)

        // Then: 1주 (일 단위는 버림)
        assertEquals(1, weeks, "부분 주는 버림 처리")
    }

    @Test
    fun testCalculateWeeksFromLastPeriod_overdue() {
        // Given: 예정일 초과 (42주차)
        val lastPeriodDate = LocalDate(2024, 1, 1)
        val currentDate = LocalDate(2024, 10, 21) // 294일 후 = 42주

        // When: 임신 주수 계산
        val weeks = PregnancyCalculator.calculateWeeksFromLastPeriod(lastPeriodDate, currentDate)

        // Then: 42주
        assertEquals(42, weeks, "예정일 초과 케이스")
    }

    @Test
    fun testCalculateWeeksFromLastPeriod_negativeCase() {
        // Given: 현재 날짜가 마지막 생리일보다 이전 (잘못된 입력)
        val lastPeriodDate = LocalDate(2024, 2, 1)
        val currentDate = LocalDate(2024, 1, 1)

        // When: 임신 주수 계산
        val weeks = PregnancyCalculator.calculateWeeksFromLastPeriod(lastPeriodDate, currentDate)

        // Then: 음수 주차
        assertTrue(weeks < 0, "잘못된 날짜 입력시 음수 반환")
    }

    // ==================== 임신 주수와 일수 계산 테스트 ====================

    @Test
    fun testCalculateWeeksAndDays_exactWeeks() {
        // Given: 정확히 주 단위
        val lastPeriodDate = LocalDate(2024, 1, 1)
        val currentDate = LocalDate(2024, 2, 19) // 49일 = 7주 0일

        // When: 주수와 일수 계산
        val (weeks, days) = PregnancyCalculator.calculateWeeksAndDays(lastPeriodDate, currentDate)

        // Then: 7주 0일
        assertEquals(7, weeks, "7주")
        assertEquals(0, days, "0일")
    }

    @Test
    fun testCalculateWeeksAndDays_withRemainingDays() {
        // Given: 주 + 남은 일수
        val lastPeriodDate = LocalDate(2024, 1, 1)
        val currentDate = LocalDate(2024, 2, 23) // 53일 = 7주 4일

        // When: 주수와 일수 계산
        val (weeks, days) = PregnancyCalculator.calculateWeeksAndDays(lastPeriodDate, currentDate)

        // Then: 7주 4일
        assertEquals(7, weeks, "7주")
        assertEquals(4, days, "4일")
    }

    @Test
    fun testCalculateWeeksAndDays_lessThanWeek() {
        // Given: 1주 미만
        val lastPeriodDate = LocalDate(2024, 1, 1)
        val currentDate = LocalDate(2024, 1, 5) // 4일 = 0주 4일

        // When: 주수와 일수 계산
        val (weeks, days) = PregnancyCalculator.calculateWeeksAndDays(lastPeriodDate, currentDate)

        // Then: 0주 4일
        assertEquals(0, weeks, "0주")
        assertEquals(4, days, "4일")
    }

    @Test
    fun testCalculateWeeksAndDays_fullTerm() {
        // Given: 만삭 (40주)
        val lastPeriodDate = LocalDate(2024, 1, 1)
        val currentDate = LocalDate(2024, 10, 7) // 280일 = 40주 0일

        // When: 주수와 일수 계산
        val (weeks, days) = PregnancyCalculator.calculateWeeksAndDays(lastPeriodDate, currentDate)

        // Then: 40주 0일
        assertEquals(40, weeks, "40주")
        assertEquals(0, days, "0일")
    }

    // ==================== 출산 예정일까지 남은 일수 테스트 ====================

    @Test
    fun testCalculateDaysUntilDue_beforeDueDate() {
        // Given: 예정일 30일 전
        val dueDate = LocalDate(2024, 10, 7)
        val currentDate = LocalDate(2024, 9, 7)

        // When: 남은 일수 계산
        val daysLeft = PregnancyCalculator.calculateDaysUntilDue(dueDate, currentDate)

        // Then: 30일 남음
        assertEquals(30, daysLeft, "예정일까지 30일")
    }

    @Test
    fun testCalculateDaysUntilDue_onDueDate() {
        // Given: 예정일 당일
        val dueDate = LocalDate(2024, 10, 7)
        val currentDate = LocalDate(2024, 10, 7)

        // When: 남은 일수 계산
        val daysLeft = PregnancyCalculator.calculateDaysUntilDue(dueDate, currentDate)

        // Then: 0일
        assertEquals(0, daysLeft, "예정일 당일")
    }

    @Test
    fun testCalculateDaysUntilDue_overdue() {
        // Given: 예정일 5일 초과
        val dueDate = LocalDate(2024, 10, 7)
        val currentDate = LocalDate(2024, 10, 12)

        // When: 남은 일수 계산
        val daysLeft = PregnancyCalculator.calculateDaysUntilDue(dueDate, currentDate)

        // Then: -5일 (초과)
        assertEquals(-5, daysLeft, "예정일 5일 초과")
    }

    @Test
    fun testCalculateDaysUntilDue_farFuture() {
        // Given: 매우 먼 미래
        val dueDate = LocalDate(2024, 10, 7)
        val currentDate = LocalDate(2024, 1, 1)

        // When: 남은 일수 계산
        val daysLeft = PregnancyCalculator.calculateDaysUntilDue(dueDate, currentDate)

        // Then: 280일 남음
        assertEquals(280, daysLeft, "임신 초기")
    }

    // ==================== 삼분기 계산 테스트 ====================

    @Test
    fun testCalculateTrimester_beforePregnancy() {
        // Given: 임신 전 (음수 주차)
        val weeks = -1

        // When: 삼분기 계산
        val trimester = PregnancyCalculator.calculateTrimester(weeks)

        // Then: 0 (임신 전)
        assertEquals(0, trimester, "임신 전")
    }

    @Test
    fun testCalculateTrimester_firstTrimester_start() {
        // Given: 첫 삼분기 시작 (0주)
        val weeks = 0

        // When: 삼분기 계산
        val trimester = PregnancyCalculator.calculateTrimester(weeks)

        // Then: 1분기
        assertEquals(1, trimester, "첫 삼분기 시작")
    }

    @Test
    fun testCalculateTrimester_firstTrimester_middle() {
        // Given: 첫 삼분기 중간 (7주)
        val weeks = 7

        // When: 삼분기 계산
        val trimester = PregnancyCalculator.calculateTrimester(weeks)

        // Then: 1분기
        assertEquals(1, trimester, "첫 삼분기 중간")
    }

    @Test
    fun testCalculateTrimester_firstTrimester_end() {
        // Given: 첫 삼분기 끝 (13주)
        val weeks = 13

        // When: 삼분기 계산
        val trimester = PregnancyCalculator.calculateTrimester(weeks)

        // Then: 1분기
        assertEquals(1, trimester, "첫 삼분기 끝")
    }

    @Test
    fun testCalculateTrimester_secondTrimester_start() {
        // Given: 둘째 삼분기 시작 (14주)
        val weeks = 14

        // When: 삼분기 계산
        val trimester = PregnancyCalculator.calculateTrimester(weeks)

        // Then: 2분기
        assertEquals(2, trimester, "둘째 삼분기 시작")
    }

    @Test
    fun testCalculateTrimester_secondTrimester_middle() {
        // Given: 둘째 삼분기 중간 (20주)
        val weeks = 20

        // When: 삼분기 계산
        val trimester = PregnancyCalculator.calculateTrimester(weeks)

        // Then: 2분기
        assertEquals(2, trimester, "둘째 삼분기 중간")
    }

    @Test
    fun testCalculateTrimester_secondTrimester_end() {
        // Given: 둘째 삼분기 끝 (27주)
        val weeks = 27

        // When: 삼분기 계산
        val trimester = PregnancyCalculator.calculateTrimester(weeks)

        // Then: 2분기
        assertEquals(2, trimester, "둘째 삼분기 끝")
    }

    @Test
    fun testCalculateTrimester_thirdTrimester_start() {
        // Given: 셋째 삼분기 시작 (28주)
        val weeks = 28

        // When: 삼분기 계산
        val trimester = PregnancyCalculator.calculateTrimester(weeks)

        // Then: 3분기
        assertEquals(3, trimester, "셋째 삼분기 시작")
    }

    @Test
    fun testCalculateTrimester_thirdTrimester_middle() {
        // Given: 셋째 삼분기 중간 (35주)
        val weeks = 35

        // When: 삼분기 계산
        val trimester = PregnancyCalculator.calculateTrimester(weeks)

        // Then: 3분기
        assertEquals(3, trimester, "셋째 삼분기 중간")
    }

    @Test
    fun testCalculateTrimester_thirdTrimester_end() {
        // Given: 셋째 삼분기 끝 (40주)
        val weeks = 40

        // When: 삼분기 계산
        val trimester = PregnancyCalculator.calculateTrimester(weeks)

        // Then: 3분기
        assertEquals(3, trimester, "셋째 삼분기 끝")
    }

    @Test
    fun testCalculateTrimester_overdue() {
        // Given: 예정일 초과 (42주)
        val weeks = 42

        // When: 삼분기 계산
        val trimester = PregnancyCalculator.calculateTrimester(weeks)

        // Then: 0 (출산 후)
        assertEquals(0, trimester, "예정일 초과")
    }

    @Test
    fun testCalculateTrimester_boundaries() {
        // Given & When & Then: 경계값 테스트
        assertEquals(1, PregnancyCalculator.calculateTrimester(0), "0주 = 1분기")
        assertEquals(1, PregnancyCalculator.calculateTrimester(13), "13주 = 1분기")
        assertEquals(2, PregnancyCalculator.calculateTrimester(14), "14주 = 2분기")
        assertEquals(2, PregnancyCalculator.calculateTrimester(27), "27주 = 2분기")
        assertEquals(3, PregnancyCalculator.calculateTrimester(28), "28주 = 3분기")
        assertEquals(3, PregnancyCalculator.calculateTrimester(40), "40주 = 3분기")
        assertEquals(0, PregnancyCalculator.calculateTrimester(41), "41주 = 출산 후")
    }

    // ==================== getDueDateOrCalculate 테스트 ====================

    @Test
    fun testGetDueDateOrCalculate_withDueDate() {
        // Given: 출산 예정일이 직접 입력된 경우
        val dueDate = DateUtils.toJulianDay(LocalDate(2024, 10, 7))
        val pregnancy = PregnancyInfo(
            id = "test",
            startsDate = DateUtils.toJulianDay(LocalDate(2024, 1, 1)),
            dueDate = dueDate,
            lastTheDayDate = null
        )

        // When: 출산 예정일 가져오기
        val result = PregnancyCalculator.getDueDateOrCalculate(pregnancy)

        // Then: 입력된 예정일 반환
        assertNotNull(result)
        assertEquals(dueDate, result, "직접 입력된 예정일 반환")
    }

    @Test
    fun testGetDueDateOrCalculate_withLastPeriodDate() {
        // Given: 마지막 생리일만 있는 경우
        val lastPeriodDate = DateUtils.toJulianDay(LocalDate(2024, 1, 1))
        val pregnancy = PregnancyInfo(
            id = "test",
            startsDate = DateUtils.toJulianDay(LocalDate(2024, 1, 1)),
            dueDate = null,
            lastTheDayDate = lastPeriodDate
        )

        // When: 출산 예정일 계산
        val result = PregnancyCalculator.getDueDateOrCalculate(pregnancy)

        // Then: 계산된 예정일 (1/1 + 280일 = 10/7)
        assertNotNull(result)
        val expectedDueDate = DateUtils.toJulianDay(LocalDate(2024, 10, 7))
        assertEquals(expectedDueDate, result, "마지막 생리일로부터 계산된 예정일")
    }

    @Test
    fun testGetDueDateOrCalculate_withBoth() {
        // Given: 둘 다 있는 경우
        val dueDate = DateUtils.toJulianDay(LocalDate(2024, 10, 10))
        val lastPeriodDate = DateUtils.toJulianDay(LocalDate(2024, 1, 1))
        val pregnancy = PregnancyInfo(
            id = "test",
            startsDate = DateUtils.toJulianDay(LocalDate(2024, 1, 1)),
            dueDate = dueDate,
            lastTheDayDate = lastPeriodDate
        )

        // When: 출산 예정일 가져오기
        val result = PregnancyCalculator.getDueDateOrCalculate(pregnancy)

        // Then: 직접 입력된 예정일 우선
        assertNotNull(result)
        assertEquals(dueDate, result, "직접 입력된 예정일 우선")
    }

    @Test
    fun testGetDueDateOrCalculate_withNeither() {
        // Given: 둘 다 없는 경우
        val pregnancy = PregnancyInfo(
            id = "test",
            startsDate = DateUtils.toJulianDay(LocalDate(2024, 1, 1)),
            dueDate = null,
            lastTheDayDate = null
        )

        // When: 출산 예정일 가져오기
        val result = PregnancyCalculator.getDueDateOrCalculate(pregnancy)

        // Then: null 반환
        assertNull(result, "계산 불가능한 경우 null")
    }

    // ==================== 체중 단위 변환 테스트 ====================

    @Test
    fun testKgToLbs_normalValue() {
        // Given: 일반적인 체중
        val kg = 70f

        // When: kg → lbs 변환
        val lbs = PregnancyCalculator.kgToLbs(kg)

        // Then: 154.32 lbs (오차 범위 0.1)
        assertEquals(154.32f, lbs, 0.1f, "70kg = 154.32lbs")
    }

    @Test
    fun testKgToLbs_zero() {
        // Given: 0 kg
        val kg = 0f

        // When: kg → lbs 변환
        val lbs = PregnancyCalculator.kgToLbs(kg)

        // Then: 0 lbs
        assertEquals(0f, lbs, "0kg = 0lbs")
    }

    @Test
    fun testKgToLbs_decimal() {
        // Given: 소수점 체중
        val kg = 65.5f

        // When: kg → lbs 변환
        val lbs = PregnancyCalculator.kgToLbs(kg)

        // Then: 144.4 lbs
        assertEquals(144.4f, lbs, 0.1f, "65.5kg 변환")
    }

    @Test
    fun testLbsToKg_normalValue() {
        // Given: 일반적인 체중
        val lbs = 154.32f

        // When: lbs → kg 변환
        val kg = PregnancyCalculator.lbsToKg(lbs)

        // Then: 70 kg (오차 범위 0.1)
        assertEquals(70f, kg, 0.1f, "154.32lbs = 70kg")
    }

    @Test
    fun testLbsToKg_zero() {
        // Given: 0 lbs
        val lbs = 0f

        // When: lbs → kg 변환
        val kg = PregnancyCalculator.lbsToKg(lbs)

        // Then: 0 kg
        assertEquals(0f, kg, "0lbs = 0kg")
    }

    @Test
    fun testKgToStone_normalValue() {
        // Given: 일반적인 체중
        val kg = 70f

        // When: kg → stone 변환
        val stone = PregnancyCalculator.kgToStone(kg)

        // Then: 11.02 stone (오차 범위 0.1)
        assertEquals(11.02f, stone, 0.1f, "70kg = 11.02stone")
    }

    @Test
    fun testKgToStone_zero() {
        // Given: 0 kg
        val kg = 0f

        // When: kg → stone 변환
        val stone = PregnancyCalculator.kgToStone(kg)

        // Then: 0 stone
        assertEquals(0f, stone, "0kg = 0stone")
    }

    @Test
    fun testStoneToKg_normalValue() {
        // Given: 일반적인 체중
        val stone = 11.02f

        // When: stone → kg 변환
        val kg = PregnancyCalculator.stoneToKg(stone)

        // Then: 70 kg (오차 범위 0.1)
        assertEquals(70f, kg, 0.1f, "11.02stone = 70kg")
    }

    @Test
    fun testStoneToKg_zero() {
        // Given: 0 stone
        val stone = 0f

        // When: stone → kg 변환
        val kg = PregnancyCalculator.stoneToKg(stone)

        // Then: 0 kg
        assertEquals(0f, kg, "0stone = 0kg")
    }

    @Test
    fun testNormalizeWeightToKg_fromKg() {
        // Given: kg 단위 체중
        val weight = 70f
        val unit = WeightUnit.KG

        // When: kg로 정규화
        val normalized = PregnancyCalculator.normalizeWeightToKg(weight, unit)

        // Then: 그대로 반환
        assertEquals(70f, normalized, "kg는 그대로")
    }

    @Test
    fun testNormalizeWeightToKg_fromLbs() {
        // Given: lbs 단위 체중
        val weight = 154.32f
        val unit = WeightUnit.LBS

        // When: kg로 정규화
        val normalized = PregnancyCalculator.normalizeWeightToKg(weight, unit)

        // Then: 70 kg
        assertEquals(70f, normalized, 0.1f, "lbs → kg 변환")
    }

    @Test
    fun testNormalizeWeightToKg_fromStone() {
        // Given: stone 단위 체중
        val weight = 11.02f
        val unit = WeightUnit.ST

        // When: kg로 정규화
        val normalized = PregnancyCalculator.normalizeWeightToKg(weight, unit)

        // Then: 70 kg
        assertEquals(70f, normalized, 0.1f, "stone → kg 변환")
    }

    @Test
    fun testWeightConversion_roundTrip_kgLbs() {
        // Given: 원래 체중
        val originalKg = 75.3f

        // When: kg → lbs → kg 왕복 변환
        val lbs = PregnancyCalculator.kgToLbs(originalKg)
        val backToKg = PregnancyCalculator.lbsToKg(lbs)

        // Then: 원래 값과 동일 (오차 범위 0.01)
        assertEquals(originalKg, backToKg, 0.01f, "왕복 변환 정확도")
    }

    @Test
    fun testWeightConversion_roundTrip_kgStone() {
        // Given: 원래 체중
        val originalKg = 82.7f

        // When: kg → stone → kg 왕복 변환
        val stone = PregnancyCalculator.kgToStone(originalKg)
        val backToKg = PregnancyCalculator.stoneToKg(stone)

        // Then: 원래 값과 동일 (오차 범위 0.01)
        assertEquals(originalKg, backToKg, 0.01f, "왕복 변환 정확도")
    }

    // ==================== 진행률 계산 테스트 ====================

    @Test
    fun testCalculateProgress_start() {
        // Given: 임신 시작일
        val lastPeriodDate = LocalDate(2024, 1, 1)
        val currentDate = LocalDate(2024, 1, 1)

        // When: 진행률 계산
        val progress = PregnancyCalculator.calculateProgress(lastPeriodDate, currentDate)

        // Then: 0%
        assertEquals(0f, progress, "임신 시작 = 0%")
    }

    @Test
    fun testCalculateProgress_quarter() {
        // Given: 1/4 지점 (70일)
        val lastPeriodDate = LocalDate(2024, 1, 1)
        val currentDate = LocalDate(2024, 3, 11) // 70일 후

        // When: 진행률 계산
        val progress = PregnancyCalculator.calculateProgress(lastPeriodDate, currentDate)

        // Then: 25%
        assertEquals(25f, progress, "70일 = 25%")
    }

    @Test
    fun testCalculateProgress_half() {
        // Given: 절반 지점 (140일)
        val lastPeriodDate = LocalDate(2024, 1, 1)
        val currentDate = LocalDate(2024, 5, 20) // 140일 후

        // When: 진행률 계산
        val progress = PregnancyCalculator.calculateProgress(lastPeriodDate, currentDate)

        // Then: 50%
        assertEquals(50f, progress, "140일 = 50%")
    }

    @Test
    fun testCalculateProgress_threeQuarters() {
        // Given: 3/4 지점 (210일)
        val lastPeriodDate = LocalDate(2024, 1, 1)
        val currentDate = LocalDate(2024, 7, 29) // 210일 후

        // When: 진행률 계산
        val progress = PregnancyCalculator.calculateProgress(lastPeriodDate, currentDate)

        // Then: 75%
        assertEquals(75f, progress, "210일 = 75%")
    }

    @Test
    fun testCalculateProgress_fullTerm() {
        // Given: 만삭 (280일)
        val lastPeriodDate = LocalDate(2024, 1, 1)
        val currentDate = LocalDate(2024, 10, 7) // 280일 후

        // When: 진행률 계산
        val progress = PregnancyCalculator.calculateProgress(lastPeriodDate, currentDate)

        // Then: 100%
        assertEquals(100f, progress, "280일 = 100%")
    }

    @Test
    fun testCalculateProgress_overdue() {
        // Given: 예정일 초과 (300일)
        val lastPeriodDate = LocalDate(2024, 1, 1)
        val currentDate = LocalDate(2024, 10, 27) // 300일 후

        // When: 진행률 계산
        val progress = PregnancyCalculator.calculateProgress(lastPeriodDate, currentDate)

        // Then: 100% (상한선)
        assertEquals(100f, progress, "예정일 초과해도 100% 상한")
    }

    @Test
    fun testCalculateProgress_beforePregnancy() {
        // Given: 임신 전 (음수일)
        val lastPeriodDate = LocalDate(2024, 2, 1)
        val currentDate = LocalDate(2024, 1, 1)

        // When: 진행률 계산
        val progress = PregnancyCalculator.calculateProgress(lastPeriodDate, currentDate)

        // Then: 0% (하한선)
        assertEquals(0f, progress, "임신 전은 0% 하한")
    }

    @Test
    fun testCalculateProgress_precision() {
        // Given: 정밀한 진행률 계산이 필요한 경우
        val lastPeriodDate = LocalDate(2024, 1, 1)
        val currentDate = LocalDate(2024, 4, 10) // 100일 후

        // When: 진행률 계산
        val progress = PregnancyCalculator.calculateProgress(lastPeriodDate, currentDate)

        // Then: 35.71% (100/280 * 100)
        val expected = (100f / 280f) * 100f
        assertEquals(expected, progress, 0.01f, "정밀한 진행률 계산")
    }

    // ==================== 엣지 케이스 및 통합 테스트 ====================

    @Test
    fun testIntegration_fullPregnancyCycle() {
        // Given: 전체 임신 주기 시뮬레이션
        val lastPeriodDate = LocalDate(2024, 1, 1)
        val dueDate = PregnancyCalculator.calculateDueDate(lastPeriodDate)

        // When & Then: 각 단계별 검증

        // 첫 삼분기 (8주)
        val week8 = lastPeriodDate.plus(56, DateTimeUnit.DAY)
        assertEquals(8, PregnancyCalculator.calculateWeeksFromLastPeriod(lastPeriodDate, week8))
        assertEquals(1, PregnancyCalculator.calculateTrimester(8))
        assertTrue(PregnancyCalculator.calculateProgress(lastPeriodDate, week8) < 30f)

        // 둘째 삼분기 (20주)
        val week20 = lastPeriodDate.plus(140, DateTimeUnit.DAY)
        assertEquals(20, PregnancyCalculator.calculateWeeksFromLastPeriod(lastPeriodDate, week20))
        assertEquals(2, PregnancyCalculator.calculateTrimester(20))
        assertEquals(50f, PregnancyCalculator.calculateProgress(lastPeriodDate, week20))

        // 셋째 삼분기 (35주)
        val week35 = lastPeriodDate.plus(245, DateTimeUnit.DAY)
        assertEquals(35, PregnancyCalculator.calculateWeeksFromLastPeriod(lastPeriodDate, week35))
        assertEquals(3, PregnancyCalculator.calculateTrimester(35))
        assertTrue(PregnancyCalculator.calculateProgress(lastPeriodDate, week35) > 85f)

        // 출산 예정일
        assertEquals(40, PregnancyCalculator.calculateWeeksFromLastPeriod(lastPeriodDate, dueDate))
        assertEquals(3, PregnancyCalculator.calculateTrimester(40))
        assertEquals(100f, PregnancyCalculator.calculateProgress(lastPeriodDate, dueDate))
        assertEquals(0, PregnancyCalculator.calculateDaysUntilDue(dueDate, dueDate))
    }

    @Test
    fun testEdgeCase_extremeDates() {
        // Given: 극단적인 날짜들
        val veryEarlyDate = LocalDate(1000, 1, 1)
        val veryLateDate = LocalDate(3000, 12, 31)

        // When & Then: 계산이 정상 작동하는지 확인
        val earlyDueDate = PregnancyCalculator.calculateDueDate(veryEarlyDate)
        assertTrue(earlyDueDate.year == 1000, "과거 극단 날짜 처리")

        val lateLastPeriod = PregnancyCalculator.calculateLastPeriodDate(veryLateDate)
        assertTrue(lateLastPeriod.year == 3000, "미래 극단 날짜 처리")
    }

    @Test
    fun testEdgeCase_pregnancyInfoWithMiscarriage() {
        // Given: 유산 시나리오
        val pregnancy = PregnancyInfo(
            id = "test",
            startsDate = DateUtils.toJulianDay(LocalDate(2024, 1, 1)),
            lastTheDayDate = DateUtils.toJulianDay(LocalDate(2024, 1, 1)),
            isMiscarriage = true,
            isEnded = true
        )

        // When: 예정일 계산 시도
        val dueDate = PregnancyCalculator.getDueDateOrCalculate(pregnancy)

        // Then: 계산은 가능 (하지만 유산 플래그 확인은 별도 로직에서)
        assertNotNull(dueDate, "유산이어도 계산은 가능")
    }

    @Test
    fun testEdgeCase_multiplePregnancy() {
        // Given: 다태아 시나리오
        val pregnancy = PregnancyInfo(
            id = "test",
            startsDate = DateUtils.toJulianDay(LocalDate(2024, 1, 1)),
            lastTheDayDate = DateUtils.toJulianDay(LocalDate(2024, 1, 1)),
            isMultipleBirth = true
        )

        // When: 일반적인 계산 (다태아는 보통 조산 경향이 있지만 기본 계산은 동일)
        val dueDate = PregnancyCalculator.getDueDateOrCalculate(pregnancy)

        // Then: 기본 계산은 동일
        assertNotNull(dueDate, "다태아도 기본 계산 동일")
        val expectedDue = DateUtils.toJulianDay(LocalDate(2024, 10, 7))
        assertEquals(expectedDue, dueDate, "280일 기준")
    }

    private fun assertEquals(expected: Float, actual: Float, delta: Float, message: String) {
        assertTrue(
            abs(expected - actual) <= delta,
            "$message - Expected: $expected, Actual: $actual, Delta: ${abs(expected - actual)}"
        )
    }
}