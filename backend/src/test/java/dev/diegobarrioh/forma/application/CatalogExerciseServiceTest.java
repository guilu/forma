package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.diegobarrioh.forma.domain.Modality;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CatalogExerciseService} (FOR-172). Uses a hand-rolled in-memory fake
 * repository (no Spring, no Mockito), mirroring {@code WeeklyTrackingRecordServiceTest} (ADR-007).
 */
class CatalogExerciseServiceTest {

  private final RecordingRepository repository = new RecordingRepository();
  private final CatalogExerciseService service = new CatalogExerciseService(repository);

  private static CatalogExercise pushUp() {
    return new CatalogExercise(
        "push-up",
        "Flexiones",
        Modality.STRENGTH,
        "PUSH",
        "BODYWEIGHT",
        null,
        null,
        null,
        null,
        null,
        "Cuerpo recto, baja el pecho hasta cerca del suelo y empuja hacia arriba.",
        List.of("pecho", "tríceps", "hombro anterior"));
  }

  private static CatalogExercise runningEasy() {
    return new CatalogExercise(
        "running-easy",
        "Rodaje suave",
        Modality.RUNNING,
        null,
        null,
        null,
        null,
        null,
        null,
        "EASY",
        null,
        List.of());
  }

  @Test
  void listWithNoModalityDelegatesToFindAll() {
    repository.all.add(pushUp());
    repository.all.add(runningEasy());

    assertThat(service.list(Optional.empty())).containsExactly(pushUp(), runningEasy());
  }

  @Test
  void listWithModalityDelegatesToFindByModality() {
    repository.byModality.put(Modality.STRENGTH, List.of(pushUp()));

    assertThat(service.list(Optional.of(Modality.STRENGTH))).containsExactly(pushUp());
    assertThat(repository.lastModalityRequested).isEqualTo(Modality.STRENGTH);
  }

  @Test
  void getByIdReturnsStoredExercise() {
    repository.byId.put("push-up", pushUp());

    assertThat(service.getById("push-up")).isEqualTo(pushUp());
  }

  @Test
  void getByIdThrowsNotFoundWhenMissing() {
    assertThatThrownBy(() -> service.getById("nope")).isInstanceOf(NotFoundException.class);
  }

  /** In-memory {@link ExerciseCatalogRepository} fake. */
  private static final class RecordingRepository implements ExerciseCatalogRepository {
    private final List<CatalogExercise> all = new ArrayList<>();
    private final Map<Modality, List<CatalogExercise>> byModality = new HashMap<>();
    private final Map<String, CatalogExercise> byId = new HashMap<>();
    private Modality lastModalityRequested;

    @Override
    public List<CatalogExercise> findAll() {
      return List.copyOf(all);
    }

    @Override
    public List<CatalogExercise> findByModality(Modality modality) {
      lastModalityRequested = modality;
      return byModality.getOrDefault(modality, List.of());
    }

    @Override
    public Optional<CatalogExercise> findById(String id) {
      return Optional.ofNullable(byId.get(id));
    }
  }
}
