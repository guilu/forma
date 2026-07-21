import { Icon } from '../../components/Icon';
import { WidgetSection } from './WidgetSection';
import styles from './TipWidget.module.css';

/**
 * "Consejo del día" widget (FOR-164 dashboard mockup): a short motivational
 * wellness tip. The tip copy is static generic advice (not personalised health
 * data — there is no tips endpoint), the same honest static-content precedent
 * as {@link SyncWidget}. The carousel dots are a visual affordance only; there
 * is a single tip, so they don't paginate anything yet.
 */
const TIP =
  'La hidratación es clave para tu rendimiento. Bebe agua a lo largo del día, no solo cuando tengas sed.';

export function TipWidget() {
  return (
    <WidgetSection id="tip-widget-title" title="Consejo del día">
      <div className={styles.card}>
        <Icon name="nutrition" size={40} className={styles.illustration} />
        <p className={styles.tip}>{TIP}</p>
        <div className={styles.dots} aria-hidden="true">
          <span className={styles.dotActive} />
          <span className={styles.dot} />
          <span className={styles.dot} />
        </div>
      </div>
    </WidgetSection>
  );
}
