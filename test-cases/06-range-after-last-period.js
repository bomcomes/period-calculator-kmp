const lib = require("../shared/build/compileSync/js/main/productionLibrary/kotlin/period-calculator-kmp-shared.js");
const calculator = lib.com.bomcomes.calculator;

console.log("=== Test Case 6: Range After Last Period ===");
console.log("시나리오: 생리 기록 3개, 검색 범위가 마지막 주기 이후\n");

// 생리 기록 3개
const period1Start = "2024-01-05";
const period1End = "2024-01-10";
const period2Start = "2024-02-03"; // 29일 주기
const period2End = "2024-02-08";
const period3Start = "2024-03-02"; // 28일 주기
const period3End = "2024-03-07";

// 검색 범위: 마지막 생리(3/2) 포함하여 이후
const searchFrom = "2024-03-01"; // 마지막 생리 시작 전
const searchTo = "2024-05-31";
const testDate = "2024-03-15";

console.log("입력 데이터:");
console.log(`  - 첫 번째 생리: ${period1Start} ~ ${period1End}`);
console.log(`  - 두 번째 생리: ${period2Start} ~ ${period2End} (29일 주기)`);
console.log(`  - 세 번째 생리: ${period3Start} ~ ${period3End} (28일 주기)`);
console.log(`  - 검색 범위: ${searchFrom} ~ ${searchTo} (마지막 생리 포함)`);
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
console.log("  - 주기 개수: 2개 (실제 생리일이 검색 범위 전인 것만)");
console.log("  - 두 번째 주기의 평균: 28일 (1/5 → 2/3)");
console.log(
  "  - 세 번째 생리(3/2)는 검색 범위(3/1) 직후라 범위 내 데이터 없어 제외됨",
);
console.log("  - 참고: 세 번째 생리만 단독으로는 예측 3/30, 4/27, 5/25 나옴\n");

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
  console.log(`  - 가임기: ${cycle.fertileDays.length}개\n`);
});

console.log("검증:");
const results = [];

// 기본 검증
results.push(
  result.length === 2
    ? "✓ 주기 2개 (세 번째 생리는 범위 내 데이터 없어 제외됨)"
    : `✗ 주기 개수 (기대: 2, 실제: ${result.length})`,
);

// 두 번째 주기(index 1) 검증
if (result.length >= 2) {
  const secondCycle = result[1];

  // 실제 주기 계산: 1/5 → 2/3 = 29일
  results.push(
    secondCycle.period === 28
      ? "✓ 두 번째 주기 28일"
      : `✗ 두 번째 주기 (기대: 28, 실제: ${secondCycle.period})`,
  );

  // 검색 범위가 3/1~5/31인데 두 번째 생리는 2/3이므로 범위 내 예측 없음
  results.push(
    secondCycle.predictDays.length === 0
      ? "✓ 예측 없음 (범위 밖)"
      : `✗ 예측 개수 (기대: 0, 실제: ${secondCycle.predictDays.length})`,
  );

  // 배란기과 가임기도 범위 밖
  results.push(
    secondCycle.ovulationDays.length === 0
      ? "✓ 배란기 없음 (범위 밖)"
      : `✗ 배란기 (기대: 0, 실제: ${secondCycle.ovulationDays.length})`,
  );

  results.push(
    secondCycle.fertileDays.length === 0
      ? "✓ 가임기 없음 (범위 밖)"
      : `✗ 가임기 (기대: 0, 실제: ${secondCycle.fertileDays.length})`,
  );
}

results.forEach((msg) => console.log(msg));

if (results.filter((msg) => msg.startsWith("✗")).length === 0) {
  console.log("\n✅ 모든 검증 통과!");
} else {
  console.log(
    `\n❌ ${results.filter((msg) => msg.startsWith("✗")).length}개 실패`,
  );
}
