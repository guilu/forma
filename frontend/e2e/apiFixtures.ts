/**
 * The whole API as fixtures, so the app can be driven with no backend running.
 *
 * <p>Two consumers, one set of data: `stubApi` intercepts requests in the
 * Playwright browser for the layout checks and the playground, and the dev
 * server's `devApiFixtures` plugin answers the same paths over HTTP so the
 * signed-in app is browsable from `npm run dev:fixtures` in an ordinary
 * browser, at any window size. Neither owns the fixtures; both ask this module.
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

/**
 * A day of the plan.
 *
 * <p>`totals` and `targetComparison` are not optional decoration: the API has returned them since
 * FOR-105 and the page reads them. A fixture missing them is a *successful* response of the wrong
 * shape, which is precisely the failure the note on `stubApi` warns about — reading a field off it
 * throws and takes the whole page down, h1 and all.
 */
const NUTRITION_DAY = {
  type: 'running',
  targets: { calories: 2300, proteinG: 160, carbsG: 250, fatG: 70 },
  totals: { calories: 2010, proteinG: 148, carbsG: 232, fatG: 61 },
  targetComparison: {
    caloriesReached: false,
    proteinReached: false,
    carbsReached: false,
    fatReached: false,
  },
  meals: [
    {
      mealType: 'BREAKFAST',
      name: 'Desayuno',
      preferredTime: '08:00',
      optional: false,
      totals: { calories: 296, proteinG: 10.4, carbsG: 48, fatG: 5.6 },
      items: [{ food: 'Avena', quantityG: 80 }],
    },
    {
      mealType: 'LUNCH',
      name: 'Comida',
      preferredTime: '14:00',
      optional: false,
      totals: { calories: 330, proteinG: 62, carbsG: 0, fatG: 7.2 },
      items: [{ food: 'Pollo', quantityG: 200 }],
    },
  ],
};

/** What has been eaten today (FOR-127/FOR-134), read by the meal log and the key-nutrient card. */
const NUTRITION_CONSUMPTION = {
  date: '2026-08-04',
  dayType: 'RUNNING',
  consumed: { kcal: 626, proteinG: 72.4, carbsG: 48, fatG: 12.8 },
  // Two known and two unknown, so the screen's «Sin datos» path is exercised rather than assumed.
  keyNutrients: { fiberG: 8.5, sugarsG: null, sodiumMg: 320, saturatedFatG: null },
  target: { kcal: 2300, proteinG: 160, carbsG: 250, fatG: 70 },
  comparison: {
    caloriesReached: false,
    proteinReached: false,
    carbsReached: false,
    fatReached: false,
  },
  entries: [{ id: 'e1', mealType: 'BREAKFAST', name: 'Avena', kcal: 296 }],
  plannedMeals: [
    { id: 'm1', mealType: 'BREAKFAST', name: 'Desayuno', optional: false, state: 'EATEN' },
    { id: 'm2', mealType: 'LUNCH', name: 'Comida', optional: false, state: 'PENDING' },
  ],
};

/*
 * A week with something on most days, so the playground has an app to walk
 * through and the layout checks measure a card with real content in it. It used
 * to be `{ days: [] }`, which renders the "no plan at all" empty state on every
 * training screen.
 *
 * Session ids follow the backend's `DAY:KIND` shape, which is what the muscle
 * map below is keyed on.
 */
const TRAINING_WEEK = {
  days: [
    {
      dayOfWeek: 'MONDAY',
      rest: false,
      sessions: [
        {
          id: 'MONDAY:RUNNING',
          kind: 'RUNNING',
          bodyView: 'FRONT',
          title: 'Carrera · Rodaje suave',
          detail: '5.0 km',
          status: 'COMPLETED',
        },
      ],
    },
    {
      dayOfWeek: 'TUESDAY',
      rest: false,
      sessions: [
        {
          id: 'TUESDAY:STRENGTH',
          kind: 'STRENGTH',
          bodyView: 'FRONT',
          title: 'Fuerza · Empuje',
          detail: '5 ejercicios',
          status: 'COMPLETED',
          workoutType: 'PUSH',
        },
      ],
    },
    {
      dayOfWeek: 'WEDNESDAY',
      rest: false,
      sessions: [
        {
          id: 'WEDNESDAY:RUNNING',
          kind: 'RUNNING',
          bodyView: 'FRONT',
          title: 'Carrera · Series',
          detail: '6 x 400 m',
          status: 'PLANNED',
        },
      ],
    },
    {
      dayOfWeek: 'THURSDAY',
      rest: false,
      sessions: [
        {
          id: 'THURSDAY:STRENGTH',
          kind: 'STRENGTH',
          bodyView: 'BACK',
          title: 'Fuerza · Tirón',
          detail: '5 ejercicios',
          status: 'PLANNED',
          workoutType: 'PULL',
        },
      ],
    },
    { dayOfWeek: 'FRIDAY', rest: true, sessions: [] },
    {
      dayOfWeek: 'SATURDAY',
      rest: false,
      sessions: [
        {
          id: 'SATURDAY:RUNNING',
          kind: 'RUNNING',
          bodyView: 'FRONT',
          title: 'Carrera · Tirada larga',
          detail: '12.0 km',
          status: 'PLANNED',
        },
      ],
    },
    {
      dayOfWeek: 'SUNDAY',
      rest: false,
      sessions: [
        {
          id: 'SUNDAY:STRENGTH',
          kind: 'STRENGTH',
          bodyView: 'FRONT',
          title: 'Fuerza · Pierna y core',
          detail: '5 ejercicios',
          status: 'PLANNED',
          workoutType: 'LEGS',
        },
      ],
    },
  ],
};

