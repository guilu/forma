package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.WeeklyTrackingRecord;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Application use cases for the weekly tracking record (FOR-155, epic FOR-148 slice 7): create
 * (upsert)/list/read the *Seguimiento* weekly rows. Owner-scoped (ADR-002).
 *
 * <p>Real multi-user auth (FOR-145b-1, ADR-012): every use case resolves the caller's account id
 * via {@link CurrentUserProvider} instead of the old fixed {@code OWNER_ID = "default-user"}
 * constant (removed by this slice).
 */
@Service
public class WeeklyTrackingRecordService {

  private final WeeklyTrackingRecordRepository repository;
  private final CurrentUserProvider currentUserProvider;

  public WeeklyTrackingRecordService(
      WeeklyTrackingRecordRepository repository, CurrentUserProvider currentUserProvider) {
    this.repository = repository;
    this.currentUserProvider = currentUserProvider;
  }

  /** Lists the owner's weekly tracking records, most recent week first. Empty when none exist. */
  public List<WeeklyTrackingRecord> list() {
    return repository.findAllByOwner(currentUserProvider.currentUserId());
  }

  /**
   * Creates or updates the owner's record for {@code record.week()} (upsert; spec FOR-155 api.md:
   * "Create/upsert a weekly record for a given week") and returns the persisted record.
   */
  public WeeklyTrackingRecord save(WeeklyTrackingRecord record) {
    return repository.upsert(currentUserProvider.currentUserId(), record);
  }

  /**
   * Reads the owner's record for {@code week}.
   *
   * @throws NotFoundException if no record exists for that week
   */
  public WeeklyTrackingRecord getByWeek(int week) {
    return repository
        .findByOwnerAndWeek(currentUserProvider.currentUserId(), week)
        .orElseThrow(
            () -> new NotFoundException("No existe registro semanal para la semana: " + week));
  }
}
