import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Chip } from './Chip';
import chipCss from './Chip.module.css?raw';

/**
 * Selection-control tests. A chip looks the same in all three groupings, but
 * the state it publishes is not interchangeable: a tablist announces
 * `aria-selected`, a radiogroup `aria-checked`, and a standalone toggle
 * `aria-pressed`. The component owns the appearance; the caller owns the
 * semantics, so these tests pin the mapping.
 */
describe('Chip', () => {
  /*
   * Round in both sizes, like `Button`, `IconButton` and `Badge`. A chip is a
   * different family — it reports a choice rather than inviting one — but that
   * difference is carried by the fill and the accessible state, never by the
   * corner. Each size separately so a later edit cannot round one and leave the
   * other.
   */
  it.each(['md', 'sm'])('gives the %s size the pill radius', (size) => {
    expect(chipCss, `${size} is not round`).toMatch(
      new RegExp(`\\.${size}\\s*{[^}]*border-radius:\\s*var\\(--radius-full\\);`, 's'),
    );
  });

  it('publishes tab semantics as aria-selected', () => {
    render(
      <Chip semantics="tab" selected>
        Frutas y verduras
      </Chip>,
    );

    expect(screen.getByRole('tab', { name: 'Frutas y verduras' })).toHaveAttribute(
      'aria-selected',
      'true',
    );
  });

  it('publishes radio semantics as aria-checked', () => {
    render(
      <Chip semantics="radio" selected={false}>
        Del catálogo
      </Chip>,
    );

    expect(screen.getByRole('radio', { name: 'Del catálogo' })).toHaveAttribute(
      'aria-checked',
      'false',
    );
  });

  it('defaults to toggle semantics, published as aria-pressed', () => {
    render(<Chip selected>30 días</Chip>);

    const chip = screen.getByRole('button', { name: '30 días', pressed: true });
    expect(chip).toBeInTheDocument();
    expect(chip).not.toHaveAttribute('aria-selected');
  });

  it('never publishes more than one state attribute', () => {
    render(
      <Chip semantics="tab" selected>
        Proteínas
      </Chip>,
    );

    const chip = screen.getByRole('tab', { name: 'Proteínas' });
    expect(chip).not.toHaveAttribute('aria-pressed');
    expect(chip).not.toHaveAttribute('aria-checked');
  });

  it('calls onClick when activated', async () => {
    const onClick = vi.fn();
    const user = userEvent.setup();
    render(
      <Chip selected={false} onClick={onClick}>
        Lácteos
      </Chip>,
    );

    await user.click(screen.getByRole('button', { name: 'Lácteos' }));

    expect(onClick).toHaveBeenCalledTimes(1);
  });

  it('defaults to type="button" so it never submits the form it filters', () => {
    render(<Chip selected={false}>Todos</Chip>);

    expect(screen.getByRole('button', { name: 'Todos' })).toHaveAttribute('type', 'button');
  });
});
