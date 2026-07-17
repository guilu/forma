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

  it.each([
    ['MANUAL', 'Manual', 'neutral'],
    ['WITHINGS', 'Withings', 'accent'],
    ['UNKNOWN', 'Origen desconocido', 'neutral'],
  ])('renders the %s source as "%s" with the %s tone', (value, label, tone) => {
    render(<StatusPill kind="source" value={value} />);

    expect(screen.getByText(label)).toHaveAttribute('data-tone', tone);
  });

  it.each([
    ['PLANNED', 'Planificado', 'neutral'],
    ['COMPLETED', 'Completado', 'accent'],
    ['SKIPPED', 'Saltado', 'warning'],
  ])('renders the %s training status as "%s" with the %s tone', (value, label, tone) => {
    render(<StatusPill kind="training" value={value} />);

    expect(screen.getByText(label)).toHaveAttribute('data-tone', tone);
  });

  it.each([
    ['ACTIVE', 'Activo', 'neutral'],
    ['ACHIEVED', 'Conseguido', 'accent'],
    ['ARCHIVED', 'Archivado', 'neutral'],
  ])('renders the %s goal status as "%s" with the %s tone (FOR-122)', (value, label, tone) => {
    render(<StatusPill kind="goalStatus" value={value} />);

    expect(screen.getByText(label)).toHaveAttribute('data-tone', tone);
  });

  it.each([
    ['HIGH', 'Carga alta', 'accent'],
    ['MEDIUM', 'Carga media', 'neutral'],
    ['LOW', 'Carga baja', 'neutral'],
  ])(
    'renders the %s muscle load as "%s" with the %s tone (FOR-53/FOR-136)',
    (value, label, tone) => {
      render(<StatusPill kind="muscleLoad" value={value} />);

      expect(screen.getByText(label)).toHaveAttribute('data-tone', tone);
    },
  );
});
