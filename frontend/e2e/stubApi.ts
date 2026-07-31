import type { Page } from '@playwright/test';

/**
 * Serves the whole API from fixtures so the layout checks need no backend and
 * render the same page on every run.
 *
 * <p>Only the shape matters here: these checks assert geometry and computed
 * style, so a fixture needs to be *plausible and stable*, not correct. What it
 * must not be is empty — an empty dashboard is all empty states, which is
 * precisely the layout that never breaks. Several measurements over several
 * days is what puts a real chart, long labels and a full card grid on screen.
 *
 * <p>Anything not matched below resolves to an empty object, so a widget whose
 * endpoint nobody thought to stub renders its own error or empty state instead
 * of hanging the page on a pending request.
 */

/**
 * Dated relative to the run, one per day ending yesterday — not on fixed dates.
 * The dashboard's trend card windows on the last 30 days, so a fixture pinned to
 * July would quietly fall out of that window as real time passed and the chart
 * would stop rendering, failing checks that have nothing to do with dates.
 */
const daysAgo = (days: number): string => {
  const date = new Date();
  date.setUTCDate(date.getUTCDate() - days);
  date.setUTCHours(7, 30, 0, 0);
  return date.toISOString();
};

const MEASUREMENTS = [
  { measuredAt: daysAgo(1), weightKg: 74.0, bodyFatPercentage: 15.0, bmi: 22.5 },
  { measuredAt: daysAgo(2), weightKg: 73.8, bodyFatPercentage: 15.1, bmi: 22.4 },
  { measuredAt: daysAgo(3), weightKg: 73.7, bodyFatPercentage: 15.3, bmi: 22.4 },
  { measuredAt: daysAgo(4), weightKg: 73.4, bodyFatPercentage: 15.6, bmi: 22.3 },
  { measuredAt: daysAgo(5), weightKg: 73.3, bodyFatPercentage: 15.4, bmi: 22.3 },
  { measuredAt: daysAgo(6), weightKg: 73.3, bodyFatPercentage: 15.9, bmi: 22.3 },
  { measuredAt: daysAgo(7), weightKg: 73.0, bodyFatPercentage: 16.0, bmi: 22.2 },
].map((m, index) => ({
  ...m,
  // The list endpoint carries the stored row's id (FOR-187); the delete action
  // in the history table is disabled without it.
  id: `00000000-0000-4000-8000-00000000000${index}`,
  source: 'MANUAL',
  fatMassKg: Number((m.weightKg * (m.bodyFatPercentage / 100)).toFixed(1)),
  leanMassKg: Number((m.weightKg * (1 - m.bodyFatPercentage / 100)).toFixed(1)),
}));

const NUTRITION_DAY = {
  type: 'running',
  targets: { calories: 2300, proteinG: 160, carbsG: 250, fatG: 70 },
  meals: [
    {
      mealType: 'BREAKFAST',
      name: 'Desayuno',
      preferredTime: '08:00',
      optional: false,
      items: [{ food: 'Avena', quantityG: 80 }],
    },
    {
      mealType: 'LUNCH',
      name: 'Comida',
      preferredTime: '14:00',
      optional: false,
      items: [{ food: 'Pollo', quantityG: 200 }],
    },
  ],
};

