const lib = require("../shared/build/compileSync/js/main/productionLibrary/kotlin/period-calculator-kmp-shared.js");
const calculator = lib.com.bomcomes.calculator;

console.log("=== Test Case 4: Past Search (과거 검색) ===");
console.log(
  "시나리오: 과거 날짜 범위 검색 (fromDate가 생리 시작일보다 이전)\n",
);

// 테스트 날짜 설정
const testDate = "2024-03-10";
const periodStart = "2024-03-05";
const periodEnd = "2024-03-10";
const searchFrom = "2024-03-02"; // 생리 시작일보다 3일 이전
const searchTo = "2024-04-30";

console.log("입력 데이터:");
console.log(`  - 마지막 생리: ${periodStart} ~ ${periodEnd}`);
console.log(`  - 검색 범위: ${searchFrom} ~ ${searchTo}`);
console.log(`  - 테스트 기준일(today): ${testDate}`);
console.log(`  - 평균 주기: 28일`);
console.log(`  - 주의: searchFrom(3/2)이 생리 시작일(3/5)보다 이전\n`);

const periods = [
  new calculator.JsPeriodRecord(
    "1",
    calculator.stringToJulianDay(periodStart),
    calculator.stringToJulianDay(periodEnd),
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
console.log("  - 예측 생리일: 2024-04-02 ~ 2024-04-06 (정상 계산)");
console.log("  - 배란기: 2024-03-17 ~ 2024-03-19 (정상 계산)");
console.log("  - 가임기: 2024-03-12 ~ 2024-03-23 (정상 계산)");
console.log("  - fromDate가 생리 시작일보다 이전이어도 정상 동작\n");

console.log("실제 결과:");
if (result.length > 0) {
  const cycle = result[0];

  console.log(`  - 주기 개수: ${result.length}`);
  console.log(
    `  - 실제 생리일: ${calculator.julianDayToString(cycle.actualPeriod.startDate)} ~ ${calculator.julianDayToString(cycle.actualPeriod.endDate)}`,
  );
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

  console.log("\n검증:");
  const results = [];

  // 검색 범위가 3/2~4/30이므로 2개의 예측이 정상 (4/2, 4/30)
  results.push(
    cycle.predictDays.length === 2
      ? "✓ 예측 생리일 2개"
      : `✗ 예측 생리일 (기대: 2, 실제: ${cycle.predictDays.length})`,
  );
  results.push(
    cycle.ovulationDays.length === 3
      ? "✓ 배란기 3개"
      : `✗ 배란기 (기대: 3, 실제: ${cycle.ovulationDays.length})`,
  );
  results.push(
    cycle.fertileDays.length === 3
      ? "✓ 가임기 3개"
      : `✗ 가임기 (기대: 3, 실제: ${cycle.fertileDays.length})`,
  );

  // 예측 생리일이 4/2에 시작해야 함
  if (cycle.predictDays.length > 0) {
    const expectedDate = calculator.stringToJulianDay("2024-04-02");
    results.push(
      cycle.predictDays[0].startDate === expectedDate
        ? "✓ 예측 생리일 날짜 정확"
        : `✗ 예측 생리일 날짜 (기대: 2024-04-02, 실제: ${calculator.julianDayToString(cycle.predictDays[0].startDate)})`,
    );
  }

  results.forEach((msg) => console.log(msg));
}
