import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
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
  });
});
