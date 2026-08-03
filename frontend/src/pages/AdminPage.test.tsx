import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { AdminPage } from './AdminPage';
import { NotificationProvider } from '../components/NotificationProvider';
import { deleteFood, listFoods, updateFood, type CatalogFood } from '../api/foods';
import { listCategories, type CategoryDisplay } from '../api/categories';

vi.mock('../api/foods', () => ({
  listFoods: vi.fn(),
  createFood: vi.fn(),
  updateFood: vi.fn(),
  deleteFood: vi.fn(),
}));
vi.mock('../api/categories', () => ({
  listCategories: vi.fn(),
  updateCategory: vi.fn(),
}));

const listMock = vi.mocked(listFoods);
const categoriesMock = vi.mocked(listCategories);

/**
 * What the backend serves for the FOOD vocabulary. LEGUMBRE is the point: it
 * exists since V43 and no version of this bundle ever hardcoded it, so it can
 * only reach the form by being asked for.
 */
const foodGroups: CategoryDisplay[] = [
  { scope: 'FOOD', code: 'CARBOHIDRATO', label: 'Carbohidrato', icon: '🌾' },
  { scope: 'FOOD', code: 'PROTEINA', label: 'Proteína', icon: '🍗' },
  { scope: 'FOOD', code: 'LEGUMBRE', label: 'Legumbre', icon: '🫘' },
];
const updateMock = vi.mocked(updateFood);
const deleteMock = vi.mocked(deleteFood);

const oats: CatalogFood = {
  id: 'oats',
  name: 'Copos de avena',
  servingSizeG: 60,
  kcal: 370,
  proteinG: 13,
  carbsG: 60,
  fatG: 7,
  foodGroupId: 'CARBOHIDRATO',
};

const chicken: CatalogFood = {
  id: 'chicken',
  name: 'Pechuga pollo',
  servingSizeG: 200,
  kcal: 110,
  proteinG: 23,
  carbsG: 0,
  fatG: 2,
  foodGroupId: 'PROTEINA',
};

function renderPage() {
  return render(
    <MemoryRouter>
      <NotificationProvider>
        <AdminPage />
      </NotificationProvider>
    </MemoryRouter>,
  );
}

