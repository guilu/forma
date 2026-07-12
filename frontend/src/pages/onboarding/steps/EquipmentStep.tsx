import type { OnboardingAnswers } from '../onboardingStorage';
import styles from './steps.module.css';

/**
 * Equipment step (FOR-59 FR, paired with training availability: "capture
 * days/equipment for plans"). Local draft answer only — no plan-generation
 * backend consumes it yet (bootstrap).
 */
const EQUIPMENT_OPTIONS: readonly string[] = [
  'Sin equipamiento (peso corporal)',
  'Mancuernas',
  'Barra y discos',
  'Bandas elásticas',
  'Máquinas de gimnasio',
  'Cinta o bicicleta estática',
];

interface EquipmentStepProps {
  readonly value: OnboardingAnswers['equipment'];
  readonly onChange: (patch: Partial<OnboardingAnswers['equipment']>) => void;
}

export function EquipmentStep({ value, onChange }: EquipmentStepProps) {
  function toggleItem(item: string) {
    const items = value.items.includes(item)
      ? value.items.filter((i) => i !== item)
      : [...value.items, item];
    onChange({ items });
  }

  return (
    <div className={styles.field}>
      <p className={styles.intro}>¿Qué equipamiento tienes disponible? (opcional)</p>
      <div className={styles.checkboxGroup} role="group" aria-label="Equipamiento disponible">
        {EQUIPMENT_OPTIONS.map((item) => (
          <label key={item} className={styles.checkboxOption}>
            <input
              type="checkbox"
              checked={value.items.includes(item)}
              onChange={() => toggleItem(item)}
            />
            {item}
          </label>
        ))}
      </div>
    </div>
  );
}
