import { Card } from '../../components/Card';
import { SettingsRow } from './SettingsRow';
import { UNIT_PREFERENCES } from './profileData';

/**
 * Units & locale (FOR-58 FR: "peso (kg), altura (cm), distancia (km), energía
 * (kcal)"). Read-only display — spec's Data Model Notes only say units
 * "may drive display formatting locally", not that they are user-editable in
 * this story, and no other screen currently reads a shared unit preference to
 * format against. Making these look like a working selector with no
 * observable effect would itself violate "don't present unsupported flows as
 * active" — so they render as plain, non-interactive rows (no `inert` badge:
 * they are not unsupported, they are simply informational for the MVP).
 */
export function UnitsSection() {
  return (
    <Card title="Unidades" headingLevel={2}>
      {UNIT_PREFERENCES.map((unit) => (
        <SettingsRow key={unit.label} label={unit.label} value={unit.value} />
      ))}
    </Card>
  );
}
