import { Link } from 'react-router-dom';
import styles from './PlanBanner.module.css';

/**
 * "Tu plan está en marcha" call-to-action banner (FOR-164 dashboard mockup):
 * an encouraging accent banner linking to the goals page. Static content — pure
 * navigation + copy, no data.
 */
export function PlanBanner() {
  return (
    <section className={styles.banner} aria-label="Tu plan está en marcha">
      <div className={styles.text}>
        <p className={styles.title}>Tu plan está en marcha 🚀</p>
        <p className={styles.subtitle}>Cada pequeño paso te acerca a tu mejor versión.</p>
      </div>
      <Link className={styles.cta} to="/objetivos">
        Ver mis objetivos
      </Link>
    </section>
  );
}
