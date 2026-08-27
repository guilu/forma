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

/** Newest first, as the API returns it. */
const dosMediciones = () => [
  base,
  {
    ...base,
    measuredAt: daysAgo(21),
    weightKg: 75.2,
    bodyFatPercentage: 16.5,
    leanMassKg: 62.0,
  },
];

describe('TrendWidget', () => {
  it('shows honest copy when the window holds fewer than two measurements', async () => {
    conMediciones([base]);

    expect(await screen.findByText(/No hay mediciones en los últimos 30 días/)).toBeInTheDocument();
    expect(screen.queryByRole('img')).not.toBeInTheDocument();
  });

  /**
   * Una gráfica por métrica y no tres líneas en la misma caja: kilos y puntos
   * porcentuales no comparten escala, y superponerlos obligaba a normalizar cada
   * serie contra su propio máximo — tres ejes invisibles pintados como si fueran
   * uno. Cada fila tiene ahora su escala real y su unidad.
   */
  it('draws one chart per metric, each named on its own', async () => {
    conMediciones(dosMediciones());

    expect(
      await screen.findByRole('img', { name: /^Peso en los últimos 30 días/ }),
    ).toBeInTheDocument();
    expect(screen.getByRole('img', { name: /^Grasa en los últimos 30 días/ })).toBeInTheDocument();
    expect(
      screen.getByRole('img', { name: /^Músculo en los últimos 30 días/ }),
    ).toBeInTheDocument();
    expect(screen.getAllByRole('img')).toHaveLength(3);
  });

  /**
   * Los valores absolutos no estaban en ninguna parte de esta tarjeta: la
   * normalización los borraba y la leyenda solo decía la unidad.
   */
  it('puts the latest value and the 30-day change beside each chart', async () => {
    conMediciones(dosMediciones());

    await screen.findByRole('img', { name: /^Peso en los últimos 30 días/ });

    expect(screen.getByText('73.6 kg')).toBeInTheDocument();
    expect(screen.getByText('-1.6')).toBeInTheDocument();
    expect(screen.getByText('14.7 %')).toBeInTheDocument();
    expect(screen.getByText('-1.8')).toBeInTheDocument();
    expect(screen.getByText('62.8 kg')).toBeInTheDocument();
    expect(screen.getByText('+0.8')).toBeInTheDocument();
  });

  /** El resumen hablado dice de dónde a dónde va cada métrica, con su unidad. */
  it('spells the whole row out for whoever is not looking at it', async () => {
    conMediciones(dosMediciones());

    expect(
      await screen.findByRole('img', {
        name: 'Peso en los últimos 30 días: de 75.2 kg a 73.6 kg. 2 mediciones.',
      }),
    ).toBeInTheDocument();
  });

  /** Una métrica que nadie ha medido no dibuja una fila vacía. */
  it('drops a metric the window never recorded', async () => {
    conMediciones(dosMediciones().map((m) => ({ ...m, bodyFatPercentage: undefined })));

    await screen.findByRole('img', { name: /^Peso en los últimos 30 días/ });
    expect(screen.getAllByRole('img')).toHaveLength(2);
    expect(screen.queryByRole('img', { name: /^Grasa/ })).not.toBeInTheDocument();
  });

  /**
   * The assignment the design asks for: weight green, body fat amber, lean mass
   * blue — the same three the nutrition rings and the body tiles use, so a
   * metric keeps one colour across the whole panel. Asserted on the row dots,
   * which are the one place the colour reaches the DOM as an inline value.
   */
  it('paints each metric in its assigned token', async () => {
    conMediciones(dosMediciones());

    await screen.findByRole('img', { name: /^Peso en los últimos 30 días/ });
    const colourFor = (label: string) => {
      const row = screen.getByText(label).closest('li') as HTMLElement;
      return row.querySelector('span[style]')?.getAttribute('style');
    };

    expect(colourFor('Peso')).toContain('--color-accent');
    expect(colourFor('Grasa')).toContain('--color-warning-graphic');
    expect(colourFor('Músculo')).toContain('--color-info');
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

      // Cuatro mediciones en el historial, dos dentro de la ventana. Cada fila
      // cuenta las suyas, así que basta con preguntarle a una.
      expect(
        await screen.findByRole('img', { name: /^Peso .*2 mediciones\.$/ }),
      ).toBeInTheDocument();
    });

    /**
     * Las fechas dicen lo que hay dibujado, no lo que promete el título. Antes
     * decían la ventana entera mientras el trazo abarcaba sólo las mediciones
     * que cayeran dentro: una semana pintada de borde a borde bajo un par de
     * fechas que anunciaban un mes. La ventana la dice el título de la tarjeta.
     */
    it('labels the axis with the span it actually plots', async () => {
      conMediciones([on(1), on(6)]);

      expect(await screen.findByText(shortDate(6))).toBeInTheDocument();
      expect(screen.getByText(shortDate(1))).toBeInTheDocument();
      expect(screen.queryByText(shortDate(30))).not.toBeInTheDocument();
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
