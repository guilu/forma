import { ButtonLink } from '../../components/ButtonLink';
import styles from './PlanBanner.module.css';

/**
 * "Tu plan está en marcha" banner (FOR-164 dashboard mockup) — made honest
 * about whether that is actually true. It used to render this message
 * unconditionally, so a brand-new account with zero plans was told its plan
 * was under way; the first thing a new user read on the dashboard was false.
 *
 * <p>`hasPlan` is lifted from {@link DashboardPage}'s own `menu` read (the
 * same signal `NutritionWidget` already turns into "No hay un plan de comidas
 * para hoy todavía."), not fetched again here: a second request would ask the
 * server the same question twice and could answer it differently mid-render.
 * `undefined` means that read has not settled yet, and the banner renders
 * nothing rather than show one message and then correct itself.
 *
 * <p>With a plan, the CTA still points at Progreso — the nearest place that
 * actually answers "how am I doing" (it used to link to the retired goals
 * page). Without one, it points at the only in-app place a plan can be
 * created today, `/app/nutrition/plans` — see {@link NoPlanEmptyState} for the
 * training/nutrition-page half of this same fix.
 */
export function PlanBanner({ hasPlan }: { readonly hasPlan: boolean | undefined }) {
  if (hasPlan === undefined) {
    return null;
  }

  if (!hasPlan) {
    return (
      <section className={styles.banner} aria-label="Todavía no tienes un plan">
        <div className={styles.text}>
          <p className={styles.title}>Todavía no tienes un plan</p>
          <p className={styles.subtitle}>Créalo y en cuanto esté en marcha verás aquí tu progreso.</p>
        </div>
        <ButtonLink variant="accent" className={styles.cta} to="/app/nutrition/plans">
          Crear mi plan
        </ButtonLink>
      </section>
    );
  }

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
