# Repository 패턴 사용 가이드

## 개요

KMP 라이브러리는 **Repository 패턴**을 지원하여 각 플랫폼이 자신의 로컬 DB에서 데이터를 가져올 수 있도록 합니다.

```
┌─────────────────────────┐
│  iOS (Realm)            │ ─┐
└─────────────────────────┘  │
                             │
┌─────────────────────────┐  │  PeriodDataRepository
│  Android (Room)         │ ─┤  인터페이스 구현
└─────────────────────────┘  │
                             │
┌─────────────────────────┐  │
│  Firebase (Firestore)   │ ─┘
└─────────────────────────┘

         ↓ Repository 전달

┌─────────────────────────┐
│  KMP Library            │
│  (계산 로직)             │
└─────────────────────────┘
```

---

## Repository 인터페이스

KMP 라이브러리는 `PeriodDataRepository` 인터페이스를 제공합니다:

```kotlin
interface PeriodDataRepository {
    suspend fun getPeriods(fromDate: LocalDate, toDate: LocalDate): List<PeriodRecord>
    suspend fun getPeriodSettings(): PeriodSettings
    suspend fun getOvulationTests(fromDate: LocalDate, toDate: LocalDate): List<OvulationTest>
    suspend fun getUserOvulationDays(fromDate: LocalDate, toDate: LocalDate): List<OvulationDay>
    suspend fun getPillPackages(): List<PillPackage>
    suspend fun getPillSettings(): PillSettings
    suspend fun getActivePregnancy(): PregnancyInfo?
}
```

각 플랫폼은 이 인터페이스를 구현하여 자신의 로컬 DB에서 데이터를 가져옵니다.

---

## 사용 방법

### 기본 사용법

```kotlin
// Repository를 구현한 객체 생성
val repository: PeriodDataRepository = MyPeriodRepository()

// KMP 라이브러리 호출 (Repository 전달)
val result = PeriodCalculator.calculateMenstrualCycles(
    repository = repository,
    fromDate = LocalDate(2025, 1, 1),
    toDate = LocalDate(2025, 2, 28)
)
```

---

## 플랫폼별 구현 예시

### 1. iOS (Realm)

```swift
class RealmPeriodRepository: PeriodDataRepository {
    private let realm: Realm
    
    func getPeriods(fromDate: LocalDate, toDate: LocalDate) async throws -> [PeriodRecord] {
        let from = fromDate.toDate()
        let to = toDate.toDate()
        
        let results = realm.objects(PeriodRealmObject.self)
            .filter("startDate >= %@ AND startDate <= %@", from, to)
        
        return results.map { realmObj in
            PeriodRecord(
                pk: realmObj.id,
                startDate: LocalDate(date: realmObj.startDate),
                endDate: LocalDate(date: realmObj.endDate)
            )
        }
    }
    
    // ... 나머지 메서드 구현
}

// 사용
let repository = try! RealmPeriodRepository()
let result = try await PeriodCalculator.shared.calculateMenstrualCycles(
    repository: repository,
    fromDate: fromDate,
    toDate: toDate
)
```

**전체 구현 예시**: [examples/RealmPeriodRepository.swift](../examples/RealmPeriodRepository.swift)

---

### 2. Android (Room)

```kotlin
class RoomPeriodRepository(
    private val periodDao: PeriodDao,
    private val settingsDao: SettingsDao
) : PeriodDataRepository {
    
    override suspend fun getPeriods(
        fromDate: LocalDate,
        toDate: LocalDate
    ): List<PeriodRecord> {
        val from = fromDate.toJavaLocalDate()
        val to = toDate.toJavaLocalDate()
        
        val entities = periodDao.getPeriodsBetween(from, to)
        
        return entities.map { entity ->
            PeriodRecord(
                pk = entity.id,
                startDate = entity.startDate.toKotlinLocalDate(),
                endDate = entity.endDate.toKotlinLocalDate()
            )
        }
    }
    
    // ... 나머지 메서드 구현
}

// 사용
val repository = RoomPeriodRepository(periodDao, settingsDao)
val result = PeriodCalculator.calculateMenstrualCycles(
    repository = repository,
    fromDate = fromDate,
    toDate = toDate
)
```

