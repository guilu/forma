import type { BodyMeasurement } from '../../api/bodyMeasurements';

/**
 * Cómo se mueve el selector de fecha del panel por el historial de mediciones.
 *
 * <p>Se navega por las fechas que TIENEN medición, no por el calendario: un día
 * sin nada registrado no tiene nada que enseñar. Por eso ningún salto aterriza
 * en una fecha exacta — todos caen en la medición más cercana a la fecha
 * pedida, que es la única que puede pintar la tarjeta.
 *
 * <p>Aparte del componente porque es la parte que se puede equivocar: la
 * cercanía, el signo de los saltos y los extremos de la lista. El componente
 * dibuja botones; esto decide a qué índice llevan.
 */

/** Los tres saltos rápidos de la cabecera. */
export type DateJump = 'latest' | 'back30d' | 'back1y';

const DAY_MS = 24 * 60 * 60 * 1000;

/**
 * El índice de la medición más cercana a un instante, en una lista que llega
 * de más nueva a más vieja.
 *
 * <p>Empatada la distancia gana la más nueva, que es la primera que se
 * encuentra recorriendo la lista: si dos mediciones equidistan de la fecha
 * pedida, enseñar la reciente es lo que espera quien mira un panel.
 */
export function closestIndexTo(history: readonly BodyMeasurement[], targetMs: number): number {
  let closest = 0;
  let smallest = Infinity;
  history.forEach((measurement, index) => {
    const distance = Math.abs(Date.parse(measurement.measuredAt) - targetMs);
    if (distance < smallest) {
      smallest = distance;
      closest = index;
    }
  });
  return closest;
}

/**
 * A dónde lleva cada salto, partiendo de la medición seleccionada.
 *
 * <p><b>Los saltos son relativos, no absolutos.</b> «-30 d» no significa «hace
 * treinta días desde hoy», sino «treinta días antes de lo que estoy mirando»,
 * así que pulsarlo dos veces retrocede dos meses. Con un historial largo eso es
 * lo que permite recorrerlo a zancadas; un salto absoluto dejaría el botón
 * inerte a partir de la segunda pulsación.
 *
 * <p>El año se resta por calendario y no en días, que es lo que hace que un 29
 * de febrero caiga donde tiene que caer.
 *
 * <p>Fuera del historial no hay nada que enseñar: pedir un año atrás con tres
 * meses de mediciones aterriza en la más antigua, no en un hueco.
 */
export function indexAfterJump(
  history: readonly BodyMeasurement[],
  selected: number,
  jump: DateJump,
): number {
  if (jump === 'latest' || history.length === 0) {
    return 0;
  }
  const from = new Date(Date.parse(history[selected].measuredAt));
  if (jump === 'back30d') {
    return closestIndexTo(history, from.getTime() - 30 * DAY_MS);
  }
  from.setFullYear(from.getFullYear() - 1);
  return closestIndexTo(history, from.getTime());
}
