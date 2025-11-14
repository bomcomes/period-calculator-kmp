const { execSync } = require("child_process");
const fs = require("fs");
const path = require("path");

console.log("╔═══════════════════════════════════════════════════════════╗");
console.log("║     Period Calculator - JavaScript Test Suite            ║");
console.log("╚═══════════════════════════════════════════════════════════╝\n");

// 테스트 파일 목록
const testFiles = [
  "01-basic-single-period.js",
  "02-delayed-period.js",
  "03-multiple-periods.js",
  "04-past-search.js",
  "05-long-range.js",
  "06-range-after-last-period.js",
  "07-range-between-periods.js",
  "08-range-before-periods.js",
];

let totalTests = 0;
let passedTests = 0;
let failedTests = 0;

testFiles.forEach((testFile, index) => {
  console.log(`\n${"=".repeat(70)}`);
  console.log(`테스트 ${index + 1}/${testFiles.length}: ${testFile}`);
  console.log("=".repeat(70));

  try {
    const output = execSync(`node ${path.join(__dirname, testFile)}`, {
      encoding: "utf-8",
      stdio: "pipe",
    });

    console.log(output);

    // 결과 파싱
    const lines = output.split("\n");
    const checkLines = lines.filter(
      (line) => line.includes("✓") || line.includes("✗"),
    );
    const passed = checkLines.filter((line) => line.includes("✓")).length;
    const failed = checkLines.filter((line) => line.includes("✗")).length;

    totalTests += passed + failed;
    passedTests += passed;
    failedTests += failed;
  } catch (error) {
    console.error(`❌ 테스트 실행 실패: ${error.message}`);
    failedTests++;
  }
});

console.log(`\n${"=".repeat(70)}`);
console.log("최종 결과");
console.log("=".repeat(70));
console.log(`총 검증 항목: ${totalTests}`);
console.log(`통과: ${passedTests} ✓`);
console.log(`실패: ${failedTests} ✗`);

if (failedTests === 0) {
  console.log("\n🎉 모든 테스트 통과!");
} else {
  console.log(`\n⚠️  ${failedTests}개 항목 실패`);
  process.exit(1);
}
