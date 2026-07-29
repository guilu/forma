import { useEffect, useRef, type RefObject } from 'react';

/**
 * A ref that tells whether the component is still mounted, for guarding a
 * `setState` in a promise callback that may resolve after unmount.
 *
 * <p>The subtlety this exists to contain: the flag has to be set on the way
 * *in* as well as cleared on the way out. React's StrictMode mounts, unmounts
 * and remounts every component in development, so an effect that only cleared
 * the flag left it `false` for the remount — the second fetch then resolved
 * into a `setState` the guard discarded, and the card sat on its loading state
 * for the rest of the session. Both settings sections shipped that bug.
 */
export function useMountedRef(): RefObject<boolean> {
  const mountedRef = useRef(true);

  useEffect(() => {
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
    };
  }, []);

  return mountedRef;
}
