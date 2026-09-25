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
  // Ejercita el bloque de nota del día en la sección de comidas (FOR-728 D8).
  notes: 'Running 4-5 km',
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
      // Sin `id` el contador de "completadas" no podía casar esta comida con
      // `NUTRITION_CONSUMPTION.plannedMeals` y se quedaba siempre en cero bajo
      // fixtures (FOR-728, bug preexistente). Comparte id con `plannedMeals[0]`.
      id: 'm1',
      mealType: 'BREAKFAST',
      name: 'Desayuno',
      preferredTime: '08:00',
      optional: false,
      instructions: 'avena remojada la noche anterior, sin azúcar añadido',
      totals: { calories: 296, proteinG: 10.4, carbsG: 48, fatG: 5.6 },
      items: [{ food: 'Avena', quantityG: 80, preparationNotes: 'con canela' }],
    },
    {
      id: 'm2',
      mealType: 'LUNCH',
      name: 'Comida',
      preferredTime: '14:00',
      optional: false,
      totals: { calories: 330, proteinG: 62, carbsG: 0, fatG: 7.2 },
      // Un ítem sin resolver: el catálogo perdió el alimento, pero el resto del
      // día se sigue mostrando (FOR-728 D5).
      items: [
        { food: 'Pollo', quantityG: 200 },
        { food: 'lost-food-id', quantityG: 0, unresolved: 'lost-food-id' },
      ],
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

/**
 * The caller's plans (V53/V54), headers only — mirrors
 * `NutritionPlanResponse.summary()` (backend/.../delivery/plan), which the
 * list endpoint returns: `days: []`, since the list never resolves a plan's
 * days against the catalog (`PlansPage.tsx` fetches one plan's days only when
 * it is opened). One ACTIVE plan and one DRAFT, so both `Badge` tones and the
 * "Seguir este" action (hidden on the active plan only) render.
 */
const NUTRITION_PLANS = [
  {
    id: '00000000-0000-4000-8000-0000000000a1',
    name: 'Recomposición 12 semanas',
    description: 'Corredor, dos días de fuerza. Ajuste -20% sobre mantenimiento.',
    objective: 'WEIGHT_LOSS',
    status: 'ACTIVE',
    active: true,
    startDate: '2026-07-06',
    endDate: '2026-09-28',
    targets: { kcalMin: 2200, kcalMax: 2400, proteinG: 160, carbsG: 250, fatG: 70 },
    generation: { by: 'AI', prompt: 'Plan de recomposición para corredor amateur', metadata: null },
    days: [],
  },
  {
    id: '00000000-0000-4000-8000-0000000000a2',
    name: 'Mantenimiento — borrador',
    description: 'Ajuste para la semana de descarga.',
    objective: null,
    status: 'DRAFT',
    active: false,
    startDate: null,
    endDate: null,
    targets: { kcalMin: null, kcalMax: null, proteinG: null, carbsG: null, fatG: null },
    generation: { by: 'HUMAN', prompt: null, metadata: null },
    days: [],
  },
];

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
  /*
   * El embudo público (FOR-190). Las cifras son las del mockup: hombre de 45
   * años, 75 kg, 182 cm, moderado, con un objetivo que ajusta un -20 %.
   */
  [
    '/api/v1/public/plan-generator/energy-requirement',
    {
      basalKcal: 1668,
      activityFactor: 1.55,
      dailyKcal: 2585,
      objectiveFactor: 0.8,
      planKcal: 2068,
    },
  ],
  ['/api/v1/public/plan-generator', { email: 'e2e@forma.test', planKcal: 2068, mealsPerDay: 5 }],
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
      // feat/onboarding-primera-vez: AppShell mounts OnboardingGate, which reads
      // this flag and redirects to /onboarding when it is false. Fixed true here
      // so the fixture-backed app keeps landing on /app — the layout specs are
      // about the app screens, not the first-run flow.
      firstRunCompleted: true,
    },
  ],
  ['/api/v1/body/measurements', MEASUREMENTS],
  /*
   * ADR-015 slice 3: the onboarding wizard's final submission. `firstRunCompleted:
   * true` above means the fixture-backed app never actually reaches /onboarding on
   * its own, but a fixture-backed dev session that clears that flag by hand (or a
   * future layout check that does) must not hit the "no fixture" 404 path — see the
   * note on `fixtureFor` below. `/current` answers the same shape a freshly-created
   * PENDING request would have, matched independently of the POST above by its own,
   * longer pathname.
   */
  [
    '/api/v1/plan-requests',
    {
      id: '00000000-0000-4000-8000-000000000001',
      status: 'PENDING',
      mainGoal: 'COMPOSICION',
      planObjective: 'WEIGHT_LOSS',
      planKcal: 2078,
      trainingDaysPerWeek: 0,
      trainingWeekdays: [],
      equipment: [],
      mealsPerDay: 5,
      dietPattern: 'UNSPECIFIED',
      cuisineStyle: 'UNSPECIFIED',
      requestedAt: daysAgo(0),
    },
  ],
  [
    '/api/v1/plan-requests/current',
    {
      id: '00000000-0000-4000-8000-000000000001',
      status: 'PENDING',
      mainGoal: 'COMPOSICION',
      planObjective: 'WEIGHT_LOSS',
      planKcal: 2078,
      trainingDaysPerWeek: 0,
      trainingWeekdays: [],
      equipment: [],
      mealsPerDay: 5,
      dietPattern: 'UNSPECIFIED',
      cuisineStyle: 'UNSPECIFIED',
      requestedAt: daysAgo(0),
    },
  ],
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
        severity: 'ACTION',
        message:
          'La grasa corporal sube varias semanas seguidas; recorta unas 100 kcal al día como ajuste mínimo.',
        reason:
          'La grasa corporal sube de forma sostenida durante 2 semanas seguidas (semana 10: 14.7% → semana 11: 15.3% → semana 12: 15.9%).',
        createdAt: '2026-07-26T08:00:00Z',
      },
      secondary: [],
      generatedAt: '2026-07-26T08:00:00Z',
    },
  ],
  ['/api/v1/insights/history', []],
  /*
   * `currentStreakDays` / `longestStreakDays` / `asOf`, which is what the
   * FOR-139 endpoint returns and what `Streak` (src/api/progress.ts) declares.
   * This used to read `{ currentWeeks, bestWeeks }` — a *successful* response of
   * the wrong shape, which is exactly the failure the note at the top of this
   * file warns about: the racha tile rendered its ready state with the numbers
   * missing ("días", "Récord: días") and nothing failed loudly enough to notice.
   */
  [
    '/api/v1/progress/streak',
    { currentStreakDays: 4, longestStreakDays: 12, asOf: daysAgo(0).slice(0, 10) },
  ],
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
  ['/api/v1/nutrition/plans', NUTRITION_PLANS],
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
        {
          id: 's1',
          productId: 'p1',
          productName: 'Atún claro al natural Hacendado',
          category: 'PROTEINAS',
          quantity: 1,
          catalogued: true,
          unit: 'UD',
          servings: 2,
          estimatedCostEur: 1.35,
          checked: false,
        },
        {
          id: 's2',
          productId: 'p2',
          productName: 'Almendra natural Hacendado',
          category: 'GRASAS_Y_ACEITES',
          quantity: 1,
          catalogued: true,
          unit: 'UD',
          servings: 8,
          estimatedCostEur: 2.65,
          checked: false,
        },
        {
          id: 's3',
          productId: 'p3',
          productName: 'Claras de huevo líquidas pasteurizadas',
          category: 'PROTEINAS',
          quantity: 1,
          catalogued: true,
          unit: 'UD',
          servings: 6,
          estimatedCostEur: 2.1,
          checked: false,
        },
        {
          id: 's4',
          productId: 'p4',
          productName: 'Ensalada mezcla 4 estaciones lavada',
          category: 'FRUTAS_Y_VERDURAS',
          quantity: 1,
          catalogued: true,
          unit: 'UD',
          servings: 3,
          estimatedCostEur: 1.8,
          checked: false,
        },
        {
          id: 's5',
          productId: 'p5',
          productName: 'Spaghetti integral Hacendado',
          category: 'CEREALES_Y_LEGUMBRES',
          quantity: 1,
          catalogued: true,
          unit: 'UD',
          servings: 5,
          estimatedCostEur: 1.45,
          checked: false,
        },
        {
          id: 's6',
          productId: 'p6',
          productName: 'Salmón fresco',
          category: 'PROTEINAS',
          quantity: 2,
          catalogued: true,
          unit: 'UD',
          servings: 4,
          estimatedCostEur: 9.98,
          checked: false,
        },
        {
          id: 's7',
          productId: 'p7',
          productName: 'Huevos camperos',
          category: 'LACTEOS_Y_HUEVOS',
          quantity: 6,
          catalogued: true,
          unit: 'UD',
          servings: 6,
          estimatedCostEur: 2.75,
          checked: false,
        },
      ],
      // Sums the items above (FOR-38); monthly is the FOR-152 weekly-times-4.33
      // approximation and the threshold/overThreshold pair mirrors
      // `ShoppingListResponse.Budget` (weeklyThresholdEur < 120 €/sem, FOR-150 rule 6).
      //
      // `monthlyEur` follows `ShoppingBudgetCalculator`'s own rounding rather than
      // being eyeballed: 22.08 × 4.33 = 95.6064, which HALF_UP at scale 2 is 95.61.
      // A fixture that rounds differently from the calculator it stands in for is a
      // trap for whoever later compares the two.
      budget: { weeklyEur: 22.08, monthlyEur: 95.61, weeklyThresholdEur: 120, overThreshold: false },
    },
  ],
];

