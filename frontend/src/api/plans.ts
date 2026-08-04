/**
 * Nutrition plans (V53/V54), on the shared {@link apiClient} boundary (ADR-006 —
 * no ad-hoc `fetch`).
 *
 * <p>A plan carries no nutrition of its own. Every figure below marked as
 * computed — `grams`, `totals`, `effectiveTargets` — is worked out on the server
 * against today's food catalog on each request, so correcting a food moves every
 * plan that uses it. Nothing here sends those back: a body offering them would
 * be offering numbers the server has to ignore.
 *
 * <p>Note the two sides of an item. `foodId`/`servingId`/`amount` are what the
 * plan SAYS — "one medium banana" — and `label`/`grams`/`totals` are what that
 * COMES TO. The editor puts the first back; the screen shows the second.
 *
 * <p>Unlike foods and recipes, these are not shared reference data: a plan is
 * somebody's own diet and every call is scoped to the signed-in account.
 */
import { apiClient, type ApiClient } from './client';

const PLANS_PATH = '/api/v1/nutrition/plans';

export type PlanStatus = 'DRAFT' | 'ACTIVE' | 'COMPLETED' | 'ARCHIVED';

export type DayType = 'RUNNING' | 'STRENGTH' | 'REST';

export type MealTypeCode =
  'BREAKFAST' | 'MID_MORNING' | 'LUNCH' | 'SNACK' | 'PRE_WORKOUT' | 'POST_WORKOUT' | 'DINNER';

/**
 * What something was asked to hit. Every field may be `null`, and that is not a
 * target of zero: it means nobody decided one, and the day falls back to the
 * plan and then to the profile.
 */
export interface PlanMacros {
  readonly calories: number | null;
  readonly proteinG: number | null;
  readonly carbsG: number | null;
  readonly fatG: number | null;
}

/** What something comes to. Always present, always computed. */
export interface PlanTotals {
  readonly calories: number;
  readonly proteinG: number;
  readonly carbsG: number;
  readonly fatG: number;
}

export interface PlanItem {
  /** Set when the line names a food; absent when it names a dish. */
  readonly foodId?: string;
  readonly recipeId?: string;
  /** Which portion `amount` counts. Absent means `amount` is grams. */
  readonly servingId?: string;
  /** How much, in the unit the fields above name. */
  readonly amount: number;
  readonly preparationNotes?: string;
  readonly optional: boolean;
  /** Computed: the food's or dish's name. */
  readonly label?: string;
  /** Computed: what the amount works out to. */
  readonly grams: number;
  readonly totals?: PlanTotals;
  /** Computed: the id that could not be found. Should always be absent. */
  readonly unresolved?: string;
}

export interface PlanMeal {
  readonly mealType: MealTypeCode;
  readonly name: string;
  readonly scheduledTime?: string;
  readonly optional: boolean;
  /** A meal that is a rule rather than a list ("una proteína y una verdura"). */
  readonly instructions?: string;
  readonly targets: PlanMacros;
  readonly totals?: PlanTotals;
  readonly items: readonly PlanItem[];
}

export interface PlanDay {
  readonly weekNumber: number;
  /** 1 = lunes. The weekday follows from it; it is not stored separately. */
  readonly dayNumber: number;
  readonly dayType?: DayType;
  /** Computed from the plan's start date; absent while it is a template. */
  readonly date?: string;
  readonly notes?: string;
  /** What this day was asked to hit, as stored — nulls and all. */
  readonly targets: PlanMacros;
  /** Computed: the same after falling back to the plan and the profile. */
  readonly effectiveTargets?: PlanMacros;
  readonly totals?: PlanTotals;
  readonly meals: readonly PlanMeal[];
}

export interface PlanTargets {
  readonly kcalMin: number | null;
  readonly kcalMax: number | null;
  readonly proteinG: number | null;
  readonly carbsG: number | null;
  readonly fatG: number | null;
}

