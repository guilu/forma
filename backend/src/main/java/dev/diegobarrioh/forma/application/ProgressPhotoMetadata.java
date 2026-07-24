package dev.diegobarrioh.forma.application;

import java.time.Instant;
import java.util.UUID;

/**
 * Metadata for a stored progress photo (FOR-140, progress-photos slice of FOR-104). Deliberately
 * has no binary/URL field: the binary lives behind {@link ProgressPhotoStore}, keyed by {@link
 * #storageRef()}, and retrieval only ever happens through the owner-scoped, access-controlled
 * {@code GET /progress/photos/{id}} endpoint — never a public/static/durable URL (spec FOR-140
 * api.md).
 *
 * @param id the private reference id returned to the caller
 * @param ownerId the owning account (FOR-145b-1: a real {@code users.id UUID}); every
 *     repository/service method is owner-scoped
 * @param contentType the validated content type at upload time (e.g. {@code image/jpeg})
 * @param sizeBytes the binary size in bytes
 * @param createdAt when the photo was uploaded
 * @param storageRef opaque key into {@link ProgressPhotoStore}; internal only, never surfaced as a
 *     client-resolvable URL
 */
public record ProgressPhotoMetadata(
    String id,
    UUID ownerId,
    String contentType,
    long sizeBytes,
    Instant createdAt,
    String storageRef) {}
