import { useCallback, useEffect, useState } from 'react';
import { EmptyState } from '../../components/EmptyState';
import { ErrorState } from '../../components/ErrorState';
import { Icon } from '../../components/Icon';
import { MuscleSilhouette } from '../../components/MuscleSilhouette';
import { StatusPill } from '../../components/StatusPill';
import { WidgetLoading } from '../../components/WidgetLoading';
import { useAnatomySex } from '../../hooks/useAnatomySex';
import { getTrainingWeek, type TrainingSession, type TrainingWeek } from '../../api/training';
import { overlayFromMuscleMap } from '../trainingMuscleOverlay';
import { useSessionMuscles } from '../useSessionMuscles';
import { muscleSummary } from '../trainingMuscleLabels';
import { SessionDetailModal, type DetailTarget } from '../training/SessionDetailModal';
import { DAY_LABELS } from '../training/trainingLabels';
import { useSessionActions } from '../training/useSessionActions';
import { WidgetSection } from './WidgetSection';
import { ProgressBar } from './ProgressBar';
import styles from './TrainingWidget.module.css';

/**
 * Weekly training status widget (FOR-51): today's session drawn on the body it
 * works, plus how many of the week's sessions are completed, from the FOR-26
 * training-week read model. Renders the API data as returned (ADR-006).
 *
 * <p><b>The card is the control.</b> Clicking anywhere on it opens the same
 * session dialog the training page opens — the one place the session can be
 * completed, skipped or moved — so the card no longer carries a button of its
 * own. A button inside a clickable card works with a mouse and with nothing
 * else; the route to the full plan moved up to the section's own "Ver plan"
 * link, which is what {@link WidgetSection} already offers every widget.
 *
 * <p>The silhouettes are the training page's, at the size this card has room
 * for: the worked muscles are lit from the same FOR-136 muscle map through the
 * same mask-over-colour technique, so a session looks like itself on both
 * screens. A rest day draws the resting body and is deliberately NOT clickable
 * — there is no session to open, and a card that lights up under the pointer
 * and then does nothing is worse than a flat one.
 *
 * <p>The "completed / total" count is a plain tally over the sessions already
 * returned by `GET /training/week` (all kinds combined) — display aggregation,
 * not the FOR-28 `WeeklyTrainingSummary` domain calculation (which splits by
 * session kind and sums running distance, and is not exposed over HTTP).
 * Documented simplification, see FOR-51 PR "Known limitations".
 */
type State =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | { readonly status: 'empty' }
  | { readonly status: 'ready'; readonly week: TrainingWeek };

const JS_DAY_TO_ENUM = [
  'SUNDAY',
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
] as const;

