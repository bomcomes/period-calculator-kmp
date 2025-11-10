# 빌드 가이드

## 현재 상태

✅ Gradle Wrapper 생성 완료  
❌ 컴파일 에러 발생 (수정 필요)

## 빌드 시도 결과

JavaScript/TypeScript용 빌드를 시도했지만, 다음과 같은 컴파일 에러가 발생했습니다:

### 주요 에러

1. **PeriodSettings 타입 충돌**
   - `com.bomcomes.calculator.models.PeriodSettings`
   - `com.bomcomes.calculator.PeriodSettings`
   - 두 개의 `PeriodSettings` 클래스가 충돌

2. **removeIf 메서드 없음**
   - `CachedPeriodCalculatorService.kt`에서 `removeIf` 사용
   - JavaScript 타겟에서는 Java의 `removeIf`를 사용할 수 없음

3. **기타 타입 에러**
   - `Unresolved reference: days`
   - `plus`/`minus` 연산자 타입 미스매치

## 빌드 방법 (에러 수정 후)

### 1. Gradle Wrapper 사용 (이미 생성됨)

```bash
# JavaScript/TypeScript 빌드
./gradlew :shared:jsNodeProductionLibraryDistribution

# 결과물 위치
# shared/build/dist/js/productionLibrary/
```

### 2. 현재 설정

`shared/build.gradle.kts`가 JavaScript만 빌드하도록 설정되어 있습니다:
- Android 타겟: 주석 처리 (Android SDK 불필요)
- iOS 타겟: 주석 처리 (Xcode 불필요)
- JavaScript 타겟: 활성화

## 수정이 필요한 파일들

### 1. PeriodSettings 타입 충돌 해결

두 가지 방법:

#### 방법 A: 하나의 클래스만 사용
```kotlin
// PeriodCalculatorV2.kt 또는 Models.kt 중 하나만 사용하도록 수정
// import 경로를 명확히 지정
import com.bomcomes.calculator.models.PeriodSettings  // 또는
import com.bomcomes.calculator.PeriodSettings
```

#### 방법 B: 클래스 이름 변경
```kotlin
// 하나를 PeriodConfig로 변경
data class PeriodConfig(...)
```

### 2. removeIf 메서드 대체

`CachedPeriodCalculatorService.kt` 파일의 `removeIf` 사용을 다음과 같이 변경:

```kotlin
// 변경 전:
list.removeIf { it.condition }

// 변경 후 (KMP 호환):
list = list.filter { !it.condition }.toMutableList()
// 또는
val itemsToRemove = list.filter { it.condition }
list.removeAll(itemsToRemove)
```

### 3. 수정이 필요한 파일 목록

1. `shared/src/commonMain/kotlin/com/bomcomes/calculator/PeriodCalculatorV2.kt`
   - Line 58: PeriodSettings 타입 수정
   - Line 61, 63, 78, 93, 94, 130: plus/minus 연산자 타입 수정

2. `shared/src/commonMain/kotlin/com/bomcomes/calculator/service/CachedPeriodCalculatorService.kt`
   - Line 97, 108, 132, 133, 134: removeIf → filter + removeAll
   - Line 156, 166: cacheTtl 참조 수정

## 빠른 수정 방법

가장 간단한 방법은 **JavaScript Export 함수만 사용**하는 것입니다:

### JsExports.kt 확인

`shared/src/jsMain/kotlin/com/bomcomes/calculator/JsExports.kt` 파일에 이미 JavaScript용 함수들이 정의되어 있습니다:

```kotlin
@JsExport
@JsName("calculateNextPeriod")
fun calculateNextPeriodJs(
    lastPeriodStartDate: String,
    averageCycleLength: Int
): String

@JsName("calculateOvulationDate")
fun calculateOvulationDateJs(
    nextPeriodDate: String
): String

@JsName("calculateFertileWindow")
fun calculateFertileWindowJs(
    ovulationDate: String
): FertileWindowResult
```

