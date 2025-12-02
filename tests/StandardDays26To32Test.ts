/**
 * 표준일 피임법 (26-32일 주기) 통합 테스트
 *
 * 적용 대상: 26-32일의 규칙적인 생리 주기
 *
 * 계산 공식:
 * - 안전기 (전반): 1-7일 (고정)
 * - 가임기: 8-19일 (고정)
 * - 배란기: 13-15일 (고정)
 * - 안전기 (후반): 20일~ (고정)
 *
 * 실패 확률: 5%
 */

const kmp = require("../shared/build/dist/js/productionLibrary/period-calculator-kmp-shared");
const {
  calculateCycleInfo,
  stringToJulianDay,
  julianDayToString,
  JsPeriodRecord,
} = kmp.com.bomcomes.calculator;
const { JsRepositoryWrapper } = kmp.com.bomcomes.calculator.repository;
const { PeriodSettings } = kmp.com.bomcomes.calculator.models;

// ============================================
// 공통 설정
// ============================================

// 생리 기록 (공통)
const ALL_PERIODS = [
  new JsPeriodRecord(
    "1",
    stringToJulianDay("2025-01-01"),
    stringToJulianDay("2025-01-05")
  ),
  new JsPeriodRecord(
    "2",
    stringToJulianDay("2025-02-01"),
    stringToJulianDay("2025-02-05")
  ),
  new JsPeriodRecord(
    "3",
    stringToJulianDay("2025-03-01"),
    stringToJulianDay("2025-03-05")
  ),
];

// 테스트 결과 추적
interface TestResult {
  name: string;
  passed: boolean;
  errors: string[];
}

const testResults: TestResult[] = [];

// ============================================
// 헬퍼 함수
// ============================================

function formatDateRange(range: any): string {
  if (!range) return "null";
  return `${julianDayToString(range.startDate)} ~ ${julianDayToString(range.endDate)}`;
}

function formatDateRanges(ranges: any[]): string {
  if (!ranges || ranges.length === 0) return "[]";
  return `[${ranges.map((r) => formatDateRange(r)).join(", ")}]`;
}

function compareDateRange(
  actual: any,
  expectedStart: string,
  expectedEnd: string
): boolean {
  if (!actual) return false;
  const actualStart = julianDayToString(actual.startDate);
  const actualEnd = julianDayToString(actual.endDate);
  return actualStart === expectedStart && actualEnd === expectedEnd;
}

function compareDateRanges(
  actual: any[],
  expected: Array<{ start: string; end: string }>
): boolean {
  if (!actual && expected.length === 0) return true;
  if (!actual || actual.length !== expected.length) return false;

  for (let i = 0; i < expected.length; i++) {
    if (!compareDateRange(actual[i], expected[i].start, expected[i].end)) {
      return false;
    }
  }
  return true;
}

// ============================================
// 테스트 케이스 정의
// ============================================

interface ExpectedCycle {
  pk: string;
  actualPeriod: { start: string; end: string } | null;
  delayDays: number;
  delayPeriod: { start: string; end: string } | null;
  predictDays: Array<{ start: string; end: string }>;
  fertileDays: Array<{ start: string; end: string }>;
  ovulationDays: Array<{ start: string; end: string }>;
  period: number;
}

interface TestCase {
  id: string;
  name: string;
  fromDate: string;
  toDate: string;
  today: string;
  expectedCycles: ExpectedCycle[];
}

