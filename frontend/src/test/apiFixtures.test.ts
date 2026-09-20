import { describe, expect, it } from 'vitest';
import { fixtureFor } from '../../e2e/apiFixtures';

/**
 * Regression guard for `e2e/apiFixtures.ts`.
 *
 * <p>The note at the top of that file warns that an unmatched/wrong-shaped
 * fixture is a *successful* response of the wrong shape: it passes `fetch`,
 * then throws (or renders `NaN`) the moment the page reads a field the
 * fixture never supplied. That failure mode is invisible to a human clicking
 * through `npm run dev:fixtures` unless they know to look for it, and
 * invisible to the type checker too — fixtures are `unknown` until a real
 * request parses them.
 *
 * <p>This file is the check that would have caught it: one entry per route a
 * page actually reads fields off of, asserting the fixture carries every
 * field that consumer needs. Adding a new consumer, or a new field an
 * existing consumer starts reading, means adding/extending a row below —
 * cheaper than tracing a `NaN €` back to a fixture by hand.
 */

type Route = {
  readonly path: string;
  readonly assert: (body: unknown) => void;
};

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null;
}

function expectFields(
  record: Record<string, unknown>,
  fields: readonly string[],
  context: string,
) {
  for (const field of fields) {
    expect(Object.prototype.hasOwnProperty.call(record, field), `${context} is missing "${field}"`).toBe(
      true,
    );
    expect(record[field], `${context}.${field} must not be undefined`).not.toBeUndefined();
  }
}

const ROUTES: readonly Route[] = [
  {
    // src/api/plans.ts `listPlans()` expects a bare array (NutritionPlan[]),
    // not `{ plans: [...] }` — unlike /api/v1/goals, which really is wrapped.
    path: '/api/v1/nutrition/plans',
    assert(body) {
      expect(Array.isArray(body), 'listPlans() expects a bare array (src/api/plans.ts)').toBe(
        true,
      );
      const plans = body as unknown[];
      expect(
        plans.length,
        'an empty plans fixture only exercises the empty state, never the real list layout',
      ).toBeGreaterThan(0);
      for (const plan of plans) {
        expect(isRecord(plan), 'each plan must be an object').toBe(true);
        expectFields(
          plan as Record<string, unknown>,
          ['id', 'name', 'status', 'active', 'targets', 'generation', 'days'],
          'NutritionPlan',
        );
      }
    },
  },
  {
    path: '/api/v1/shopping/list',
    assert(body) {
      expect(isRecord(body), 'ShoppingList must be an object').toBe(true);
      const list = body as Record<string, unknown>;
      expectFields(
        list,
        ['weekStartDate', 'status', 'items', 'budget', 'generatedAt'],
        'ShoppingList',
      );
      expect(isRecord(list.budget), 'ShoppingList.budget must be an object').toBe(true);
      // ShoppingPage.tsx reads budget.weeklyEur directly (src/api/shopping.ts ShoppingBudget).
      expectFields(list.budget as Record<string, unknown>, ['weeklyEur', 'monthlyEur'], 'ShoppingList.budget');
      expect(Array.isArray(list.items), 'ShoppingList.items must be an array').toBe(true);
      const items = list.items as unknown[];
      expect(items.length, 'an empty items fixture never exercises the real table layout').toBeGreaterThan(0);
      for (const item of items) {
        expect(isRecord(item), 'each shopping item must be an object').toBe(true);
        // Every field ShoppingPage.tsx reads off a ShoppingItem (src/api/shopping.ts):
        // category (grouping/tabs), estimatedCostEur (price cell), catalogued
        // (quantity stepper + edit button gating) plus the rest of the contract.
        expectFields(
          item as Record<string, unknown>,
          [
            'id',
            'productId',
            'productName',
            'category',
            'quantity',
            'catalogued',
            'unit',
            'servings',
            'estimatedCostEur',
            'checked',
          ],
          'ShoppingItem',
        );
      }
    },
  },
];

describe('e2e/apiFixtures.ts contract', () => {
  it.each(ROUTES.map((route) => [route.path, route] as const))(
    '%s matches the real API shape',
    (path, route) => {
      const response = fixtureFor(path);
      expect(response.status, `${path} has no fixture (fixtureFor() 404d)`).toBe(200);
      route.assert(response.body);
    },
  );
});
