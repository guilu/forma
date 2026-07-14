import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ConfirmDialog } from './ConfirmDialog';

/**
 * ConfirmDialog tests (FOR-63): the shared destructive-action confirmation
 * pattern built on top of {@link Modal} (spec `specs/FOR-63/spec.md`:
 * "Destructive-action confirmation: explicit confirm for delete/disconnect
 * (reuse Modal.tsx)"; `tests.md`: "requires explicit confirmation; cancel
 * has no effect").
 */
describe('ConfirmDialog', () => {
  it('renders the title and message as an accessible dialog', () => {
    render(
      <ConfirmDialog
        title="Desconectar Withings"
        message="¿Seguro que quieres desconectar Withings?"
        onConfirm={vi.fn()}
        onCancel={vi.fn()}
      />,
    );

    expect(screen.getByRole('dialog', { name: 'Desconectar Withings' })).toBeInTheDocument();
    expect(screen.getByText('¿Seguro que quieres desconectar Withings?')).toBeInTheDocument();
  });

  it('calls onConfirm when the confirm action is clicked', async () => {
    const onConfirm = vi.fn();
    const user = userEvent.setup();
    render(
      <ConfirmDialog
        title="Eliminar"
        message="¿Eliminar este elemento?"
        confirmLabel="Eliminar"
        onConfirm={onConfirm}
        onCancel={vi.fn()}
      />,
    );

    await user.click(screen.getByRole('button', { name: 'Eliminar' }));

    expect(onConfirm).toHaveBeenCalledTimes(1);
  });

  it('cancel has no side effect: it calls onCancel and never onConfirm', async () => {
    const onConfirm = vi.fn();
    const onCancel = vi.fn();
    const user = userEvent.setup();
    render(
      <ConfirmDialog
        title="Eliminar"
        message="¿Eliminar este elemento?"
        onConfirm={onConfirm}
        onCancel={onCancel}
      />,
    );

    await user.click(screen.getByRole('button', { name: 'Cancelar' }));

    expect(onCancel).toHaveBeenCalledTimes(1);
    expect(onConfirm).not.toHaveBeenCalled();
  });

  it('Escape also cancels with no side effect (inherited from Modal, FOR-61)', async () => {
    const onConfirm = vi.fn();
    const onCancel = vi.fn();
    const user = userEvent.setup();
    render(
      <ConfirmDialog
        title="Eliminar"
        message="¿Eliminar este elemento?"
        onConfirm={onConfirm}
        onCancel={onCancel}
      />,
    );

    await user.keyboard('{Escape}');

    expect(onCancel).toHaveBeenCalledTimes(1);
    expect(onConfirm).not.toHaveBeenCalled();
  });

  it('shows a pending confirm action and keeps it usable to disable double-submits', () => {
    render(
      <ConfirmDialog
        title="Eliminar"
        message="¿Eliminar este elemento?"
        confirmLabel="Eliminar"
        pending
        onConfirm={vi.fn()}
        onCancel={vi.fn()}
      />,
    );

    expect(screen.getByRole('button', { name: 'Eliminar' })).toBeDisabled();
  });
});
