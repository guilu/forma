import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { BodyFigure } from './BodyFigure';

/**
 * BodyFigure is a placeholder silhouette to be swapped for a real asset pack
 * later (see the component doc comment). These tests pin its two contracts:
 * decorative by default, and a labelled `img` when a label is provided.
 */
describe('BodyFigure', () => {
  it('is decorative (aria-hidden, no role) by default', () => {
    const { container } = render(<BodyFigure />);
    const svg = container.querySelector('svg');
    expect(svg).not.toBeNull();
    expect(svg).toHaveAttribute('aria-hidden', 'true');
    expect(svg).not.toHaveAttribute('role', 'img');
    // Marked as a placeholder so it's obvious what to replace later.
    expect(svg).toHaveAttribute('data-placeholder', 'body-figure');
  });

  it('exposes an accessible image label when one is provided', () => {
    render(<BodyFigure label="Pecho trabajado" />);
    expect(screen.getByRole('img', { name: 'Pecho trabajado' })).toBeInTheDocument();
  });
});
