import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { PlanActivationGate } from './PlanActivationGate';
import * as api from '../api/planAcceptance';

vi.mock('../api/planAcceptance');

const getPlanAcceptance = vi.mocked(api.getPlanAcceptance);
const acceptPlan = vi.mocked(api.acceptPlan);

describe('PlanActivationGate', () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  it('offers the plan that is waiting to be started', async () => {
    getPlanAcceptance.mockResolvedValue({ pending: true, planName: 'Dieta semanal' });

    render(<PlanActivationGate onActivated={vi.fn()} />);

    expect(await screen.findByText(/Dieta semanal/)).toBeInTheDocument();
  });

  it('asks nothing when there is no plan waiting', async () => {
    getPlanAcceptance.mockResolvedValue({ pending: false });

    render(<PlanActivationGate onActivated={vi.fn()} />);

    await waitFor(() => expect(getPlanAcceptance).toHaveBeenCalled());
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  /** The screens must refresh once the plan is on, or they keep showing their empty state. */
  it('activates the plan and tells the shell to reload its screens', async () => {
    const onActivated = vi.fn();
    getPlanAcceptance.mockResolvedValue({ pending: true, planName: 'Dieta semanal' });
    acceptPlan.mockResolvedValue(undefined);
    const user = userEvent.setup();

    render(<PlanActivationGate onActivated={onActivated} />);
    await user.click(await screen.findByRole('button', { name: 'Sí, activa mi plan' }));

    await waitFor(() => expect(acceptPlan).toHaveBeenCalledOnce());
    expect(onActivated).toHaveBeenCalledOnce();
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  /** Declining persists nothing: the question is meant to come back next session. */
  it('closes without starting anything when declined', async () => {
    getPlanAcceptance.mockResolvedValue({ pending: true, planName: 'Dieta semanal' });
    const user = userEvent.setup();

    render(<PlanActivationGate onActivated={vi.fn()} />);
    await user.click(await screen.findByRole('button', { name: 'No, lo haré en otro momento' }));

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(acceptPlan).not.toHaveBeenCalled();
  });

  it('keeps the question open when activating fails', async () => {
    getPlanAcceptance.mockResolvedValue({ pending: true, planName: 'Dieta semanal' });
    acceptPlan.mockRejectedValue(new Error('boom'));
    const user = userEvent.setup();

    render(<PlanActivationGate onActivated={vi.fn()} />);
    await user.click(await screen.findByRole('button', { name: 'Sí, activa mi plan' }));

    expect(await screen.findByRole('alert')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Sí, activa mi plan' })).toBeInTheDocument();
  });

  /** A failing check must not block the app behind a modal it cannot dismiss. */
  it('asks nothing when the check itself fails', async () => {
    getPlanAcceptance.mockRejectedValue(new Error('offline'));

    render(<PlanActivationGate onActivated={vi.fn()} />);

    await waitFor(() => expect(getPlanAcceptance).toHaveBeenCalled());
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });
});
