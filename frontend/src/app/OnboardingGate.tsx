import { useEffect, useRef } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { getProfile } from '../api/profile';

/**
 * Sends an authenticated user who has not completed the first-run onboarding
 * wizard to `/onboarding`, once per profile check, from wherever they land
 * authenticated.
 *
 * <p><b>Why the flag, not the plan</b>: the criterion is the profile's own
 * {@code firstRunCompleted} flag (FOR-121, {@link getProfile}), not whether
 * the account has an active plan — a returning user who finished onboarding
 * but never started a plan must land on `/app` like anyone else and see that
 * screen's own empty state, not be sent back into the wizard. Once
 * `firstRunCompleted` is `true` it never flips back (see the domain
 * `UserProfileService`'s one-way OR), so a completed user is never
 * redirected again.
 *
 * <p><b>Mounted in `AppShell`</b> (same placement as {@link
 * PlanActivationGate}), not in `RequireAuth`: every way of arriving
 * authenticated — email login, register, Google login (whose backend success
 * handler already lands hard on `/app`) and reopening the app with a live
 * session — resolves through `RequireAuth` into `/app`, which mounts this
 * shell. `/onboarding` itself is a sibling route, not nested under
 * `AppShell`, so this component simply never mounts while the user is
 * already there — the `location.pathname` guard below is a second,
 * belt-and-suspenders check for the same rule, not the only thing enforcing
 * it.
 *
 * <p><b>Runs once per mount, not once per navigation</b>: `AppShell` mounts
 * once per session (it wraps every `/app/*` route behind a single `<Outlet
 * />`, per `app/routes.tsx`), so the profile check — and the redirect
 * decision it drives — only needs to happen once. The effect's dependency
 * array is empty; both `location.pathname` and `navigate` are captured once
 * into refs at mount instead of listed as dependencies, so navigating
 * between in-app routes (`/app` -&gt; `/app/nutrition` -&gt; ...) does not
 * re-trigger it and fire another `GET /api/v1/profile` for a decision that
 * is already settled. `navigate` specifically has to go through a ref
 * (not just be omitted): this app uses the classic `<BrowserRouter>` (see
 * `main.tsx`), not a data router, and under that mode `useNavigate()`
 * returns a *new function identity on every navigation* — it closes over
 * the current location to resolve relative paths — so leaving it in the
 * effect's own dependency array would silently reintroduce the same
 * per-navigation re-fetch this fix removes. Capturing it once is safe here
 * because this component only ever navigates to the fixed absolute path
 * `/onboarding`, whose resolution does not depend on which render produced
 * the closure.
 *
 * <p><b>No trap</b>: this only ever adds a redirect *into* onboarding, never
 * removes a way out of it — the flow's own "Ahora no, ir al panel" exit
 * (visible on every step, see `OnboardingPage.tsx`) and its completion
 * screen's "Ir al panel" both mark the flag done and navigate back to
 * `/app`, where this gate then leaves the user alone.
 *
 * <p><b>Fails open</b>: a failed profile check does not redirect — same
 * choice {@link PlanActivationGate} makes for its own check. A broken
 * network call must never lock a user out of the app behind a screen they
 * cannot leave.
 */
export function OnboardingGate() {
  const navigate = useNavigate();
  const location = useLocation();
  // Captured once, at mount — see class doc "Runs once per mount". Reading
  // these from refs instead of listing `navigate`/`location.pathname` as
  // effect dependencies is what keeps the check from re-running on every
  // in-app navigation.
  const navigateRef = useRef(navigate);
  const initialPathnameRef = useRef(location.pathname);

  useEffect(() => {
    if (initialPathnameRef.current.startsWith('/onboarding')) {
      return;
    }
    let active = true;
    getProfile()
      .then((profile) => {
        if (active && !profile.firstRunCompleted) {
          navigateRef.current('/onboarding', { replace: true });
        }
      })
      .catch(() => {
        // Fail open — see class doc. The screens behind /app already handle
        // "no plan yet"/empty states, which is what an unfinished first run
        // would otherwise look like anyway.
      });
    return () => {
      active = false;
    };
  }, []);

  return null;
}
