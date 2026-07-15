import { useEffect, useState, type FormEvent } from 'react';
import { Button } from '../components/Button';
import { Card } from '../components/Card';
import { EmptyState } from '../components/EmptyState';
import { ErrorState } from '../components/ErrorState';
import { Icon } from '../components/Icon';
import { LoadingState } from '../components/LoadingState';
import { Modal } from '../components/Modal';
import { useNotify } from '../components/NotificationProvider';
import { StatusPill } from '../components/StatusPill';
import { SelectField, TextField } from '../components/FormField';
import { ApiRequestError, type ApiFieldError } from '../api/client';
import {
  createGoal,
  listGoals,
  updateGoal,
  type CreateGoalInput,
  type Goal,
  type GoalMetric,
  type GoalStatus,
  type Milestone,
} from '../api/goals';
import { ProgressBar } from './dashboard/ProgressBar';
import { formatDueDate, formatMetricValue, metricLabel, statusLabel } from './goalsDisplay';
import styles from './GoalsPage.module.css';

/**
 * Objetivos screen (FOR-122): replaces the `PagePlaceholder` this route
 * rendered since FOR-81. Lists goals with derived progress + milestones from
 * the FOR-125 backend (first slice of FOR-104), and lets the user create a
 * goal or edit one's fields; milestone completion toggles inline (the only
 * milestone mutation FOR-125's PATCH supports — no add/remove/rename via this
 * slice, spec FOR-125 Open Questions).
 *
 * <p><b>Scope note (spec FOR-122 Data Model Notes / Open Questions):</b> the
 * spec assumed a full "FOR-104" domain and flagged this as its single largest
 * open dependency; the real backend that landed is FOR-125, the first of six
 * planned FOR-104 slices, covering only body-composition goals (`BODY_FAT_PCT`
 * /`WEIGHT_KG`/`LEAN_MASS_KG`) with a minimal ACTIVE/ACHIEVED/ARCHIVED status
 * and user-set milestone completion. There is no separate
 * `GET /api/v1/goals/{id}` — the list response already carries everything a
 * detail view would show — so "detail" is a {@link Modal} over the
 * already-fetched goal (edit form doubles as detail) rather than a dedicated
 * sub-route, and no adherence/streaks/achievements/categories-breakdown data
 * exists to back the fuller `docs/7-objetivos.png` mockup (those are later
 * FOR-104 slices); this screen renders only what FOR-125 actually returns,
 * never a fabricated stat.
 *
 * <p>Progress is never computed client-side (architecture-overview.md,
 * spec NFR): {@link ProgressBar} is fed `progress.ratio` directly (already
 * divided by the backend), not raw `current`/`target` — passing `max={1}`
 * means the component's own `value/max` arithmetic reproduces the exact
 * backend ratio rather than deriving a new one.
 */
type State =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | { readonly status: 'ready'; readonly goals: readonly Goal[] };

const LOAD_ERROR = 'No se pudieron cargar tus objetivos. Inténtalo de nuevo más tarde.';
const MILESTONE_ERROR = 'No se pudo actualizar el hito. Inténtalo de nuevo.';

export function GoalsPage() {
  const notify = useNotify();
  const [state, setState] = useState<State>({ status: 'loading' });
  const [retryToken, setRetryToken] = useState(0);
  const [actionError, setActionError] = useState<string | undefined>(undefined);
  const [milestonePendingId, setMilestonePendingId] = useState<string | undefined>(undefined);
  const [creating, setCreating] = useState(false);
  const [editingGoal, setEditingGoal] = useState<Goal | undefined>(undefined);

  useEffect(() => {
    let active = true;
    setState({ status: 'loading' });
    listGoals()
      .then((goals) => {
        if (active) {
          setState({ status: 'ready', goals });
        }
      })
      .catch(() => {
        if (active) {
          setState({ status: 'error' });
        }
      });
    return () => {
      active = false;
    };
  }, [retryToken]);

  async function toggleMilestone(goal: Goal, milestone: Milestone) {
    setActionError(undefined);
    setMilestonePendingId(milestone.id);
    try {
      const updated = await updateGoal(goal.id, {
        milestones: [{ id: milestone.id, completed: !milestone.completed }],
      });
      applyUpdatedGoal(updated);
      notify.success('Hito actualizado.');
    } catch (error) {
      setActionError(error instanceof ApiRequestError ? error.message : MILESTONE_ERROR);
    } finally {
      setMilestonePendingId(undefined);
    }
  }

  function applyUpdatedGoal(updated: Goal) {
    setState((current) =>
      current.status === 'ready'
        ? { status: 'ready', goals: current.goals.map((g) => (g.id === updated.id ? updated : g)) }
        : current,
    );
  }

  function applyCreatedGoal(created: Goal) {
    setState((current) =>
      current.status === 'ready'
        ? { status: 'ready', goals: [...current.goals, created] }
        : current,
    );
  }

  return (
    <div className={styles.wrapper}>
      <header className={styles.header}>
        <div className={styles.headerText}>
          <h1 className={styles.title}>Objetivos</h1>
          <p className={styles.subtitle}>Define tus metas y sigue tu progreso.</p>
        </div>
        {state.status === 'ready' && (
          <Button variant="primary" type="button" onClick={() => setCreating(true)}>
            Crear objetivo
          </Button>
        )}
      </header>
      {actionError && (
        <p className={styles.actionError} role="alert">
          {actionError}
        </p>
      )}
      {renderContent(
        state,
        () => setRetryToken((t) => t + 1),
        setEditingGoal,
        toggleMilestone,
        milestonePendingId,
        () => setCreating(true),
      )}
      {creating && (
        <GoalFormModal
          mode="create"
          onClose={() => setCreating(false)}
          onSaved={(goal) => {
            applyCreatedGoal(goal);
            setCreating(false);
            notify.success('Objetivo creado.');
          }}
        />
      )}
      {editingGoal && (
        <GoalFormModal
          mode="edit"
          goal={editingGoal}
          onClose={() => setEditingGoal(undefined)}
          onSaved={(goal) => {
            applyUpdatedGoal(goal);
            setEditingGoal(undefined);
            notify.success('Objetivo actualizado.');
          }}
        />
      )}
    </div>
  );
}

