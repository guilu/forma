import { afterEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { NutritionWidget } from './NutritionWidget';
import { NotificationProvider } from '../../components/NotificationProvider';
import {
  logPlannedMealAsPlanned,
  unmarkPlannedMeal,
  type DayConsumption,
  type NutritionDay,
} from '../../api/nutrition';

vi.mock('../../api/nutrition', () => ({
  logPlannedMealAsPlanned: vi.fn(),
  unmarkPlannedMeal: vi.fn(),
}));

const logMock = vi.mocked(logPlannedMealAsPlanned);
const unmarkMock = vi.mocked(unmarkPlannedMeal);

const day: NutritionDay = {
  type: 'RUNNING',
  targets: { calories: 2300, proteinG: 160, carbsG: 250, fatG: 70 },
  totals: { calories: 444, proteinG: 15.6, carbsG: 72, fatG: 8.4 },
  targetComparison: {
    caloriesReached: false,
    proteinReached: false,
    carbsReached: false,
    fatReached: false,
  },
  meals: [
    {
      id: 'm1',
      mealType: 'BREAKFAST',
      name: 'Desayuno',
      preferredTime: '08:00',
      optional: false,
      totals: { calories: 444, proteinG: 15.6, carbsG: 72, fatG: 8.4 },
      items: [
        { food: 'Copos de avena', quantityG: 80 },
        { food: 'Plátano', quantityG: 100 },
      ],
    },
  ],
};
const consumption = {
  consumed: { kcal: 444, proteinG: 15.6, carbsG: 72, fatG: 8.4 },
  target: { kcal: 2300, proteinG: 160, carbsG: 250, fatG: 70 },
} as DayConsumption;
const renderWidget = (
  menu: Parameters<typeof NutritionWidget>[0]['menu'],
  current = consumption,
  onMealToggled: () => Promise<unknown> = () => Promise.resolve(),
) =>
  render(
    <MemoryRouter>
      <NotificationProvider>
        <NutritionWidget
          menu={menu}
          consumption={current}
          dateIso="2026-08-18"
          onMealToggled={onMealToggled}
        />
      </NotificationProvider>
    </MemoryRouter>,
  );

describe('NutritionWidget', () => {
  afterEach(() => vi.clearAllMocks());
  it('renders the real meal type, food description, meal kcal and daily consumption', () => {
    renderWidget({ status: 'ready', day });
    expect(screen.getByText('Desayuno')).toBeInTheDocument();
    expect(screen.getByText('Copos de avena, Plátano')).toBeInTheDocument();
    expect(screen.getByText('444 kcal')).toBeInTheDocument();
    expect(screen.getByText(/444 kcal \/ 2300 kcal/)).toBeInTheDocument();
  });
  it('renders honest terminal states', () => {
    const { rerender } = renderWidget({ status: 'empty' });
    expect(screen.getByText(/No hay un plan/)).toBeInTheDocument();
    rerender(
      <MemoryRouter>
        <NotificationProvider>
          <NutritionWidget
            menu={{ status: 'error' }}
            consumption={consumption}
            dateIso="2026-08-18"
            onMealToggled={() => Promise.resolve()}
          />
        </NotificationProvider>
      </MemoryRouter>,
    );
    expect(screen.getByRole('alert')).toHaveTextContent('No se pudo cargar');
  });

  /*
   * The row's glyph became the control (FOR-164 follow-up): it used to be a
   * decorative clock, which is a poor use of the only affordance a five-row card
   * has room for. Marking here is the same write the nutrition page makes.
   */
  it('marks a planned meal as eaten from the menu row', async () => {
    logMock.mockResolvedValue({} as never);
    const onMealToggled = vi.fn().mockResolvedValue(undefined);
    const user = userEvent.setup();
    renderWidget({ status: 'ready', day }, consumption, onMealToggled);

    await user.click(screen.getByRole('checkbox', { name: /Marcar Desayuno como hecha/ }));

    expect(logMock).toHaveBeenCalledWith('2026-08-18', day.meals[0]);
    // The card's own kcal total has to catch up with the write.
    await waitFor(() => expect(onMealToggled).toHaveBeenCalled());
  });

  it('takes an eaten meal back', async () => {
    unmarkMock.mockResolvedValue(undefined);
    const user = userEvent.setup();
    renderWidget({ status: 'ready', day }, {
      ...consumption,
      plannedMeals: [
        { id: 'm1', mealType: 'BREAKFAST', name: 'Desayuno', optional: false, state: 'EATEN' },
      ],
    } as DayConsumption);

    const check = screen.getByRole('checkbox', { name: /Desmarcar Desayuno como hecha/ });
    expect(check).toBeChecked();
    await user.click(check);

    expect(unmarkMock).toHaveBeenCalledWith('2026-08-18', 'm1');
  });

  it('shows consumed kcal without a fake zero-percent bar when there is no target', () => {
    renderWidget({ status: 'ready', day }, { ...consumption, target: null });
    expect(screen.getByText('444 kcal')).toBeInTheDocument();
    expect(screen.getByText(/Sin objetivo/)).toBeInTheDocument();
    expect(screen.queryByRole('progressbar')).not.toBeInTheDocument();
  });
});
