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
  type DayConsumption,
  type NutritionDay,
} from '../api/nutrition';

vi.mock('../api/nutrition', () => ({
  // La tarjeta de agua lee hidratación de verdad; estos tests no van de eso, así que responde un
  // día vacío y se aparta.
  getHydration: vi.fn().mockResolvedValue({
    date: '2026-08-07',
    totalMl: 0,
    goalMl: 2000,
    progress: 0,
  }),
  logWaterIntake: vi.fn(),
  getNutritionDay: vi.fn(),
  getDayConsumption: vi.fn(),
  logMeal: vi.fn(),
  logPlannedMealAsPlanned: vi.fn(),
}));

const getDayMock = vi.mocked(getNutritionDay);
const getConsumptionMock = vi.mocked(getDayConsumption);
const logAsPlannedMock = vi.mocked(logPlannedMealAsPlanned);
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
    expect(
      await screen.findByRole('img', { name: /2\.?150 de 2\.?850 kcal consumidas/ }),
    ).toBeInTheDocument();
    // Restantes: lo que falta, no lo que suma el plan.
    expect(screen.getByText('700')).toBeInTheDocument();
  });

  it('shows each macro eaten against its target', async () => {
    renderPage();

    expect(await screen.findByText('140 g')).toBeInTheDocument();
    expect(screen.getByText('/ 180 g')).toBeInTheDocument();
    expect(screen.getByText('250 g')).toBeInTheDocument();
    expect(screen.getByText('55 g')).toBeInTheDocument();
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
   * under the type label the same word came out twice and neither said what there was to eat.
   */
  it('titles a meal with its food when its name only repeats the meal type', async () => {
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

    expect(
      await screen.findByRole('heading', { name: 'Copos de avena, Plátano' }),
    ).toBeInTheDocument();
  });

  /** A plan that does name its meals keeps the name: somebody wrote it and it says more. */
  it('keeps a meal name that says something the type does not', async () => {
    renderPage();

    expect(
      await screen.findByRole('heading', { name: 'Bowl de Yogur Proteico y Fruta' }),
    ).toBeInTheDocument();
  });

  /** The check reflects the state the server reports, and an eaten meal cannot be logged twice. */
  it('ticks the meals already eaten and leaves the rest open', async () => {
    renderPage();

    const eaten = await screen.findByRole('checkbox', {
      name: 'Bowl de Yogur Proteico y Fruta: hecha',
    });
    expect(eaten).toBeChecked();
    expect(eaten).toBeDisabled();

    expect(
      screen.getByRole('checkbox', { name: 'Marcar Pollo a la Plancha con Boniato como hecha' }),
    ).not.toBeChecked();
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

  /** A plan that sets no target draws no bar: the only ceiling available would be invented here. */
  it('shows the figures without bars when the plan sets no target', async () => {
    getConsumptionMock.mockResolvedValue(consumption({ target: null, comparison: null }));
    renderPage();

    expect(await screen.findByText('140 g')).toBeInTheDocument();
    expect(screen.queryByText('/ 180 g')).not.toBeInTheDocument();
    expect(screen.getByLabelText(/Tu plan no fija un objetivo/)).toBeInTheDocument();
  });

  it('points at the generator when there is no plan for today', async () => {
    getDayMock.mockResolvedValue({ ...strengthDay, meals: [] });
    renderPage();

    expect(await screen.findByText('No existe ningún plan planificado.')).toBeInTheDocument();
  });

  it('shows an error state with a retry action', async () => {
    getDayMock.mockRejectedValueOnce(new Error('network')).mockResolvedValueOnce(strengthDay);
    const user = userEvent.setup();
    renderPage();

    await user.click(await screen.findByRole('button', { name: 'Reintentar' }));

    expect(await screen.findByText('Bowl de Yogur Proteico y Fruta')).toBeInTheDocument();
  });
});
