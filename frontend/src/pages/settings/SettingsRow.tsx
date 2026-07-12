import { Badge } from '../../components/Badge';
import { Icon } from '../../components/Icon';
import styles from './SettingsRow.module.css';

/**
 * Shared row primitive for the Ajustes screen (FOR-58). Page-local — like
 * `dashboard/WidgetSection` — because only Ajustes needs this exact
 * "label + value + entry-point affordance" shape.
 *
 * <p>Renders as a plain, non-interactive `<div>` (never a `<button>`/`<a>`) so
 * every row using it is inherently un-clickable: there is no destination
 * screen or backend action to send the user to yet. `inert` adds the chevron
 * + "Próximamente" badge that visually signals "this will be an entry point
 * later" without pretending it works today (spec FOR-58 edge case: unsupported
 * options must render inert, never as a broken/fake action).
 */
interface SettingsRowProps {
  readonly label: string;
  readonly description?: string;
  readonly value?: string;
  readonly inert?: boolean;
}

export function SettingsRow({ label, description, value, inert = false }: SettingsRowProps) {
  return (
    <div className={styles.row} data-readonly={!inert || undefined}>
      <div className={styles.text}>
        <span className={styles.label}>{label}</span>
        {description && <span className={styles.description}>{description}</span>}
      </div>
      <div className={styles.trailing}>
        {value && <span className={styles.value}>{value}</span>}
        {inert && (
          <>
            <Badge tone="neutral">Próximamente</Badge>
            <Icon name="chevron" size={16} className={styles.chevron} />
          </>
        )}
      </div>
    </div>
  );
}
