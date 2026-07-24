package dev.diegobarrioh.forma.adapter.persistence;

import dev.diegobarrioh.forma.application.CatalogExercise;
import dev.diegobarrioh.forma.application.ExerciseCatalogRepository;
import dev.diegobarrioh.forma.domain.Modality;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * JDBC adapter reading the read-only {@code exercise_catalog} + {@code exercise_catalog_muscle}
 * tables (FOR-172, V24). Plain JDBC via {@link JdbcTemplate} (no ORM, like FOR-16).
 *
 * <p>2-query load strategy (avoids N+1 and avoids {@code ResultSetExtractor} complexity): parents
 * are loaded first, then muscle children are loaded and grouped by {@code exercise_id} preserving
 * {@code ordinal} order, and attached to the parent in a second pass.
 */
@Repository
public class JdbcExerciseCatalogRepository implements ExerciseCatalogRepository {

  private static final String PARENT_COLUMNS =
      "id, name, modality, movement_pattern, equipment, default_sets, default_reps,"
          + " default_distance_km, default_pace_min_per_km, session_kind, instructions";

  private static final RowMapper<CatalogExercise> PARENT_ROW_MAPPER =
      (rs, rowNum) ->
          new CatalogExercise(
              rs.getString("id"),
              rs.getString("name"),
              Modality.valueOf(rs.getString("modality")),
              rs.getString("movement_pattern"),
              rs.getString("equipment"),
              rs.getObject("default_sets", Integer.class),
              rs.getString("default_reps"),
              rs.getBigDecimal("default_distance_km"),
              rs.getString("default_pace_min_per_km"),
              rs.getString("session_kind"),
              rs.getString("instructions"),
              List.of());

  private final JdbcTemplate jdbcTemplate;

  public JdbcExerciseCatalogRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public List<CatalogExercise> findAll() {
    List<CatalogExercise> parents =
        jdbcTemplate.query(
            "SELECT " + PARENT_COLUMNS + " FROM exercise_catalog ORDER BY id", PARENT_ROW_MAPPER);
    return attachMuscles(parents, allMuscles());
  }

  @Override
  public List<CatalogExercise> findByModality(Modality modality) {
    List<CatalogExercise> parents =
        jdbcTemplate.query(
            "SELECT " + PARENT_COLUMNS + " FROM exercise_catalog WHERE modality = ? ORDER BY id",
            PARENT_ROW_MAPPER,
            modality.name());
    return attachMuscles(parents, allMuscles());
  }

  @Override
  public Optional<CatalogExercise> findById(String id) {
    List<CatalogExercise> parents =
        jdbcTemplate.query(
            "SELECT " + PARENT_COLUMNS + " FROM exercise_catalog WHERE id = ?",
            PARENT_ROW_MAPPER,
            id);
    if (parents.isEmpty()) {
      return Optional.empty();
    }
    List<String> muscles =
        jdbcTemplate.query(
            "SELECT muscle FROM exercise_catalog_muscle WHERE exercise_id = ? ORDER BY ordinal",
            (rs, rowNum) -> rs.getString("muscle"),
            id);
    return Optional.of(withMuscles(parents.get(0), muscles));
  }

  private Map<String, List<String>> allMuscles() {
    Map<String, List<String>> byExerciseId = new LinkedHashMap<>();
    jdbcTemplate.query(
        "SELECT exercise_id, muscle FROM exercise_catalog_muscle ORDER BY exercise_id, ordinal",
        rs -> {
          byExerciseId
              .computeIfAbsent(rs.getString("exercise_id"), key -> new ArrayList<>())
              .add(rs.getString("muscle"));
        });
    return byExerciseId;
  }

  private static List<CatalogExercise> attachMuscles(
      List<CatalogExercise> parents, Map<String, List<String>> musclesByExerciseId) {
    return parents.stream()
        .map(
            parent -> withMuscles(parent, musclesByExerciseId.getOrDefault(parent.id(), List.of())))
        .toList();
  }

  private static CatalogExercise withMuscles(CatalogExercise parent, List<String> muscles) {
    return new CatalogExercise(
        parent.id(),
        parent.name(),
        parent.modality(),
        parent.movementPattern(),
        parent.equipment(),
        parent.defaultSets(),
        parent.defaultReps(),
        parent.defaultDistanceKm(),
        parent.defaultPaceMinPerKm(),
        parent.sessionKind(),
        parent.instructions(),
        muscles);
  }
}
