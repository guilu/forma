import { describe, expect, it } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { TrendWidget } from './TrendWidget';
import type { BodyMeasurement } from '../../api/bodyMeasurements';

/*
 * Render puro: el widget ya no busca nada. El historial se lee una vez en
 * `DashboardPage` y se reparte, así que aquí solo entra por props — ver
 * `measurementsState.ts`.
 */
const conMediciones = (history: BodyMeasurement[]) =>
  render(<TrendWidget state={{ status: 'ready', history }} />);

/*
 * Dates are relative to the run, not pinned with fake timers. The widget windows
 * on the clock, so the fixtures have to move with it — and freezing time instead
 * left Recharts' own timers queued past the end of the test, which surfaced in
 * CI as `cancelAnimationFrame is not defined` after the environment tore down.
 */
const DAY_MS = 24 * 60 * 60 * 1000;
const daysAgo = (days: number): string => new Date(Date.now() - days * DAY_MS).toISOString();
const shortDate = (days: number): string =>
  new Date(Date.now() - days * DAY_MS).toLocaleDateString('es-ES', {
    day: 'numeric',
    month: 'short',
  });

const base: BodyMeasurement = {
  measuredAt: daysAgo(1),
  source: 'MANUAL',
  weightKg: 73.6,
  bodyFatPercentage: 14.7,
  leanMassKg: 62.8,
  bmi: 22.7,
};

describe('TrendWidget', () => {
  it('shows honest copy when the window holds fewer than two measurements', async () => {
    conMediciones([base]);

    expect(await screen.findByText(/No hay mediciones en los últimos 30 días/)).toBeInTheDocument();
    expect(screen.queryByRole('img')).not.toBeInTheDocument();
  });

  it('plots the trend line once there are at least two measurements', async () => {
    conMediciones([base, { ...base, measuredAt: daysAgo(3), weightKg: 74.1 }]);

    expect(await screen.findByRole('img', { name: /Tendencia de peso/ })).toBeInTheDocument();
  });

  /**
   * The assignment the design asks for: weight green, body fat blue, lean mass
   * amber. It shipped with fat on the warning token and muscle on info, which
   * read as "body fat is a warning". Asserted on the legend dots, which are the
   * one place the colour reaches the DOM as an inline value.
   */
  it('paints each series in its assigned token', async () => {
    conMediciones([base, { ...base, measuredAt: daysAgo(3), weightKg: 74.1 }]);

    await screen.findByRole('img', { name: /Tendencia de peso/ });
    const colourFor = (label: string) => {
      const item = screen.getByText(label).closest('li') as HTMLElement;
      return item.querySelector('span')?.getAttribute('style');
    };

    // La misma asignación que los aros de nutrición y que las fichas de arriba:
    // grasa en ámbar, músculo en azul.
    expect(colourFor('Peso (kg)')).toContain('--color-accent');
    expect(colourFor('Grasa (%)')).toContain('--color-warning-graphic');
    expect(colourFor('Músculo (kg)')).toContain('--color-info');
  });

  /**
   * The card is titled "30 días" and was showing the last 30 *measurements* — a
   * `slice(0, 30)` over a newest-first list. With 896 rows that plotted ten
   * months of history under a label promising one, and squeezed a real month
   * into the last few pixels.
   */
  describe('the 30-day window', () => {
    const on = (days: number): BodyMeasurement => ({ ...base, measuredAt: daysAgo(days) });

    it('plots only the measurements inside it', async () => {
      conMediciones([
        on(1),
        on(20),
        // Older than 30 days: outside the window the title promises.
        on(90),
        on(260),
      ]);

      expect(await screen.findByRole('img', { name: /2 mediciones/ })).toBeInTheDocument();
    });

    it('labels the axis with the window, not with the data', async () => {
      conMediciones([on(1), on(2)]);

      // The window is fixed: 30 days back from today, to today — regardless of
      // where the first and last measurements happen to sit inside it.
      expect(await screen.findByText(shortDate(30))).toBeInTheDocument();
      expect(screen.getByText(shortDate(0))).toBeInTheDocument();
    });

    it('says the window is empty when every measurement predates it', async () => {
      conMediciones([on(90), on(260)]);

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
    render(<TrendWidget state={{ status: 'error' }} />);

    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo cargar tu tendencia');
  });
});
