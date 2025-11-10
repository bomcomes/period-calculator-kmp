# Firebase Functions에서 사용하기 🔥

## ✅ 빌드 완료!

JavaScript/TypeScript 라이브러리가 성공적으로 빌드되었습니다.

**빌드 결과물 위치**: `shared/build/dist/js/productionLibrary/`

## 📦 설치 방법

### 1. Firebase Functions 프로젝트에 설치

```bash
cd your-firebase-project/functions
npm install /path/to/period-calculator-kmp/shared/build/dist/js/productionLibrary
```

### 2. 의존성 확인

`package.json`에 자동으로 추가됩니다:

```json
{
  "dependencies": {
    "@bomcomes/period-calculator": "file:../path/to/period-calculator-kmp/shared/build/dist/js/productionLibrary",
    "@js-joda/core": "^5.5.3"
  }
}
```

## 🚀 사용 예제

### JavaScript (CommonJS)

```javascript
const functions = require('firebase-functions');
const admin = require('firebase-admin');
const { com } = require('@bomcomes/period-calculator');

// calculator 객체 가져오기
const calculator = com.bomcomes.calculator;

admin.initializeApp();

// 다음 생리 예정일 계산
exports.calculateNextPeriod = functions.https.onCall((data, context) => {
  const { lastPeriodStart, averageCycle } = data;
  
  const nextPeriod = calculator.calculateNextPeriod(
    lastPeriodStart,  // "2025-01-05"
    averageCycle      // 30
  );
  
  return { nextPeriod }; // "2025-02-04"
});

// 배란일 계산
exports.calculateOvulation = functions.https.onCall((data, context) => {
  const { nextPeriodDate } = data;
  
  const ovulationDate = calculator.calculateOvulationDate(nextPeriodDate);
  
  return { ovulationDate }; // "2025-01-21"
});

// 가임기 계산
exports.calculateFertileWindow = functions.https.onCall((data, context) => {
  const { ovulationDate } = data;
  
  const fertileWindow = calculator.calculateFertileWindow(ovulationDate);
  
  return {
    start: fertileWindow.start,  // "2025-01-16"
    end: fertileWindow.end        // "2025-01-21"
  };
});
```

### TypeScript

```typescript
import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';

// TypeScript에서 import
const lib = require('@bomcomes/period-calculator');
const calculator = lib.com.bomcomes.calculator;

admin.initializeApp();

// 전체 계산 함수
export const calculatePeriodInfo = functions.https.onCall(async (data, context) => {
  const { lastPeriodStart, averageCycle } = data;
  
  // 1. 다음 생리 예정일
  const nextPeriod = calculator.calculateNextPeriod(lastPeriodStart, averageCycle);
  
  // 2. 배란일
  const ovulationDate = calculator.calculateOvulationDate(nextPeriod);
  
  // 3. 가임기
  const fertileWindow = calculator.calculateFertileWindow(ovulationDate);
  
  return {
    nextPeriod,
    ovulationDate,
    fertileWindow: {
      start: fertileWindow.start,
      end: fertileWindow.end
    }
  };
});
```

### Firestore 트리거 예제

```javascript
const { com } = require('@bomcomes/period-calculator');
const calculator = com.bomcomes.calculator;

// 생리 기록 추가 시 자동으로 예정일 계산
exports.onPeriodRecordCreated = functions.firestore
  .document('users/{userId}/periods/{periodId}')
  .onCreate(async (snap, context) => {
    const periodData = snap.data();
    const { startDate } = periodData;
    
    // 기본 주기 30일로 계산 (사용자 설정이 있으면 그것 사용)
    const userDoc = await admin.firestore()
      .collection('users')
      .doc(context.params.userId)
      .get();
    
    const averageCycle = userDoc.data()?.averageCycle || 30;
    
    // 계산
    const nextPeriod = calculator.calculateNextPeriod(startDate, averageCycle);
    const ovulationDate = calculator.calculateOvulationDate(nextPeriod);
    const fertileWindow = calculator.calculateFertileWindow(ovulationDate);
    
    // Firestore에 저장
    await admin.firestore()
      .collection('users')
      .doc(context.params.userId)
      .collection('predictions')
      .add({
        nextPeriod,
        ovulationDate,
        fertileWindow: {
          start: fertileWindow.start,
          end: fertileWindow.end
        },
        createdAt: admin.firestore.FieldValue.serverTimestamp()
      });
    
    return null;
  });
```

## 📱 클라이언트에서 호출하기

### Android (Kotlin)

```kotlin
val functions = Firebase.functions
val data = hashMapOf(
    "lastPeriodStart" to "2025-01-05",
    "averageCycle" to 30
)

functions
    .getHttpsCallable("calculatePeriodInfo")
    .call(data)
    .addOnSuccessListener { result ->
        val response = result.data as Map<*, *>
        val nextPeriod = response["nextPeriod"] as String
        val ovulationDate = response["ovulationDate"] as String
        
        Log.d("Period", "다음 생리: $nextPeriod")
        Log.d("Period", "배란일: $ovulationDate")
    }
```

### iOS (Swift)

