import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { NotificationProvider } from '../../components/NotificationProvider';
import { StoreAislesManager } from './StoreAislesManager';
import {
  listStoreCategories,
  syncStoreCategories,
  type StoreCategory,
} from '../../api/storeCategories';

vi.mock('../../api/storeCategories', () => ({
  listStoreCategories: vi.fn(),
  syncStoreCategories: vi.fn(),
}));

const listMock = vi.mocked(listStoreCategories);
const syncMock = vi.mocked(syncStoreCategories);

/** Two levels, as Mercadona's index publishes them: a heading and a shelf under it. */
const aisles: StoreCategory[] = [
  {
    id: 'MERCADONA:12',
    storeId: 'MERCADONA',
    externalId: '12',
    name: 'Aceite, especias y salsas',
    slug: 'aceite-especias-y-salsas',
    level: 0,
    sortOrder: 0,
  },
  {
    id: 'MERCADONA:112',
    storeId: 'MERCADONA',
    parentId: 'MERCADONA:12',
    externalId: '112',
    name: 'Aceite, vinagre y sal',
    slug: 'aceite-vinagre-y-sal',
    level: 1,
    sortOrder: 0,
  },
];

function renderManager(storeId = 'MERCADONA', storeName = 'Mercadona') {
  return render(
    <NotificationProvider>
      <StoreAislesManager storeId={storeId} storeName={storeName} onClose={() => undefined} />
    </NotificationProvider>,
  );
}

describe('StoreAislesManager', () => {
  beforeEach(() => {
    listMock.mockReset();
    syncMock.mockReset();
    listMock.mockResolvedValue(aisles);
  });

  it('shows the shop tree with the shop own ids', async () => {
    renderManager();

    expect(await screen.findByText('Aceite, especias y salsas')).toBeInTheDocument();
    expect(screen.getByText('Aceite, vinagre y sal')).toBeInTheDocument();
    // The identity these rows are keyed on, and what makes a re-sync explicable
    // when a name changes underneath.
    expect(screen.getByText('112')).toBeInTheDocument();
  });

  /**
   * These rows are a copy of somebody else's words. An edit here would be
   * changing what Mercadona calls its own shelf, and the next sync would undo it.
   */
  it('offers no way to edit or delete an aisle', async () => {
    renderManager();
    await screen.findByText('Aceite, vinagre y sal');

    expect(screen.queryByRole('button', { name: /editar/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /eliminar/i })).not.toBeInTheDocument();
  });

  /** The table starts empty: a shop's aisles arrive when the shop is asked. */
  it('says when the shop has not been asked yet', async () => {
    listMock.mockResolvedValue([]);

    renderManager();

    expect(await screen.findByText(/Todavía no se le han pedido/)).toBeInTheDocument();
  });

  it('asks the shop again and reports how many came back', async () => {
    syncMock.mockResolvedValue(aisles);
    const user = userEvent.setup();

    renderManager();
    await screen.findByText('Aceite, vinagre y sal');

    await user.click(screen.getByRole('button', { name: 'Sincronizar' }));

    await waitFor(() => expect(syncMock).toHaveBeenCalledWith('MERCADONA'));
    expect(await screen.findByText(/Mercadona: 2 pasillos/)).toBeInTheDocument();
  });

  /**
   * A chain with no catalogue behind it answers 404, and that is a real answer
   * rather than a failure: OTRAS is where things bought at a market stall go.
   */
  it('surfaces a chain that has no catalogue to ask', async () => {
    const { ApiRequestError } = await import('../../api/client');
    listMock.mockResolvedValue([]);
    syncMock.mockRejectedValue(new ApiRequestError(404, 'No hay catálogo disponible para: OTRAS'));
    const user = userEvent.setup();

    renderManager('OTRAS', 'Otras');
    await screen.findByText(/Todavía no se le han pedido/);

    await user.click(screen.getByRole('button', { name: 'Sincronizar' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(/No hay catálogo disponible/);
  });
});
