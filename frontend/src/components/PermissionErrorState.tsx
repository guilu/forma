import type { ReactNode } from 'react';
import { Icon } from './Icon';
import styles from './PermissionErrorState.module.css';

/**
 * Shared permission/access-error state (FOR-60): a distinct state from
 * {@link ErrorState} for "you can't see this" rather than "loading failed" —
 * the MVP is currently single-user (ADR-002/AGENTS.md "bypassing
 * authorization because the MVP is currently single-user" is forbidden, not
 * "this state doesn't exist"), so this exists ahead of multi-user auth so
 * future stories have a ready target instead of overloading `ErrorState`.
 *
 * <p>Deliberately has no built-in retry: a permission error will not resolve
 * by repeating the same request (edge case in `specs/FOR-60/spec.md`:
 * "Permission error (future auth) → distinct from a generic error"). Callers
 * may still pass a domain-appropriate `action` (e.g. a "Volver" link) when
 * one makes sense.
 */
interface PermissionErrorStateProps {
  readonly title?: string;
  readonly message?: string;
  readonly action?: ReactNode;
}

const DEFAULT_TITLE = 'Acceso restringido';
const DEFAULT_MESSAGE = 'No tienes permiso para ver este contenido.';

export function PermissionErrorState({
  title = DEFAULT_TITLE,
  message = DEFAULT_MESSAGE,
  action,
}: PermissionErrorStateProps) {
  return (
    <div className={styles.wrapper} role="alert">
      <Icon name="lock" size={28} className={styles.icon} />
      <p className={styles.title}>{title}</p>
      <p className={styles.message}>{message}</p>
      {action && <div className={styles.action}>{action}</div>}
    </div>
  );
}
