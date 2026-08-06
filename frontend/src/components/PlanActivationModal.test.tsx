import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { PlanActivationModal } from './PlanActivationModal';

describe('PlanActivationModal', () => {
  it('names the plan being offered', () => {
    render(
      <PlanActivationModal
        planName="Dieta semanal — recomposición"
        onAccept={vi.fn()}
        onDismiss={vi.fn()}
      />,
    );

    expect(screen.getByText(/Dieta semanal — recomposición/)).toBeInTheDocument();
  });

  it('activates the plan when accepted', async () => {
    const onAccept = vi.fn();
    const user = userEvent.setup();
    render(<PlanActivationModal planName="Plan" onAccept={onAccept} onDismiss={vi.fn()} />);

    await user.click(screen.getByRole('button', { name: 'Sí, activa mi plan' }));

    expect(onAccept).toHaveBeenCalledOnce();
  });

  /** Saying "later" must never start the plan — that is the whole point of asking. */
  it('starts nothing when dismissed', async () => {
    const onAccept = vi.fn();
    const onDismiss = vi.fn();
    const user = userEvent.setup();
    render(<PlanActivationModal planName="Plan" onAccept={onAccept} onDismiss={onDismiss} />);

    await user.click(screen.getByRole('button', { name: 'No, lo haré en otro momento' }));

    expect(onDismiss).toHaveBeenCalledOnce();
    expect(onAccept).not.toHaveBeenCalled();
  });

  it('blocks both actions while the request is in flight', () => {
    render(<PlanActivationModal planName="Plan" pending onAccept={vi.fn()} onDismiss={vi.fn()} />);

    expect(screen.getByRole('button', { name: 'Sí, activa mi plan' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'No, lo haré en otro momento' })).toBeDisabled();
  });

  it('shows the error without closing, so the answer is not lost', () => {
    render(
      <PlanActivationModal
        planName="Plan"
        error="No se pudo activar tu plan."
        onAccept={vi.fn()}
        onDismiss={vi.fn()}
      />,
    );

    expect(screen.getByRole('alert')).toHaveTextContent('No se pudo activar tu plan.');
    expect(screen.getByRole('button', { name: 'Sí, activa mi plan' })).toBeEnabled();
  });
});
