import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { NutritionRings, RING_ARCS } from './NutritionRings';

const consumed = { kcal: 446, proteinG: 32.5, carbsG: 66, fatG: 6.4 };
const target = { kcal: 2320, proteinG: 165, carbsG: 270, fatG: 65 };

/** The drawn sweep of one ring, as the fraction of its circumference that is painted. */
function sweep(container: HTMLElement, key: string): number {
  const arc = container.querySelector(`[data-arc="${key}"]`);
  if (arc === null) throw new Error(`No arc for ${key}`);
  const length = Number(arc.getAttribute('stroke-dasharray'));
  const offset = Number(arc.getAttribute('stroke-dashoffset'));
  return Number((1 - offset / length).toFixed(4));
}

describe('NutritionRings', () => {
  it('reads out the four figures so the rings are never the only carrier', () => {
    render(<NutritionRings consumed={consumed} target={target} />);

    expect(
      screen.getByRole('img', {
        name: '446 de 2320 kcal. Proteínas 32.5 de 165 g, carbohidratos 66 de 270 g, grasas 6.4 de 65 g.',
      }),
    ).toBeInTheDocument();
  });

  it('draws each ring at its own share of its own target', () => {
    const { container } = render(<NutritionRings consumed={consumed} target={target} />);

    expect(sweep(container, 'kcal')).toBeCloseTo(446 / 2320, 3);
    expect(sweep(container, 'proteinG')).toBeCloseTo(32.5 / 165, 3);
    expect(sweep(container, 'carbsG')).toBeCloseTo(66 / 270, 3);
    expect(sweep(container, 'fatG')).toBeCloseTo(6.4 / 65, 3);
  });

  /*
   * Going over is something to see, so the figures beside the rings keep saying it. The arc stops
   * at twelve o'clock: a second lap drawn over the first reads as less than a full ring, not more.
   */
  it('stops the arc at one full lap when the target is exceeded', () => {
    const { container } = render(
      <NutritionRings consumed={{ ...consumed, proteinG: 250 }} target={target} />,
    );

    expect(sweep(container, 'proteinG')).toBe(1);
  });

  it('draws no arc for a macro the plan sets no target for', () => {
    const { container } = render(
      <NutritionRings consumed={consumed} target={{ ...target, fatG: 0 }} />,
    );

    expect(sweep(container, 'fatG')).toBe(0);
  });

  it('keeps the tracks and says so when the plan fixes no targets at all', () => {
    const { container } = render(<NutritionRings consumed={consumed} target={null} />);

    expect(
      screen.getByRole('img', {
        name: '446 kcal. Proteínas 32.5 g, carbohidratos 66 g, grasas 6.4 g. Tu plan no fija objetivos.',
      }),
    ).toBeInTheDocument();
    for (const arc of RING_ARCS) expect(sweep(container, arc.key)).toBe(0);
    // The unfilled tracks still render, so the card keeps its shape instead of collapsing.
    expect(container.querySelectorAll('[data-track]')).toHaveLength(RING_ARCS.length);
  });
});