const TEST_CASES: TestCase[] = [
  // 그룹 1: 마지막 생리 이후 조회 (Period 3 기준)
  {
    id: "TC-01-01",
    name: "1일 조회 (마지막 생리 이후)",
    fromDate: "2025-03-15",
    toDate: "2025-03-15",
    today: "2025-03-15",
    expectedCycles: [
      {
        pk: "3",
        actualPeriod: { start: "2025-03-01", end: "2025-03-05" },
        delayDays: 0,
        delayPeriod: null,
        predictDays: [],
        fertileDays: [{ start: "2025-03-08", end: "2025-03-19" }],
        ovulationDays: [{ start: "2025-03-13", end: "2025-03-15" }],
        period: 30,
      },
    ],
  },
  {
    id: "TC-01-02",
    name: "1주일 조회 (마지막 생리 이후)",
    fromDate: "2025-03-09",
    toDate: "2025-03-15",
    today: "2025-03-15",
    expectedCycles: [
      {
        pk: "3",
        actualPeriod: { start: "2025-03-01", end: "2025-03-05" },
        delayDays: 0,
        delayPeriod: null,
        predictDays: [],
        fertileDays: [{ start: "2025-03-08", end: "2025-03-19" }],
        ovulationDays: [{ start: "2025-03-13", end: "2025-03-15" }],
        period: 30,
      },
    ],
  },
  {
    id: "TC-01-03",
    name: "1개월 조회 (마지막 생리 이후)",
    fromDate: "2025-03-01",
    toDate: "2025-03-31",
    today: "2025-03-15",
    expectedCycles: [
      {
        pk: "3",
        actualPeriod: { start: "2025-03-01", end: "2025-03-05" },
        delayDays: 0,
        delayPeriod: null,
        predictDays: [{ start: "2025-03-31", end: "2025-04-04" }],
        fertileDays: [{ start: "2025-03-08", end: "2025-03-19" }],
        ovulationDays: [{ start: "2025-03-13", end: "2025-03-15" }],
        period: 30,
      },
    ],
  },

  // 그룹 2: 과거 기록 중심 (Period 2)
  {
    id: "TC-01-04",
    name: "1일 조회 (과거)",
    fromDate: "2025-02-07",
    toDate: "2025-02-07",
    today: "2025-03-15",
    expectedCycles: [
      {
        pk: "2",
        actualPeriod: { start: "2025-02-01", end: "2025-02-05" },
        delayDays: 0,
        delayPeriod: null,
        predictDays: [],
        fertileDays: [],
        ovulationDays: [],
        period: 28,
      },
    ],
  },
  {
    id: "TC-01-05",
    name: "1주일 조회 (과거)",
    fromDate: "2025-02-09",
    toDate: "2025-02-15",
    today: "2025-03-15",
    expectedCycles: [
      {
        pk: "2",
        actualPeriod: { start: "2025-02-01", end: "2025-02-05" },
        delayDays: 0,
        delayPeriod: null,
        predictDays: [],
        fertileDays: [{ start: "2025-02-08", end: "2025-02-19" }],
        ovulationDays: [{ start: "2025-02-13", end: "2025-02-15" }],
        period: 28,
      },
    ],
  },
  {
    id: "TC-01-06",
    name: "1개월 조회 (과거)",
    fromDate: "2025-02-01",
    toDate: "2025-02-28",
    today: "2025-03-15",
    expectedCycles: [
      {
        pk: "2",
        actualPeriod: { start: "2025-02-01", end: "2025-02-05" },
        delayDays: 0,
        delayPeriod: null,
        predictDays: [],
        fertileDays: [{ start: "2025-02-08", end: "2025-02-19" }],
        ovulationDays: [{ start: "2025-02-13", end: "2025-02-15" }],
        period: 28,
      },
    ],
  },

  // 그룹 3: 장기 조회 & 특수 구간
  {
    id: "TC-01-07",
    name: "3개월 조회",
    fromDate: "2025-03-01",
    toDate: "2025-05-31",
    today: "2025-03-15",
    expectedCycles: [
      {
        pk: "3",
        actualPeriod: { start: "2025-03-01", end: "2025-03-05" },
        delayDays: 0,
        delayPeriod: null,
        predictDays: [
          { start: "2025-03-31", end: "2025-04-04" },
          { start: "2025-04-30", end: "2025-05-04" },
          { start: "2025-05-30", end: "2025-06-03" },
        ],
        fertileDays: [
          { start: "2025-03-08", end: "2025-03-19" },
          { start: "2025-04-07", end: "2025-04-18" },
          { start: "2025-05-07", end: "2025-05-18" },
        ],
        ovulationDays: [
          { start: "2025-03-13", end: "2025-03-15" },
          { start: "2025-04-12", end: "2025-04-14" },
          { start: "2025-05-12", end: "2025-05-14" },
        ],
        period: 30,
      },
    ],
  },
  {
    id: "TC-01-08",
    name: "생리 기간 경계 조회 (전체 5개월)",
    fromDate: "2025-01-01",
    toDate: "2025-05-31",
    today: "2025-03-15",
    expectedCycles: [
      {
        pk: "1",
        actualPeriod: { start: "2025-01-01", end: "2025-01-05" },
        delayDays: 0,
        delayPeriod: null,
        predictDays: [],
        fertileDays: [{ start: "2025-01-08", end: "2025-01-19" }],
        ovulationDays: [{ start: "2025-01-13", end: "2025-01-15" }],
        period: 31,
      },
      {
        pk: "2",
        actualPeriod: { start: "2025-02-01", end: "2025-02-05" },
        delayDays: 0,
        delayPeriod: null,
        predictDays: [],
        fertileDays: [{ start: "2025-02-08", end: "2025-02-19" }],
        ovulationDays: [{ start: "2025-02-13", end: "2025-02-15" }],
        period: 28,
      },
      {
        pk: "3",
        actualPeriod: { start: "2025-03-01", end: "2025-03-05" },
        delayDays: 0,
        delayPeriod: null,
        predictDays: [
          { start: "2025-03-31", end: "2025-04-04" },
          { start: "2025-04-30", end: "2025-05-04" },
          { start: "2025-05-30", end: "2025-06-03" },
        ],
        fertileDays: [
          { start: "2025-03-08", end: "2025-03-19" },
          { start: "2025-04-07", end: "2025-04-18" },
          { start: "2025-05-07", end: "2025-05-18" },
        ],
        ovulationDays: [
          { start: "2025-03-13", end: "2025-03-15" },
          { start: "2025-04-12", end: "2025-04-14" },
          { start: "2025-05-12", end: "2025-05-14" },
        ],
        period: 30,
      },
    ],
  },
  {
    id: "TC-01-09",
    name: "생리 기간 경계 조회 (과거만)",
    fromDate: "2025-01-01",
    toDate: "2025-02-28",
    today: "2025-03-15",
    expectedCycles: [
      {
        pk: "1",
        actualPeriod: { start: "2025-01-01", end: "2025-01-05" },
        delayDays: 0,
        delayPeriod: null,
        predictDays: [],
        fertileDays: [{ start: "2025-01-08", end: "2025-01-19" }],
        ovulationDays: [{ start: "2025-01-13", end: "2025-01-15" }],
        period: 31,
      },
      {
        pk: "2",
        actualPeriod: { start: "2025-02-01", end: "2025-02-05" },
        delayDays: 0,
        delayPeriod: null,
        predictDays: [],
        fertileDays: [{ start: "2025-02-08", end: "2025-02-19" }],
        ovulationDays: [{ start: "2025-02-13", end: "2025-02-15" }],
        period: 28,
      },
    ],
  },

  // 그룹 4: 생리 지연 케이스
  {
    id: "TC-01-10",
    name: "생리 지연 1-7일 (예정일 뒤로 미룸)",
    fromDate: "2025-03-20",
    toDate: "2025-04-10",
    today: "2025-04-06",
    expectedCycles: [
      {
        pk: "3",
        actualPeriod: { start: "2025-03-01", end: "2025-03-05" },
        delayDays: 7,
        delayPeriod: { start: "2025-03-31", end: "2025-04-06" },
        predictDays: [{ start: "2025-04-07", end: "2025-04-11" }],
        fertileDays: [],
        ovulationDays: [],
        period: 30,
      },
    ],
  },
  {
    id: "TC-01-11",
    name: "생리 지연 8일 이상 (병원 권장)",
    fromDate: "2025-03-20",
    toDate: "2025-05-10",
    today: "2025-04-07",
    expectedCycles: [
      {
        pk: "3",
        actualPeriod: { start: "2025-03-01", end: "2025-03-05" },
        delayDays: 8,
        delayPeriod: { start: "2025-03-31", end: "2025-04-07" },
        predictDays: [],
        fertileDays: [],
        ovulationDays: [],
        period: 30,
      },
    ],
  },
];

