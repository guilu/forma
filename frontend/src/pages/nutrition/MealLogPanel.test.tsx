import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MealLogPanel } from './MealLogPanel';
import { NotificationProvider } from '../../components/NotificationProvider';
import { listFoods, type CatalogFood } from '../../api/foods';
import { listServings, type FoodServing } from '../../api/servings';
import { logMeal, type DayConsumption } from '../../api/nutrition';

vi.mock('../../api/nutrition', () => ({
  logMeal: vi.fn(),
}));
vi.mock('../../api/foods', () => ({ listFoods: vi.fn() }));
vi.mock('../../api/servings', () => ({ listServings: vi.fn() }));

const logMock = vi.mocked(logMeal);
const foodsMock = vi.mocked(listFoods);
const servingsMock = vi.mocked(listServings);

const DATE = '2026-08-04';

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

/** A day with one thing eaten, one planned meal done and one still pending. */
const consumed: DayConsumption = {
  date: DATE,
  dayType: 'RUNNING',
  consumed: { kcal: 551, proteinG: 16.9, carbsG: 99.6, fatG: 8.8 },
  keyNutrients: { fiberG: null, sugarsG: null, sodiumMg: null, saturatedFatG: null },
  target: { kcal: 2300, proteinG: 160, carbsG: 260, fatG: 70 },
  comparison: {
    caloriesReached: false,
    proteinReached: false,
    carbsReached: false,
    fatReached: false,
  },
  entries: [
    { id: 'entry-1', mealType: 'BREAKFAST', name: 'Copos de avena', kcal: 444 },
    { id: 'entry-2', mealType: 'SNACK', name: 'Plátano', kcal: 107 },
  ],
  plannedMeals: [
    { id: 'meal-1', mealType: 'BREAKFAST', name: 'Desayuno', optional: false, state: 'EATEN' },
    { id: 'meal-2', mealType: 'LUNCH', name: 'Comida', optional: false, state: 'PENDING' },
  ],
};

const onLogged = vi.fn();

/**
 * The consumption is a prop, not a request of this panel's.
 *
 * <p>The page owns it because the key-nutrient card reads the same answer, so the panel takes what
 * it is given and says when something has been logged.
 */
function renderPanel(day: DayConsumption | undefined = consumed) {
  const user = userEvent.setup();
  render(
    <NotificationProvider>
      <MealLogPanel date={DATE} day={day} onLogged={onLogged} />
    </NotificationProvider>,
  );
  return user;
}

