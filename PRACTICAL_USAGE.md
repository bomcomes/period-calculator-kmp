## 실전 사용 가이드

## 개요

KMP 라이브러리를 실제 앱에서 사용하는 방법을 단계별로 설명합니다.

```
1. Repository 구현 (DB 접근)
2. Service 초기화
3. ViewModel에서 사용
4. UI에서 표시
```

## 1단계: Repository 구현

### Android (Room DB) 예제

```kotlin
// app/src/main/java/repository/

// Room Database
@Database(entities = [PeriodEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun periodDao(): PeriodDao
}

// DAO
@Dao
interface PeriodDao {
    @Query("SELECT * FROM periods WHERE startDate >= :from AND endDate <= :to")
    suspend fun getPeriods(from: Long, to: Long): List<PeriodEntity>
    
    @Query("SELECT * FROM periods ORDER BY startDate DESC LIMIT 1")
    suspend fun getLatestPeriod(): PeriodEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(period: PeriodEntity)
}

// Entity
@Entity(tableName = "periods")
data class PeriodEntity(
    @PrimaryKey val pk: String = UUID.randomUUID().toString(),
    val startDate: Long,  // EpochDays
    val endDate: Long
)

// Repository 구현
class AndroidPeriodRepository(
    private val dao: PeriodDao
) : PeriodRepository {
    
    override suspend fun getPeriods(from: LocalDate, to: LocalDate): List<PeriodRecord> {
        return dao.getPeriods(from.toEpochDays(), to.toEpochDays())
            .map { it.toModel() }
    }
    
    override suspend fun getLatestPeriod(): PeriodRecord? {
        return dao.getLatestPeriod()?.toModel()
    }
    
    override suspend fun savePeriod(period: PeriodRecord) {
        dao.insert(period.toEntity())
    }
    
    override suspend fun deletePeriod(pk: String) {
        // 구현
    }
}

// 변환 함수
fun PeriodEntity.toModel() = PeriodRecord(
    pk = pk,
    startDate = LocalDate.fromEpochDays(startDate.toInt()),
    endDate = LocalDate.fromEpochDays(endDate.toInt())
)

fun PeriodRecord.toEntity() = PeriodEntity(
    pk = pk.ifEmpty { UUID.randomUUID().toString() },
    startDate = startDate.toEpochDays(),
    endDate = endDate.toEpochDays()
)
```

### 간단한 In-Memory Repository (테스트/개발용)

```kotlin
/**
 * 메모리 기반 Repository (테스트, 프로토타입용)
 */
class InMemoryPeriodRepository : PeriodRepository {
    private val periods = mutableListOf<PeriodRecord>()
    
    override suspend fun getPeriods(from: LocalDate, to: LocalDate): List<PeriodRecord> {
        return periods.filter { period ->
            period.startDate >= from && period.endDate <= to
        }
    }
    
    override suspend fun getLatestPeriod(): PeriodRecord? {
        return periods.maxByOrNull { it.startDate }
    }
    
    override suspend fun savePeriod(period: PeriodRecord) {
        periods.removeIf { it.pk == period.pk }
        periods.add(period)
    }
    
    override suspend fun deletePeriod(pk: String) {
        periods.removeIf { it.pk == pk }
    }
}

class InMemoryOvulationTestRepository : OvulationTestRepository {
    private val tests = mutableListOf<OvulationTest>()
    
    override suspend fun getTests(from: LocalDate, to: LocalDate): List<OvulationTest> {
        return tests.filter { it.date in from..to }
    }
    
    override suspend fun getPositiveTests(from: LocalDate, to: LocalDate): List<OvulationTest> {
        return tests.filter { 
            it.date in from..to && it.result == OvulationTest.TestResult.POSITIVE 
        }
    }
    
    override suspend fun saveTest(test: OvulationTest) {
        tests.removeIf { it.date == test.date }
        tests.add(test)
    }
}

class InMemoryPeriodSettingsRepository : PeriodSettingsRepository {
    private var settings = PeriodSettings()
    
    override suspend fun getSettings(): PeriodSettings = settings
    
    override suspend fun saveSettings(settings: PeriodSettings) {
        this.settings = settings
    }
}

// 나머지 Repository도 동일하게 구현...
```

## 2단계: Service 초기화

