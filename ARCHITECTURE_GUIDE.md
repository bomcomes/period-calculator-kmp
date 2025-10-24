# 아키텍처 가이드: DB와 계산 로직 분리

## 문제점: 기존 iOS 구조

```swift
// iOS - DB에 직접 의존
class PredictCalculatorUseCase {
    private let periodUseCase: PeriodUseCaseProtocol  // DB 접근
    private let ovulationDayUseCase: OvulationDayUseCaseProtocol
    private let pillPackageUseCase: PillPackageUseCaseProtocol
    // ...
    
    func status(dateTimezoneZero: Date) -> (...) {
        // DB에서 직접 읽음
        let periods = periodUseCase.readItems(with: predicate)
        let ovulationTests = ovulationTestResultUseCase.readPositiveItems(...)
        // ...
    }
}
```

**문제:**
- 계산 로직과 DB 접근이 강하게 결합
- 테스트하기 어려움
- 플랫폼 독립적이지 않음

## 해결책: Repository Pattern

### 아키텍처 구조

```
┌─────────────────────────────────────────────────────────┐
│                    Platform Layer                        │
│  (Android Activity, iOS ViewController, Web Component)  │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────┐
│                  ViewModel / Presenter                   │
│              (플랫폼별 구현 또는 공통)                      │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────┐
│              PeriodCalculatorService                     │
│          (비즈니스 로직 조합, 캐싱, 최적화)                  │
└─────┬────────────────┬────────────────────────┬─────────┘
      │                │                        │
      │                │                        │
┌─────▼────────┐ ┌────▼──────────┐ ┌──────────▼──────────┐
│ Repository   │ │ KMP Calculator │ │   Cache Manager    │
│ (DB 접근)     │ │   (순수 계산)   │ │   (메모리 캐시)      │
└──────────────┘ └────────────────┘ └────────────────────┘
```

## 1. Repository 인터페이스 (공통)

```kotlin
// shared/src/commonMain/kotlin/repository/

/**
 * 생리 주기 데이터 Repository 인터페이스
 */
interface PeriodRepository {
    suspend fun getPeriods(from: LocalDate, to: LocalDate): List<PeriodRecord>
    suspend fun getLatestPeriod(): PeriodRecord?
    suspend fun savePeriod(period: PeriodRecord)
}

interface OvulationTestRepository {
    suspend fun getTests(from: LocalDate, to: LocalDate): List<OvulationTest>
    suspend fun getPositiveTests(from: LocalDate, to: LocalDate): List<OvulationTest>
}

interface OvulationDayRepository {
    suspend fun getOvulationDays(from: LocalDate, to: LocalDate): List<OvulationDay>
}

interface PillRepository {
    suspend fun getPillPackages(from: LocalDate): List<PillPackage>
    suspend fun getPillSettings(): PillSettings
}

interface PregnancyRepository {
    suspend fun getActivePregnancy(): PregnancyInfo?
    suspend fun getPregnancy(at: LocalDate): PregnancyInfo?
}

interface PeriodSettingsRepository {
    suspend fun getSettings(): PeriodSettings
    suspend fun saveSettings(settings: PeriodSettings)
}
```

## 2. Service 레이어 (공통)

