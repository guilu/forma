import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { DashboardPage } from './DashboardPage';
import { listBodyMeasurements, type BodyMeasurement } from '../api/bodyMeasurements';
import { getTrainingWeek, type TrainingWeek } from '../api/training';
import { getNutritionDay, type NutritionDay } from '../api/nutrition';
import { getShoppingList, type ShoppingList } from '../api/shopping';
import { listGoals, type Goal } from '../api/goals';
import { getProfile } from '../api/profile';
import { axe } from '../test/axe';

/**
 * Dashboard composition tests (FOR-51, rebuilt for the FOR-164 mockup). Verifies
 * the page composes its panels with the new mockup titles, that widgets link to
 * their feature pages, and that one widget failing does not take down the others
 * (spec `specs/FOR-51/tests.md`). Per-widget loading/empty/error coverage lives
 * in each widget's own test file.
 */
vi.mock('../api/bodyMeasurements', () => ({ listBodyMeasurements: vi.fn() }));
vi.mock('../api/training', () => ({ getTrainingWeek: vi.fn() }));
vi.mock('../api/nutrition', () => ({ getNutritionDay: vi.fn() }));
vi.mock('../api/shopping', () => ({ getShoppingList: vi.fn() }));
vi.mock('../api/goals', () => ({ listGoals: vi.fn() }));
vi.mock('../api/profile', () => ({ getProfile: vi.fn() }));

const listMock = vi.mocked(listBodyMeasurements);
const trainingMock = vi.mocked(getTrainingWeek);
const nutritionMock = vi.mocked(getNutritionDay);
const shoppingMock = vi.mocked(getShoppingList);
const goalsMock = vi.mocked(listGoals);
const profileMock = vi.mocked(getProfile);

const goal: Goal = {
  id: 'g1',
  title: 'Bajar a 68 kg',
  metric: 'WEIGHT_KG',
  target: 68,
  dueDate: '2026-09-01',
  status: 'ACTIVE',
  progress: { current: 69.2, target: 68, ratio: 0.78, source: 'BODY_MEASUREMENT' },
  milestones: [],
};

const measurement: BodyMeasurement = {
  measuredAt: '2026-07-05T08:00:00Z',
  source: 'MANUAL',
  weightKg: 73.6,
  bodyFatPercentage: 14.7,
  leanMassKg: 62.8,
  bmi: 22.7,
};

const trainingWeek: TrainingWeek = {
  days: [
    {
      dayOfWeek: 'MONDAY',
      rest: false,
      sessions: [
        {
          id: 'MONDAY:RUNNING',
          kind: 'RUNNING',
          title: 'Running - Intervalos',
          detail: '5 km',
          status: 'PLANNED',
        },
      ],
    },
  ],
};

const nutritionDay: NutritionDay = {
  type: 'RUNNING',
  targets: { calories: 2300, proteinG: 160, carbsG: 250, fatG: 70 },
  meals: [
    { mealType: 'BREAKFAST', name: 'Desayuno', preferredTime: '08:00', optional: false, items: [] },
  ],
};

const shoppingList: ShoppingList = {
  weekStartDate: '2026-07-06',
  status: 'ACTIVE',
  generatedAt: '2026-07-06T08:00:00Z',
  items: [
    {
      id: 'i1',
      productId: 'p1',
      productName: 'Avena',
      category: 'CEREALES_Y_LEGUMBRES',
      quantity: 1,
      unit: 'UD',
      servings: null,
      estimatedCostEur: 3.5,
      checked: false,
    },
  ],
  budget: { weeklyEur: 103.8, monthlyEur: 451.2 },
};

function renderDashboard() {
  return render(
    <MemoryRouter>
      <DashboardPage />
    </MemoryRouter>,
  );
}

function mockAllSuccess() {
  listMock.mockResolvedValue([measurement]);
  trainingMock.mockResolvedValue(trainingWeek);
  nutritionMock.mockResolvedValue(nutritionDay);
  shoppingMock.mockResolvedValue(shoppingList);
  goalsMock.mockResolvedValue([goal]);
}

