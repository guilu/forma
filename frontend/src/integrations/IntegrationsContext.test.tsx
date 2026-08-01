import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { IntegrationsProvider } from './IntegrationsContext';
import { Sidebar } from '../layout/Sidebar';
import { IntegrationsSection } from '../pages/integrations/IntegrationsSection';
import { NotificationProvider } from '../components/NotificationProvider';
import {
  disconnectIntegration,
  listIntegrations,
  type IntegrationConnection,
} from '../api/integrations';

vi.mock('../api/integrations', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../api/integrations')>();
  return {
    ...actual,
    listIntegrations: vi.fn(),
    connectIntegration: vi.fn(),
    disconnectIntegration: vi.fn(),
    syncIntegration: vi.fn(),
  };
});

const listMock = vi.mocked(listIntegrations);
const disconnectMock = vi.mocked(disconnectIntegration);

const withings: IntegrationConnection = {
  providerId: 'WITHINGS',
  providerName: 'Withings',
  description: 'Sincroniza automáticamente tus datos.',
  status: 'CONNECTED',
  lastSyncAt: '2026-07-30T10:01:00Z',
};

/**
 * The sidebar's status card and the settings section are two views of one
 * thing, rendered together exactly as the app shell renders them.
 */
function renderBoth() {
  return render(
    <MemoryRouter>
      <NotificationProvider>
        <IntegrationsProvider>
          <Sidebar />
          <IntegrationsSection />
        </IntegrationsProvider>
      </NotificationProvider>
    </MemoryRouter>,
  );
}

/** The sidebar's own card — "Conectado" also appears on the section's pill. */
const sidebar = () => within(screen.getByRole('complementary'));

describe('shared integration state', () => {
  beforeEach(() => {
    listMock.mockReset();
    disconnectMock.mockReset();
  });

  /**
   * The reported bug: disconnecting in settings left the sidebar claiming the
   * provider was still connected until a full page reload, because each view
   * fetched its own copy of the list once on mount and never re-read it.
   */
  it('updates the sidebar when the section disconnects a provider', async () => {
    listMock
      .mockResolvedValueOnce([withings])
      .mockResolvedValue([{ ...withings, status: 'NOT_CONNECTED', lastSyncAt: undefined }]);
    disconnectMock.mockResolvedValue({
      provider: 'WITHINGS',
      status: 'DISCONNECTED',
      connectedAt: null,
    });
    const user = userEvent.setup();

    renderBoth();

    await waitFor(() => expect(sidebar().getByText('Conectado')).toBeInTheDocument());

    await user.click(screen.getByRole('button', { name: 'Desconectar' }));
    const dialog = await screen.findByRole('dialog');
    await user.click(within(dialog).getByRole('button', { name: 'Desconectar' }));

    await waitFor(() => expect(disconnectMock).toHaveBeenCalledWith('WITHINGS'));
    await waitFor(() => expect(sidebar().getByText('No conectado')).toBeInTheDocument());
    expect(sidebar().queryByText('Conectado')).not.toBeInTheDocument();
  });

  it('reads the list once for both views rather than once each', async () => {
    listMock.mockResolvedValue([withings]);

    renderBoth();

    await waitFor(() => expect(sidebar().getByText('Conectado')).toBeInTheDocument());
    expect(listMock).toHaveBeenCalledTimes(1);
  });
});
