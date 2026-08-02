import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { AdminPage } from '../AdminPage';
import { NotificationProvider } from '../../components/NotificationProvider';
import { listFoods } from '../../api/foods';
import { listCategories, updateCategory, type CategoryDisplay } from '../../api/categories';

vi.mock('../../api/foods', () => ({
  listFoods: vi.fn(),
  createFood: vi.fn(),
  updateFood: vi.fn(),
  deleteFood: vi.fn(),
}));
vi.mock('../../api/storeProducts', () => ({
  listStoreProducts: vi.fn(),
  createStoreProduct: vi.fn(),
  updateStoreProduct: vi.fn(),
  deleteStoreProduct: vi.fn(),
  refreshStoreProduct: vi.fn(),
  listStoreSuggestions: vi.fn(),
}));
vi.mock('../../api/categories', () => ({
  listCategories: vi.fn(),
  updateCategory: vi.fn(),
}));

const listMock = vi.mocked(listCategories);
const updateMock = vi.mocked(updateCategory);

const categories: CategoryDisplay[] = [
  { scope: 'FOOD', code: 'LACTEO', label: 'Lácteo', icon: '🥛' },
  { scope: 'FOOD', code: 'PROTEINA', label: 'Proteína', icon: '🍗' },
  { scope: 'SHOPPING', code: 'OTROS', label: 'Otros', icon: '🛒' },
];

async function openCategoriesTab() {
  const user = userEvent.setup();
  render(
    <MemoryRouter>
      <NotificationProvider>
        <AdminPage />
      </NotificationProvider>
    </MemoryRouter>,
  );
  await user.click(await screen.findByRole('tab', { name: 'Categorías' }));
  return user;
}

/**
 * The third catalog: how a category is written and drawn. Same table, same disclosure, same
 * confirmation-before-writing as the other two — the only difference is that this one cannot create
 * or delete, because which categories exist is a schema decision and not a preference.
 */
describe('AdminPage — the categories tab', () => {
  beforeEach(() => {
    vi.mocked(listFoods).mockResolvedValue([]);
    listMock.mockReset();
    updateMock.mockReset();
    listMock.mockResolvedValue(categories);
  });

  it('lists both vocabularies with their icon and which one they belong to', async () => {
    await openCategoriesTab();

    const table = await screen.findByRole('table', { name: 'Categorías' });
    expect(within(table).getByText('Lácteo')).toBeInTheDocument();
    expect(within(table).getAllByText('Macros')).toHaveLength(2);
    expect(within(table).getByText('Compra')).toBeInTheDocument();
    // The stored token, shown because it is what every row points at.
    expect(within(table).getByText('LACTEO')).toBeInTheDocument();
  });

  it('renames a category and re-reads the list', async () => {
    updateMock.mockResolvedValue({ ...categories[0], label: 'Lácteos y derivados' });
    const user = await openCategoriesTab();
    await screen.findByText('Lácteo');

    // Counted from here: the Macros panel also asks for its own labels now, so a
    // total would be counting somebody else's request.
    const before = listMock.mock.calls.length;
    await user.click(screen.getByRole('button', { name: /Editar Lácteo/ }));
    const dialog = await screen.findByRole('dialog');
    const label = within(dialog).getByLabelText('Nombre');
    await user.clear(label);
    await user.type(label, 'Lácteos y derivados');
    await user.click(within(dialog).getByRole('button', { name: 'Guardar' }));

    await waitFor(() =>
      expect(updateMock).toHaveBeenCalledWith(
        'FOOD',
        'LACTEO',
        expect.objectContaining({ label: 'Lácteos y derivados' }),
      ),
    );
    await waitFor(() => expect(listMock.mock.calls.length).toBeGreaterThan(before));
  });

  /**
   * Which categories exist is fixed by the domain enums and the database's CHECK constraints, so
   * the screen offers no way to add or remove one — an action that would always fail is worse than
   * no action.
   */
  it('offers no way to create or delete a category', async () => {
    await openCategoriesTab();
    await screen.findByText('Lácteo');

    expect(screen.queryByRole('button', { name: /Eliminar Lácteo/ })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /^\+/ })).not.toBeInTheDocument();
  });
});
