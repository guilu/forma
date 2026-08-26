import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Icon } from './Icon';
import { IconButton, type IconButtonVariant } from './IconButton';
import iconButtonCss from './IconButton.module.css?raw';

/**
 * Icon-only action tests. The point of the component is that a control with no
 * text still reaches the accessibility tree with a name, so that is what these
 * assert: the label is required by the type and becomes the accessible name,
 * and the glyph stays decorative. Appearance props (variant/tone/size) are
 * presentation only, so they are asserted to change nothing semantic.
 */
describe('IconButton', () => {
  const variants: IconButtonVariant[] = ['surface', 'soft', 'ghost'];

  /*
   * All three sizes are round; the size decides how big, not how square. They
   * used to step `--radius-sm` / `--radius-md` with the box, which put a dense
   * row of icons in a different family from the buttons beside them. Each size
   * is asserted separately so a later edit cannot round two and leave one.
   */
  it.each(['sm', 'md', 'lg'])('gives the %s size the pill radius', (size) => {
    expect(iconButtonCss, `${size} is not round`).toMatch(
      new RegExp(`\\.${size}\\s*{[^}]*border-radius:\\s*var\\(--radius-full\\);`, 's'),
    );
  });

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

/*
 * An icon-only control has no room for a spinner beside its glyph — there is no
 * "beside". So loading replaces the glyph rather than joining it, which is the
 * one difference from `Button`; everything else about the state is the same
 * contract: the action cannot be re-triggered, and assistive tech is told the
 * control is busy rather than being left to infer it from a disabled attribute.
 */
describe('loading', () => {
  it('replaces the glyph with a spinner and blocks the action', async () => {
    const onClick = vi.fn();
    render(
      <IconButton label="Guardar" loading onClick={onClick}>
        <span data-testid="glyph">★</span>
      </IconButton>,
    );

    const button = screen.getByRole('button', { name: 'Guardar' });
    expect(button).toBeDisabled();
    expect(button).toHaveAttribute('aria-busy', 'true');
    expect(screen.queryByTestId('glyph')).not.toBeInTheDocument();

    await userEvent.click(button);
    expect(onClick).not.toHaveBeenCalled();
  });

  it('keeps its name while busy, so the control never goes anonymous mid-action', () => {
    render(
      <IconButton label="Completar carrera" loading>
        <span>★</span>
      </IconButton>,
    );

    expect(screen.getByRole('button', { name: 'Completar carrera' })).toBeInTheDocument();
  });

  it('shows the glyph and no busy state when it is not loading', () => {
    render(
      <IconButton label="Guardar">
        <span data-testid="glyph">★</span>
      </IconButton>,
    );

    expect(screen.getByTestId('glyph')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Guardar' })).not.toHaveAttribute('aria-busy');
  });
});