export function TrainingWidget({ date = new Date() }: { readonly date?: Date } = {}) {
  const [state, setState] = useState<State>({ status: 'loading' });
  const [detail, setDetail] = useState<DetailTarget | undefined>(undefined);
  const anatomySex = useAnatomySex();

  const load = useCallback(async () => {
    try {
      const week = await getTrainingWeek();
      const hasAnySession = week.days.some((day) => day.sessions.length > 0);
      setState(hasAnySession ? { status: 'ready', week } : { status: 'empty' });
    } catch {
      setState({ status: 'error' });
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const { pendingId, mark, move } = useSessionActions({
    reload: load,
    onMarked: (sessionId, status) =>
      setDetail((current) =>
        current && current.session.id === sessionId
          ? { ...current, session: { ...current.session, status } }
          : current,
      ),
    onMoved: () => setDetail(undefined),
  });

  return (
    <WidgetSection
      id="training-widget-title"
      title="Entrenamiento"
      linkTo="/app/training"
      linkLabel="Ver plan"
    >
      {renderContent(state, date, setDetail, anatomySex)}
      {detail && (
        <SessionDetailModal
          target={detail}
          onClose={() => setDetail(undefined)}
          mark={mark}
          move={move}
          pending={pendingId === detail.session.id}
          anatomySex={anatomySex}
        />
      )}
    </WidgetSection>
  );
}

function renderContent(
  state: State,
  date: Date,
  openDetail: (target: DetailTarget) => void,
  anatomySex: 'male' | 'female',
) {
  if (state.status === 'loading') {
    return <WidgetLoading label="Cargando tu semana de entrenamiento…" rows={2} />;
  }

  if (state.status === 'error') {
    return (
      <ErrorState message="No se pudo cargar tu entrenamiento. Inténtalo de nuevo más tarde." />
    );
  }

  if (state.status === 'empty') {
    return (
      <EmptyState variant="filtered" title="No hay entrenamientos planificados esta semana." />
    );
  }

  const allSessions: TrainingSession[] = state.week.days.flatMap((day) => day.sessions);
  const completed = allSessions.filter((s) => s.status === 'COMPLETED').length;
  const total = allSessions.length;

  const today = state.week.days.find((day) => day.dayOfWeek === JS_DAY_TO_ENUM[date.getDay()]);
  const dayLabel = today ? (DAY_LABELS[today.dayOfWeek] ?? today.dayOfWeek) : '';
  /* The session the body is drawn for, matching the training page's own rule:
     a strength session on a day that also holds a run is the one with muscles
     to show. */
  const todaysSessions = today && !today.rest ? today.sessions : [];
  const session = todaysSessions.find((one) => one.kind === 'STRENGTH') ?? todaysSessions[0];

  return (
    <div className={styles.content}>
      {today && session ? (
        <div className={styles.today}>
          {/* Positioned over the whole block rather than wrapping it: the
              progress bar below carries `role="progressbar"`, and neither a
              progress bar nor a heading belongs inside a <button>. */}
          <button
            type="button"
            className={styles.open}
            onClick={() => openDetail({ dayOfWeek: today.dayOfWeek, session })}
          >
            <span className={styles.srOnly}>Ver el detalle de {session.title}</span>
          </button>
          <SessionHead session={session} dayLabel={dayLabel} />
          <SessionFigures session={session} sex={anatomySex} />
        </div>
      ) : (
        <div className={styles.today}>
          <p className={styles.message}>
            {today?.rest
              ? 'Hoy es día de descanso.'
              : 'No hay datos de hoy en el plan de esta semana.'}
          </p>
          {today?.rest && (
            <div className={styles.figures}>
              <MuscleSilhouette className={styles.figure} sex={anatomySex} variant="rest" />
            </div>
          )}
        </div>
      )}
      <div className={styles.completion}>
        <span className={styles.completionLabel}>
          {completed} de {total} sesiones completadas
        </span>
        {/*
         * Azul, el mismo tono que las proteínas en los aros de nutrición: cada
         * barra del panel dice de qué habla por su color, y el verde de acento
         * las dejaba a las dos iguales.
         */}
        <ProgressBar
          value={completed}
          max={total}
          label="Sesiones completadas esta semana"
          color="var(--color-info)"
        />
      </div>
    </div>
  );
}

/** Title, day and status — everything the card says in words. */
function SessionHead({
  session,
  dayLabel,
}: {
  readonly session: TrainingSession;
  readonly dayLabel: string;
}) {
  return (
    <div className={styles.session}>
      <span className={styles.sessionTitle}>
        <Icon name="training" size={22} className={styles.sessionIcon} />
        {session.title}
      </span>
      <span className={styles.sessionMeta}>
        <span className={styles.sessionDay}>
          Hoy · {dayLabel}
          {session.detail ? ` · ${session.detail}` : ''}
        </span>
        <StatusPill kind="training" value={session.status} />
      </span>
    </div>
  );
}

/**
 * The body, or bodies: a strength session works both sheets (a push day's
 * triceps live on the back), a run has its own whole-body art and no muscles to
 * light.
 */
function SessionFigures({
  session,
  sex,
}: {
  readonly session: TrainingSession;
  readonly sex: 'male' | 'female';
}) {
  const strength = session.kind === 'STRENGTH';
  // Asking a run for its muscle map is a round trip that always answers empty.
  const muscles = useSessionMuscles(strength ? session.id : undefined);

  if (!strength) {
    return (
      <div className={styles.figures}>
        <MuscleSilhouette
          className={styles.figure}
          sex={sex}
          variant={session.kind === 'RUNNING' ? 'running' : 'rest'}
        />
      </div>
    );
  }

  const overlay = overlayFromMuscleMap(muscles);
  const worked = muscles.length > 0;

  return (
    <div
      className={styles.figures}
      role={worked ? 'img' : undefined}
      aria-label={worked ? muscleSummary(muscles) : undefined}
    >
      <MuscleSilhouette className={styles.figure} sex={sex} view="front" muscles={overlay} />
      <MuscleSilhouette className={styles.figure} sex={sex} view="back" muscles={overlay} />
    </div>
  );
}
