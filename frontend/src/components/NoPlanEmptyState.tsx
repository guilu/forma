import { ButtonLink } from './ButtonLink';
import { EmptyState } from './EmptyState';

/**
 * What training, nutrition and the shopping list show when there is no plan behind them.
 *
 * <p>One component and not three copies of the same sentence: the three screens are empty for the
 * same reason, and a user who reads a different explanation on each would reasonably conclude they
 * are three different problems.
 *
 * <p>The way out is the public generator — the same funnel the landing page sends people to, by
 * relative route rather than by absolute URL so it stays inside whichever environment is running.
 */
export function NoPlanEmptyState() {
  return (
    <EmptyState
      title="No existe ningún plan planificado."
      description="Crea uno y verás aquí tus entrenamientos, tus comidas y tu lista de la compra."
      action={
        <ButtonLink to="/plan">Crea tu plan gratis</ButtonLink>
      }
    />
  );
}
