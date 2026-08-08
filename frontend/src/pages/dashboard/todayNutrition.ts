import type { DayConsumption, NutritionDay } from '../../api/nutrition';

/** One dashboard-owned read of today's nutrition data, shared by all three nutrition widgets. */
export type TodayConsumptionState =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | { readonly status: 'ready'; readonly consumption: DayConsumption };

export type TodayMenuState =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | { readonly status: 'empty' }
  | { readonly status: 'ready'; readonly day: NutritionDay };
