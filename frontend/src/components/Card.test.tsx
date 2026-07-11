import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { Card } from './Card';

/**
 * Isolated component-rendering example (FOR-87). Template for future UI stories:
 * render a single component in isolation (no router, no backend) and assert on
 * the accessible output rather than snapshots. Keep assertions behavior-focused.
 */
describe('Card', () => {
  it('renders its title as a heading', () => {
    render(<Card title="Peso">content</Card>);

    expect(screen.getByRole('heading', { name: 'Peso' })).toBeInTheDocument();
  });

  it('renders its children', () => {
    render(
      <Card>
        <p>73.6 kg</p>
      </Card>,
    );

    expect(screen.getByText('73.6 kg')).toBeInTheDocument();
  });

  it('omits the heading when no title is provided', () => {
    render(<Card>only content</Card>);

    expect(screen.queryByRole('heading')).not.toBeInTheDocument();
  });

  it('renders an optional header action next to the title', () => {
    render(
      <Card title="Evolución de peso" action={<button type="button">Ver todo</button>}>
        content
      </Card>,
    );

    expect(screen.getByRole('heading', { name: 'Evolución de peso' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Ver todo' })).toBeInTheDocument();
  });
});
