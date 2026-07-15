/**
 * Static display fixtures for the parts of the Ajustes screen not yet backed
 * by their own API (FOR-58).
 *
 * <p>FOR-119: the profile summary and units preference fixtures
 * (`MOCK_PROFILE`/`UNIT_PREFERENCES`) that used to live here were removed —
 * `ProfileSection`/`UnitsSection` now read the real FOR-107
 * {@code GET /api/v1/profile} response through `frontend/src/api/profile.ts`
 * instead. The remaining fixtures below (`DEFAULT_OBJECTIVES`,
 * `NOTIFICATION_PREFERENCES`, `SECURITY_ACTIONS`, `APP_VERSION`) are still
 * genuinely backend-less/out of this story's scope, so they stay static
 * fixtures, clearly documented as such.
 */

/**
 * Default objectives (spec FOR-58 FR: "déficit calórico, proteínas, agua
 * diaria — entry points"). Out of FOR-119's scope (spec's Data Model Notes:
 * "`DEFAULT_OBJECTIVES` stays with `ObjectivesSection` unless that section is
 * also touched here for the training/nutrition entry points" — FOR-119
 * resolved the training/nutrition bullet with its own distinct
 * `TrainingNutritionSection` instead of touching this one).
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
