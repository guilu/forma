import { Card } from '../../components/Card';
import { SettingsRow } from './SettingsRow';

/**
 * Training / nutrition preference entry point (FOR-58 FR: "Training /
 * nutrition preference entry points"). FOR-58 originally folded this bullet
 * into {@link import('./ObjectivesSection').ObjectivesSection} as a
 * documented assumption ("`ui.md`'s Components list has no distinct
 * component for that bullet"). FOR-119 is the story that resolves that
 * folding by giving it its own distinct, visible section (spec FOR-119:
 * "give training and nutrition preferences their own visible entry point,
 * distinct from `ObjectivesSection`'s default-objectives rows").
 *
 * <p><b>Repository-state discrepancy (documented, not invented):</b>
 * FOR-119's `spec.md` says this entry point "persists through FOR-107", but
 * the actually shipped FOR-107 API (`UserProfileController`, verified
 * directly against the backend source) exposes no dedicated
 * training/nutrition-preference field or update endpoint. Training days and
 * nutrition preference/restrictions only exist inside the onboarding draft
 * (`PATCH /api/v1/profile/onboarding`, `UserProfileResponse.onboardingAnswers`),
 * and wiring an edit flow through that endpoint is explicitly FOR-121's scope
 * (onboarding persistence) — out of bounds for this story. Building a
 * selector that silently calls the onboarding endpoint would misrepresent
 * what this story edits; inventing a new backend endpoint is equally out of
 * scope (AGENTS.md: "never invent missing code"). So this entry point stays
 * a real, visible row — distinct from `ObjectivesSection` — marked inert
 * ("Próximamente"), the same honest pattern {@link
 * import('./SecuritySection').SecuritySection} already uses for other
 * backend-less flows (spec edge case: "unsupported option -> shown as
 * próximamente/inert, not a broken action"). Follow-up: FOR-121 (or a
 * dedicated training/nutrition-preference endpoint) is needed before this
 * can persist.
 */
export function TrainingNutritionSection() {
  return (
    <Card title="Preferencias de entrenamiento y nutrición" headingLevel={2}>
      <SettingsRow
        label="Preferencias de entrenamiento"
        description="Días disponibles, equipamiento y nivel."
        inert
      />
      <SettingsRow
        label="Preferencias de nutrición"
        description="Restricciones y preferencias alimentarias."
        inert
      />
    </Card>
  );
}
