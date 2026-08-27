import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { DashboardPage } from './DashboardPage';
import { NotificationProvider } from '../components/NotificationProvider';
import { listBodyMeasurements, type BodyMeasurement } from '../api/bodyMeasurements';
import { getTrainingWeek, type TrainingWeek } from '../api/training';
import {
  getDayConsumption,
  getNutritionDay,
  type DayConsumption,
  type NutritionDay,
} from '../api/nutrition';
import { getShoppingList, type ShoppingList } from '../api/shopping';
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
vi.mock('../api/nutrition', () => ({
  getNutritionDay: vi.fn(),
  getDayConsumption: vi.fn(),
}));
vi.mock('../api/shopping', () => ({ getShoppingList: vi.fn() }));
vi.mock('../api/profile', () => ({ getProfile: vi.fn() }));

const listMock = vi.mocked(listBodyMeasurements);
const trainingMock = vi.mocked(getTrainingWeek);
const nutritionMock = vi.mocked(getNutritionDay);
const consumptionMock = vi.mocked(getDayConsumption);
const shoppingMock = vi.mocked(getShoppingList);
const profileMock = vi.mocked(getProfile);

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
      dayOfWeek: 'SATURDAY',
      rest: false,
      sessions: [
        {
          id: 'SATURDAY:RUNNING',
          kind: 'RUNNING',
          bodyView: 'FRONT',
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
  totals: { calories: 1776, proteinG: 62.4, carbsG: 288, fatG: 33.6 },
  targetComparison: {
    caloriesReached: false,
    proteinReached: false,
    carbsReached: false,
    fatReached: false,
  },
  meals: [
    {
      id: 'meal-desayuno',
      mealType: 'BREAKFAST',
      name: 'Desayuno',
      preferredTime: '08:00',
      optional: false,
      totals: { calories: 444, proteinG: 15.6, carbsG: 72, fatG: 8.4 },
      items: [],
    },
  ],
};
const dayConsumption = {
  date: '2026-08-08',
  dayType: 'RUNNING',
  consumed: { kcal: 444, proteinG: 15.6, carbsG: 72, fatG: 8.4 },
  target: { kcal: 2300, proteinG: 160, carbsG: 250, fatG: 70 },
  plannedMeals: [],
} as unknown as DayConsumption;

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
      catalogued: true,
      checked: false,
    },
  ],
  budget: { weeklyEur: 103.8, monthlyEur: 451.2 },
};

function renderDashboard() {
  /* Mirrors App.tsx, which wraps every route in the provider: the menu widget's
     meal checks report a failed write through it. */
  return render(
    <MemoryRouter>
      <NotificationProvider>
        <DashboardPage />
      </NotificationProvider>
    </MemoryRouter>,
  );
}

function mockAllSuccess() {
  listMock.mockResolvedValue([measurement]);
  trainingMock.mockResolvedValue(trainingWeek);
  nutritionMock.mockResolvedValue(nutritionDay);
  consumptionMock.mockResolvedValue(dayConsumption);
  shoppingMock.mockResolvedValue(shoppingList);
}

