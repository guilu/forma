import { useEffect, useState } from 'react';
import { Button } from '../../components/Button';
import { Modal } from '../../components/Modal';
import { MuscleSilhouette, type AnatomySex } from '../../components/MuscleSilhouette';
import { StatusPill } from '../../components/StatusPill';
import { statusLabel } from '../../components/statusLabels';
import { WidgetLoading } from '../../components/WidgetLoading';
import {
  getMuscleMap,
  getWorkout,
  type DayOfWeek,
  type MuscleWorked,
  type SessionStatus,
  type TrainingSession,
  type WorkoutItem,
} from '../../api/training';
import {
  groupMusclesForDisplay,
  muscleSummary,
  type MuscleGroupDisplay,
} from '../trainingMuscleLabels';
import { overlayFromMuscleMap, roleForLoad, viewForMuscle } from '../trainingMuscleOverlay';
import { displayTitle, prescriptionSummary } from '../trainingPrescription';
import { DAY_LABELS, KIND_LABELS, WEEK_ORDER } from './trainingLabels';
import styles from './SessionDetailModal.module.css';

/**
 * Which session the detail dialog is showing, and the day it was opened from.
 *
 * <p>The day travels with the session because the dialog can move it: the
 * "Mover a otro día" select needs to know where it currently sits, and the
 * session read model carries no day of its own.
 */
export interface DetailTarget {
  readonly dayOfWeek: string;
  readonly session: TrainingSession;
}

/**
 * The session detail dialog, redesigned in the "dos columnas" direction: the
 * muscle silhouettes and their legend on the left, the session's identity,
 * exercise breakdown and actions on the right. Design canvas:
 * `docs/design/modal-detalle-entrenamiento`.
 *
 * <p>Only a strength session earns the wide two-column panel — it is the one
 * that has a body to draw and a breakdown to list. A run keeps the 32rem
 * dialog every other modal uses, because widening a panel that holds three
 * lines of text just spreads them out.
 */
export function SessionDetailModal({
  target,
  onClose,
  mark,
  move,
  pending,
  anatomySex,
}: {
  readonly target: DetailTarget;
  readonly onClose: () => void;
  readonly mark: (id: string, status: SessionStatus) => void;
  readonly move: (id: string, day: DayOfWeek) => void;
  readonly pending: boolean;
  readonly anatomySex: AnatomySex;
}) {
  const { dayOfWeek, session } = target;
  const strength = session.kind === 'STRENGTH';

  return (
    <Modal
      title={`${DAY_LABELS[dayOfWeek] ?? dayOfWeek} · ${KIND_LABELS[session.kind]}`}
      onClose={onClose}
      size={strength ? 'lg' : 'md'}
    >
      <div className={strength ? styles.detailLayout : styles.detailSingle}>
        {strength && <MuscleMapSection sessionId={session.id} sex={anatomySex} />}

        <div className={styles.detailMain}>
          {/* The dialog title already carries the day and the kind ("Martes ·
              Fuerza"), so the body prints only what it does not: which strength
              session this is, its status, and how many exercises it holds. */}
          <div className={styles.detailHeading}>
            <p className={styles.detailTitle}>{displayTitle(session.title)}</p>
            <div className={styles.detailMeta}>
              <StatusPill kind="training" value={session.status} />
              <span className={styles.sessionDetail}>{session.detail}</span>
            </div>
          </div>

          {session.notes && <p className={styles.notes}>{session.notes}</p>}

          {strength && <ExerciseBreakdown workoutType={session.workoutType} />}

          {/* Sticky: the panel caps at 90vh and a five-exercise breakdown
              scrolls past it on a phone, so the two actions the dialog exists
              for stay within a thumb's reach instead of below the fold. */}
          <div className={styles.detailFooter}>
            {/* Moving one session is the whole primitive: swapping two days is two
                moves and reordering the week is several, so there is no rule here
                for a session pushed past Sunday — the user decides where each one
                lands. The override lasts this week only. */}
            <label className={styles.detailMoveField}>
              <span className={styles.moveLabel}>Mover a otro día</span>
              <select
                className={styles.moveSelect}
                value={dayOfWeek}
                disabled={pending}
                onChange={(event) => move(session.id, event.target.value as DayOfWeek)}
              >
                {WEEK_ORDER.map((day) => (
                  <option key={day} value={day}>
                    {DAY_LABELS[day] ?? day}
                  </option>
                ))}
              </select>
            </label>
            {/* "Saltar" first, "Completar" last: the row is right-aligned, so
                DOM order puts the primary action nearest the corner the thumb
                and the eye both end on — and tab order reaches it last. */}
            <div className={styles.actions}>
              {session.status !== 'SKIPPED' && (
                <Button
                  type="button"
                  variant="secondary"
                  disabled={pending}
                  onClick={() => mark(session.id, 'SKIPPED')}
                >
                  Saltar
                </Button>
              )}
              {session.status !== 'COMPLETED' && (
                <Button
                  type="button"
                  disabled={pending}
                  loading={pending}
                  onClick={() => mark(session.id, 'COMPLETED')}
                >
                  Completar
                </Button>
              )}
            </div>
          </div>
        </div>
      </div>
    </Modal>
  );
}