```swift
let functions = Functions.functions()
let data: [String: Any] = [
    "lastPeriodStart": "2025-01-05",
    "averageCycle": 30
]

functions.httpsCallable("calculatePeriodInfo").call(data) { result, error in
    if let error = error {
        print("Error: \(error)")
        return
    }
    
    if let data = result?.data as? [String: Any] {
        let nextPeriod = data["nextPeriod"] as? String
        let ovulationDate = data["ovulationDate"] as? String
        
        print("다음 생리: \(nextPeriod ?? "")")
        print("배란일: \(ovulationDate ?? "")")
    }
}
```

### Web (JavaScript)

```javascript
import { getFunctions, httpsCallable } from 'firebase/functions';

const functions = getFunctions();
const calculatePeriod = httpsCallable(functions, 'calculatePeriodInfo');

calculatePeriod({
  lastPeriodStart: '2025-01-05',
  averageCycle: 30
}).then((result) => {
  console.log('다음 생리:', result.data.nextPeriod);
  console.log('배란일:', result.data.ovulationDate);
  console.log('가임기:', result.data.fertileWindow);
});
```

## 🔄 재빌드 방법

코드를 수정한 후 다시 빌드하려면:

```bash
# 프로젝트 루트에서
./gradlew :shared:jsNodeProductionLibraryDistribution

# Firebase Functions에서 재설치
cd your-firebase-project/functions
npm install
```

## 📊 API 레퍼런스

### calculateNextPeriod(lastPeriodStartDate, averageCycleLength)

다음 생리 예정일을 계산합니다.

**Parameters:**
- `lastPeriodStartDate` (string): 마지막 생리 시작일 (ISO 8601 형식: "2025-01-05")
- `averageCycleLength` (number): 평균 주기 (일)

**Returns:** (string) 다음 생리 예정일 (ISO 8601 형식)

**Example:**
```javascript
const nextPeriod = calculator.calculateNextPeriod("2025-01-05", 30);
// "2025-02-04"
```

### calculateOvulationDate(nextPeriodDate)

배란일을 계산합니다 (생리 예정일 14일 전).

**Parameters:**
- `nextPeriodDate` (string): 다음 생리 예정일 (ISO 8601 형식)

**Returns:** (string) 배란일 (ISO 8601 형식)

**Example:**
```javascript
const ovulationDate = calculator.calculateOvulationDate("2025-02-04");
// "2025-01-21"
```

### calculateFertileWindow(ovulationDate)

가임기를 계산합니다 (배란일 -5일 ~ 배란일).

**Parameters:**
- `ovulationDate` (string): 배란일 (ISO 8601 형식)

**Returns:** (object) `{ start: string, end: string }` - 가임기 시작일과 종료일

**Example:**
```javascript
const fertileWindow = calculator.calculateFertileWindow("2025-01-21");
// { start: "2025-01-16", end: "2025-01-21" }
```

## 🧪 로컬 테스트

```bash
# 프로젝트 루트에서
node test-js.js
```

출력 예제:
```
=== Period Calculator JavaScript Test ===

1. 다음 생리 예정일 계산
   마지막 생리: 2025-01-05
   평균 주기: 30일
   ➜ 다음 생리 예정일: 2025-02-04

2. 배란일 계산
   다음 생리 예정일: 2025-02-04
   ➜ 배란일: 2025-01-21

3. 가임기 계산
   배란일: 2025-01-21
   ➜ 가임기: 2025-01-16 ~ 2025-01-21

✅ 모든 테스트 완료!
```

## 📝 주의사항

### 현재 빌드에 포함된 기능

✅ 다음 생리 예정일 계산  
✅ 배란일 계산  
✅ 가임기 계산

### 제외된 기능 (복잡한 V2 API)

❌ 피임약 계산  
❌ 배란 테스트 분석  
❌ 임신 정보 처리  
❌ 고급 주기 분석

**이유**: JavaScript 빌드 호환성 문제로 간단한 V1 API만 포함되었습니다.

Android/iOS에서는 전체 기능을 사용할 수 있습니다.

## 🔧 트러블슈팅

### 에러: Cannot find module '@js-joda/core'

```bash
cd shared/build/dist/js/productionLibrary
npm install
```

### 에러: calculator.calculateNextPeriod is not a function

올바른 import 경로를 사용하세요:

```javascript
// ❌ 잘못됨
const calculator = require('@bomcomes/period-calculator');

// ✅ 올바름
const { com } = require('@bomcomes/period-calculator');
const calculator = com.bomcomes.calculator;
```

### TypeScript 타입 에러

현재 `.d.ts` 파일은 자동 생성되지 않습니다. 타입을 직접 정의하세요:

```typescript
interface PeriodCalculator {
  calculateNextPeriod(lastPeriodStart: string, averageCycle: number): string;
  calculateOvulationDate(nextPeriodDate: string): string;
  calculateFertileWindow(ovulationDate: string): {
    start: string;
    end: string;
  };
}
```

## 🎉 완료!

이제 Firebase Functions에서 생리 주기 계산 라이브러리를 사용할 수 있습니다!
