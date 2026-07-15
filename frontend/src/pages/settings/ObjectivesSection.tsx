import { Card } from '../../components/Card';
import { SettingsRow } from './SettingsRow';
import { DEFAULT_OBJECTIVES } from './profileData';

/**
 * Default objectives (FOR-58 FR: "déficit calórico, proteínas, agua diaria —
 * entry points").
 *
 * <p>FOR-58 originally also treated this section as covering the spec's
 * separate "Training / nutrition preference entry points" bullet, since
 * `ui.md`'s Components list had no distinct component for it (documented
 * assumption). FOR-119 resolves that folding with its own distinct {@link
 * import('./TrainingNutritionSection').TrainingNutritionSection} instead, so
 * this section is back to covering only the default-objectives rows it was
 * named for.
 *
 * <p>Each row shows the current default value but is an inert entry point: no
 * screen exists yet to edit these.
 *
 * <p><b>FOR-122 update:</b> a dedicated Objetivos screen (`GoalsPage.tsx`) now
 * exists, closing the gap this comment used to describe ("no dedicated FOR-47
 * child story"). It does not replace this section, though: verified against
 * the real FOR-125 backend, the new screen's `Goal` domain only tracks
 * body-composition metrics (`BODY_FAT_PCT`/`WEIGHT_KG`/`LEAN_MASS_KG`) with
 * milestones — a different data model from these rows' déficit calórico/
 * proteínas/agua diaria nutrition defaults (`DEFAULT_OBJECTIVES`). The FOR-122
 * spec assumed these would turn out to be the same data ("the same data as
 * both a dead-end summary and a real screen"); the actual backend that
 * shipped shows they are not, so this section stays as its own inert,
 * still-backend-less entry point rather than being retired or linked through
 * to a screen that cannot show its data (AGENTS.md: repository state over
 * spec intent — document the gap, don't force a merge that isn't real).
 */
export function ObjectivesSection() {
  return (
    <Card title="Objetivos por defecto" headingLevel={2}>
      {DEFAULT_OBJECTIVES.map((objective) => (
        <SettingsRow key={objective.label} label={objective.label} value={objective.value} inert />
      ))}
    </Card>
  );
}
