import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { axe } from '../test/axe';
import { DesignSystemExamples } from './DesignSystemExamples';

/**
 * Smoke test for the design-system usage/examples surface (FOR-50): every
 * primitive variant renders without crashing and stays queryable by role/text.
 */
describe('DesignSystemExamples', () => {
  it('renders every button variant', () => {
    render(<DesignSystemExamples />);

    for (const variant of ['primary', 'secondary', 'ghost', 'destructive']) {
      expect(screen.getByRole('button', { name: variant })).toBeInTheDocument();
    }
  });

  it('renders every severity, connection and plazo status pill', () => {
    render(<DesignSystemExamples />);

    expect(screen.getByText('Info')).toBeInTheDocument();
    expect(screen.getByText('Atención')).toBeInTheDocument();
    expect(screen.getByText('Acción')).toBeInTheDocument();
    expect(screen.getByText('Conectado')).toBeInTheDocument();
    expect(screen.getByText('No conectado')).toBeInTheDocument();
    expect(screen.getByText('Corto plazo')).toBeInTheDocument();
  });

  it('renders the chart container in ready, loading and empty states', () => {
    render(<DesignSystemExamples />);

    expect(screen.getAllByRole('img', { name: 'Peso' })).toHaveLength(1);
    expect(screen.getByRole('status')).toBeInTheDocument();
  });

  it('renders every example card title as <h2>, one level below the page <h1> (FOR-112)', () => {
    render(<DesignSystemExamples />);

    expect(screen.getByRole('heading', { name: 'Design system', level: 1 })).toBeInTheDocument();
    // Every card here is a direct sibling of the page <h1>, so per FOR-112
    // each must render as <h2> to avoid skipping a level.
    expect(screen.getByRole('heading', { name: 'Buttons', level: 2 })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Badges y estados', level: 2 })).toBeInTheDocument();
    expect(
      screen.getByRole('heading', { name: 'Campos de formulario', level: 2 }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole('heading', { name: 'Evolución de peso', level: 2 }),
    ).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Métricas', level: 2 })).toBeInTheDocument();
    expect(
      screen.getByRole('heading', { name: 'Distribución de macros', level: 2 }),
    ).toBeInTheDocument();
  });

  it('renders MetricCard examples with and without a trend sparkline (FOR-164)', () => {
    render(<DesignSystemExamples />);

    expect(screen.getByRole('heading', { name: 'Peso', level: 3 })).toBeInTheDocument();
    expect(
      screen.getByText(
        (_, element) => element?.tagName === 'P' && element.textContent === '82.4 kg',
      ),
    ).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Racha', level: 3 })).toBeInTheDocument();
    expect(
      screen.getByText(
        (_, element) => element?.tagName === 'P' && element.textContent === '12 días',
      ),
    ).toBeInTheDocument();
  });

  it('renders a MacroRing example with its accessible summary (FOR-164)', () => {
    render(<DesignSystemExamples />);

    expect(
      screen.getByRole('img', {
        name: 'Objetivo de macronutrientes: proteínas 162 gramos, carbohidratos 236 gramos, grasas 68 gramos',
      }),
    ).toBeInTheDocument();
    expect(screen.getByText('Proteínas')).toBeInTheDocument();
  });

  it('has no detectable accessibility violations (FOR-61/FOR-164)', async () => {
    const { container } = render(<DesignSystemExamples />);

    expect(await axe(container)).toHaveNoViolations();
  });
});
