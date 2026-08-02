package dev.diegobarrioh.forma.delivery.store;

/**
 * The photo a linked page advertises (FOR-200), or nothing.
 *
 * <p>An object rather than a bare string so "we looked and found none" is a 200 with a null field
 * instead of a 404 — the page was read fine, it simply publishes no image, and the screen says
 * something different for each.
 */
public record LinkImageResponse(String imageUrl) {}
