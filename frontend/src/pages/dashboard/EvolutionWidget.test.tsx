import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { EvolutionWidget } from './EvolutionWidget';
import { listBodyMeasurements, type BodyMeasurement } from '../../api/bodyMeasurements';

vi.mock('../../api/bodyMeasurements', () => ({ listBodyMeasurements: vi.fn() }));

const listMock = vi.mocked(listBodyMeasurements);

const base: BodyMeasurement = {
  measuredAt: '2026-07-05T08:00:00Z',
  source: 'MANUAL',
  weightKg: 69.2,
  bodyFatPercentage: 12.1,
  leanMassKg: 62.9,
  bmi: 21.3,
};

const history = [
  base,
  { ...base, measuredAt: '2026-06-28T08:00:00Z', weightKg: 70.5, bodyFatPercentage: 13.0 },
];

/**
 * Six points over 50 days: enough that "7D" is a strict subset of the history
 * (so the range actually narrows the plot) while "Todos" shows all of it. The
 * dashboard widget's tabs were inert until FOR-188 — they rendered under
 * `aria-hidden` and changed nothing.
 */
const spread: BodyMeasurement[] = [
  { ...base, measuredAt: '2026-07-05T08:00:00Z', weightKg: 69.2 },
  { ...base, measuredAt: '2026-07-02T08:00:00Z', weightKg: 69.6 },
  { ...base, measuredAt: '2026-06-20T08:00:00Z', weightKg: 70.1 },
  { ...base, measuredAt: '2026-06-10T08:00:00Z', weightKg: 70.4 },
  { ...base, measuredAt: '2026-05-30T08:00:00Z', weightKg: 71.0 },
  { ...base, measuredAt: '2026-05-16T08:00:00Z', weightKg: 71.5 },
];

describe('EvolutionWidget', () => {
  beforeEach(() => {
    listMock.mockReset();
  });

  it('shows the latest value for the default metric and plots the series', async () => {
    listMock.mockResolvedValue(history);

    render(<EvolutionWidget />);

    // Latest weight highlighted.
    expect(await screen.findByText('69.2')).toBeInTheDocument();
    expect(screen.getByRole('img', { name: /Evolución de peso/ })).toBeInTheDocument();
  });

  it('re-plots a different backed metric when the selector changes', async () => {
    listMock.mockResolvedValue(history);
    const user = userEvent.setup();

    render(<EvolutionWidget />);
    await screen.findByText('69.2');

    await user.selectOptions(screen.getByRole('combobox', { name: 'Métrica' }), 'fat');

    // Latest body-fat value + a body-fat trend.
    expect(screen.getByText('12.1')).toBeInTheDocument();
    expect(screen.getByRole('img', { name: /Evolución de grasa/ })).toBeInTheDocument();
  });

  it('shows an empty state when there are no measurements', async () => {
    listMock.mockResolvedValue([]);

    render(<EvolutionWidget />);

    expect(
      await screen.findByText(/Aún no hay mediciones para mostrar tu evolución/),
    ).toBeInTheDocument();
  });

  it('shows an error state when the request fails', async () => {
    listMock.mockRejectedValue(new Error('network'));

    render(<EvolutionWidget />);

    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo cargar tu evolución');
  });

  describe('range tabs', () => {
    it('narrows the plotted series to the chosen range', async () => {
      listMock.mockResolvedValue(spread);
      const user = userEvent.setup();

      render(<EvolutionWidget />);

      // "Todos" is the default: every measurement is plotted.
      const all = await screen.findByRole('button', { name: 'Todos' });
      expect(all).toHaveAttribute('aria-pressed', 'true');
      expect(screen.getByRole('img', { name: /6 mediciones/ })).toBeInTheDocument();

      await user.click(screen.getByRole('button', { name: '7D' }));

      // Only the two points within a week of the latest one survive.
      expect(screen.getByRole('img', { name: /2 mediciones/ })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: '7D' })).toHaveAttribute('aria-pressed', 'true');
      expect(all).toHaveAttribute('aria-pressed', 'false');
    });

    /**
     * Same rule the measurements page applies: a range that would show exactly
     * the same series as "Todos" is not a choice, so it is not offered.
     */
    /**
     * With two measurements a week apart, neither window narrows anything — so
     * the whole group goes, rather than leaving a lone "Todos" button that
     * cannot change what is on screen.
     */
    it('hides the group entirely when there is nothing to choose', async () => {
      listMock.mockResolvedValue(history);

      render(<EvolutionWidget />);

      await screen.findByText('69.2');
      expect(screen.queryByRole('group', { name: 'Rango del gráfico' })).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: 'Todos' })).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: '7D' })).not.toBeInTheDocument();
    });

    it('keeps the chosen range when the metric changes', async () => {
      listMock.mockResolvedValue(spread);
      const user = userEvent.setup();

      render(<EvolutionWidget />);

      await user.click(await screen.findByRole('button', { name: '7D' }));
      await user.selectOptions(screen.getByRole('combobox'), 'lean');

      expect(screen.getByRole('button', { name: '7D' })).toHaveAttribute('aria-pressed', 'true');
      expect(screen.getByRole('img', { name: /Evolución de músculo/ })).toBeInTheDocument();
    });
  });
});
