import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { AdminPage } from '../AdminPage';
import { NotificationProvider } from '../../components/NotificationProvider';
import { listFoods, type CatalogFood } from '../../api/foods';
import { createRecipe, listRecipes, updateRecipe, type Recipe } from '../../api/recipes';

vi.mock('../../api/foods', () => ({
  listFoods: vi.fn(),
  createFood: vi.fn(),
  updateFood: vi.fn(),
  deleteFood: vi.fn(),
}));
vi.mock('../../api/recipes', () => ({
  listRecipes: vi.fn(),
  createRecipe: vi.fn(),
  updateRecipe: vi.fn(),
  deleteRecipe: vi.fn(),
}));

const foodsMock = vi.mocked(listFoods);
const listMock = vi.mocked(listRecipes);
const createMock = vi.mocked(createRecipe);
const updateMock = vi.mocked(updateRecipe);

const oats: CatalogFood = {
  id: 'oats',
  name: 'Copos de avena',
  kcal: 370,
  proteinG: 13,
  carbsG: 60,
  fatG: 7,
};

const milk: CatalogFood = {
  id: 'skim-milk',
  name: 'Leche desnatada',
  kcal: 35,
  proteinG: 3.5,
  carbsG: 5,
  fatG: 0.1,
};

/** A stew for four: the whole dish and one portion are deliberately different figures. */
const stew: Recipe = {
  id: 'guiso',
  name: 'Guiso de arroz',
  servings: 4,
  ingredients: [{ foodId: 'oats', grams: 400 }],
  total: { calories: 1440, proteinG: 28, carbsG: 316, fatG: 4 },
  perServing: { calories: 360, proteinG: 7, carbsG: 79, fatG: 1 },
  unknownFoodIds: [],
};

async function openRecipesTab() {
  const user = userEvent.setup();
  render(
    <MemoryRouter>
      <NotificationProvider>
        <AdminPage />
      </NotificationProvider>
    </MemoryRouter>,
  );
  await user.click(screen.getByRole('tab', { name: 'Recetas' }));
  return user;
}

describe('AdminPage — the recipes tab', () => {
  beforeEach(() => {
    foodsMock.mockReset();
    foodsMock.mockResolvedValue([oats, milk]);
    listMock.mockReset();
    listMock.mockResolvedValue([stew]);
    createMock.mockReset();
    updateMock.mockReset();
  });

  /**
   * Per serving, not per dish: it is the figure anybody eating it wants, and showing the whole
   * thing next to a stew for four invites reading one as the other.
   */
  it('lists dishes by what one portion works out to', async () => {
    await openRecipesTab();

    expect(await screen.findByText('Guiso de arroz')).toBeInTheDocument();
    expect(screen.getByText('360')).toBeInTheDocument();
    // The whole dish is there too, in the row's detail, so both are available and only one is
    // headline.
    expect(screen.queryByText('1440')).not.toBeInTheDocument();
  });

  /** A dish's macros are the sum over its ingredients; there is nowhere to type them. */
  it('offers no macro fields on the form', async () => {
    const user = await openRecipesTab();
    await screen.findByText('Guiso de arroz');

    await user.click(screen.getByRole('button', { name: '+ Receta' }));

    const dialog = await screen.findByRole('dialog', { name: /Nueva receta/ });
    expect(within(dialog).queryByLabelText(/kcal/i)).not.toBeInTheDocument();
    expect(within(dialog).queryByLabelText(/Proteínas/i)).not.toBeInTheDocument();
    expect(within(dialog).getByLabelText('Raciones que salen')).toBeInTheDocument();
  });

  it('sends the ingredients somebody chose', async () => {
    createMock.mockResolvedValue(stew);
    const user = await openRecipesTab();
    await screen.findByText('Guiso de arroz');

    await user.click(screen.getByRole('button', { name: '+ Receta' }));
    const dialog = await screen.findByRole('dialog', { name: /Nueva receta/ });

    await user.type(within(dialog).getByLabelText('Identificador'), 'avena-overnight');
    await user.type(within(dialog).getByLabelText('Nombre'), 'Avena overnight');
    await user.selectOptions(within(dialog).getByLabelText('Alimento'), 'oats');
    await user.type(within(dialog).getByLabelText('Gramos'), '60');
    await user.click(within(dialog).getByRole('button', { name: 'Guardar' }));

    await waitFor(() =>
      expect(createMock).toHaveBeenCalledWith(
        expect.objectContaining({
          id: 'avena-overnight',
          name: 'Avena overnight',
          ingredients: [{ foodId: 'oats', grams: 60 }],
        }),
      ),
    );
  });

  /** A row somebody added and did not fill is not an ingredient of nothing. */
  it('leaves out ingredient rows nobody filled in', async () => {
    createMock.mockResolvedValue(stew);
    const user = await openRecipesTab();
    await screen.findByText('Guiso de arroz');

    await user.click(screen.getByRole('button', { name: '+ Receta' }));
    const dialog = await screen.findByRole('dialog', { name: /Nueva receta/ });

    await user.type(within(dialog).getByLabelText('Identificador'), 'avena');
    await user.type(within(dialog).getByLabelText('Nombre'), 'Avena');
    await user.selectOptions(within(dialog).getByLabelText('Alimento'), 'oats');
    await user.type(within(dialog).getByLabelText('Gramos'), '60');
    // A second row, opened and left blank.
    await user.click(within(dialog).getByRole('button', { name: '+ Ingrediente' }));
    await user.click(within(dialog).getByRole('button', { name: 'Guardar' }));

    await waitFor(() =>
      expect(createMock).toHaveBeenCalledWith(
        expect.objectContaining({ ingredients: [{ foodId: 'oats', grams: 60 }] }),
      ),
    );
  });

  /** Editing shows what the dish adds up to, read back from the server rather than recomputed. */
  it('shows the per-serving totals when editing', async () => {
    const user = await openRecipesTab();

    await user.click(await screen.findByRole('button', { name: 'Editar Guiso de arroz' }));

    const dialog = await screen.findByRole('dialog', { name: /Editar Guiso de arroz/ });
    expect(within(dialog).getByText('360 kcal')).toBeInTheDocument();
    expect(within(dialog).getByText('HC 79 g')).toBeInTheDocument();
  });

  /** The amounts are read as the catalog records each food, and that is not obvious. */
  it('says the grams are of the food as the catalog holds it', async () => {
    const user = await openRecipesTab();
    await screen.findByText('Guiso de arroz');

    await user.click(screen.getByRole('button', { name: '+ Receta' }));

    expect(await screen.findByText(/si el arroz está registrado seco/i)).toBeInTheDocument();
  });
});
