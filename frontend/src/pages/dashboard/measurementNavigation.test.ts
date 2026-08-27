import { describe, expect, it } from 'vitest';
import { closestIndexTo, indexAfterJump } from './measurementNavigation';
import type { BodyMeasurement } from '../../api/bodyMeasurements';

const measurement = (iso: string): BodyMeasurement => ({
  measuredAt: iso,
  source: 'MANUAL',
  weightKg: 74,
  bodyFatPercentage: 15,
  leanMassKg: 62,
  bmi: 22.5,
});

/** Como llega de la API: de más nueva a más vieja. */
const history = [
  measurement('2026-08-27T08:00:00Z'),
  measurement('2026-07-30T08:00:00Z'),
  measurement('2026-07-26T08:00:00Z'),
  measurement('2026-02-27T08:00:00Z'),
  measurement('2025-08-25T08:00:00Z'),
  measurement('2025-01-10T08:00:00Z'),
];

describe('closestIndexTo', () => {
  it('lands on the nearest measurement, before or after the date asked for', () => {
    // Un día después de la del 26 de julio y tres antes de la del 30.
    expect(closestIndexTo(history, Date.parse('2026-07-27T08:00:00Z'))).toBe(2);
  });

  it('clamps to the ends instead of falling off them', () => {
    expect(closestIndexTo(history, Date.parse('2030-01-01T00:00:00Z'))).toBe(0);
    expect(closestIndexTo(history, Date.parse('2000-01-01T00:00:00Z'))).toBe(history.length - 1);
  });

  /** Empatadas gana la reciente: es lo que espera quien mira un panel. */
  it('prefers the newer of two equidistant measurements', () => {
    const pair = [measurement('2026-08-10T00:00:00Z'), measurement('2026-08-08T00:00:00Z')];

    expect(closestIndexTo(pair, Date.parse('2026-08-09T00:00:00Z'))).toBe(0);
  });
});

describe('indexAfterJump', () => {
  it('sends "latest" to the newest measurement', () => {
    expect(indexAfterJump(history, 4, 'latest')).toBe(0);
  });

  it('sends "-30 d" to the measurement nearest a month before the selected one', () => {
    // Desde el 27 de agosto, un mes atrás es el 28 de julio: la más cercana es
    // la del 30 de julio, a dos días, frente a la del 26, a cuatro.
    expect(indexAfterJump(history, 0, 'back30d')).toBe(1);
  });

  /**
   * Los saltos son relativos a lo que se está mirando, no a hoy. Es lo que
   * permite recorrer un historial largo a zancadas; medidos desde la última
   * medición, el botón quedaría inerte a partir de la segunda pulsación.
   */
  it('measures the jump from the selected measurement, not from the newest', () => {
    const first = indexAfterJump(history, 0, 'back30d');
    const second = indexAfterJump(history, first, 'back30d');

    expect(second).toBeGreaterThan(first);
  });

  it('sends "-1 año" a calendar year back, not 365 days', () => {
    // Desde el 27 de febrero de 2026, un año atrás es el 27 de febrero de 2025:
    // la más cercana es la del 10 de enero de 2025.
    expect(indexAfterJump(history, 3, 'back1y')).toBe(5);
  });

  /** Un año atrás con tres meses de historial no es un hueco: es la más antigua. */
  it('lands on the oldest measurement when the jump overshoots the history', () => {
    const short = [measurement('2026-08-27T08:00:00Z'), measurement('2026-06-01T08:00:00Z')];

    expect(indexAfterJump(short, 0, 'back1y')).toBe(1);
  });
});