/** What an endpoint answers: the exact-path table, plus the one rule that cannot be a fixed path. */
export interface FixtureResponse {
  readonly status: number;
  readonly body: unknown;
}

/*
 * The strength prescriptions, mirroring `WorkoutTemplateCatalog.java` and the
 * display names in `V24__exercise_catalog.sql`. The session detail dialog and
 * the "Entrenar" page both read this endpoint, so without it `dev:fixtures`
 * renders their breakdowns as error states.
 */
const WORKOUTS: Record<string, unknown> = {
  PUSH: {
    workoutType: 'PUSH',
    items: [
      {
        exerciseId: 'dumbbell-bench-press',
        exerciseName: 'Press de banca con mancuernas',
        order: 1,
        sets: 4,
        repScheme: 'RANGE',
        repsMin: 8,
        repsMax: 12,
        restSeconds: 90,
        rir: 2,
      },
      {
        exerciseId: 'dumbbell-shoulder-press',
        exerciseName: 'Press de hombro con mancuernas',
        order: 2,
        sets: 3,
        repScheme: 'RANGE',
        repsMin: 8,
        repsMax: 10,
        restSeconds: 90,
        rir: 2,
      },
      {
        exerciseId: 'push-up',
        exerciseName: 'Flexiones',
        order: 3,
        sets: 3,
        repScheme: 'AMRAP',
        restSeconds: 60,
        rir: 1,
      },
      {
        exerciseId: 'lateral-raise',
        exerciseName: 'Elevaciones laterales',
        order: 4,
        sets: 3,
        repScheme: 'RANGE',
        repsMin: 12,
        repsMax: 20,
        restSeconds: 45,
        rir: 2,
      },
      {
        exerciseId: 'plank',
        exerciseName: 'Plancha',
        order: 5,
        sets: 3,
        repScheme: 'TIME_HOLD',
        durationSecondsMin: 45,
        durationSecondsMax: 75,
        restSeconds: 45,
        rir: 2,
      },
    ],
  },
  PULL: {
    workoutType: 'PULL',
    items: [
      {
        exerciseId: 'pull-up',
        exerciseName: 'Dominadas',
        order: 1,
        sets: 4,
        repScheme: 'AMRAP',
        restSeconds: 120,
        rir: 1,
      },
      {
        exerciseId: 'dumbbell-row',
        exerciseName: 'Remo con mancuerna',
        order: 2,
        sets: 4,
        repScheme: 'RANGE',
        repsMin: 8,
        repsMax: 12,
        restSeconds: 90,
        rir: 2,
      },
      {
        exerciseId: 'band-face-pull',
        exerciseName: 'Face pull con banda',
        order: 3,
        sets: 3,
        repScheme: 'RANGE',
        repsMin: 15,
        repsMax: 25,
        restSeconds: 45,
        rir: 2,
      },
      {
        exerciseId: 'biceps-curl',
        exerciseName: 'Curl de bíceps',
        order: 4,
        sets: 3,
        repScheme: 'RANGE',
        repsMin: 10,
        repsMax: 15,
        restSeconds: 60,
        rir: 2,
      },
      {
        exerciseId: 'rear-delt-fly',
        exerciseName: 'Pájaros posteriores',
        order: 5,
        sets: 3,
        repScheme: 'RANGE',
        repsMin: 12,
        repsMax: 20,
        restSeconds: 45,
        rir: 2,
      },
    ],
  },
  LEGS: {
    workoutType: 'LEGS',
    items: [
      {
        exerciseId: 'goblet-squat',
        exerciseName: 'Sentadilla goblet',
        order: 1,
        sets: 4,
        repScheme: 'RANGE',
        repsMin: 10,
        repsMax: 15,
        restSeconds: 90,
        rir: 2,
      },
      {
        exerciseId: 'dumbbell-rdl',
        exerciseName: 'Peso muerto rumano con mancuernas',
        order: 2,
        sets: 4,
        repScheme: 'RANGE',
        repsMin: 8,
        repsMax: 12,
        restSeconds: 90,
        rir: 2,
      },
      {
        exerciseId: 'reverse-lunge',
        exerciseName: 'Zancada hacia atrás',
        order: 3,
        sets: 3,
        repScheme: 'RANGE',
        repsMin: 10,
        repsMax: 12,
        restSeconds: 90,
        rir: 2,
      },
      {
        exerciseId: 'calf-raise',
        exerciseName: 'Elevación de gemelos',
        order: 4,
        sets: 4,
        repScheme: 'RANGE',
        repsMin: 15,
        repsMax: 25,
        restSeconds: 45,
        rir: 1,
      },
      {
        exerciseId: 'dead-bug',
        exerciseName: 'Dead bug',
        order: 5,
        sets: 3,
        repScheme: 'RANGE',
        repsMin: 10,
        repsMax: 15,
        restSeconds: 45,
        rir: 2,
      },
    ],
  },
};

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
  /* Same shape of problem as the muscle map: the workout type sits in the URL. */
  const workout = /\/training\/workouts\/([^/]+)$/.exec(pathname);
  if (workout) {
    const type = decodeURIComponent(workout[1]).toUpperCase();
    const found = WORKOUTS[type];
    return found
      ? { status: 200, body: found }
      : { status: 404, body: { message: `No workout template for ${type}` } };
  }

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
