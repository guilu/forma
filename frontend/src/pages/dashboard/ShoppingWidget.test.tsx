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
  generatedAt: '2026-07-06T08:00:00Z',
  items: [
    {
      id: 'i1',
      productId: 'p1',
      productName: 'Avena',
      category: 'CEREALES_Y_LEGUMBRES',
      quantity: 1,
      unit: 'UD',
      servings: null,
      estimatedCostEur: 3.5,
      checked: false,
    },
  ],
  budget: { weeklyEur: 103.8, monthlyEur: 451.2 },
};

describe('ShoppingWidget (Lista de compra)', () => {
  beforeEach(() => {
    shoppingMock.mockReset();
  });

  it('shows a loading state while the request resolves', () => {
    shoppingMock.mockReturnValue(new Promise(() => {}));

    renderWidget();

    expect(screen.getByRole('status')).toHaveTextContent('Cargando tu lista de compra');
  });

  it('renders a preview of the list items with product name and quantity + unit', async () => {
    shoppingMock.mockResolvedValue(list);

    renderWidget();

    expect(await screen.findByText('Avena')).toBeInTheDocument();
    // UD renders as "unidades" (FOR-164 unit labels).
    expect(screen.getByText('1 unidades')).toBeInTheDocument();
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

  it('links to the shopping feature page via "Ver lista completa"', async () => {
    shoppingMock.mockResolvedValue(list);

    renderWidget();

    expect(await screen.findByRole('link', { name: 'Ver lista completa' })).toHaveAttribute(
      'href',
      '/lista-compra',
    );
  });
});
