import styles from './ValidationError.module.css';

/**
 * Shared inline validation-error state (FOR-60): the field-level error
 * paragraph `FormField`'s `TextField`/`SelectField` already rendered inline,
 * extracted so every current and future form uses the exact same element and
 * styling instead of re-deriving it (ADR-006: "forms must display validation
 * errors close to fields").
 *
 * <p>Deliberately carries no ARIA role of its own — the field it belongs to
 * links it via `aria-describedby` (see `FormField.tsx`), which is the
 * pattern already established there; adding `role="alert"` here would cause
 * a duplicate announcement when the invalid field receives focus.
 */
interface ValidationErrorProps {
  readonly id: string;
  readonly message: string;
}

export function ValidationError({ id, message }: ValidationErrorProps) {
  return (
    <p id={id} className={styles.error}>
      {message}
    </p>
  );
}
