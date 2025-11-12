package com.bomcomes.calculator.repository

import com.bomcomes.calculator.dto.*
import com.bomcomes.calculator.models.*
import kotlin.js.JsExport
import kotlin.js.Promise

/**
 * JavaScript용 Period Data Repository 인터페이스
 *
 * DateInput을 통해 여러 날짜 형식을 지원:
 * - ISO-8601 문자열: "2020-03-26T00:00:00.000Z"
 * - Julian day: 정수 값
 * - LocalDate: kotlinx-datetime LocalDate 객체
 */
@JsExport
interface JsPeriodDataRepository {

    /**
     * 생리 기록 가져오기
     */
    fun getPeriods(fromDate: DateInput, toDate: DateInput): Promise<Array<JsPeriodRecordDto>>

    /**
     * 생리 주기 설정 가져오기
     */
    fun getPeriodSettings(): Promise<PeriodSettings>

    /**
     * 배란 테스트 결과 가져오기
     */
    fun getOvulationTests(fromDate: DateInput, toDate: DateInput): Promise<Array<JsOvulationTestDto>>

    /**
     * 사용자가 직접 입력한 배란일 가져오기
     */
    fun getUserOvulationDays(fromDate: DateInput, toDate: DateInput): Promise<Array<JsOvulationDayDto>>

    /**
     * 피임약 패키지 정보 가져오기
     */
    fun getPillPackages(): Promise<Array<JsPillPackageDto>>

    /**
     * 피임약 설정 가져오기
     */
    fun getPillSettings(): Promise<PillSettings>

    /**
     * 활성 임신 정보 가져오기
     */
    fun getActivePregnancy(): Promise<JsPregnancyInfoDto?>

    /**
     * 특정 날짜 이전의 가장 최근 생리 기록 가져오기
     *
     * @param date 기준 날짜
     * @param excludeBeforeDate 이 날짜 이전의 생리 제외 (임신 출산일 등)
     */
    fun getLastPeriodBefore(date: DateInput, excludeBeforeDate: DateInput? = null): Promise<JsPeriodRecordDto?>

    /**
     * 특정 날짜 이후의 가장 가까운 생리 기록 가져오기
     *
     * @param date 기준 날짜
     * @param excludeAfterDate 이 날짜 이후에 끝난 생리 제외 (임신 시작일 등)
     */
    fun getFirstPeriodAfter(date: DateInput, excludeAfterDate: DateInput? = null): Promise<JsPeriodRecordDto?>
}
