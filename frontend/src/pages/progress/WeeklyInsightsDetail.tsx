import { Card, type HeadingLevel } from '../../components/Card';
import { StatusPill } from '../../components/StatusPill';
import {
  type WeeklyCheckIn,
  type WeeklyInsights,
  type WeeklyInsightsDeltas,
} from '../../api/insights';
import styles from './WeeklyInsightsDetail.module.css';

/**
 * Full "main recommendation + related signals + secondary recommendations +
 * disclaimer" rendering for one {@link WeeklyInsights} payload — extracted
 * from `InsightsSection` (FOR-56) so it can be reused verbatim (FOR-124 spec:
 * "mirroring the current week's existing rendering, reused for a historical
 * period") by both the current-week `InsightsSection` and the new
 * `InsightsHistorySection`'s selected-entry detail. Identical markup either
 * way; only the `insights` payload (current week vs. a persisted historical
 * one) differs.
 *
 * <p>"Related signals" now also render FOR-110's week-over-week deltas
 * alongside each absolute value, when the backend provides one — `undefined`
 * (no prior period, or a missing value on either side) renders the absolute
 * value only, exactly as before FOR-110, never a fabricated "no change" or
 * `undefined` text (spec FOR-124 Edge Cases). Every delta value comes
 * directly from the backend; nothing is computed client-side (ADR-006).
 *
 * <p>Delta text is always explicit (`+`/`−`/plain for exact zero) and never
 * conveyed by color alone (FOR-61, spec `specs/FOR-124/ui.md`
 * Accessibility).
 */
const DISCLAIMER =
  'Estas recomendaciones son orientativas y se generan a partir de tus datos. No sustituyen el diagnóstico ni el consejo de un profesional sanitario.';

const MINUS_SIGN = '−';

interface WeeklyInsightsDetailProps {
  readonly insights: WeeklyInsights;
  readonly headingLevel?: HeadingLevel;
}

function formatBody(value: number | undefined, unit: string): string | undefined {
  return value === undefined ? undefined : `${value.toFixed(1)} ${unit}`;
}

/** Signed, unambiguous delta text (e.g. `+0.4 kg`, `−0.4 kg`, `0.0 kg` for exact zero). */
function formatDelta(
  value: number | undefined,
  unit: string,
  decimals: number,
): string | undefined {
  if (value === undefined) {
    return undefined;
  }
  const rounded = Number(value.toFixed(decimals));
  const magnitude = Math.abs(rounded).toFixed(decimals);
  if (rounded > 0) {
    return `+${magnitude} ${unit}`;
  }
  if (rounded < 0) {
    return `${MINUS_SIGN}${magnitude} ${unit}`;
  }
  return `${magnitude} ${unit}`;
}

/** Signed session-count delta text with singular/plural wording (e.g. `+2 sesiones`, `−1 sesión`). */
function formatTrainingDelta(value: number | undefined): string | undefined {
  if (value === undefined) {
    return undefined;
  }
  const unit = Math.abs(value) === 1 ? 'sesión' : 'sesiones';
  if (value > 0) {
    return `+${value} ${unit}`;
  }
  if (value < 0) {
    return `${MINUS_SIGN}${Math.abs(value)} ${unit}`;
  }
  return `0 ${unit}`;
}

export function WeeklyInsightsDetail({ insights, headingLevel = 3 }: WeeklyInsightsDetailProps) {
  const { checkIn, main, secondary, deltas } = insights;

  return (
    <div className={styles.content}>
      <div className={styles.topRow}>
        <Card title="Recomendación principal" headingLevel={headingLevel}>
          <StatusPill kind="severity" value={main.severity} />
          <p className={styles.mainMessage}>{main.message}</p>
          <p className={styles.reason}>{main.reason}</p>
        </Card>

        <RelatedSignals checkIn={checkIn} deltas={deltas} headingLevel={headingLevel} />
      </div>

      {secondary.length > 0 && (
        <Card title="Otras recomendaciones" headingLevel={headingLevel}>
          <ul className={styles.secondaryList}>
            {secondary.map((rec, index) => (
              <li key={`${rec.category}-${index}`} className={styles.secondaryItem}>
                <StatusPill kind="severity" value={rec.severity} />
                <div>
                  <p className={styles.secondaryMessage}>{rec.message}</p>
                  <p className={styles.reason}>{rec.reason}</p>
                </div>
              </li>
            ))}
          </ul>
        </Card>
      )}

      <p className={styles.disclaimer}>{DISCLAIMER}</p>
    </div>
  );
}

function RelatedSignals({
  checkIn,
  deltas,
  headingLevel,
}: {
  readonly checkIn: WeeklyCheckIn;
  readonly deltas?: WeeklyInsightsDeltas;
  readonly headingLevel: HeadingLevel;
}) {
  const bodySignals = [
    {
      label: 'Peso',
      value: formatBody(checkIn.latestWeightKg, 'kg'),
      delta: formatDelta(deltas?.weightDeltaKg, 'kg', 1),
    },
    {
      label: 'Grasa corporal',
      value: formatBody(checkIn.latestBodyFatPercentage, '%'),
      delta: formatDelta(deltas?.bodyFatPercentageDelta, '%', 1),
    },
    {
      label: 'Masa magra',
      value: formatBody(checkIn.latestLeanMassKg, 'kg'),
      delta: formatDelta(deltas?.leanMassDeltaKg, 'kg', 1),
    },
  ].filter(
    (signal): signal is { label: string; value: string; delta: string | undefined } =>
      signal.value !== undefined,
  );

  const totalCompleted = checkIn.completedRunningSessions + checkIn.completedStrengthSessions;
  const trainingDelta = formatTrainingDelta(deltas?.trainingCompletionDelta);

  return (
    <Card title="Señales de esta semana" headingLevel={headingLevel}>
      <ul className={styles.signalsList}>
        {bodySignals.map((signal) => (
          <li key={signal.label} className={styles.signalItem}>
            <span className={styles.signalLabel}>{signal.label}</span>
            <span className={styles.signalValue}>
              {signal.value}
              {signal.delta && (
                <span className={styles.signalDelta}> ({signal.delta} vs. semana anterior)</span>
              )}
            </span>
          </li>
        ))}
        <li className={styles.signalItem}>
          <span className={styles.signalLabel}>Entrenamiento running</span>
          <span className={styles.signalValue}>
            {checkIn.completedRunningSessions} de {checkIn.plannedRunningSessions} sesiones
          </span>
        </li>
        <li className={styles.signalItem}>
          <span className={styles.signalLabel}>Entrenamiento de fuerza</span>
          <span className={styles.signalValue}>
            {checkIn.completedStrengthSessions} de {checkIn.plannedStrengthSessions} sesiones
          </span>
        </li>
        {trainingDelta && (
          <li className={styles.signalItem}>
            <span className={styles.signalLabel}>Entrenamientos completados</span>
            <span className={styles.signalValue}>
              {totalCompleted} sesiones
              <span className={styles.signalDelta}> ({trainingDelta} vs. semana anterior)</span>
            </span>
          </li>
        )}
      </ul>
    </Card>
  );
}
