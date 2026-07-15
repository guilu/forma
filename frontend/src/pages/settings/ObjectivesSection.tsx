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
 * screen exists yet to edit these (spec's Objetivos screen has no dedicated
 * FOR-47 child story either — see `ProfileSection`'s "Objetivo principal").
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
