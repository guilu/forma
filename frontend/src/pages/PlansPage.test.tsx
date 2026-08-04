import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { PlansPage } from './PlansPage';
import { NotificationProvider } from '../components/NotificationProvider';
import { listFoods, type CatalogFood } from '../api/foods';
import { listServings, type FoodServing } from '../api/servings';
import {
  activatePlan,
  createPlan,
  getPlan,
  listPlans,
  updatePlan,
  type NutritionPlan,
} from '../api/plans';

vi.mock('../api/plans', () => ({
  listPlans: vi.fn(),
  getPlan: vi.fn(),
  createPlan: vi.fn(),
  updatePlan: vi.fn(),
  activatePlan: vi.fn(),
  changePlanStatus: vi.fn(),
  deletePlan: vi.fn(),
}));
vi.mock('../api/foods', () => ({ listFoods: vi.fn() }));
vi.mock('../api/servings', () => ({ listServings: vi.fn() }));

const listMock = vi.mocked(listPlans);
const getMock = vi.mocked(getPlan);
const createMock = vi.mocked(createPlan);
const updateMock = vi.mocked(updatePlan);
const activateMock = vi.mocked(activatePlan);
const foodsMock = vi.mocked(listFoods);
const servingsMock = vi.mocked(listServings);

const oats: CatalogFood = {
  id: 'oats',
  name: 'Copos de avena',
  kcal: 370,
  proteinG: 13,
  carbsG: 60,
  fatG: 7,
};

const banana: CatalogFood = {
  id: 'banana',
  name: 'Plátano',
  kcal: 89,
  proteinG: 1.1,
  carbsG: 23,
  fatG: 0.3,
};

const mediumBanana: FoodServing = {
  id: 'banana-md',
  foodId: 'banana',
  name: 'Mediano',
  grams: 120,
  isDefault: true,
  sortOrder: 0,
};

const noTargets = { calories: null, proteinG: null, carbsG: null, fatG: null };

/** A draft with one monday, so the editor has something to read back. */
const draft: NutritionPlan = {
  id: 'plan-1',
  name: 'Semana base',
  status: 'DRAFT',
  active: false,
  targets: { kcalMin: null, kcalMax: null, proteinG: null, carbsG: null, fatG: null },
  generation: { by: 'HUMAN' },
  days: [
    {
      weekNumber: 1,
      dayNumber: 1,
      dayType: 'RUNNING',
      targets: noTargets,
      totals: { calories: 444, proteinG: 15.6, carbsG: 72, fatG: 8.4 },
      meals: [
        {
          mealType: 'BREAKFAST',
          name: 'Desayuno',
          scheduledTime: '08:00:00',
          optional: false,
          targets: noTargets,
          totals: { calories: 444, proteinG: 15.6, carbsG: 72, fatG: 8.4 },
          items: [
            {
              foodId: 'oats',
              amount: 120,
              optional: false,
              label: 'Copos de avena',
              grams: 120,
              totals: { calories: 444, proteinG: 15.6, carbsG: 72, fatG: 8.4 },
            },
          ],
        },
      ],
    },
  ],
};

const following: NutritionPlan = {
  ...draft,
  id: 'plan-2',
  name: 'La que sigo',
  status: 'ACTIVE',
  active: true,
};

function renderPage() {
  const user = userEvent.setup();
  render(
    <MemoryRouter>
      <NotificationProvider>
        <PlansPage />
      </NotificationProvider>
    </MemoryRouter>,
  );
  return user;
}

