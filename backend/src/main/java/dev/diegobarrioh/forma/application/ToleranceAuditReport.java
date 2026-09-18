package dev.diegobarrioh.forma.application;

import java.util.List;

/**
 * The result of one {@link PlanToleranceAuditor#audit} run (ADR-015 slice 4): every comparison it
 * made, day-level and plan-level, whether it agreed, disagreed, or had nothing to compare.
 *
 * <p>Every entry is kept, not only the failing ones — {@link ToleranceVerdict#NOT_CLAIMED} has to
 * survive into the report for a plan with no targets to read as "nothing to compare" instead of a
 * silent pass, and a plan that agrees everywhere needs its {@link
 * ToleranceVerdict#WITHIN_TOLERANCE} entries on record for the same reason a clean audit still gets
 * a report. Slice 6 persists this whole shape into {@code plan_request.validation_report} (TEXT,
 * V63); a caller wanting only the problems uses {@link #drifts()}.
 *
 * @param entries every comparison this audit made
 */
public record ToleranceAuditReport(List<ToleranceEntry> entries) {

  public ToleranceAuditReport {
    entries = entries == null ? List.of() : List.copyOf(entries);
  }

  /** Whether anything exceeded its tolerance. */
  public boolean hasDrift() {
    return entries.stream()
        .anyMatch(entry -> entry.verdict() == ToleranceVerdict.EXCEEDS_TOLERANCE);
  }

  /**
   * Whether the plan claimed anything at all to check.
   *
   * <p>{@code false} together with {@link #hasDrift()} being {@code false} is a plan with no {@code
   * target_*} anywhere — nothing was verified, which is not the same claim as "verified and
   * correct".
   */
  public boolean anythingClaimed() {
    return entries.stream().anyMatch(entry -> entry.verdict() != ToleranceVerdict.NOT_CLAIMED);
  }

  /** Only the entries that exceeded their tolerance — the findings worth showing a human. */
  public List<ToleranceEntry> drifts() {
    return entries.stream()
        .filter(entry -> entry.verdict() == ToleranceVerdict.EXCEEDS_TOLERANCE)
        .toList();
  }
}
