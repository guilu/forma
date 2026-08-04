package dev.diegobarrioh.forma.delivery.plan;

import java.util.List;
import java.util.UUID;

/**
 * What an import did.
 *
 * <p>Only ever returned when the whole file was good: a file with one bad line writes nothing, so
 * there is no partial outcome to describe. The failure case is a 400 carrying every problem found,
 * not a 200 carrying some.
 *
 * @param imported the plans written, in the file's own order
 */
public record PlanImportResult(List<Imported> imported) {

  /**
   * @param id the plan's new id
   * @param name what it is called
   * @param forUserEmail the account it went to
   */
  public record Imported(UUID id, String name, String forUserEmail) {}
}
