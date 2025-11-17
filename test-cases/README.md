# 테스트 케이스 구조

이 폴더는 생리 주기 계산기의 테스트 케이스를 정리한 곳입니다.

## 📁 폴더 구조

### 📝 `/docs` - 테스트 케이스 문서
테스트 시나리오별로 정리된 마크다운 문서들입니다.

- **01-basic-regular-cycle.md** - 기본 규칙적/불규칙 주기
- **02-with-pill.md** - 피임약 복용 케이스
- **03-with-ovulation-test.md** - 배란 테스트 기록
- **04-with-manual-ovulation.md** - 배란일 직접 입력
- **05-edge-cases.md** - 엣지 케이스
- **06-complex-scenarios.md** - 복합 시나리오
- **README.md** - 전체 개요 및 테스트 매트릭스

### 💻 `/js-tests` - JavaScript 테스트 파일
실제 실행 가능한 JavaScript 테스트 코드들입니다.

- **01-basic-single-period.js** - 단일 생리 기록 테스트
- **02-delayed-period.js** - 생리 지연 테스트
- **03-multiple-periods.js** - 다중 생리 기록 테스트
- **04-past-search.js** - 과거 조회 테스트
- **05-long-range.js** - 긴 기간 조회 테스트
- **06-range-after-last-period.js** - 마지막 생리 이후 조회
- **07-range-between-periods.js** - 생리 사이 기간 조회
- **08-range-before-periods.js** - 생리 이전 조회
- **run-all-tests.js** - 모든 테스트 실행 스크립트

## 사용 방법

### 테스트 문서 확인
```bash
cd docs
# 원하는 테스트 시나리오 문서를 확인
```

### JavaScript 테스트 실행
```bash
cd js-tests
node run-all-tests.js  # 모든 테스트 실행
node 01-basic-single-period.js  # 개별 테스트 실행
```