export interface NutritionPlan {
  readonly id: string;
  readonly name: string;
  readonly description?: string;
  readonly objective?: string;
  readonly status: PlanStatus;
  /** Whether this is the plan being followed. At most one per account. */
  readonly active: boolean;
  readonly startDate?: string;
  readonly endDate?: string;
  readonly targets: PlanTargets;
  readonly generation: {
    readonly by: string;
    readonly prompt?: string;
    readonly metadata?: string;
  };
  readonly days: readonly PlanDay[];
}

/** What a plan looks like before it exists. No totals: those are the answer, not the ask. */
export interface NewPlan {
  readonly name: string;
  readonly description?: string;
  readonly startDate?: string;
  readonly endDate?: string;
  readonly targets?: Partial<PlanTargets>;
  readonly days: readonly NewPlanDay[];
}

export interface NewPlanDay {
  readonly weekNumber: number;
  readonly dayNumber: number;
  readonly dayType?: DayType;
  readonly targets?: Partial<PlanMacros>;
  readonly notes?: string;
  readonly meals: readonly NewPlanMeal[];
}

export interface NewPlanMeal {
  readonly mealType: MealTypeCode;
  readonly name: string;
  readonly scheduledTime?: string;
  readonly optional?: boolean;
  readonly instructions?: string;
  readonly items: readonly NewPlanItem[];
}

export interface NewPlanItem {
  readonly foodId?: string;
  readonly recipeId?: string;
  readonly servingId?: string;
  readonly amount: number;
  readonly optional?: boolean;
}

/** The signed-in account's plans, newest first. Headers only — no days. */
export function listPlans(client: ApiClient = apiClient): Promise<NutritionPlan[]> {
  return client.request<NutritionPlan[]>(PLANS_PATH);
}

/** One plan with its days worked out. */
export function getPlan(id: string, client: ApiClient = apiClient): Promise<NutritionPlan> {
  return client.request<NutritionPlan>(`${PLANS_PATH}/${encodeURIComponent(id)}`);
}

/** Adds a plan. It is created as a draft whatever it asks for; activating is its own call. */
export function createPlan(plan: NewPlan, client: ApiClient = apiClient): Promise<NutritionPlan> {
  return client.request<NutritionPlan>(PLANS_PATH, { method: 'POST', body: JSON.stringify(plan) });
}

/** Replaces the plan's contents whole, leaving its status where it was. */
export function updatePlan(
  id: string,
  plan: NewPlan,
  client: ApiClient = apiClient,
): Promise<NutritionPlan> {
  return client.request<NutritionPlan>(`${PLANS_PATH}/${encodeURIComponent(id)}`, {
    method: 'PUT',
    body: JSON.stringify(plan),
  });
}

/**
 * Makes this the plan being followed. Whatever was being followed becomes
 * COMPLETED — the server does both in one step, so there is never a moment with
 * no active plan.
 */
export function activatePlan(id: string, client: ApiClient = apiClient): Promise<NutritionPlan> {
  return client.request<NutritionPlan>(`${PLANS_PATH}/${encodeURIComponent(id)}/activation`, {
    method: 'POST',
  });
}

/** Moves a plan to DRAFT, COMPLETED or ARCHIVED. Not ACTIVE — that is {@link activatePlan}. */
export function changePlanStatus(
  id: string,
  status: Exclude<PlanStatus, 'ACTIVE'>,
  client: ApiClient = apiClient,
): Promise<NutritionPlan> {
  return client.request<NutritionPlan>(`${PLANS_PATH}/${encodeURIComponent(id)}/status`, {
    method: 'PUT',
    body: JSON.stringify({ status }),
  });
}

/** Removes a plan and everything under it. */
export function deletePlan(id: string, client: ApiClient = apiClient): Promise<void> {
  return client.request<void>(`${PLANS_PATH}/${encodeURIComponent(id)}`, { method: 'DELETE' });
}
