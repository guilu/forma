import type { WorkoutItem } from '../api/training';

/**
 * How a strength prescription is written down, in one place.
 *
 * <p>Two screens print the same numbers: the full "Entrenar" page
 * ({@link TrainingDetailPage}), which lays them out as a set table, and the
 * week's session dialog ({@link TrainingPage}), which prints a one-line summary
 * under each exercise name. They must agree — a rep range shown as "8–12" on
 * one screen and "8-12" on the other is the kind of drift nobody notices until
 * a user does — so the formatting rules live here rather than in either page
 * (`.ai/conventions.md`: "do not duplicate domain calculations").
 *
 * <p>This is presentation only. The prescription itself is a backend read model
 * ({@link WorkoutItem}, FOR-53); nothing here decides what a session is, only
 * how its numbers are spelled.
 */

/** Strips the kind prefix ("Fuerza · Empuje" -> "Empuje") a caller already shows elsewhere. */
export function displayTitle(title: string): string {
  return title.replace(/^Fuerza\s*·\s*/i, '');
}

/**
 * The rep target for one exercise: a range, the AMRAP marker, or a hold in
 * seconds. Ranges use an en dash, and a range whose ends match collapses to a
 * single number rather than printing "10–10".
 */
export function repTarget(item: WorkoutItem): string {
  if (item.repScheme === 'AMRAP') return 'AMRAP';
  if (item.repScheme === 'TIME_HOLD') {
    const min = item.durationSecondsMin ?? item.durationSecondsMax ?? 0;
    const max = item.durationSecondsMax ?? min;
    return min === max ? `${min} s` : `${min}–${max} s`;
  }
  const min = item.repsMin ?? item.repsMax ?? 0;
  const max = item.repsMax ?? min;
  return min === max ? String(min) : `${min}–${max}`;
}

/**
 * The whole prescription on one line: "4 series · 8–12 reps · RIR 2 · Desc 90 s".
 *
 * <p>The "reps" unit is attached to the RANGE scheme only. AMRAP already names
 * itself and a timed hold already carries its seconds, so appending the word to
 * either would produce "AMRAP reps" and "45–75 s reps".
 */
export function prescriptionSummary(item: WorkoutItem): string {
  const sets = `${item.sets} ${item.sets === 1 ? 'serie' : 'series'}`;
  const target = item.repScheme === 'RANGE' ? `${repTarget(item)} reps` : repTarget(item);
  return `${sets} · ${target} · RIR ${item.rir} · Desc ${item.restSeconds} s`;
}
