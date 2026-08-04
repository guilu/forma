package dev.diegobarrioh.forma.application;

/**
 * Something wrong with a plan, and where.
 *
 * <p>Two fields rather than one sentence, because the API already has a shape for exactly this —
 * {@code ApiError.FieldValidationError} — and concatenating them here would mean the delivery layer
 * had to split them back apart with a regular expression to fill it in.
 *
 * @param path where it is, e.g. {@code plans[0].days[2].meals[1].items[0]}
 * @param message what is wrong, in words somebody can act on
 */
public record PlanProblem(String path, String message) {}
