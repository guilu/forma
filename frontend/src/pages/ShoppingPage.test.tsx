import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ShoppingPage } from './ShoppingPage';
import { buildCategoryTabs, filterItemsByCategory } from './shoppingCategories';
import { NotificationProvider } from '../components/NotificationProvider';
import {
  getShoppingList,
  listShoppingProducts,
  setItemChecked,
  updateShoppingProduct,
  type ShoppingItem,
  type ShoppingList,
  type ShoppingProduct,
} from '../api/shopping';

/** ShoppingPage calls `useNotify()` (FOR-63), which requires a provider. */
function renderPage() {
  return render(
    <NotificationProvider>
      <ShoppingPage />
    </NotificationProvider>,
  );
}

vi.mock('../api/shopping', () => ({
  getShoppingList: vi.fn(),
  setItemChecked: vi.fn(),
  listShoppingProducts: vi.fn(),
  updateShoppingProduct: vi.fn(),
}));

const getListMock = vi.mocked(getShoppingList);
const setCheckedMock = vi.mocked(setItemChecked);
const listProductsMock = vi.mocked(listShoppingProducts);
const updateProductMock = vi.mocked(updateShoppingProduct);

/**
 * Fixture spans four categories (>= three required by tests.md), one `OTROS`
 * item ("Detergente") and two same-name/different-id products ("Leche
 * entera", `p4`/`p5`) — the FOR-111 regression guard for the old
 * name-matching bug.
 */
const list: ShoppingList = {
  weekStartDate: '2026-07-06',
  status: 'ACTIVE',
  items: [
    {
      id: 'i1',
      productId: 'p1',
      productName: 'Avena 1 kg',
      category: 'CEREALES_Y_LEGUMBRES',
      quantity: 2,
      estimatedCostEur: 3.9,
      checked: false,
    },
    {
      id: 'i2',
      productId: 'p2',
      productName: 'Pollo 1 kg',
      category: 'PROTEINAS',
      quantity: 3,
      estimatedCostEur: 16.5,
      checked: true,
    },
    {
      id: 'i3',
      productId: 'p3',
      productName: 'Detergente',
      category: 'OTROS',
      quantity: 1,
      estimatedCostEur: 4.2,
      checked: false,
    },
    {
      id: 'i4',
      productId: 'p4',
      productName: 'Leche entera',
      category: 'LACTEOS_Y_HUEVOS',
      quantity: 2,
      estimatedCostEur: 1.2,
      checked: false,
    },
    {
      id: 'i5',
      productId: 'p5',
      productName: 'Leche entera',
      category: 'LACTEOS_Y_HUEVOS',
      quantity: 1,
      estimatedCostEur: 1.85,
      checked: false,
    },
  ],
  budget: { weeklyEur: 24.6, monthlyEur: 106.52 },
};

const avenaProduct: ShoppingProduct = {
  id: 'p1',
  name: 'Avena 1 kg',
  url: 'https://tienda.example/avena',
  estimatedPriceEur: 1.95,
};

const lecheP4: ShoppingProduct = {
  id: 'p4',
  name: 'Leche entera',
  estimatedPriceEur: 1.2,
};

const lecheP5: ShoppingProduct = {
  id: 'p5',
  name: 'Leche entera',
  estimatedPriceEur: 1.85,
};

