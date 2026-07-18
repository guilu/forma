package dev.diegobarrioh.forma.application;

/**
 * The binary content of a progress photo together with its stored content type (FOR-140), returned
 * by {@link ProgressPhotoService#retrieve} for the owner-scoped {@code GET /progress/photos/{id}}
 * endpoint to stream back with the correct {@code Content-Type} header. Never logged in full — see
 * {@link ProgressPhotoService} javadoc.
 */
public record ProgressPhotoContent(String contentType, byte[] content) {}