/** A legend group set, split by the sheet that draws it. */
interface MusclesByView {
  readonly front: readonly MuscleGroupDisplay[];
  readonly back: readonly MuscleGroupDisplay[];
  readonly unplaced: readonly MuscleGroupDisplay[];
}

/**
 * Partitions the display groups into the two silhouettes' columns, keeping the
 * groups the pack cannot place in a third bucket rather than forcing them into
 * a column — a muscle listed under the wrong body is worse than one listed
 * under neither. Preserves the incoming order inside each bucket, which is the
 * catalog's own first-appearance order.
 */
function splitMusclesByView(muscles: readonly MuscleGroupDisplay[]): MusclesByView {
  const front: MuscleGroupDisplay[] = [];
  const back: MuscleGroupDisplay[] = [];
  const unplaced: MuscleGroupDisplay[] = [];

  for (const muscle of muscles) {
    const view = viewForMuscle(muscle.canonical);
    if (view === 'front') front.push(muscle);
    else if (view === 'back') back.push(muscle);
    else unplaced.push(muscle);
  }

  return { front, back, unplaced };
}

/**
 * One labelled column of the legend. Renders nothing when the session works
 * none of that sheet's muscles: a "Frente" heading over an empty column is
 * noise, and the asymmetry itself says something true (a pull day is mostly
 * back).
 */
function MuscleLegendColumn({
  title,
  muscles,
}: {
  readonly title: string;
  readonly muscles: readonly MuscleGroupDisplay[];
}) {
  if (muscles.length === 0) return null;

  return (
    <div className={styles.muscleLegendColumn}>
      {/* The columns are labelled on purpose: two unlabelled ones read as a
          list that happened to wrap, which teaches the reader nothing about
          why a muscle sits where it does. */}
      <h4 className={styles.muscleLegendTitle}>{title}</h4>
      <ul className={styles.muscleLegendList} aria-label={title}>
        {muscles.map((muscle) => (
          <MuscleLegendItem key={muscle.canonical} muscle={muscle} />
        ))}
      </ul>
    </div>
  );
}

/** One legend row: the swatch that keys the picture, the name and the load. */
function MuscleLegendItem({ muscle }: { readonly muscle: MuscleGroupDisplay }) {
  return (
    <li className={styles.muscleLegendItem}>
      <span className={styles.muscleDot} data-role={roleForLoad(muscle.load)} aria-hidden="true" />
      <span className={styles.muscleLegendText}>
        <span className={styles.muscleLegendName}>{muscle.label}</span>
        <span className={styles.muscleLegendLoad}>{statusLabel('muscleLoad', muscle.load)}</span>
      </span>
    </li>
  );
}

type WorkoutState =
  | { readonly status: 'unavailable' }
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | { readonly status: 'ready'; readonly items: readonly WorkoutItem[] };

const WORKOUT_ERROR = 'No se pudieron cargar los ejercicios de esta sesión.';
const WORKOUT_UNAVAILABLE = 'Este entrenamiento no tiene desglose por ejercicio.';

/**
 * The real exercise prescription for a strength session, replacing the notice
 * that used to apologise for its absence.
 *
 * <p>That notice ("la API no expone las plantillas de fuerza por HTTP") had
 * been false since `GET /api/v1/training/workouts/{type}` shipped — the
 * "Entrenar" page has been calling it all along. This section is the same
 * endpoint, summarised: one line per exercise instead of an editable set table,
 * because this dialog is for deciding whether to do the session, not for
 * logging it.
 *
 * <p>Fetched independently of the muscle map so neither request can take the
 * other's section down, matching the page's existing "error, prior state
 * preserved" pattern.
 */
function ExerciseBreakdown({ workoutType }: { readonly workoutType?: string }) {
  const [state, setState] = useState<WorkoutState>({ status: 'loading' });

  useEffect(() => {
    if (!workoutType) {
      // Not an error: the read model marks some strength sessions as having no
      // template to look up, and a spinner that never resolves is worse than
      // saying so.
      setState({ status: 'unavailable' });
      return;
    }
    let cancelled = false;
    setState({ status: 'loading' });
    getWorkout(workoutType)
      .then((workout) => {
        if (!cancelled) setState({ status: 'ready', items: workout.items });
      })
      .catch(() => {
        if (!cancelled) setState({ status: 'error' });
      });
    return () => {
      cancelled = true;
    };
  }, [workoutType]);

  return (
    <div className={styles.exerciseSection}>
      {state.status === 'loading' && <WidgetLoading label="Cargando ejercicios…" rows={3} />}
      {state.status === 'error' && (
        <p className={styles.detailError} role="alert">
          {WORKOUT_ERROR}
        </p>
      )}
      {state.status === 'unavailable' && <p className={styles.message}>{WORKOUT_UNAVAILABLE}</p>}
      {state.status === 'ready' &&
        (state.items.length === 0 ? (
          <p className={styles.message}>{WORKOUT_UNAVAILABLE}</p>
        ) : (
          /* No visible heading: the session's "5 ejercicios" subtitle already
             names this list. The label keeps it announced all the same. */
          <ol className={styles.exerciseList} aria-label="Ejercicios">
            {state.items.map((item, index) => (
              <li key={item.exerciseId} className={styles.exerciseItem}>
                <span className={styles.exerciseOrder} aria-hidden="true">
                  {index + 1}
                </span>
                <span className={styles.exerciseLines}>
                  <span className={styles.exerciseName}>{item.exerciseName}</span>
                  <span className={styles.exercisePrescription}>{prescriptionSummary(item)}</span>
                </span>
              </li>
            ))}
          </ol>
        ))}
    </div>
  );
}