### Koin DI 사용

```kotlin
// shared/src/commonMain/kotlin/di/

val repositoryModule = module {
    // Repositories는 플랫폼별로 주입
}

val serviceModule = module {
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
    
    single {
        CachedPeriodCalculatorService(
            service = get(),
            cacheTtl = 300_000  // 5분
        )
    }
}

// Android Module
val androidModule = module {
    single<PeriodRepository> {
        AndroidPeriodRepository(get())
    }
    single { Room.databaseBuilder(get(), AppDatabase::class.java, "period-db").build() }
    single { get<AppDatabase>().periodDao() }
}

// App.kt
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        startKoin {
            androidContext(this@MyApplication)
            modules(repositoryModule, serviceModule, androidModule)
        }
    }
}
```

### 수동 초기화 (DI 없이)

```kotlin
// 메모리 기반 Repository로 간단하게 시작
val periodRepo = InMemoryPeriodRepository()
val ovulationTestRepo = InMemoryOvulationTestRepository()
val ovulationDayRepo = InMemoryOvulationDayRepository()
val pillRepo = InMemoryPillRepository()
val pregnancyRepo = InMemoryPregnancyRepository()
val settingsRepo = InMemoryPeriodSettingsRepository()

val calculatorService = PeriodCalculatorService(
    periodRepo = periodRepo,
    ovulationTestRepo = ovulationTestRepo,
    ovulationDayRepo = ovulationDayRepo,
    pillRepo = pillRepo,
    pregnancyRepo = pregnancyRepo,
    settingsRepo = settingsRepo
)

val cachedService = CachedPeriodCalculatorService(calculatorService)
```

## 3단계: ViewModel에서 사용

### Android ViewModel

```kotlin
class CalendarViewModel(
    private val calculatorService: CachedPeriodCalculatorService
) : ViewModel() {
    
    private val _monthData = MutableStateFlow<UiState<MonthData>>(UiState.Loading)
    val monthData: StateFlow<UiState<MonthData>> = _monthData.asStateFlow()
    
    private val _todayStatus = MutableStateFlow<CalendarStatus?>(null)
    val todayStatus: StateFlow<CalendarStatus?> = _todayStatus.asStateFlow()
    
    /**
     * 특정 월의 데이터 로딩
     */
    fun loadMonth(year: Int, month: Int) {
        viewModelScope.launch {
            try {
                _monthData.value = UiState.Loading
                
                val data = calculatorService.getMonthData(year, month)
                
                _monthData.value = UiState.Success(data)
            } catch (e: Exception) {
                _monthData.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    /**
     * 오늘 상태 로딩
     */
    fun loadTodayStatus() {
        viewModelScope.launch {
            try {
                val today = Clock.System.now().toLocalDateTime(TimeZone.UTC).date
                val status = calculatorService.getStatus(today)
                _todayStatus.value = status
            } catch (e: Exception) {
                // 에러 처리
            }
        }
    }
    
    /**
     * 생리 기록 추가 후 캐시 무효화
     */
    fun addPeriod(period: PeriodRecord) {
        viewModelScope.launch {
            // DB에 저장
            periodRepository.savePeriod(period)
            
            // 캐시 무효화 (앞뒤 3개월)
            val from = period.startDate.minus(90, DateTimeUnit.DAY)
            val to = period.endDate.plus(90, DateTimeUnit.DAY)
            calculatorService.invalidateRange(from, to)
            
            // 현재 월 다시 로딩
            val today = Clock.System.now().toLocalDateTime(TimeZone.UTC).date
            loadMonth(today.year, today.monthNumber)
        }
    }
}

sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}
```

## 4단계: UI에서 표시

### Jetpack Compose (Android)

