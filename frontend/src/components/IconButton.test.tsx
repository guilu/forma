import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Icon } from './Icon';
import { IconButton, type IconButtonVariant } from './IconButton';

/**
 * Icon-only action tests. The point of the component is that a control with no
 * text still reaches the accessibility tree with a name, so that is what these
 * assert: the label is required by the type and becomes the accessible name,
 * and the glyph stays decorative. Appearance props (variant/tone/size) are
 * presentation only, so they are asserted to change nothing semantic.
 */
describe('IconButton', () => {
  const variants: IconButtonVariant[] = ['surface', 'soft', 'ghost'];

  it('exposes its label as the accessible name', () => {
    render(
      <IconButton label="Notificaciones">
        <Icon name="bell" />
      </IconButton>,
    );

    expect(screen.getByRole('button', { name: 'Notificaciones' })).toBeInTheDocument();
  });

  it.each(variants)('renders the %s variant as a named native button', (variant) => {
    render(
      <IconButton label="Editar" variant={variant}>
        <Icon name="edit" />
      </IconButton>,
    );

    expect(screen.getByRole('button', { name: 'Editar' })).toBeInTheDocument();
  });

  it('calls onClick when activated', async () => {
    const onClick = vi.fn();
    const user = userEvent.setup();
    render(
      <IconButton label="Cerrar" onClick={onClick}>
        <Icon name="cross" />
      </IconButton>,
    );

    await user.click(screen.getByRole('button', { name: 'Cerrar' }));

    expect(onClick).toHaveBeenCalledTimes(1);
  });

  it('is disabled and not activatable when disabled', async () => {
    const onClick = vi.fn();
    const user = userEvent.setup();
    render(
      <IconButton label="Eliminar" disabled onClick={onClick}>
        <Icon name="trash" />
      </IconButton>,
    );

    const button = screen.getByRole('button', { name: 'Eliminar' });
    expect(button).toBeDisabled();
    await user.click(button);
    expect(onClick).not.toHaveBeenCalled();
  });

  it('defaults to type="button" so it never submits a form by accident', () => {
    render(
      <IconButton label="Editar">
        <Icon name="edit" />
      </IconButton>,
    );

    expect(screen.getByRole('button', { name: 'Editar' })).toHaveAttribute('type', 'button');
  });

  it('keeps the danger tone visual only — it announces nothing extra', () => {
    render(
      <IconButton label="Eliminar medición" tone="danger" size="lg">
        <Icon name="trash" />
      </IconButton>,
    );

    const button = screen.getByRole('button', { name: 'Eliminar medición' });
    expect(button).toBeInTheDocument();
    expect(button).not.toHaveAttribute('aria-pressed');
  });

  it('forwards the caller className so pages keep control of layout', () => {
    render(
      <IconButton label="Anterior" className="page-local">
        <Icon name="chevron" />
      </IconButton>,
    );

    expect(screen.getByRole('button', { name: 'Anterior' })).toHaveClass('page-local');
  });
});
