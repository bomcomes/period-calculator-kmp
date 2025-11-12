/**
 * Room Period Repository 구현 예시
 *
 * Android에서 사용할 Repository 구현체
 * Room에서 데이터를 가져와서 KMP 라이브러리에 전달
 */

package com.example.period.repository

import com.bomcomes.calculator.models.*
import com.bomcomes.calculator.repository.PeriodDataRepository
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toKotlinLocalDate
import java.time.ZoneId

// Room Entity 예시 (실제 프로젝트의 Entity에 맞게 수정 필요)
@Entity(tableName = "periods")
data class PeriodEntity(
    @PrimaryKey val id: String,
    val startDate: java.time.LocalDate,
    val endDate: java.time.LocalDate,
    val isMenstruation: Boolean = true,
    val isDeleted: Boolean = false
)

@Entity(tableName = "period_settings")
data class PeriodSettingsEntity(
    @PrimaryKey val id: String = "default",
    val period: Int = 28,
    val days: Int = 5,
    val isRegularCycle: Boolean = true
)

// Room DAO 예시
@Dao
interface PeriodDao {
    @Query("SELECT * FROM periods WHERE startDate >= :fromDate AND startDate <= :toDate AND isDeleted = 0 ORDER BY startDate DESC")
    suspend fun getPeriodsBetween(fromDate: java.time.LocalDate, toDate: java.time.LocalDate): List<PeriodEntity>
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM period_settings LIMIT 1")
    suspend fun getPeriodSettings(): PeriodSettingsEntity?
}

// ... 나머지 DAO들

/**
 * Room을 사용하는 Repository 구현체
 */
class RoomPeriodRepository(
    private val periodDao: PeriodDao,
    private val settingsDao: SettingsDao,
    // ... 나머지 DAO들
) : PeriodDataRepository {

    override suspend fun getPeriods(
        fromDate: LocalDate,
        toDate: LocalDate
    ): List<PeriodRecord> {
        // kotlinx.datetime.LocalDate를 java.time.LocalDate로 변환
        val from = fromDate.toJavaLocalDate()
        val to = toDate.toJavaLocalDate()

        // Room에서 조회
        val entities = periodDao.getPeriodsBetween(from, to)

        // KMP 모델로 변환
        return entities.map { entity ->
            PeriodRecord(
                pk = entity.id,
                startDate = entity.startDate.toKotlinLocalDate(),
                endDate = entity.endDate.toKotlinLocalDate(),
                isMenstruation = entity.isMenstruation,
                isDeleted = entity.isDeleted
            )
        }
    }

    override suspend fun getPeriodSettings(): PeriodSettings {
        // Room에서 설정 조회
        val entity = settingsDao.getPeriodSettings()

        return if (entity != null) {
            PeriodSettings(
                period = entity.period,
                days = entity.days,
                isRegularCycle = entity.isRegularCycle
            )
        } else {
            // 기본값 반환
            PeriodSettings(
                period = 28,
                days = 5,
                isRegularCycle = true
            )
        }
    }

    override suspend fun getOvulationTests(
        fromDate: LocalDate,
        toDate: LocalDate
    ): List<OvulationTest> {
        // 구현...
        return emptyList()
    }

    override suspend fun getUserOvulationDays(
        fromDate: LocalDate,
        toDate: LocalDate
    ): List<OvulationDay> {
        // 구현...
        return emptyList()
    }

    override suspend fun getPillPackages(): List<PillPackage> {
        // 구현...
        return emptyList()
    }

    override suspend fun getPillSettings(): PillSettings {
        // 구현...
        return PillSettings(
            isCalculatingWithPill = false,
            reminderEnabled = false
        )
    }

    override suspend fun getActivePregnancy(): PregnancyInfo? {
        // 구현...
        return null
    }
}

// MARK: - Helper Extensions

/**
 * kotlinx.datetime.LocalDate를 java.time.LocalDate로 변환
 */
fun LocalDate.toJavaLocalDate(): java.time.LocalDate {
    return java.time.LocalDate.of(this.year, this.monthNumber, this.dayOfMonth)
}

// MARK: - 사용 예시

class PeriodViewModel(
    private val repository: PeriodDataRepository
) : ViewModel() {

    private val _periodCycles = MutableStateFlow<List<PeriodCycle>>(emptyList())
    val periodCycles: StateFlow<List<PeriodCycle>> = _periodCycles.asStateFlow()

    fun calculatePeriod(from: LocalDate, to: LocalDate) {
        viewModelScope.launch {
            try {
                // KMP 라이브러리 호출 (Repository 전달)
                val result = PeriodCalculator.calculateMenstrualCycles(
                    repository = repository,
                    fromDate = from,
                    toDate = to
                )

                _periodCycles.value = result
            } catch (e: Exception) {
                // 에러 처리
                Log.e("PeriodViewModel", "Failed to calculate period", e)
            }
        }
    }
}

// Hilt/Koin을 사용한 DI 예시
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun providePeriodDataRepository(
        periodDao: PeriodDao,
        settingsDao: SettingsDao
    ): PeriodDataRepository {
        return RoomPeriodRepository(
            periodDao = periodDao,
            settingsDao = settingsDao
        )
    }
}
