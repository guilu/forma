import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { DashboardPage } from './DashboardPage';
import { listBodyMeasurements, type BodyMeasurement } from '../api/bodyMeasurements';
import { getTrainingWeek, type TrainingWeek } from '../api/training';
import { getNutritionDay, type NutritionDay } from '../api/nutrition';
import { getShoppingList, type ShoppingList } from '../api/shopping';
import { getWeeklyInsights, type WeeklyInsights } from '../api/insights';
import { axe } from '../test/axe';

/**
 * Dashboard composition tests (FOR-51). Verifies the page composes all six MVP
 * widgets, that the insight widget's main recommendation renders, that each widget
 * links to its feature page, and that one widget failing does not take down the
 * others (spec `specs/FOR-51/tests.md`). Per-widget loading/empty/error coverage lives
 * in each widget's own test file.
 */
vi.mock('../api/bodyMeasurements', () => ({ listBodyMeasurements: vi.fn() }));
vi.mock('../api/training', () => ({ getTrainingWeek: vi.fn() }));
vi.mock('../api/nutrition', () => ({ getNutritionDay: vi.fn() }));
vi.mock('../api/shopping', () => ({ getShoppingList: vi.fn() }));
vi.mock('../api/insights', () => ({ getWeeklyInsights: vi.fn() }));

const listMock = vi.mocked(listBodyMeasurements);
const trainingMock = vi.mocked(getTrainingWeek);
const nutritionMock = vi.mocked(getNutritionDay);
const shoppingMock = vi.mocked(getShoppingList);
const insightsMock = vi.mocked(getWeeklyInsights);

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
  items: [
    {
      id: 'i1',
      productId: 'p1',
      productName: 'Avena',
      category: 'CEREALES_Y_LEGUMBRES',
      quantity: 1,
      estimatedCostEur: 3.5,
      checked: false,
    },
  ],
  budget: { weeklyEur: 103.8, monthlyEur: 451.2 },
};

const insights: WeeklyInsights = {
  checkIn: {
    weekStartDate: '2026-07-06',
    plannedRunningSessions: 3,
    completedRunningSessions: 3,
    plannedStrengthSessions: 3,
    completedStrengthSessions: 2,
  },
  main: {
    category: 'BODY',
    severity: 'ACTION',
    message: 'El peso baja rápido; considera aumentar un poco las calorías.',
    reason: 'El peso baja 1.5 kg en 7 días, por encima del 1% semanal recomendado.',
    createdAt: '2026-07-10T08:00:00Z',
  },
  secondary: [],
  generatedAt: '2026-07-10T08:00:00Z',
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
  insightsMock.mockResolvedValue(insights);
}

describe('DashboardPage', () => {
  beforeEach(() => {
    listMock.mockReset();
    trainingMock.mockReset();
    nutritionMock.mockReset();
    shoppingMock.mockReset();
    insightsMock.mockReset();
  });

  it('shows the header greeting and renders all six MVP widgets', async () => {
    mockAllSuccess();

    renderDashboard();

    expect(screen.getByRole('heading', { name: 'Hola Diego 👋', level: 1 })).toBeInTheDocument();
    expect(screen.getByText('Este es tu resumen de hoy')).toBeInTheDocument();

    // FOR-112 full-page fixture: every WidgetSection heading is a <h2>
    // (unchanged by this story — dashboard widgets already sit under a <h2>
    // section heading, so their Card/MetricCard titles correctly stay at the
    // default <h3>, verified below for one widget).
    expect(
      await screen.findByRole('heading', { name: 'Composición corporal', level: 2 }),
    ).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Entrenamiento', level: 2 })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Nutrición de hoy', level: 2 })).toBeInTheDocument();
    expect(
      screen.getByRole('heading', { name: 'Presupuesto de la compra', level: 2 }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole('heading', { name: 'Recomendación de la semana', level: 2 }),
    ).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Integraciones', level: 2 })).toBeInTheDocument();

    // The metric cards inside "Composición corporal" (<h2>) correctly stay at
    // the default <h3> — no skipped level, no headingLevel override needed.
    expect(screen.getByRole('heading', { name: 'Peso', level: 3 })).toBeInTheDocument();
  });

  it('renders the insight main recommendation (message + reason)', async () => {
    mockAllSuccess();

    renderDashboard();

    expect(
      await screen.findByText('El peso baja rápido; considera aumentar un poco las calorías.'),
    ).toBeInTheDocument();
    expect(
      screen.getByText('El peso baja 1.5 kg en 7 días, por encima del 1% semanal recomendado.'),
    ).toBeInTheDocument();
  });

  it('links each widget to its feature page', async () => {
    mockAllSuccess();

    renderDashboard();
    await screen.findByRole('heading', { name: 'Composición corporal' });

    expect(
      screen.getAllByRole('link', { name: 'Ver más' }).map((el) => el.getAttribute('href')),
    ).toEqual(
      expect.arrayContaining([
        '/mediciones',
        '/entrenamiento',
        '/nutricion',
        '/lista-compra',
        '/ajustes/integraciones',
      ]),
    );
  });

  it('shows one widget in its empty state while another still renders its data', async () => {
    listMock.mockResolvedValue([]);
    trainingMock.mockResolvedValue(trainingWeek);
    nutritionMock.mockResolvedValue(nutritionDay);
    shoppingMock.mockResolvedValue(shoppingList);
    insightsMock.mockResolvedValue(insights);

    renderDashboard();

    expect(await screen.findByText(/Aún no hay mediciones/)).toBeInTheDocument();
    // Training widget still renders its next-session data, unaffected by the body
    // widget's empty state.
    expect(screen.getByText('Running - Intervalos')).toBeInTheDocument();
  });

  it('shows a failing widget error state without breaking the rest of the dashboard', async () => {
    listMock.mockRejectedValue(new Error('network'));
    trainingMock.mockResolvedValue(trainingWeek);
    nutritionMock.mockResolvedValue(nutritionDay);
    shoppingMock.mockResolvedValue(shoppingList);
    insightsMock.mockResolvedValue(insights);

    renderDashboard();

    // The failing body widget shows its error state.
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'No se pudo cargar tu composición corporal',
    );
    // The other widgets still render their data.
    expect(await screen.findByText('Running - Intervalos')).toBeInTheDocument();
    expect(screen.getByText('2300', { exact: false })).toBeInTheDocument();
  });

  it('has no accessibility violations once all six widgets have settled (FOR-114)', async () => {
    mockAllSuccess();

    const { container } = renderDashboard();
    // Wait for every widget to leave its loading state before scanning: the
    // full-page fixture is also this suite's representative multi-heading-level
    // screen (page <h1>, section <h2>s, card <h3>s), so the axe "heading-order"
    // rule here covers the FOR-112 heading-hierarchy fix.
    await screen.findByRole('heading', { name: 'Composición corporal', level: 2 });
    await screen.findByText('El peso baja rápido; considera aumentar un poco las calorías.');

    expect(await axe(container)).toHaveNoViolations();
  });
});
