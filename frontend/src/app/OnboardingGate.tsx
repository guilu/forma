import { useEffect } from 'react';
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

  useEffect(() => {
    if (location.pathname.startsWith('/onboarding')) {
      return;
    }
    let active = true;
    getProfile()
      .then((profile) => {
        if (active && !profile.firstRunCompleted) {
          navigate('/onboarding', { replace: true });
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
  }, [location.pathname, navigate]);

  return null;
}