/** Endpoint path → response body. Matched exactly on the pathname. */
const FIXTURES: ReadonlyArray<readonly [string, unknown]> = [
  ['/api/v1/auth/me', { id: 'e2e-user', email: 'e2e@forma.test' }],
  [
    '/api/v1/profile',
    {
      name: 'Diego',
      email: 'e2e@forma.test',
      unitPreferences: {
        weightUnit: 'KG',
        heightUnit: 'CM',
        distanceUnit: 'KM',
        energyUnit: 'KCAL',
      },
      themeMode: 'DARK',
    },
  ],
  ['/api/v1/body/measurements', MEASUREMENTS],
  [
    '/api/v1/insights/weekly',
    {
      checkIn: {
        weekStartDate: '2026-07-20',
        latestWeightKg: 74.0,
        latestBodyFatPercentage: 15.0,
        latestLeanMassKg: 62.9,
        plannedRunningSessions: 3,
        completedRunningSessions: 2,
        plannedStrengthSessions: 2,
        completedStrengthSessions: 2,
      },
      main: {
        category: 'BODY',
        severity: 'INFO',
        message: 'Mantienes el peso con una ligera bajada de grasa corporal.',
        reason: 'Siete mediciones en la última semana con tendencia estable.',
        createdAt: '2026-07-26T08:00:00Z',
      },
      secondary: [],
      generatedAt: '2026-07-26T08:00:00Z',
    },
  ],
  ['/api/v1/insights/history', []],
  ['/api/v1/progress/streak', { currentWeeks: 3, bestWeeks: 5 }],
  ['/api/v1/progress/weekly-history', { weeks: [] }],
  ['/api/v1/progress/photos', { photos: [] }],
  // `{ goals: [...] }`, not a bare array — and a goal rather than none, so the
  // goals page renders its list layout (its empty state hides half the page).
  [
    '/api/v1/goals',
    {
      goals: [
        {
          id: 'g1',
          title: 'Bajar a 12% de grasa corporal',
          metric: 'BODY_FAT_PCT',
          target: 12,
          dueDate: '2026-12-31',
          status: 'ACTIVE',
          progress: { current: 15.0, target: 12, ratio: 0.42, source: 'BODY_MEASUREMENT' },
          milestones: [{ id: 'm1', label: '15%', target: 15, completed: true }],
        },
      ],
    },
  ],
  ['/api/v1/nutrition/days/running', NUTRITION_DAY],
  // All three provider rows the backend knows about, so the settings checks
  // see what the UI does with the two FORMA does not offer yet.
  [
    '/api/v1/integrations',
    {
      providers: [
        {
          provider: 'WITHINGS',
          status: 'DISCONNECTED',
          connectedAt: null,
          lastSyncAt: null,
          lastSyncOutcome: null,
        },
        {
          provider: 'GOOGLE_FIT',
          status: 'DISCONNECTED',
          connectedAt: null,
          lastSyncAt: null,
          lastSyncOutcome: null,
        },
        {
          provider: 'APPLE_HEALTH',
          status: 'DISCONNECTED',
          connectedAt: null,
          lastSyncAt: null,
          lastSyncOutcome: null,
        },
      ],
    },
  ],
  ['/api/v1/training/week', { days: [] }],
  [
    '/api/v1/shopping/list',
    {
      weekStartDate: '2026-07-20',
      status: 'ACTIVE',
      generatedAt: '2026-07-20T06:00:00Z',
      items: [
        { id: 's1', productName: 'Pechuga de pollo', quantity: '1.2 kg', checked: false },
        { id: 's2', productName: 'Arroz integral', quantity: '1 kg', checked: false },
      ],
      budget: { estimatedTotal: 48.5, currency: 'EUR' },
    },
  ],
];

export async function stubApi(page: Page): Promise<void> {
  // Matched on the versioned prefix, not `**/api/**`: in dev the app's own
  // source modules are served from `/src/api/…`, and a looser glob answers
  // those with JSON too — the page then loads no JavaScript at all.
  await page.route('**/api/v1/**', async (route) => {
    const path = new URL(route.request().url()).pathname;
    const match = FIXTURES.find(([fixturePath]) => path === fixturePath);

    // An unstubbed endpoint answers 404 rather than `{}`: a widget handles a
    // failed request by rendering its error state, but an empty object is a
    // *successful* response of the wrong shape, and reading a field off it
    // throws and takes the whole page down.
    await route.fulfill({
      status: match ? 200 : 404,
      contentType: 'application/json',
      body: JSON.stringify(match ? match[1] : { message: `No e2e fixture for ${path}` }),
    });
  });
}
