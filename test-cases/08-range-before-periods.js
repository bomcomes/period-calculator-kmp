const lib = require("../shared/build/compileSync/js/main/productionLibrary/kotlin/period-calculator-kmp-shared.js");
const calculator = lib.com.bomcomes.calculator;

console.log("=== Test Case 8: Range Before All Periods ===");
console.log("시나리오: 생리 기록 3개, 검색 범위가 모든 생리 기록 이전\n");

// 생리 기록 3개
const period1Start = "2024-02-05";
const period1End = "2024-02-10";
const period2Start = "2024-03-03"; // 28일 주기
const period2End = "2024-03-08";
const period3Start = "2024-03-31"; // 28일 주기
const period3End = "2024-04-05";

// 검색 범위: 모든 생리 기록보다 이전
const searchFrom = "2024-01-01";
const searchTo = "2024-02-04"; // 첫 번째 생리(2/5) 시작 전날까지
const testDate = "2024-01-15";

console.log("입력 데이터:");
console.log(`  - 첫 번째 생리: ${period1Start} ~ ${period1End}`);
console.log(`  - 두 번째 생리: ${period2Start} ~ ${period2End} (27일 주기)`);
console.log(`  - 세 번째 생리: ${period3Start} ~ ${period3End} (28일 주기)`);
console.log(`  - 검색 범위: ${searchFrom} ~ ${searchTo} (모든 생리 이전)`);
console.log(`  - 테스트 기준일(today): ${testDate}\n`);

const periods = [
  new calculator.JsPeriodRecord(
    "1",
    calculator.stringToJulianDay(period1Start),
    calculator.stringToJulianDay(period1End),
  ),
  new calculator.JsPeriodRecord(
    "2",
    calculator.stringToJulianDay(period2Start),
    calculator.stringToJulianDay(period2End),
  ),
  new calculator.JsPeriodRecord(
    "3",
    calculator.stringToJulianDay(period3Start),
    calculator.stringToJulianDay(period3End),
  ),
];

const result = calculator.calculateCycleInfo(
  periods,
  calculator.stringToJulianDay(searchFrom),
  calculator.stringToJulianDay(searchTo),
  calculator.stringToJulianDay(testDate),
  28, // averageCycle
  5, // periodDays
);

console.log("기대 결과:");
console.log("  - 검색 범위가 첫 번째 생리(2/5)보다 이전이므로:");
console.log("  - 주기 개수: 0개 또는 1개 (첫 번째 주기만, 예측만 포함)");
console.log("  - 실제 생리일: 없음 (모든 생리가 범위 밖)");
console.log("  - 예측/배란기/가임기: 범위 내 있으면 표시\n");

console.log("실제 결과:");
console.log(`  - 주기 개수: ${result.length}\n`);

if (result.length === 0) {
  console.log("  [결과 없음]\n");
} else {
  result.forEach((cycle, index) => {
    console.log(`[주기 ${index + 1}]`);
    if (cycle.actualPeriod) {
      console.log(
        `  - 실제 생리일: ${calculator.julianDayToString(cycle.actualPeriod.startDate)} ~ ${calculator.julianDayToString(cycle.actualPeriod.endDate)}`,
      );
    } else {
      console.log("  - 실제 생리일: 없음");
    }
    console.log(`  - 주기: ${cycle.period}일`);
    console.log(`  - 예측 생리일: ${cycle.predictDays.length}개`);
    cycle.predictDays.forEach((range, i) => {
      console.log(
        `      ${i + 1}. ${calculator.julianDayToString(range.startDate)} ~ ${calculator.julianDayToString(range.endDate)}`,
      );
    });
    console.log(`  - 배란기: ${cycle.ovulationDays.length}개`);
    cycle.ovulationDays.forEach((range, i) => {
      console.log(
        `      ${i + 1}. ${calculator.julianDayToString(range.startDate)} ~ ${calculator.julianDayToString(range.endDate)}`,
      );
    });
    console.log(`  - 가임기: ${cycle.fertileDays.length}개`);
    cycle.fertileDays.forEach((range, i) => {
      console.log(
        `      ${i + 1}. ${calculator.julianDayToString(range.startDate)} ~ ${calculator.julianDayToString(range.endDate)}`,
      );
    });
    console.log();
  });
}

console.log("검증:");
const results = [];

// 검색 범위가 모든 생리보다 이전이므로
// 결과가 없거나, 있다면 실제 생리일이 범위 밖에 있어야 함
if (result.length === 0) {
  results.push("✓ 결과 없음 (검색 범위가 모든 생리 이전)");
} else {
  // 결과가 있다면
  results.push(`✓ 주기 ${result.length}개 반환됨`);

  result.forEach((cycle, index) => {
    // 실제 생리일이 검색 범위 내에 있는지 확인
    if (cycle.actualPeriod) {
      const periodStart = cycle.actualPeriod.startDate;
      const rangeEnd = calculator.stringToJulianDay(searchTo);

      if (periodStart > rangeEnd) {
        results.push(
          `✓ 주기 ${index + 1}: 실제 생리일이 검색 범위 밖 (${calculator.julianDayToString(periodStart)} > ${searchTo})`,
        );
      } else {
        results.push(
          `✗ 주기 ${index + 1}: 실제 생리일이 검색 범위 내 (기대하지 않음)`,
        );
      }
    } else {
      results.push(`✓ 주기 ${index + 1}: 실제 생리일 없음`);
    }

    // 예측/배란기/가임기는 있을 수도 있고 없을 수도 있음
    const totalItems =
      cycle.predictDays.length +
      cycle.ovulationDays.length +
      cycle.fertileDays.length;
    results.push(
      `  → 예측/배란기/가임기 총 ${totalItems}개 (범위 내 포함 가능)`,
    );
  });
}

results.forEach((msg) => console.log(msg));

if (results.filter((msg) => msg.startsWith("✗")).length === 0) {
  console.log("\n✅ 모든 검증 통과!");
} else {
  console.log(
    `\n❌ ${results.filter((msg) => msg.startsWith("✗")).length}개 실패`,
  );
}
