import { useCallback, useEffect, useRef, useState, type FormEvent } from 'react';
import { Button } from '../../components/Button';
import { Card } from '../../components/Card';
import { ErrorState } from '../../components/ErrorState';
import { LoadingState } from '../../components/LoadingState';
import { Modal } from '../../components/Modal';
import { SelectField, TextField } from '../../components/FormField';
import { useNotify } from '../../components/NotificationProvider';
import { ThemeToggle } from '../../components/ThemeToggle';
import { useTheme } from '../../theme/ThemeContext';
import { ApiRequestError, type ApiFieldError } from '../../api/client';
import {
  getProfile,
  updateProfileFields,
  type ActivityLevel,
  type MainGoal,
  type Sex,
  type UserProfile,
} from '../../api/profile';
import styles from './ProfileSection.module.css';

const THEME_LABELS = { light: 'Claro', dark: 'Oscuro' } as const;

const NOT_SPECIFIED = 'No especificado';
const PROFILE_LOAD_ERROR = 'No se pudo cargar tu perfil. Inténtalo de nuevo.';
const PROFILE_SAVE_ERROR = 'No se pudo guardar tu perfil. Inténtalo de nuevo.';
const HEIGHT_ERROR = 'Introduce una altura válida.';
const EMAIL_ERROR = 'Introduce un email válido.';
const EMAIL_PATTERN = /\S+@\S+\.\S+/;

const SEX_LABELS: Record<Sex, string> = { MALE: 'Masculino', FEMALE: 'Femenino', OTHER: 'Otro' };
const ACTIVITY_LABELS: Record<ActivityLevel, string> = {
  SEDENTARY: 'Sedentario',
  LIGHT: 'Ligero',
  MODERATE: 'Moderado',
  ACTIVE: 'Activo',
  VERY_ACTIVE: 'Muy activo',
};
const GOAL_LABELS: Record<MainGoal, string> = {
  COMPOSICION: 'Composición corporal',
  RENDIMIENTO: 'Rendimiento',
  HABITO: 'Hábito',
};

function themeDescription(
  mode: 'light' | 'dark' | 'system',
  resolvedTheme: 'light' | 'dark',
): string {
  if (mode === 'system') {
    return `Sistema (${THEME_LABELS[resolvedTheme]})`;
  }
  return THEME_LABELS[mode];
}

/** First two initials of a display name; falls back to "?" when no name is set. */
function initialsOf(name: string | undefined): string {
  const trimmed = name?.trim();
  if (!trimmed) {
    return '?';
  }
  return trimmed
    .split(/\s+/)
    .slice(0, 2)
    .map((word) => word[0]?.toUpperCase() ?? '')
    .join('');
}

type State =
  | { readonly status: 'loading' }
  | { readonly status: 'error'; readonly detail?: string }
  | { readonly status: 'ready'; readonly profile: UserProfile };

/**
 * Personal profile summary (FOR-58 FR: "avatar, name, email, birthdate, sex,
 * height, activity level, main goal; 'Editar perfil' entry point"). FOR-119:
 * loads the real profile from {@code GET /api/v1/profile} (FOR-107) instead
 * of the removed {@code MOCK_PROFILE} fixture, and "Editar perfil" opens a
 * real, working edit form that persists through {@code PATCH /api/v1/profile}
 * -- the "Próximamente" badge/disabled state and `edit-profile-hint` copy
 * this section shipped with under FOR-58 are gone (spec FOR-119 Common
 * Pitfall: "leaving the 'Próximamente' badge/disabled state in place after
 * wiring real data").
 *
 * <p>The "Tema" row remains FOR-62's working {@link ThemeToggle} backed by
 * {@link useTheme} -- unchanged by this story (theme persistence through the
 * backend is FOR-120's scope, not this one's).
 */
