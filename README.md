# Period Calculator KMP

생리 주기, 배란일, 가임기를 계산하는 Kotlin Multiplatform 라이브러리

## 지원 플랫폼

- **Android** (`.aar`)
- **iOS** (`.framework`)
- **JavaScript/Node.js** (`.js` + `.d.ts`)

## 주요 기능

### 1. 생리 예정일 계산
마지막 생리 시작일과 평균 주기로 다음 생리일 계산

### 2. 배란일 계산
생리 예정일 14일 전을 배란일로 계산

### 3. 가임기 계산
배란일 -5일 ~ 배란일을 가임기로 계산

### 4. 배란 테스트 결과 분석
배란 테스트 양성 결과를 기반으로 배란일 추정

### 5. 피임약 고려
피임약 복용 중일 경우 배란 억제 반영

### 6. 종합 배란일 추정
사용자 입력, 배란 테스트, 계산값을 우선순위에 따라 종합

## 빌드 방법

```bash
# Android AAR 빌드
./gradlew :shared:assembleRelease

# iOS Framework 빌드
./gradlew :shared:linkReleaseFrameworkIosArm64

# JavaScript 빌드
./gradlew :shared:jsNodeProductionLibraryDistribution
```

## 테스트 데이터

`test-data.md` 파일에 실제 사용 시나리오 기반 테스트 케이스가 정의되어 있습니다.

## 사용 예시

### Kotlin (Android)
```kotlin
val nextPeriod = PeriodCalculator.calculateNextPeriod(
    lastPeriodStartDate = LocalDate(2025, 1, 5),
    averageCycleLength = 30
)
// 결과: 2025-02-04
```

### Swift (iOS)
```swift
let calculator = PeriodCalculator()
let nextPeriod = calculator.calculateNextPeriod(
    lastPeriodStartDate: LocalDate(year: 2025, month: 1, day: 5),
    averageCycleLength: 30
)
```

### TypeScript (Node.js)
```typescript
import { calculateNextPeriod } from '@bomcomes/period-calculator';

const nextPeriod = calculateNextPeriod("2025-01-05", 30);
// 결과: "2025-02-04"
```

## 개발 환경

- Kotlin: 1.9.22
- Gradle: 8.5+
- kotlinx-datetime: 0.5.0

## 라이센스

MIT
