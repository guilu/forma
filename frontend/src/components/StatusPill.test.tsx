import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { StatusPill } from './StatusPill';

describe('StatusPill', () => {
  it.each([
    ['INFO', 'Info'],
    ['WARNING', 'Atención'],
    ['ACTION', 'Acción'],
  ])('renders the %s severity as "%s"', (value, label) => {
    render(<StatusPill kind="severity" value={value} />);

    expect(screen.getByText(label)).toBeInTheDocument();
  });

  it.each([
    ['Conectado', 'accent'],
    ['No conectado', 'neutral'],
  ])('renders the %s connection status with the %s tone', (value, tone) => {
    render(<StatusPill kind="connection" value={value} />);

    expect(screen.getByText(value)).toHaveAttribute('data-tone', tone);
  });

  it.each(['Corto plazo', 'Medio plazo', 'Largo plazo'])('renders the %s tag', (value) => {
    render(<StatusPill kind="plazo" value={value} />);

    expect(screen.getByText(value)).toBeInTheDocument();
  });

  it('falls back to a neutral badge for an unknown severity value', () => {
    render(<StatusPill kind="severity" value="UNKNOWN" />);

    expect(screen.getByText('UNKNOWN')).toHaveAttribute('data-tone', 'neutral');
  });

  it('falls back to a neutral badge for an unknown connection value', () => {
    render(<StatusPill kind="connection" value="Desconocido" />);

    expect(screen.getByText('Desconocido')).toHaveAttribute('data-tone', 'neutral');
  });
});
