package dev.diegobarrioh.forma.delivery.progress;

import dev.diegobarrioh.forma.application.ProgressPhotoMetadata;
import java.util.List;

/** Response DTO for {@code GET /progress/photos} (FOR-140 api.md): metadata only, no binary/URL. */
public record ProgressPhotoListResponse(List<ProgressPhotoResponse> photos) {

  public static ProgressPhotoListResponse from(List<ProgressPhotoMetadata> photos) {
    return new ProgressPhotoListResponse(photos.stream().map(ProgressPhotoResponse::from).toList());
  }
}
