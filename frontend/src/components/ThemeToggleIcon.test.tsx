import { describe, expect, it } from 'vitest';
import { render } from '@testing-library/react';
import { ThemeToggleIcon } from './ThemeToggleIcon';

describe('ThemeToggleIcon', () => {
  it('exposes which glyph it is showing, like the shared Icon does', () => {
    // The topbar's theme button already asserts its state through its
    // accessible name; `data-icon` mirrors Icon.tsx's convention so a test can
    // pin the glyph itself without reaching into CSS.
    const { container, rerender } = render(<ThemeToggleIcon icon="sun" />);
    expect(container.querySelector('svg')).toHaveAttribute('data-icon', 'sun');

    rerender(<ThemeToggleIcon icon="moon" />);
    expect(container.querySelector('svg')).toHaveAttribute('data-icon', 'moon');
  });

  it('keeps the same nodes across the swap so the glyph morphs instead of cutting', () => {
    // The whole point of this component over `<Icon name={isDark ? ... }>`: the
    // disc and the crescent mask are one persistent pair of elements whose CSS
    // state changes, not two paths that unmount. If a future change went back
    // to swapping subtrees, the transition would silently stop running.
    const { container, rerender } = render(<ThemeToggleIcon icon="sun" />);
    const disc = container.querySelector('circle[mask]') ?? container.querySelector('circle');

    rerender(<ThemeToggleIcon icon="moon" />);

    expect(container.querySelector('circle')).toBe(disc);
  });

  it('is decorative, so the button label remains the sole accessible name', () => {
    const { container } = render(<ThemeToggleIcon icon="sun" />);

    expect(container.querySelector('svg')).toHaveAttribute('aria-hidden', 'true');
  });

  it('draws the crescent from two arcs, not one gapped circle', () => {
    // Regression guard for how this was first built: masking only the outer
    // circle leaves a plain arc — a circle with a bite out of it — which is not
    // Icon's `moon`. The crescent needs its inner edge drawn too, so the r=7
    // circle at (16.8, 7.2) must be a real stroked element and not live only
    // inside the mask. Difference-blending the rendered result against Icon's
    // `moon` path is what caught the original; this pins the structure that
    // fixed it, since jsdom paints nothing.
    const { container } = render(<ThemeToggleIcon icon="moon" />);

    const drawn = [...container.querySelectorAll('svg > g > circle')];
    expect(
      drawn.map((c) => [c.getAttribute('cx'), c.getAttribute('cy'), c.getAttribute('r')]),
    ).toEqual([
      ['12', '12', '9'],
      ['16.8', '7.2', '7'],
    ]);
  });

  it('gives every instance its own mask ids', () => {
    // The topbar renders the toggle in two mutually exclusive branches today,
    // but Brand.tsx already documents what a baked-in id costs when a second
    // instance appears. `useId` keeps that from ever being a bug here.
    const { container } = render(
      <>
        <ThemeToggleIcon icon="sun" />
        <ThemeToggleIcon icon="moon" />
      </>,
    );

    const ids = [...container.querySelectorAll('mask')].map((mask) => mask.id);
    expect(ids).toHaveLength(4);
    expect(new Set(ids).size).toBe(4);
  });
});
