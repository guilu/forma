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

  it('defaults to an <h3> when headingLevel is not passed (FOR-112 regression guard)', () => {
    render(<Card title="Peso">content</Card>);

    expect(screen.getByRole('heading', { name: 'Peso', level: 3 })).toBeInTheDocument();
  });

  it.each([2, 4, 5, 6] as const)('renders an <h%s> when headingLevel is %s', (level) => {
    render(
      <Card title="Peso" headingLevel={level}>
        content
      </Card>,
    );

    expect(screen.getByRole('heading', { name: 'Peso', level })).toBeInTheDocument();
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

  it('omits the heading when no title is provided even if headingLevel is set', () => {
    render(<Card headingLevel={2}>only content</Card>);

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