/*
 * Worked muscles per strength session, in the catalog's own vocabulary
 * (lowercase, accented Spanish) — the shape the real endpoint answers with.
 * Running and rest days have no entry: their map is legitimately empty.
 */
const MUSCLE_MAPS: Record<string, readonly { muscle: string; load: string }[]> = {
  'TUESDAY:STRENGTH': [
    { muscle: 'pecho', load: 'HIGH' },
    { muscle: 'tríceps', load: 'HIGH' },
    { muscle: 'hombro anterior', load: 'MEDIUM' },
    { muscle: 'core', load: 'LOW' },
  ],
  'THURSDAY:STRENGTH': [
    { muscle: 'dorsal', load: 'HIGH' },
    { muscle: 'bíceps', load: 'HIGH' },
    { muscle: 'romboides', load: 'MEDIUM' },
    { muscle: 'deltoides posterior', load: 'MEDIUM' },
    { muscle: 'trapecio', load: 'LOW' },
  ],
  'SUNDAY:STRENGTH': [
    { muscle: 'cuádriceps', load: 'HIGH' },
    { muscle: 'glúteo', load: 'HIGH' },
    { muscle: 'isquiotibiales', load: 'HIGH' },
    { muscle: 'gemelos', load: 'MEDIUM' },
    { muscle: 'core', load: 'MEDIUM' },
    { muscle: 'abdomen', load: 'LOW' },
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
  // Matched on the pathname, so the `?date=` the page sends does not have to be guessed here.
  ['/api/v1/nutrition/consumption', NUTRITION_CONSUMPTION],
  // The water tile reads this for real since FOR-130's endpoints got a caller. Without a fixture it
  // renders its failure state, which is honest but not what these layout checks are looking at.
  [
    '/api/v1/nutrition/hydration',
    { date: '2026-08-04', totalMl: 1500, goalMl: 2500, progress: 0.6 },
  ],
  ['/api/v1/foods', []],
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
  ['/api/v1/training/week', TRAINING_WEEK],
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

/** What an endpoint answers: the exact-path table, plus the one rule that cannot be a fixed path. */
export interface FixtureResponse {
  readonly status: number;
  readonly body: unknown;
}

/**
 * Resolves a request path to its fixture.
 *
 * <p>An unstubbed endpoint answers 404 rather than `{}`: a widget handles a
 * failed request by rendering its error state, but an empty object is a
 * *successful* response of the wrong shape, and reading a field off it throws
 * and takes the whole page down.
 */
export function fixtureFor(pathname: string): FixtureResponse {
  /*
   * The muscle map is per session, so it cannot be a fixed path: the id sits in
   * the middle of the URL and arrives percent-encoded (`SUNDAY%3ASTRENGTH`). A
   * session with no entry answers an empty list, which is exactly what the real
   * endpoint does for a run.
   */
  const muscleMap = /\/training\/sessions\/([^/]+)\/muscle-map$/.exec(pathname);
  if (muscleMap) {
    const sessionId = decodeURIComponent(muscleMap[1]);
    return { status: 200, body: { sessionId, muscles: MUSCLE_MAPS[sessionId] ?? [] } };
  }

  const match = FIXTURES.find(([fixturePath]) => pathname === fixturePath);
  return match
    ? { status: 200, body: match[1] }
    : { status: 404, body: { message: `No fixture for ${pathname}` } };
}
