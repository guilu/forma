import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { NutritionPage } from './NutritionPage';
import { getNutritionDay, type NutritionDay } from '../api/nutrition';

vi.mock('../api/nutrition', () => ({
  // The water tile reads real hydration now (FOR-130's endpoints, reachable at last). These tests
  // are not about it, so it answers with an empty day and stays out of the way.
  getHydration: vi.fn().mockResolvedValue({
    date: '2026-08-04',
    totalMl: 0,
    goalMl: 2000,
    progress: 0,
  }),
  logWaterIntake: vi.fn(),
  getNutritionDay: vi.fn(),
  // The page now also carries the meal log (FOR-127's endpoints, reachable at last). These tests
  // are about the PLAN half of the page, so the log answers with an empty day and stays out of the
  // way; MealLogPanel.test.tsx covers it on its own.
  getDayConsumption: vi.fn().mockResolvedValue({
    date: '2026-08-04',
    dayType: null,
    consumed: { kcal: 0, proteinG: 0, carbsG: 0, fatG: 0 },
    keyNutrients: { fiberG: null, sugarsG: null, sodiumMg: null, saturatedFatG: null },
    target: null,
    comparison: null,
    entries: [],
    plannedMeals: [],
  }),
  logMeal: vi.fn(),
}));

const getDayMock = vi.mocked(getNutritionDay);

const runningDay: NutritionDay = {
  type: 'RUNNING',
  targets: { calories: 1940, proteinG: 162, carbsG: 271, fatG: 25 },
  totals: { calories: 1776, proteinG: 62.4, carbsG: 288, fatG: 33.6 },
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
      totals: { calories: 444, proteinG: 15.6, carbsG: 72, fatG: 8.4 },
      items: [
        { food: 'Avena', quantityG: 120 },
        { food: 'Plátano', quantityG: 120 },
      ],
    },
    {
      mealType: 'PRE_WORKOUT',
      name: 'Snack pre-carrera',
      preferredTime: '18:00',
      optional: false,
      totals: { calories: 444, proteinG: 15.6, carbsG: 72, fatG: 8.4 },
      items: [{ food: 'Plátano', quantityG: 120 }],
    },
    {
      mealType: 'POST_WORKOUT',
      name: 'Recuperación (opcional)',
      preferredTime: '20:00',
      optional: true,
      totals: { calories: 444, proteinG: 15.6, carbsG: 72, fatG: 8.4 },
      items: [{ food: 'Proteína whey', quantityG: 20 }],
    },
    {
      mealType: 'DINNER',
      name: 'Cena ligera',
      preferredTime: '21:30',
      optional: false,
      totals: { calories: 444, proteinG: 15.6, carbsG: 72, fatG: 8.4 },
      items: [{ food: 'Pescado blanco', quantityG: 150 }],
    },
  ],
};

const strengthDay: NutritionDay = {
  type: 'STRENGTH',
  targets: { calories: 2200, proteinG: 180, carbsG: 220, fatG: 60 },
  totals: { calories: 1776, proteinG: 62.4, carbsG: 288, fatG: 33.6 },
  targetComparison: {
    caloriesReached: false,
    proteinReached: false,
    carbsReached: false,
    fatReached: false,
  },
  meals: [
    {
      mealType: 'BREAKFAST',
      name: 'Desayuno de fuerza',
      preferredTime: '08:00',
      optional: false,
      totals: { calories: 444, proteinG: 15.6, carbsG: 72, fatG: 8.4 },
      items: [{ food: 'Huevos', quantityG: 150 }],
    },
    {
      mealType: 'DINNER',
      name: 'Cena de fuerza',
      preferredTime: '21:00',
      optional: false,
      totals: { calories: 444, proteinG: 15.6, carbsG: 72, fatG: 8.4 },
      items: [{ food: 'Pollo', quantityG: 200 }],
    },
  ],
};

function renderPage() {
  return render(
    <MemoryRouter>
      <NutritionPage />
    </MemoryRouter>,
  );
}