function renderContent(
  state: State,
  retry: () => void,
  onEdit: (goal: Goal) => void,
  onToggleMilestone: (goal: Goal, milestone: Milestone) => void,
  milestonePendingId: string | undefined,
  onCreate: () => void,
) {
  if (state.status === 'loading') {
    return <LoadingState message="Cargando tus objetivos…" />;
  }

  if (state.status === 'error') {
    return <ErrorState message={LOAD_ERROR} onRetry={retry} />;
  }

  if (state.goals.length === 0) {
    return (
      <EmptyState
        title="Aún no tienes objetivos."
        description="Crea tu primer objetivo para empezar a seguir tu progreso."
        action={
          <Button variant="primary" type="button" onClick={onCreate}>
            Crear objetivo
          </Button>
        }
      />
    );
  }

  return (
    <ul className={styles.goalList}>
      {state.goals.map((goal) => (
        <li key={goal.id}>
          <GoalCard
            goal={goal}
            onEdit={() => onEdit(goal)}
            onToggleMilestone={(milestone) => onToggleMilestone(goal, milestone)}
            milestonePendingId={milestonePendingId}
          />
        </li>
      ))}
    </ul>
  );
}

function GoalCard({
  goal,
  onEdit,
  onToggleMilestone,
  milestonePendingId,
}: {
  readonly goal: Goal;
  readonly onEdit: () => void;
  readonly onToggleMilestone: (milestone: Milestone) => void;
  readonly milestonePendingId: string | undefined;
}) {
  const { progress } = goal;

  return (
    <Card
      title={goal.title}
      headingLevel={2}
      action={
        <div className={styles.cardActions}>
          <StatusPill kind="goalStatus" value={goal.status} />
          <button
            type="button"
            className={styles.editButton}
            onClick={onEdit}
            aria-label={`Editar objetivo ${goal.title}`}
          >
            <Icon name="edit" size={16} />
          </button>
        </div>
      }
    >
      <p className={styles.metricLabel}>{metricLabel(goal.metric)}</p>

      {progress.current != null && progress.ratio != null ? (
        <>
          <ProgressBar value={progress.ratio} max={1} label={`Progreso de ${goal.title}`} />
          <p className={styles.progressValues}>
            {formatMetricValue(progress.current, goal.metric)} /{' '}
            {formatMetricValue(progress.target, goal.metric)}
          </p>
        </>
      ) : (
        <p className={styles.noProgress}>Sin datos de progreso todavía.</p>
      )}

      <p className={styles.dueDate}>Meta: {formatDueDate(goal.dueDate)}</p>

      {goal.milestones.length > 0 && (
        <ul className={styles.milestones}>
          {goal.milestones.map((milestone) => (
            <li key={milestone.id} className={styles.milestone}>
              <label className={styles.milestoneLabel}>
                <input
                  type="checkbox"
                  checked={milestone.completed}
                  disabled={milestonePendingId === milestone.id}
                  onChange={() => onToggleMilestone(milestone)}
                />
                <span className={milestone.completed ? styles.completedMilestone : undefined}>
                  {milestone.title}
                </span>
              </label>
              <span className={styles.milestoneTarget}>
                {formatMetricValue(milestone.target, goal.metric)}
              </span>
            </li>
          ))}
        </ul>
      )}
    </Card>
  );
}

