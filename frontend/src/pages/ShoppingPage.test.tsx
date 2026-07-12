import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ShoppingPage } from './ShoppingPage';
import {
  getShoppingList,
  listShoppingProducts,
  setItemChecked,
  updateShoppingProduct,
  type ShoppingList,
  type ShoppingProduct,
} from '../api/shopping';

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

const list: ShoppingList = {
  weekStartDate: '2026-07-06',
  status: 'ACTIVE',
  items: [
    { id: 'i1', productName: 'Avena 1 kg', quantity: 2, estimatedCostEur: 3.9, checked: false },
    { id: 'i2', productName: 'Pollo 1 kg', quantity: 3, estimatedCostEur: 16.5, checked: true },
  ],
  budget: { weeklyEur: 24.6, monthlyEur: 106.52 },
};

const avenaProduct: ShoppingProduct = {
  id: 'p1',
  name: 'Avena 1 kg',
  url: 'https://tienda.example/avena',
  estimatedPriceEur: 1.95,
};

describe('ShoppingPage', () => {
  beforeEach(() => {
    getListMock.mockReset();
    setCheckedMock.mockReset();
    listProductsMock.mockReset();
    updateProductMock.mockReset();
  });

  it('renders the checklist grouped under a single "Todas" tab, with items and the budget', async () => {
    getListMock.mockResolvedValue(list);

    render(<ShoppingPage />);

    const avenaCheckbox = await screen.findByRole('checkbox', { name: /Avena 1 kg/ });
    expect(screen.getByRole('checkbox', { name: /Pollo 1 kg/ })).toBeChecked();

    // Category filter tabs — no category data exists on the API, so a single
    // documented "Todas" group/tab is shown (see ShoppingPage.tsx comment).
    const tab = screen.getByRole('tab', { name: /Todas/ });
    expect(tab).toHaveAttribute('aria-selected', 'true');

    // Per-item row: quantity + price, scoped to the Avena row.
    const avenaRow = avenaCheckbox.closest('li') as HTMLElement;
    expect(within(avenaRow).getByText('2')).toBeInTheDocument();
    expect(within(avenaRow).getByText(/3,90/)).toBeInTheDocument();

    // Budget summary: product count, weekly total, monthly estimate (EUR, es-ES comma).
    expect(screen.getByText('Productos')).toBeInTheDocument();
    expect(screen.getByText(/productos únicos/)).toBeInTheDocument();
    expect(screen.getByText(/24,60/)).toBeInTheDocument();
    expect(screen.getByText(/106,52/)).toBeInTheDocument();
  });

  it('checks an item and reflects the new state', async () => {
    getListMock.mockResolvedValue(list);
    setCheckedMock.mockResolvedValue({ id: 'i1', checked: true });
    const user = userEvent.setup();

    render(<ShoppingPage />);
    const checkbox = await screen.findByRole('checkbox', { name: /Avena 1 kg/ });

    await user.click(checkbox);

    await waitFor(() => expect(setCheckedMock).toHaveBeenCalledWith('i1', true));
    expect(await screen.findByRole('checkbox', { name: /Avena 1 kg/ })).toBeChecked();
  });

  it('shows an error and preserves the list when a toggle fails', async () => {
    getListMock.mockResolvedValue(list);
    setCheckedMock.mockRejectedValue(new Error('network'));
    const user = userEvent.setup();

    render(<ShoppingPage />);
    await user.click(await screen.findByRole('checkbox', { name: /Avena 1 kg/ }));

    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo actualizar');
    // List preserved.
    expect(screen.getByRole('checkbox', { name: /Pollo 1 kg/ })).toBeInTheDocument();
  });

  it('shows an error state with a retry action when the list fails to load', async () => {
    getListMock.mockRejectedValueOnce(new Error('network'));
    getListMock.mockResolvedValueOnce(list);
    const user = userEvent.setup();

    render(<ShoppingPage />);

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

    render(<ShoppingPage />);

    expect(await screen.findByText(/No hay artículos en la lista/)).toBeInTheDocument();
    expect(screen.getAllByText(/0,00/).length).toBeGreaterThan(0);
  });

  it('reaches the product price/URL edit entry point and saves changes', async () => {
    getListMock.mockResolvedValue(list);
    listProductsMock.mockResolvedValue([avenaProduct]);
    updateProductMock.mockResolvedValue({ ...avenaProduct, estimatedPriceEur: 2.1 });
    const user = userEvent.setup();

    render(<ShoppingPage />);
    await screen.findByRole('checkbox', { name: /Avena 1 kg/ });

    await user.click(screen.getByRole('button', { name: /Editar producto Avena 1 kg/ }));

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

  it('shows a not-found message when no product matches the item name', async () => {
    getListMock.mockResolvedValue(list);
    listProductsMock.mockResolvedValue([]);
    const user = userEvent.setup();

    render(<ShoppingPage />);
    await screen.findByRole('checkbox', { name: /Avena 1 kg/ });

    await user.click(screen.getByRole('button', { name: /Editar producto Avena 1 kg/ }));

    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo encontrar el producto');
  });
});
