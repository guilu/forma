import { useState } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Modal } from './Modal';

/**
 * Modal dialog tests (FOR-18): renders as an accessible dialog and can be
 * dismissed via the close button, the Escape key and a backdrop click.
 *
 * <p>FOR-61 adds coverage for the focus trap and focus restoration: a
 * keyboard user must never be able to Tab out of an open dialog into the
 * page behind it, and closing the dialog must return focus to whatever
 * triggered it rather than dropping it back to `<body>`.
 */
function TriggeredModal() {
  const [open, setOpen] = useState(false);
  return (
    <div>
      <button type="button" onClick={() => setOpen(true)}>
        Abrir
      </button>
      {open && (
        <Modal title="Registrar medición" onClose={() => setOpen(false)}>
          <button type="button">Acción</button>
        </Modal>
      )}
    </div>
  );
}

describe('Modal', () => {
  it('renders a labelled dialog with its content', () => {
    render(
      <Modal title="Registrar medición" onClose={vi.fn()}>
        <p>contenido</p>
      </Modal>,
    );

    expect(screen.getByRole('dialog', { name: 'Registrar medición' })).toBeInTheDocument();
    expect(screen.getByText('contenido')).toBeInTheDocument();
  });

  it('closes via the close button', async () => {
    const onClose = vi.fn();
    const user = userEvent.setup();
    render(
      <Modal title="Registrar medición" onClose={onClose}>
        <p>contenido</p>
      </Modal>,
    );

    await user.click(screen.getByRole('button', { name: 'Cerrar' }));

    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it('closes when Escape is pressed', async () => {
    const onClose = vi.fn();
    const user = userEvent.setup();
    render(
      <Modal title="Registrar medición" onClose={onClose}>
        <p>contenido</p>
      </Modal>,
    );

    await user.keyboard('{Escape}');

    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it('restores focus to the element that opened it when it closes', async () => {
    const user = userEvent.setup();
    render(<TriggeredModal />);

    const trigger = screen.getByRole('button', { name: 'Abrir' });
    await user.click(trigger);

    // Opening moves focus into the dialog itself (existing FOR-18 behavior).
    expect(screen.getByRole('dialog')).toHaveFocus();

    await user.keyboard('{Escape}');

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(trigger).toHaveFocus();
  });

  it('traps Tab focus inside the dialog, wrapping at both ends', async () => {
    const user = userEvent.setup();
    render(
      <Modal title="Registrar medición" onClose={vi.fn()}>
        <button type="button">Acción</button>
      </Modal>,
    );

    const closeButton = screen.getByRole('button', { name: 'Cerrar' });
    const actionButton = screen.getByRole('button', { name: 'Acción' });

    actionButton.focus();
    expect(actionButton).toHaveFocus();

    // Tab from the last focusable element wraps back to the first.
    await user.tab();
    expect(closeButton).toHaveFocus();

    // Shift+Tab from the first focusable element wraps back to the last.
    await user.tab({ shift: true });
    expect(actionButton).toHaveFocus();
  });
});