```kotlin
// shared/src/commonMain/kotlin/service/

/**
 * 생리 주기 계산 서비스
 * Repository에서 데이터를 가져와 Calculator에 전달
 */
class PeriodCalculatorService(
    private val periodRepo: PeriodRepository,
    private val ovulationTestRepo: OvulationTestRepository,
    private val ovulationDayRepo: OvulationDayRepository,
    private val pillRepo: PillRepository,
    private val pregnancyRepo: PregnancyRepository,
    private val settingsRepo: PeriodSettingsRepository
) {
    
    /**
     * 특정 날짜의 상태 계산
     */
    suspend fun getStatus(date: LocalDate): CalendarStatus {
        // 1. Repository에서 필요한 데이터 수집
        val input = buildInput(
            from = date.minus(90, DateTimeUnit.DAY),  // 최근 3개월
            to = date.plus(90, DateTimeUnit.DAY)
        )
        
        // 2. 계산 로직에 전달
        return PeriodCalculatorV2.calculateStatus(input, date)
    }
    
    /**
     * 기간 내의 주기 정보 계산
     */
    suspend fun getMenstrualCycles(
        from: LocalDate,
        to: LocalDate
    ): List<PeriodCycle> {
        val input = buildInput(from, to)
        return PeriodCalculatorV2.calculateMenstrualCycles(input, from, to)
    }
    
    /**
     * Repository에서 데이터를 모아서 PeriodCycleInput 생성
     */
    private suspend fun buildInput(
        from: LocalDate,
        to: LocalDate
    ): PeriodCycleInput {
        // 병렬로 데이터 로딩 (코루틴 활용)
        return coroutineScope {
            val periodsDeferred = async { periodRepo.getPeriods(from, to) }
            val ovulationTestsDeferred = async { ovulationTestRepo.getTests(from, to) }
            val ovulationDaysDeferred = async { ovulationDayRepo.getOvulationDays(from, to) }
            val pillPackagesDeferred = async { pillRepo.getPillPackages(from) }
            val pillSettingsDeferred = async { pillRepo.getPillSettings() }
            val pregnancyDeferred = async { pregnancyRepo.getActivePregnancy() }
            val settingsDeferred = async { settingsRepo.getSettings() }
            
            PeriodCycleInput(
                periods = periodsDeferred.await(),
                periodSettings = settingsDeferred.await(),
                ovulationTests = ovulationTestsDeferred.await(),
                userOvulationDays = ovulationDaysDeferred.await(),
                pillPackages = pillPackagesDeferred.await(),
                pillSettings = pillSettingsDeferred.await(),
                pregnancy = pregnancyDeferred.await()
            )
        }
    }
}
```

## 3. 플랫폼별 Repository 구현

### Android (Room DB)

```kotlin
// androidMain/kotlin/repository/

class AndroidPeriodRepository(
    private val periodDao: PeriodDao
) : PeriodRepository {
    
    override suspend fun getPeriods(from: LocalDate, to: LocalDate): List<PeriodRecord> {
        return periodDao.getPeriods(from.toEpochDays(), to.toEpochDays())
            .map { it.toModel() }
    }
    
    override suspend fun getLatestPeriod(): PeriodRecord? {
        return periodDao.getLatestPeriod()?.toModel()
    }
    
    override suspend fun savePeriod(period: PeriodRecord) {
        periodDao.insert(period.toEntity())
    }
}

// Room Entity
@Entity(tableName = "periods")
data class PeriodEntity(
    @PrimaryKey val pk: String,
    val startDate: Long,  // EpochDays
    val endDate: Long
)

// 변환 함수
fun PeriodEntity.toModel() = PeriodRecord(
    pk = pk,
    startDate = LocalDate.fromEpochDays(startDate.toInt()),
    endDate = LocalDate.fromEpochDays(endDate.toInt())
)

fun PeriodRecord.toEntity() = PeriodEntity(
    pk = pk,
    startDate = startDate.toEpochDays(),
    endDate = endDate.toEpochDays()
)
```

### iOS (Realm)

```swift
// iosMain/kotlin/ (Kotlin Native)

class IosPeriodRepository(
    private val periodUseCase: PeriodUseCaseProtocol
) : PeriodRepository {
    
    override suspend fun getPeriods(from: LocalDate, to: LocalDate): List<PeriodRecord> {
        return withContext(Dispatchers.Main) {
            val predicate = NSPredicate(
                format: "theDayStartDate <= %@ AND theDayEndDate >= %@",
                to.toNSDate(), from.toNSDate()
            )
            periodUseCase.readItems(with: predicate).map { it.toModel() }
        }
    }
    
    // ...
}
```

### Web (IndexedDB / LocalStorage)

```kotlin
// jsMain/kotlin/repository/

class JsPeriodRepository : PeriodRepository {
    
    override suspend fun getPeriods(from: LocalDate, to: LocalDate): List<PeriodRecord> {
        // IndexedDB 또는 API 호출
        return fetchFromIndexedDB("periods", from, to)
    }
}
```

