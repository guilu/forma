package dev.diegobarrioh.forma.adapter.scheduling;

import dev.diegobarrioh.forma.application.PlanLeadRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Deletes leads once they are older than the period the privacy notice declares.
 *
 * <p>This exists because the notice says twelve months. A declared retention period is a promise
 * under article 13.2(a), and the only thing that makes it true is code that deletes — «we keep it
 * until you ask us to remove it» is what the notice would say if this class did not exist, and that
 * is the answer the GDPR does not accept.
 *
 * <p>A driving adapter, not application logic: what it does is call one port method. The trigger is
 * the clock, which is infrastructure, and the rule it enforces lives in the notice.
 *
 * <p><b>Safe to run twice.</b> Deleting rows already gone is a no-op, so several instances running
 * this at the same time need no lock between them — they just find less to do. That is why there is
 * no leader election here, and why adding one would be machinery for a problem this job does not
 * have.
 *
 * <p>It logs how many rows went, never which: the count is an operational fact, the rows are
 * personal data, and a deletion log that names what it deleted defeats the deletion.
 */
@Component
public class PlanLeadRetentionJob {

  private static final Logger log = LoggerFactory.getLogger(PlanLeadRetentionJob.class);

  /** Twelve months, the period declared in the privacy notice. Change both or neither. */
  static final int RETENTION_MONTHS = 12;

  private final PlanLeadRepository repository;
  private final Clock clock;

  public PlanLeadRetentionJob(PlanLeadRepository repository, Clock clock) {
    this.repository = repository;
    this.clock = clock;
  }

  /**
   * Runs nightly. The hour is arbitrary and quiet; the frequency is not — a period declared in
   * months is not honoured by a job that runs monthly, because a lead that ages out on the 2nd
   * would sit there until the 1st of the following month.
   */
  @Scheduled(cron = "${forma.plan-lead.retention-cron:0 15 3 * * *}", zone = "UTC")
  public void deleteExpiredLeads() {
    // Meses de calendario, no múltiplos de 30 días: el aviso dice «doce meses» y 360 días
    // borrarían casi una semana antes de lo declarado. `Instant` no sabe restar meses —no tienen
    // duración fija— así que la cuenta pasa por un calendario y vuelve.
    Instant cutoff =
        clock.instant().atZone(ZoneOffset.UTC).minusMonths(RETENTION_MONTHS).toInstant();
    int deleted = repository.deleteOlderThan(cutoff);
    if (deleted > 0) {
      log.info("Retención de leads: {} borrados por superar {} meses", deleted, RETENTION_MONTHS);
    }
  }
}
