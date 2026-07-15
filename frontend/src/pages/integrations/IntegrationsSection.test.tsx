import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { IntegrationsSection } from './IntegrationsSection';
import { ApiRequestError } from '../../api/client';
import {
  connectIntegration,
  disconnectIntegration,
  listIntegrations,
  syncIntegration,
  type IntegrationConnection,
} from '../../api/integrations';
import { axe } from '../../test/axe';

function renderSection() {
  return render(<IntegrationsSection />);
}

vi.mock('../../api/integrations', () => ({
  listIntegrations: vi.fn(),
  connectIntegration: vi.fn(),
  disconnectIntegration: vi.fn(),
  syncIntegration: vi.fn(),
}));

const listMock = vi.mocked(listIntegrations);
const connectMock = vi.mocked(connectIntegration);
const disconnectMock = vi.mocked(disconnectIntegration);
const syncMock = vi.mocked(syncIntegration);

const withings: IntegrationConnection = {
  providerId: 'WITHINGS',
  providerName: 'Withings',
  description: 'Sincroniza automáticamente tus datos de salud y composición corporal.',
  status: 'CONNECTED',
  lastSyncAt: '2026-07-10T08:15:00Z',
};

const googleFit: IntegrationConnection = {
  providerId: 'GOOGLE_FIT',
  providerName: 'Google Fit',
  description: 'Sincroniza tu actividad y entrenamientos.',
  status: 'NOT_CONNECTED',
};

const appleHealth: IntegrationConnection = {
  providerId: 'APPLE_HEALTH',
  providerName: 'Apple Health',
  description: 'Sincroniza tus datos de salud de Apple.',
  status: 'NOT_CONNECTED',
};

