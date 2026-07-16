import { StrictMode } from 'react';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { AuthCallbackPage } from './AuthCallbackPage';
import { NotificationProvider } from '../components/NotificationProvider';
import { ApiRequestError } from '../api/client';
import { completeIntegrationCallback } from '../api/integrations';
import { axe } from '../test/axe';

/**
 * FOR-133: the SPA route Withings redirects the browser back to
 * (`https://forma.diegobarrioh.dev/auth?code=...&state=...`, registered in
 * FOR-131). Completes the OAuth round trip by relaying `code`/`state` to the
 * backend callback, then lands the user back on Integraciones.
 */
vi.mock('../api/integrations', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../api/integrations')>();
  return {
    ...actual,
    completeIntegrationCallback: vi.fn(),
    connectIntegration: vi.fn(),
  };
});

const callbackMock = vi.mocked(completeIntegrationCallback);

const INTEGRATIONS_STUB_HEADING = 'Integraciones (stub)';

function renderAt(path: string, options?: { strict?: boolean }) {
  const tree = (
    <MemoryRouter initialEntries={[path]}>
      <NotificationProvider>
        <Routes>
          <Route path="/auth" element={<AuthCallbackPage />} />
          <Route path="/ajustes/integraciones" element={<h1>{INTEGRATIONS_STUB_HEADING}</h1>} />
        </Routes>
      </NotificationProvider>
    </MemoryRouter>
  );
  return render(options?.strict ? <StrictMode>{tree}</StrictMode> : tree);
}

describe('AuthCallbackPage (FOR-133)', () => {
  beforeEach(() => {
    callbackMock.mockReset();
  });

  it('has a page-level heading', async () => {
    callbackMock.mockReturnValue(new Promise(() => {}));
    renderAt('/auth?code=abc&state=xyz');
    expect(screen.getByRole('heading')).toBeInTheDocument();
  });

  it('calls the backend callback with code/state from the query and shows a loading state while pending', async () => {
    callbackMock.mockReturnValue(new Promise(() => {}));

    renderAt('/auth?code=auth-code&state=state-value');

    expect(screen.getByRole('status')).toHaveTextContent('Completando conexión con Withings');
    await waitFor(() =>
      expect(callbackMock).toHaveBeenCalledWith('WITHINGS', 'auth-code', 'state-value'),
    );
  });

  it('on success, navigates to Integraciones and fires a success toast', async () => {
    callbackMock.mockResolvedValue({
      provider: 'WITHINGS',
      status: 'CONNECTED',
      connectedAt: '2026-07-16T15:00:00Z',
    });

    renderAt('/auth?code=auth-code&state=state-value');

    expect(
      await screen.findByRole('heading', { name: INTEGRATIONS_STUB_HEADING }),
    ).toBeInTheDocument();
    const region = await screen.findByRole('log');
    expect(region).toHaveTextContent('Conectado con Withings.');
  });

  it('shows an error state with no backend call when code is missing', async () => {
    renderAt('/auth?state=state-value');

    expect(await screen.findByRole('alert')).toBeInTheDocument();
    expect(callbackMock).not.toHaveBeenCalled();
  });

  it('shows an error state with no backend call when state is blank', async () => {
    renderAt('/auth?code=auth-code&state=');

    expect(await screen.findByRole('alert')).toBeInTheDocument();
    expect(callbackMock).not.toHaveBeenCalled();
  });

  it('redirects to Integraciones with no backend call on a direct visit (no query at all)', async () => {
    renderAt('/auth');

    expect(
      await screen.findByRole('heading', { name: INTEGRATIONS_STUB_HEADING }),
    ).toBeInTheDocument();
    expect(callbackMock).not.toHaveBeenCalled();
  });

  it('shows a readable ErrorState with retry/back actions when the backend rejects (400 invalid/expired state)', async () => {
    callbackMock.mockRejectedValue(
      new ApiRequestError(
        400,
        'El enlace de conexión ha caducado o no es válido.',
        'VALIDATION_ERROR',
      ),
    );

    renderAt('/auth?code=auth-code&state=state-value');

    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('El enlace de conexión ha caducado o no es válido.');
    expect(screen.getByRole('button', { name: /volver a intentar/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /volver a integraciones/i })).toBeInTheDocument();
  });

  it('never shows a raw/technical error for a non-ApiRequestError failure', async () => {
    callbackMock.mockRejectedValue(new Error('network'));

    renderAt('/auth?code=auth-code&state=state-value');

    const alert = await screen.findByRole('alert');
    expect(alert.textContent).not.toMatch(/network/i);
  });

  it('calls the callback exactly once even under React StrictMode double-invocation', async () => {
    callbackMock.mockResolvedValue({
      provider: 'WITHINGS',
      status: 'CONNECTED',
      connectedAt: '2026-07-16T15:00:00Z',
    });

    renderAt('/auth?code=auth-code&state=state-value', { strict: true });

    await waitFor(() => expect(callbackMock).toHaveBeenCalledTimes(1));
    await screen.findByRole('heading', { name: INTEGRATIONS_STUB_HEADING });
    expect(callbackMock).toHaveBeenCalledTimes(1);
  });

  it('never persists code/state to localStorage', async () => {
    const setItemSpy = vi.spyOn(Storage.prototype, 'setItem');
    callbackMock.mockResolvedValue({
      provider: 'WITHINGS',
      status: 'CONNECTED',
      connectedAt: '2026-07-16T15:00:00Z',
    });

    renderAt('/auth?code=auth-code&state=state-value');
    await screen.findByRole('heading', { name: INTEGRATIONS_STUB_HEADING });

    expect(setItemSpy).not.toHaveBeenCalledWith(
      expect.anything(),
      expect.stringContaining('auth-code'),
    );
    expect(setItemSpy).not.toHaveBeenCalledWith(
      expect.anything(),
      expect.stringContaining('state-value'),
    );
    setItemSpy.mockRestore();
  });

  it('has no accessibility violations while loading', async () => {
    callbackMock.mockReturnValue(new Promise(() => {}));
    const { container } = renderAt('/auth?code=auth-code&state=state-value');

    expect(screen.getByRole('status')).toBeInTheDocument();
    expect(await axe(container)).toHaveNoViolations();
  });

  it('has no accessibility violations in the error state', async () => {
    callbackMock.mockRejectedValue(
      new ApiRequestError(
        400,
        'El enlace de conexión ha caducado o no es válido.',
        'VALIDATION_ERROR',
      ),
    );
    const { container } = renderAt('/auth?code=auth-code&state=state-value');
    await screen.findByRole('alert');

    expect(await axe(container)).toHaveNoViolations();
  });
});