이 함수들은 간단한 `PeriodCalculator` 클래스를 사용하므로, V2보다 단순하고 에러가 적을 수 있습니다.

## 대안: 간소화된 빌드

만약 V2 API의 복잡한 기능이 필요하지 않다면:

1. `PeriodCalculator.kt` (V1)만 사용
2. `JsExports.kt`의 간단한 함수만 export
3. 복잡한 V2 API는 Android/iOS 전용으로 유지

### 간소화된 build.gradle.kts 예제

```kotlin
// V2를 제외하고 V1만 컴파일하도록 설정
sourceSets {
    val commonMain by getting {
        kotlin.srcDirs("src/commonMain/kotlin")
        // V2 관련 파일 제외
        kotlin.exclude("**/PeriodCalculatorV2.kt")
        kotlin.exclude("**/service/**")
    }
}
```

## TypeScript용 사용 예제 (빌드 성공 시)

### 1. 빌드 후 파일 구조

```
shared/build/dist/js/productionLibrary/
├── period-calculator-kmp-shared.js
├── period-calculator-kmp-shared.d.ts
└── package.json (직접 생성 필요)
```

### 2. package.json 생성

```json
{
  "name": "@bomcomes/period-calculator",
  "version": "1.0.0",
  "main": "period-calculator-kmp-shared.js",
  "types": "period-calculator-kmp-shared.d.ts",
  "description": "Period Calculator for TypeScript/JavaScript",
  "keywords": ["period", "calculator", "menstrual", "cycle"],
  "license": "MIT"
}
```

### 3. Firebase Functions에서 사용

```typescript
// 로컬 패키지로 설치
// npm install /path/to/shared/build/dist/js/productionLibrary

import {
  calculateNextPeriodJs,
  calculateOvulationDateJs,
  calculateFertileWindowJs
} from '@bomcomes/period-calculator';

export const calculatePeriod = functions.https.onCall(async (data) => {
  const { lastPeriodStart, averageCycle } = data;
  
  const nextPeriod = calculateNextPeriodJs(lastPeriodStart, averageCycle);
  const ovulationDate = calculateOvulationDateJs(nextPeriod);
  const fertileWindow = calculateFertileWindowJs(ovulationDate);
  
  return {
    nextPeriod,
    ovulationDate,
    fertileWindow
  };
});
```

### 4. Node.js에서 직접 사용

```javascript
const {
  calculateNextPeriodJs,
  calculateOvulationDateJs
} = require('./period-calculator-kmp-shared.js');

const nextPeriod = calculateNextPeriodJs("2025-01-05", 30);
console.log("다음 생리:", nextPeriod); // "2025-02-04"

const ovulation = calculateOvulationDateJs(nextPeriod);
console.log("배란일:", ovulation); // "2025-01-21"
```

## 다음 단계

1. ✅ Gradle Wrapper 생성 완료
2. ❌ 컴파일 에러 수정 필요
3. ⏳ JavaScript 빌드
4. ⏳ TypeScript 타입 정의 확인
5. ⏳ NPM 패키지 생성
6. ⏳ Firebase Functions 통합 테스트

## 도움말

### 빌드 명령어

```bash
# 전체 빌드
./gradlew build

# JS만 빌드
./gradlew :shared:jsNodeProductionLibraryDistribution

# 클린 빌드
./gradlew clean build

# 디버그 모드
./gradlew :shared:jsNodeProductionLibraryDistribution --stacktrace
```

### Gradle 캐시 정리

```bash
# 빌드 캐시 삭제
./gradlew clean

# Gradle 데몬 중지
./gradlew --stop

# 완전 클린
rm -rf build shared/build .gradle
```

## 문의

코드를 수정하거나 빌드 에러를 해결하려면:
1. 위의 "수정이 필요한 파일들" 섹션 참조
2. 간단한 V1 API만 사용하는 것을 고려
3. Android/iOS는 별도 빌드 (원본 코드 유지)
