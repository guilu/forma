import { useState, type FormEvent } from 'react';
import { ApiRequestError } from '../api/client';
import { createBodyMeasurement, type BodyMeasurement } from '../api/bodyMeasurements';
import styles from './MeasurementForm.module.css';

/**
 * Body measurement entry form (FOR-18). Collects a manual measurement and submits
 * it to the FOR-17 API through the shared client. The frontend owns no domain
 * rules (ADR-006): it validates required fields for fast feedback, but derived
 * values and authoritative validation live in the backend.
 *
 * <p>Surface-agnostic: it renders just the form, so the caller provides the
 * container (e.g. the Measurements page opens it inside a {@link Modal}).
 */
interface MeasurementFormProps {
  /** Called after a successful save so a list/dashboard (FOR-19) can refresh. */
  readonly onCreated?: (measurement: BodyMeasurement) => void;
  /** When provided, renders a "Cancelar" action that invokes this (e.g. close a modal). */
  readonly onCancel?: () => void;
}

type FieldName = 'measuredAt' | 'weightKg' | 'bodyFatPercentage' | 'bmi';
type FieldErrors = Partial<Record<FieldName, string>>;
type Status = 'idle' | 'pending' | 'success' | 'error';

const REQUIRED_MESSAGE = 'Este campo es obligatorio.';
const NUMBER_MESSAGE = 'Introduce un número válido.';
const GENERIC_ERROR = 'No se pudo guardar la medición. Inténtalo de nuevo.';

const EMPTY_VALUES = {
  measuredAt: '',
  weightKg: '',
  bodyFatPercentage: '',
  bmi: '',
  notes: '',
};

function validate(values: typeof EMPTY_VALUES): FieldErrors {
  const errors: FieldErrors = {};
  const requiredNumbers: FieldName[] = ['weightKg', 'bodyFatPercentage', 'bmi'];

  if (!values.measuredAt) {
    errors.measuredAt = REQUIRED_MESSAGE;
  }
  for (const field of requiredNumbers) {
    const raw = values[field];
    if (!raw) {
      errors[field] = REQUIRED_MESSAGE;
    } else if (!Number.isFinite(Number(raw))) {
      errors[field] = NUMBER_MESSAGE;
    }
  }
  return errors;
}

export function MeasurementForm({ onCreated, onCancel }: MeasurementFormProps) {
  const [values, setValues] = useState(EMPTY_VALUES);
  const [errors, setErrors] = useState<FieldErrors>({});
  const [status, setStatus] = useState<Status>('idle');
  const [submitError, setSubmitError] = useState<string | undefined>(undefined);

  const pending = status === 'pending';

  function update(field: keyof typeof EMPTY_VALUES, value: string) {
    setValues((current) => ({ ...current, [field]: value }));
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const nextErrors = validate(values);
    setErrors(nextErrors);
    setSubmitError(undefined);
    if (Object.keys(nextErrors).length > 0) {
      return;
    }

    setStatus('pending');
    try {
      const created = await createBodyMeasurement({
        // datetime-local yields local wall-clock time; send an absolute UTC instant.
        measuredAt: new Date(values.measuredAt).toISOString(),
        weightKg: Number(values.weightKg),
        bodyFatPercentage: Number(values.bodyFatPercentage),
        bmi: Number(values.bmi),
        notes: values.notes.trim() ? values.notes.trim() : undefined,
      });
      setStatus('success');
      setValues(EMPTY_VALUES);
      onCreated?.(created);
    } catch (error) {
      setStatus('error');
      setSubmitError(error instanceof ApiRequestError ? error.message : GENERIC_ERROR);
    }
  }

  return (
    <form className={styles.form} onSubmit={handleSubmit} noValidate>
      <Field
        id="measuredAt"
        label="Fecha y hora"
        type="datetime-local"
        value={values.measuredAt}
        error={errors.measuredAt}
        disabled={pending}
        onChange={(value) => update('measuredAt', value)}
      />
      <Field
        id="weightKg"
        label="Peso (kg)"
        type="number"
        value={values.weightKg}
        error={errors.weightKg}
        disabled={pending}
        onChange={(value) => update('weightKg', value)}
      />
      <Field
        id="bodyFatPercentage"
        label="Grasa corporal (%)"
        type="number"
        value={values.bodyFatPercentage}
        error={errors.bodyFatPercentage}
        disabled={pending}
        onChange={(value) => update('bodyFatPercentage', value)}
      />
      <Field
        id="bmi"
        label="IMC"
        type="number"
        value={values.bmi}
        error={errors.bmi}
        disabled={pending}
        onChange={(value) => update('bmi', value)}
      />

      <div className={styles.field}>
        <label className={styles.label} htmlFor="notes">
          Notas (opcional)
        </label>
        <textarea
          id="notes"
          className={styles.textarea}
          value={values.notes}
          disabled={pending}
          rows={3}
          onChange={(event) => update('notes', event.target.value)}
        />
      </div>

      {submitError && (
        <p className={styles.formError} role="alert">
          {submitError}
        </p>
      )}
      {status === 'success' && (
        <output className={styles.success}>Medición guardada correctamente.</output>
      )}

      <div className={styles.actions}>
        {onCancel && (
          <button className={styles.cancel} type="button" onClick={onCancel} disabled={pending}>
            Cancelar
          </button>
        )}
        <button className={styles.submit} type="submit" disabled={pending}>
          {pending ? 'Guardando…' : 'Guardar medición'}
        </button>
      </div>
    </form>
  );
}

interface FieldProps {
  readonly id: FieldName;
  readonly label: string;
  readonly type: 'number' | 'datetime-local';
  readonly value: string;
  readonly error?: string;
  readonly disabled: boolean;
  readonly onChange: (value: string) => void;
}

/** A labelled input with an associated inline error (ADR-006: errors close to fields). */
function Field({ id, label, type, value, error, disabled, onChange }: FieldProps) {
  const errorId = `${id}-error`;
  return (
    <div className={styles.field}>
      <label className={styles.label} htmlFor={id}>
        {label}
      </label>
      <input
        id={id}
        className={styles.input}
        type={type}
        value={value}
        disabled={disabled}
        step={type === 'number' ? 'any' : undefined}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? errorId : undefined}
        onChange={(event) => onChange(event.target.value)}
      />
      {error && (
        <p id={errorId} className={styles.fieldError}>
          {error}
        </p>
      )}
    </div>
  );
}
