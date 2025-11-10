// Period Calculator 시나리오 테스트
const lib = require('../shared/build/dist/js/productionLibrary/period-calculator-kmp-shared.js');
const calculator = lib.com.bomcomes.calculator;

console.log('=== 생리 주기 계산 테스트 ===\n');

// 시나리오 1: 10월 1일 생리 시작 (평균 주기 28일)
console.log('📅 시나리오 1: 10월 1일 생리 시작 (평균 주기 28일)');
console.log('입력:');
console.log('  - 생리 기간: 2025년 10월 1일 ~ 5일');
console.log('  - 평균 주기: 28일\n');

const periods1 = [
    { startDate: '2025-10-01', endDate: '2025-10-05' }
];

const result1 = calculator.calculateMenstrualCycles(
    periods1,
    '2025-10-01',  // fromDate
    '2025-11-30',  // toDate
    28,            // averageCycle
    5              // periodDays
);

if (result1.length > 0) {
    const cycle = result1[0];

    console.log('계산 결과:');
    console.log('  생리 기간:', cycle.theDay?.startDate, '~', cycle.theDay?.endDate);

    if (cycle.predictDays.length > 0) {
        console.log('  생리 예정일:', cycle.predictDays[0].startDate, '~', cycle.predictDays[0].endDate);
    }

    if (cycle.ovulationDays.length > 0) {
        console.log('  배란기:', cycle.ovulationDays[0].startDate, '~', cycle.ovulationDays[0].endDate);
    }

    if (cycle.childbearingAges.length > 0) {
        console.log('  가임기:', cycle.childbearingAges[0].startDate, '~', cycle.childbearingAges[0].endDate);
    }

    console.log('  주기:', cycle.period, '일');
    if (cycle.delayTheDays > 0) {
        console.log('  지연:', cycle.delayTheDays, '일');
    }
}

console.log('\n' + '='.repeat(60) + '\n');

// 시나리오 2: 11월 1일 생리 시작 추가 (평균 주기 31일로 업데이트)
console.log('📅 시나리오 2: 11월 1일 생리 시작 추가 (평균 주기 31일)');
console.log('입력:');
console.log('  - 생리 기간 1: 2025년 10월 1일 ~ 5일');
console.log('  - 생리 기간 2: 2025년 11월 1일 ~ 5일');
console.log('  - 평균 주기: 31일\n');

const periods2 = [
    { startDate: '2025-10-01', endDate: '2025-10-05' },
    { startDate: '2025-11-01', endDate: '2025-11-05' }
];

const result2 = calculator.calculateMenstrualCycles(
    periods2,
    '2025-11-01',  // fromDate
    '2025-12-31',  // toDate
    31,            // averageCycle (업데이트됨)
    5              // periodDays
);

if (result2.length > 0) {
    const cycle = result2[0];

    console.log('계산 결과:');
    console.log('  생리 기간:', cycle.theDay?.startDate, '~', cycle.theDay?.endDate);

    if (cycle.predictDays.length > 0) {
        console.log('  생리 예정일:', cycle.predictDays[0].startDate, '~', cycle.predictDays[0].endDate);
    }

    if (cycle.ovulationDays.length > 0) {
        console.log('  배란기:', cycle.ovulationDays[0].startDate, '~', cycle.ovulationDays[0].endDate);
    }

    if (cycle.childbearingAges.length > 0) {
        console.log('  가임기:', cycle.childbearingAges[0].startDate, '~', cycle.childbearingAges[0].endDate);
    }

    console.log('  주기:', cycle.period, '일');
}

console.log('\n' + '='.repeat(60) + '\n');

// 추가 테스트: 특정 날짜의 달력 상태 확인
console.log('📅 달력 상태 확인 테스트\n');

const testDates = [
    '2025-11-03',  // 생리 중
    '2025-11-14',  // 배란기
    '2025-11-10',  // 가임기
    '2025-12-03',  // 생리 예정일
    '2025-11-20'   // 일반일
];

testDates.forEach(date => {
    const status = calculator.calculateCalendarStatus(periods2, date, 31, 5);
    console.log(`${date}: ${status.calendarType} (임신가능성: ${status.probability}, 주기 ${status.gap}일차)`);
});

console.log('\n✅ 테스트 완료!');
