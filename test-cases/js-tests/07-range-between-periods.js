const lib = require("../shared/build/compileSync/js/main/productionLibrary/kotlin/period-calculator-kmp-shared.js");
const calculator = lib.com.bomcomes.calculator;

console.log("=== Test Case 7: Range Between Periods ===");
console.log("시나리오: 생리 기록 3개, 검색 범위가 생리 기록 사이\n");

// 생리 기록 3개
const period1Start = "2024-01-05";
const period1End = "2024-01-10";
const period2Start = "2024-02-03"; // 29일 주기
const period2End = "2024-02-08";
const period3Start = "2024-03-02"; // 28일 주기
const period3End = "2024-03-07";

// 검색 범위: 두 번째와 세 번째 생리 사이
const searchFrom = "2024-02-10"; // 두 번째 생리 종료 직후
const searchTo = "2024-02-29"; // 세 번째 생리 시작 전
const testDate = "2024-02-15";

console.log("입력 데이터:");
console.log(`  - 첫 번째 생리: ${period1Start} ~ ${period1End}`);
console.log(`  - 두 번째 생리: ${period2Start} ~ ${period2End} (29일 주기)`);
console.log(`  - 세 번째 생리: ${period3Start} ~ ${period3End} (28일 주기)`);
console.log(`  - 검색 범위: ${searchFrom} ~ ${searchTo} (두 번째와 세 번째 생리 사이)`);
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
console.log("  - 주기 개수: 2개 (두 번째, 세 번째 주기만 범위에 관련)");
console.log("  - 두 번째 주기에서 배란기/가임기만 범위 내 포함");
console.log("  - 세 번째 주기의 예측은 3/2 이후이므로 범위(~2/29) 밖\n");

console.log("실제 결과:");
console.log(`  - 주기 개수: ${result.length}\n`);

result.forEach((cycle, index) => {
  console.log(`[주기 ${index + 1}]`);
  if (cycle.actualPeriod) {
    console.log(
      `  - 실제 생리일: ${calculator.julianDayToString(cycle.actualPeriod.startDate)} ~ ${calculator.julianDayToString(cycle.actualPeriod.endDate)}`,
    );
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

console.log("검증:");
const results = [];

// 두 번째 주기 검증 (2/3~2/8 생리)
// 배란기: 2/3 + 12~14일 = 2/15~2/17 (범위 2/10~2/29 내)
// 가임기: 2/3 + 7~18일 = 2/10~2/21 (범위 2/10~2/29 내)
results.push(
  result.length >= 1
    ? `✓ 최소 1개 주기 반환`
    : `✗ 주기 개수 (기대: 1개 이상, 실제: ${result.length})`,
);

if (result.length >= 1) {
  const cycle = result.find((c) => c.actualPeriod &&
    calculator.julianDayToString(c.actualPeriod.startDate) === "2024-02-03");

  if (cycle) {
    results.push("✓ 두 번째 주기 (2/3) 포함됨");

    // 배란기이 범위 내 있는지 확인
    results.push(
      cycle.ovulationDays.length >= 1
        ? `✓ 배란기 ${cycle.ovulationDays.length}개 (범위 내)`
        : `✗ 배란기 (기대: 1개 이상, 실제: ${cycle.ovulationDays.length})`,
    );

    // 가임기가 범위 내 있는지 확인
    results.push(
      cycle.fertileDays.length >= 1
        ? `✓ 가임기 ${cycle.fertileDays.length}개 (범위 내)`
        : `✗ 가임기 (기대: 1개 이상, 실제: ${cycle.fertileDays.length})`,
    );

    // 예측 생리일은 3/3경이므로 범위(~2/29) 밖
    results.push(
      cycle.predictDays.length === 0
        ? "✓ 예측 생리일 없음 (범위 밖)"
        : `✗ 예측 생리일 (기대: 0, 실제: ${cycle.predictDays.length})`,
    );
  } else {
    results.push("✗ 두 번째 주기 (2/3)를 찾을 수 없음");
  }
}

results.forEach((msg) => console.log(msg));

if (results.filter((msg) => msg.startsWith("✗")).length === 0) {
  console.log("\n✅ 모든 검증 통과!");
} else {
  console.log(
    `\n❌ ${results.filter((msg) => msg.startsWith("✗")).length}개 실패`,
  );
}
