import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { TypedConfirmDialog } from './TypedConfirmDialog';

/**
 * TypedConfirmDialog tests: the stronger, type-the-word confirmation used for deleting a
 * nutrition plan (PlansPage). Mirrors ConfirmDialog.test.tsx's coverage (accessible dialog,
 * cancel has no side effect, Escape cancels, pending disables) and adds the behaviour that is
 * new here: the confirm action starts disabled and only becomes clickable once the typed text
 * matches the required word, case-insensitively and trimmed.
 */
describe('TypedConfirmDialog', () => {
  it('renders the title and message as an accessible dialog', () => {
    render(
      <TypedConfirmDialog
        title="Eliminar Semana base"
        message="El plan y todo su historial desaparecen para siempre."
        confirmWord="eliminar"
        onConfirm={vi.fn()}
        onCancel={vi.fn()}
      />,
    );

    expect(screen.getByRole('dialog', { name: 'Eliminar Semana base' })).toBeInTheDocument();
    expect(
      screen.getByText('El plan y todo su historial desaparecen para siempre.'),
    ).toBeInTheDocument();
  });

  it('disables the confirm action until the typed word matches', async () => {
    const user = userEvent.setup();
    render(
      <TypedConfirmDialog
        title="Eliminar"
        message="¿Eliminar este plan?"
        confirmWord="eliminar"
        confirmLabel="Eliminar"
        onConfirm={vi.fn()}
        onCancel={vi.fn()}
      />,
    );

    const confirmButton = screen.getByRole('button', { name: 'Eliminar' });
    const input = screen.getByLabelText('Escribe "eliminar" para confirmar');
    expect(confirmButton).toBeDisabled();

    await user.type(input, 'borrar');
    expect(confirmButton).toBeDisabled();

    await user.clear(input);
    await user.type(input, 'eliminar');
    expect(confirmButton).toBeEnabled();
  });

  it('matches case-insensitively and trims surrounding whitespace', async () => {
    const user = userEvent.setup();
    render(
      <TypedConfirmDialog
        title="Eliminar"
        message="¿Eliminar este plan?"
        confirmWord="eliminar"
        confirmLabel="Eliminar"
        onConfirm={vi.fn()}
        onCancel={vi.fn()}
      />,
    );

    const input = screen.getByLabelText('Escribe "eliminar" para confirmar');
    await user.type(input, '  ELIMINAR  ');

    expect(screen.getByRole('button', { name: 'Eliminar' })).toBeEnabled();
  });

  it('calls onConfirm only once the word matches and the button is clicked', async () => {
    const onConfirm = vi.fn();
    const user = userEvent.setup();
    render(
      <TypedConfirmDialog
        title="Eliminar"
        message="¿Eliminar este plan?"
        confirmWord="eliminar"
        confirmLabel="Eliminar"
        onConfirm={onConfirm}
        onCancel={vi.fn()}
      />,
    );

    const input = screen.getByLabelText('Escribe "eliminar" para confirmar');
    await user.type(input, 'eliminar');
    await user.click(screen.getByRole('button', { name: 'Eliminar' }));

    expect(onConfirm).toHaveBeenCalledTimes(1);
  });

  it('cancel has no side effect: it calls onCancel and never onConfirm', async () => {
    const onConfirm = vi.fn();
    const onCancel = vi.fn();
    const user = userEvent.setup();
    render(
      <TypedConfirmDialog
        title="Eliminar"
        message="¿Eliminar este plan?"
        confirmWord="eliminar"
        onConfirm={onConfirm}
        onCancel={onCancel}
      />,
    );

    await user.click(screen.getByRole('button', { name: 'Cancelar' }));

    expect(onCancel).toHaveBeenCalledTimes(1);
    expect(onConfirm).not.toHaveBeenCalled();
  });

  it('Escape also cancels with no side effect (inherited from Modal)', async () => {
    const onConfirm = vi.fn();
    const onCancel = vi.fn();
    const user = userEvent.setup();
    render(
      <TypedConfirmDialog
        title="Eliminar"
        message="¿Eliminar este plan?"
        confirmWord="eliminar"
        onConfirm={onConfirm}
        onCancel={onCancel}
      />,
    );

    await user.keyboard('{Escape}');

    expect(onCancel).toHaveBeenCalledTimes(1);
    expect(onConfirm).not.toHaveBeenCalled();
  });

  it('shows a pending confirm action and keeps the word-matched button disabled while in flight', async () => {
    const user = userEvent.setup();
    render(
      <TypedConfirmDialog
        title="Eliminar"
        message="¿Eliminar este plan?"
        confirmWord="eliminar"
        confirmLabel="Eliminar"
        pending
        onConfirm={vi.fn()}
        onCancel={vi.fn()}
      />,
    );

    const input = screen.getByLabelText('Escribe "eliminar" para confirmar');
    await user.type(input, 'eliminar');

    expect(screen.getByRole('button', { name: 'Eliminar' })).toBeDisabled();
    expect(input).toBeDisabled();
  });
});
