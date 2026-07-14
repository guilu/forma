import { Card } from '../../components/Card';
import { SettingsRow } from './SettingsRow';
import { DEFAULT_OBJECTIVES } from './profileData';

/**
 * Default objectives (FOR-58 FR: "déficit calórico, proteínas, agua diaria —
 * entry points"). Also covers the spec's separate "Training / nutrition
 * preference entry points" bullet — `ui.md`'s Components list has no distinct
 * component for that bullet, so this section is treated as satisfying both
 * (documented assumption; see FOR-58 PR description).
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
