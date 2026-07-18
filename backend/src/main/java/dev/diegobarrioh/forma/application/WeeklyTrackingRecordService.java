package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.WeeklyTrackingRecord;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Application use cases for the weekly tracking record (FOR-155, epic FOR-148 slice 7): create
 * (upsert)/list/read the *Seguimiento* weekly rows. Owner-scoped (ADR-002), single-user MVP,
 * mirroring {@code GoalService}'s fixed-owner pattern.
 */
@Service
public class WeeklyTrackingRecordService {

  /** Fixed single-user owner id for the MVP (ADR-002), mirroring {@code GoalService#OWNER_ID}. */
  public static final String OWNER_ID = "default-user";

  private final WeeklyTrackingRecordRepository repository;

  public WeeklyTrackingRecordService(WeeklyTrackingRecordRepository repository) {
    this.repository = repository;
  }

  /** Lists the owner's weekly tracking records, most recent week first. Empty when none exist. */
  public List<WeeklyTrackingRecord> list() {
    return repository.findAllByOwner(OWNER_ID);
  }

  /**
   * Creates or updates the owner's record for {@code record.week()} (upsert; spec FOR-155 api.md:
   * "Create/upsert a weekly record for a given week") and returns the persisted record.
   */
  public WeeklyTrackingRecord save(WeeklyTrackingRecord record) {
    return repository.upsert(OWNER_ID, record);
  }

  /**
   * Reads the owner's record for {@code week}.
   *
   * @throws NotFoundException if no record exists for that week
   */
  public WeeklyTrackingRecord getByWeek(int week) {
    return repository
        .findByOwnerAndWeek(OWNER_ID, week)
        .orElseThrow(
            () -> new NotFoundException("No existe registro semanal para la semana: " + week));
  }
}
