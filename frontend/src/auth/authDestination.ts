export interface AuthDestination {
  readonly pathname?: string;
  readonly search?: string;
  readonly hash?: string;
}

/**
 * Builds a same-origin SPA destination from React Router location state.
 * Protocol-relative and non-rooted values are rejected rather than handed to
 * the router as attacker-controlled post-auth redirects.
 */
export function resolveAuthDestination(destination?: AuthDestination): string {
  const pathname = destination?.pathname;
  if (!pathname || !pathname.startsWith('/') || pathname.startsWith('//')) {
    return '/app';
  }
  return `${pathname}${destination.search ?? ''}${destination.hash ?? ''}`;
}
