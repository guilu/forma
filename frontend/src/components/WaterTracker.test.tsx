import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { WaterTracker } from './WaterTracker';

/**
 * WaterTracker renders static placeholder hydration data (no backend endpoint
 * exists yet — see the component doc comment). These tests pin the placeholder
 * output and its accessible summary, so a future wiring to real data is an
 * obvious, test-visible change.
 */
describe('WaterTracker', () => {
  it('renders the placeholder hydration values with an accessible summary', () => {
    render(<WaterTracker />);

    expect(screen.getByRole('heading', { name: 'Agua' })).toBeInTheDocument();
    expect(screen.getByText('84%')).toBeInTheDocument();
    expect(
      screen.getByRole('img', { name: /Hidratación: 2,1 de 2,5 litros \(84%\)/ }),
    ).toBeInTheDocument();
  });
});