describe('PlansPage — the user’s own nutrition plans', () => {
  beforeEach(() => {
    listMock.mockReset();
    listMock.mockResolvedValue([draft]);
    getMock.mockReset();
    getMock.mockResolvedValue(draft);
    createMock.mockReset();
    updateMock.mockReset();
    activateMock.mockReset();
    activateMock.mockResolvedValue(following);
    foodsMock.mockReset();
    foodsMock.mockResolvedValue([oats, banana]);
    servingsMock.mockReset();
    servingsMock.mockResolvedValue([]);
  });

  it('lists the plans with what each one is', async () => {
    renderPage();

    expect(await screen.findByText('Semana base')).toBeInTheDocument();
    expect(screen.getByText('Borrador')).toBeInTheDocument();
  });

  /** Somebody with no plan sees why the nutrition screen is empty, not just that it is. */
  it('explains itself when there is no plan at all', async () => {
    listMock.mockResolvedValue([]);
    renderPage();

    expect(await screen.findByText('Todavía no hay ningún plan')).toBeInTheDocument();
  });

  /** At most one plan is followed, and the server stands the other down — so only one offers it. */
  it('offers to follow a plan that is not the one being followed', async () => {
    const user = renderPage();
    await screen.findByText('Semana base');

    await user.click(screen.getByRole('button', { name: 'Seguir este' }));

    await waitFor(() => expect(activateMock).toHaveBeenCalledWith('plan-1'));
  });

  it('does not offer to follow the plan already being followed', async () => {
    listMock.mockResolvedValue([following]);
    renderPage();
    await screen.findByText('La que sigo');

    expect(screen.queryByRole('button', { name: 'Seguir este' })).not.toBeInTheDocument();
  });

  /** The list carries headers only; the days arrive when one is opened. */
  it('fetches the days when a plan is opened', async () => {
    const user = renderPage();
    await screen.findByText('Semana base');

    await user.click(screen.getByRole('button', { name: 'Editar' }));

    await waitFor(() => expect(getMock).toHaveBeenCalledWith('plan-1'));
    expect(await screen.findByRole('dialog', { name: /Editar Semana base/ })).toBeInTheDocument();
  });

  /** A day's macros are the sum over its meals; there is nowhere to type them. */
  it('offers no macro fields on the day, only a target', async () => {
    const user = renderPage();
    await screen.findByText('Semana base');
    await user.click(screen.getByRole('button', { name: 'Editar' }));
    const dialog = await screen.findByRole('dialog', { name: /Editar Semana base/ });

    await user.click(within(dialog).getByRole('button', { name: /Lunes/ }));

    expect(within(dialog).getByLabelText('Objetivo kcal')).toBeInTheDocument();
    expect(within(dialog).queryByLabelText(/Proteínas del día/i)).not.toBeInTheDocument();
  });

  /** What the day comes to is read back from the server, never recomputed here. */
  it('shows what a day works out to beside its name', async () => {
    const user = renderPage();
    await screen.findByText('Semana base');
    await user.click(screen.getByRole('button', { name: 'Editar' }));

    const dialog = await screen.findByRole('dialog', { name: /Editar Semana base/ });
    expect(within(dialog).getByText(/444 kcal/)).toBeInTheDocument();
  });

  /** An amount is grams until a portion is chosen, and portions are asked for per food. */
  it('offers the portions of the food somebody picks', async () => {
    servingsMock.mockResolvedValue([mediumBanana]);
    const user = renderPage();
    await screen.findByText('Semana base');
    await user.click(screen.getByRole('button', { name: 'Editar' }));
    const dialog = await screen.findByRole('dialog', { name: /Editar Semana base/ });
    await user.click(within(dialog).getByRole('button', { name: /Lunes/ }));

    await user.selectOptions(within(dialog).getByLabelText('Alimento'), 'banana');

    const portion = await within(dialog).findByLabelText('Ración');
    // Still grams until somebody picks one: offering the portions is not choosing one.
    expect(within(dialog).getByLabelText('Gramos')).toBeInTheDocument();

    await user.selectOptions(portion, 'banana-md');

    expect(within(dialog).getByLabelText('Raciones')).toBeInTheDocument();
  });

  /** The amount's label follows what it counts: grams, or portions of the chosen one. */
  it('asks for grams while no portion is chosen', async () => {
    const user = renderPage();
    await screen.findByText('Semana base');
    await user.click(screen.getByRole('button', { name: 'Editar' }));
    const dialog = await screen.findByRole('dialog', { name: /Editar Semana base/ });
    await user.click(within(dialog).getByRole('button', { name: /Lunes/ }));

    expect(within(dialog).getByLabelText('Gramos')).toBeInTheDocument();
  });

  it('sends the plan somebody wrote', async () => {
    createMock.mockResolvedValue(draft);
    const user = renderPage();
    await screen.findByText('Semana base');

    await user.click(screen.getByRole('button', { name: '+ Plan' }));
    const dialog = await screen.findByRole('dialog', { name: /Nuevo plan/ });
    await user.type(within(dialog).getByLabelText('Nombre'), 'Mi semana');
    await user.click(within(dialog).getByRole('button', { name: /Lunes/ }));
    await user.click(within(dialog).getByRole('button', { name: '+ Comida' }));
    await user.type(
      within(dialog).getByLabelText('Nombre', { selector: '#meal-name-1-0' }),
      'Desayuno',
    );
    await user.selectOptions(within(dialog).getByLabelText('Alimento'), 'oats');
    await user.type(within(dialog).getByLabelText('Gramos'), '60');
    await user.click(within(dialog).getByRole('button', { name: 'Guardar' }));

    await waitFor(() =>
      expect(createMock).toHaveBeenCalledWith(
        expect.objectContaining({
          name: 'Mi semana',
          days: [
            expect.objectContaining({
              dayNumber: 1,
              meals: [
                expect.objectContaining({
                  name: 'Desayuno',
                  items: [expect.objectContaining({ foodId: 'oats', amount: 60 })],
                }),
              ],
            }),
          ],
        }),
      ),
    );
  });

  /** Seven empty days is what the form shows, not what somebody wrote. */
  it('leaves out days nobody filled in', async () => {
    createMock.mockResolvedValue(draft);
    const user = renderPage();
    await screen.findByText('Semana base');

    await user.click(screen.getByRole('button', { name: '+ Plan' }));
    const dialog = await screen.findByRole('dialog', { name: /Nuevo plan/ });
    await user.type(within(dialog).getByLabelText('Nombre'), 'Vacío');
    await user.click(within(dialog).getByRole('button', { name: 'Guardar' }));

    await waitFor(() =>
      expect(createMock).toHaveBeenCalledWith(expect.objectContaining({ days: [] })),
    );
  });

  /** An empty target is a real answer: it means the day inherits, not that it aims for zero. */
  it('sends an unset target as null rather than as zero', async () => {
    updateMock.mockResolvedValue(draft);
    const user = renderPage();
    await screen.findByText('Semana base');
    await user.click(screen.getByRole('button', { name: 'Editar' }));
    const dialog = await screen.findByRole('dialog', { name: /Editar Semana base/ });

    await user.click(within(dialog).getByRole('button', { name: 'Guardar' }));

    await waitFor(() =>
      expect(updateMock).toHaveBeenCalledWith(
        'plan-1',
        expect.objectContaining({
          targets: expect.objectContaining({ kcalMin: null }),
          days: [expect.objectContaining({ targets: { calories: null } })],
        }),
      ),
    );
  });
});
