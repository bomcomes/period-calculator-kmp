const calculator = require('./shared/build/js/packages/period-calculator-kmp-shared/kotlin/period-calculator-kmp-shared.js');

console.log('=== Debug Test ===');

// Test case: One period, search from before the period
const jan1 = calculator.stringToJulianDay('2025-01-01');
const jan5 = calculator.stringToJulianDay('2025-01-05');
const jan9 = calculator.stringToJulianDay('2025-01-09');
const feb28 = calculator.stringToJulianDay('2025-02-28');

console.log('Dates:');
console.log('  Jan 1 (fromDate):', jan1);
console.log('  Jan 5 (period start):', jan5);
console.log('  Jan 9 (period end):', jan9);
console.log('  Feb 28 (toDate):', feb28);

const periods = [
  {
    pk: '1',
    startDate: jan5,
    endDate: jan9
  }
];

const periodSettings = {
  period: 28,
  days: 5,
  ovulation: 14
};

const result = calculator.calculateCycleInfoDirect(
  periods,
  periodSettings,
  [], // ovulationTests
  [], // userOvulationDays
  [], // pillPackages
  { isCalculatingWithPill: false, pillCount: 21, restDays: 7 },
  null, // pregnancy
  jan1,
  feb28
);

console.log('\nResult:', JSON.stringify(result, null, 2));
