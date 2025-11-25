# Claude Code 작업 가이드

## 🚨 중요 규칙

### test-cases/docs/ 디렉토리는 절대 수정 금지
- **`test-cases/docs/`** 디렉토리의 모든 파일은 **READ-ONLY**입니다
- 이 디렉토리의 파일들은 오직 **읽기만** 가능합니다
- 절대로 수정, 추가, 삭제하지 마세요

## 테스트 개발 워크플로우

### 1. 문서 우선 개발 (Documentation-First Development)

```
사용자가 테스트 케이스 문서 작성 → Claude가 문서 읽기 → Claude가 테스트 코드 생성
```

### 2. 단계별 프로세스

#### Step 1: 사용자가 테스트 케이스 문서 작성
- 위치: `test-cases/docs/XX-name.md`
- 사용자가 직접 작성하고 관리
- 테스트 케이스의 명세를 포함 (입력, 예상 결과 등)

#### Step 2: Claude가 문서 읽기
- `Read` 도구를 사용하여 테스트 문서 읽기
- 각 테스트 케이스의 요구사항 파악
- **절대로 문서를 수정하지 않음**

#### Step 3: Claude가 테스트 코드 생성
- 적절한 테스트 파일에 코드 작성
  - 예: `shared/src/commonTest/kotlin/com/bomcomes/calculator/integration/`
- 문서의 명세에 정확히 맞춰서 구현
- 테스트 실행 및 검증

#### Step 4: 반복
- 사용자가 문서를 수정하면
- Claude가 문서를 다시 읽고
- 테스트 코드를 업데이트

### 3. 예시

#### 잘못된 워크플로우 ❌
```
1. test-cases/docs/05-with-pill.md 읽기
2. 테스트 코드 작성
3. test-cases/docs/05-with-pill.md 수정  ← 절대 안 됨!
```

#### 올바른 워크플로우 ✅
```
1. test-cases/docs/05-with-pill.md 읽기 (Read only)
2. 테스트 코드 작성
3. 사용자가 문서를 업데이트하면
4. 업데이트된 문서를 다시 읽기 (Read only)
5. 테스트 코드 업데이트
```

## 테스트 케이스 구현 가이드

### WithPillTest 예시

테스트 문서 (`test-cases/docs/05-with-pill.md`)를 기반으로:
1. 각 TC-05-XX를 `testTC_05_XX` 메서드로 구현
2. 문서의 입력 데이터를 정확히 반영
3. 문서의 예상 결과를 assertion으로 검증

### 네이밍 컨벤션
- 의미 있는 상수명 사용 (예: `LAST_PERIOD_START`, `PILL_START`)
- pk 값은 간단하게 ("1", "2" 등)
- 테스트 메서드명은 TC 번호와 일치

## 현재 테스트 상태

### 완료된 테스트 스위트
- ✅ WithPillTest (10/10 tests passing)
  - TC-05-01: 기본 복용
  - TC-05-02: 휴약기 0일
  - TC-05-03: 여러 패키지
  - TC-05-04: 지연 1-7일
  - TC-05-05: 지연 8일 이상
  - TC-05-06: 5일 전 복용 (첫 복용)
  - TC-05-07: 5일 이후 복용 (첫 복용)
  - TC-05-08: 5일 전 복용 (생리 사이)
  - TC-05-09: 5일 이후 복용 (생리 사이)
  - TC-05-10: 피임약 중단

## 요약

**핵심 원칙**:
- 📖 **Read** test-cases/docs/
- ✏️ **Write** test code
- 🚫 **Never modify** test-cases/docs/
