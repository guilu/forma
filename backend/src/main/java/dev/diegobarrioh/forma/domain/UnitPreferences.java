package dev.diegobarrioh.forma.domain;

/**
 * The user's unit preferences (FOR-107, spec FOR-58's Ajustes mockup): weight, height, distance and
 * energy units.
 *
 * <p>The MVP ships the metric set ({@link WeightUnit#KG}, {@link HeightUnit#CM}, {@link
 * DistanceUnit#KM}, {@link EnergyUnit#KCAL}) as the default and only supported value per dimension
 * (spec FOR-107 Functional Requirements), but each dimension is a real, independently persisted
 * enum — not a hardcoded constant — so FOR-119 can add a unit selector later without a data-model
 * rewrite. A missing field on construction (e.g. a partial update payload) defaults to its metric
 * value rather than nulling the preference out, mirroring {@link ShoppingProduct}'s category
 * default-on-null pattern (FOR-106).
 *
 * @param weightUnit the preferred weight unit; defaults to {@link WeightUnit#KG} when {@code null}
 * @param heightUnit the preferred height unit; defaults to {@link HeightUnit#CM} when {@code null}
 * @param distanceUnit the preferred distance unit; defaults to {@link DistanceUnit#KM} when {@code
 *     null}
 * @param energyUnit the preferred energy unit; defaults to {@link EnergyUnit#KCAL} when {@code
 *     null}
 */
public record UnitPreferences(
    WeightUnit weightUnit,
    HeightUnit heightUnit,
    DistanceUnit distanceUnit,
    EnergyUnit energyUnit) {

  public UnitPreferences {
    if (weightUnit == null) {
      weightUnit = WeightUnit.KG;
    }
    if (heightUnit == null) {
      heightUnit = HeightUnit.CM;
    }
    if (distanceUnit == null) {
      distanceUnit = DistanceUnit.KM;
    }
    if (energyUnit == null) {
      energyUnit = EnergyUnit.KCAL;
    }
  }

  /** The metric default set, used before any preference has been saved. */
  public static final UnitPreferences DEFAULT =
      new UnitPreferences(WeightUnit.KG, HeightUnit.CM, DistanceUnit.KM, EnergyUnit.KCAL);
}