describe('ShoppingPage', () => {
  beforeEach(() => {
    getListMock.mockReset();
    setCheckedMock.mockReset();
    listProductsMock.mockReset();
    updateProductMock.mockReset();
  });

  it('renders category tabs (Todas + distinct categories) with items grouped under "Todas" by default', async () => {
    getListMock.mockResolvedValue(list);

    renderPage();

    const avenaCheckbox = await screen.findByRole('checkbox', { name: /Avena 1 kg/ });
    expect(screen.getByRole('checkbox', { name: /Pollo 1 kg/ })).toBeChecked();

    // "Todas" tab is selected by default and covers every item across categories (FOR-111/FOR-106).
    const todasTab = screen.getByRole('tab', { name: /Todas \(5\)/ });
    expect(todasTab).toHaveAttribute('aria-selected', 'true');

    // One tab per distinct category present in the list.
    expect(screen.getByRole('tab', { name: /Cereales y legumbres/ })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: /Proteínas/ })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: /Lácteos y huevos/ })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: /Otros/ })).toBeInTheDocument();

    // Per-item row: quantity + price, scoped to the Avena row.
    const avenaRow = avenaCheckbox.closest('li') as HTMLElement;
    expect(within(avenaRow).getByText('2')).toBeInTheDocument();
    expect(within(avenaRow).getByText(/3,90/)).toBeInTheDocument();

    // Budget summary: product count, weekly total, monthly estimate (EUR, es-ES comma).
    expect(screen.getByText('Productos')).toBeInTheDocument();
    expect(screen.getByText(/productos únicos/)).toBeInTheDocument();
    expect(screen.getByText(/24,60/)).toBeInTheDocument();
    expect(screen.getByText(/106,52/)).toBeInTheDocument();

    // The budget tiles are direct siblings of the page <h1> (no intervening
    // <h2>), so per FOR-112 they must render as <h2>.
    expect(screen.getByRole('heading', { name: 'Productos', level: 2 })).toBeInTheDocument();
  });

  it('filters the rendered items when a category tab is selected; aria-selected updates accordingly', async () => {
    getListMock.mockResolvedValue(list);
    const user = userEvent.setup();

    renderPage();
    await screen.findByRole('checkbox', { name: /Avena 1 kg/ });

    await user.click(screen.getByRole('tab', { name: /Proteínas/ }));

    expect(screen.getByRole('tab', { name: /Proteínas/ })).toHaveAttribute('aria-selected', 'true');
    expect(screen.getByRole('tab', { name: /^Todas/ })).toHaveAttribute('aria-selected', 'false');
    expect(screen.getByRole('checkbox', { name: /Pollo 1 kg/ })).toBeInTheDocument();
    expect(screen.queryByRole('checkbox', { name: /Avena 1 kg/ })).not.toBeInTheDocument();
  });

  it('shows all items again when "Todas" is selected after filtering', async () => {
    getListMock.mockResolvedValue(list);
    const user = userEvent.setup();

    renderPage();
    await screen.findByRole('checkbox', { name: /Avena 1 kg/ });

    await user.click(screen.getByRole('tab', { name: /Proteínas/ }));
    expect(screen.queryByRole('checkbox', { name: /Avena 1 kg/ })).not.toBeInTheDocument();

    await user.click(screen.getByRole('tab', { name: /^Todas/ }));
    expect(screen.getByRole('checkbox', { name: /Avena 1 kg/ })).toBeInTheDocument();
    expect(screen.getByRole('checkbox', { name: /Pollo 1 kg/ })).toBeInTheDocument();
  });

  it('groups items with category OTROS under a clearly-labelled "Otros" tab', async () => {
    getListMock.mockResolvedValue(list);
    const user = userEvent.setup();

    renderPage();
    await screen.findByRole('checkbox', { name: /Avena 1 kg/ });

    await user.click(screen.getByRole('tab', { name: /Otros/ }));

    expect(screen.getByRole('checkbox', { name: /Detergente/ })).toBeInTheDocument();
    expect(screen.queryByRole('checkbox', { name: /Avena 1 kg/ })).not.toBeInTheDocument();
  });

  it('checks an item and reflects the new state', async () => {
    getListMock.mockResolvedValue(list);
    setCheckedMock.mockResolvedValue({ id: 'i1', checked: true });
    const user = userEvent.setup();

    renderPage();
    const checkbox = await screen.findByRole('checkbox', { name: /Avena 1 kg/ });

    await user.click(checkbox);

    await waitFor(() => expect(setCheckedMock).toHaveBeenCalledWith('i1', true));
    expect(await screen.findByRole('checkbox', { name: /Avena 1 kg/ })).toBeChecked();
  });

  it('shows a success notification after toggling an item (FOR-63)', async () => {
    getListMock.mockResolvedValue(list);
    setCheckedMock.mockResolvedValue({ id: 'i1', checked: true });
    const user = userEvent.setup();

    renderPage();
    await user.click(await screen.findByRole('checkbox', { name: /Avena 1 kg/ }));

    const region = screen.getByRole('log');
    expect(await within(region).findByText(/actualizado/i)).toBeInTheDocument();
  });

  it('de-duplicates the toast on rapid repeated toggles of the same item (edge case)', async () => {
    getListMock.mockResolvedValue(list);
    setCheckedMock.mockResolvedValue({ id: 'i1', checked: true });
    const user = userEvent.setup();

    renderPage();
    const checkbox = await screen.findByRole('checkbox', { name: /Avena 1 kg/ });
    await user.click(checkbox);
    await waitFor(() => expect(setCheckedMock).toHaveBeenCalledTimes(1));
    await user.click(checkbox);
    await waitFor(() => expect(setCheckedMock).toHaveBeenCalledTimes(2));

    const region = screen.getByRole('log');
    expect(within(region).getAllByText(/actualizado/i)).toHaveLength(1);
  });

  it('shows an error and preserves the list when a toggle fails', async () => {
    getListMock.mockResolvedValue(list);
    setCheckedMock.mockRejectedValue(new Error('network'));
    const user = userEvent.setup();

    renderPage();
    await user.click(await screen.findByRole('checkbox', { name: /Avena 1 kg/ }));

    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo actualizar');
    // List preserved.
    expect(screen.getByRole('checkbox', { name: /Pollo 1 kg/ })).toBeInTheDocument();
  });

  it('shows an error state with a retry action when the list fails to load', async () => {
    getListMock.mockRejectedValueOnce(new Error('network'));
    getListMock.mockResolvedValueOnce(list);
    const user = userEvent.setup();

    renderPage();

    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo cargar');

    await user.click(screen.getByRole('button', { name: /Reintentar/ }));

    expect(await screen.findByRole('checkbox', { name: /Avena 1 kg/ })).toBeInTheDocument();
    expect(getListMock).toHaveBeenCalledTimes(2);
  });

  it('shows a clear empty state with a 0,00 € total when there is no list this week', async () => {
    getListMock.mockResolvedValue({
      weekStartDate: '2026-07-06',
      status: 'ACTIVE',
      items: [],
      budget: { weeklyEur: 0, monthlyEur: 0 },
    });

    renderPage();

    expect(await screen.findByText(/No hay artículos en la lista/)).toBeInTheDocument();
    expect(screen.getAllByText(/0,00/).length).toBeGreaterThan(0);
  });

  it('reaches the product price/URL edit entry point and saves changes', async () => {
    getListMock.mockResolvedValue(list);
    listProductsMock.mockResolvedValue([avenaProduct]);
    updateProductMock.mockResolvedValue({ ...avenaProduct, estimatedPriceEur: 2.1 });
    const user = userEvent.setup();

    renderPage();
    await screen.findByRole('checkbox', { name: /Avena 1 kg/ });

    await user.click(screen.getByRole('button', { name: /Editar producto Avena 1 kg/ }));

    // The item-list Card (titled after the selected category, "Todas" by
    // default) is also a direct sibling of the page <h1>, so it must render
    // as <h2> too (FOR-112). "Editar producto" is the unrelated Modal dialog
    // title, left untouched by this story.
    expect(screen.getByRole('heading', { name: 'Todas', level: 2 })).toBeInTheDocument();
    expect(await screen.findByRole('heading', { name: 'Editar producto' })).toBeInTheDocument();
    const priceInput = await screen.findByLabelText('Precio estimado (€)');
    expect(priceInput).toHaveValue(1.95);

    await user.clear(priceInput);
    await user.type(priceInput, '2.10');
    await user.click(screen.getByRole('button', { name: 'Guardar' }));

    await waitFor(() =>
      expect(updateProductMock).toHaveBeenCalledWith(
        'p1',
        expect.objectContaining({
          name: 'Avena 1 kg',
          url: 'https://tienda.example/avena',
          estimatedPriceEur: 2.1,
        }),
      ),
    );
    expect(await screen.findByText('Producto actualizado.')).toBeInTheDocument();
  });

  it('shows a not-found message when no product matches the item id', async () => {
    getListMock.mockResolvedValue(list);
    listProductsMock.mockResolvedValue([]);
    const user = userEvent.setup();

    renderPage();
    await screen.findByRole('checkbox', { name: /Avena 1 kg/ });

    await user.click(screen.getByRole('button', { name: /Editar producto Avena 1 kg/ }));

    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo encontrar el producto');
  });

  it('resolves distinct products by id even when two items share the same product name (regression guard)', async () => {
    getListMock.mockResolvedValue(list);
    listProductsMock.mockResolvedValue([lecheP4, lecheP5]);
    const user = userEvent.setup();

    renderPage();
    await screen.findByRole('checkbox', { name: /Avena 1 kg/ });

    const editButtons = screen.getAllByRole('button', { name: /Editar producto Leche entera/ });
    expect(editButtons).toHaveLength(2);

    await user.click(editButtons[0]);
    expect(await screen.findByLabelText('Precio estimado (€)')).toHaveValue(1.2);
    // Modal.tsx exposes two "Cerrar"-named controls (the icon close button and
    // the footer action) — either closes it; index 0 is enough here.
    await user.click(screen.getAllByRole('button', { name: 'Cerrar' })[0]);

    await user.click(editButtons[1]);
    expect(await screen.findByLabelText('Precio estimado (€)')).toHaveValue(1.85);
  });

  it('shows a not-found state when productId no longer resolves, even if another product shares the item name (regression guard)', async () => {
    getListMock.mockResolvedValue({
      weekStartDate: '2026-07-06',
      status: 'ACTIVE',
      items: [
        {
          id: 'ig',
          productId: 'p999',
          productName: 'Producto fantasma',
          category: 'OTROS',
          quantity: 1,
          estimatedCostEur: 2.5,
          checked: false,
        },
      ],
      budget: { weeklyEur: 2.5, monthlyEur: 10 },
    });
    listProductsMock.mockResolvedValue([
      { id: 'p1', name: 'Producto fantasma', estimatedPriceEur: 5 },
    ]);
    const user = userEvent.setup();

    renderPage();
    await screen.findByRole('checkbox', { name: /Producto fantasma/ });

    await user.click(screen.getByRole('button', { name: /Editar producto Producto fantasma/ }));

    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo encontrar el producto');
  });
});

