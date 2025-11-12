/**
 * Firestore Period Repository 구현 예시
 *
 * Firebase Functions에서 사용할 Repository 구현체
 * Firestore에서 데이터를 가져와서 KMP 라이브러리에 전달
 */

import * as admin from "firebase-admin";
import { Firestore } from "firebase-admin/firestore";

// KMP에서 export한 타입들 (JavaScript 빌드 후)
import {
  PeriodRecord,
  PeriodSettings,
  OvulationTest,
  OvulationDay,
  PillPackage,
  PillSettings,
  PregnancyInfo,
  JsPeriodDataRepository,
  LocalDate,
  calculateMenstrualCyclesWithRepository,
} from "../shared/build/dist/js/productionLibrary/period-calculator-kmp-shared";

/**
 * Firestore Repository 구현체
 * JsPeriodDataRepository 인터페이스를 구현합니다.
 */
export class FirestorePeriodRepository implements JsPeriodDataRepository {
  private db: Firestore;
  private userId: string;

  constructor(userId: string) {
    this.userId = userId;
    this.db = admin.firestore();
  }

  /**
   * 생리 기록 가져오기
   */
  async getPeriods(
    fromDate: LocalDate,
    toDate: LocalDate,
  ): Promise<PeriodRecord[]> {
    // LocalDate를 JavaScript Date로 변환
    const from = this.toDate(fromDate);
    const to = this.toDate(toDate);

    const snapshot = await this.db
      .collection("users")
      .doc(this.userId)
      .collection("periods")
      .where("startDate", ">=", from)
      .where("startDate", "<=", to)
      .orderBy("startDate", "desc")
      .get();

    return snapshot.docs.map((doc) => {
      const data = doc.data();
      return {
        pk: doc.id,
        startDate: this.toLocalDate(data.startDate.toDate()),
        endDate: this.toLocalDate(data.endDate.toDate()),
        isMenstruation: data.isMenstruation ?? true,
        isDeleted: data.isDeleted ?? false,
      };
    });
  }

  /**
   * 생리 설정 가져오기
   */
  async getPeriodSettings(): Promise<PeriodSettings> {
    const doc = await this.db
      .collection("users")
      .doc(this.userId)
      .collection("settings")
      .doc("period")
      .get();

    if (doc.exists) {
      const data = doc.data()!;
      return {
        period: data.period ?? 28,
        days: data.days ?? 5,
        isRegularCycle: data.isRegularCycle ?? true,
      };
    }

    // 기본값 반환
    return {
      period: 28,
      days: 5,
      isRegularCycle: true,
    };
  }

  /**
   * 배란 테스트 결과 가져오기
   */
  async getOvulationTests(
    fromDate: LocalDate,
    toDate: LocalDate,
  ): Promise<OvulationTest[]> {
    const from = this.toDate(fromDate);
    const to = this.toDate(toDate);

    const snapshot = await this.db
      .collection("users")
      .doc(this.userId)
      .collection("ovulationTests")
      .where("testDate", ">=", from)
      .where("testDate", "<=", to)
      .orderBy("testDate", "asc")
      .get();

    return snapshot.docs.map((doc) => {
      const data = doc.data();
      return {
        pk: doc.id,
        testDate: this.toLocalDate(data.testDate.toDate()),
        isPositive: data.isPositive ?? false,
        isDeleted: data.isDeleted ?? false,
      };
    });
  }

  /**
   * 사용자 입력 배란일 가져오기
   */
  async getUserOvulationDays(
    fromDate: LocalDate,
    toDate: LocalDate,
  ): Promise<OvulationDay[]> {
    const from = this.toDate(fromDate);
    const to = this.toDate(toDate);

    const snapshot = await this.db
      .collection("users")
      .doc(this.userId)
      .collection("ovulationDays")
      .where("date", ">=", from)
      .where("date", "<=", to)
      .orderBy("date", "asc")
      .get();

    return snapshot.docs.map((doc) => {
      const data = doc.data();
      return {
        pk: doc.id,
        date: this.toLocalDate(data.date.toDate()),
        isDeleted: data.isDeleted ?? false,
      };
    });
  }

  /**
   * 피임약 패키지 가져오기
   */
  async getPillPackages(): Promise<PillPackage[]> {
    const snapshot = await this.db
      .collection("users")
      .doc(this.userId)
      .collection("pillPackages")
      .where("isDeleted", "==", false)
      .orderBy("startDate", "desc")
      .get();

    return snapshot.docs.map((doc) => {
      const data = doc.data();
      return {
        pk: doc.id,
        startDate: this.toLocalDate(data.startDate.toDate()),
        endDate: data.endDate ? this.toLocalDate(data.endDate.toDate()) : null,
        totalPills: data.totalPills ?? 21,
        activePills: data.activePills ?? 21,
        isDeleted: data.isDeleted ?? false,
      };
    });
  }

  /**
   * 피임약 설정 가져오기
   */
  async getPillSettings(): Promise<PillSettings> {
    const doc = await this.db
      .collection("users")
      .doc(this.userId)
      .collection("settings")
      .doc("pill")
      .get();

    if (doc.exists) {
      const data = doc.data()!;
      return {
        isCalculatingWithPill: data.isCalculatingWithPill ?? false,
        reminderEnabled: data.reminderEnabled ?? false,
      };
    }

    // 기본값 반환
    return {
      isCalculatingWithPill: false,
      reminderEnabled: false,
    };
  }

  /**
   * 활성 임신 정보 가져오기
   */
  async getActivePregnancy(): Promise<PregnancyInfo | null> {
    const snapshot = await this.db
      .collection("users")
      .doc(this.userId)
      .collection("pregnancies")
      .where("isEnded", "==", false)
      .where("isDeleted", "==", false)
      .limit(1)
      .get();

    if (snapshot.empty) {
      return null;
    }

    const doc = snapshot.docs[0];
    const data = doc.data();

    return {
      pk: doc.id,
      startsDate: this.toLocalDate(data.startsDate.toDate()),
      endsDate: data.endsDate ? this.toLocalDate(data.endsDate.toDate()) : null,
      isEnded: data.isEnded ?? false,
      isMiscarriage: data.isMiscarriage ?? false,
      isDeleted: data.isDeleted ?? false,
    };
  }

  // Helper methods
  private toDate(localDate: LocalDate): Date {
    return new Date(
      localDate.year,
      localDate.monthNumber - 1,
      localDate.dayOfMonth,
    );
  }

  private toLocalDate(date: Date): LocalDate {
    return {
      year: date.getFullYear(),
      monthNumber: date.getMonth() + 1,
      dayOfMonth: date.getDate(),
    } as LocalDate;
  }
}

/**
 * Firebase Functions 사용 예시
 */
export async function calculatePeriodWithRepository(
  userId: string,
  fromDate: Date,
  toDate: Date,
) {
  // 1. Repository 생성
  const repository = new FirestorePeriodRepository(userId);

  // 2. Date를 LocalDate로 변환
  const from: LocalDate = {
    year: fromDate.getFullYear(),
    monthNumber: fromDate.getMonth() + 1,
    dayOfMonth: fromDate.getDate(),
  } as LocalDate;

  const to: LocalDate = {
    year: toDate.getFullYear(),
    monthNumber: toDate.getMonth() + 1,
    dayOfMonth: toDate.getDate(),
  } as LocalDate;

  // 3. KMP 라이브러리 호출 (Repository 전달)
  const result = await calculateMenstrualCyclesWithRepository(
    repository,
    from,
    to,
  );

  return result;
}