// ============================================
// 테스트 실행
// ============================================

async function runTestCase(tc: TestCase): Promise<TestResult> {
  const errors: string[] = [];

  console.log("\n=====================================");
  console.log(`${tc.id}: ${tc.name}`);
  console.log("=====================================");
  console.log(`조회 기간: ${tc.fromDate} ~ ${tc.toDate}`);
  console.log(`오늘: ${tc.today}`);

  // 예상 결과 출력
  console.log("\n[예상]");
  console.log(`주기 개수: ${tc.expectedCycles.length}`);
  tc.expectedCycles.forEach((cycle, idx) => {
    console.log(`\n주기 ${idx + 1} (pk=${cycle.pk}):`);
    console.log(
      `  - 실제 생리: ${cycle.actualPeriod ? `${cycle.actualPeriod.start} ~ ${cycle.actualPeriod.end}` : "null"}`
    );
    console.log(`  - 지연 일수: ${cycle.delayDays}`);
    console.log(
      `  - 지연 기간: ${cycle.delayPeriod ? `${cycle.delayPeriod.start} ~ ${cycle.delayPeriod.end}` : "null"}`
    );
    console.log(
      `  - 생리 예정일들: ${formatDateRanges(cycle.predictDays.map((d) => ({ startDate: stringToJulianDay(d.start), endDate: stringToJulianDay(d.end) })))}`
    );
    console.log(
      `  - 가임기들: ${formatDateRanges(cycle.fertileDays.map((d) => ({ startDate: stringToJulianDay(d.start), endDate: stringToJulianDay(d.end) })))}`
    );
    console.log(
      `  - 배란기들: ${formatDateRanges(cycle.ovulationDays.map((d) => ({ startDate: stringToJulianDay(d.start), endDate: stringToJulianDay(d.end) })))}`
    );
    console.log(`  - 주기: ${cycle.period}일`);
  });

  // Repository 생성 및 데이터 설정
  const repository = new JsRepositoryWrapper();
  ALL_PERIODS.forEach((period: any) => repository.addPeriod(period));
  repository.setPeriodSettings(new PeriodSettings(30, 5, 28, true, false));

  // 실제 결과 계산 (Repository 패턴 사용)
  const cycles = await calculateCycleInfo(
    repository,
    stringToJulianDay(tc.fromDate),
    stringToJulianDay(tc.toDate),
    stringToJulianDay(tc.today)
  );

  // 실제 결과 출력
  console.log("\n[실제]");
  console.log(`주기 개수: ${cycles.length}`);
  cycles.forEach((cycle: any, idx: number) => {
    console.log(`\n주기 ${idx + 1} (pk=${cycle.pk}):`);
    console.log(`  - 실제 생리: ${formatDateRange(cycle.actualPeriod)}`);
    console.log(`  - 지연 일수: ${cycle.delayDays || 0}`);
    console.log(`  - 지연 기간: ${formatDateRange(cycle.delayPeriod)}`);
    console.log(`  - 생리 예정일들: ${formatDateRanges(cycle.predictDays)}`);
    console.log(`  - 가임기들: ${formatDateRanges(cycle.fertileDays)}`);
    console.log(`  - 배란기들: ${formatDateRanges(cycle.ovulationDays)}`);
    console.log(`  - 주기: ${cycle.period}일`);
  });

  // 검증
  console.log("\n[검증]");

  // 주기 개수 검증
  if (cycles.length !== tc.expectedCycles.length) {
    const msg = `주기 개수 불일치: 예상 ${tc.expectedCycles.length}, 실제 ${cycles.length}`;
    errors.push(msg);
    console.log(`✗ ${msg}`);
  } else {
    console.log("✓ 주기 개수 일치");
  }

  // 각 주기 검증
  const minLength = Math.min(cycles.length, tc.expectedCycles.length);
  for (let i = 0; i < minLength; i++) {
    const actual = cycles[i];
    const expected = tc.expectedCycles[i];

    // pk 검증
    if (actual.pk !== expected.pk) {
      const msg = `주기 ${i + 1}: pk 불일치 - 예상 ${expected.pk}, 실제 ${actual.pk}`;
      errors.push(msg);
      console.log(`✗ ${msg}`);
    } else {
      console.log(`✓ 주기 ${i + 1}: pk 일치`);
    }

    // 실제 생리 검증
    if (expected.actualPeriod) {
      if (
        !compareDateRange(
          actual.actualPeriod,
          expected.actualPeriod.start,
          expected.actualPeriod.end
        )
      ) {
        const msg = `주기 ${i + 1}: 실제 생리 불일치 - 예상 ${expected.actualPeriod.start}~${expected.actualPeriod.end}, 실제 ${formatDateRange(actual.actualPeriod)}`;
        errors.push(msg);
        console.log(`✗ ${msg}`);
      } else {
        console.log(`✓ 주기 ${i + 1}: 실제 생리 일치`);
      }
    }

    // 지연 일수 검증
    const actualDelayDays = actual.delayDays || 0;
    if (actualDelayDays !== expected.delayDays) {
      const msg = `주기 ${i + 1}: 지연 일수 불일치 - 예상 ${expected.delayDays}, 실제 ${actualDelayDays}`;
      errors.push(msg);
      console.log(`✗ ${msg}`);
    } else {
      console.log(`✓ 주기 ${i + 1}: 지연 일수 일치`);
    }

    // 지연 기간 검증
    if (expected.delayPeriod) {
      if (
        !compareDateRange(
          actual.delayPeriod,
          expected.delayPeriod.start,
          expected.delayPeriod.end
        )
      ) {
        const msg = `주기 ${i + 1}: 지연 기간 불일치 - 예상 ${expected.delayPeriod.start}~${expected.delayPeriod.end}, 실제 ${formatDateRange(actual.delayPeriod)}`;
        errors.push(msg);
        console.log(`✗ ${msg}`);
      } else {
        console.log(`✓ 주기 ${i + 1}: 지연 기간 일치`);
      }
    } else if (actual.delayPeriod) {
      const msg = `주기 ${i + 1}: 지연 기간 불일치 - 예상 null, 실제 ${formatDateRange(actual.delayPeriod)}`;
      errors.push(msg);
      console.log(`✗ ${msg}`);
    } else {
      console.log(`✓ 주기 ${i + 1}: 지연 기간 일치 (null)`);
    }

    // 생리 예정일 검증
    if (!compareDateRanges(actual.predictDays, expected.predictDays)) {
      const msg = `주기 ${i + 1}: 생리 예정일 불일치`;
      errors.push(msg);
      console.log(`✗ ${msg}`);
    } else {
      console.log(`✓ 주기 ${i + 1}: 생리 예정일 일치`);
    }

    // 가임기 검증
    if (!compareDateRanges(actual.fertileDays, expected.fertileDays)) {
      const msg = `주기 ${i + 1}: 가임기 불일치`;
      errors.push(msg);
      console.log(`✗ ${msg}`);
    } else {
      console.log(`✓ 주기 ${i + 1}: 가임기 일치`);
    }

    // 배란기 검증
    if (!compareDateRanges(actual.ovulationDays, expected.ovulationDays)) {
      const msg = `주기 ${i + 1}: 배란기 불일치`;
      errors.push(msg);
      console.log(`✗ ${msg}`);
    } else {
      console.log(`✓ 주기 ${i + 1}: 배란기 일치`);
    }

    // 주기 검증
    if (actual.period !== expected.period) {
      const msg = `주기 ${i + 1}: 주기 일수 불일치 - 예상 ${expected.period}, 실제 ${actual.period}`;
      errors.push(msg);
      console.log(`✗ ${msg}`);
    } else {
      console.log(`✓ 주기 ${i + 1}: 주기 일수 일치`);
    }
  }

  const passed = errors.length === 0;
  console.log(`\n결과: ${passed ? "PASS ✓" : "FAIL ✗"}`);

  return { name: `${tc.id}: ${tc.name}`, passed, errors };
}

