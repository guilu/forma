package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Domain tests for {@link SyncOutcome} (FOR-126): construction validation. {@code importedCount}
 * defaults/validates to a non-negative value — this slice never fabricates imported measures (spec
 * FOR-126 Functional Requirements: "must NOT fabricate imported data"), so 0 is always the honest
 * value produced by {@link IntegrationConnection}'s stub sync.
 */
class SyncOutcomeTest {

  @Test
  void rejectsNullResult() {
    assertThatThrownBy(() -> new SyncOutcome(null, 0, null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsNegativeImportedCount() {
    assertThatThrownBy(() -> new SyncOutcome(SyncResult.OK, -1, null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void aZeroImportedCountIsAllowed() {
    SyncOutcome outcome = new SyncOutcome(SyncResult.OK, 0, null);

    assertThat(outcome.importedCount()).isZero();
  }

  @Test
  void messageIsOptional() {
    SyncOutcome outcome =
        new SyncOutcome(SyncResult.NOT_CONNECTED, 0, "El proveedor no está conectado.");

    assertThat(outcome.message()).isEqualTo("El proveedor no está conectado.");
  }
}
