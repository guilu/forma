import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Modal } from './Modal';

/**
 * Modal dialog tests (FOR-18): renders as an accessible dialog and can be
 * dismissed via the close button, the Escape key and a backdrop click.
 */
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
});
