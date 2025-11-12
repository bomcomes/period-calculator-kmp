# 빌드 가이드

## 요구사항

- JDK 11 이상
- Gradle 8.0 이상 (Wrapper 포함)

---

## 빌드 명령어

### 전체 빌드
```bash
./gradlew build
```

### JavaScript/TypeScript 빌드
```bash
./gradlew :shared:jsNodeProductionLibraryDistribution
```

결과물 위치: `shared/build/dist/js/productionLibrary/`

### JVM 빌드 (디버깅용)
```bash
./gradlew :shared:jvmJar
```

---

## 테스트 실행

### 모든 테스트
```bash
./gradlew :shared:allTests
```

### JVM 테스트만
```bash
./gradlew :shared:jvmTest
```

### JavaScript 테스트만
```bash
./gradlew :shared:jsTest
```

---

## 클린 빌드

```bash
# 빌드 캐시 삭제
./gradlew clean

# 클린 후 빌드
./gradlew clean build

# Gradle 데몬 중지
./gradlew --stop
```

---

## 빌드 결과물

### JavaScript/TypeScript
```
shared/build/dist/js/productionLibrary/
├── period-calculator-kmp-shared.js
├── period-calculator-kmp-shared.d.ts
└── package.json
```

### JVM
```
shared/build/libs/
└── shared-jvm.jar
```

---

## TypeScript 사용

### 1. 로컬 패키지 설치
```bash
cd functions  # Firebase Functions 디렉토리
npm install ../shared/build/dist/js/productionLibrary
```

### 2. Import
```typescript
import { calculateMenstrualCycles } from '@bomcomes/period-calculator';
```

---

## 트러블슈팅

### Gradle 버전 확인
```bash
./gradlew --version
```

### 디버그 모드로 빌드
```bash
./gradlew build --stacktrace --info
```

### 캐시 완전 삭제
```bash
rm -rf build shared/build .gradle
./gradlew build
```

---

## 다음 단계

- [Repository 패턴 사용법](REPOSITORY_PATTERN.md)
- [Firebase Functions 통합](FIREBASE_FUNCTIONS_GUIDE.md)
