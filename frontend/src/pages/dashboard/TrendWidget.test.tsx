import { afterEach, describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { TrendWidget } from './TrendWidget';
import { listBodyMeasurements, type BodyMeasurement } from '../../api/bodyMeasurements';

vi.mock('../../api/bodyMeasurements', () => ({ listBodyMeasurements: vi.fn() }));

const listMock = vi.mocked(listBodyMeasurements);

const base: BodyMeasurement = {
  measuredAt: '2026-07-05T08:00:00Z',
  source: 'MANUAL',
  weightKg: 73.6,
  bodyFatPercentage: 14.7,
  leanMassKg: 62.8,
  bmi: 22.7,
};

describe('TrendWidget', () => {
  beforeEach(() => {
    listMock.mockReset();
    // The widget windows on the clock now, so every test here pins it.
    // `shouldAdvanceTime` keeps timers ticking under the frozen time — without
    // it testing-library's async waits never resolve and tests time out rather
    // than failing on their assertion.
    vi.useFakeTimers({ shouldAdvanceTime: true });
    vi.setSystemTime(new Date('2026-07-30T12:00:00Z'));
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('shows honest copy when the window holds fewer than two measurements', async () => {
    listMock.mockResolvedValue([base]);

    render(<TrendWidget />);

    expect(await screen.findByText(/No hay mediciones en los últimos 30 días/)).toBeInTheDocument();
    expect(screen.queryByRole('img')).not.toBeInTheDocument();
  });

  it('plots the trend line once there are at least two measurements', async () => {
    listMock.mockResolvedValue([
      base,
      { ...base, measuredAt: '2026-07-08T08:00:00Z', weightKg: 74.1 },
    ]);

    render(<TrendWidget />);

    expect(await screen.findByRole('img', { name: /Tendencia de peso/ })).toBeInTheDocument();
  });

  /**
   * The assignment the design asks for: weight green, body fat blue, lean mass
   * amber. It shipped with fat on the warning token and muscle on info, which
   * read as "body fat is a warning". Asserted on the legend dots, which are the
   * one place the colour reaches the DOM as an inline value.
   */
  it('paints each series in its assigned token', async () => {
    listMock.mockResolvedValue([
      base,
      { ...base, measuredAt: '2026-07-08T08:00:00Z', weightKg: 74.1 },
    ]);

    render(<TrendWidget />);

    await screen.findByRole('img', { name: /Tendencia de peso/ });
    const colourFor = (label: string) => {
      const item = screen.getByText(label).closest('li') as HTMLElement;
      return item.querySelector('span')?.getAttribute('style');
    };

    expect(colourFor('Peso (kg)')).toContain('--color-accent');
    expect(colourFor('Grasa (%)')).toContain('--color-info');
    expect(colourFor('Músculo (kg)')).toContain('--color-warning-graphic');
  });

  /**
   * The card is titled "30 días" and was showing the last 30 *measurements* — a
   * `slice(0, 30)` over a newest-first list. With 896 rows that plotted ten
   * months of history under a label promising one, and squeezed a real month
   * into the last few pixels.
   */
  describe('the 30-day window', () => {
    const on = (measuredAt: string): BodyMeasurement => ({ ...base, measuredAt });

    it('plots only the measurements inside it', async () => {
      listMock.mockResolvedValue([
        on('2026-07-29T08:00:00Z'),
        on('2026-07-10T08:00:00Z'),
        // Older than 30 days: outside the window the title promises.
        on('2026-05-02T08:00:00Z'),
        on('2025-11-11T08:00:00Z'),
      ]);

      render(<TrendWidget />);

      expect(await screen.findByRole('img', { name: /2 mediciones/ })).toBeInTheDocument();
    });

    it('labels the axis with the window, not with the data', async () => {
      listMock.mockResolvedValue([on('2026-07-29T08:00:00Z'), on('2026-07-28T08:00:00Z')]);

      render(<TrendWidget />);

      // The window is fixed: 30 days back from today, to today — regardless of
      // where the first and last measurements happen to sit inside it.
      expect(await screen.findByText('30 jun')).toBeInTheDocument();
      expect(screen.getByText('30 jul')).toBeInTheDocument();
    });

    it('says the window is empty when every measurement predates it', async () => {
      listMock.mockResolvedValue([on('2026-05-02T08:00:00Z'), on('2025-11-11T08:00:00Z')]);

      render(<TrendWidget />);

      // Loading and the empty window are both announced via role="status", so
      // wait for the terminal content rather than the first match.
      await waitFor(() =>
        expect(screen.getByRole('status')).toHaveTextContent(
          'No hay mediciones en los últimos 30 días',
        ),
      );
      expect(screen.queryByRole('img')).not.toBeInTheDocument();
    });
  });

  it('shows an error state when the request fails', async () => {
    listMock.mockRejectedValue(new Error('network'));

    render(<TrendWidget />);

    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo cargar tu tendencia');
  });
});
