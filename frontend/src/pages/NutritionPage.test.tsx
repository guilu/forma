import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { NutritionPage } from './NutritionPage';
import { getNutritionDay, type NutritionDay } from '../api/nutrition';
import { getShoppingList } from '../api/shopping';

vi.mock('../api/nutrition', () => ({
  getNutritionDay: vi.fn(),
}));
vi.mock('../api/shopping', () => ({
  getShoppingList: vi.fn(),
}));

const getDayMock = vi.mocked(getNutritionDay);
const getShoppingListMock = vi.mocked(getShoppingList);

const runningDay: NutritionDay = {
  type: 'RUNNING',
  targets: { calories: 1940, proteinG: 162, carbsG: 271, fatG: 25 },
  meals: [
    {
      mealType: 'BREAKFAST',
      name: 'Desayuno',
      preferredTime: '08:00',
      optional: false,
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
      items: [{ food: 'Plátano', quantityG: 120 }],
    },
    {
      mealType: 'POST_WORKOUT',
      name: 'Recuperación (opcional)',
      preferredTime: '20:00',
      optional: true,
      items: [{ food: 'Proteína whey', quantityG: 20 }],
    },
    {
      mealType: 'DINNER',
      name: 'Cena ligera',
      preferredTime: '21:30',
      optional: false,
      items: [{ food: 'Pescado blanco', quantityG: 150 }],
    },
  ],
};

const strengthDay: NutritionDay = {
  type: 'STRENGTH',
  targets: { calories: 2200, proteinG: 180, carbsG: 220, fatG: 60 },
  meals: [
    {
      mealType: 'BREAKFAST',
      name: 'Desayuno de fuerza',
      preferredTime: '08:00',
      optional: false,
      items: [{ food: 'Huevos', quantityG: 150 }],
    },
    {
      mealType: 'DINNER',
      name: 'Cena de fuerza',
      preferredTime: '21:00',
      optional: false,
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
    getShoppingListMock.mockReset();
    getShoppingListMock.mockResolvedValue({
      weekStartDate: '2026-07-06',
      status: 'ACTIVE',
      generatedAt: '2026-07-06T08:00:00Z',
      items: [],
      budget: { weeklyEur: 0, monthlyEur: 0 },
    });
  });

  it('renders the day, the macro summary and the meal list with items', async () => {
    getDayMock.mockResolvedValue(runningDay);

    renderPage();

    expect(await screen.findByRole('heading', { name: 'Nutrición', level: 1 })).toBeInTheDocument();
    // Macro summary: calories target + macro ring grams (no per-meal macros/kcal — not
    // returned by the API, see NutritionPage.tsx doc comment).
    // Every card here is a direct sibling of the page <h1> (no intervening
    // <h2>), so per FOR-112 each must render as <h2>.
    expect(screen.getByRole('heading', { name: 'Calorías', level: 2 })).toBeInTheDocument();
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

  it('shows a shopping shortcut that links to the shopping list', async () => {
    getDayMock.mockResolvedValue(runningDay);
    getShoppingListMock.mockResolvedValue({
      weekStartDate: '2026-07-06',
      status: 'ACTIVE',
      generatedAt: '2026-07-06T08:00:00Z',
      items: [
        {
          id: '1',
          productId: 'p1',
          productName: 'Avena',
          category: 'CEREALES_Y_LEGUMBRES',
          quantity: 1,
          unit: 'UD',
          servings: null,
          estimatedCostEur: 2,
          checked: false,
        },
      ],
      budget: { weeklyEur: 10, monthlyEur: 40 },
    });

    renderPage();

    const link = await screen.findByRole('link', { name: 'Ver lista de la compra' });
    expect(link).toHaveAttribute('href', '/lista-compra');
    await waitFor(() => expect(screen.getByText('1 producto')).toBeInTheDocument());
    // Direct sibling of the page <h1>, so it must render as <h2> (FOR-112).
    expect(
      screen.getByRole('heading', { name: 'Lista de la compra', level: 2 }),
    ).toBeInTheDocument();
  });

  it('shows an empty state when there is no plan for the day', async () => {
    getDayMock.mockResolvedValue({ ...runningDay, meals: [] });

    renderPage();

    expect(
      await screen.findByText('No hay un plan de comidas para este tipo de día.'),
    ).toBeInTheDocument();
  });

  it('shows an error state with a retry action', async () => {
    getDayMock.mockRejectedValueOnce(new Error('network')).mockResolvedValueOnce(runningDay);

    renderPage();

    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo cargar');

    await userEvent.click(screen.getByRole('button', { name: 'Reintentar' }));

    expect(await screen.findByRole('heading', { name: 'Desayuno' })).toBeInTheDocument();
  });
});
