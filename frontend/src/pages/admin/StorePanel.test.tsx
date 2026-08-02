import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { AdminPage } from '../AdminPage';
import { NotificationProvider } from '../../components/NotificationProvider';
import { listFoods } from '../../api/foods';
import {
  createStoreProduct,
  deleteStoreProduct,
  listStoreProducts,
  listStoreSuggestions,
  updateStoreProduct,
  type StoreProduct,
} from '../../api/storeProducts';

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
  listStoreSuggestions: vi.fn(),
}));

const listMock = vi.mocked(listStoreProducts);
const updateMock = vi.mocked(updateStoreProduct);
const deleteMock = vi.mocked(deleteStoreProduct);
const createMock = vi.mocked(createStoreProduct);
const suggestMock = vi.mocked(listStoreSuggestions);

const oats: StoreProduct = {
  id: 'mercadona-oats',
  store: 'MERCADONA',
  name: 'Copos de avena Brüggen',
  foodId: 'oats',
  packageSize: '500 g',
  priceEur: 1.55,
  url: 'https://tienda.mercadona.es/product/86341',
  category: 'CEREALES_Y_LEGUMBRES',
};

const salmon: StoreProduct = {
  id: 'mercadona-salmon',
  store: 'MERCADONA',
  name: 'Salmón',
  foodId: 'salmon',
  packageSize: 'kg',
  priceEur: 14.5,
  category: 'PROTEINAS',
  notes: 'Principal variable de presupuesto',
};

async function openStoreTab() {
  const user = userEvent.setup();
  render(
    <MemoryRouter>
      <NotificationProvider>
        <AdminPage />
      </NotificationProvider>
    </MemoryRouter>,
  );
  await user.click(await screen.findByRole('tab', { name: 'Compra' }));
  return user;
}

