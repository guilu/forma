import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { NutritionPage } from './NutritionPage';
import { NotificationProvider } from '../components/NotificationProvider';
import {
  getDayConsumption,
  getNutritionDay,
  logPlannedMealAsPlanned,
  unmarkPlannedMeal,
  type DayConsumption,
  type NutritionDay,
} from '../api/nutrition';

vi.mock('../api/nutrition', () => ({
  getNutritionDay: vi.fn(),
  getDayConsumption: vi.fn(),
  logMeal: vi.fn(),
  logPlannedMealAsPlanned: vi.fn(),
  unmarkPlannedMeal: vi.fn(),
}));

const getDayMock = vi.mocked(getNutritionDay);
const getConsumptionMock = vi.mocked(getDayConsumption);
const logAsPlannedMock = vi.mocked(logPlannedMealAsPlanned);
const unmarkMock = vi.mocked(unmarkPlannedMeal);
const TODAY = new Date('2026-08-07T12:00:00Z');

const strengthDay: NutritionDay = {
  type: 'STRENGTH',
  targets: { calories: 2850, proteinG: 180, carbsG: 320, fatG: 75 },
  totals: { calories: 2850, proteinG: 180, carbsG: 320, fatG: 75 },
  targetComparison: {
    caloriesReached: true,
    proteinReached: true,
    carbsReached: true,
    fatReached: true,
  },
  meals: [
    {
      id: 'meal-desayuno',
      mealType: 'BREAKFAST',
      name: 'Bowl de Yogur Proteico y Fruta',
      preferredTime: '08:00',
      optional: false,
      totals: { calories: 350, proteinG: 30, carbsG: 45, fatG: 8 },
      items: [{ food: 'Yogur griego', quantityG: 200 }],
    },
    {
      id: 'meal-comida',
      mealType: 'LUNCH',
      name: 'Pollo a la Plancha con Boniato',
      preferredTime: '14:00',
      optional: false,
      totals: { calories: 650, proteinG: 55, carbsG: 70, fatG: 12 },
      items: [{ food: 'Pechuga de pollo', quantityG: 180 }],
    },
  ],
};

/**
 * Una variante con el detalle que el corte añade: nota del día, instrucciones en solo una comida, y
 * un ítem sin resolver junto a uno normal, para ejercitar los tres caminos a la vez.
 */
const richDay: NutritionDay = {
  ...strengthDay,
  notes: 'Running 4-5 km',
  meals: [
    {
      id: 'meal-desayuno',
      mealType: 'BREAKFAST',
      name: 'Bowl de Yogur Proteico y Fruta',
      preferredTime: '08:00',
      optional: false,
      instructions: 'una proteína, un carbo y una verdura',
      totals: { calories: 350, proteinG: 30, carbsG: 45, fatG: 8 },
      items: [
        { food: 'Yogur griego', quantityG: 200, preparationNotes: 'sin azúcar' },
        { food: 'lost-food-id', quantityG: 0, unresolved: 'lost-food-id' },
      ],
    },
    {
      id: 'meal-comida',
      mealType: 'LUNCH',
      name: 'Pollo a la Plancha con Boniato',
      preferredTime: '14:00',
      optional: false,
      totals: { calories: 650, proteinG: 55, carbsG: 70, fatG: 12 },
      items: [{ food: 'Pechuga de pollo', quantityG: 180 }],
    },
  ],
};

function consumption(overrides: Partial<DayConsumption> = {}): DayConsumption {
  return {
    date: '2026-08-07',
    dayType: 'STRENGTH',
    consumed: { kcal: 2150, proteinG: 140, carbsG: 250, fatG: 55 },
    keyNutrients: { fiberG: null, sugarsG: null, sodiumMg: null, saturatedFatG: null },
    target: { kcal: 2850, proteinG: 180, carbsG: 320, fatG: 75 },
    comparison: {
      caloriesReached: false,
      proteinReached: false,
      carbsReached: false,
      fatReached: false,
    },
    entries: [],
    plannedMeals: [
      {
        id: 'meal-desayuno',
        mealType: 'BREAKFAST',
        name: 'Bowl de Yogur Proteico y Fruta',
        optional: false,
        state: 'EATEN',
      },
      {
        id: 'meal-comida',
        mealType: 'LUNCH',
        name: 'Pollo a la Plancha con Boniato',
        optional: false,
        state: 'PENDING',
      },
    ],
    ...overrides,
  };
}

