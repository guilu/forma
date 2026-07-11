import type { InputHTMLAttributes, ReactNode, SelectHTMLAttributes } from 'react';
import styles from './FormField.module.css';

/**
 * Reusable form-field primitives (FOR-50). Extracted from the input/label/error
 * styling that {@link MeasurementForm} already established, so future forms get
 * the same labelled-input + inline-error pattern (ADR-006: "forms must display
 * validation errors close to fields") without re-deriving it per feature.
 * `MeasurementForm` itself is left untouched — the styling is aligned via the
 * same tokens/classes shape, not a shared runtime dependency, to avoid touching
 * already-tested, unrelated code for this story.
 */
interface FieldChromeProps {
  readonly id: string;
  readonly label: string;
  readonly error?: string;
}

function errorIdOf(id: string): string {
  return `${id}-error`;
}

interface TextFieldProps
  extends FieldChromeProps, Omit<InputHTMLAttributes<HTMLInputElement>, 'id' | 'className'> {}

export function TextField({ id, label, error, ...rest }: TextFieldProps) {
  const errorId = errorIdOf(id);
  return (
    <div className={styles.field}>
      <label className={styles.label} htmlFor={id}>
        {label}
      </label>
      <input
        id={id}
        className={styles.input}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? errorId : undefined}
        {...rest}
      />
      {error && (
        <p id={errorId} className={styles.fieldError}>
          {error}
        </p>
      )}
    </div>
  );
}

interface SelectFieldProps
  extends FieldChromeProps, Omit<SelectHTMLAttributes<HTMLSelectElement>, 'id' | 'className'> {
  readonly children: ReactNode;
}

export function SelectField({ id, label, error, children, ...rest }: SelectFieldProps) {
  const errorId = errorIdOf(id);
  return (
    <div className={styles.field}>
      <label className={styles.label} htmlFor={id}>
        {label}
      </label>
      <select
        id={id}
        className={styles.select}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? errorId : undefined}
        {...rest}
      >
        {children}
      </select>
      {error && (
        <p id={errorId} className={styles.fieldError}>
          {error}
        </p>
      )}
    </div>
  );
}
