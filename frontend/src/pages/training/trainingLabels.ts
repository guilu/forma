/**
 * The day and kind names every training surface prints, in one place.
 *
 * <p>They used to live inside `TrainingPage`, which was fine while the page was
 * the only screen that named a session. The dashboard's training card opens the
 * same detail dialog now, so the labels — and the week's own order, which the
 * dialog's "move to another day" select walks — belong to neither of them.
 */
export const DAY_LABELS: Record<string, string> = {
  MONDAY: 'Lunes',
  TUESDAY: 'Martes',
  WEDNESDAY: 'Miércoles',
  THURSDAY: 'Jueves',
  FRIDAY: 'Viernes',
  SATURDAY: 'Sábado',
  SUNDAY: 'Domingo',
};

export const KIND_LABELS: Record<'RUNNING' | 'STRENGTH', string> = {
  RUNNING: 'Carrera',
  STRENGTH: 'Fuerza',
};

/**
 * The week the arrows walk, in the order the API composes it.
 *
 * <p>Monday-to-Sunday and nothing beyond: `GET /training/week` returns the
 * *current* week and accepts no date parameter (`docs/api/training-week.md` —
 * "no dates, no week navigation"). Stepping past either end would have no data
 * to show, so the controls stop there instead of promising a week the backend
 * cannot answer for.
 */
export const WEEK_ORDER = [
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
  'SUNDAY',
] as const;
