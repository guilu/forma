package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.User;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Importing a file of nutrition plans (admin).
 *
 * <p>ALL OR NOTHING, and every problem at once. The file is checked whole before a single row is
 * written, and a bad one comes back with every fault it has rather than the first — because the
 * thing writing these files is a language model, and telling it one problem per attempt turns a
 * five-mistake file into five round trips.
 *
 * <p>The checks themselves are not new ones: they are {@link NutritionPlanService}'s own, read in
 * collecting mode instead of throwing mode. A second copy of "does this food exist" would be a
 * second answer to it.
 */
@Service
public class PlanImportService {

  private final NutritionPlanService plans;
  private final UserRepository users;

  public PlanImportService(NutritionPlanService plans, UserRepository users) {
    this.plans = plans;
    this.users = users;
  }

  /**
   * Stands in for the owner while a plan is being read, before the email has been resolved.
   *
   * <p>{@code NutritionPlan} refuses a null userId, and rightly — a plan with no owner is not a
   * thing. But the file names its owner by email, and resolving that is a lookup that can fail, so
   * the plan has to exist before its owner does. This is the placeholder it wears in between, and
   * it never reaches the database: every plan is rewritten under its real owner before being
   * written.
   */
  public static final UUID PLACEHOLDER_OWNER =
      UUID.fromString("00000000-0000-0000-0000-000000000000");

  /** One plan from the file: who it is for, and the plan itself. */
  public record Entry(String forUserEmail, NutritionPlan plan) {}

  /** A plan that was written. */
  public record Written(UUID id, String name, String forUserEmail) {}

  /**
   * Raised when the file is not importable. Carries every problem found, not the first.
   *
   * <p>Its own exception rather than {@link ValidationException} because that one holds a sentence
   * and this one holds a list, and flattening the list into a sentence is exactly what makes an
   * error message useless to whoever has to act on it.
   */
  public static class ImportRejected extends RuntimeException {

    private final transient List<PlanProblem> problems;

    public ImportRejected(List<PlanProblem> problems) {
      super("El fichero no se puede importar: " + problems.size() + " problema(s).");
      this.problems = List.copyOf(problems);
    }

    public List<PlanProblem> problems() {
      return problems;
    }
  }

  /**
   * Writes every plan in the file, or none of them.
   *
   * <p>Transactional as well as pre-checked, and the two are not the same guarantee. The check
   * catches what can be seen by reading; the transaction catches what cannot — a food deleted
   * between the check and the write, or the fourteenth plan failing on something nobody predicted.
   *
   * @throws ImportRejected when anything at all is wrong, carrying all of it
   */
  @Transactional
  public List<Written> importAll(List<Entry> entries) {
    if (entries == null || entries.isEmpty()) {
      throw new ImportRejected(
          List.of(new PlanProblem("plans", "El fichero no trae ningún plan.")));
    }

    List<PlanProblem> problems = new ArrayList<>();
    List<UUID> owners = new ArrayList<>();
    for (int at = 0; at < entries.size(); at++) {
      Entry entry = entries.get(at);
      String path = "plans[%d]".formatted(at);
      owners.add(ownerOf(entry, path, problems));
      if (entry.plan() != null) {
        problems.addAll(plans.problemsIn(entry.plan(), path));
      }
    }
    if (!problems.isEmpty()) {
      throw new ImportRejected(problems);
    }

    List<Written> written = new ArrayList<>();
    for (int at = 0; at < entries.size(); at++) {
      Entry entry = entries.get(at);
      NutritionPlan stored = plans.create(forOwner(entry.plan(), owners.get(at)));
      written.add(new Written(stored.id(), stored.name(), entry.forUserEmail()));
    }
    return written;
  }

  /**
   * The account a plan is for, or null with a problem recorded.
   *
   * <p>By email, because that is the thing about a person whoever writes the file already knows. An
   * unknown one is reported as unknown rather than created: importing a plan is not a way to make
   * accounts, and a typo in an email would otherwise leave a silent orphan nobody can sign into.
   */
  private UUID ownerOf(Entry entry, String path, List<PlanProblem> problems) {
    if (entry == null || entry.forUserEmail() == null || entry.forUserEmail().isBlank()) {
      problems.add(new PlanProblem(path + ".forUserEmail", "Falta el correo de la cuenta."));
      return null;
    }
    if (entry.plan() == null) {
      problems.add(new PlanProblem(path + ".plan", "Falta el plan."));
      return null;
    }
    return users
        .findByEmail(entry.forUserEmail().trim())
        .map(User::id)
        .orElseGet(
            () -> {
              problems.add(
                  new PlanProblem(
                      path + ".forUserEmail",
                      "No existe ninguna cuenta con el correo " + entry.forUserEmail() + "."));
              return null;
            });
  }

  /**
   * The plan under its owner, as a DRAFT.
   *
   * <p>Always a draft, whatever the file asked for. Somebody's diet changing because a file was
   * uploaded would be a surprising thing for an import to do, and the one-active-plan rule already
   * has a single home in {@code activate} — reaching it from here would be a second door onto it.
   */
  private static NutritionPlan forOwner(NutritionPlan plan, UUID userId) {
    return new NutritionPlan(
        null,
        userId,
        plan.name(),
        plan.description(),
        plan.objective(),
        dev.diegobarrioh.forma.domain.PlanStatus.DRAFT,
        plan.startDate(),
        plan.endDate(),
        plan.targets(),
        plan.generation(),
        plan.days());
  }
}
