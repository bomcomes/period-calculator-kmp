package com.bomcomes.calculator.repository

import com.bomcomes.calculator.models.*

/**
 * Period Data Repository Interface
 *
 * 각 플랫폼(iOS/Android/Firebase)이 자신의 로컬 DB로 구현해야 하는 인터페이스
 * - iOS: Realm
 * - Android: Room
 * - Firebase: Firestore
 */
interface PeriodDataRepository {

    /**
     * 생리 기록 가져오기
     *
     * 검색 범위와 겹치는 모든 생리 기록 반환
     * 조건: startDate <= toDate AND fromDate <= endDate
     *
     * @param fromDate 시작 날짜 (julianDay)
     * @param toDate 종료 날짜 (julianDay)
     * @return 생리 기록 리스트
     */
    suspend fun getPeriods(fromDate: Double, toDate: Double): List<PeriodRecord>

    /**
     * 생리 주기 설정 가져오기
     * @return 생리 설정 (없으면 기본값)
     */
    suspend fun getPeriodSettings(): PeriodSettings

    /**
     * 배란 테스트 결과 가져오기
     * @param fromDate 시작 날짜 (julianDay)
     * @param toDate 종료 날짜 (julianDay)
     * @return 배란 테스트 리스트
     */
    suspend fun getOvulationTests(fromDate: Double, toDate: Double): List<OvulationTest>

    /**
     * 사용자가 직접 입력한 배란일 가져오기
     * @param fromDate 시작 날짜 (julianDay)
     * @param toDate 종료 날짜 (julianDay)
     * @return 배란일 리스트
     */
    suspend fun getUserOvulationDays(fromDate: Double, toDate: Double): List<OvulationDay>

    /**
     * 피임약 패키지 정보 가져오기
     * @return 피임약 패키지 리스트
     */
    suspend fun getPillPackages(): List<PillPackage>

    /**
     * 피임약 설정 가져오기
     * @return 피임약 설정
     */
    suspend fun getPillSettings(): PillSettings

    /**
     * 활성 임신 정보 가져오기
     * @return 임신 정보 (없으면 null)
     */
    suspend fun getActivePregnancy(): PregnancyInfo?

    /**
     * 특정 날짜 이전의 가장 최근 생리 기록 가져오기
     *
     * 임신 출산일이 있으면 출산일 이후의 생리만 반환
     * 조건: startDate <= date AND (pregnancy == null OR startDate > pregnancy.dueDate)
     *
     * @param date 기준 날짜 (julianDay)
     * @param excludeBeforeDate 이 날짜 이전의 생리 제외 (julianDay)
     * @return 기준 날짜 이전의 가장 최근 생리 기록 (없으면 null)
     */
    suspend fun getLastPeriodBefore(date: Double, excludeBeforeDate: Double? = null): PeriodRecord?

    /**
     * 특정 날짜 이후의 가장 가까운 생리 기록 가져오기
     *
     * 임신 시작일이 있으면 시작일 이전에 끝난 생리만 반환
     * 조건: startDate >= date AND (pregnancy == null OR endDate < pregnancy.startsDate)
     *
     * @param date 기준 날짜 (julianDay)
     * @param excludeAfterDate 이 날짜 이후에 끝난 생리 제외 (julianDay)
     * @return 기준 날짜 이후의 가장 가까운 생리 기록 (없으면 null)
     */
    suspend fun getFirstPeriodAfter(date: Double, excludeAfterDate: Double? = null): PeriodRecord?
}
