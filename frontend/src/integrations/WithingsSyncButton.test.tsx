import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { IntegrationsProvider } from './IntegrationsContext';
import { WithingsSyncButton } from './WithingsSyncButton';
import { NotificationProvider } from '../components/NotificationProvider';
import { ApiRequestError } from '../api/client';
import { listIntegrations, syncIntegration, type IntegrationConnection } from '../api/integrations';

vi.mock('../api/integrations', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../api/integrations')>();
  return {
    ...actual,
    listIntegrations: vi.fn(),
    syncIntegration: vi.fn(),
  };
});

const listMock = vi.mocked(listIntegrations);
const syncMock = vi.mocked(syncIntegration);

const CONNECTED: IntegrationConnection = {
  providerId: 'WITHINGS',
  providerName: 'Withings',
  description: 'Sincroniza automáticamente tus datos.',
  status: 'CONNECTED',
  lastSyncAt: '2026-08-14T20:28:00Z',
};

const DISCONNECTED: IntegrationConnection = { ...CONNECTED, status: 'NOT_CONNECTED' };

function renderButton() {
  return render(
    <NotificationProvider>
      <IntegrationsProvider>
        <WithingsSyncButton />
      </IntegrationsProvider>
    </NotificationProvider>,
  );
}

function syncButton() {
  return screen.getByRole('button', { name: 'Sincronizar Withings' });
}

beforeEach(() => {
  vi.clearAllMocks();
});

describe('WithingsSyncButton', () => {
  it('renders nothing while the connection list is still loading', () => {
    listMock.mockReturnValue(new Promise(() => {}));

    renderButton();

    expect(screen.queryByRole('button')).not.toBeInTheDocument();
  });

  it('renders nothing when Withings is not connected', async () => {
    listMock.mockResolvedValue([DISCONNECTED]);

    renderButton();

    await waitFor(() => expect(listMock).toHaveBeenCalled());
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
  });

  // The tooltip is the only text the control carries: it is icon-only, so the
  // hover hint and the accessible name must both say what it does.
  it('renders a tooltipped icon button when Withings is connected', async () => {
    listMock.mockResolvedValue([CONNECTED]);

    renderButton();

    const button = await screen.findByRole('button', { name: 'Sincronizar Withings' });
    expect(button).toHaveAttribute('title', 'Sincronizar Withings');
  });

  it('syncs and re-reads the connection list on click', async () => {
    listMock.mockResolvedValue([CONNECTED]);
    syncMock.mockResolvedValue({
      result: 'OK',
      importedCount: 2,
      lastSyncAt: '2026-08-16T09:00:00Z',
      message: null,
    });

    renderButton();
    await screen.findByRole('button', { name: 'Sincronizar Withings' });
    await userEvent.click(syncButton());

    await waitFor(() => expect(syncMock).toHaveBeenCalledWith('WITHINGS'));
    expect(await screen.findByText('Sincronizado con Withings.')).toBeInTheDocument();
    // Once on mount, once after the sync: `lastSyncAt` changed server-side.
    await waitFor(() => expect(listMock).toHaveBeenCalledTimes(2));
  });

  // A resolved sync is not a successful sync (FOR-123): never fabricate a
  // success toast for a call that imported nothing because the provider is gone.
  it('reports a NOT_CONNECTED outcome as an error, not a success', async () => {
    listMock.mockResolvedValue([CONNECTED]);
    syncMock.mockResolvedValue({
      result: 'NOT_CONNECTED',
      importedCount: 0,
      lastSyncAt: null,
      message: null,
    });

    renderButton();
    await screen.findByRole('button', { name: 'Sincronizar Withings' });
    await userEvent.click(syncButton());

    expect(
      await screen.findByText('No se pudo sincronizar Withings: ya no está conectado.'),
    ).toBeInTheDocument();
    expect(screen.queryByText('Sincronizado con Withings.')).not.toBeInTheDocument();
  });

  it('surfaces the API message when the sync fails', async () => {
    listMock.mockResolvedValue([CONNECTED]);
    syncMock.mockRejectedValue(new ApiRequestError(502, 'Withings no responde.'));

    renderButton();
    await screen.findByRole('button', { name: 'Sincronizar Withings' });
    await userEvent.click(syncButton());

    expect(await screen.findByText('Withings no responde.')).toBeInTheDocument();
  });

  it('blocks a second click while a sync is in flight', async () => {
    listMock.mockResolvedValue([CONNECTED]);
    syncMock.mockReturnValue(new Promise(() => {}));

    renderButton();
    await screen.findByRole('button', { name: 'Sincronizar Withings' });
    await userEvent.click(syncButton());

    await waitFor(() => expect(syncButton()).toBeDisabled());
    expect(syncButton()).toHaveAttribute('aria-busy', 'true');
    expect(syncMock).toHaveBeenCalledTimes(1);
  });
});