describe('DashboardPage', () => {
  beforeEach(() => {
    listMock.mockReset();
    trainingMock.mockReset();
    nutritionMock.mockReset();
    shoppingMock.mockReset();
    goalsMock.mockReset();
    goalsMock.mockResolvedValue([goal]);
    profileMock.mockReset();
    // A saved profile with a name → the greeting personalises to it.
    profileMock.mockResolvedValue({ name: 'Diego', firstRunCompleted: true } as never);
  });

  it('shows the header greeting and renders the mockup panels', async () => {
    mockAllSuccess();

    renderDashboard();

    expect(
      await screen.findByRole('heading', { name: 'Hola Diego 👋', level: 1 }),
    ).toBeInTheDocument();
    expect(screen.getByText('Este es tu resumen de hoy')).toBeInTheDocument();

    // Second- and third-row panels each render as a <h2> section heading.
    expect(
      await screen.findByRole('heading', { name: 'Próximo entrenamiento', level: 2 }),
    ).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Menú de hoy', level: 2 })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Macronutrientes', level: 2 })).toBeInTheDocument();
    expect(
      screen.getByRole('heading', { name: 'Tendencia 30 días', level: 2 }),
    ).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Tu progreso', level: 2 })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Evolución', level: 2 })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Lista de compra', level: 2 })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Consejo del día', level: 2 })).toBeInTheDocument();

    // Metrics-row tiles are <h3> under the (sr-only) row <h2>, so heading order
    // never skips a level (FOR-112).
    expect(await screen.findByRole('heading', { name: 'Peso', level: 3 })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Calorías hoy', level: 3 })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Agua', level: 3 })).toBeInTheDocument();
  });

  it('links each widget to its feature page', async () => {
    mockAllSuccess();

    renderDashboard();
    await screen.findByRole('heading', { name: 'Próximo entrenamiento' });

    const hrefs = screen.getAllByRole('link').map((el) => el.getAttribute('href'));
    expect(hrefs).toEqual(
      expect.arrayContaining(['/entrenamiento', '/nutricion', '/lista-compra', '/objetivos']),
    );
  });

  it('shows one widget in its empty state while another still renders its data', async () => {
    listMock.mockResolvedValue([]);
    trainingMock.mockResolvedValue(trainingWeek);
    nutritionMock.mockResolvedValue(nutritionDay);
    shoppingMock.mockResolvedValue(shoppingList);

    renderDashboard();

    expect(
      await screen.findByText(/Aún no hay mediciones\. Registra tu primera medición/),
    ).toBeInTheDocument();
    // Training widget still renders its next-session data.
    expect(screen.getByText('Running - Intervalos')).toBeInTheDocument();
  });

  it('shows a failing widget error state without breaking the rest of the dashboard', async () => {
    listMock.mockRejectedValue(new Error('network'));
    trainingMock.mockResolvedValue(trainingWeek);
    nutritionMock.mockResolvedValue(nutritionDay);
    shoppingMock.mockResolvedValue(shoppingList);

    renderDashboard();

    // The body-measurement source fails, so the widgets reading it (body /
    // trend / first-summary) show error states — assert the body one is among
    // them.
    const alerts = await screen.findAllByRole('alert');
    expect(
      alerts.some((a) => a.textContent?.includes('No se pudo cargar tu composición corporal')),
    ).toBe(true);
    // The widgets on other data sources still render their data.
    expect(await screen.findByText('Running - Intervalos')).toBeInTheDocument();
    expect(screen.getByText('Desayuno')).toBeInTheDocument();
  });

  it('has no accessibility violations once the widgets have settled (FOR-114)', async () => {
    mockAllSuccess();

    const { container } = renderDashboard();
    await screen.findByRole('heading', { name: 'Próximo entrenamiento', level: 2 });
    await screen.findByRole('heading', { name: 'Peso', level: 3 });

    expect(await axe(container)).toHaveNoViolations();
  });
});
