package com.bomcomes.calculator.service

import com.bomcomes.calculator.models.CalendarStatus
import com.bomcomes.calculator.models.PeriodCycle
import kotlinx.datetime.*

/**
 * 캐싱 기능이 추가된 생리 주기 계산 서비스
 */
class CachedPeriodCalculatorService(
    private val service: PeriodCalculatorService,
    private val cacheTtl: Long = 60_000  // 기본 TTL: 1분
) {
    private val statusCache = mutableMapOf<String, CachedStatus>()
    private val cyclesCache = mutableMapOf<String, CachedCycles>()
    private val monthCache = mutableMapOf<String, CachedMonth>()

    /**
     * 특정 날짜의 상태 조회 (캐싱)
     */
    suspend fun getStatus(date: LocalDate): CalendarStatus {
        val key = "status_${date}"
        val cached = statusCache[key]

        // 캐시가 유효하면 반환
        if (cached != null && !cached.isExpired()) {
            return cached.result
        }

        // 캐시 없거나 만료되면 새로 계산
        val result = service.getStatus(date)
        statusCache[key] = CachedStatus(
            result = result,
            timestamp = Clock.System.now().toEpochMilliseconds()
        )

        return result
    }

    /**
     * 기간 내의 주기 정보 조회 (캐싱)
     */
    suspend fun getMenstrualCycles(
        from: LocalDate,
        to: LocalDate
    ): List<PeriodCycle> {
        val key = "cycles_${from}_${to}"
        val cached = cyclesCache[key]

        if (cached != null && !cached.isExpired()) {
            return cached.result
        }

        val result = service.getMenstrualCycles(from, to)
        cyclesCache[key] = CachedCycles(
            result = result,
            timestamp = Clock.System.now().toEpochMilliseconds()
        )

        return result
    }

    /**
     * 한 달치 데이터 조회 (캐싱)
     */
    suspend fun getMonthData(year: Int, month: Int): MonthData {
        val key = "month_${year}_${month}"
        val cached = monthCache[key]

        if (cached != null && !cached.isExpired()) {
            return cached.result
        }

        val result = service.getMonthData(year, month)
        monthCache[key] = CachedMonth(
            result = result,
            timestamp = Clock.System.now().toEpochMilliseconds()
        )

        return result
    }

    /**
     * 모든 캐시 무효화
     */
    fun invalidateAll() {
        statusCache.clear()
        cyclesCache.clear()
        monthCache.clear()
    }

    /**
     * 특정 날짜 범위의 캐시만 무효화
     */
    fun invalidateRange(from: LocalDate, to: LocalDate) {
        // 상태 캐시 무효화
        statusCache.keys.removeIf { key ->
            val dateStr = key.removePrefix("status_")
            try {
                val date = LocalDate.parse(dateStr)
                date in from..to
            } catch (e: Exception) {
                false
            }
        }

        // 주기 캐시 무효화 (범위가 겹치는 것)
        cyclesCache.keys.removeIf { key ->
            val parts = key.removePrefix("cycles_").split("_")
            if (parts.size >= 2) {
                try {
                    val cacheFrom = LocalDate.parse(parts[0])
                    val cacheTo = LocalDate.parse(parts[1])
                    // 범위가 겹치면 무효화
                    !(cacheTo < from || cacheFrom > to)
                } catch (e: Exception) {
                    false
                }
            } else {
                false
            }
        }

        // 월 캐시는 전체 무효화 (간단하게)
        monthCache.clear()
    }

    /**
     * 만료된 캐시만 제거
     */
    fun cleanExpiredCache() {
        statusCache.entries.removeIf { it.value.isExpired() }
        cyclesCache.entries.removeIf { it.value.isExpired() }
        monthCache.entries.removeIf { it.value.isExpired() }
    }

    /**
     * 캐시 통계
     */
    fun getCacheStats(): CacheStats {
        return CacheStats(
            statusCacheSize = statusCache.size,
            cyclesCacheSize = cyclesCache.size,
            monthCacheSize = monthCache.size,
            totalSize = statusCache.size + cyclesCache.size + monthCache.size
        )
    }

    // 캐시 데이터 클래스들
    private data class CachedStatus(
        val result: CalendarStatus,
        val timestamp: Long
    ) {
        fun isExpired(): Boolean {
            val now = Clock.System.now().toEpochMilliseconds()
            return now - timestamp > cacheTtl
        }
    }

    private data class CachedCycles(
        val result: List<PeriodCycle>,
        val timestamp: Long
    ) {
        fun isExpired(): Boolean {
            val now = Clock.System.now().toEpochMilliseconds()
            return now - timestamp > cacheTtl
        }
    }

    private data class CachedMonth(
        val result: MonthData,
        val timestamp: Long
    ) {
        fun isExpired(): Boolean {
            val now = Clock.System.now().toEpochMilliseconds()
            return now - timestamp > cacheTtl
        }
    }
}

/**
 * 캐시 통계 정보
 */
data class CacheStats(
    val statusCacheSize: Int,
    val cyclesCacheSize: Int,
    val monthCacheSize: Int,
    val totalSize: Int
)