```kotlin
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = koinViewModel()
) {
    val monthData by viewModel.monthData.collectAsState()
    val todayStatus by viewModel.todayStatus.collectAsState()
    
    LaunchedEffect(Unit) {
        val today = Clock.System.now().toLocalDateTime(TimeZone.UTC).date
        viewModel.loadMonth(today.year, today.monthNumber)
        viewModel.loadTodayStatus()
    }
    
    Column {
        // 오늘 상태 위젯
        TodayStatusCard(todayStatus)
        
        // 달력
        when (val state = monthData) {
            is UiState.Loading -> CircularProgressIndicator()
            is UiState.Success -> CalendarGrid(state.data)
            is UiState.Error -> ErrorView(state.message)
        }
    }
}

@Composable
fun TodayStatusCard(status: CalendarStatus?) {
    status?.let {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("오늘의 상태", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                
                when (it.calendarType) {
                    CalendarStatus.CalendarType.THE_DAY -> {
                        Text("🔴 생리 중", style = MaterialTheme.typography.bodyLarge)
                        Text("${it.gap + 1}일차", style = MaterialTheme.typography.bodyMedium)
                    }
                    CalendarStatus.CalendarType.OVULATION_DAY -> {
                        Text("💙 배란일", style = MaterialTheme.typography.bodyLarge)
                        Text("임신 가능성 높음", style = MaterialTheme.typography.bodyMedium, color = Color.Blue)
                    }
                    CalendarStatus.CalendarType.CHILDBEARING_AGE -> {
                        Text("💚 가임기", style = MaterialTheme.typography.bodyLarge)
                        Text("임신 가능 기간", style = MaterialTheme.typography.bodyMedium, color = Color.Green)
                    }
                    CalendarStatus.CalendarType.PREDICT -> {
                        Text("🔮 생리 예정일", style = MaterialTheme.typography.bodyLarge)
                    }
                    CalendarStatus.CalendarType.DELAY -> {
                        Text("⚠️ 생리 지연", style = MaterialTheme.typography.bodyLarge)
                        Text("${it.gap}일 지연", style = MaterialTheme.typography.bodyMedium, color = Color.Red)
                    }
                    else -> {
                        Text("일반 기간", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarGrid(monthData: MonthData) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier.fillMaxWidth()
    ) {
        val from = LocalDate(monthData.year, monthData.month, 1)
        val to = from.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)
        
        var currentDate = from
        while (currentDate <= to) {
            item(key = currentDate.toString()) {
                CalendarDay(
                    date = currentDate,
                    status = monthData.getStatusForDate(currentDate)
                )
            }
            currentDate = currentDate.plus(1, DateTimeUnit.DAY)
        }
    }
}

@Composable
fun CalendarDay(date: LocalDate, status: CalendarStatus?) {
    val backgroundColor = when (status?.calendarType) {
        CalendarStatus.CalendarType.THE_DAY -> Color(0xFFFFCDD2)  // 빨강
        CalendarStatus.CalendarType.OVULATION_DAY -> Color(0xFFBBDEFB)  // 파랑
        CalendarStatus.CalendarType.CHILDBEARING_AGE -> Color(0xFFC8E6C9)  // 초록
        CalendarStatus.CalendarType.PREDICT -> Color(0xFFFFF9C4)  // 노랑
        CalendarStatus.CalendarType.DELAY -> Color(0xFFFFCCBC)  // 주황
        else -> Color.White
    }
    
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .background(backgroundColor)
            .border(1.dp, Color.LightGray),
        contentAlignment = Alignment.Center
    ) {
        Text(text = date.dayOfMonth.toString())
    }
}
```

### SwiftUI (iOS) - Kotlin Native

```swift
import shared

class CalendarViewModel: ObservableObject {
    @Published var monthData: MonthData?
    @Published var todayStatus: CalendarStatus?
    
    private let service: CachedPeriodCalculatorService
    
    init(service: CachedPeriodCalculatorService) {
        self.service = service
    }
    
    func loadMonth(year: Int32, month: Int32) {
        service.getMonthData(year: year, month: month) { data, error in
            if let data = data {
                DispatchQueue.main.async {
                    self.monthData = data
                }
            }
        }
    }
    
    func loadTodayStatus() {
        let today = Clock.Companion.shared.System.now().toLocalDateTime(timeZone: TimeZone.UTC).date
        service.getStatus(date: today) { status, error in
            if let status = status {
                DispatchQueue.main.async {
                    self.todayStatus = status
                }
            }
        }
    }
}

struct CalendarView: View {
    @StateObject var viewModel: CalendarViewModel
    
    var body: some View {
        VStack {
            if let status = viewModel.todayStatus {
                TodayStatusCard(status: status)
            }
            
            if let data = viewModel.monthData {
                CalendarGridView(monthData: data)
            }
        }
        .onAppear {
            let today = Clock.Companion.shared.System.now()
            viewModel.loadMonth(year: today.year, month: today.monthNumber)
            viewModel.loadTodayStatus()
        }
    }
}
```

