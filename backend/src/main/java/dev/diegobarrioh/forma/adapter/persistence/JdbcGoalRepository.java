package dev.diegobarrioh.forma.adapter.persistence;

import dev.diegobarrioh.forma.application.GoalRepository;
import dev.diegobarrioh.forma.application.StoredGoal;
import dev.diegobarrioh.forma.domain.Goal;
import dev.diegobarrioh.forma.domain.GoalMetric;
import dev.diegobarrioh.forma.domain.GoalStatus;
import dev.diegobarrioh.forma.domain.Milestone;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * JDBC adapter that persists {@link Goal}s and their {@link Milestone}s to the {@code goal}/{@code
 * goal_milestone} tables (FOR-125, migration V11).
 *
 * <p>Plain JDBC via {@link JdbcTemplate} — no ORM (ADR-003), following {@link
 * JdbcShoppingListRepository}'s two-table shape (FOR-39): a goal is a per-owner list of aggregates,
 * each with ordered child milestone rows. Milestone completion is updated in place by id (never
 * delete-and-reinsert) since this slice's PATCH only ever toggles existing milestones, never adds
 * or removes one ({@code GoalService}).
 *
 * <p><b>owner_id -&gt; user_id (FOR-145b-1, migration V27).</b> All reads/writes now scope by the
 * real {@code user_id UUID} column. The legacy {@code owner_id VARCHAR} column is kept alive (not
 * yet dropped — a later contract migration will drop it) and is written on insert as {@code
 * userId.toString()} purely so it stays populated/non-null for existing rows' shape; it is never
 * read by this adapter anymore.
 */
@Repository
public class JdbcGoalRepository implements GoalRepository {

  private static final RowMapper<GoalRow> GOAL_ROW_MAPPER =
      (rs, rowNum) ->
          new GoalRow(
              rs.getString("id"),
              rs.getString("title"),
              GoalMetric.valueOf(rs.getString("metric")),
              rs.getBigDecimal("target").doubleValue(),
              rs.getObject("due_date", LocalDate.class),
              GoalStatus.valueOf(rs.getString("status")));

  private static final RowMapper<Milestone> MILESTONE_ROW_MAPPER =
      (rs, rowNum) ->
          new Milestone(
              rs.getString("id"),
              rs.getString("title"),
              rs.getBigDecimal("target").doubleValue(),
              rs.getBoolean("completed"));

  private final JdbcTemplate jdbcTemplate;

  public JdbcGoalRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public List<StoredGoal> findAllByOwner(UUID userId) {
    List<GoalRow> rows =
        jdbcTemplate.query(
            "SELECT id, title, metric, target, due_date, status FROM goal"
                + " WHERE user_id = ? ORDER BY id",
            GOAL_ROW_MAPPER,
            userId);
    return rows.stream().map(this::toStoredGoal).toList();
  }

  @Override
  public StoredGoal create(UUID userId, Goal goal) {
    UUID goalId = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO goal (id, owner_id, user_id, title, metric, target, due_date, status)"
            + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
        goalId,
        userId.toString(),
        userId,
        goal.title(),
        goal.metric().name(),
        BigDecimal.valueOf(goal.target()),
        goal.dueDate() == null ? null : Date.valueOf(goal.dueDate()),
        goal.status().name());
    List<Milestone> insertedMilestones = insertMilestones(goalId, goal.milestones());
    Goal persisted =
        new Goal(
            goal.title(),
            goal.metric(),
            goal.target(),
            goal.dueDate(),
            goal.status(),
            insertedMilestones);
    return new StoredGoal(goalId.toString(), persisted);
  }

  @Override
  public Optional<StoredGoal> findById(UUID userId, String goalId) {
    UUID id;
    try {
      id = UUID.fromString(goalId);
    } catch (IllegalArgumentException ex) {
      return Optional.empty();
    }
    try {
      GoalRow row =
          jdbcTemplate.queryForObject(
              "SELECT id, title, metric, target, due_date, status FROM goal"
                  + " WHERE id = ? AND user_id = ?",
              GOAL_ROW_MAPPER,
              id,
              userId);
      return Optional.of(toStoredGoal(row));
    } catch (EmptyResultDataAccessException ex) {
      return Optional.empty();
    }
  }

  @Override
  public Optional<StoredGoal> update(UUID userId, String goalId, Goal goal) {
    UUID id = UUID.fromString(goalId);
    int updated =
        jdbcTemplate.update(
            "UPDATE goal SET title = ?, target = ?, due_date = ?, status = ?"
                + " WHERE id = ? AND user_id = ?",
            goal.title(),
            BigDecimal.valueOf(goal.target()),
            goal.dueDate() == null ? null : Date.valueOf(goal.dueDate()),
            goal.status().name(),
            id,
            userId);
    if (updated == 0) {
      return Optional.empty();
    }
    for (Milestone milestone : goal.milestones()) {
      if (milestone.id() != null) {
        jdbcTemplate.update(
            "UPDATE goal_milestone SET completed = ? WHERE id = ? AND goal_id = ?",
            milestone.completed(),
            UUID.fromString(milestone.id()),
            id);
      }
    }
    return findById(userId, goalId);
  }

  private StoredGoal toStoredGoal(GoalRow row) {
    List<Milestone> milestones = milestonesFor(UUID.fromString(row.id()));
    Goal goal =
        new Goal(row.title(), row.metric(), row.target(), row.dueDate(), row.status(), milestones);
    return new StoredGoal(row.id(), goal);
  }

  private List<Milestone> insertMilestones(UUID goalId, List<Milestone> milestones) {
    List<Milestone> result = new ArrayList<>();
    int position = 0;
    for (Milestone milestone : milestones) {
      UUID milestoneId = UUID.randomUUID();
      jdbcTemplate.update(
          "INSERT INTO goal_milestone (id, goal_id, title, target, completed, position)"
              + " VALUES (?, ?, ?, ?, ?, ?)",
          milestoneId,
          goalId,
          milestone.title(),
          BigDecimal.valueOf(milestone.target()),
          milestone.completed(),
          position);
      result.add(
          new Milestone(
              milestoneId.toString(),
              milestone.title(),
              milestone.target(),
              milestone.completed()));
      position++;
    }
    return result;
  }

  private List<Milestone> milestonesFor(UUID goalId) {
    return jdbcTemplate.query(
        "SELECT id, title, target, completed FROM goal_milestone"
            + " WHERE goal_id = ? ORDER BY position",
        MILESTONE_ROW_MAPPER,
        goalId);
  }

  /** Row of {@code goal} without its milestones. */
  private record GoalRow(
      String id,
      String title,
      GoalMetric metric,
      double target,
      LocalDate dueDate,
      GoalStatus status) {}
}