## 4. 캐싱 전략

```kotlin
/**
 * 계산 결과 캐싱 서비스
 */
class CachedPeriodCalculatorService(
    private val service: PeriodCalculatorService
) {
    private val cache = mutableMapOf<String, CachedResult>()
    
    data class CachedResult(
        val result: CalendarStatus,
        val timestamp: Long,
        val ttl: Long = 60_000  // 1분
    )
    
    suspend fun getStatus(date: LocalDate): CalendarStatus {
        val key = "status_${date}"
        val cached = cache[key]
        
        // 캐시가 유효하면 반환
        if (cached != null && !cached.isExpired()) {
            return cached.result
        }
        
        // 캐시 없거나 만료되면 새로 계산
        val result = service.getStatus(date)
        cache[key] = CachedResult(
            result = result,
            timestamp = Clock.System.now().toEpochMilliseconds()
        )
        
        return result
    }
    
    fun invalidateCache() {
        cache.clear()
    }
    
    private fun CachedResult.isExpired(): Boolean {
        val now = Clock.System.now().toEpochMilliseconds()
        return now - timestamp > ttl
    }
}
```

## 5. ViewModel 예제 (Android)

```kotlin
class CalendarViewModel(
    private val calculatorService: CachedPeriodCalculatorService
) : ViewModel() {
    
    private val _calendarState = MutableStateFlow<CalendarState>(CalendarState.Loading)
    val calendarState: StateFlow<CalendarState> = _calendarState.asStateFlow()
    
    fun loadMonth(year: Int, month: Int) {
        viewModelScope.launch {
            try {
                val from = LocalDate(year, month, 1)
                val to = from.plus(1, DateTimeUnit.MONTH)
                
                val cycles = calculatorService.getMenstrualCycles(from, to)
                
                _calendarState.value = CalendarState.Success(
                    cycles = cycles,
                    month = month
                )
            } catch (e: Exception) {
                _calendarState.value = CalendarState.Error(e.message)
            }
        }
    }
    
    fun getDateStatus(date: LocalDate) = viewModelScope.launch {
        val status = calculatorService.getStatus(date)
        // UI 업데이트
    }
}

sealed class CalendarState {
    object Loading : CalendarState()
    data class Success(val cycles: List<PeriodCycle>, val month: Int) : CalendarState()
    data class Error(val message: String?) : CalendarState()
}
```

## 6. 의존성 주입 (Koin 예제)

```kotlin
// shared/src/commonMain/kotlin/di/

val calculatorModule = module {
    // Repositories (플랫폼별 구현)
    single<PeriodRepository> { 
        // 플랫폼에서 주입
    }
    single<OvulationTestRepository> { /* ... */ }
    single<OvulationDayRepository> { /* ... */ }
    single<PillRepository> { /* ... */ }
    single<PregnancyRepository> { /* ... */ }
    single<PeriodSettingsRepository> { /* ... */ }
    
    // Service
    single {
        PeriodCalculatorService(
            periodRepo = get(),
            ovulationTestRepo = get(),
            ovulationDayRepo = get(),
            pillRepo = get(),
            pregnancyRepo = get(),
            settingsRepo = get()
        )
    }
    
    // Cached Service
    single {
        CachedPeriodCalculatorService(
            service = get()
        )
    }
}

// Android
val androidModule = module {
    single<PeriodRepository> {
        AndroidPeriodRepository(get())
    }
    single { Room.databaseBuilder(...).build() }
    single { get<AppDatabase>().periodDao() }
}

// iOS
val iosModule = module {
    single<PeriodRepository> {
        IosPeriodRepository(get())
    }
}
```

## 7. 실전 사용 예제

### 달력 화면 (Android Jetpack Compose)

