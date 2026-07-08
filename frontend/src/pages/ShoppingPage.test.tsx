import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ShoppingPage } from './ShoppingPage';
import { getShoppingList, setItemChecked, type ShoppingList } from '../api/shopping';

vi.mock('../api/shopping', () => ({
  getShoppingList: vi.fn(),
  setItemChecked: vi.fn(),
}));

const getListMock = vi.mocked(getShoppingList);
const setCheckedMock = vi.mocked(setItemChecked);

const list: ShoppingList = {
  weekStartDate: '2026-07-06',
  status: 'ACTIVE',
  items: [
    { id: 'i1', productName: 'Avena 1 kg', quantity: 2, estimatedCostEur: 3.9, checked: false },
    { id: 'i2', productName: 'Pollo 1 kg', quantity: 3, estimatedCostEur: 16.5, checked: true },
  ],
  budget: { weeklyEur: 24.6, monthlyEur: 106.52 },
};

describe('ShoppingPage', () => {
  beforeEach(() => {
    getListMock.mockReset();
    setCheckedMock.mockReset();
  });

  it('renders the checklist with items and the weekly + monthly budget', async () => {
    getListMock.mockResolvedValue(list);

    render(<ShoppingPage />);

    expect(await screen.findByRole('checkbox', { name: /Avena 1 kg/ })).toBeInTheDocument();
    expect(screen.getByRole('checkbox', { name: /Pollo 1 kg/ })).toBeChecked();
    // Weekly total and monthly estimate, EUR-formatted (es-ES uses a comma).
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

  it('shows an error state when the list fails to load', async () => {
    getListMock.mockRejectedValue(new Error('network'));

    render(<ShoppingPage />);

    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo cargar');
  });
});
