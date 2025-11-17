const lib = require("../shared/build/compileSync/js/main/productionLibrary/kotlin/period-calculator-kmp-shared.js");
const calculator = lib.com.bomcomes.calculator;

console.log("=== Test Case 1: Basic Single Period ===");
console.log("시나리오: 생리 1개, 28일 주기, 범위 내 예측\n");

// 테스트 날짜 설정
const testDate = "2024-03-10"; // 테스트 기준일
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
  calculator.stringToJulianDay(testDate), // today 파라미터
  28, // averageCycle
  5, // periodDays
);

console.log("기대 결과:");
console.log("  - 예측 생리일: 2024-04-02 ~ 2024-04-06 (3/5 + 28일)");
console.log("  - 배란기: 2024-03-17 ~ 2024-03-19 (3/5 + 12~14일)");
console.log("  - 가임기: 2024-03-12 ~ 2024-03-23 (3/5 + 7~18일)");
console.log("  - 지연일: 없음 (아직 예정일 지나지 않음)\n");

console.log("실제 결과:");
console.log(`  - 주기 개수: ${result.length}`);

if (result.length > 0) {
  const cycle = result[0];

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

  console.log(
    `  - 지연일: ${cycle.delayPeriod ? calculator.julianDayToString(cycle.delayPeriod.startDate) + " ~ " + calculator.julianDayToString(cycle.delayPeriod.endDate) : "없음"}`,
  );
  console.log(`  - 지연 일수: ${cycle.delayDays}일`);
  console.log(`  - 평균 주기: ${cycle.period}일`);

  // 검증
  console.log("\n검증:");
  const passed = [];
  const failed = [];

  // 검색 범위가 2개월(3/1~4/30)이므로 2개의 예측이 정상
  if (cycle.predictDays.length === 2) {
    passed.push("✓ 예측 생리일 2개 (4/2, 4/30)");
  } else {
    failed.push(
      `✗ 예측 생리일 개수 (기대: 2, 실제: ${cycle.predictDays.length})`,
    );
  }

  // 검색 범위 내 3개의 배란기이 정상 (3월, 4월, 5월)
  if (cycle.ovulationDays.length === 3) {
    passed.push("✓ 배란기 3개");
  } else {
    failed.push(`✗ 배란기 개수 (기대: 3, 실제: ${cycle.ovulationDays.length})`);
  }

  // 검색 범위 내 3개의 가임기가 정상
  if (cycle.fertileDays.length === 3) {
    passed.push("✓ 가임기 3개");
  } else {
    failed.push(`✗ 가임기 개수 (기대: 3, 실제: ${cycle.fertileDays.length})`);
  }

  if (cycle.delayDays === 0) {
    passed.push("✓ 지연 없음");
  } else {
    failed.push(`✗ 지연 일수 (기대: 0, 실제: ${cycle.delayDays})`);
  }

  passed.forEach((msg) => console.log(msg));
  failed.forEach((msg) => console.log(msg));

  if (failed.length === 0) {
    console.log("\n✅ 모든 검증 통과!");
  } else {
    console.log(`\n❌ ${failed.length}개 실패`);
  }
}