async function runAllTests() {
  console.log("=".repeat(50));
  console.log("표준일 피임법 (26-32일 주기) 통합 테스트");
  console.log("=".repeat(50));

  console.log("\n[공통 입력 조건]");
  console.log("생리 기록:");
  console.log("  - 2025-01-01 ~ 2025-01-05 (pk=1)");
  console.log("  - 2025-02-01 ~ 2025-02-05 (pk=2)");
  console.log("  - 2025-03-01 ~ 2025-03-05 (pk=3)");
  console.log("평균 주기: 30일 (자동 계산)");
  console.log("평균 기간: 5일");
  console.log("수동 평균 주기: 28일");
  console.log("자동 계산 사용: true");
  console.log("피임약 계산: false");

  for (const tc of TEST_CASES) {
    const result = await runTestCase(tc);
    testResults.push(result);
  }

  // 최종 요약
  console.log("\n" + "=".repeat(50));
  console.log("최종 결과 요약");
  console.log("=".repeat(50));

  const passedCount = testResults.filter((r) => r.passed).length;
  const failedCount = testResults.filter((r) => !r.passed).length;

  console.log(`\n총 ${testResults.length}개 테스트`);
  console.log(`✓ 통과: ${passedCount}개`);
  console.log(`✗ 실패: ${failedCount}개`);

  if (failedCount > 0) {
    console.log("\n[실패한 테스트]");
    testResults
      .filter((r) => !r.passed)
      .forEach((r) => {
        console.log(`\n${r.name}:`);
        r.errors.forEach((e) => console.log(`  - ${e}`));
      });
  }

  console.log("\n" + "=".repeat(50));
  console.log(passedCount === testResults.length ? "ALL TESTS PASSED!" : "SOME TESTS FAILED");
  console.log("=".repeat(50));
}

runAllTests().catch(console.error);
