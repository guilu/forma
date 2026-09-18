import type { DirectionOption, OnboardingAnswers } from '../onboardingStorage';
import styles from './steps.module.css';

/**
 * Plan-direction step (ADR-015 decision 5's amendment, {@code
 * docs/FORMA_Contrato_Agente_Plan.md}): the explicit question the wizard
 * asks instead of deriving a plan's calorie direction from {@link GoalStep}.
 * "Composición corporal" is honestly ambiguous between losing fat and
 * gaining muscle — the product owner rejected inferring one from the other,
 * because guessing somebody's calories is exactly where guessing is
 * unacceptable.
 *
 * <p>A different question from {@link
 * import('./GoalStep').GoalStep GoalStep}'s standing life goal, not a
 * replacement for it: the backend carries both `main_goal` (this profile's
 * standing goal) and `plan_objective` (what THIS plan should do), because a
 * `HABITO` user asking to gain muscle is an ordinary combination, not a
 * contradiction (ADR-015 decision 5). Placed as its own step, immediately
 * after Objetivo, rather than folded into it: the two answers are visually
 * and conceptually distinct choices (one radio group each) and cramming a
 * second question into `GoalStep` — which stays exactly as it was, per the
 * ADR — would make it unclear which choice sets which field.
 *
 * <p>Required for the final plan-request submission ({@code
 * PlanRequestCreateRequest.direction} is {@code @NotNull}), but skippable
 * here like every non-critical step (only "Nombre" blocks the flow): a
 * skipped direction is caught before the request is ever sent (see {@link
 * import('../OnboardingPage').OnboardingPage}), which is more honest than
 * forcing a choice this early just to avoid a later, better-explained check.
 */
const DIRECTION_OPTIONS: ReadonlyArray<{
  id: DirectionOption;
  title: string;
  description: string;
}> = [
  {
    id: 'LOSE_FAT',
    title: 'Perder grasa',
    description: 'Un déficit calórico para reducir grasa corporal.',
  },
  {
    id: 'GAIN_MUSCLE',
    title: 'Ganar músculo',
    description: 'Un superávit calórico para ganar masa muscular.',
  },
  {
    id: 'MAINTAIN',
    title: 'Mantenerme',
    description: 'Conservar tu peso y tu composición actuales.',
  },
];

interface DirectionStepProps {
  readonly value: OnboardingAnswers['direction'];
  readonly onChange: (patch: Partial<OnboardingAnswers['direction']>) => void;
}

export function DirectionStep({ value, onChange }: DirectionStepProps) {
  return (
    <div className={styles.field}>
      <p className={styles.intro}>
        ¿Qué dirección quieres que siga tu próximo plan? Esto decide cuántas calorías te
        propondremos — es distinto de tu objetivo general.
      </p>
      <div className={styles.choiceGrid} role="radiogroup" aria-label="Dirección del plan">
        {DIRECTION_OPTIONS.map((option) => (
          <button
            key={option.id}
            type="button"
            role="radio"
            aria-checked={value.selected === option.id}
            className={styles.choiceCard}
            onClick={() => onChange({ selected: option.id })}
          >
            <span className={styles.choiceTitle}>{option.title}</span>
            <span className={styles.choiceDescription}>{option.description}</span>
          </button>
        ))}
      </div>
    </div>
  );
}