**전체 구현 예시**: [examples/RoomPeriodRepository.kt](../examples/RoomPeriodRepository.kt)

---

### 3. Firebase Functions (Firestore)

```typescript
class FirestorePeriodRepository implements PeriodDataRepository {
    private db: Firestore;
    private userId: string;
    
    async getPeriods(fromDate: Date, toDate: Date): Promise<PeriodRecord[]> {
        const snapshot = await this.db
            .collection('users')
            .doc(this.userId)
            .collection('periods')
            .where('startDate', '>=', fromDate)
            .where('startDate', '<=', toDate)
            .get();
        
        return snapshot.docs.map(doc => ({
            pk: doc.id,
            startDate: doc.data().startDate.toDate(),
            endDate: doc.data().endDate.toDate()
        }));
    }
    
    // ... 나머지 메서드 구현
}

// 사용
const repository = new FirestorePeriodRepository(userId);
const result = await calculateMenstrualCycles(
    repository,
    fromDate,
    toDate
);
```

**전체 구현 예시**: [examples/FirestorePeriodRepository.ts](../examples/FirestorePeriodRepository.ts)

---

## 장점

### 1. 각 플랫폼이 자신의 로컬 DB 사용

```
iOS → Realm → KMP Library
Android → Room → KMP Library
Firebase → Firestore → KMP Library
```

### 2. 데이터 조립 로직 중복 제거

**Before (Repository 없이):**
```swift
// iOS에서 데이터 조립
let periods = fetchPeriods()
let settings = fetchSettings()
let ovulationTests = fetchOvulationTests()
// ... 5개 더 가져와야 함

let input = PeriodCycleInput(
    periods: periods,
    settings: settings,
    // ...
)

let result = PeriodCalculator.calculate(input: input, ...)
```

**After (Repository 사용):**
```swift
// Repository가 알아서 조립
let repository = RealmPeriodRepository()
let result = await PeriodCalculator.calculate(repository: repository, ...)
```

### 3. KMP 라이브러리는 DB에 의존하지 않음

```kotlin
// KMP는 어떤 DB인지 몰라도 됨
suspend fun calculate(repository: PeriodDataRepository) {
    val data = repository.getPeriods(...) // Realm? Room? Firestore? 몰라도 됨!
}
```

### 4. 테스트가 쉬워짐

```kotlin
// Mock Repository로 테스트
class MockPeriodRepository : PeriodDataRepository {
    override suspend fun getPeriods(...) = listOf(testPeriod1, testPeriod2)
    // ...
}

val result = PeriodCalculator.calculateMenstrualCycles(
    repository = MockPeriodRepository(),
    fromDate = testFromDate,
    toDate = testToDate
)
```

---

## 기존 방식도 계속 지원

Repository 패턴을 사용하지 않고, 데이터를 직접 전달하는 방식도 계속 지원됩니다:

```kotlin
// 기존 방식 (데이터를 직접 전달)
val input = PeriodCycleInput(
    periods = listOf(...),
    periodSettings = PeriodSettings(...),
    // ...
)

val result = PeriodCalculator.calculateMenstrualCycles(
    input = input,
    fromDate = fromDate,
    toDate = toDate
)
```

---

## 다음 단계

1. 각 플랫폼에서 Repository 구현
2. 기존 코드를 Repository 패턴으로 마이그레이션
3. 테스트 작성

---

## 참고

- **Delegate 패턴과 유사**: iOS의 UITableViewDelegate처럼 "구현체를 몰라도 호출 가능"한 패턴
- **의존성 역전 (Dependency Inversion)**: KMP가 DB에 직접 의존하지 않고 인터페이스에만 의존
- **Strategy 패턴**: 각 플랫폼이 자신만의 데이터 가져오기 전략을 구현