export function ProfileSection() {
  const { mode, resolvedTheme } = useTheme();
  const [state, setState] = useState<State>({ status: 'loading' });
  const [editing, setEditing] = useState(false);
  // Guards against a late-resolving fetch calling setState after unmount
  // (mirrors ShoppingPage's ProductEditModal `active` flag guard, FOR-113).
  const mountedRef = useRef(true);
  useEffect(
    () => () => {
      mountedRef.current = false;
    },
    [],
  );

  const load = useCallback(() => {
    setState({ status: 'loading' });
    getProfile()
      .then((profile) => {
        if (mountedRef.current) {
          setState({ status: 'ready', profile });
        }
      })
      .catch((error: unknown) => {
        if (mountedRef.current) {
          setState({
            status: 'error',
            detail: error instanceof ApiRequestError ? error.message : undefined,
          });
        }
      });
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  return (
    <Card
      title="Perfil y preferencias"
      headingLevel={2}
      action={
        state.status === 'ready' ? (
          <Button variant="primary" type="button" onClick={() => setEditing(true)}>
            Editar perfil
          </Button>
        ) : undefined
      }
    >
      {state.status === 'loading' && <LoadingState message="Cargando tu perfil…" />}
      {state.status === 'error' && (
        <ErrorState
          message={PROFILE_LOAD_ERROR}
          onRetry={load}
          detail={state.detail}
          showDetail={import.meta.env.DEV}
        />
      )}
      {state.status === 'ready' && (
        <>
          <div className={styles.identity}>
            <span className={styles.avatar} aria-hidden="true">
              {initialsOf(state.profile.name)}
            </span>
            <div>
              <p className={styles.name}>{state.profile.name || NOT_SPECIFIED}</p>
              <p className={styles.email}>{state.profile.email || NOT_SPECIFIED}</p>
            </div>
          </div>

          <dl className={styles.fields}>
            <div className={styles.field}>
              <dt>Fecha de nacimiento</dt>
              <dd>{state.profile.birthDate ?? NOT_SPECIFIED}</dd>
            </div>
            <div className={styles.field}>
              <dt>Sexo</dt>
              <dd>{state.profile.sex ? SEX_LABELS[state.profile.sex] : NOT_SPECIFIED}</dd>
            </div>
            <div className={styles.field}>
              <dt>Altura</dt>
              <dd>
                {state.profile.heightCm != null ? `${state.profile.heightCm} cm` : NOT_SPECIFIED}
              </dd>
            </div>
            <div className={styles.field}>
              <dt>Nivel de actividad</dt>
              <dd>
                {state.profile.activityLevel
                  ? ACTIVITY_LABELS[state.profile.activityLevel]
                  : NOT_SPECIFIED}
              </dd>
            </div>
            <div className={styles.field}>
              <dt>Objetivo principal</dt>
              <dd className={styles.goal}>
                {state.profile.mainGoal ? GOAL_LABELS[state.profile.mainGoal] : NOT_SPECIFIED}
              </dd>
            </div>
          </dl>

          <div className={styles.themeRow}>
            <div>
              <span className={styles.label}>Tema</span>
              <span className={styles.description}>{themeDescription(mode, resolvedTheme)}</span>
            </div>
            <ThemeToggle />
          </div>
        </>
      )}

      {editing && state.status === 'ready' && (
        <ProfileEditModal
          profile={state.profile}
          onClose={() => setEditing(false)}
          onSaved={(updated) => {
            setState({ status: 'ready', profile: updated });
            setEditing(false);
          }}
        />
      )}
    </Card>
  );
}

type FieldKey = 'name' | 'email' | 'birthDate' | 'sex' | 'heightCm' | 'activityLevel' | 'mainGoal';
type FieldErrors = Partial<Record<FieldKey, string>>;

const FIELD_KEYS: readonly FieldKey[] = [
  'name',
  'email',
  'birthDate',
  'sex',
  'heightCm',
  'activityLevel',
  'mainGoal',
];

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

interface ProfileEditModalProps {
  readonly profile: UserProfile;
  readonly onClose: () => void;
  readonly onSaved: (profile: UserProfile) => void;
}

/**
 * "Editar perfil" form (FOR-119): pre-filled with the current profile,
 * persists through {@code PATCH /api/v1/profile}. Frontend validation is
 * limited to basic required-field/format checks (email shape, a positive
 * height) -- authoritative validation stays server-side (ADR-006,
 * Non-Functional Requirements: "no client-side validation logic duplicating
 * backend rules"). Backend validation failures are mapped field-by-field
 * from {@link ApiRequestError.details} onto the offending input (ADR-006:
 * "forms must display validation errors close to fields"), and the form
 * never resets on a failed save, so the user's other entered values survive
 * (spec FOR-119 Edge Cases).
 */
function ProfileEditModal({ profile, onClose, onSaved }: ProfileEditModalProps) {
  const notify = useNotify();
  const [name, setName] = useState(profile.name ?? '');
  const [email, setEmail] = useState(profile.email ?? '');
  const [birthDate, setBirthDate] = useState(profile.birthDate ?? '');
  const [sex, setSex] = useState<Sex | ''>(profile.sex ?? '');
  const [heightCm, setHeightCm] = useState(
    profile.heightCm != null ? String(profile.heightCm) : '',
  );
  const [activityLevel, setActivityLevel] = useState<ActivityLevel | ''>(
    profile.activityLevel ?? '',
  );
  const [mainGoal, setMainGoal] = useState<MainGoal | ''>(profile.mainGoal ?? '');
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const [saveError, setSaveError] = useState<string | undefined>(undefined);
  const [saving, setSaving] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (saving) {
      return;
    }

    const nextErrors: FieldErrors = {};
    const trimmedEmail = email.trim();
    if (trimmedEmail && !EMAIL_PATTERN.test(trimmedEmail)) {
      nextErrors.email = EMAIL_ERROR;
    }

    let heightValue: number | undefined;
    if (heightCm.trim()) {
      heightValue = Number(heightCm);
      if (!Number.isFinite(heightValue) || heightValue <= 0) {
        nextErrors.heightCm = HEIGHT_ERROR;
      }
    }

    setFieldErrors(nextErrors);
    setSaveError(undefined);
    if (Object.keys(nextErrors).length > 0) {
      return;
    }

    setSaving(true);
    try {
      const updated = await updateProfileFields({
        name: name.trim() || undefined,
        email: trimmedEmail || undefined,
        birthDate: birthDate || undefined,
        sex: sex || undefined,
        heightCm: heightValue,
        activityLevel: activityLevel || undefined,
        mainGoal: mainGoal || undefined,
      });
      onSaved(updated);
      notify.success('Perfil actualizado.');
    } catch (error) {
      if (error instanceof ApiRequestError && error.details && error.details.length > 0) {
        const mapped = mapFieldErrors(error.details);
        if (Object.keys(mapped).length > 0) {
          setFieldErrors(mapped);
        } else {
          setSaveError(error.message);
        }
      } else {
        setSaveError(error instanceof ApiRequestError ? error.message : PROFILE_SAVE_ERROR);
      }
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal title="Editar perfil" onClose={onClose}>
      <form className={styles.editForm} onSubmit={handleSubmit} noValidate>
        <TextField
          id="profile-name"
          label="Nombre"
          value={name}
          disabled={saving}
          onChange={(event) => setName(event.target.value)}
        />
        <TextField
          id="profile-email"
          label="Email"
          type="email"
          value={email}
          error={fieldErrors.email}
          disabled={saving}
          onChange={(event) => setEmail(event.target.value)}
        />
        <TextField
          id="profile-birthDate"
          label="Fecha de nacimiento"
          type="date"
          value={birthDate}
          error={fieldErrors.birthDate}
          disabled={saving}
          onChange={(event) => setBirthDate(event.target.value)}
        />
        <SelectField
          id="profile-sex"
          label="Sexo"
          value={sex}
          error={fieldErrors.sex}
          disabled={saving}
          onChange={(event) => setSex(event.target.value as Sex | '')}
        >
          <option value="">{NOT_SPECIFIED}</option>
          <option value="MALE">{SEX_LABELS.MALE}</option>
          <option value="FEMALE">{SEX_LABELS.FEMALE}</option>
          <option value="OTHER">{SEX_LABELS.OTHER}</option>
        </SelectField>
        <TextField
          id="profile-heightCm"
          label="Altura (cm)"
          type="number"
          step="0.1"
          value={heightCm}
          error={fieldErrors.heightCm}
          disabled={saving}
          onChange={(event) => setHeightCm(event.target.value)}
        />
        <SelectField
          id="profile-activityLevel"
          label="Nivel de actividad"
          value={activityLevel}
          error={fieldErrors.activityLevel}
          disabled={saving}
          onChange={(event) => setActivityLevel(event.target.value as ActivityLevel | '')}
        >
          <option value="">{NOT_SPECIFIED}</option>
          <option value="SEDENTARY">{ACTIVITY_LABELS.SEDENTARY}</option>
          <option value="LIGHT">{ACTIVITY_LABELS.LIGHT}</option>
          <option value="MODERATE">{ACTIVITY_LABELS.MODERATE}</option>
          <option value="ACTIVE">{ACTIVITY_LABELS.ACTIVE}</option>
          <option value="VERY_ACTIVE">{ACTIVITY_LABELS.VERY_ACTIVE}</option>
        </SelectField>
        <SelectField
          id="profile-mainGoal"
          label="Objetivo principal"
          value={mainGoal}
          error={fieldErrors.mainGoal}
          disabled={saving}
          onChange={(event) => setMainGoal(event.target.value as MainGoal | '')}
        >
          <option value="">{NOT_SPECIFIED}</option>
          <option value="COMPOSICION">{GOAL_LABELS.COMPOSICION}</option>
          <option value="RENDIMIENTO">{GOAL_LABELS.RENDIMIENTO}</option>
          <option value="HABITO">{GOAL_LABELS.HABITO}</option>
        </SelectField>

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
            Guardar
          </Button>
        </div>
      </form>
    </Modal>
  );
}
