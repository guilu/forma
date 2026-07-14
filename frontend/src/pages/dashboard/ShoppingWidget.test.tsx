import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { ShoppingWidget } from './ShoppingWidget';
import { getShoppingList, type ShoppingList } from '../../api/shopping';

vi.mock('../../api/shopping', () => ({ getShoppingList: vi.fn() }));

const shoppingMock = vi.mocked(getShoppingList);

function renderWidget() {
  return render(
    <MemoryRouter>
      <ShoppingWidget />
    </MemoryRouter>,
  );
}

const list: ShoppingList = {
  weekStartDate: '2026-07-06',
  status: 'ACTIVE',
  items: [
    {
      id: 'i1',
      productId: 'p1',
      productName: 'Avena',
      category: 'CEREALES_Y_LEGUMBRES',
      quantity: 1,
      estimatedCostEur: 3.5,
      checked: false,
    },
  ],
  budget: { weeklyEur: 103.8, monthlyEur: 451.2 },
};

/** Matches on digits + currency symbol, tolerant of the non-breaking space Intl.NumberFormat inserts. */
function matchesAmount(expectedDigits: string) {
  return (text: string) => text.includes(expectedDigits) && text.includes('€');
}

describe('ShoppingWidget', () => {
  beforeEach(() => {
    shoppingMock.mockReset();
  });

  it('shows a loading state while the request resolves', () => {
    shoppingMock.mockReturnValue(new Promise(() => {}));

    renderWidget();

    expect(screen.getByRole('status')).toHaveTextContent('Cargando tu presupuesto');
  });

  it('renders the weekly total and monthly estimate', async () => {
    shoppingMock.mockResolvedValue(list);

    renderWidget();

    expect(await screen.findByRole('heading', { name: 'Presupuesto semanal' })).toBeInTheDocument();
    expect(screen.getByText(matchesAmount('103,80'))).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Estimado mensual' })).toBeInTheDocument();
    expect(screen.getByText(matchesAmount('451,20'))).toBeInTheDocument();
  });

  it('shows an empty state when the list has no items', async () => {
    shoppingMock.mockResolvedValue({ ...list, items: [] });

    renderWidget();

    // Loading and empty are both announced via role="status" (FOR-60 shared
    // states), so wait for the terminal content instead of the first match.
    await waitFor(() => {
      expect(screen.getByRole('status')).toHaveTextContent(
        'Aún no hay una lista de compra generada',
      );
    });
  });

  it('shows an error state when the request fails', async () => {
    shoppingMock.mockRejectedValue(new Error('network'));

    renderWidget();

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'No se pudo cargar tu lista de compra',
    );
  });

  it('links to the shopping feature page', async () => {
    shoppingMock.mockResolvedValue(list);

    renderWidget();

    expect(await screen.findByRole('link', { name: 'Ver más' })).toHaveAttribute(
      'href',
      '/lista-compra',
    );
  });
});
