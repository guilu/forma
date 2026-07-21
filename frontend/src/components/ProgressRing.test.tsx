import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { ProgressRing } from './ProgressRing';

describe('ProgressRing', () => {
  it('exposes the accessible label and renders its center content', () => {
    render(
      <ProgressRing value={1} max={1} label="1 medición registrada">
        <span>1</span>
      </ProgressRing>,
    );

    expect(screen.getByRole('img', { name: '1 medición registrada' })).toBeInTheDocument();
    expect(screen.getByText('1')).toBeInTheDocument();
  });

  it('clamps the fill to 100% when value exceeds max and to 0% when max is 0', () => {
    const { rerender } = render(<ProgressRing value={5} max={2} label="over" />);
    expect(screen.getByRole('img', { name: 'over' })).toBeInTheDocument();

    // max 0 must not throw (division guard).
    rerender(<ProgressRing value={3} max={0} label="zero" />);
    expect(screen.getByRole('img', { name: 'zero' })).toBeInTheDocument();
  });
});
