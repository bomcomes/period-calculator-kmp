const lib = require("../shared/build/compileSync/js/main/productionLibrary/kotlin/period-calculator-kmp-shared.js");
const calculator = lib.com.bomcomes.calculator;

console.log("=== Test Case 5: Long Range (긴 검색 범위) ===");
console.log("시나리오: 3개월 범위에서 여러 주기 확인\n");

// 테스트 날짜 설정
const testDate = "2024-03-10";
const periodStart = "2024-03-05";
const periodEnd = "2024-03-10";
const searchFrom = "2024-03-01";
const searchTo = "2024-06-30"; // 4개월 범위

console.log("입력 데이터:");
console.log(`  - 마지막 생리: ${periodStart} ~ ${periodEnd}`);
console.log(`  - 검색 범위: ${searchFrom} ~ ${searchTo} (4개월)`);
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
console.log("  - 예측 생리일: 3개 (4/2, 4/30, 5/28)");
console.log("  - 배란기: 4개 정도 (3월, 4월, 5월, 6월)");
console.log("  - 가임기: 4개 정도\n");

console.log("실제 결과:");
if (result.length > 0) {
  const cycle = result[0];

  console.log(`  - 주기 개수: ${result.length}`);
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

  // 4개월 범위 = 약 120일 = 4~5개 주기
  results.push(
    cycle.predictDays.length >= 3 && cycle.predictDays.length <= 5
      ? `✓ 예측 생리일 ${cycle.predictDays.length}개 (3~5개 예상)`
      : `✗ 예측 생리일 (기대: 3~5개, 실제: ${cycle.predictDays.length})`,
  );
  results.push(
    cycle.ovulationDays.length >= 3 && cycle.ovulationDays.length <= 5
      ? `✓ 배란기 ${cycle.ovulationDays.length}개`
      : `✗ 배란기 (기대: 3~5개, 실제: ${cycle.ovulationDays.length})`,
  );
  results.push(
    cycle.fertileDays.length >= 3 && cycle.fertileDays.length <= 5
      ? `✓ 가임기 ${cycle.fertileDays.length}개`
      : `✗ 가임기 (기대: 3~5개, 실제: ${cycle.fertileDays.length})`,
  );

  // 첫 예측이 4/2에 시작해야 함
  if (cycle.predictDays.length > 0) {
    const expectedDate = calculator.stringToJulianDay("2024-04-02");
    results.push(
      cycle.predictDays[0].startDate === expectedDate
        ? "✓ 첫 예측 날짜 정확 (4/2)"
        : `✗ 첫 예측 날짜 (기대: 2024-04-02, 실제: ${calculator.julianDayToString(cycle.predictDays[0].startDate)})`,
    );
  }

  results.forEach((msg) => console.log(msg));
}