describe('DashboardPage', () => {
  beforeEach(() => {
    vi.useFakeTimers({ toFake: ['Date'] });
    vi.setSystemTime(new Date('2026-08-08T12:00:00Z'));

    listMock.mockReset();
    trainingMock.mockReset();
    nutritionMock.mockReset();
    consumptionMock.mockReset();
    consumptionMock.mockResolvedValue(dayConsumption);
    shoppingMock.mockReset();
    profileMock.mockReset();
    // A saved profile with a name → the greeting personalises to it.
    profileMock.mockResolvedValue({ name: 'Diego', firstRunCompleted: true } as never);
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('shows the header greeting and renders the mockup panels', async () => {
    mockAllSuccess();

    renderDashboard();

    expect(
      await screen.findByRole('heading', { name: 'Hola Diego 👋', level: 1 }),
    ).toBeInTheDocument();
    // "de hoy" dropped (FOR-189): the date navigator moves the body tiles off
    // today, so a subtitle claiming the whole screen is today's would be wrong
    // half the time.
    expect(screen.getByText('Este es tu resumen')).toBeInTheDocument();

    // Second- and third-row panels each render as a <h2> section heading.
    expect(
      await screen.findByRole('heading', { name: 'Entrenamiento', level: 2 }),
    ).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Menu', level: 2 })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Nutrición', level: 2 })).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Macronutrientes' })).not.toBeInTheDocument();
    expect(
      screen.getByRole('heading', { name: 'Tendencia 30 días', level: 2 }),
    ).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Evolución', level: 2 })).toBeInTheDocument();
    // The goals feature is retired from the UI, and with it the card that was
    // nothing but a view of one goal's progress.
    expect(
      screen.queryByRole('heading', { name: 'Tu progreso', level: 2 }),
    ).not.toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Lista de compra', level: 2 })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Consejo del día', level: 2 })).toBeInTheDocument();

    // Metrics-row tiles are <h3> under the (sr-only) row <h2>, so heading order
    // never skips a level (FOR-112).
    expect(await screen.findByRole('heading', { name: 'Peso', level: 3 })).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Calorias' })).not.toBeInTheDocument();
    // The hydration tile is retired: the dashboard no longer tracks water.
    expect(screen.queryByRole('heading', { name: 'Agua' })).not.toBeInTheDocument();
  });

  describe('date navigator', () => {
    /**
     * Scoped to the metrics row: the same numbers and the same "no measurements"
     * copy also appear in Evolución, so a page-wide query matches twice and says
     * nothing about which widget it found.
     */
    const tiles = () => within(screen.getByRole('region', { name: 'Resumen de hoy' }));

    const dated = (measuredAt: string, weightKg: number): BodyMeasurement => ({
      ...measurement,
      measuredAt,
      weightKg,
      bodyFatPercentage: weightKg / 5,
      leanMassKg: weightKg - 10,
      bmi: weightKg / 3,
    });

    /**
     * The arrows step through the dates that actually have a measurement, not
     * through the calendar: an empty day has nothing to show, so offering it
     * would be a dead end.
     */
    it('starts on the newest measurement and shows its values', async () => {
      mockAllSuccess();
      listMock.mockResolvedValue([
        dated('2026-07-05T08:00:00Z', 75),
        dated('2026-07-01T08:00:00Z', 78),
      ]);

      renderDashboard();

      expect(await screen.findByText('5 jul 2026')).toBeInTheDocument();
      expect(tiles().getByText('75.0')).toBeInTheDocument();
    });

    it('steps back to the previous measurement and re-reads the tiles', async () => {
      mockAllSuccess();
      listMock.mockResolvedValue([
        dated('2026-07-05T08:00:00Z', 75),
        dated('2026-07-01T08:00:00Z', 78),
      ]);
      const user = userEvent.setup();

      renderDashboard();
      await screen.findByText('5 jul 2026');

      await user.click(screen.getByRole('button', { name: 'Medición anterior' }));

      expect(screen.getByText('1 jul 2026')).toBeInTheDocument();
      expect(tiles().getByText('78.0')).toBeInTheDocument();
      expect(tiles().queryByText('75.0')).not.toBeInTheDocument();
    });

    it('stops at both ends of the history', async () => {
      mockAllSuccess();
      listMock.mockResolvedValue([
        dated('2026-07-05T08:00:00Z', 75),
        dated('2026-07-01T08:00:00Z', 78),
      ]);
      const user = userEvent.setup();

      renderDashboard();
      await screen.findByText('5 jul 2026');

      // Newest: there is nothing newer to step to.
      expect(screen.getByRole('button', { name: 'Medición siguiente' })).toBeDisabled();

      await user.click(screen.getByRole('button', { name: 'Medición anterior' }));

      expect(screen.getByRole('button', { name: 'Medición anterior' })).toBeDisabled();
      expect(screen.getByRole('button', { name: 'Medición siguiente' })).toBeEnabled();
    });

    it('is not rendered at all when there are no measurements', async () => {
      mockAllSuccess();
      listMock.mockResolvedValue([]);

      renderDashboard();

      await waitFor(() => expect(tiles().getByText(/Aún no hay mediciones/)).toBeInTheDocument());
      expect(screen.queryByRole('button', { name: 'Medición anterior' })).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: 'Medición siguiente' })).not.toBeInTheDocument();
    });
  });

  it('links each widget to its feature page', async () => {
    mockAllSuccess();

    renderDashboard();
    await screen.findByRole('heading', { name: 'Entrenamiento' });

    const hrefs = screen.getAllByRole('link').map((el) => el.getAttribute('href'));
    expect(hrefs).toEqual(
      expect.arrayContaining(['/app/training', '/app/nutrition', '/app/shopping-list']),
    );
    // Nothing links to the retired goals route — a dead link is worse than a
    // missing one.
    expect(hrefs).not.toContain('/app/goals');
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

  it('keeps real calories and macros when only the plan menu request fails', async () => {
    listMock.mockResolvedValue([measurement]);
    trainingMock.mockResolvedValue(trainingWeek);
    shoppingMock.mockResolvedValue(shoppingList);
    consumptionMock.mockResolvedValue(dayConsumption);
    nutritionMock.mockRejectedValue(new Error('plan unavailable'));

    renderDashboard();

    expect(await screen.findByRole('img', { name: /444 de 2300 kcal\./ })).toBeInTheDocument();
    expect(screen.getByText('15,6 / 160 g')).toBeInTheDocument();
    expect(screen.getByRole('alert')).toHaveTextContent('No se pudo cargar tu menú de hoy');
  });

  it('has no accessibility violations once the widgets have settled (FOR-114)', async () => {
    mockAllSuccess();

    const { container } = renderDashboard();
    await screen.findByRole('heading', { name: 'Entrenamiento', level: 2 });
    await screen.findByRole('heading', { name: 'Peso', level: 3 });

    expect(await axe(container)).toHaveNoViolations();
  });
});
