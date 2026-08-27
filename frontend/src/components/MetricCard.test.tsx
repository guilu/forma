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

  /**
   * La variación respecto a la medición anterior, pegada al valor: «73,6 kg
   * (-0,2)». Entre paréntesis y en pequeño no compite con el número titular,
   * que sigue siendo lo que se lee de un vistazo.
   */
  it('renders an optional change beside the value', () => {
    render(<MetricCard label="Peso" value="73,6" unit="kg" delta="(-0,2)" />);

    expect(screen.getByText('(-0,2)')).toBeInTheDocument();
  });

  /**
   * «(-0,2)» no significa nada suelto. Lo que se ve es decoración sobre una
   * frase que dice de qué es la diferencia y contra qué se mide.
   */
  it('spells the change out for whoever is not looking at it', () => {
    render(
      <MetricCard
        label="Peso"
        value="73,6"
        unit="kg"
        delta="(-0,2)"
        deltaDescription="0,2 kg menos que la medición anterior"
      />,
    );

    expect(screen.getByText('0,2 kg menos que la medición anterior')).toBeInTheDocument();
    expect(screen.getByText('(-0,2)')).toHaveAttribute('aria-hidden', 'true');
  });

  it('renders an optional caption under the value (FOR-164)', () => {
    render(<MetricCard label="Peso" value="73.6" unit="kg" caption="1 medición" />);

    expect(screen.getByText('1 medición')).toBeInTheDocument();
  });
});
