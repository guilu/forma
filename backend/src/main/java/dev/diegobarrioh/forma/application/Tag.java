package dev.diegobarrioh.forma.application;

/**
 * A label a food can carry (V50).
 *
 * <p>Read model for a {@code tag} row. Deliberately says nothing about what KIND of label it is —
 * "vegano" is a fact about the ingredients, "congelado" about how it was kept, "cena" about when it
 * suits — because the whole point of a tag system is that the schema does not need to know.
 *
 * @param id a slug, stable and readable in a URL; never renamed once foods point at it
 * @param name what a person reads
 * @param sortOrder where it sits in a list of checkboxes, grouped by kind rather than
 *     alphabetically
 * @param enabled whether it is still offered
 */
public record Tag(String id, String name, int sortOrder, boolean enabled) {}
