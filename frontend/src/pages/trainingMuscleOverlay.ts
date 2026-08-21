import type { MuscleLoad, MuscleWorked } from '../api/training';

/**
 * Translates the FOR-136 muscle-worked map into the silhouette pack's muscle
 * codes, so the training card can light up the body illustration.
 *
 * <p>Two vocabularies meet here. The backend returns labels verbatim from the
 * exercise catalog — lowercase, accented Spanish, granular ("hombro anterior"
 * and "hombro lateral" are distinct rows) — and never normalizes them. The
 * overlay pack (`assets/anatomy`, see its `muscle-map.json`) speaks a fixed set
 * of twenty anatomical codes. Neither side is going to change for the other, so
 * the translation is a UI concern, exactly like the display-label grouping in
 * the sibling {@link trainingMuscleLabels}.
 *
 * <p>The map is deliberately partial in both directions. Six pack codes have no
 * catalog counterpart at all (both forearms, adductors, the front lower leg,
 * the lower back and the soleus) and simply stay dark; a catalog label with no
 * code is skipped rather than approximated onto a neighbouring muscle, because
 * lighting the wrong muscle is worse than lighting none.
 */
export type MuscleCode =
  | 'ABS'
  | 'ADDUCTORS'
  | 'BICEPS'
  | 'CALVES'
  | 'DELTOID_FRONT'
  | 'DELTOID_REAR'
  | 'FOREARM_BACK'
  | 'FOREARM_FRONT'
  | 'GLUTES'
  | 'HAMSTRINGS'
  | 'LATS'
  | 'LOWER_BACK'
  | 'LOWER_LEG_FRONT'
  | 'OBLIQUES'
  | 'PECTORAL'
  | 'QUADRICEPS'
  | 'SERRATUS'
  | 'SOLEUS'
  | 'TRAPEZIUS'
  | 'TRICEPS'
  | 'UPPER_BACK';

/** How strongly a muscle is drawn: the pack's own two emphasis levels. */
export type MuscleRole = 'primary' | 'secondary';

export type BodyView = 'front' | 'back';

/**
 * Which codes each silhouette can actually draw, copied from the pack's
 * `muscle-map.json`. A code asked for on the wrong view has no mask file, so
 * the component uses this to skip it instead of requesting a 404.
 */
export const MUSCLE_CODES_BY_VIEW: Readonly<Record<BodyView, readonly MuscleCode[]>> = {
  front: [
    'ABS',
    'ADDUCTORS',
    'BICEPS',
    'DELTOID_FRONT',
    'FOREARM_FRONT',
    'LOWER_LEG_FRONT',
    'OBLIQUES',
    'PECTORAL',
    'QUADRICEPS',
    'SERRATUS',
  ],
  back: [
    'CALVES',
    'DELTOID_REAR',
    'FOREARM_BACK',
    'GLUTES',
    'HAMSTRINGS',
    'LATS',
    'LOWER_BACK',
    'SOLEUS',
    'TRAPEZIUS',
    'TRICEPS',
    'UPPER_BACK',
  ],
};

/**
 * Catalog label -> the code(s) it lights.
 *
 * <p>Most rows are a plain one-to-one rename. The three that are not:
 * <ul>
 *   <li>the three "hombro" variants all fold onto the front deltoid, matching
 *       the grouping {@link trainingMuscleLabels} already applies to the same
 *       labels for display;
 *   <li>"core" lights the abdominals *and* the obliques — it is the catalog's
 *       one genuinely regional label, and drawing only the abs would under-sell
 *       a session built around it;
 *   <li>"romboides" has no code of its own; the rhomboids sit under the upper
 *       back, which is the closest the pack can draw.
 * </ul>
 * "deltoides posterior" stays on the rear deltoid rather than joining the
 * "hombro" group, for the reason its sibling gives: which deltoid head a
 * workout targets is information worth keeping.
 */
const LABEL_TO_CODES: ReadonlyMap<string, readonly MuscleCode[]> = new Map([
  ['abdomen', ['ABS']],
  ['bíceps', ['BICEPS']],
  ['core', ['ABS', 'OBLIQUES']],
  ['cuádriceps', ['QUADRICEPS']],
  ['deltoides posterior', ['DELTOID_REAR']],
  ['dorsal', ['LATS']],
  ['gemelos', ['CALVES']],
  ['glúteo', ['GLUTES']],
  ['hombro', ['DELTOID_FRONT']],
  ['hombro anterior', ['DELTOID_FRONT']],
  ['hombro lateral', ['DELTOID_FRONT']],
  ['isquiotibiales', ['HAMSTRINGS']],
  ['pecho', ['PECTORAL']],
  ['romboides', ['UPPER_BACK']],
  ['serrato anterior', ['SERRATUS']],
  ['trapecio', ['TRAPEZIUS']],
  ['tríceps', ['TRICEPS']],
]);

/**
 * Which sheet draws a muscle group, for a legend that splits by view.
 *
 * <p>Reads the same {@link LABEL_TO_CODES} map the overlay does, so the column
 * a group is listed in and the body it is lit on can never disagree — a second
 * hand-written list of "front muscles" would drift the first time the catalog
 * grew.
 *
 * <p>Returns `undefined` in the two cases where a column would be a guess: a
 * label the pack has no code for (the map is deliberately partial), and a label
 * whose codes span both sheets. No catalog label does the latter today; the
 * check is here so that adding one shows up as an unplaced group rather than as
 * a muscle silently listed on the wrong body.
 */
export function viewForMuscle(muscle: string): BodyView | undefined {
  const codes = LABEL_TO_CODES.get(muscle.trim().toLowerCase());
  if (!codes || codes.length === 0) return undefined;

  const views = new Set<BodyView>();
  for (const code of codes) {
    for (const view of ['front', 'back'] as const) {
      if (MUSCLE_CODES_BY_VIEW[view].includes(code)) views.add(view);
    }
  }
  return views.size === 1 ? [...views][0] : undefined;
}

/**
 * `HIGH` is what the session is built around; everything else supports it.
 *
 * <p>Exported so a legend beside the silhouette can key its swatch off the same
 * rule the overlay uses. A legend whose emphasis disagrees with the body it
 * explains is worse than no legend at all.
 */
export function roleForLoad(load: MuscleLoad): MuscleRole {
  return load === 'HIGH' ? 'primary' : 'secondary';
}

/**
 * Builds the code -> emphasis overlay for a session's muscle map.
 *
 * <p>When two labels reach the same code (the catalog has both "abdomen" and
 * "core", and all three shoulder variants), the stronger emphasis wins: a
 * muscle worked hard by one exercise and lightly by another is still worked
 * hard.
 */
export function overlayFromMuscleMap(
  muscles: readonly MuscleWorked[],
): Readonly<Partial<Record<MuscleCode, MuscleRole>>> {
  const overlay: Partial<Record<MuscleCode, MuscleRole>> = {};

  for (const { muscle, load } of muscles) {
    const codes = LABEL_TO_CODES.get(muscle.trim().toLowerCase());
    if (!codes) continue;

    const role = roleForLoad(load);
    for (const code of codes) {
      if (overlay[code] !== 'primary') overlay[code] = role;
    }
  }

  return overlay;
}
