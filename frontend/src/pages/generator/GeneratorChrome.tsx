import type { ReactNode } from 'react';
import { Icon } from '../../components/Icon';
import styles from './PlanGenerator.module.css';

/**
 * Las piezas que se repiten en los cuatro pasos: el indicador de progreso, las
 * tarjetas que se eligen y los bloques con candado.
 */
export const STEPS = [
  { key: 'paciente', label: 'Tus datos' },
  { key: 'clinico', label: 'Objetivo' },
  { key: 'preferencias', label: 'Preferencias' },
  { key: 'contacto', label: 'Tu plan' },
] as const;

export function Stepper({ current }: { readonly current: number }) {
  return (
    <ol className={styles.stepper} aria-label="Progreso">
      {STEPS.map((step, index) => {
        const done = index < current;
        const now = index === current;
        return (
          <li key={step.key} className={styles.step} aria-current={now ? 'step' : undefined}>
            <span className={done ? styles.dotDone : now ? styles.dotNow : styles.dot}>
              {done ? <Icon name="check" size={14} /> : index + 1}
            </span>
            <span className={styles.stepLabel}>{step.label}</span>
          </li>
        );
      })}
    </ol>
  );
}

/**
 * Una opción que se elige tocándola.
 *
 * <p>Un `radio` de verdad y no un `div` con `onClick`: el teclado y los lectores de
 * pantalla ya saben qué es un grupo de opciones excluyentes, y reimplementarlo peor
 * es la forma más común de dejar fuera a alguien.
 */
export function ChoiceCard({
  name,
  value,
  checked,
  onSelect,
  glyph,
  title,
  description,
  disabled,
}: {
  readonly name: string;
  readonly value: string;
  readonly checked: boolean;
  readonly onSelect: (value: string) => void;
  readonly glyph?: string;
  readonly title: string;
  readonly description?: string;
  readonly disabled?: boolean;
}) {
  return (
    <label className={checked ? styles.choiceOn : styles.choice} data-disabled={disabled}>
      <input
        type="radio"
        className={styles.choiceInput}
        name={name}
        value={value}
        checked={checked}
        disabled={disabled}
        onChange={() => onSelect(value)}
      />
      {glyph && (
        <span className={styles.choiceGlyph} aria-hidden="true">
          {glyph}
        </span>
      )}
      <span className={styles.choiceText}>
        <span className={styles.choiceTitle}>{title}</span>
        {description && <span className={styles.choiceDescription}>{description}</span>}
      </span>
      {checked && (
        <span className={styles.choiceCheck} aria-hidden="true">
          <Icon name="check" size={14} />
        </span>
      )}
    </label>
  );
}

/**
 * Lo que se desbloquea al suscribirse.
 *
 * <p>Enseña que existe y no lo pide, que es exactamente para lo que sirve. En el caso
 * de las patologías eso resuelve además el problema legal: son datos de salud, y no se
 * recogen — se enseña que el plan de pago los tiene en cuenta.
 *
 * <p>No es un botón ni un desplegable: no hay nada que abrir. Un candado que al pulsarlo
 * no hace nada enfada más que un candado que se ve cerrado.
 */
export function LockedTeaser({
  title,
  examples,
}: {
  readonly title: string;
  readonly examples: string;
}) {
  return (
    <div className={styles.locked}>
      <span className={styles.lockedIcon} aria-hidden="true">
        <Icon name="lock" size={16} />
      </span>
      <span className={styles.lockedText}>
        <span className={styles.lockedTitle}>{title}</span>
        <span className={styles.lockedExamples}>{examples}</span>
      </span>
    </div>
  );
}

/** El resumen del cálculo, tal y como lo enseña el mockup: una fila por paso. */
export function EnergyBreakdown({ rows }: { readonly rows: ReactNode }) {
  return <dl className={styles.breakdown}>{rows}</dl>;
}

export function BreakdownRow({
  label,
  value,
  total,
}: {
  readonly label: string;
  readonly value: string;
  readonly total?: boolean;
}) {
  return (
    <div className={total ? styles.breakdownTotal : styles.breakdownRow}>
      <dt>{label}</dt>
      <dd>{value}</dd>
    </div>
  );
}
