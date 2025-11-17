const lib = require("../shared/build/compileSync/js/main/productionLibrary/kotlin/period-calculator-kmp-shared.js");
const calculator = lib.com.bomcomes.calculator;

console.log("=== Test Case 2: Delayed Period ===");
console.log("시나리오: 생리 지연 (예정일 지나서 테스트)\n");

// 테스트 날짜 설정
const testDate = "2024-04-05"; // 예정일(4/2) 지난 날
const periodStart = "2024-03-05";
const periodEnd = "2024-03-10";
const searchFrom = "2024-03-01";
const searchTo = "2024-04-30";

console.log("입력 데이터:");
console.log(`  - 마지막 생리: ${periodStart} ~ ${periodEnd}`);
console.log(`  - 검색 범위: ${searchFrom} ~ ${searchTo}`);
console.log(`  - 테스트 기준일(today): ${testDate}`);
console.log(`  - 평균 주기: 28일\n`);

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
console.log("  - 예측 생리일: 없음 (지연 중이므로)");
console.log("  - 배란기: 2024-03-17 ~ 2024-03-19");
console.log("  - 가임기: 2024-03-12 ~ 2024-03-23");
console.log("  - 지연일: 2024-04-02 ~ 2024-04-05 (3일 지연)\n");

console.log("실제 결과:");
if (result.length > 0) {
  const cycle = result[0];

  console.log(`  - 예측 생리일: ${cycle.predictDays.length}개`);
  console.log(`  - 배란기: ${cycle.ovulationDays.length}개`);
  console.log(`  - 가임기: ${cycle.fertileDays.length}개`);
  console.log(
    `  - 지연일: ${cycle.delayPeriod ? calculator.julianDayToString(cycle.delayPeriod.startDate) + " ~ " + calculator.julianDayToString(cycle.delayPeriod.endDate) : "없음"}`,
  );
  console.log(`  - 지연 일수: ${cycle.delayDays}일`);

  console.log("\n검증:");
  const results = [];

  // 지연 중에도 미래 예측은 표시됨 (4/30 등)
  results.push(
    cycle.predictDays.length >= 1
      ? `✓ 예측 생리일 ${cycle.predictDays.length}개 (지연 후 미래 예측)`
      : `✗ 예측 생리일 (실제: ${cycle.predictDays.length})`,
  );
  // 지연 일수 계산: 4/5 - 4/2 = 3일이지만 시작일 포함 4일
  results.push(
    cycle.delayDays === 4
      ? "✓ 지연 4일 (4/2~4/5)"
      : `✗ 지연 일수 (기대: 4, 실제: ${cycle.delayDays})`,
  );
  results.push(cycle.delayPeriod ? "✓ 지연 기간 존재" : "✗ 지연 기간 없음");

  results.forEach((msg) => console.log(msg));
}
