package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Domain unit tests for {@link OnboardingAnswers} (FOR-107): the per-step onboarding draft, mirrors
 * the frontend {@code OnboardingAnswers} shape (FOR-59, {@code onboardingStorage.ts}). Plain JUnit
 * 5 + AssertJ (ADR-007).
 */
class OnboardingAnswersTest {

  @Test
  void emptyConstantHasBlankAnswersAndNoSelections() {
    OnboardingAnswers empty = OnboardingAnswers.EMPTY;

    assertThat(empty.profile().name()).isEmpty();
    assertThat(empty.profile().birthDate()).isEmpty();
    assertThat(empty.profile().sex()).isEmpty();
    assertThat(empty.profile().heightCm()).isEmpty();
    assertThat(empty.metrics().choice()).isNull();
    assertThat(empty.metrics().measurementSaved()).isFalse();
    assertThat(empty.goal().selected()).isNull();
    assertThat(empty.training().days()).isEmpty();
    assertThat(empty.equipment().items()).isEmpty();
    assertThat(empty.nutrition().preference()).isEmpty();
    assertThat(empty.nutrition().restrictions()).isEmpty();
  }

  @Test
  void nullGroupsIndividuallyDefaultToEmpty() {
    OnboardingAnswers answers = new OnboardingAnswers(null, null, null, null, null, null);

    assertThat(answers).isEqualTo(OnboardingAnswers.EMPTY);
  }

  @Test
  void preservesSuppliedAnswers() {
    OnboardingAnswers answers =
        new OnboardingAnswers(
            new OnboardingAnswers.ProfileDraft("Ada", "1990-01-01", "FEMALE", "170"),
            new OnboardingAnswers.MetricsDraft("MANUAL", true),
            new OnboardingAnswers.GoalDraft("COMPOSICION"),
            new OnboardingAnswers.TrainingDraft(List.of("MONDAY", "THURSDAY")),
            new OnboardingAnswers.EquipmentDraft(List.of("DUMBBELLS")),
            new OnboardingAnswers.NutritionDraft("high-protein", "lactose"));

    assertThat(answers.profile().name()).isEqualTo("Ada");
    assertThat(answers.metrics().choice()).isEqualTo("MANUAL");
    assertThat(answers.goal().selected()).isEqualTo("COMPOSICION");
    assertThat(answers.training().days()).containsExactly("MONDAY", "THURSDAY");
    assertThat(answers.equipment().items()).containsExactly("DUMBBELLS");
    assertThat(answers.nutrition().preference()).isEqualTo("high-protein");
  }

  @Test
  void trainingDaysAndEquipmentItemsAreImmutable() {
    OnboardingAnswers.TrainingDraft training =
        new OnboardingAnswers.TrainingDraft(List.of("MONDAY"));

    assertThat(training.days()).isUnmodifiable();
  }
}