describe('IntegrationsSection', () => {
  beforeEach(() => {
    listMock.mockReset();
    connectMock.mockReset();
    disconnectMock.mockReset();
    syncMock.mockReset();
  });

  it('renders the connected provider with status pill and last-sync timestamp', async () => {
    listMock.mockResolvedValue([withings, googleFit, appleHealth]);

    renderSection();

    expect(await screen.findByText('Withings')).toBeInTheDocument();
    const connectedCard = screen.getByText('Withings').closest('li') as HTMLElement;
    expect(connectedCard).toHaveTextContent('Conectado');
    expect(connectedCard).toHaveTextContent('Última sincronización');
  });

  it('renders available providers with a connect action', async () => {
    listMock.mockResolvedValue([withings, googleFit, appleHealth]);

    renderSection();

    expect(await screen.findByText('Google Fit')).toBeInTheDocument();
    expect(screen.getByText('Apple Health')).toBeInTheDocument();
    expect(screen.getAllByRole('button', { name: 'Conectar' })).toHaveLength(2);
    expect(screen.getAllByText('No conectado')).toHaveLength(2);
  });

  it('shows connect, disconnect and manual-sync entry points where supported', async () => {
    listMock.mockResolvedValue([withings, googleFit, appleHealth]);

    renderSection();
    await screen.findByText('Withings');

    // Connected provider: sync + disconnect entry points.
    expect(screen.getByRole('button', { name: 'Sincronizar ahora' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Desconectar' })).toBeInTheDocument();
    // Available providers: connect entry point.
    expect(screen.getAllByRole('button', { name: 'Conectar' })).toHaveLength(2);
  });

  it('shows a sync error clearly with no token/PII when manual sync fails', async () => {
    listMock.mockResolvedValue([withings]);
    syncMock.mockRejectedValue(
      new ApiRequestError(
        501,
        'No se pudo sincronizar Withings: la integración con proveedores externos todavía no está disponible.',
        'NOT_IMPLEMENTED',
      ),
    );
    const user = userEvent.setup();

    renderSection();
    await user.click(await screen.findByRole('button', { name: 'Sincronizar ahora' }));

    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('No se pudo sincronizar Withings');
    expect(alert.textContent).not.toMatch(/token|secret|password|bearer/i);
  });

  it('shows a connect error clearly without sensitive data', async () => {
    listMock.mockResolvedValue([googleFit]);
    connectMock.mockRejectedValue(
      new ApiRequestError(
        501,
        'No se pudo conectar con Google Fit: la integración con proveedores externos todavía no está disponible.',
        'NOT_IMPLEMENTED',
      ),
    );
    const user = userEvent.setup();

    renderSection();
    await user.click(await screen.findByRole('button', { name: 'Conectar' }));

    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('No se pudo conectar con Google Fit');
    expect(alert.textContent).not.toMatch(/token|secret|password|bearer/i);
  });

  it('asks for explicit confirmation before disconnecting, and shows the resulting error', async () => {
    listMock.mockResolvedValue([withings]);
    disconnectMock.mockRejectedValue(
      new ApiRequestError(
        501,
        'No se pudo desconectar Withings: la integración con proveedores externos todavía no está disponible.',
        'NOT_IMPLEMENTED',
      ),
    );
    const user = userEvent.setup();

    renderSection();
    await user.click(await screen.findByRole('button', { name: 'Desconectar' }));

    // Explicit confirmation modal (FOR-63 destructive pattern, reusing Modal).
    const modal = await screen.findByRole('dialog');
    expect(
      within(modal).getByRole('heading', { name: 'Desconectar Withings' }),
    ).toBeInTheDocument();
    expect(disconnectMock).not.toHaveBeenCalled();

    await user.click(within(modal).getByRole('button', { name: 'Desconectar' }));

    await waitFor(() => expect(disconnectMock).toHaveBeenCalledWith('WITHINGS'));
    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo desconectar Withings');
  });

  it('cancels disconnect without calling the API when the user backs out', async () => {
    listMock.mockResolvedValue([withings]);
    const user = userEvent.setup();

    renderSection();
    await user.click(await screen.findByRole('button', { name: 'Desconectar' }));
    const modal = await screen.findByRole('dialog');
    within(modal).getByRole('heading', { name: 'Desconectar Withings' });

    await user.click(within(modal).getByRole('button', { name: 'Cancelar' }));

    expect(screen.queryByRole('heading', { name: 'Desconectar Withings' })).not.toBeInTheDocument();
    expect(disconnectMock).not.toHaveBeenCalled();
  });

  it('renders a clean empty connected state alongside the available list', async () => {
    listMock.mockResolvedValue([googleFit, appleHealth]);

    renderSection();

    expect(await screen.findByText('Aún no tienes integraciones conectadas.')).toBeInTheDocument();
    expect(screen.getByText('Google Fit')).toBeInTheDocument();
    expect(screen.getByText('Apple Health')).toBeInTheDocument();
  });

  it('shows a loading state while providers load', () => {
    listMock.mockReturnValue(new Promise(() => {}));

    renderSection();

    expect(screen.getByRole('status')).toHaveTextContent('Cargando tus integraciones');
  });

  it('shows an error state with retry when the provider list fails to load', async () => {
    listMock.mockRejectedValueOnce(new Error('network'));
    listMock.mockResolvedValueOnce([withings, googleFit, appleHealth]);
    const user = userEvent.setup();

    renderSection();

    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudieron cargar');

    await user.click(screen.getByRole('button', { name: 'Reintentar' }));

    expect(await screen.findByText('Withings')).toBeInTheDocument();
    expect(listMock).toHaveBeenCalledTimes(2);
  });

  it('has no accessibility violations in the primary connected/available state (FOR-114)', async () => {
    listMock.mockResolvedValue([withings, googleFit, appleHealth]);

    const { container } = renderSection();
    await screen.findByText('Withings');

    expect(await axe(container)).toHaveNoViolations();
  });

  it('has no accessibility violations with the destructive disconnect confirmation dialog open (FOR-114)', async () => {
    listMock.mockResolvedValue([withings]);
    const user = userEvent.setup();

    const { container } = renderSection();
    await user.click(await screen.findByRole('button', { name: 'Desconectar' }));
    const modal = await screen.findByRole('dialog');
    within(modal).getByRole('heading', { name: 'Desconectar Withings' });

    expect(await axe(container)).toHaveNoViolations();
  });

  // FOR-116: brand-logo replacement was researched and found not safely
  // embeddable for any of the three providers today (no verified partnership
  // or functioning technical integration with Google, Apple or Withings — see
  // IntegrationsSection.tsx doc comment on PROVIDER_ICON_FALLBACK). The
  // documented generic-icon fallback therefore remains the shipped behavior;
  // these tests lock in the two invariants the story still requires: the
  // fallback icon is consistent across both render sites (ai-context.md
  // Common Pitfalls: "updating only one of the two render sites"), and the
  // accessible name never depends on the (decorative) icon.
  it('renders the same documented fallback icon per provider in both the connected and available lists (FOR-116)', async () => {
    const allConnected: IntegrationConnection[] = [
      { ...withings, status: 'CONNECTED', lastSyncAt: '2026-07-10T08:15:00Z' },
      { ...googleFit, status: 'CONNECTED', lastSyncAt: '2026-07-10T08:15:00Z' },
      { ...appleHealth, status: 'CONNECTED', lastSyncAt: '2026-07-10T08:15:00Z' },
    ];
    listMock.mockResolvedValueOnce(allConnected);
    const connectedRender = render(<IntegrationsSection />);
    await within(connectedRender.container).findByText('Withings');

    const allAvailable: IntegrationConnection[] = [
      { ...withings, status: 'NOT_CONNECTED', lastSyncAt: undefined },
      googleFit,
      appleHealth,
    ];
    listMock.mockResolvedValueOnce(allAvailable);
    const availableRender = render(<IntegrationsSection />);
    await within(availableRender.container).findByText('Withings');

    const expectedIcon: Record<string, string> = {
      Withings: 'heart',
      'Google Fit': 'activity',
      'Apple Health': 'cross',
    };

    for (const [providerName, iconName] of Object.entries(expectedIcon)) {
      const connectedIcon = within(connectedRender.container)
        .getByText(providerName)
        .closest('li')
        ?.querySelector('svg');
      const availableIcon = within(availableRender.container)
        .getByText(providerName)
        .closest('li')
        ?.querySelector('svg');

      expect(connectedIcon).toHaveAttribute('data-icon', iconName);
      expect(availableIcon).toHaveAttribute('data-icon', iconName);
    }
  });

  it("keeps each provider row's accessible name in visible text, independent of the decorative icon (FOR-116)", async () => {
    listMock.mockResolvedValue([withings, googleFit, appleHealth]);

    renderSection();
    await screen.findByText('Withings');

    for (const name of ['Withings', 'Google Fit', 'Apple Health']) {
      const row = screen.getByText(name).closest('li') as HTMLElement;
      const icon = row.querySelector('svg');
      expect(icon).toHaveAttribute('aria-hidden', 'true');
      expect(within(row).getByText(name)).toBeInTheDocument();
    }
  });
});