describe('AdminPage — the shopping catalog tab', () => {
  beforeEach(() => {
    vi.mocked(listFoods).mockResolvedValue([]);
    listMock.mockReset();
    updateMock.mockReset();
    deleteMock.mockReset();
    listMock.mockResolvedValue([oats, salmon]);
  });

  afterEach(() => vi.clearAllMocks());

  it('lists the catalog with its store, price and package', async () => {
    await openStoreTab();

    const table = await screen.findByRole('table', { name: 'Productos' });
    expect(within(table).getByText('Copos de avena Brüggen')).toBeInTheDocument();
    // Both rows carry the chain: it is a column, not a tab.
    expect(within(table).getAllByText('Mercadona')).toHaveLength(2);
    // Labels, not the stored tokens.
    expect(within(table).getByText('Cereales y legumbres')).toBeInTheDocument();
    expect(within(table).getByText('1,55 €')).toBeInTheDocument();
    expect(within(table).getByText('500 g')).toBeInTheDocument();
  });

  /**
   * One table holds every chain (V36), so the filter is a query rather than a
   * tab per supermarket — adding Carrefour must not add a tab.
   */
  it('narrows the catalog to one chain', async () => {
    const user = await openStoreTab();
    await screen.findByText('Copos de avena Brüggen');

    await user.selectOptions(screen.getByLabelText('Tienda'), 'MERCADONA');

    await waitFor(() => expect(listMock).toHaveBeenLastCalledWith('MERCADONA'));
  });

  it('edits a product through a form and re-reads the list', async () => {
    updateMock.mockResolvedValue({ ...oats, name: 'Avena Brüggen' });
    const user = await openStoreTab();
    await screen.findByText('Copos de avena Brüggen');

    await user.click(screen.getByRole('button', { name: /Editar Copos de avena Brüggen/ }));
    const dialog = await screen.findByRole('dialog');
    const name = within(dialog).getByLabelText('Nombre');
    await user.clear(name);
    await user.type(name, 'Avena Brüggen');
    await user.click(within(dialog).getByRole('button', { name: 'Guardar' }));

    await waitFor(() =>
      expect(updateMock).toHaveBeenCalledWith(
        'mercadona-oats',
        expect.objectContaining({ name: 'Avena Brüggen', store: 'MERCADONA' }),
      ),
    );
    await waitFor(() => expect(listMock).toHaveBeenCalledTimes(2));
  });

  it('asks before deleting and removes the product once confirmed', async () => {
    deleteMock.mockResolvedValue(undefined);
    const user = await openStoreTab();
    await screen.findByText('Salmón');

    await user.click(screen.getByRole('button', { name: /Eliminar Salmón/ }));
    expect(deleteMock).not.toHaveBeenCalled();

    await user.click(
      within(await screen.findByRole('dialog')).getByRole('button', { name: 'Eliminar' }),
    );

    await waitFor(() => expect(deleteMock).toHaveBeenCalledWith('mercadona-salmon'));
  });

  /**
   * The linked food is a select over the food catalog. When the catalog request
   * fails or has not landed, a product's own food is not among the options — and
   * a select whose value matches no option renders blank. It showed "Sin
   * enlazar" over a product that was linked: the control was lying about the
   * data, and the operator's next save would be made on that reading.
   */
  it('still shows the linked food when the catalog options have not loaded', async () => {
    vi.mocked(listFoods).mockResolvedValue([]);
    const user = await openStoreTab();
    await screen.findByText('Copos de avena Brüggen');

    await user.click(screen.getByRole('button', { name: /Editar Copos de avena Brüggen/ }));
    const dialog = await screen.findByRole('dialog');

    expect(within(dialog).getByLabelText('Alimento enlazado')).toHaveValue('oats');
  });

  /**
   * The catalog's whole point is being able to check a product on the shelf, so the open row links
   * out to it. A product with no link shows nothing rather than a dead one.
   */
  describe('the link to the store', () => {
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

    it('opens the product page from the unfolded row', async () => {
      matchNarrow(true);
      const user = await openStoreTab();
      await user.click(await screen.findByRole('button', { name: /Copos de avena Brüggen/ }));

      const link = screen.getByRole('link', { name: /Ver en Mercadona/ });
      expect(link).toHaveAttribute('href', 'https://tienda.mercadona.es/product/86341');
      // A link to somebody else's site: never hand them the opener.
      expect(link).toHaveAttribute('target', '_blank');
      expect(link).toHaveAttribute('rel', expect.stringContaining('noopener'));
    });

    it('shows no link for a product that has none', async () => {
      matchNarrow(true);
      const user = await openStoreTab();
      // Salmón carries no url in this fixture.
      await user.click(await screen.findByRole('button', { name: /Salmón/ }));

      expect(screen.queryByRole('link', { name: /Ver en/ })).not.toBeInTheDocument();
    });
  });

  /** A product with no price yet is a real state, not a zero. */
  it('shows an unpriced product as unknown rather than free', async () => {
    listMock.mockResolvedValue([{ ...oats, priceEur: undefined, packageSize: undefined }]);

    await openStoreTab();

    const row = (await screen.findByText('Copos de avena Brüggen')).closest('tr');
    expect(within(row!).getAllByText('—').length).toBeGreaterThan(0);
  });

  /**
   * Importing is a confirmation step, never an automatic write: Mercadona can say what a product
   * is called and what it costs, but not which food it is or which of our six aisles it belongs to.
   * Those two the admin supplies, on the same form a hand-typed product uses.
   */
  describe('importing from a store', () => {
    beforeEach(() => {
      vi.mocked(listFoods).mockResolvedValue([
        {
          id: 'oats',
          name: 'Copos de avena',
          kcal: 370,
          proteinG: 13,
          carbsG: 60,
          fatG: 7,
          servingSizeG: 60,
          category: 'CARBOHIDRATO',
        },
      ]);
      suggestMock.mockReset();
      createMock.mockReset();
    });

    it("offers the shop's products for a chosen food and prefills the form with one", async () => {
      suggestMock.mockResolvedValue([
        {
          externalId: '4241',
          name: 'Copos de avena Brüggen',
          packaging: 'Caja 500 g',
          priceEur: 1.55,
          url: 'https://tienda.mercadona.es/product/4241',
          storeCategory: 'Cereales',
        },
      ]);
      createMock.mockResolvedValue(oats);
      const user = await openStoreTab();

      await user.click(screen.getByRole('button', { name: 'Importar de Mercadona' }));
      await user.selectOptions(await screen.findByLabelText('Alimento'), 'oats');

      await waitFor(() => expect(suggestMock).toHaveBeenCalledWith('oats', 'MERCADONA'));
      // Scoped to the dialog: the catalog behind it lists a product with the
      // same name, which is exactly the situation an import is for.
      const picker = await screen.findByRole('dialog', { name: /Importar de Mercadona/ });
      expect(within(picker).getByText('Copos de avena Brüggen')).toBeInTheDocument();
      expect(within(picker).getByText(/1,55/)).toBeInTheDocument();

      await user.click(within(picker).getByRole('button', { name: /Usar Copos de avena Brüggen/ }));

      const form = await screen.findByRole('dialog', { name: /Nuevo producto/ });
      expect(within(form).getByLabelText('Nombre')).toHaveValue('Copos de avena Brüggen');
      expect(within(form).getByLabelText('Precio (€)')).toHaveValue(1.55);
      // The link to the food is the whole point of importing this way.
      expect(within(form).getByLabelText('Alimento enlazado')).toHaveValue('oats');
      // Derived from the store's own id, which is stable; ours never is.
      expect(within(form).getByLabelText('Identificador')).toHaveValue('mercadona-4241');
    });

    it('says so when the shop has nothing that matches', async () => {
      suggestMock.mockResolvedValue([]);
      const user = await openStoreTab();

      await user.click(screen.getByRole('button', { name: 'Importar de Mercadona' }));
      await user.selectOptions(await screen.findByLabelText('Alimento'), 'oats');

      expect(await screen.findByText(/no ha encontrado/i)).toBeInTheDocument();
    });

    /** The shop being down must read as "vuelve a intentarlo", not as "ese alimento no existe". */
    it('reports a shop that cannot be reached without losing the screen', async () => {
      suggestMock.mockRejectedValue(new Error('502'));
      const user = await openStoreTab();

      await user.click(screen.getByRole('button', { name: 'Importar de Mercadona' }));
      await user.selectOptions(await screen.findByLabelText('Alimento'), 'oats');

      expect(await screen.findByRole('alert')).toHaveTextContent(/Mercadona/);
      // The catalog underneath is untouched.
      expect(screen.getByText('Salmón')).toBeInTheDocument();
    });
  });
});