type MuscleMapState =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | {
      readonly status: 'ready';
      readonly muscles: readonly MuscleGroupDisplay[];
      readonly raw: readonly MuscleWorked[];
    };

const MUSCLE_MAP_ERROR = 'No se pudieron cargar los músculos trabajados.';

/**
 * The FOR-136 worked-muscle heatmap for a strength session (spec FOR-53: "now
 * backed by FOR-136 — wire the heatmap to the muscle-map endpoint"), drawn as
 * the two anatomical sheets with the worked muscles lit rather than as a row of
 * chips. A push session works the triceps, which live on the back sheet, so
 * both views are shown: one sheet would silently drop muscles the legend names.
 *
 * <p>The legend is not decoration — it is the key to the picture, so each
 * swatch takes its emphasis from {@link roleForLoad}, the same rule that drives
 * the overlay's opacity. A legend that disagreed with the body beside it would
 * be worse than none.
 *
 * <p>Fetched on demand per session (the endpoint is per-session, not part of
 * the FOR-26 week payload) and normalized for display via
 * `trainingMuscleLabels` (frontend-owned grouping — the backend read model
 * itself stays untouched). A load failure here is scoped to this section only:
 * the rest of the session detail (status, breakdown, actions) keeps working,
 * matching the page's existing "error, prior state preserved" pattern.
 */
function MuscleMapSection({
  sessionId,
  sex,
}: {
  readonly sessionId: string;
  readonly sex: AnatomySex;
}) {
  const [state, setState] = useState<MuscleMapState>({ status: 'loading' });

  useEffect(() => {
    let cancelled = false;
    setState({ status: 'loading' });
    getMuscleMap(sessionId)
      .then((map) => {
        if (!cancelled) {
          setState({
            status: 'ready',
            muscles: groupMusclesForDisplay(map.muscles),
            raw: map.muscles,
          });
        }
      })
      .catch(() => {
        if (!cancelled) {
          setState({ status: 'error' });
        }
      });
    return () => {
      cancelled = true;
    };
  }, [sessionId]);

  const worked = state.status === 'ready' && state.muscles.length > 0;
  const byView = splitMusclesByView(state.status === 'ready' ? state.muscles : []);

  return (
    <aside className={styles.muscleColumn}>
      <h3 className={styles.detailSectionTitle}>Músculos trabajados</h3>
      {state.status === 'loading' && (
        <WidgetLoading label="Cargando músculos trabajados…" rows={2} />
      )}
      {state.status === 'error' && (
        <p className={styles.detailError} role="alert">
          {MUSCLE_MAP_ERROR}
        </p>
      )}
      {state.status === 'ready' && !worked && (
        <p className={styles.message}>Sin datos de músculos para esta sesión.</p>
      )}
      {state.status === 'ready' && worked && (
        <>
          <div className={styles.muscleFigures} role="img" aria-label={muscleSummary(state.raw)}>
            <MuscleSilhouette
              className={styles.muscleFigure}
              sex={sex}
              view="front"
              muscles={overlayFromMuscleMap(state.raw)}
            />
            <MuscleSilhouette
              className={styles.muscleFigure}
              sex={sex}
              view="back"
              muscles={overlayFromMuscleMap(state.raw)}
            />
          </div>
          <div className={styles.muscleLegend}>
            <MuscleLegendColumn title="Frente" muscles={byView.front} />
            <MuscleLegendColumn title="Espalda" muscles={byView.back} />
            {/* Worked, but on neither sheet — see `viewForMuscle`. Listed
                across the full width rather than dropped, because the muscle is
                still part of the session even when the pack cannot draw it. */}
            {byView.unplaced.length > 0 && (
              <ul className={styles.muscleLegendUnplaced}>
                {byView.unplaced.map((muscle) => (
                  <MuscleLegendItem key={muscle.canonical} muscle={muscle} />
                ))}
              </ul>
            )}
          </div>
        </>
      )}
    </aside>
  );
}
