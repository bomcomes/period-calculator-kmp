package com.bomcomes.calculator

import com.bomcomes.calculator.dto.*
import com.bomcomes.calculator.models.*
import com.bomcomes.calculator.repository.JsPeriodDataRepository
import com.bomcomes.calculator.utils.DateUtils
import kotlinx.datetime.LocalDate
import kotlin.js.Promise

/**
 * JavaScript용 Repository 패턴 Export
 */

/**
 * Repository를 사용하여 생리 주기 계산
 */
@JsExport
@JsName("calculateCycleInfoWithRepository")
fun calculateCycleInfoWithRepositoryJs(
    repository: JsPeriodDataRepository,
    fromDate: DateInput,
    toDate: DateInput
): Promise<Array<CycleInfo>> = Promise { resolve, reject ->
    try {
        // DateInput을 LocalDate로 변환
        val fromLocalDate = DateUtils.toLocalDate(fromDate)
        val toLocalDate = DateUtils.toLocalDate(toDate)

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
            val periodDtos = results[0] as Array<JsPeriodRecordDto>
            val periods = periodDtos.map { dto ->
                PeriodRecord(
                    pk = dto.pk,
                    startDate = LocalDate.parse(dto.startDate),
                    endDate = LocalDate.parse(dto.endDate)
                )
            }.toMutableList()

            // 이전/다음 생리 기록 추가
            val previousPeriodDto = results[1] as JsPeriodRecordDto?
            if (previousPeriodDto != null) {
                periods.add(
                    0,
                    PeriodRecord(
                        pk = previousPeriodDto.pk,
                        startDate = LocalDate.parse(previousPeriodDto.startDate),
                        endDate = LocalDate.parse(previousPeriodDto.endDate)
                    )
                )
            }

            val nextPeriodDto = results[2] as JsPeriodRecordDto?
            if (nextPeriodDto != null) {
                periods.add(
                    PeriodRecord(
                        pk = nextPeriodDto.pk,
                        startDate = LocalDate.parse(nextPeriodDto.startDate),
                        endDate = LocalDate.parse(nextPeriodDto.endDate)
                    )
                )
            }

            val periodSettings = results[3] as PeriodSettings

            val ovulationTestDtos = results[4] as Array<JsOvulationTestDto>
            val ovulationTests = ovulationTestDtos.map { dto ->
                OvulationTest(
                    date = LocalDate.parse(dto.date),
                    result = when (dto.result) {
                        "POSITIVE" -> TestResult.POSITIVE
                        "NEGATIVE" -> TestResult.NEGATIVE
                        "UNCLEAR" -> TestResult.UNCLEAR
                        else -> TestResult.UNCLEAR
                    }
                )
            }

            val ovulationDayDtos = results[5] as Array<JsOvulationDayDto>
            val userOvulationDays = ovulationDayDtos.map { dto ->
                OvulationDay(date = LocalDate.parse(dto.date))
            }

            val pillPackageDtos = results[6] as Array<JsPillPackageDto>
            val pillPackages = pillPackageDtos.map { dto ->
                PillPackage(
                    packageStart = LocalDate.parse(dto.packageStart),
                    pillCount = dto.pillCount,
                    restDays = dto.restDays
                )
            }

            val pillSettings = results[7] as PillSettings

            val pregnancyDto = results[8] as JsPregnancyInfoDto?
            val pregnancy = pregnancyDto?.let { dto ->
                PregnancyInfo(
                    id = dto.id,
                    babyName = dto.babyName,
                    isDueDateDecided = dto.isDueDateDecided,
                    lastTheDayDate = dto.lastTheDayDate?.let { LocalDate.parse(it) },
                    dueDate = dto.dueDate?.let { LocalDate.parse(it) },
                    beforePregnancyWeight = dto.beforePregnancyWeight,
                    weightUnit = when (dto.weightUnit) {
                        "KG" -> WeightUnit.KG
                        "LBS" -> WeightUnit.LBS
                        "ST" -> WeightUnit.ST
                        else -> WeightUnit.KG
                    },
                    isMultipleBirth = dto.isMultipleBirth,
                    isMiscarriage = dto.isMiscarriage,
                    startsDate = LocalDate.parse(dto.startsDate),
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
            val result = PeriodCalculator.calculateCycleInfo(
                input = input,
                fromDate = fromLocalDate,
                toDate = toLocalDate
            )

            resolve(result.toTypedArray())
        }.catch { error ->
            reject(error)
        }
    } catch (e: Throwable) {
        reject(e)
    }
}
