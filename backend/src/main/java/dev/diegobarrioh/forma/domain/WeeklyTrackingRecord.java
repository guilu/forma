package dev.diegobarrioh.forma.domain;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * One row of the weekly tracking record — the *Seguimiento* sheet (FOR-155, epic FOR-148
 * "Personalizar FORMA a Diego", slice 7 of 7): {@code docs/fitness_os.xlsm} sheet Seguimiento, one
 * row per week.
 *
 * <p>Distinct from two existing, deliberately-not-reused types (spec FOR-155 Data Model Notes):
 *
 * <ul>
 *   <li>{@link BodyMeasurement} is a <em>per-event</em> value (no week concept, no km/ritmo/kcal).
 *   <li>{@link WeeklyCheckIn} is an Insights <em>snapshot</em> assembled on read, not a persisted
 *       user-filled row.
 * </ul>
 *
 * <p>This type is the new, persisted, user-filled per-week aggregate: it captures the whole
 * Seguimiento row (peso, grasa %, masa grasa, masa magra, IMC, km running, ritmo 4 km, kcal
 * recomendadas, comentario) so the FOR-150 pace/hunger/trend rules have a single source to read
 * from. Framework-free (ADR-001).
 *
 * <p><strong>Empty is the default state</strong> (agreed model, spec FOR-155 Summary): the
 * collection starts with zero rows and the user fills one row per week going forward. A partial
 * record (e.g. body metrics without a run, or a run without body metrics) is a normal, valid state
 * (spec FOR-155 Edge Cases) — every field except {@code week}/{@code date} is optional.
 *
 * <p>{@code fatMassKg}/{@code leanMassKg} are intentionally <strong>not</strong> stored fields:
 * like {@link BodyMeasurement#fatMassKg()}/{@link BodyMeasurement#leanMassKg()}, they are derived
 * on demand from {@code weightKg}/{@code bodyFatPercentage} so there is a single source of truth
 * and no stored-vs-derived drift. The Excel's "Masa grasa kg"/"Masa magra kg" columns are exactly
 * these derived values for week 1 (73.6 * 14.7 / 100 = 10.8192 kg ≈ 10.8; 73.6 - 10.8 = 62.8),
 * confirming the derivation matches the source sheet.
 *
 * <p><strong>Kcal recomendadas</strong> is accepted as a plain user-entered/optional value in this
 * slice, not auto-derived from the FOR-149 {@link PersonalTargets#baseCaloriesKcal()} profile
 * value. The spec (Data Model Notes, Open Questions) explicitly defers this choice ("decide in
 * design"); user-entered is the simplest option that satisfies the story without introducing a
 * cross-service dependency (Application layer would need to inject {@code UserProfileService} into
 * a otherwise-independent weekly-tracking use case) not requested by this slice. Deriving a
 * profile-based default is a reasonable, documented follow-up, not implemented here.
 *
 * <p><strong>Heart-rate field</strong>: deliberately <em>not</em> added. FOR-150 rule 4 needs a
 * pace/HR signal, but the Seguimiento sheet only has "Ritmo 4 km" (pace), not FC/ppm (spec FOR-155
 * cross-slice gap flag). Adding a speculative HR field now would be an unrequested abstraction
 * (AGENTS.md "Forbidden shortcuts": no speculative abstractions); FOR-150 design resolved this by
 * gating rule 4 on pace-degradation alone (see {@code PaceDegradationRules}) rather than adding HR
 * support.
 *
 * <p><strong>Hunger field</strong>: also deliberately <em>not</em> added. FOR-150 rule 5 ("Hambre
 * alta: &gt;7/10 varios días") needs a hunger check-in scale, but no such field exists on this
 * record, on {@link WeeklyCheckIn}, or anywhere else in the repository. Same reasoning as the
 * heart-rate field above: fabricating one now would be a speculative, unrequested addition. FOR-150
 * therefore ships without rule 5 — it is gated (documented gap, no stub rule/service added) until a
 * future story captures hunger, matching spec FOR-150 api.md's "rules gated on missing data simply
 * do not appear."
 *
 * @param week the Seguimiento "Semana" number; required, strictly positive; one record per week
 *     (upsert/dedupe behavior lives in {@code WeeklyTrackingRecordRepository})
 * @param date the week's reference date ("Fecha"); required
 * @param weightKg body weight in kilograms ("Peso kg"); optional, strictly positive when present
 * @param bodyFatPercentage body fat percentage ("Grasa %"); optional, within {@code [0, 100]}
 * @param bmi body mass index ("IMC"); optional, strictly positive when present; not derived here
 *     (no height input), mirroring {@link BodyMeasurement#bmi()}
 * @param runningKm running distance for the week in kilometers ("Km running"); optional,
 *     non-negative when present
 * @param pace4kmMinPerKm the 4 km pace ("Ritmo 4 km"), user-entered, formatted {@code mm:ss}
 *     (minutes:seconds per km); optional, must match {@code mm:ss} with seconds in {@code [0, 59]}
 *     when present
 * @param recommendedKcal recommended daily kcal for the week ("Kcal recomendadas"); optional,
 *     user-entered (see class javadoc), non-negative when present
 * @param comment free-text weekly note ("Comentario"); optional, never affects calculation
 */
public record WeeklyTrackingRecord(
    int week,
    LocalDate date,
    Double weightKg,
    Double bodyFatPercentage,
    Double bmi,
    Double runningKm,
    String pace4kmMinPerKm,
    Double recommendedKcal,
    String comment) {

  private static final Pattern PACE_PATTERN = Pattern.compile("^\\d{1,2}:[0-5]\\d$");

  public WeeklyTrackingRecord {
    if (week <= 0) {
      throw new IllegalArgumentException("week must be strictly positive, was: " + week);
    }
    Objects.requireNonNull(date, "date must not be null");
    if (weightKg != null && weightKg <= 0) {
      throw new IllegalArgumentException("weightKg must be strictly positive, was: " + weightKg);
    }
    if (bodyFatPercentage != null && (bodyFatPercentage < 0 || bodyFatPercentage > 100)) {
      throw new IllegalArgumentException(
          "bodyFatPercentage must be within [0, 100], was: " + bodyFatPercentage);
    }
    if (bmi != null && bmi <= 0) {
      throw new IllegalArgumentException("bmi must be strictly positive, was: " + bmi);
    }
    if (runningKm != null && runningKm < 0) {
      throw new IllegalArgumentException("runningKm must not be negative, was: " + runningKm);
    }
    if (recommendedKcal != null && recommendedKcal < 0) {
      throw new IllegalArgumentException(
          "recommendedKcal must not be negative, was: " + recommendedKcal);
    }
    if (pace4kmMinPerKm != null && !PACE_PATTERN.matcher(pace4kmMinPerKm).matches()) {
      throw new IllegalArgumentException(
          "pace4kmMinPerKm must be in mm:ss format (e.g. \"6:00\"), was: " + pace4kmMinPerKm);
    }
  }

  /**
   * Fat mass in kilograms, derived as {@code weightKg * bodyFatPercentage / 100}.
   *
   * @return the derived fat mass, or empty when {@code weightKg} or {@code bodyFatPercentage} is
   *     absent
   */
  public Optional<Double> fatMassKg() {
    if (weightKg == null || bodyFatPercentage == null) {
      return Optional.empty();
    }
    return Optional.of(weightKg * bodyFatPercentage / 100);
  }

  /**
   * Lean mass in kilograms, derived as {@code weightKg - fatMassKg}.
   *
   * @return the derived lean mass, or empty when {@code weightKg} or {@code bodyFatPercentage} is
   *     absent
   */
  public Optional<Double> leanMassKg() {
    return fatMassKg().map(fatMass -> weightKg - fatMass);
  }
}
