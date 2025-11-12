package com.bomcomes.calculator.dto

import kotlin.js.JsExport

/**
 * JavaScript용 DTO 클래스들
 * 모든 날짜는 문자열 (ISO-8601 형식: "2024-01-05")
 */

@JsExport
data class JsPeriodRecordDto(
    val pk: String = "",
    val startDate: String,  // ISO-8601: "2024-01-05"
    val endDate: String
)

@JsExport
data class JsOvulationTestDto(
    val date: String,  // ISO-8601: "2024-01-05"
    val result: String     // "POSITIVE", "NEGATIVE", "UNCLEAR"
)

@JsExport
data class JsOvulationDayDto(
    val date: String  // ISO-8601: "2024-01-05"
)

@JsExport
data class JsPillPackageDto(
    val packageStart: String,  // ISO-8601: "2024-01-05"
    val pillCount: Int = 21,
    val restDays: Int = 7
)

@JsExport
data class JsPregnancyInfoDto(
    val id: String = "",
    val babyName: String = "",
    val isDueDateDecided: Boolean = false,
    val lastTheDayDate: String? = null,  // ISO-8601: "2024-01-05"
    val dueDate: String? = null,
    val beforePregnancyWeight: Float? = null,
    val weightUnit: String = "KG",  // "KG", "LBS", "ST"
    val isMultipleBirth: Boolean = false,
    val isMiscarriage: Boolean = false,
    val startsDate: String,  // ISO-8601: "2024-01-05"
    val isEnded: Boolean = false,
    val modifyDate: Double = 0.0,  // JavaScript Number (Long은 export 안됨)
    val regDate: Double = 0.0,
    val isDeleted: Boolean = false
)
