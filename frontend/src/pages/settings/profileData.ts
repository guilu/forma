/**
 * Static profile fixture for the Ajustes screen (FOR-58).
 *
 * <p><b>No user/profile/preferences backend exists</b> (verified: no controller
 * under `backend/src/main/java/.../delivery/**`, ADR-002 single-user MVP). This
 * is display-only fixture data, not a read model from an API — every field is
 * clearly a placeholder until a profile backend story lands. Deliberately uses
 * generic values rather than a real person's name/email (AGENTS.md forbidden
 * shortcut: "hardcoding user-specific data outside fixtures or seed data").
 *
 * <p>`mainGoal` intentionally overlaps with the Objetivos screen
 * (`docs/7-objetivos.png`, no dedicated FOR-47 child story) — spec FOR-58 Data
 * Model Notes says to treat it as a read-only summary here and flag the
 * ownership gap (see PR description / FOR-58 Assumptions).
 */
export interface ProfileFixture {
  readonly name: string;
  readonly email: string;
  readonly initials: string;
  readonly birthDate: string;
  readonly sex: string;
  readonly heightCm: number;
  readonly activityLevel: string;
  readonly mainGoal: string;
}

export const MOCK_PROFILE: ProfileFixture = {
  name: 'Usuario FORMA',
  email: 'usuario@forma.app',
  initials: 'UF',
  birthDate: '12 mayo 1990',
  sex: 'No especificado',
  heightCm: 178,
  activityLevel: 'Moderado',
  mainGoal: 'Recomposición',
};

/** Units & locale preferences (spec FOR-58: "Units preference may drive display formatting locally"). */
export interface UnitPreference {
  readonly label: string;
  readonly value: string;
}

export const UNIT_PREFERENCES: readonly UnitPreference[] = [
  { label: 'Peso', value: 'Kilogramos (kg)' },
  { label: 'Altura', value: 'Centímetros (cm)' },
  { label: 'Distancia', value: 'Kilómetros (km)' },
  { label: 'Energía', value: 'Kilocalorías (kcal)' },
];

/**
 * Default objectives (spec FOR-58 FR: "déficit calórico, proteínas, agua
 * diaria — entry points"). Also stands in for the spec's separate "Training /
 * nutrition preference entry points" bullet — `ui.md`'s Components list has no
 * dedicated component for that bullet, so these nutrition defaults are the
 * closest existing grouping (documented assumption, FOR-58).
 */
export interface DefaultObjective {
  readonly label: string;
  readonly value: string;
}

export const DEFAULT_OBJECTIVES: readonly DefaultObjective[] = [
  { label: 'Déficit calórico diario', value: '300 kcal' },
  { label: 'Proteínas diarias', value: '160 g' },
  { label: 'Agua diaria', value: '2.5 L' },
];

/** Notification categories previewed here; real toggles/persistence belong to FOR-63. */
export interface NotificationPreference {
  readonly label: string;
  readonly description: string;
  readonly enabledByDefault: boolean;
}

export const NOTIFICATION_PREFERENCES: readonly NotificationPreference[] = [
  {
    label: 'Recordatorios de entrenamientos',
    description: 'Recibe notificaciones de tus entrenamientos programados.',
    enabledByDefault: true,
  },
  {
    label: 'Recordatorios de comidas',
    description: 'Recibe recordatorios para registrar tus comidas.',
    enabledByDefault: true,
  },
  {
    label: 'Objetivos y progreso',
    description: 'Recibe alertas cuando alcances tus objetivos.',
    enabledByDefault: true,
  },
  {
    label: 'Resumen semanal',
    description: 'Recibe un resumen de tu progreso cada domingo.',
    enabledByDefault: true,
  },
];

/** Security & data entry points — all inert until their backing flows exist. */
export interface SecurityAction {
  readonly label: string;
  readonly description: string;
  readonly destructive?: boolean;
}

export const SECURITY_ACTIONS: readonly SecurityAction[] = [
  { label: 'Cambiar contraseña', description: 'Actualiza la contraseña de tu cuenta.' },
  {
    label: 'Autenticación en dos pasos',
    description: 'Protege tu cuenta con 2FA.',
  },
  {
    label: 'Eliminar cuenta',
    description: 'Elimina tu cuenta y todos tus datos de forma permanente.',
    destructive: true,
  },
  {
    label: 'Exportar mis datos',
    description: 'Descarga una copia de todos tus datos.',
  },
  {
    label: 'Importar datos',
    description: 'Restaura tus datos desde un archivo.',
  },
];

/** Static "Acerca de" facts — no backend needed for these. */
export const APP_VERSION = '0.0.1';
