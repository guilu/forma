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
  fetchLinkImage,
  searchStoreProducts,
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
  searchStoreProducts: vi.fn(),
  refreshStoreProduct: vi.fn(),
  fetchLinkImage: vi.fn(),
}));

const listMock = vi.mocked(listStoreProducts);
const updateMock = vi.mocked(updateStoreProduct);
const deleteMock = vi.mocked(deleteStoreProduct);
const refreshMock = vi.mocked(refreshStoreProduct);
const searchMock = vi.mocked(searchStoreProducts);
const imageMock = vi.mocked(fetchLinkImage);

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

  /**
   * A catalog of hundreds is read by sorting it. Every column sorts except Formato, whose values
   * are free text ("Caja 0.8 kg", "kg", "Paquete 12 ud") and would sort alphabetically into an
   * order that means nothing.
   */
  describe('sorting', () => {
    const names = () =>
      screen
        .getAllByRole('row')
        .slice(1)
        .map((row) => row.querySelector('td')?.textContent);

    it('sorts by a column and reverses on a second click', async () => {
      const user = await openStoreTab();
      await screen.findByText('Salmón');

      await user.click(screen.getByRole('button', { name: 'Producto' }));
      expect(names()).toEqual(['Copos de avena Brüggen', 'Salmón']);

      await user.click(screen.getByRole('button', { name: 'Producto' }));
      expect(names()).toEqual(['Salmón', 'Copos de avena Brüggen']);
    });

    it('sorts numbers as numbers, not as text', async () => {
      listMock.mockResolvedValue([
        { ...oats, id: 'a', name: 'A', priceEur: 9 },
        { ...oats, id: 'b', name: 'B', priceEur: 10 },
        { ...oats, id: 'c', name: 'C', priceEur: 1.5 },
      ]);
      const user = await openStoreTab();
      await screen.findByText('A');

      await user.click(screen.getByRole('button', { name: 'Precio' }));

      expect(names()).toEqual(['C', 'A', 'B']);
    });

    it('does not offer sorting on the free-text format column', async () => {
      await openStoreTab();
      await screen.findByText('Salmón');

      expect(screen.queryByRole('button', { name: 'Formato' })).not.toBeInTheDocument();
    });

    /** The state of the sort has to be announced, not just drawn. */
    it('tells assistive tech which way a column is sorted', async () => {
      const user = await openStoreTab();
      await screen.findByText('Salmón');

      await user.click(screen.getByRole('button', { name: 'Producto' }));

      expect(screen.getByRole('columnheader', { name: 'Producto' })).toHaveAttribute(
        'aria-sort',
        'ascending',
      );
    });
  });

  /**
   * The wide table had no way through to the shop: the link out lived only in the phone layout's
   * disclosure. The product's name is the obvious door.
   */
  it('links the product name to its page in the shop', async () => {
    await openStoreTab();
    await screen.findByText('Copos de avena Brüggen');

    const link = screen.getByRole('link', { name: 'Copos de avena Brüggen' });
    expect(link).toHaveAttribute('href', 'https://tienda.mercadona.es/product/86341');
    expect(link).toHaveAttribute('target', '_blank');
    expect(link).toHaveAttribute('rel', expect.stringContaining('noopener'));
  });

  /**
   * Importing from a food only reaches what our own catalog names. Boniato and whey protein are
   * the proof: V40 could not match them, and there is no food row to start from for anything the
   * catalog does not describe. So the store itself is searchable by name.
   */
  describe('importing by searching the shop', () => {
    it('waits for a specific chain before offering the search', async () => {
      const user = await openStoreTab();
      await screen.findByText('Salmón');

      expect(screen.getByRole('button', { name: 'Importar desde tienda' })).toBeDisabled();

      await user.selectOptions(screen.getByLabelText('Tienda'), 'MERCADONA');

      expect(screen.getByRole('button', { name: 'Importar desde tienda' })).toBeEnabled();
    });

    it('searches the shop by name and prefills the form with a result', async () => {
      searchMock.mockResolvedValue([
        {
          externalId: '86809',
          name: 'Almendra natural Hacendado',
          packaging: 'Paquete 0.2 kg',
          priceEur: 2.3,
          storeCategory: 'Frutos secos y fruta desecada',
          imageUrl: 'https://prod-mercadona.imgix.net/images/x.jpg?fit=crop&h=24&w=24',
        },
      ]);
      const user = await openStoreTab();
      await screen.findByText('Salmón');
      await user.selectOptions(screen.getByLabelText('Tienda'), 'MERCADONA');
      await user.click(screen.getByRole('button', { name: 'Importar desde tienda' }));

      const dialog = await screen.findByRole('dialog', { name: /Importar de Mercadona/ });
      await user.type(within(dialog).getByLabelText(/Buscar en Mercadona/), 'almendra');
      await user.click(within(dialog).getByRole('button', { name: /Buscar/ }));

      await waitFor(() => expect(searchMock).toHaveBeenCalledWith('almendra', 'MERCADONA'));
      await user.click(
        await within(dialog).findByRole('button', { name: /Usar Almendra natural Hacendado/ }),
      );

      const form = await screen.findByRole('dialog', { name: /Nuevo producto/ });
      expect(within(form).getByLabelText('Identificador')).toHaveValue('mercadona-86809');
      expect(within(form).getByLabelText('Precio (€)')).toHaveValue(2.3);
      // Nothing to link it to: this search started from the shop, not from a food.
      expect(within(form).getByLabelText('Alimento enlazado')).toHaveValue('');
    });

    /** Asking before anything is typed would be a request for the whole shop. */
    it('asks for something to search for before searching', async () => {
      const user = await openStoreTab();
      await screen.findByText('Salmón');
      await user.selectOptions(screen.getByLabelText('Tienda'), 'MERCADONA');
      await user.click(screen.getByRole('button', { name: 'Importar desde tienda' }));

      const dialog = await screen.findByRole('dialog', { name: /Importar de Mercadona/ });
      expect(within(dialog).getByRole('button', { name: /Buscar/ })).toBeDisabled();
      expect(searchMock).not.toHaveBeenCalled();
    });
  });

  /**
   * Plenty of what a plan is built from comes from nowhere we track — whey protein bought online,
   * bread from a local baker. Those rows still belong in the catalog, and they still deserve a
   * picture: the form takes an image URL, and offers to read one off the product page.
   */
  describe('a product from no shop we track', () => {
    it('offers a store for what no supermarket sells', async () => {
      const user = await openStoreTab();
      await screen.findByText('Salmón');

      await user.click(screen.getByRole('button', { name: '+ Producto' }));
      const form = await screen.findByRole('dialog', { name: /Nuevo producto/ });

      expect(within(form).getByRole('option', { name: 'Otras' })).toBeInTheDocument();
    });

    it('reads the photo off the product page', async () => {
      imageMock.mockResolvedValue('https://m.media-amazon.com/images/I/31kt192oAzL._AC_.jpg');
      const user = await openStoreTab();
      await screen.findByText('Salmón');
      await user.click(screen.getByRole('button', { name: '+ Producto' }));
      const form = await screen.findByRole('dialog', { name: /Nuevo producto/ });

      await user.type(within(form).getByLabelText('Enlace'), 'https://www.amazon.es/dp/B07Q31N9D4');
      await user.click(within(form).getByRole('button', { name: /Obtener imagen/ }));

      await waitFor(() =>
        expect(imageMock).toHaveBeenCalledWith('https://www.amazon.es/dp/B07Q31N9D4'),
      );
      expect(await within(form).findByLabelText('Imagen (URL)')).toHaveValue(
        'https://m.media-amazon.com/images/I/31kt192oAzL._AC_.jpg',
      );
    });

    /** A page that publishes nothing is not a failure; the field is there to be filled by hand. */
    it('says so when the page advertises no photo', async () => {
      imageMock.mockResolvedValue(undefined);
      const user = await openStoreTab();
      await screen.findByText('Salmón');
      await user.click(screen.getByRole('button', { name: '+ Producto' }));
      const form = await screen.findByRole('dialog', { name: /Nuevo producto/ });

      await user.type(within(form).getByLabelText('Enlace'), 'https://tienda.example/p');
      await user.click(within(form).getByRole('button', { name: /Obtener imagen/ }));

      expect(await within(form).findByText(/no publica ninguna imagen/i)).toBeInTheDocument();
    });

    it('has nothing to read without a link', async () => {
      const user = await openStoreTab();
      await screen.findByText('Salmón');
      await user.click(screen.getByRole('button', { name: '+ Producto' }));
      const form = await screen.findByRole('dialog', { name: /Nuevo producto/ });

      expect(within(form).getByRole('button', { name: /Obtener imagen/ })).toBeDisabled();
    });
  });
});
