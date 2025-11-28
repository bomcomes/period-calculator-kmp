package com.bomcomes.calculator

import com.bomcomes.calculator.dto.*
import com.bomcomes.calculator.models.*
import com.bomcomes.calculator.repository.JsPeriodDataRepository
import kotlin.js.Promise

/**
 * JavaScript용 Repository 패턴 Export
 */

/**
 * Repository를 사용하여 생리 주기 계산
 *
 * @param repository 데이터 저장소
 * @param fromDate 검색 시작 날짜 (julianDay)
 * @param toDate 검색 종료 날짜 (julianDay)
 * @param today 오늘 날짜 (선택, 없으면 현재 시스템 시간 사용) (julianDay)
 */
@JsExport
@JsName("calculateCycleInfoWithRepository")
fun calculateCycleInfoWithRepositoryJs(
    repository: JsPeriodDataRepository,
    fromDate: DateInput,
    toDate: DateInput,
    today: DateInput? = null
): Promise<Array<JsCycleInfo>> = Promise { resolve, reject ->
    try {
        // Repository에서 데이터 가져오기
        Promise.all(
            arrayOf(
                repository.getPeriods(fromDate, toDate),
                repository.getLastPeriodBefore(fromDate),
                repository.getFirstPeriodAfter(toDate),
                repository.getPeriodSettings(),
                repository.getOvulationTests(fromDate, toDate),
                repository.getUserOvulationDays(fromDate, toDate),
                repository.getPillPackages(),
                repository.getPillSettings(),
                repository.getActivePregnancy()
            )
        ).then { results ->
            // DTO를 모델로 변환
            val periodDtos = results[0] as Array<JsPeriodRecord>
            val periods = periodDtos.map { dto ->
                PeriodRecord(
                    pk = dto.pk,
                    startDate = dto.startDate.toDouble(),
                    endDate = dto.endDate.toDouble()
                )
            }.toMutableList()

            // 이전/다음 생리 기록 추가
            val previousPeriodDto = results[1] as JsPeriodRecord?
            if (previousPeriodDto != null) {
                periods.add(
                    0,
                    PeriodRecord(
                        pk = previousPeriodDto.pk,
                        startDate = previousPeriodDto.startDate.toDouble(),
                        endDate = previousPeriodDto.endDate.toDouble()
                    )
                )
            }

            val nextPeriodDto = results[2] as JsPeriodRecord?
            if (nextPeriodDto != null) {
                periods.add(
                    PeriodRecord(
                        pk = nextPeriodDto.pk,
                        startDate = nextPeriodDto.startDate.toDouble(),
                        endDate = nextPeriodDto.endDate.toDouble()
                    )
                )
            }

            val periodSettings = results[3] as PeriodSettings

            val ovulationTestDtos = results[4] as Array<JsOvulationTest>
            val ovulationTests = ovulationTestDtos.map { dto ->
                OvulationTest(
                    date = dto.date.toDouble(),
                    result = when (dto.result) {
                        "POSITIVE" -> TestResult.POSITIVE
                        "NEGATIVE" -> TestResult.NEGATIVE
                        "UNCLEAR" -> TestResult.UNCLEAR
                        else -> TestResult.UNCLEAR
                    }
                )
            }

            val ovulationDayDtos = results[5] as Array<JsOvulationDay>
            val userOvulationDays = ovulationDayDtos.map { dto ->
                OvulationDay(date = dto.date.toDouble())
            }

            val pillPackageDtos = results[6] as Array<JsPillPackage>
            val pillPackages = pillPackageDtos.map { dto ->
                PillPackage(
                    packageStart = dto.packageStart.toDouble()
                )
            }

            val pillSettings = results[7] as PillSettings

            val pregnancyDto = results[8] as JsPregnancyInfo?
            val pregnancy = pregnancyDto?.let { dto ->
                PregnancyInfo(
                    id = dto.id,
                    babyName = dto.babyName,
                    isDueDateDecided = dto.isDueDateDecided,
                    lastTheDayDate = dto.lastTheDayDate?.toDouble(),
                    dueDate = dto.dueDate?.toDouble(),
                    beforePregnancyWeight = dto.beforePregnancyWeight,
                    weightUnit = when (dto.weightUnit) {
                        "KG" -> WeightUnit.KG
                        "LBS" -> WeightUnit.LBS
                        "ST" -> WeightUnit.ST
                        else -> WeightUnit.KG
                    },
                    isMultipleBirth = dto.isMultipleBirth,
                    isMiscarriage = dto.isMiscarriage,
                    startsDate = dto.startsDate.toDouble(),
                    isEnded = dto.isEnded,
                    modifyDate = dto.modifyDate.toLong(),
                    regDate = dto.regDate.toLong(),
                    isDeleted = dto.isDeleted
                )
            }

            // CycleInput 생성
            val input = CycleInput(
                periods = periods,
                periodSettings = periodSettings,
                ovulationTests = ovulationTests,
                userOvulationDays = userOvulationDays,
                pillPackages = pillPackages,
                pillSettings = pillSettings,
                pregnancy = pregnancy
            )

            // 계산 실행
            val fromDateJulian = fromDate.toJulianDay()
            val toDateJulian = toDate.toJulianDay()
            val todayJulian = today?.toJulianDay()

            val allCycles = PeriodCalculator.calculateCycleInfo(
                input = input,
                fromDate = fromDateJulian,
                toDate = toDateJulian,
                today = todayJulian
            )

            // fromDate 이후의 실제 생리만 반환 (이전 생리는 계산에만 사용)
            val result = allCycles.filter { cycle ->
                cycle.actualPeriod?.startDate?.let { it >= fromDateJulian } ?: true
            }

            // CycleInfo를 JS 호환 형태로 변환 - 이미 Double이므로 그대로 복사
            val jsResult = result.map { cycle ->
                JsCycleInfo(
                    pk = cycle.pk,
                    actualPeriod = cycle.actualPeriod?.let {
                        JsDateRange(
                            it.startDate,
                            it.endDate
                        )
                    },
                    predictDays = cycle.predictDays.map {
                        JsDateRange(
                            it.startDate,
                            it.endDate
                        )
                    }.toTypedArray(),
                    ovulationDays = cycle.ovulationDays.map {
                        JsDateRange(
                            it.startDate,
                            it.endDate
                        )
                    }.toTypedArray(),
                    fertileDays = cycle.fertileDays.map {
                        JsDateRange(
                            it.startDate,
                            it.endDate
                        )
                    }.toTypedArray(),
                    delayPeriod = cycle.delayDay?.let {
                        JsDateRange(
                            it.startDate,
                            it.endDate
                        )
                    },
                    delayDays = cycle.delayTheDays,
                    period = cycle.period,
                    pregnancyStartDate = cycle.pregnancyStartDate
                )
            }.toTypedArray()

            resolve(jsResult)
        }.catch { error ->
            reject(error)
        }
    } catch (e: Throwable) {
        reject(e)
    }
}
