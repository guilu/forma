import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { AdminPage } from '../AdminPage';
import { NotificationProvider } from '../../components/NotificationProvider';
import { listFoods } from '../../api/foods';
import {
  deleteStoreProduct,
  listStoreProducts,
  refreshStoreProduct,
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
  refreshStoreProduct: vi.fn(),
}));

const listMock = vi.mocked(listStoreProducts);
const updateMock = vi.mocked(updateStoreProduct);
const deleteMock = vi.mocked(deleteStoreProduct);
const refreshMock = vi.mocked(refreshStoreProduct);

const oats: StoreProduct = {
  id: 'mercadona-oats',
  store: 'MERCADONA',
  name: 'Copos de avena Brüggen',
  foodId: 'oats',
  packageSize: '500 g',
  priceEur: 1.55,
  url: 'https://tienda.mercadona.es/product/86341',
  category: 'CEREALES_Y_LEGUMBRES',
  externalId: '4241',
  imageUrl: 'https://prod-mercadona.imgix.net/images/abc.jpg?fit=crop&h=300&w=300',
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
   * The shop's own photo goes in front of the product; the category glyph goes in the category's
   * column. Both in the same place meant two icons saying different things about one row — one of
   * this product, one of its aisle.
   */
  it('shows the store photo beside the name and the category glyph in its own column', async () => {
    await openStoreTab();
    await screen.findByText('Copos de avena Brüggen');

    const nameCell = screen.getByText('Copos de avena Brüggen').closest('td');
    expect(within(nameCell!).getByRole('presentation', { hidden: true })).toHaveAttribute(
      'src',
      expect.stringContaining('h=24&w=24'),
    );
    expect(nameCell?.textContent).toBe('Copos de avena Brüggen');
    expect(screen.getByText('Cereales y legumbres').closest('td')?.textContent).toContain('🌾');
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
   * A price moves every week; a row imported a month ago is a price nobody checked. Refresh takes
   * the shop's figures again and leaves ours alone — the server decides what changed, so the screen
   * re-reads the list instead of patching the row with its own guess.
   */
  describe('refreshing an imported product', () => {
    it('re-reads the product from the shop and reloads the list', async () => {
      refreshMock.mockResolvedValue({ ...oats, priceEur: 1.79 });
      const user = await openStoreTab();
      await screen.findByText('Copos de avena Brüggen');

      await user.click(screen.getByRole('button', { name: /Refrescar Copos de avena Brüggen/ }));

      await waitFor(() => expect(refreshMock).toHaveBeenCalledWith('mercadona-oats'));
      await waitFor(() => expect(listMock).toHaveBeenCalledTimes(2));
    });

    /** Nothing to refresh against: the row was typed by hand, not imported. */
    it('offers no refresh for a product that was never imported', async () => {
      listMock.mockResolvedValue([{ ...oats, externalId: undefined }]);
      await openStoreTab();
      await screen.findByText('Copos de avena Brüggen');

      expect(
        screen.queryByRole('button', { name: /Refrescar Copos de avena Brüggen/ }),
      ).not.toBeInTheDocument();
    });

    it('surfaces a refusal without losing the row', async () => {
      refreshMock.mockRejectedValue(new Error('502'));
      const user = await openStoreTab();
      await screen.findByText('Copos de avena Brüggen');

      await user.click(screen.getByRole('button', { name: /Refrescar Copos de avena Brüggen/ }));

      expect(await screen.findByRole('alert')).toHaveTextContent(/No se pudo actualizar/);
      expect(screen.getByText('Copos de avena Brüggen')).toBeInTheDocument();
    });
  });
});