/** El formulario de registro usa `useNotify`; en la aplicación el proveedor vive en App.tsx. */
function renderPage() {
  return render(
    <MemoryRouter>
      <NotificationProvider>
        <NutritionPage />
      </NotificationProvider>
    </MemoryRouter>,
  );
}

describe('NutritionPage', () => {
  beforeEach(() => {
    // Mock Date only: RTL polling timers remain real, while every render gets the same "today".
    vi.useFakeTimers({ toFake: ['Date'] });
    vi.setSystemTime(TODAY);
    vi.clearAllMocks();
    getConsumptionMock.mockResolvedValue(consumption());
    getDayMock.mockResolvedValue(strengthDay);
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  /**
   * The day type is not chosen on screen any more: the server resolves today's date to its kind and
   * the plan is asked for THAT. This is what stops the two halves of the page describing different
   * days, which is exactly what the old selector allowed.
   */
  it('asks the plan for the kind of day the server says today is', async () => {
    renderPage();

    await waitFor(() => expect(getDayMock).toHaveBeenCalledWith('strength'));
    expect(screen.queryByRole('radiogroup', { name: 'Tipo de día' })).not.toBeInTheDocument();
  });

  it('shows calories eaten against the target', async () => {
    renderPage();

    // El separador de miles depende del ICU del entorno, así que el matcher lo hace opcional en
    // vez de fijar el del navegador o el del runner.
    expect(await screen.findByRole('img', { name: /2\.?150 de 2\.?850 kcal/ })).toBeInTheDocument();
    // Restantes: lo que falta, no lo que suma el plan.
    expect(screen.getByText(/Te quedan 700 kcal/)).toBeInTheDocument();
  });

  it('shows each macro eaten against its target', async () => {
    renderPage();

    expect(await screen.findByText('140 g')).toBeInTheDocument();
    expect(screen.getByText('/ 180 g')).toBeInTheDocument();
    expect(screen.getByText('250 g')).toBeInTheDocument();
    expect(screen.getByText('/ 320 g')).toBeInTheDocument();
    expect(screen.getByText('55 g')).toBeInTheDocument();
    expect(screen.getByText('/ 75 g')).toBeInTheDocument();
  });

  it('lists the meals with their macros and how many are done', async () => {
    renderPage();

    expect(await screen.findByText('Bowl de Yogur Proteico y Fruta')).toBeInTheDocument();
    expect(screen.getByText('350 kcal')).toBeInTheDocument();
    expect(screen.getByText('30g P')).toBeInTheDocument();
    expect(screen.getByText('1 de 2 completadas')).toBeInTheDocument();
  });

  /**
   * The seeded plan names each meal after its own type — the breakfast is called "Desayuno" — so
   * under the type label the same word came out twice and neither said what there was to eat. When
   * the meal name duplicates the type label, the heading falls back to that label; the type line
   * is hidden to avoid repetition (FOR-728 D7).
   */
  it('falls back the heading to the meal type label when the name only repeats it', async () => {
    getDayMock.mockResolvedValue({
      ...strengthDay,
      meals: [
        {
          id: 'meal-desayuno',
          mealType: 'BREAKFAST',
          name: 'Desayuno',
          preferredTime: '08:00',
          optional: false,
          totals: { calories: 372, proteinG: 22.8, carbsG: 36.4, fatG: 15.2 },
          items: [
            { food: 'Copos de avena', quantityG: 80 },
            { food: 'Plátano', quantityG: 120 },
          ],
        },
      ],
    });
    renderPage();

    // h3 shows the fallback label since the meal name duplicates it (FOR-728 D7).
    expect(await screen.findByRole('heading', { name: 'Desayuno' })).toBeInTheDocument();
    // Items are listed with quantities now, not joined.
    expect(screen.getByText('Copos de avena 80g')).toBeInTheDocument();
    expect(screen.getByText('Plátano 120g')).toBeInTheDocument();
  });

  /**
   * The optional badge used to live inside the type line, and the type line hides whenever the
   * meal name duplicates its type label — silently dropping the badge for a meal like this one
   * (FOR-728 review finding #1). The badge must survive independently of that suppression.
   */
  it('shows the optional badge even when the meal name duplicates its type label', async () => {
    getDayMock.mockResolvedValue({
      ...strengthDay,
      meals: [
        {
          id: 'meal-post',
          mealType: 'POST_WORKOUT',
          name: 'Post-entreno',
          preferredTime: '20:00',
          optional: true,
          totals: { calories: 78, proteinG: 15.6, carbsG: 1.6, fatG: 1.2 },
          items: [{ food: 'Proteína whey', quantityG: 20 }],
        },
      ],
    });
    renderPage();

    expect(await screen.findByRole('heading', { name: 'Post-entreno' })).toBeInTheDocument();
    expect(screen.getByText(/opcional/)).toBeInTheDocument();
  });

  /** A plan that does name its meals keeps the name: somebody wrote it and it says more. */
  it('keeps a meal name that says something the type does not', async () => {
    renderPage();

    expect(
      await screen.findByRole('heading', { name: 'Bowl de Yogur Proteico y Fruta' }),
    ).toBeInTheDocument();
  });

  /** The check reflects the state the server reports and remains interactive for undo. */
  it('ticks the meals already eaten and leaves the rest open', async () => {
    renderPage();

    const eaten = await screen.findByRole('checkbox', {
      name: 'Desmarcar Bowl de Yogur Proteico y Fruta como hecha',
    });
    expect(eaten).toBeChecked();
    expect(eaten).toBeEnabled();

    expect(
      screen.getByRole('checkbox', { name: 'Marcar Pollo a la Plancha con Boniato como hecha' }),
    ).not.toBeChecked();
  });

  it('unmarks an eaten planned meal and reloads persisted consumption', async () => {
    unmarkMock.mockResolvedValue(undefined);
    const user = userEvent.setup();
    renderPage();

    await user.click(
      await screen.findByRole('checkbox', {
        name: 'Desmarcar Bowl de Yogur Proteico y Fruta como hecha',
      }),
    );

    await waitFor(() => expect(unmarkMock).toHaveBeenCalledWith('2026-08-07', 'meal-desayuno'));
    await waitFor(() => expect(getConsumptionMock).toHaveBeenCalledTimes(2));
  });

  it('keeps each meal disabled until its own overlapping request finishes', async () => {
    let resolveBreakfast!: () => void;
    let resolveLunch!: (value: Awaited<ReturnType<typeof logPlannedMealAsPlanned>>) => void;
    unmarkMock.mockReturnValue(new Promise<void>((resolve) => (resolveBreakfast = resolve)));
    logAsPlannedMock.mockReturnValue(
      new Promise((resolve) => {
        resolveLunch = resolve;
      }),
    );
    const user = userEvent.setup();
    renderPage();
    const breakfast = await screen.findByRole('checkbox', {
      name: 'Desmarcar Bowl de Yogur Proteico y Fruta como hecha',
    });
    const lunch = screen.getByRole('checkbox', {
      name: 'Marcar Pollo a la Plancha con Boniato como hecha',
    });

    await user.click(breakfast);
    await user.click(lunch);
    expect(breakfast).toBeDisabled();
    expect(lunch).toBeDisabled();

    resolveBreakfast();
    await waitFor(() => expect(breakfast).toBeEnabled());
    expect(lunch).toBeDisabled();
    await user.click(lunch);
    expect(logAsPlannedMock).toHaveBeenCalledTimes(1);

    resolveLunch({
      id: 'entry-2',
      date: '2026-08-07',
      mealType: 'LUNCH',
      name: 'Pollo',
      kcal: 650,
      proteinG: 55,
      carbsG: 70,
      fatG: 12,
    });
    await waitFor(() => expect(lunch).toBeEnabled());
  });

  it('reports a rejected toggle and re-enables only that meal', async () => {
    unmarkMock.mockRejectedValue(new Error('network down'));
    const user = userEvent.setup();
    renderPage();
    const breakfast = await screen.findByRole('checkbox', {
      name: 'Desmarcar Bowl de Yogur Proteico y Fruta como hecha',
    });

    await user.click(breakfast);

    expect(
      await screen.findByText('No se pudo actualizar la comida. Inténtalo de nuevo.'),
    ).toBeInTheDocument();
    await waitFor(() => expect(breakfast).toBeEnabled());
    expect(getConsumptionMock).toHaveBeenCalledTimes(1);
  });

  /** Ticking logs the meal as the plan wrote it, with the totals the server worked out. */
  it('logs a meal as planned when its check is ticked', async () => {
    logAsPlannedMock.mockResolvedValue({
      id: 'entry-1',
      date: '2026-08-07',
      mealType: 'LUNCH',
      name: 'Pollo a la Plancha con Boniato',
      kcal: 650,
      proteinG: 55,
      carbsG: 70,
      fatG: 12,
    });
    const user = userEvent.setup();
    renderPage();

    await user.click(
      await screen.findByRole('checkbox', {
        name: 'Marcar Pollo a la Plancha con Boniato como hecha',
      }),
    );

    await waitFor(() =>
      expect(logAsPlannedMock).toHaveBeenCalledWith(
        '2026-08-07',
        expect.objectContaining({ id: 'meal-comida' }),
      ),
    );
    // Y se vuelve a preguntar, o el check se quedaría mintiendo hasta recargar.
    await waitFor(() => expect(getConsumptionMock).toHaveBeenCalledTimes(2));
  });

  it('opens the log form from the header action', async () => {
    const user = userEvent.setup();
    renderPage();

    await user.click(await screen.findByRole('button', { name: '+ Registrar' }));

    expect(within(screen.getByRole('dialog')).getByText('Registrar comida')).toBeInTheDocument();
  });

  /** A plan that sets no target fills no ring: the only ceiling available would be invented here. */
  it('shows the figures without a goal when the plan sets no target', async () => {
    getConsumptionMock.mockResolvedValue(consumption({ target: null, comparison: null }));
    renderPage();

    expect(await screen.findByText('140 g')).toBeInTheDocument();
    expect(screen.queryByText('/ 180 g')).not.toBeInTheDocument();
    expect(screen.getByLabelText(/Tu plan no fija objetivos/)).toBeInTheDocument();
    expect(screen.getByText('Sin objetivo')).toBeInTheDocument();
  });

  it('points at the in-app plan creator when there is no plan for today', async () => {
    getDayMock.mockResolvedValue({ ...strengthDay, meals: [] });
    renderPage();

    expect(await screen.findByText('No existe ningún plan planificado.')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Crear mi plan' })).toHaveAttribute(
      'href',
      '/app/nutrition/plans',
    );
  });

  it('shows an error state with a retry action', async () => {
    getDayMock.mockRejectedValueOnce(new Error('network')).mockResolvedValueOnce(strengthDay);
    const user = userEvent.setup();
    renderPage();

    await user.click(await screen.findByRole('button', { name: 'Reintentar' }));

    expect(await screen.findByText('Bowl de Yogur Proteico y Fruta')).toBeInTheDocument();
  });

  /**
   * FOR-728: each item shows `food + quantityG` in grams format without space before the unit,
   * matching the macro chips of the same card (162g P).
   */
  it('shows each food with its quantity in grams', async () => {
    getDayMock.mockResolvedValue(richDay);
    renderPage();

    expect(await screen.findByText('Yogur griego 200g')).toBeInTheDocument();
    expect(screen.getByText('Pechuga de pollo 180g')).toBeInTheDocument();
  });

  /**
   * A meal's instructions block only renders when the plan sets one. No empty label, no reserved
   * space (FOR-728 D6).
   */
  it("shows a meal's instructions block only when the plan sets them", async () => {
    getDayMock.mockResolvedValue(richDay);
    renderPage();

    const instructionsBlocks = await screen.findAllByTestId('meal-instructions');
    expect(instructionsBlocks).toHaveLength(1);
    expect(instructionsBlocks[0]).toHaveTextContent('una proteína, un carbo y una verdura');
  });

  /**
   * An unresolved item (food gone from the catalog) does NOT print "id 0g" — zero is
   * indistinguishable from truly resolved 0g. It also must not print the raw missing id as its
   * visible label: `NutritionPlanReader.unresolved()` sets `label = id`, and a catalog slug or a
   * bare UUID means nothing to whoever reads the plan (FOR-728 D5, review finding #5). A fixed,
   * human-readable label is shown instead; the id stays available via `title` for debugging.
   */
  it('does not print a confident 0g for an item whose food could not be resolved', async () => {
    const dayWithUnresolved: NutritionDay = {
      ...strengthDay,
      meals: [
        {
          id: 'meal-desayuno',
          mealType: 'BREAKFAST',
          name: 'Desayuno con extras',
          preferredTime: '08:00',
          optional: false,
          totals: { calories: 350, proteinG: 30, carbsG: 45, fatG: 8 },
          items: [
            { food: 'Yogur griego', quantityG: 200 },
            { food: 'unknown-food', quantityG: 0, unresolved: 'unknown-food' },
          ],
        },
      ],
    };
    getConsumptionMock.mockResolvedValue(consumption());
    getDayMock.mockResolvedValue(dayWithUnresolved);
    renderPage();

    // Wait for the meal to render.
    await screen.findByText('Yogur griego 200g');
    const unresolvedLabel = screen.getByText('Alimento no disponible');
    expect(unresolvedLabel).toBeInTheDocument();
    expect(unresolvedLabel).toHaveAttribute('title', 'unknown-food');
    // Neither the raw id alone nor concatenated with a fake quantity should ever be the visible text.
    expect(screen.queryByText('unknown-food 0g')).not.toBeInTheDocument();
    expect(screen.queryByText('unknown-food')).not.toBeInTheDocument();
  });

  /**
   * The day's free-text note, when set, appears in the meals section — not the page header,
   * to preserve the layout check's date-finding logic (FOR-728 D8).
   */
  it("shows the day's free-text note in the meals section", async () => {
    getDayMock.mockResolvedValue(richDay);
    renderPage();

    // Scoped to the meals section on purpose: the whole point of keeping the note out of the
    // page header (FOR-728 D8, so the layout check's date-finding logic keeps matching) is that
    // this assertion would fail if the note ever moved back there (review finding #7).
    const mealsSection = await screen.findByRole('region', { name: /comidas/i });
    expect(within(mealsSection).getByText('Running 4-5 km')).toBeInTheDocument();
  });

  it('shows no note block when the day carries none', async () => {
    renderPage(); // strengthDay has no notes field
    await screen.findByText('Bowl de Yogur Proteico y Fruta');
    expect(screen.queryByText(/Running/)).not.toBeInTheDocument();
  });

  /**
   * When an item carries preparation notes, they display below the quantity in muted text
   * (FOR-728 D7).
   */
  it("shows an item's preparation notes when set", async () => {
    getDayMock.mockResolvedValue(richDay);
    renderPage();

    expect(await screen.findByText('sin azúcar')).toBeInTheDocument();
  });
});
