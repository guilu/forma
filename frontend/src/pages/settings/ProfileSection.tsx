import { Badge } from '../../components/Badge';
import { Button } from '../../components/Button';
import { Card } from '../../components/Card';
import { MOCK_PROFILE } from './profileData';
import styles from './ProfileSection.module.css';

/**
 * Personal profile summary (FOR-58 FR: "avatar, name, email, birthdate, sex,
 * height, activity level, main goal; 'Editar perfil' entry point"). Reads a
 * static {@link MOCK_PROFILE} fixture — there is no profile backend yet
 * (ADR-002, single-user MVP) — so every field here is read-only display, never
 * an editable form (spec FOR-58 Common Pitfall: "building profile persistence
 * with no backend").
 *
 * <p>"Editar perfil" is a real, visible entry point but stays a disabled
 * `Button` (dimmed via the shared `Button:disabled` style) with an adjacent
 * "Próximamente" badge, so it never looks like a working action (spec FOR-58
 * edge case).
 *
 * <p>The "Tema" row is FOR-62's placeholder entry point — also inert.
 */
export function ProfileSection() {
  return (
    <Card
      title="Perfil y preferencias"
      action={
        <div className={styles.editAction}>
          <Badge tone="neutral">Próximamente</Badge>
          <Button variant="primary" type="button" disabled aria-describedby="edit-profile-hint">
            Editar perfil
          </Button>
        </div>
      }
    >
      <p id="edit-profile-hint" className={styles.hint}>
        La edición de perfil estará disponible cuando exista un backend de perfil.
      </p>

      <div className={styles.identity}>
        <span className={styles.avatar} aria-hidden="true">
          {MOCK_PROFILE.initials}
        </span>
        <div>
          <p className={styles.name}>{MOCK_PROFILE.name}</p>
          <p className={styles.email}>{MOCK_PROFILE.email}</p>
        </div>
      </div>

      <dl className={styles.fields}>
        <div className={styles.field}>
          <dt>Fecha de nacimiento</dt>
          <dd>{MOCK_PROFILE.birthDate}</dd>
        </div>
        <div className={styles.field}>
          <dt>Sexo</dt>
          <dd>{MOCK_PROFILE.sex}</dd>
        </div>
        <div className={styles.field}>
          <dt>Altura</dt>
          <dd>{MOCK_PROFILE.heightCm} cm</dd>
        </div>
        <div className={styles.field}>
          <dt>Nivel de actividad</dt>
          <dd>{MOCK_PROFILE.activityLevel}</dd>
        </div>
        <div className={styles.field}>
          {/* Read-only summary — FOR-58 spec: goals ("Objetivos") have no
              dedicated owning story/backend yet; documented gap, not built here. */}
          <dt>Objetivo principal</dt>
          <dd className={styles.goal}>{MOCK_PROFILE.mainGoal}</dd>
        </div>
      </dl>

      <div className={styles.themeRow}>
        <div>
          <span className={styles.label}>Tema</span>
          <span className={styles.description}>Oscuro (predeterminado)</span>
        </div>
        <Badge tone="neutral">Próximamente</Badge>
      </div>
    </Card>
  );
}