describe('MealLogPanel — what was actually eaten', () => {
  beforeEach(() => {
    onLogged.mockReset();
    logMock.mockReset();
    logMock.mockResolvedValue({
      id: 'entry-2',
      date: DATE,
      mealType: 'LUNCH',
      name: 'Plátano',
      kcal: 107,
      proteinG: 1.3,
      carbsG: 27.6,
      fatG: 0.4,
    });
    foodsMock.mockReset();
    foodsMock.mockResolvedValue([oats, banana]);
    servingsMock.mockReset();
    servingsMock.mockResolvedValue([]);
  });

  /** The total is the server's sum of the entries, not this screen's — 444 + 107. */
  it('shows the day’s consumed total against the target', async () => {
    renderPanel();

    expect(await screen.findByText(/551 kcal/)).toBeInTheDocument();
    expect(screen.getByText(/de 2300/)).toBeInTheDocument();
  });

  it('lists what has been logged', async () => {
    renderPanel();

    expect(await screen.findByText('Copos de avena')).toBeInTheDocument();
    expect(screen.getByText('444 kcal')).toBeInTheDocument();
    expect(screen.getByText('107 kcal')).toBeInTheDocument();
  });

  /** The state is the server's, derived from the entries — this screen never works it out. */
  it('shows each planned meal and what has become of it', async () => {
    renderPanel();

    expect(await screen.findByText('Hecha')).toBeInTheDocument();
    expect(screen.getByText('Pendiente')).toBeInTheDocument();
  });

  /** Somebody with nothing logged is told what logging is for, not just that the list is empty. */
  it('explains itself when nothing has been logged', async () => {
    renderPanel({
      ...consumed,
      entries: [],
      consumed: { kcal: 0, proteinG: 0, carbsG: 0, fatG: 0 },
    });

    expect(await screen.findByText(/Todavía no has registrado nada/)).toBeInTheDocument();
  });

  /** A meal already done offers nothing to press: it has been answered. */
  it('offers to log only the planned meals still outstanding', async () => {
    renderPanel();
    await screen.findByText('Hecha');

    expect(screen.getAllByRole('button', { name: 'Registrar esta' })).toHaveLength(1);
  });

  it('sends a catalog food measured in grams', async () => {
    const user = renderPanel();
    await screen.findByText('Copos de avena');

    await user.click(screen.getByRole('button', { name: '+ Registrar' }));
    const dialog = await screen.findByRole('dialog', { name: /Registrar comida/ });
    await user.selectOptions(within(dialog).getByLabelText('Alimento'), 'oats');
    await user.type(within(dialog).getByLabelText('Gramos'), '60');
    await user.click(within(dialog).getByRole('button', { name: 'Registrar' }));

    await waitFor(() =>
      expect(logMock).toHaveBeenCalledWith(
        expect.objectContaining({ date: DATE, foodItemId: 'oats', grams: 60 }),
      ),
    );
  });

  /** A named portion is how anybody says it, and V49 has had them since long before this screen. */
  it('sends a count of a named portion when one is chosen', async () => {
    servingsMock.mockResolvedValue([mediumBanana]);
    const user = renderPanel();
    await screen.findByText('Copos de avena');

    await user.click(screen.getByRole('button', { name: '+ Registrar' }));
    const dialog = await screen.findByRole('dialog', { name: /Registrar comida/ });
    await user.selectOptions(within(dialog).getByLabelText('Alimento'), 'banana');
    await user.selectOptions(await within(dialog).findByLabelText('Ración'), 'banana-md');
    await user.type(within(dialog).getByLabelText('Raciones'), '1');
    await user.click(within(dialog).getByRole('button', { name: 'Registrar' }));

    await waitFor(() =>
      expect(logMock).toHaveBeenCalledWith(
        expect.objectContaining({ foodItemId: 'banana', servingId: 'banana-md', portions: 1 }),
      ),
    );
  });

  /** A catalog food already knows its macros; a second answer would only be there to disagree. */
  it('offers no macro fields for a catalog food', async () => {
    const user = renderPanel();
    await screen.findByText('Copos de avena');

    await user.click(screen.getByRole('button', { name: '+ Registrar' }));
    const dialog = await screen.findByRole('dialog', { name: /Registrar comida/ });

    expect(within(dialog).queryByLabelText('kcal')).not.toBeInTheDocument();
  });

  /** And a plate nobody has ever weighed can only be typed in. */
  it('asks for macros when the food is not in the catalog', async () => {
    const user = renderPanel();
    await screen.findByText('Copos de avena');

    await user.click(screen.getByRole('button', { name: '+ Registrar' }));
    const dialog = await screen.findByRole('dialog', { name: /Registrar comida/ });
    await user.click(within(dialog).getByRole('radio', { name: 'Escribir los macros' }));

    await user.type(within(dialog).getByLabelText('Qué has comido'), 'Menú del día');
    await user.type(within(dialog).getByLabelText('kcal'), '800');
    await user.type(within(dialog).getByLabelText('Proteínas (g)'), '35');
    await user.type(within(dialog).getByLabelText('Hidratos (g)'), '90');
    await user.type(within(dialog).getByLabelText('Grasas (g)'), '30');
    await user.click(within(dialog).getByRole('button', { name: 'Registrar' }));

    await waitFor(() =>
      expect(logMock).toHaveBeenCalledWith(
        expect.objectContaining({ name: 'Menú del día', kcal: 800, proteinG: 35 }),
      ),
    );
  });

  /** Answering a planned meal from its own row preselects it — that is the point of the button. */
  it('attaches the entry to the planned meal it was opened from', async () => {
    const user = renderPanel();
    await screen.findByText('Pendiente');

    await user.click(screen.getByRole('button', { name: 'Registrar esta' }));
    const dialog = await screen.findByRole('dialog', { name: /Registrar comida/ });
    await user.selectOptions(within(dialog).getByLabelText('Alimento'), 'oats');
    await user.type(within(dialog).getByLabelText('Gramos'), '80');
    await user.click(within(dialog).getByRole('button', { name: 'Registrar' }));

    await waitFor(() =>
      expect(logMock).toHaveBeenCalledWith(expect.objectContaining({ plannedMealId: 'meal-2' })),
    );
  });

  /** Not answering one is the ordinary case, not a lesser one. */
  it('sends no planned meal for something eaten apart from the plan', async () => {
    const user = renderPanel();
    await screen.findByText('Copos de avena');

    await user.click(screen.getByRole('button', { name: '+ Registrar' }));
    const dialog = await screen.findByRole('dialog', { name: /Registrar comida/ });
    await user.selectOptions(within(dialog).getByLabelText('Alimento'), 'oats');
    await user.type(within(dialog).getByLabelText('Gramos'), '30');
    await user.click(within(dialog).getByRole('button', { name: 'Registrar' }));

    await waitFor(() =>
      expect(logMock).toHaveBeenCalledWith(expect.objectContaining({ plannedMealId: undefined })),
    );
  });

  /** An account with no plan still logs; there is simply nothing to be adherent to. */
  it('hides the plan section when there is no plan', async () => {
    renderPanel({ ...consumed, plannedMeals: [], target: null, comparison: null });

    await screen.findByText('Copos de avena');
    expect(screen.queryByText('Lo que pedía el plan')).not.toBeInTheDocument();
    expect(screen.queryByText(/de 2300/)).not.toBeInTheDocument();
  });

  /** The panel does not re-read anything; it tells whoever owns the request to ask again. */
  it('reports upwards after something is logged', async () => {
    const user = renderPanel();
    await screen.findByText('Copos de avena');

    await user.click(screen.getByRole('button', { name: '+ Registrar' }));
    const dialog = await screen.findByRole('dialog', { name: /Registrar comida/ });
    await user.selectOptions(within(dialog).getByLabelText('Alimento'), 'oats');
    await user.type(within(dialog).getByLabelText('Gramos'), '60');
    await user.click(within(dialog).getByRole('button', { name: 'Registrar' }));

    await waitFor(() => expect(onLogged).toHaveBeenCalled());
  });

  it('waits while the day is still loading', () => {
    // Rendered directly rather than through the helper: passing `undefined` to a parameter with a
    // default gets the default, which is the opposite of what this test is about.
    render(
      <NotificationProvider>
        <MealLogPanel date={DATE} day={undefined} onLogged={onLogged} />
      </NotificationProvider>,
    );

    expect(screen.getByText(/Cargando lo que has comido/)).toBeInTheDocument();
  });
});