## 5단계: 실전 시나리오

### 시나리오 1: 생리 기록 앱

```kotlin
class PeriodTrackerApp {
    private val service: CachedPeriodCalculatorService
    private val periodRepo: PeriodRepository
    
    /**
     * 생리 시작 기록
     */
    suspend fun startPeriod(date: LocalDate) {
        val period = PeriodRecord(
            pk = UUID.randomUUID().toString(),
            startDate = date,
            endDate = date  // 일단 시작일만
        )
        
        periodRepo.savePeriod(period)
        
        // 캐시 무효화
        service.invalidateRange(
            from = date.minus(90, DateTimeUnit.DAY),
            to = date.plus(90, DateTimeUnit.DAY)
        )
    }
    
    /**
     * 생리 종료 기록
     */
    suspend fun endPeriod(pk: String, endDate: LocalDate) {
        val period = periodRepo.getPeriods(
            from = endDate.minus(30, DateTimeUnit.DAY),
            to = endDate
        ).find { it.pk == pk } ?: return
        
        val updated = period.copy(endDate = endDate)
        periodRepo.savePeriod(updated)
        
        // 캐시 무효화
        service.invalidateRange(
            from = period.startDate.minus(90, DateTimeUnit.DAY),
            to = endDate.plus(90, DateTimeUnit.DAY)
        )
    }
    
    /**
     * 다음 생리 예정일 알림 설정
     */
    suspend fun scheduleNextPeriodNotification() {
        val nextPeriod = service.getNextPeriodDate()
        nextPeriod?.let {
            // 푸시 알림 스케줄링
            scheduleNotification(
                date = it.startDate,
                title = "생리 예정일",
                message = "내일부터 생리 예정입니다"
            )
        }
    }
}
```

### 시나리오 2: 임신 준비 앱

```kotlin
class FertilityTrackerApp {
    private val service: CachedPeriodCalculatorService
    
    /**
     * 오늘 가임기인지 확인
     */
    suspend fun isTodayFertile(): Boolean {
        val today = Clock.System.now().toLocalDateTime(TimeZone.UTC).date
        val status = service.getStatus(today)
        
        return status.calendarType == CalendarStatus.CalendarType.CHILDBEARING_AGE ||
               status.calendarType == CalendarStatus.CalendarType.OVULATION_DAY
    }
    
    /**
     * 이번 주기의 가임기 목록
     */
    suspend fun getThisMonthFertileDays(): List<LocalDate> {
        val today = Clock.System.now().toLocalDateTime(TimeZone.UTC).date
        val monthData = service.getMonthData(today.year, today.monthNumber)
        
        return monthData.getFertileDates()
    }
    
    /**
     * 배란 테스트 결과 기록
     */
    suspend fun recordOvulationTest(date: LocalDate, result: OvulationTest.TestResult) {
        ovulationTestRepo.saveTest(
            OvulationTest(date = date, result = result)
        )
        
        // 양성이면 알림
        if (result == OvulationTest.TestResult.POSITIVE) {
            notify("배란일입니다! 임신 확률이 가장 높은 기간입니다.")
        }
        
        service.invalidateAll()
    }
}
```

## 요약

### 데이터 흐름

```
User Action
    ↓
ViewModel.addPeriod()
    ↓
Repository.savePeriod() (DB 저장)
    ↓
CachedService.invalidateRange() (캐시 무효화)
    ↓
ViewModel.loadMonth() (데이터 다시 로딩)
    ↓
Service.getMonthData()
    ↓
Service.buildInput() (Repository에서 데이터 수집)
    ↓
Calculator.calculateMenstrualCycles() (순수 계산)
    ↓
UI 업데이트
```

### 핵심 포인트

1. **Repository**: DB 접근 담당 (플랫폼별 구현)
2. **Service**: Repository → Calculator 브릿지
3. **CachedService**: 성능 최적화 (캐싱)
4. **ViewModel**: UI 상태 관리
5. **UI**: 데이터 표시

이제 완전히 분리된 아키텍처로 모든 플랫폼에서 동일한 계산 로직을 사용할 수 있습니다! 🎉
