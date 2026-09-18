import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import userEvent from '@testing-library/user-event';
import { CompletionStep } from './CompletionStep';

describe('CompletionStep', () => {
  it('shows a calm "todo listo" message with a single next action on first completion', () => {
    render(
      <CompletionStep alreadyCompleted={false} onGoToDashboard={vi.fn()} onRestart={vi.fn()} />,
    );

    expect(screen.getByRole('heading', { name: 'Todo listo' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Ir al panel' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Volver a empezar' })).not.toBeInTheDocument();
  });

  it('shows the already-completed gate with a restart option on a return visit', () => {
    render(<CompletionStep alreadyCompleted onGoToDashboard={vi.fn()} onRestart={vi.fn()} />);

    expect(
      screen.getByRole('heading', { name: 'Ya completaste la configuración inicial' }),
    ).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Volver a empezar' })).toBeInTheDocument();
  });

  it('invokes the provided callbacks', async () => {
    const user = userEvent.setup();
    const onGoToDashboard = vi.fn();
    const onRestart = vi.fn();
    render(
      <CompletionStep alreadyCompleted onGoToDashboard={onGoToDashboard} onRestart={onRestart} />,
    );

    await user.click(screen.getByRole('button', { name: 'Ir al panel' }));
    await user.click(screen.getByRole('button', { name: 'Volver a empezar' }));

    expect(onGoToDashboard).toHaveBeenCalledTimes(1);
    expect(onRestart).toHaveBeenCalledTimes(1);
  });

  it('renders no plan-request notice by default', () => {
    render(
      <CompletionStep alreadyCompleted={false} onGoToDashboard={vi.fn()} onRestart={vi.fn()} />,
    );

    expect(screen.queryByRole('status')).not.toBeInTheDocument();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('shows an informational plan-request notice with role=status when tone is status', () => {
    render(
      <CompletionStep
        alreadyCompleted={false}
        onGoToDashboard={vi.fn()}
        onRestart={vi.fn()}
        planRequestNotice={{ message: 'Hemos enviado tu petición de plan.', tone: 'status' }}
      />,
    );

    expect(screen.getByRole('status')).toHaveTextContent('Hemos enviado tu petición de plan.');
  });

  it('shows an actionable plan-request notice with role=alert and a step-jump action button when tone is alert', async () => {
    const user = userEvent.setup();
    const onAction = vi.fn();
    render(
      <CompletionStep
        alreadyCompleted={false}
        onGoToDashboard={vi.fn()}
        onRestart={vi.fn()}
        planRequestNotice={{
          message: 'No elegiste una dirección para tu plan.',
          tone: 'alert',
          action: { kind: 'step', label: 'Elegir dirección', onClick: onAction },
        }}
      />,
    );

    expect(screen.getByRole('alert')).toHaveTextContent('No elegiste una dirección para tu plan.');
    await user.click(screen.getByRole('button', { name: 'Elegir dirección' }));
    expect(onAction).toHaveBeenCalledTimes(1);
  });

  it('renders a link-action as a real navigation (ButtonLink), not an onClick button', () => {
    render(
      <MemoryRouter>
        <CompletionStep
          alreadyCompleted={false}
          onGoToDashboard={vi.fn()}
          onRestart={vi.fn()}
          planRequestNotice={{
            message: 'Completa tu altura en tu perfil antes de pedir un plan.',
            tone: 'alert',
            action: { kind: 'link', label: 'Ir a Ajustes', to: '/app/settings' },
          }}
        />
      </MemoryRouter>,
    );

    const link = screen.getByRole('link', { name: 'Ir a Ajustes' });
    expect(link).toHaveAttribute('href', '/app/settings');
  });
});
