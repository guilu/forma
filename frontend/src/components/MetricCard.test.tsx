import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MetricCard } from './MetricCard';

/**
 * FOR-112: MetricCard forwards `headingLevel` to the underlying Card so its
 * title tag can match the page's actual heading order.
 */
describe('MetricCard', () => {
  it('renders its label and value', () => {
    render(<MetricCard label="Peso" value="73.6" unit="kg" />);

    expect(screen.getByRole('heading', { name: 'Peso' })).toBeInTheDocument();
    expect(screen.getByText('73.6')).toBeInTheDocument();
    expect(screen.getByText('kg')).toBeInTheDocument();
  });

  it('defaults to an <h3> label when headingLevel is not passed', () => {
    render(<MetricCard label="Peso" value="73.6" />);

    expect(screen.getByRole('heading', { name: 'Peso', level: 3 })).toBeInTheDocument();
  });

  it('forwards headingLevel to the underlying Card', () => {
    render(<MetricCard label="Peso" value="73.6" headingLevel={2} />);

    expect(screen.getByRole('heading', { name: 'Peso', level: 2 })).toBeInTheDocument();
  });
});
