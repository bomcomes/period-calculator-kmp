const lib = require("../shared/build/compileSync/js/main/productionLibrary/kotlin/period-calculator-kmp-shared.js");
const calculator = lib.com.bomcomes.calculator;

console.log("=== Test Case 3: Multiple Periods ===");
console.log("시나리오: 생리 2개, 실제 주기 계산\n");

// 테스트 날짜 설정
const testDate = "2024-03-10";
const period1Start = "2024-02-05";
const period1End = "2024-02-10";
const period2Start = "2024-03-05";
const period2End = "2024-03-10";
const searchFrom = "2024-02-01";
const searchTo = "2024-04-30";

console.log("입력 데이터:");
console.log(`  - 첫 번째 생리: ${period1Start} ~ ${period1End}`);
console.log(`  - 두 번째 생리: ${period2Start} ~ ${period2End}`);
console.log(`  - 검색 범위: ${searchFrom} ~ ${searchTo}`);
console.log(`  - 테스트 기준일(today): ${testDate}`);
console.log(`  - 설정 평균 주기: 28일\n`);

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
const actualCycle = Math.floor(
  calculator.stringToJulianDay(period2Start) -
    calculator.stringToJulianDay(period1Start),
);
console.log(`  - 실제 주기: ${actualCycle}일 (2/5 → 3/5)`);
console.log(`  - 주기 개수: 2개`);
console.log(`  - 첫 번째 주기: 2/5 생리, 실제 주기 ${actualCycle}일`);
console.log(`  - 두 번째 주기: 3/5 생리, 평균 주기 28일 사용`);
console.log(`  - 다음 예측: 2024-04-02 (3/5 + 28일)\n`);

console.log("실제 결과:");
console.log(`  - 주기 개수: ${result.length}\n`);

result.forEach((cycle, index) => {
  console.log(`[주기 ${index + 1}]`);
  console.log(
    `  - 실제 생리일: ${calculator.julianDayToString(cycle.actualPeriod.startDate)} ~ ${calculator.julianDayToString(cycle.actualPeriod.endDate)}`,
  );
  console.log(`  - 주기: ${cycle.period}일`);
  console.log(`  - 예측 생리일: ${cycle.predictDays.length}개`);
  cycle.predictDays.forEach((range, i) => {
    console.log(
      `      ${i + 1}. ${calculator.julianDayToString(range.startDate)} ~ ${calculator.julianDayToString(range.endDate)}`,
    );
  });
  console.log(`  - 배란기: ${cycle.ovulationDays.length}개`);
  console.log(`  - 가임기: ${cycle.fertileDays.length}개\n`);
});

console.log("검증:");
const results = [];

results.push(
  result.length === 2
    ? "✓ 주기 2개"
    : `✗ 주기 개수 (기대: 2, 실제: ${result.length})`,
);

if (result.length >= 1) {
  results.push(
    result[0].period === actualCycle
      ? `✓ 첫 번째 주기 ${actualCycle}일`
      : `✗ 첫 번째 주기 (기대: ${actualCycle}, 실제: ${result[0].period})`,
  );
}

if (result.length >= 2) {
  results.push(
    result[1].period === 28
      ? "✓ 두 번째 주기 28일 (평균)"
      : `✗ 두 번째 주기 (기대: 28, 실제: ${result[1].period})`,
  );
  // 검색 범위가 4/30까지이므로 2개 예측 (4/2, 4/30)
  results.push(
    result[1].predictDays.length === 2
      ? "✓ 다음 예측 2개 (4/2, 4/30)"
      : `✗ 다음 예측 (기대: 2, 실제: ${result[1].predictDays.length})`,
  );
}

results.forEach((msg) => console.log(msg));
