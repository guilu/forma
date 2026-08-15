import { ButtonLink } from '../../components/ButtonLink';
import styles from './PlanBanner.module.css';

/**
 * "Tu plan está en marcha" call-to-action banner (FOR-164 dashboard mockup):
 * an encouraging accent banner. Static content — pure navigation + copy, no
 * data.
 *
 * <p>It used to link to the goals page. With that feature retired from the UI,
 * the CTA points at Progreso — the nearest place that actually answers "how am
 * I doing" — rather than being dropped, which would leave an encouraging banner
 * with nothing to encourage the user towards.
 */
export function PlanBanner() {
  return (
    <section className={styles.banner} aria-label="Tu plan está en marcha">
      <div className={styles.text}>
        <p className={styles.title}>Tu plan está en marcha 🚀</p>
        <p className={styles.subtitle}>Cada pequeño paso te acerca a tu mejor versión.</p>
      </div>
      <ButtonLink variant="accent" className={styles.cta} to="/app/progress">
        Ver mi progreso
      </ButtonLink>
    </section>
  );
}
