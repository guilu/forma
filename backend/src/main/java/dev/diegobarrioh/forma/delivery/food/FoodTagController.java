package dev.diegobarrioh.forma.delivery.food;

import dev.diegobarrioh.forma.application.FoodTagService;
import dev.diegobarrioh.forma.delivery.ApiPaths;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Labels and which foods carry them (V50) under {@link ApiPaths#V1}.
 *
 * <p>Reads are open to any signed-in account — somebody avoiding gluten needs to see the labels —
 * and setting them is admin only, like the rest of the shared catalog.
 *
 * <p>A food's labels are set with PUT rather than added and removed one at a time. The screen shows
 * every label at once, so it knows the complete answer; a POST-and-DELETE pair would let two people
 * editing together each keep half of what the other did.
 *
 * <p>There is no way to create a label here. The vocabulary is a decision about what the catalog
 * can say, not something a food form should be able to grow by accident — that is how "Vegano" and
 * "vegano" end up side by side.
 */
@RestController
@RequestMapping(ApiPaths.V1)
public class FoodTagController {

  private final FoodTagService service;

  public FoodTagController(FoodTagService service) {
    this.service = service;
  }

  /** Every label there is, in the order a list of checkboxes should show them. */
  @GetMapping("/tags")
  public List<TagResponse> tags() {
    return service.allTags().stream().map(TagResponse::from).toList();
  }

  /** The labels a food carries. */
  @GetMapping("/foods/{foodId}/tags")
  public List<TagResponse> tagsOf(@PathVariable String foodId) {
    return service.tagsOf(foodId).stream().map(TagResponse::from).toList();
  }

  /** Sets a food's labels to exactly those in the body. */
  @PutMapping("/foods/{foodId}/tags")
  @PreAuthorize("hasRole('ADMIN')")
  public List<TagResponse> setTagsOf(
      @PathVariable String foodId, @Valid @RequestBody FoodTagsRequest request) {
    return service.setTagsOf(foodId, request.tagIdsOrEmpty()).stream()
        .map(TagResponse::from)
        .toList();
  }
}
