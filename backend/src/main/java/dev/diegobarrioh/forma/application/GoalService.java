package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.Goal;
import dev.diegobarrioh.forma.domain.GoalProgress;
import dev.diegobarrioh.forma.domain.GoalStatus;
import dev.diegobarrioh.forma.domain.Milestone;
import dev.diegobarrioh.forma.domain.WeeklyBodySummary;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Application use cases for goals & milestones (FOR-125): create/read/update, owner-scoped
 * (ADR-002), with progress always freshly derived — never stored (spec FOR-125 Data Model Notes).
 *
 * <p>Reuses {@link WeeklyBodySummaryService} (FOR-21, itself built on {@code BodyMeasurement}
 * history, FOR-16) for progress derivation rather than re-deriving body metrics here — see {@link
 * dev.diegobarrioh.forma.domain.GoalProgress#derive}.
 *
 * <p>Real multi-user auth (FOR-145b-1, ADR-012): every use case resolves the caller's account id
 * via {@link CurrentUserProvider} instead of the old fixed {@code OWNER_ID = "default-user"}
 * constant (removed by this slice).
 */
@Service
public class GoalService {

  private final GoalRepository repository;
  private final WeeklyBodySummaryService weeklyBodySummaryService;
  private final CurrentUserProvider currentUserProvider;

  public GoalService(
      GoalRepository repository,
      WeeklyBodySummaryService weeklyBodySummaryService,
      CurrentUserProvider currentUserProvider) {
    this.repository = repository;
    this.weeklyBodySummaryService = weeklyBodySummaryService;
    this.currentUserProvider = currentUserProvider;
  }

  /** Lists the owner's goals with freshly derived progress. Empty when none exist yet. */
  public List<GoalView> list() {
    WeeklyBodySummary summary = weeklyBodySummaryService.currentSummary();
    return repository.findAllByOwner(currentUserProvider.currentUserId()).stream()
        .map(stored -> toView(stored, summary))
        .toList();
  }

  /** Creates a goal (with its milestones) for the owner and returns it with derived progress. */
  public GoalView create(Goal goal) {
    StoredGoal stored = repository.create(currentUserProvider.currentUserId(), goal);
    return toView(stored, weeklyBodySummaryService.currentSummary());
  }

  /**
   * Partially updates a goal's fields and/or its milestones' completion state. A {@code null}
   * argument leaves the corresponding stored field unchanged (mirrors {@link UserProfileService}'s
   * merge convention). {@code metric} is never changeable via PATCH in this slice — reclassifying a
   * goal's tracked dimension is out of scope.
   *
   * @throws NotFoundException if no goal with {@code goalId} exists for the owner
   */
  public GoalView update(
      String goalId,
      String title,
      Double target,
      LocalDate dueDate,
      GoalStatus status,
      List<MilestonePatch> milestonePatches) {
    var userId = currentUserProvider.currentUserId();
    StoredGoal current =
        repository
            .findById(userId, goalId)
            .orElseThrow(() -> new NotFoundException("No existe el objetivo: " + goalId));
    Goal existing = current.goal();

    Map<String, Boolean> patchByMilestoneId =
        (milestonePatches == null ? List.<MilestonePatch>of() : milestonePatches)
            .stream()
                .collect(
                    Collectors.toMap(
                        MilestonePatch::milestoneId, MilestonePatch::completed, (a, b) -> b));
    List<Milestone> mergedMilestones =
        existing.milestones().stream()
            .map(
                milestone ->
                    patchByMilestoneId.containsKey(milestone.id())
                        ? milestone.withCompleted(patchByMilestoneId.get(milestone.id()))
                        : milestone)
            .toList();

    Goal merged =
        new Goal(
            title != null ? title : existing.title(),
            existing.metric(),
            target != null ? target : existing.target(),
            dueDate != null ? dueDate : existing.dueDate(),
            status != null ? status : existing.status(),
            mergedMilestones);

    StoredGoal updated =
        repository
            .update(userId, goalId, merged)
            .orElseThrow(() -> new NotFoundException("No existe el objetivo: " + goalId));
    return toView(updated, weeklyBodySummaryService.currentSummary());
  }

  private static GoalView toView(StoredGoal stored, WeeklyBodySummary summary) {
    GoalProgress progress =
        GoalProgress.derive(stored.goal().metric(), stored.goal().target(), summary);
    return new GoalView(stored.id(), stored.goal(), progress);
  }
}
