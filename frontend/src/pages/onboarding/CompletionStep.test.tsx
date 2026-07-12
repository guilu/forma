import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
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
});