```kotlin
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = koinViewModel()
) {
    val state by viewModel.calendarState.collectAsState()
    
    when (val s = state) {
        is CalendarState.Loading -> CircularProgressIndicator()
        is CalendarState.Success -> {
            CalendarGrid(cycles = s.cycles) { date ->
                viewModel.getDateStatus(date)
            }
        }
        is CalendarState.Error -> ErrorView(s.message)
    }
}

@Composable
fun CalendarGrid(cycles: List<PeriodCycle>, onDateClick: (LocalDate) -> Unit) {
    // 주기 정보로 달력 렌더링
    cycles.forEach { cycle ->
        // 생리일 표시
        cycle.theDay?.let { theDay ->
            DateCell(theDay, color = Color.Red)
        }
        
        // 배란일 표시
        cycle.ovulationDays.forEach { ovulation ->
            DateCell(ovulation, color = Color.Blue)
        }
        
        // 가임기 표시
        cycle.childbearingAges.forEach { fertile ->
            DateCell(fertile, color = Color.Green)
        }
    }
}
```

### 오늘 상태 위젯

```kotlin
@Composable
fun TodayStatusWidget(
    viewModel: CalendarViewModel = koinViewModel()
) {
    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.UTC).date }
    val status = remember { viewModel.getDateStatus(today) }
    
    Card {
        Column {
            Text("오늘의 상태")
            when (status.calendarType) {
                CalendarType.THE_DAY -> Text("생리 중 ${status.gap}일차")
                CalendarType.OVULATION_DAY -> Text("배란일 (임신 가능성 높음)")
                CalendarType.CHILDBEARING_AGE -> Text("가임기")
                CalendarType.PREDICT -> Text("생리 예정일")
                else -> Text("일반 기간")
            }
        }
    }
}
```

## 8. 최적화 전략

### 배치 로딩

```kotlin
class OptimizedPeriodCalculatorService(
    private val service: PeriodCalculatorService
) {
    // 한 달치 데이터를 한 번에 로딩하고 캐싱
    suspend fun loadMonthData(year: Int, month: Int): MonthData {
        val from = LocalDate(year, month, 1)
        val to = from.plus(1, DateTimeUnit.MONTH)
        
        val cycles = service.getMenstrualCycles(from, to)
        
        // 각 날짜의 상태를 미리 계산
        val dailyStatus = (from..to).associate { date ->
            date to service.getStatus(date)
        }
        
        return MonthData(cycles, dailyStatus)
    }
}

data class MonthData(
    val cycles: List<PeriodCycle>,
    val dailyStatus: Map<LocalDate, CalendarStatus>
)
```

### 증분 업데이트

```kotlin
class IncrementalUpdateService(
    private val service: PeriodCalculatorService,
    private val cache: MutableMap<LocalDate, CalendarStatus> = mutableMapOf()
) {
    
    // 데이터 변경 시 영향받는 날짜만 다시 계산
    suspend fun onPeriodAdded(period: PeriodRecord) {
        // 추가된 생리일 전후 3개월만 무효화
        val from = period.startDate.minus(90, DateTimeUnit.DAY)
        val to = period.endDate.plus(90, DateTimeUnit.DAY)
        
        invalidateRange(from, to)
    }
    
    private fun invalidateRange(from: LocalDate, to: LocalDate) {
        cache.keys.removeIf { it in from..to }
    }
}
```

## 요약

### Before (iOS - Monolithic)
```
UI → UseCase (DB 포함) → DB
```

### After (KMP - Layered)
```
UI → ViewModel → Service → Repository → DB
                         ↓
                   Calculator (순수 로직)
```

### 장점
1. ✅ **플랫폼 독립성**: Calculator는 순수 계산만
2. ✅ **테스트 용이**: Repository 목킹 가능
3. ✅ **재사용성**: Service는 모든 플랫폼 공통
4. ✅ **유연성**: DB 변경해도 Calculator 영향 없음
5. ✅ **성능**: 캐싱, 배치 로딩 등 최적화 가능

### 다음 단계
1. Repository 인터페이스 구현
2. Service 레이어 구현
3. 플랫폼별 DI 설정
4. 기존 iOS 코드를 새 구조로 마이그레이션
