import { useId } from 'react';
import { useTheme } from '../theme/ThemeContext';
import type { ThemeMode } from '../theme/theme';
import styles from './ThemeToggle.module.css';

const OPTIONS: ReadonlyArray<{ readonly mode: ThemeMode; readonly label: string }> = [
  { mode: 'light', label: 'Claro' },
  { mode: 'dark', label: 'Oscuro' },
  { mode: 'system', label: 'Sistema' },
];

/**
 * Light/dark/system control (FOR-62), wired into the Ajustes "Tema" row.
 *
 * <p>A radio group since FOR-189, not three toggle buttons. Three mutually
 * exclusive choices of one setting is what a radio group *is*: the platform
 * then gives arrow-key navigation and one tab stop for the set, where the
 * button version made every option its own stop and left "exactly one is
 * selected" to `aria-pressed` on three unrelated controls.
 *
 * <p>The inputs are visually hidden and their labels carry the segmented look,
 * so this stays token-driven with no per-theme styling (ADR-006) while keeping
 * native semantics, keyboard behaviour and focus.
 */
export function ThemeToggle() {
  const { mode, setMode } = useTheme();
  // Scoped, so two groups on one page would not join into a single set.
  const name = useId();

  return (
    <div className={styles.group} role="radiogroup" aria-label="Tema">
      {OPTIONS.map((option) => (
        <label key={option.mode} className={styles.option}>
          <input
            className={styles.input}
            type="radio"
            name={name}
            value={option.mode}
            checked={mode === option.mode}
            onChange={() => setMode(option.mode)}
          />
          <span className={styles.face}>{option.label}</span>
        </label>
      ))}
    </div>
  );
}
