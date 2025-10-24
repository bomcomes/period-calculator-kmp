package com.bomcomes.calculator.repository

import com.bomcomes.calculator.models.*
import kotlinx.datetime.LocalDate

/**
 * 생리 기록 Repository 인터페이스
 */
interface PeriodRepository {
    /**
     * 특정 기간의 생리 기록 조회
     */
    suspend fun getPeriods(from: LocalDate, to: LocalDate): List<PeriodRecord>

    /**
     * 가장 최근 생리 기록 조회
     */
    suspend fun getLatestPeriod(): PeriodRecord?

    /**
     * 생리 기록 저장
     */
    suspend fun savePeriod(period: PeriodRecord)

    /**
     * 생리 기록 삭제
     */
    suspend fun deletePeriod(pk: String)
}

/**
 * 배란 테스트 Repository 인터페이스
 */
interface OvulationTestRepository {
    /**
     * 특정 기간의 배란 테스트 결과 조회
     */
    suspend fun getTests(from: LocalDate, to: LocalDate): List<OvulationTest>

    /**
     * 양성 결과만 조회
     */
    suspend fun getPositiveTests(from: LocalDate, to: LocalDate): List<OvulationTest>

    /**
     * 배란 테스트 결과 저장
     */
    suspend fun saveTest(test: OvulationTest)
}

/**
 * 배란일 직접 입력 Repository 인터페이스
 */
interface OvulationDayRepository {
    /**
     * 특정 기간의 배란일 조회
     */
    suspend fun getOvulationDays(from: LocalDate, to: LocalDate): List<OvulationDay>

    /**
     * 배란일 저장
     */
    suspend fun saveOvulationDay(ovulationDay: OvulationDay)

    /**
     * 배란일 삭제
     */
    suspend fun deleteOvulationDay(date: LocalDate)
}

/**
 * 피임약 Repository 인터페이스
 */
interface PillRepository {
    /**
     * 특정 날짜 이후의 피임약 패키지 조회
     */
    suspend fun getPillPackages(from: LocalDate): List<PillPackage>

    /**
     * 피임약 설정 조회
     */
    suspend fun getPillSettings(): PillSettings

    /**
     * 피임약 패키지 저장
     */
    suspend fun savePillPackage(pillPackage: PillPackage)

    /**
     * 피임약 설정 저장
     */
    suspend fun savePillSettings(settings: PillSettings)
}

/**
 * 임신 정보 Repository 인터페이스
 */
interface PregnancyRepository {
    /**
     * 진행 중인 임신 정보 조회
     */
    suspend fun getActivePregnancy(): PregnancyInfo?

    /**
     * 특정 날짜에 해당하는 임신 정보 조회
     */
    suspend fun getPregnancy(at: LocalDate): PregnancyInfo?

    /**
     * 모든 임신 기록 조회
     */
    suspend fun getAllPregnancies(): List<PregnancyInfo>

    /**
     * 임신 정보 저장
     */
    suspend fun savePregnancy(pregnancy: PregnancyInfo)

    /**
     * 임신 정보 삭제 (soft delete)
     */
    suspend fun deletePregnancy(id: String)
}

/**
 * 생리 주기 설정 Repository 인터페이스
 */
interface PeriodSettingsRepository {
    /**
     * 생리 주기 설정 조회
     */
    suspend fun getSettings(): PeriodSettings

    /**
     * 생리 주기 설정 저장
     */
    suspend fun saveSettings(settings: PeriodSettings)
}
