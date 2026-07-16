package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Domain tests for {@link SyncOutcome} (FOR-126, extended by FOR-132): construction validation.
 * {@code importedCount}/{@code duplicatesSkipped} validate to non-negative values — a sync must
 * never fabricate imported measures or dedup counts (spec FOR-126/FOR-132 Functional Requirements:
 * "must NOT fabricate imported data").
 */
class SyncOutcomeTest {

  @Test
  void rejectsNullResult() {
    assertThatThrownBy(() -> new SyncOutcome(null, 0, 0, null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsNegativeImportedCount() {
    assertThatThrownBy(() -> new SyncOutcome(SyncResult.OK, -1, 0, null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsNegativeDuplicatesSkipped() {
    assertThatThrownBy(() -> new SyncOutcome(SyncResult.OK, 0, -1, null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void aZeroImportedCountIsAllowed() {
    SyncOutcome outcome = new SyncOutcome(SyncResult.OK, 0, 0, null);

    assertThat(outcome.importedCount()).isZero();
  }

  @Test
  void duplicatesSkippedIsExposed() {
    SyncOutcome outcome = new SyncOutcome(SyncResult.OK, 3, 12, null);

    assertThat(outcome.duplicatesSkipped()).isEqualTo(12);
  }

  @Test
  void messageIsOptional() {
    SyncOutcome outcome =
        new SyncOutcome(SyncResult.NOT_CONNECTED, 0, 0, "El proveedor no está conectado.");

    assertThat(outcome.message()).isEqualTo("El proveedor no está conectado.");
  }
}
