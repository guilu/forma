import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { BodyFigure } from './BodyFigure';

/**
 * Strength figures use the anatomical asset pack; running/rest retain the
 * schematic SVG. Both forms stay decorative unless the caller supplies a label.
 */
describe('BodyFigure', () => {
  it('uses the requested anatomical sex and side and stays decorative by default', () => {
    render(<BodyFigure sex="female" view="back" />);
    const figure = screen.getByTestId('anatomy-figure');
    expect(figure).toHaveAttribute('aria-hidden', 'true');
    expect(figure).toHaveAttribute('data-sex', 'female');
    expect(figure).toHaveAttribute('data-view', 'back');
  });

  it('exposes an accessible image label when one is provided', () => {
    render(<BodyFigure label="Pecho trabajado" />);
    expect(screen.getByRole('img', { name: 'Pecho trabajado' })).toBeInTheDocument();
  });
});
