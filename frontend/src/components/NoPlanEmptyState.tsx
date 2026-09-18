import { ButtonLink } from './ButtonLink';
import { EmptyState } from './EmptyState';

/**
 * What training and nutrition show when there is no plan behind them.
 *
 * <p>One component and not two copies of the same sentence: the two screens are empty for the same
 * reason, and a user who reads a different explanation on each would reasonably conclude they are
 * two different problems. The shopping list does NOT use this component any more — an empty list
 * there means "no list generated yet", not "no plan", and this component's own message would have
 * been wrong for that screen (see the comment above `ShoppingPage`'s empty state).
 *
 * <p>The CTA used to be the public generator, `/plan` — the same funnel the landing page sends
 * anonymous visitors to. For a signed-in user that was a dead end: `PlanGeneratorController` says of
 * itself that the plan, the PDF and the mail it produces "do not exist yet", and `V61__plan_lead.sql`
 * keeps its leads deliberately unlinked from `users`, so filling that form left the account no better
 * off. It now points at the one in-app place a plan can actually be created today,
 * `/app/nutrition/plans` ("+ Plan" on {@link PlansPage}).
 */
export function NoPlanEmptyState() {
  return (
    <EmptyState
      title="No existe ningún plan planificado."
      description="Crea uno y verás aquí tus entrenamientos, tus comidas y tu lista de la compra."
      action={<ButtonLink to="/app/nutrition/plans">Crear mi plan</ButtonLink>}
    />
  );
}
