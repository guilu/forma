package dev.diegobarrioh.forma.delivery.plan;

import dev.diegobarrioh.forma.application.FoodCatalogService;
import dev.diegobarrioh.forma.application.FoodServingRepository;
import dev.diegobarrioh.forma.application.PlanImportService;
import dev.diegobarrioh.forma.delivery.ApiPaths;
import dev.diegobarrioh.forma.domain.FoodItem;
import dev.diegobarrioh.forma.domain.PlanStatus;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Importing nutrition plans written elsewhere: {@code /api/v1/nutrition/plans/import}.
 *
 * <p>ADMIN ONLY, and for a different reason from the food catalog's. That one is admin because it
 * is shared reference data. This one is admin because a file can carry plans for other people's
 * accounts, and writing into somebody else's account is precisely what being an administrator
 * means. The plan endpoints beside it stay open to every account for their own.
 *
 * <p>Two operations, and the second is what makes the first usable. {@code POST} takes the file.
 * {@code GET /catalog} hands out the vocabulary it may use — every food, its macros and its named
 * portions — because a model asked to write a plan with no list of foods will invent ids, and every
 * one of them will be rejected.
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/nutrition/plans/import")
public class PlanImportController {

  private final PlanImportService importService;
  private final FoodCatalogService foods;
  private final FoodServingRepository servings;

  public PlanImportController(
      PlanImportService importService, FoodCatalogService foods, FoodServingRepository servings) {
    this.importService = importService;
    this.foods = foods;
    this.servings = servings;
  }

  /**
   * Writes every plan in the file, or none of them.
   *
   * <p>A file with one bad line writes nothing and comes back with every fault it has — see {@link
   * PlanImportService}. Plans arrive as {@link PlanStatus#DRAFT}; each account activates its own.
   */
  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.CREATED)
  public PlanImportResult importPlans(@Valid @RequestBody PlanImportRequest request) {
    List<PlanImportService.Entry> entries = new ArrayList<>();
    List<dev.diegobarrioh.forma.application.PlanProblem> unreadable = new ArrayList<>();
    List<PlanImportRequest.Entry> given = request.plans() == null ? List.of() : request.plans();

    for (int at = 0; at < given.size(); at++) {
      PlanImportRequest.Entry entry = given.get(at);
      String path = "plans[%d]".formatted(at);
      try {
        // The domain records refuse impossible states by throwing, which is right for them and
        // wrong
        // here: an import must report what is wrong with line fourteen without stopping at line
        // two.
        // Caught per entry so one malformed plan does not hide the rest.
        entries.add(
            new PlanImportService.Entry(
                entry == null ? null : entry.forUserEmail(),
                entry == null || entry.plan() == null
                    ? null
                    : entry.plan().toPlan(PlanImportService.PLACEHOLDER_OWNER, PlanStatus.DRAFT)));
      } catch (RuntimeException malformed) {
        unreadable.add(
            new dev.diegobarrioh.forma.application.PlanProblem(path, malformed.getMessage()));
        entries.add(new PlanImportService.Entry(entry == null ? null : entry.forUserEmail(), null));
      }
    }
    if (!unreadable.isEmpty()) {
      throw new PlanImportService.ImportRejected(unreadable);
    }

    List<PlanImportResult.Imported> written =
        importService.importAll(entries).stream()
            .map(one -> new PlanImportResult.Imported(one.id(), one.name(), one.forUserEmail()))
            .toList();
    return new PlanImportResult(written);
  }

  /**
   * Every food an import file may name, with its macros and its portions.
   *
   * <p>Open to any signed-in account, unlike the import itself: it is the food catalog, which the
   * whole app already reads. Kept beside the import rather than under {@code /foods} because its
   * shape is the import's — ids first, macros as a nested object — and a caller pasting it into a
   * prompt wants exactly this and not the admin catalog's read model.
   */
  @GetMapping("/catalog")
  public ImportCatalogResponse catalog() {
    List<ImportCatalogResponse.Food> rows =
        foods.allFoods().stream()
            .map(
                food ->
                    ImportCatalogResponse.food(
                        food, preparationOf(food), servings.findByFood(food.id())))
            .toList();
    return new ImportCatalogResponse(rows);
  }

  private String preparationOf(FoodItem food) {
    var preparation = foods.preparationOf(food.id());
    return preparation == null ? null : preparation.name();
  }
}
