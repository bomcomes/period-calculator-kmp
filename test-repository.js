// KMP 라이브러리 Repository 패턴 테스트
const kmp = require("./shared/build/dist/js/productionLibrary/period-calculator-kmp-shared");
const { calculateCycleInfoWithRepository } = kmp.com.bomcomes.calculator;
const { PeriodSettings, PillSettings } = kmp.com.bomcomes.calculator.models;
const { JsPeriodRecordDto, DateInput } = kmp.com.bomcomes.calculator.dto;

console.log("=== KMP Library Test (With Repository) ===\n");

// 마지막 생리일 (실제로는 Firebase에서 조회)
const lastPeriod = new JsPeriodRecordDto("last", "2024-03-01", "2024-03-05");

// Repository 구현
class FirebasePeriodRepository {
  constructor(userId) {
    this.userId = userId;
  }

  // 생리 기록 조회 - DTO 사용
  async getPeriods(fromDate, toDate) {
    console.log(`[Repository] getPeriods for user: ${this.userId}`);
    console.log(`  Range: ${fromDate} ~ ${toDate}`);

    // 마지막 생리일만 반환
    const fromDateStr = fromDate.iso8601 || fromDate.toString();
    const toDateStr = toDate.iso8601 || toDate.toString();

    if (
      lastPeriod.startDate >= fromDateStr &&
      lastPeriod.startDate <= toDateStr
    ) {
      console.log("  → 1개 반환 (마지막 생리일)");
      return [lastPeriod];
    }

    console.log("  → 0개 반환 (범위 밖)");
    return [];
  }

  // 시작일 이전의 마지막 생리
  async getLastPeriodBefore(date, excludeBeforeDate) {
    console.log(`[Repository] getLastPeriodBefore(${date})`);
    // 마지막 생리일 이전 생리 (28일 전)
    return new JsPeriodRecordDto("prev", "2024-02-02", "2024-02-06");
  }

  // 종료일 이후의 첫 생리
  async getFirstPeriodAfter(date, excludeAfterDate) {
    console.log(`[Repository] getFirstPeriodAfter(${date})`);
    return null;
  }

  // 생리 설정 조회
  getPeriodSettings() {
    console.log("[Repository] getPeriodSettings()");
    return new PeriodSettings(28, 5, 28, false);
  }

  // 배란 테스트 기록 조회
  async getOvulationTests(fromDate, toDate) {
    console.log(`[Repository] getOvulationTests(${fromDate}, ${toDate})`);
    return [];
  }

  // 사용자가 입력한 배란일 조회
  async getUserOvulationDays(fromDate, toDate) {
    console.log(`[Repository] getUserOvulationDays(${fromDate}, ${toDate})`);
    return [];
  }

  // 피임약 복용 기록 조회
  async getPillPackages() {
    console.log("[Repository] getPillPackages()");
    return [];
  }

  // 피임약 설정 조회
  getPillSettings() {
    console.log("[Repository] getPillSettings()");
    return new PillSettings(false, 21, 7);
  }

  // 활성 임신 정보 조회
  async getActivePregnancy() {
    console.log("[Repository] getActivePregnancy()");
    return null;
  }
}

async function runTest() {
  try {
    // Repository 인스턴스 생성
    const repository = new FirebasePeriodRepository("user123");

    console.log("마지막 생리일 기준으로 가임기/배란기/생리 예정일 계산...\n");
    console.log("마지막 생리: 2024-03-01 ~ 2024-03-05");
    console.log("평균 주기: 28일");
    console.log("조회 범위: 2024-03-01 ~ 2024-04-30 (60일)\n");

    // 마지막 생리일부터 60일 후까지 계산
    const cycles = await calculateCycleInfoWithRepository(
      repository,
      new DateInput("2024-03-01"), // 마지막 생리 시작일
      new DateInput("2024-04-30"), // 60일 후
    );

    console.log("\n=== 계산 결과 ===");
    console.log("주기 개수:", cycles.length);

    // 모든 주기 출력
    cycles.forEach((cycle, idx) => {
      console.log(`\n[주기 ${idx + 1}]`);
      console.log(
        "  - 실제 생리일:",
        cycle.actualPeriod
          ? `${cycle.actualPeriod.startDate.iso8601} ~ ${cycle.actualPeriod.endDate.iso8601}`
          : "N/A",
      );
      console.log("  - 예측 생리일:", cycle.predictDays?.length || 0, "개");
      if (cycle.predictDays && cycle.predictDays.length > 0) {
        cycle.predictDays.forEach((range, i) => {
          console.log(
            `    ${i + 1}. ${range.startDate.iso8601} ~ ${range.endDate.iso8601}`,
          );
        });
      }
      console.log("  - 배란일:", cycle.ovulationDays?.length || 0, "개");
      if (cycle.ovulationDays && cycle.ovulationDays.length > 0) {
        cycle.ovulationDays.forEach((range, i) => {
          console.log(
            `    ${i + 1}. ${range.startDate.iso8601} ~ ${range.endDate.iso8601}`,
          );
        });
      }
      console.log("  - 가임기:", cycle.fertileDays?.length || 0, "개");
      if (cycle.fertileDays && cycle.fertileDays.length > 0) {
        cycle.fertileDays.forEach((range, i) => {
          console.log(
            `    ${i + 1}. ${range.startDate.iso8601} ~ ${range.endDate.iso8601}`,
          );
        });
      }
      console.log(
        "  - 지연일:",
        cycle.delayDay
          ? `${cycle.delayDay.startDate.iso8601} ~ ${cycle.delayDay.endDate.iso8601}`
          : "N/A",
      );
      console.log("  - 평균 주기:", cycle.period, "일");
    });

    console.log("\n=== Test Complete ===");
  } catch (error) {
    console.error("Error:", error);
    console.error("Stack:", error.stack);
  }
}

runTest();