const METRIC_OPTIONS: readonly GoalMetric[] = ['BODY_FAT_PCT', 'WEIGHT_KG', 'LEAN_MASS_KG'];
const STATUS_OPTIONS: readonly GoalStatus[] = ['ACTIVE', 'ACHIEVED', 'ARCHIVED'];

type FieldKey = 'title' | 'metric' | 'target' | 'dueDate' | 'status';
type FieldErrors = Partial<Record<FieldKey, string>>;
const FIELD_KEYS: readonly FieldKey[] = ['title', 'metric', 'target', 'dueDate', 'status'];

function isFieldKey(value: string): value is FieldKey {
  return (FIELD_KEYS as readonly string[]).includes(value);
}

/** Maps the backend's per-field validation errors onto this form's field ids. */
function mapFieldErrors(details: ReadonlyArray<ApiFieldError>): FieldErrors {
  const mapped: FieldErrors = {};
  for (const detail of details) {
    if (isFieldKey(detail.field)) {
      mapped[detail.field] = detail.message;
    }
  }
  return mapped;
}

interface MilestoneDraft {
  readonly title: string;
  readonly target: string;
}

type GoalFormModalProps =
  | {
      readonly mode: 'create';
      readonly onClose: () => void;
      readonly onSaved: (goal: Goal) => void;
    }
  | {
      readonly mode: 'edit';
      readonly goal: Goal;
      readonly onClose: () => void;
      readonly onSaved: (goal: Goal) => void;
    };

const TITLE_ERROR = 'Introduce un título.';
const METRIC_ERROR = 'Selecciona una métrica.';
const TARGET_ERROR = 'Introduce un valor numérico.';
const CREATE_ERROR = 'No se pudo crear el objetivo. Inténtalo de nuevo.';
const SAVE_ERROR = 'No se pudo guardar el objetivo. Inténtalo de nuevo.';

/**
 * Create/edit form for a goal (FOR-122 ui.md: "Edit form for a goal's
 * editable fields (pending FOR-104's exposed fields)"). Also stands in for
 * ui.md's "detail view" — there is no separate `GET /goals/{id}` to show
 * anything beyond what the already-fetched list row carries (file doc
 * comment) — so opening "Editar" both shows and edits a goal's full data.
 *
 * <p>Create accepts optional milestones (`CreateGoalRequest.milestones`);
 * edit does not expose milestone add/remove/rename fields, since FOR-125's
 * PATCH only supports toggling an existing milestone's `completed` state (by
 * id) — that toggle lives on {@link GoalCard} itself, not in this form, since
 * it applies with or without opening "Editar".
 */
