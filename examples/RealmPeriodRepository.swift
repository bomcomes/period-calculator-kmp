/**
 * Realm Period Repository 구현 예시
 *
 * iOS에서 사용할 Repository 구현체
 * Realm에서 데이터를 가져와서 KMP 라이브러리에 전달
 */

import Foundation
import RealmSwift
import PeriodCalculatorKMP

// Realm 모델 예시 (실제 프로젝트의 모델에 맞게 수정 필요)
class PeriodRealmObject: Object {
    @Persisted(primaryKey: true) var id: String
    @Persisted var startDate: Date
    @Persisted var endDate: Date
    @Persisted var isMenstruation: Bool = true
    @Persisted var isDeleted: Bool = false
}

class PeriodSettingsRealmObject: Object {
    @Persisted(primaryKey: true) var id: String = "default"
    @Persisted var period: Int = 28
    @Persisted var days: Int = 5
    @Persisted var isRegularCycle: Bool = true
}

class OvulationTestRealmObject: Object {
    @Persisted(primaryKey: true) var id: String
    @Persisted var testDate: Date
    @Persisted var isPositive: Bool
    @Persisted var isDeleted: Bool = false
}

// ... 나머지 Realm 모델들

/**
 * Realm을 사용하는 Repository 구현체
 */
class RealmPeriodRepository: PeriodDataRepository {
    private let realm: Realm

    init() throws {
        self.realm = try Realm()
    }

    // MARK: - PeriodDataRepository 구현

    func getPeriods(fromDate: LocalDate, toDate: LocalDate) async throws -> [PeriodRecord] {
        // LocalDate를 Date로 변환
        let from = fromDate.toDate()
        let to = toDate.toDate()

        // Realm에서 조회
        let results = realm.objects(PeriodRealmObject.self)
            .filter("startDate >= %@ AND startDate <= %@ AND isDeleted == false", from, to)
            .sorted(byKeyPath: "startDate", ascending: false)

        // KMP 모델로 변환
        return results.map { realmObj in
            PeriodRecord(
                pk: realmObj.id,
                startDate: LocalDate(date: realmObj.startDate),
                endDate: LocalDate(date: realmObj.endDate),
                isMenstruation: realmObj.isMenstruation,
                isDeleted: realmObj.isDeleted
            )
        }
    }

    func getPeriodSettings() async throws -> PeriodSettings {
        // Realm에서 설정 조회
        if let settings = realm.objects(PeriodSettingsRealmObject.self).first {
            return PeriodSettings(
                period: Int32(settings.period),
                days: Int32(settings.days),
                isRegularCycle: settings.isRegularCycle
            )
        }

        // 기본값 반환
        return PeriodSettings(
            period: 28,
            days: 5,
            isRegularCycle: true
        )
    }

    func getOvulationTests(fromDate: LocalDate, toDate: LocalDate) async throws -> [OvulationTest] {
        let from = fromDate.toDate()
        let to = toDate.toDate()

        let results = realm.objects(OvulationTestRealmObject.self)
            .filter("testDate >= %@ AND testDate <= %@ AND isDeleted == false", from, to)
            .sorted(byKeyPath: "testDate", ascending: true)

        return results.map { realmObj in
            OvulationTest(
                pk: realmObj.id,
                testDate: LocalDate(date: realmObj.testDate),
                isPositive: realmObj.isPositive,
                isDeleted: realmObj.isDeleted
            )
        }
    }

    func getUserOvulationDays(fromDate: LocalDate, toDate: LocalDate) async throws -> [OvulationDay] {
        // 구현...
        return []
    }

    func getPillPackages() async throws -> [PillPackage] {
        // 구현...
        return []
    }

    func getPillSettings() async throws -> PillSettings {
        // 구현...
        return PillSettings(
            isCalculatingWithPill: false,
            reminderEnabled: false
        )
    }

    func getActivePregnancy() async throws -> PregnancyInfo? {
        // 구현...
        return nil
    }
}

// MARK: - Helper Extensions

extension LocalDate {
    /// LocalDate를 Swift Date로 변환
    func toDate() -> Date {
        var components = DateComponents()
        components.year = Int(self.year)
        components.month = Int(self.monthNumber)
        components.day = Int(self.dayOfMonth)
        return Calendar.current.date(from: components)!
    }

    /// Swift Date를 LocalDate로 변환
    init(date: Date) {
        let components = Calendar.current.dateComponents([.year, .month, .day], from: date)
        self.init(
            year: Int32(components.year!),
            monthNumber: Int32(components.month!),
            dayOfMonth: Int32(components.day!)
        )
    }
}

// MARK: - 사용 예시

class PeriodViewModel {
    private let repository: PeriodDataRepository

    init(repository: PeriodDataRepository = try! RealmPeriodRepository()) {
        self.repository = repository
    }

    func calculatePeriod(from: Date, to: Date) async throws -> [PeriodCycle] {
        let fromDate = LocalDate(date: from)
        let toDate = LocalDate(date: to)

        // KMP 라이브러리 호출 (Repository 전달)
        let result = try await PeriodCalculator.shared.calculateMenstrualCycles(
            repository: repository,
            fromDate: fromDate,
            toDate: toDate
        )

        return result
    }
}
