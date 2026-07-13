import { describe, expect, it, vi, afterEach } from 'vitest';
import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { NotificationProvider, useNotify } from './NotificationProvider';

/**
 * NotificationProvider / useNotify tests (FOR-63): the shared success/
 * warning/error feedback pattern — dedupe/limit, dismissible, aria-live
 * announced, and auto-dismiss for success only (spec `specs/FOR-63/tests.md`
 * UI Tests + Edge Cases; fixture pattern: "a component wired to the
 * notification hook").
 */
function NotifyConsumer() {
  const notify = useNotify();
  return (
    <div>
      <button type="button" onClick={() => notify.success('Medición guardada.')}>
        success
      </button>
      <button type="button" onClick={() => notify.warning('Revisa tus datos.')}>
        warning
      </button>
      <button type="button" onClick={() => notify.error('No se pudo guardar.')}>
        error
      </button>
    </div>
  );
}

describe('NotificationProvider / useNotify (FOR-63)', () => {
  afterEach(() => {
    vi.useRealTimers();
  });

  it('renders an aria-live region with no notifications initially', () => {
    render(
      <NotificationProvider>
        <p>contenido</p>
      </NotificationProvider>,
    );

    expect(screen.getByText('contenido')).toBeInTheDocument();
    const region = screen.getByRole('log');
    expect(region).toHaveAttribute('aria-live', 'polite');
    expect(region).toBeEmptyDOMElement();
  });

  it('shows a success toast announced via the live region', async () => {
    const user = userEvent.setup();
    render(
      <NotificationProvider>
        <NotifyConsumer />
      </NotificationProvider>,
    );

    await user.click(screen.getByRole('button', { name: 'success' }));

    const region = screen.getByRole('log');
    expect(within(region).getByText(/Medición guardada\./)).toBeInTheDocument();
  });

  it('shows a warning toast and an error toast, distinguishable by text (not color alone)', async () => {
    const user = userEvent.setup();
    render(
      <NotificationProvider>
        <NotifyConsumer />
      </NotificationProvider>,
    );

    await user.click(screen.getByRole('button', { name: 'warning' }));
    await user.click(screen.getByRole('button', { name: 'error' }));

    expect(screen.getByText(/Atención/)).toBeInTheDocument();
    expect(screen.getByText(/Revisa tus datos\./)).toBeInTheDocument();
    expect(screen.getByText(/Error/)).toBeInTheDocument();
    expect(screen.getByText(/No se pudo guardar\./)).toBeInTheDocument();
  });

  it('a toast is dismissible via its own button', async () => {
    const user = userEvent.setup();
    render(
      <NotificationProvider>
        <NotifyConsumer />
      </NotificationProvider>,
    );

    await user.click(screen.getByRole('button', { name: 'error' }));
    expect(screen.getByText(/No se pudo guardar\./)).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /Descartar notificación/ }));

    expect(screen.queryByText(/No se pudo guardar\./)).not.toBeInTheDocument();
  });

  it('auto-dismisses a success toast after a timeout', () => {
    vi.useFakeTimers();
    render(
      <NotificationProvider>
        <NotifyConsumer />
      </NotificationProvider>,
    );

    act(() => {
      fireEvent.click(screen.getByRole('button', { name: 'success' }));
    });
    expect(screen.getByText(/Medición guardada\./)).toBeInTheDocument();

    act(() => {
      vi.advanceTimersByTime(10_000);
    });

    expect(screen.queryByText(/Medición guardada\./)).not.toBeInTheDocument();
  });

  it('a warning/error toast persists — it does not auto-dismiss (edge case: no stuck pending, but errors persist)', () => {
    vi.useFakeTimers();
    render(
      <NotificationProvider>
        <NotifyConsumer />
      </NotificationProvider>,
    );

    act(() => {
      fireEvent.click(screen.getByRole('button', { name: 'error' }));
    });
    act(() => {
      vi.advanceTimersByTime(20_000);
    });

    expect(screen.getByText(/No se pudo guardar\./)).toBeInTheDocument();
  });

  it('de-duplicates rapid repeated identical notifications (edge case)', async () => {
    const user = userEvent.setup();
    render(
      <NotificationProvider>
        <NotifyConsumer />
      </NotificationProvider>,
    );

    await user.click(screen.getByRole('button', { name: 'success' }));
    await user.click(screen.getByRole('button', { name: 'success' }));
    await user.click(screen.getByRole('button', { name: 'success' }));

    expect(screen.getAllByText(/Medición guardada\./)).toHaveLength(1);
  });

  it('limits the number of stacked toasts, dropping the oldest first (edge case)', async () => {
    function ManyNotifyConsumer() {
      const notify = useNotify();
      return (
        <button
          type="button"
          onClick={() => {
            notify.warning('Uno');
            notify.warning('Dos');
            notify.warning('Tres');
            notify.warning('Cuatro');
          }}
        >
          fire
        </button>
      );
    }

    const user = userEvent.setup();
    render(
      <NotificationProvider>
        <ManyNotifyConsumer />
      </NotificationProvider>,
    );

    await user.click(screen.getByRole('button', { name: 'fire' }));

    await waitFor(() => {
      expect(screen.queryByText('Uno')).not.toBeInTheDocument();
    });
    expect(screen.getByText('Dos')).toBeInTheDocument();
    expect(screen.getByText('Tres')).toBeInTheDocument();
    expect(screen.getByText('Cuatro')).toBeInTheDocument();
  });

  it('useNotify throws when used outside a NotificationProvider', () => {
    function BareConsumer() {
      useNotify();
      return null;
    }

    expect(() => render(<BareConsumer />)).toThrow(
      'useNotify must be used within a NotificationProvider',
    );
  });
});
