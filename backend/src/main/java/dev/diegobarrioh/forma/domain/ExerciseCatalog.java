package dev.diegobarrioh.forma.domain;

import java.util.List;
import java.util.Optional;

/**
 * The initial strength exercise catalog (FOR-24): a fixed set of home-friendly exercises covering
 * push, pull, legs (squat/hinge) and core.
 *
 * <p>Defined in code with stable ids rather than as persisted seed data — no strength persistence
 * exists yet and templates (FOR-25) only need to reference exercises by a stable id (spec FOR-24
 * Open Questions, consistent with the FOR-23 in-code running plan). No migration is introduced.
 * Every exercise uses only home {@link Equipment}; there are no machine/gym exercises.
 *
 * <p><b>FOR-154 additions:</b> dumbbell bench press, lateral raise, biceps curl, rear-delt fly and
 * calf raise were added so {@link WorkoutTemplateCatalog} can reference Diego's real 5-exercise
 * blocks (sheet <em>Fuerza</em>). {@code bench-dip} stays in the catalog even though no current
 * template references it — reference data is independent of which templates happen to use it.
 */
public final class ExerciseCatalog {

  private static final List<Exercise> EXERCISES =
      List.of(
          // Push
          new Exercise(
              "push-up",
              "Flexiones",
              MovementPattern.PUSH,
              List.of("pecho", "tríceps", "hombro anterior"),
              Equipment.BODYWEIGHT,
              "Cuerpo recto, baja el pecho hasta cerca del suelo y empuja hacia arriba."),
          new Exercise(
              "dumbbell-shoulder-press",
              "Press de hombro con mancuernas",
              MovementPattern.PUSH,
              List.of("hombro", "tríceps"),
              Equipment.DUMBBELL,
              "Sentado o de pie, empuja las mancuernas por encima de la cabeza sin arquear la espalda."),
          new Exercise(
              "bench-dip",
              "Fondos en banco",
              MovementPattern.PUSH,
              List.of("tríceps", "pecho"),
              Equipment.BENCH,
              "Manos en el banco, baja los codos a 90 grados y empuja hacia arriba."),
          new Exercise(
              "dumbbell-bench-press",
              "Press de banca con mancuernas",
              MovementPattern.PUSH,
              List.of("pecho", "tríceps", "hombro anterior"),
              Equipment.DUMBBELL,
              "Tumbado en el banco, empuja las mancuernas desde el pecho hasta extender los brazos."),
          new Exercise(
              "lateral-raise",
              "Elevaciones laterales",
              MovementPattern.PUSH,
              List.of("hombro lateral"),
              Equipment.DUMBBELL,
              "De pie, eleva las mancuernas hacia los lados hasta la altura del hombro, codos ligeramente flexionados."),
          // Pull
          new Exercise(
              "pull-up",
              "Dominadas",
              MovementPattern.PULL,
              List.of("dorsal", "bíceps"),
              Equipment.PULL_UP_BAR,
              "Cuelga de la barra y tira hasta que la barbilla supere la barra."),
          new Exercise(
              "dumbbell-row",
              "Remo con mancuerna",
              MovementPattern.PULL,
              List.of("dorsal", "romboides", "bíceps"),
              Equipment.DUMBBELL,
              "Con la espalda plana, tira de la mancuerna hacia la cadera y baja controlado."),
          new Exercise(
              "band-face-pull",
              "Face pull con banda",
              MovementPattern.PULL,
              List.of("deltoides posterior", "trapecio"),
              Equipment.BAND,
              "Tira de la banda hacia la cara separando las manos al final del movimiento."),
          new Exercise(
              "biceps-curl",
              "Curl de bíceps",
              MovementPattern.PULL,
              List.of("bíceps"),
              Equipment.DUMBBELL,
              "De pie, flexiona los codos elevando las mancuernas sin balancear el torso."),
          new Exercise(
              "rear-delt-fly",
              "Pájaros posteriores",
              MovementPattern.PULL,
              List.of("deltoides posterior"),
              Equipment.DUMBBELL,
              "Inclinado hacia delante, abre los brazos con las mancuernas hasta la altura de los hombros."),
          // Legs (squat / hinge)
          new Exercise(
              "goblet-squat",
              "Sentadilla goblet",
              MovementPattern.SQUAT,
              List.of("cuádriceps", "glúteo"),
              Equipment.DUMBBELL,
              "Sujeta la mancuerna al pecho y baja en sentadilla manteniendo el torso erguido."),
          new Exercise(
              "dumbbell-rdl",
              "Peso muerto rumano con mancuernas",
              MovementPattern.HINGE,
              List.of("isquiotibiales", "glúteo"),
              Equipment.DUMBBELL,
              "Bisagra de cadera bajando las mancuernas por delante de las piernas con la espalda recta."),
          new Exercise(
              "reverse-lunge",
              "Zancada hacia atrás",
              MovementPattern.SQUAT,
              List.of("cuádriceps", "glúteo"),
              Equipment.BODYWEIGHT,
              "Da un paso atrás y baja la rodilla trasera cerca del suelo; alterna piernas."),
          new Exercise(
              "calf-raise",
              "Elevación de gemelos",
              MovementPattern.SQUAT,
              List.of("gemelos"),
              Equipment.BODYWEIGHT,
              "De pie, elévate sobre la punta de los pies y baja controlado sin rebotar."),
          // Core
          new Exercise(
              "plank",
              "Plancha",
              MovementPattern.CORE,
              List.of("core", "abdomen"),
              Equipment.BODYWEIGHT,
              "Antebrazos y puntas de los pies, cuerpo recto, mantén sin hundir la cadera."),
          new Exercise(
              "dead-bug",
              "Dead bug",
              MovementPattern.CORE,
              List.of("core", "abdomen"),
              Equipment.BODYWEIGHT,
              "Boca arriba, extiende brazo y pierna opuestos sin arquear la zona lumbar."));

  private ExerciseCatalog() {}

  /** All catalog exercises (immutable). */
  public static List<Exercise> exercises() {
    return EXERCISES;
  }

  /** Finds an exercise by its stable id. */
  public static Optional<Exercise> findById(String id) {
    return EXERCISES.stream().filter(exercise -> exercise.id().equals(id)).findFirst();
  }
}
