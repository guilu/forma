/**
 * Display-only normalization for the FOR-136 muscle-worked map, consumed by
 * {@link TrainingPage} (FOR-53). The backend returns muscle labels verbatim
 * from `Exercise.primaryMuscles()` — granular, lowercase, accented Spanish
 * (e.g. `"hombro"` and `"hombro anterior"` are distinct catalog values) — and
 * never normalizes them (repository priority: no fabrication in the read
 * model). Per spec FOR-53 Data Model Notes, "the frontend owns a display-
 * label / normalization map" so the heatmap doesn't render fragmented or
 * inconsistent muscle groups. Kept in the UI layer only; the API read model
 * itself is untouched (matches the sibling `goalsDisplay.ts`/
 * `shoppingDisplay.ts` convention of pure presentation helpers split out of
 * their page component).
 */
import type { MuscleLoad, MuscleWorked } from '../api/training';

/**
 * Raw catalog label -> canonical grouping key. Only genuine "hombro" synonyms
 * confirmed against the real catalog (backend `ExerciseCatalog`/
 * `WorkoutTemplateCatalog`): "hombro anterior" (FOR-53) and "hombro lateral",
 * introduced by FOR-154's `lateral-raise` exercise and grouped here per
 * FOR-160. "deltoides posterior" (rear-delt-fly, band-face-pull) is a
 * distinct catalog term, not a "hombro" variant, and is deliberately left
 * ungrouped — grouping it was never asked for and would hide which deltoid
 * head a workout actually targets. Any label not listed here is its own
 * group, normalized by {@link displayLabel} alone — this avoids inventing
 * groupings the spec/backend never described.
 */
const MUSCLE_GROUPS: ReadonlyMap<string, string> = new Map([
  ['hombro anterior', 'hombro'],
  ['hombro lateral', 'hombro'],
]);

/** Canonicalizes a raw muscle label: trims, lowercases (defensive) and applies the group map. */
function canonicalize(rawMuscle: string): string {
  const normalized = rawMuscle.trim().toLowerCase();
  return MUSCLE_GROUPS.get(normalized) ?? normalized;
}

/** Capitalizes a canonical (lowercase, accented) muscle key for display, e.g. "hombro" -> "Hombro". */
function displayLabel(canonical: string): string {
  return canonical.charAt(0).toLocaleUpperCase('es-ES') + canonical.slice(1);
}

const LOAD_RANK: Record<MuscleLoad, number> = { LOW: 0, MEDIUM: 1, HIGH: 2 };

/** The stronger of two loads — used when grouping merges more than one raw muscle into one group. */
function strongerLoad(a: MuscleLoad, b: MuscleLoad): MuscleLoad {
  return LOAD_RANK[b] > LOAD_RANK[a] ? b : a;
}

/** A muscle group ready for display: normalized label + merged load. */
export interface MuscleGroupDisplay {
  readonly label: string;
  readonly load: MuscleLoad;
}

/**
 * Groups raw FOR-136 muscle-worked entries into display-ready groups: applies
 * the synonym map, capitalizes for display, and — when grouping merges more
 * than one raw muscle (e.g. "hombro" + "hombro anterior") — keeps the higher
 * of their loads rather than the last-seen one. Preserves first-appearance
 * order. An empty input (non-strength session) yields an empty result.
 */
export function groupMusclesForDisplay(
  muscles: readonly MuscleWorked[],
): readonly MuscleGroupDisplay[] {
  const order: string[] = [];
  const loadByCanonical = new Map<string, MuscleLoad>();

  for (const { muscle, load } of muscles) {
    const canonical = canonicalize(muscle);
    const existing = loadByCanonical.get(canonical);
    if (existing === undefined) {
      order.push(canonical);
      loadByCanonical.set(canonical, load);
    } else {
      loadByCanonical.set(canonical, strongerLoad(existing, load));
    }
  }

  return order.map((canonical) => ({
    label: displayLabel(canonical),
    load: loadByCanonical.get(canonical) as MuscleLoad,
  }));
}
