package com.bomcomes.calculator.integration.common

import com.bomcomes.calculator.models.DateRange
import kotlinx.datetime.LocalDate

/**
 * 공통 테스트 모델 정의
 *
 * 모든 테스트 파일에서 공유하는 데이터 클래스들
 * TC-XX-GG-CC 형식의 테스트 케이스 ID 사용
 * (XX: 파일번호, GG: 그룹번호, CC: 케이스번호)
 */

/**
 * 테스트 케이스 데이터
 *
 * @param id TC-XX-GG-CC 형식의 테스트 케이스 ID
 * @param name 테스트 케이스 이름
 * @param fromDate 조회 시작일
 * @param toDate 조회 종료일
 * @param today 오늘 날짜 (기준일)
 * @param expectedCycles 예상 결과 주기 목록
 */
data class TestCase(
    val id: String,
    val name: String,
    val fromDate: LocalDate,
    val toDate: LocalDate,
    val today: LocalDate,
    val expectedCycles: List<ExpectedCycle>
)

/**
 * 피임약 테스트 케이스 데이터
 *
 * @param id TC-XX-GG-CC 형식의 테스트 케이스 ID
 * @param name 테스트 케이스 이름
 * @param fromDate 조회 시작일
 * @param toDate 조회 종료일
 * @param today 오늘 날짜 (기준일)
 * @param pillSettings 피임약 설정
 * @param pillPackages 피임약 패키지 시작일 목록
 * @param expectedCycles 예상 결과 주기 목록
 */
data class PillTestCase(
    val id: String,
    val name: String,
    val fromDate: LocalDate,
    val toDate: LocalDate,
    val today: LocalDate,
    val pillSettings: PillSettingsData,
    val pillPackages: List<LocalDate>,
    val expectedCycles: List<ExpectedCycle>
)

/**
 * 피임약 설정 데이터
 *
 * @param isCalculatingWithPill 피임약 기반 계산 여부
 * @param pillCount 피임약 개수
 * @param restPill 휴약 기간
 */
data class PillSettingsData(
    val isCalculatingWithPill: Boolean,
    val pillCount: Int = 21,
    val restPill: Int = 7
)

/**
 * 예상 주기 결과 데이터
 *
 * CycleInfo 모델과 대응되는 테스트용 데이터 클래스
 * 모든 필드에 기본값을 제공하여 유연하게 사용 가능
 *
 * @param pk 주기 식별자
 * @param actualPeriod 실제 생리 기간
 * @param delayDays 지연 일수
 * @param delayPeriod 지연 기간
 * @param predictDays 생리 예정일들
 * @param fertileDays 가임기들
 * @param ovulationDays 배란기들
 * @param period 주기 일수
 * @param isOvulationUserInput 배란일 사용자 입력 여부
 * @param ovulationBasedPeriod 배란일 기준 주기
 * @param pillBasedPeriod 피임약 기준 주기
 * @param remainingPlaceboDay 남은 휴약일
 * @param isContinuousPillUsage 연속 복용 여부 (휴약기 0일)
 * @param pregnancyStartDate 임신 시작일 (Julian Day)
 */
data class ExpectedCycle(
    val pk: String,
    val actualPeriod: DateRange? = null,
    val delayDays: Int = 0,
    val delayPeriod: DateRange? = null,
    val predictDays: List<DateRange> = emptyList(),
    val fertileDays: List<DateRange> = emptyList(),
    val ovulationDays: List<DateRange> = emptyList(),
    val period: Int = 0,
    val isOvulationUserInput: Boolean = false,
    val ovulationBasedPeriod: Int? = null,
    val pillBasedPeriod: Int? = null,
    val remainingPlaceboDay: Int? = null,
    val isContinuousPillUsage: Boolean = false,
    val pregnancyStartDate: Double? = null
)
