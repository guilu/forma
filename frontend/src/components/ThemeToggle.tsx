import { useTheme } from '../theme/ThemeContext';
import type { ThemeMode } from '../theme/theme';
import { Button } from './Button';
import styles from './ThemeToggle.module.css';

const OPTIONS: ReadonlyArray<{ readonly mode: ThemeMode; readonly label: string }> = [
  { mode: 'light', label: 'Claro' },
  { mode: 'dark', label: 'Oscuro' },
  { mode: 'system', label: 'Sistema' },
];

/**
 * Light/dark/system toggle (FOR-62), wired into the Ajustes "Tema" row
 * (previously an inert `Próximamente` placeholder left by FOR-58). Built from
 * the shared {@link Button} primitive as a three-way segmented control rather
 * than a new component, so no per-theme styling is introduced (ADR-006:
 * tokens only) — the selected option simply switches to the `primary`
 * variant.
 *
 * <p>Rendered as a labelled `group` of toggle buttons: each option announces
 * its selected state via `aria-pressed` and keeps the app-wide
 * `:focus-visible` outline (spec FOR-62 UI: "labelled control with visible
 * focus; state announced").
 */
export function ThemeToggle() {
  const { mode, setMode } = useTheme();

  return (
    <div className={styles.group} role="group" aria-label="Tema">
      {OPTIONS.map((option) => {
        const selected = mode === option.mode;
        return (
          <Button
            key={option.mode}
            type="button"
            variant={selected ? 'primary' : 'secondary'}
            aria-pressed={selected}
            className={styles.option}
            onClick={() => setMode(option.mode)}
          >
            {option.label}
          </Button>
        );
      })}
    </div>
  );
}