describe('NutritionPage', () => {
  beforeEach(() => {
    getDayMock.mockReset();
  });

  it('renders the day, the macro summary and the meal list with items', async () => {
    getDayMock.mockResolvedValue(runningDay);

    renderPage();

    expect(await screen.findByRole('heading', { name: 'Nutrición', level: 1 })).toBeInTheDocument();
    // Macro summary: what the day aims for and what its meals come to, both real.
    // Every card here is a direct sibling of the page <h1> (no intervening
    // <h2>), so per FOR-112 each must render as <h2>.
    expect(
      screen.getByRole('heading', { name: 'Calorías del plan', level: 2 }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole('heading', { name: 'Distribución de macros', level: 2 }),
    ).toBeInTheDocument();
    expect(screen.getByText('1940')).toBeInTheDocument();
    expect(screen.getByText('162 g')).toBeInTheDocument();
    // Meal list: name, time and items. "Comidas del día" becomes <h2>
    // (FOR-112); the nested meal name was a hardcoded <h4> that would have
    // skipped a level under the new <h2> — fixed to <h3> as part of this
    // audit (NutritionPage.tsx MealCard).
    expect(screen.getByRole('heading', { name: 'Comidas del día', level: 2 })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Desayuno', level: 3 })).toBeInTheDocument();
    expect(screen.getByText('08:00')).toBeInTheDocument();
    expect(screen.getByText('Avena')).toBeInTheDocument();
    expect(screen.getByText('150 g')).toBeInTheDocument();
  });

  /**
   * The FOR-164 placeholders are gone, and this test is what they looked like: it used to assert a
   * fabricated "480 kcal" chip and a "kcal restantes" figure derived from an invented ratio. Both
   * numbers now come from the API, which had been sending them since FOR-105.
   */
  it('shows real figures where the FOR-164 placeholders used to be', async () => {
    getDayMock.mockResolvedValue(runningDay);

    renderPage();

    // The day's target, and what its meals actually add up to. Both real, and free to disagree.
    expect(await screen.findByText('1940')).toBeInTheDocument();
    expect(screen.getByText('1776')).toBeInTheDocument();
    // 1940 asked for, 1776 on the plate: 164 short, and the page says which way.
    expect(screen.getByText('164 kcal por debajo del objetivo')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Agua', level: 2 })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Nutrientes clave', level: 2 })).toBeInTheDocument();
    expect(screen.getByText('Fibra')).toBeInTheDocument();
    // The first meal's own total, from the API rather than from a cycled fixture.
    expect(screen.getAllByText('444 kcal').length).toBeGreaterThan(0);
  });

  /**
   * A key nutrient nobody can compute reads as unknown, never as zero.
   *
   * <p>A day's total is null when any single thing eaten has no figure for it (FOR-134): summing
   * the rest would report a number lower than the truth and look like a measurement.
   */
  it('says «sin datos» for a key nutrient rather than showing a zero', async () => {
    getDayMock.mockResolvedValue(runningDay);

    renderPage();

    expect(await screen.findByText('Fibra')).toBeInTheDocument();
    expect(screen.getAllByText('Sin datos').length).toBeGreaterThan(0);
  });

  it('switches between day types via the selector and refetches the plan', async () => {
    getDayMock.mockImplementation((type: string) =>
      Promise.resolve(type === 'strength' ? strengthDay : runningDay),
    );

    renderPage();

    expect(await screen.findByRole('heading', { name: 'Desayuno' })).toBeInTheDocument();
    expect(getDayMock).toHaveBeenCalledWith('running');

    await userEvent.click(screen.getByRole('radio', { name: 'Fuerza' }));

    expect(await screen.findByRole('heading', { name: 'Desayuno de fuerza' })).toBeInTheDocument();
    expect(getDayMock).toHaveBeenCalledWith('strength');
  });

  it('shows the running-day guidance with the carbs-early / lighter-dinner explanation', async () => {
    getDayMock.mockResolvedValue(runningDay);

    renderPage();

    expect(await screen.findByText(/carbohidratos se concentran temprano/)).toBeInTheDocument();
    expect(
      screen.getByRole('list', { name: 'Flujo de comidas del día de carrera' }),
    ).toBeInTheDocument();
    expect(screen.getByText('Correr')).toBeInTheDocument();
    // Direct sibling of the page <h1>, so it must render as <h2> (FOR-112).
    expect(
      screen.getByRole('heading', { name: 'Estrategia de día de carrera', level: 2 }),
    ).toBeInTheDocument();
  });

  it('hides the running-day guidance for a strength day', async () => {
    getDayMock.mockImplementation((type: string) =>
      Promise.resolve(type === 'strength' ? strengthDay : runningDay),
    );

    renderPage();

    expect(await screen.findByRole('heading', { name: 'Desayuno' })).toBeInTheDocument();

    await userEvent.click(screen.getByRole('radio', { name: 'Fuerza' }));

    expect(await screen.findByRole('heading', { name: 'Desayuno de fuerza' })).toBeInTheDocument();
    expect(screen.queryByText(/carbohidratos se concentran temprano/)).not.toBeInTheDocument();
  });

  it('shows the recovery recommendation when the day includes an optional meal', async () => {
    getDayMock.mockResolvedValue(runningDay);

    renderPage();

    // Direct sibling of the page <h1>, so it must render as <h2> (FOR-112).
    expect(
      await screen.findByRole('heading', { name: 'Recomendación de recuperación', level: 2 }),
    ).toBeInTheDocument();
    expect(screen.getByText(/Proteína whey \(20 g\)/)).toBeInTheDocument();
  });

  it('hides the recovery recommendation when no meal is optional', async () => {
    getDayMock.mockResolvedValue(strengthDay);

    renderPage();

    expect(await screen.findByRole('heading', { name: 'Desayuno de fuerza' })).toBeInTheDocument();
    expect(
      screen.queryByRole('heading', { name: 'Recomendación de recuperación' }),
    ).not.toBeInTheDocument();
  });

  it('shows an empty state when there is no plan for the day', async () => {
    getDayMock.mockResolvedValue({ ...runningDay, meals: [] });

    renderPage();

    expect(await screen.findByText('No existe ningún plan planificado.')).toBeInTheDocument();
  });

  it('shows an error state with a retry action', async () => {
    getDayMock.mockRejectedValueOnce(new Error('network')).mockResolvedValueOnce(runningDay);

    renderPage();

    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo cargar');

    await userEvent.click(screen.getByRole('button', { name: 'Reintentar' }));

    expect(await screen.findByRole('heading', { name: 'Desayuno' })).toBeInTheDocument();
  });
});