describe('category filtering helpers (FOR-111)', () => {
  const items: ShoppingItem[] = [
    {
      id: 'i1',
      productId: 'p1',
      productName: 'Avena',
      category: 'CEREALES_Y_LEGUMBRES',
      quantity: 1,
      estimatedCostEur: 1,
      checked: false,
    },
  ];

  it('builds "Todas" first, then one tab per distinct category present, with item counts', () => {
    const tabs = buildCategoryTabs(items);
    expect(tabs.map((tab) => tab.label)).toEqual(['Todas (1)', 'Cereales y legumbres (1)']);
  });

  it('falls back an unrecognized/empty category into the "Otros" bucket instead of hiding the item', () => {
    const withUnknown: ShoppingItem[] = [
      ...items,
      {
        id: 'i2',
        productId: 'p2',
        productName: 'Producto sin categoría',
        category: '',
        quantity: 1,
        estimatedCostEur: 1,
        checked: false,
      },
    ];

    const tabs = buildCategoryTabs(withUnknown);
    expect(tabs.map((tab) => tab.label)).toContain('Otros (1)');
    expect(filterItemsByCategory(withUnknown, 'OTROS')).toHaveLength(1);
  });

  it('returns an empty array — not a crash — for a category key with no matching items (e.g. a stale selection after a list refresh)', () => {
    expect(filterItemsByCategory(items, 'PROTEINAS')).toEqual([]);
  });

  it('"ALL" returns every item unfiltered', () => {
    expect(filterItemsByCategory(items, 'ALL')).toEqual(items);
  });
});
