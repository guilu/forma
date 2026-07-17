package dev.diegobarrioh.forma.delivery.progress;

import dev.diegobarrioh.forma.application.ProgressPhotoMetadata;
import java.time.Instant;

/**
 * Response DTO for a single progress photo's metadata (FOR-140 api.md). Deliberately carries no URL
 * field — {@link ProgressPhotoMetadata#storageRef()} is internal-only and never surfaced here;
 * retrieval always goes through the owner-scoped {@code GET /progress/photos/{id}} endpoint.
 */
public record ProgressPhotoResponse(
    String id, String contentType, long sizeBytes, Instant createdAt) {

  public static ProgressPhotoResponse from(ProgressPhotoMetadata metadata) {
    return new ProgressPhotoResponse(
        metadata.id(), metadata.contentType(), metadata.sizeBytes(), metadata.createdAt());
  }
}
