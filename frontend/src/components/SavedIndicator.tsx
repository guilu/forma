import { Icon } from './Icon';
import styles from './SavedIndicator.module.css';

/**
 * Shared inline "saved" confirmation (FOR-63): standardizes the ad-hoc
 * `<output>Producto actualizado.</output>` pattern (`ShoppingPage`'s
 * `ProductEditModal`) into one reusable, calm field-level save indicator
 * (spec `specs/FOR-63/ui.md`: "Inline confirmation (field-level save) +
 * saved/unsaved indicator").
 *
 * <p>`role="status"` (passive announcement, like {@link LoadingState}/
 * {@link EmptyState}) — a successful save is not a failure and must not
 * interrupt the user the way `role="alert"` would.
 */
interface SavedIndicatorProps {
  readonly message?: string;
}

export function SavedIndicator({ message = 'Guardado.' }: SavedIndicatorProps) {
  return (
    <output className={styles.indicator} role="status">
      <Icon name="check" size={16} className={styles.icon} />
      {message}
    </output>
  );
}