function GoalFormModal(props: GoalFormModalProps) {
  const { mode, onClose, onSaved } = props;
  const existing = mode === 'edit' ? props.goal : undefined;

  const [title, setTitle] = useState(existing?.title ?? '');
  const [metric, setMetric] = useState<GoalMetric | ''>(existing?.metric ?? '');
  const [target, setTarget] = useState(existing ? String(existing.target) : '');
  const [dueDate, setDueDate] = useState(existing?.dueDate ?? '');
  const [status, setStatus] = useState<GoalStatus>(existing?.status ?? 'ACTIVE');
  const [milestoneDrafts, setMilestoneDrafts] = useState<readonly MilestoneDraft[]>([]);
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const [saveError, setSaveError] = useState<string | undefined>(undefined);
  const [saving, setSaving] = useState(false);

  function addMilestoneDraft() {
    setMilestoneDrafts((current) => [...current, { title: '', target: '' }]);
  }

  function removeMilestoneDraft(index: number) {
    setMilestoneDrafts((current) => current.filter((_, i) => i !== index));
  }

  function updateMilestoneDraft(index: number, field: keyof MilestoneDraft, value: string) {
    setMilestoneDrafts((current) =>
      current.map((draft, i) => (i === index ? { ...draft, [field]: value } : draft)),
    );
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (saving) {
      return;
    }

    const nextErrors: FieldErrors = {};
    if (!title.trim()) {
      nextErrors.title = TITLE_ERROR;
    }
    if (mode === 'create' && !metric) {
      nextErrors.metric = METRIC_ERROR;
    }
    const targetValue = Number(target);
    if (!target.trim() || !Number.isFinite(targetValue)) {
      nextErrors.target = TARGET_ERROR;
    }
    setFieldErrors(nextErrors);
    setSaveError(undefined);
    if (Object.keys(nextErrors).length > 0) {
      return;
    }

    setSaving(true);
    try {
      if (mode === 'create') {
        const validMilestones = milestoneDrafts
          .filter((draft) => draft.title.trim() && draft.target.trim())
          .map((draft) => ({ title: draft.title.trim(), target: Number(draft.target) }));
        const input: CreateGoalInput = {
          title: title.trim(),
          metric: metric as GoalMetric,
          target: targetValue,
          dueDate: dueDate || undefined,
          milestones: validMilestones.length > 0 ? validMilestones : undefined,
        };
        const created = await createGoal(input);
        onSaved(created);
      } else {
        const updated = await updateGoal(props.goal.id, {
          title: title.trim(),
          target: targetValue,
          dueDate: dueDate || undefined,
          status,
        });
        onSaved(updated);
      }
    } catch (error) {
      if (error instanceof ApiRequestError && error.details && error.details.length > 0) {
        const mapped = mapFieldErrors(error.details);
        if (Object.keys(mapped).length > 0) {
          setFieldErrors(mapped);
        } else {
          setSaveError(error.message);
        }
      } else {
        const fallback = mode === 'create' ? CREATE_ERROR : SAVE_ERROR;
        setSaveError(error instanceof ApiRequestError ? error.message : fallback);
      }
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal title={mode === 'create' ? 'Crear objetivo' : 'Editar objetivo'} onClose={onClose}>
      <form className={styles.form} onSubmit={handleSubmit} noValidate>
        <TextField
          id="goal-title"
          label="Título"
          value={title}
          error={fieldErrors.title}
          disabled={saving}
          onChange={(event) => setTitle(event.target.value)}
        />
        {mode === 'create' ? (
          <SelectField
            id="goal-metric"
            label="Métrica"
            value={metric}
            error={fieldErrors.metric}
            disabled={saving}
            onChange={(event) => setMetric(event.target.value as GoalMetric | '')}
          >
            <option value="">Selecciona una métrica</option>
            {METRIC_OPTIONS.map((option) => (
              <option key={option} value={option}>
                {metricLabel(option)}
              </option>
            ))}
          </SelectField>
        ) : (
          <p className={styles.readonlyMetric}>Métrica: {metricLabel(props.goal.metric)}</p>
        )}
        <TextField
          id="goal-target"
          label="Objetivo"
          type="number"
          step="0.1"
          value={target}
          error={fieldErrors.target}
          disabled={saving}
          onChange={(event) => setTarget(event.target.value)}
        />
        <TextField
          id="goal-dueDate"
          label="Fecha límite (opcional)"
          type="date"
          value={dueDate ?? ''}
          error={fieldErrors.dueDate}
          disabled={saving}
          onChange={(event) => setDueDate(event.target.value)}
        />
        {mode === 'edit' && (
          <SelectField
            id="goal-status"
            label="Estado"
            value={status}
            error={fieldErrors.status}
            disabled={saving}
            onChange={(event) => setStatus(event.target.value as GoalStatus)}
          >
            {STATUS_OPTIONS.map((option) => (
              <option key={option} value={option}>
                {statusLabel(option)}
              </option>
            ))}
          </SelectField>
        )}

        {mode === 'create' && (
          <fieldset className={styles.milestonesFieldset}>
            <legend>Hitos (opcional)</legend>
            {milestoneDrafts.map((draft, index) => (
              <div key={index} className={styles.milestoneRow}>
                <TextField
                  id={`goal-milestone-title-${index}`}
                  label="Título del hito"
                  value={draft.title}
                  disabled={saving}
                  onChange={(event) => updateMilestoneDraft(index, 'title', event.target.value)}
                />
                <TextField
                  id={`goal-milestone-target-${index}`}
                  label="Valor objetivo"
                  type="number"
                  step="0.1"
                  value={draft.target}
                  disabled={saving}
                  onChange={(event) => updateMilestoneDraft(index, 'target', event.target.value)}
                />
                <Button
                  type="button"
                  variant="ghost"
                  disabled={saving}
                  onClick={() => removeMilestoneDraft(index)}
                  aria-label={`Eliminar hito ${index + 1}`}
                >
                  <Icon name="cross" size={16} />
                </Button>
              </div>
            ))}
            <Button type="button" variant="secondary" disabled={saving} onClick={addMilestoneDraft}>
              Añadir hito
            </Button>
          </fieldset>
        )}

        {saveError && (
          <p className={styles.actionError} role="alert">
            {saveError}
          </p>
        )}

        <div className={styles.editActions}>
          <Button variant="secondary" type="button" onClick={onClose} disabled={saving}>
            Cancelar
          </Button>
          <Button type="submit" loading={saving}>
            {mode === 'create' ? 'Crear' : 'Guardar'}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