describe('AdminPage', () => {
  beforeEach(() => {
    listMock.mockReset();
    updateMock.mockReset();
    deleteMock.mockReset();
    categoriesMock.mockReset();
    categoriesMock.mockResolvedValue(foodGroups);
  });

  it('lists the catalog with its macros per 100 g', async () => {
    listMock.mockResolvedValue([oats, chicken]);

    renderPage();

    expect(await screen.findByRole('tab', { name: 'Macros' })).toBeInTheDocument();
    // Named for assistive tech without a visible card title repeating the
    // column header underneath it.
    const table = screen.getByRole('table', { name: 'Alimentos' });
    expect(screen.queryByRole('heading', { name: 'Alimentos' })).not.toBeInTheDocument();
    expect(within(table).getByText('Copos de avena')).toBeInTheDocument();
    expect(within(table).getByText('Pechuga pollo')).toBeInTheDocument();
    // The category the sheet carries, rendered as a label rather than the stored token.
    expect(within(table).getByText('Carbohidrato')).toBeInTheDocument();
  });

  /**
   * The glyph belongs to the category, so it is drawn in the category's column. In front of the
   * food's name it read as that food's own icon — and every carbohydrate wore the same wheat ear.
   */
  it('draws the category glyph in the category column, not beside the name', async () => {
    listMock.mockResolvedValue([oats]);

    renderPage();
    await screen.findByText('Copos de avena');

    const nameCell = screen.getByText('Copos de avena').closest('td');
    expect(nameCell?.textContent).toBe('Copos de avena');
    const categoryCell = screen.getByText('Carbohidrato').closest('td');
    expect(categoryCell?.textContent).toContain('🌾');
  });

  it('edits a food through a form and re-reads the list', async () => {
    listMock.mockResolvedValue([oats]);
    updateMock.mockResolvedValue({ ...oats, name: 'Avena integral' });
    const user = userEvent.setup();

    renderPage();
    await screen.findByText('Copos de avena');

    await user.click(screen.getByRole('button', { name: /Editar Copos de avena/ }));
    const dialog = await screen.findByRole('dialog');
    const name = within(dialog).getByLabelText('Nombre');
    await user.clear(name);
    await user.type(name, 'Avena integral');
    await user.click(within(dialog).getByRole('button', { name: 'Guardar' }));

    await waitFor(() =>
      expect(updateMock).toHaveBeenCalledWith(
        'oats',
        expect.objectContaining({ name: 'Avena integral' }),
      ),
    );
    await waitFor(() => expect(listMock).toHaveBeenCalledTimes(2));
  });

  it('asks before deleting and removes the food once confirmed', async () => {
    listMock.mockResolvedValueOnce([oats]).mockResolvedValue([]);
    deleteMock.mockResolvedValue(undefined);
    const user = userEvent.setup();

    renderPage();
    await screen.findByText('Copos de avena');

    await user.click(screen.getByRole('button', { name: /Eliminar Copos de avena/ }));
    const dialog = await screen.findByRole('dialog');
    expect(deleteMock).not.toHaveBeenCalled();

    await user.click(within(dialog).getByRole('button', { name: 'Eliminar' }));

    await waitFor(() => expect(deleteMock).toHaveBeenCalledWith('oats'));
    await waitFor(() => expect(listMock).toHaveBeenCalledTimes(2));
  });

  /**
   * The backend refuses to delete a food a shopping product still links to. The
   * screen has to say so rather than leaving the row looking deleted.
   */
  it('surfaces a refused delete and keeps the row', async () => {
    listMock.mockResolvedValue([oats]);
    deleteMock.mockRejectedValue(new Error('en uso'));
    const user = userEvent.setup();

    renderPage();
    await screen.findByText('Copos de avena');

    await user.click(screen.getByRole('button', { name: /Eliminar Copos de avena/ }));
    await user.click(
      within(await screen.findByRole('dialog')).getByRole('button', { name: 'Eliminar' }),
    );

    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo eliminar');
    expect(screen.getByText('Copos de avena')).toBeInTheDocument();
  });

  /**
   * The add action lives in the page header, beside the title, and names what the open tab creates.
   * It used to sit inside the panel, which on a phone pushed the table a row further down for a
   * button the header had room for.
   */
  it('names the add action after what the open tab creates', async () => {
    listMock.mockResolvedValue([oats]);
    const user = userEvent.setup();

    renderPage();
    expect(await screen.findByRole('button', { name: '+ Alimento' })).toBeInTheDocument();

    await user.click(screen.getByRole('tab', { name: 'Compra' }));

    expect(screen.getByRole('button', { name: '+ Producto' })).toBeInTheDocument();
    // Exactly one add action on screen: the header's, for the open tab.
    expect(screen.queryByRole('button', { name: '+ Alimento' })).not.toBeInTheDocument();
  });

  it('opens an empty form from the header action', async () => {
    listMock.mockResolvedValue([oats]);
    const user = userEvent.setup();

    renderPage();
    await screen.findByText('Copos de avena');

    await user.click(screen.getByRole('button', { name: '+ Alimento' }));

    const dialog = await screen.findByRole('dialog', { name: /Nuevo alimento/ });
    expect(within(dialog).getByLabelText('Nombre')).toHaveValue('');
  });

  /**
   * The groups the form offers are the ones the backend has, not the ones this
   * bundle was built with. Before V43 the set was a compiled enum and adding a
   * group meant a deploy; the whole point of making it data is that it no longer
   * does.
   */
  it('offers the food groups the backend serves, including ones it never shipped with', async () => {
    listMock.mockResolvedValue([oats]);
    const user = userEvent.setup();

    renderPage();
    await screen.findByText('Copos de avena');
    await user.click(screen.getByRole('button', { name: '+ Alimento' }));

    const dialog = await screen.findByRole('dialog', { name: /Nuevo alimento/ });
    const select = within(dialog).getByLabelText('Categoría');

    await waitFor(() =>
      expect(within(select).getByRole('option', { name: 'Legumbre' })).toBeInTheDocument(),
    );
    expect(
      within(select)
        .getAllByRole('option')
        .map((option) => option.textContent),
    ).toEqual(['Sin clasificar', 'Carbohidrato', 'Proteína', 'Legumbre']);
  });

  /**
   * Saving a food must never silently reclassify it. If its group is missing
   * from the list — the request is still in flight, or it failed, or the group
   * was retired — the option is added rather than dropped, because a select
   * that cannot show its own value would quietly change it on the next save.
   */
  it('keeps a food filed under a group the list does not contain', async () => {
    categoriesMock.mockResolvedValue([]);
    listMock.mockResolvedValue([oats]);
    const user = userEvent.setup();

    renderPage();
    await user.click(await screen.findByRole('button', { name: 'Editar Copos de avena' }));

    const dialog = await screen.findByRole('dialog', { name: /Editar Copos de avena/ });
    expect(within(dialog).getByLabelText('Categoría')).toHaveValue('CARBOHIDRATO');
  });

  /**
   * The catalog is already 23 foods and grows with every store sheet loaded into
   * it, so the table pages rather than rendering the lot.
   */
  describe('pagination', () => {
    const many = (count: number): CatalogFood[] =>
      Array.from({ length: count }, (_, index) => ({
        ...oats,
        id: `food-${index}`,
        name: `Alimento ${index}`,
      }));

    it('shows one page at a time and moves through the rest', async () => {
      listMock.mockResolvedValue(many(12));
      const user = userEvent.setup();

      renderPage();
      await screen.findByText('Alimento 0');

      expect(screen.getAllByRole('row')).toHaveLength(11); // 10 foods + the header
      expect(screen.queryByText('Alimento 10')).not.toBeInTheDocument();
      expect(screen.getByText('Página 1 de 2')).toBeInTheDocument();

      await user.click(screen.getByRole('button', { name: 'Página siguiente' }));

      expect(await screen.findByText('Alimento 10')).toBeInTheDocument();
      expect(screen.queryByText('Alimento 0')).not.toBeInTheDocument();
      expect(screen.getByText('Página 2 de 2')).toBeInTheDocument();
    });

    it('stays out of the way while everything fits on one page', async () => {
      listMock.mockResolvedValue([oats, chicken]);

      renderPage();
      await screen.findByText('Copos de avena');

      expect(screen.queryByRole('button', { name: 'Página siguiente' })).not.toBeInTheDocument();
    });

    /**
     * Deleting the only row of the last page used to leave the table on a page
     * that no longer exists — an empty table with no way back.
     */
    it('falls back a page when the last row of the last one goes', async () => {
      listMock.mockResolvedValueOnce(many(11)).mockResolvedValue(many(10));
      deleteMock.mockResolvedValue(undefined);
      const user = userEvent.setup();

      renderPage();
      await screen.findByText('Alimento 0');
      await user.click(screen.getByRole('button', { name: 'Página siguiente' }));

      await user.click(await screen.findByRole('button', { name: /Eliminar Alimento 10/ }));
      await user.click(
        within(await screen.findByRole('dialog')).getByRole('button', { name: 'Eliminar' }),
      );

      expect(await screen.findByText('Alimento 0')).toBeInTheDocument();
    });
  });

  /**
   * Seven columns do not fit a phone. The table drops to name + kcal there and
   * the rest of the row — macros and actions — opens on demand, so nothing is
   * reachable only by scrolling sideways.
   */
  describe('on a phone', () => {
    const matchNarrow = (matches: boolean) => {
      window.matchMedia = ((query: string) => ({
        matches,
        media: query,
        onchange: null,
        addEventListener: () => {},
        removeEventListener: () => {},
        addListener: () => {},
        removeListener: () => {},
        dispatchEvent: () => false,
      })) as typeof window.matchMedia;
    };

    afterEach(() => matchNarrow(false));

    /**
     * The panel spells the macros out with their unit. "Prot. 13" in a column
     * header is readable next to six other columns; alone on a phone it is a
     * number with no unit and no basis.
     */
    it('spells out the macros of the open row with their unit and basis', async () => {
      matchNarrow(true);
      listMock.mockResolvedValue([oats]);
      const user = userEvent.setup();

      renderPage();
      await user.click(await screen.findByRole('button', { name: /Copos de avena/ }));

      expect(screen.getByText('Proteínas')).toBeInTheDocument();
      expect(screen.getByText('13 g')).toBeInTheDocument();
      expect(screen.getByText('HC (hidratos)')).toBeInTheDocument();
      expect(screen.getByText('Grasa')).toBeInTheDocument();
      // The ration is a column of its own, so it is not repeated in here.
      expect(screen.queryByText('Ración recomendada')).not.toBeInTheDocument();
      // Without this the figures are grams of nothing in particular.
      expect(screen.getByText('/100 g')).toBeInTheDocument();
    });

    it('hides the macro columns and opens the row to reach them', async () => {
      matchNarrow(true);
      listMock.mockResolvedValue([oats, chicken]);
      const user = userEvent.setup();

      renderPage();
      await screen.findByText('Copos de avena');

      // What a phone keeps: what to eat, what it costs, how much of it.
      expect(screen.getByRole('columnheader', { name: 'kcal' })).toBeInTheDocument();
      expect(screen.getByRole('columnheader', { name: 'Ración' })).toBeInTheDocument();
      expect(screen.queryByRole('columnheader', { name: 'HC' })).not.toBeInTheDocument();
      expect(
        screen.queryByRole('button', { name: /Editar Copos de avena/ }),
      ).not.toBeInTheDocument();

      await user.click(screen.getByRole('button', { name: /Copos de avena/ }));

      expect(screen.getByRole('button', { name: /Editar Copos de avena/ })).toBeInTheDocument();
      expect(screen.getByText('Carbohidrato')).toBeInTheDocument();
      // One row open at a time: the phone has no room for two detail panels.
      await user.click(screen.getByRole('button', { name: /Pechuga pollo/ }));
      expect(
        screen.queryByRole('button', { name: /Editar Copos de avena/ }),
      ).not.toBeInTheDocument();
    });
  });
});
