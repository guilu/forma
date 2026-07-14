import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react';
import type { ReactNode } from 'react';
import { Icon, type IconName } from './Icon';
import styles from './NotificationProvider.module.css';

/**
 * Shared notification/feedback pattern (FOR-63): a small provider + `useNotify`
 * hook rendering success/warning/error toasts (spec `specs/FOR-63/ui.md`:
 * "Notification provider + hook (`useNotify`) rendering success/warning/error
 * toasts (dedupe/limit, dismissible)"). Presentational only — callers decide
 * *when* to notify, this component only renders the feedback consistently
 * across modules (AGENTS.md: frontend consumes read models/commands, no
 * feature logic here).
 *
 * <p>Toasts render inside a single `aria-live="polite"` region (FOR-61:
 * "aria-live region wired into FOR-60 states and FOR-63 feedback") so
 * assistive tech announces new feedback without interrupting the user —
 * unlike {@link import('./ErrorState').ErrorState}'s `role="alert"`, which
 * is reserved for a page's own blocking failure, not a transient toast.
 * Each toast also carries a text label (not just an icon/color) so type is
 * conveyed without relying on color alone (spec `specs/FOR-63/ui.md`
 * accessibility requirement).
 */
export type NotificationType = 'success' | 'warning' | 'error';

interface Notification {
  readonly id: string;
  readonly type: NotificationType;
  readonly message: string;
}

interface NotifyApi {
  readonly success: (message: string) => void;
  readonly warning: (message: string) => void;
  readonly error: (message: string) => void;
}

const NotificationContext = createContext<NotifyApi | null>(null);

/** Caps concurrent toasts (spec edge case: "avoid stacking excessive notifications"). */
const MAX_NOTIFICATIONS = 3;
/** Success toasts auto-dismiss; warnings/errors persist until the user dismisses them
 * (spec ui.md: "auto-dismiss for success only" / "errors persist until dismissed"). */
const SUCCESS_AUTO_DISMISS_MS = 5000;

const TYPE_ICON: Record<NotificationType, IconName> = {
  success: 'check',
  warning: 'alertTriangle',
  error: 'alertTriangle',
};

const TYPE_LABEL: Record<NotificationType, string> = {
  success: 'Éxito',
  warning: 'Atención',
  error: 'Error',
};

export function NotificationProvider({ children }: { readonly children: ReactNode }) {
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const nextId = useRef(0);

  const dismiss = useCallback((id: string) => {
    setNotifications((current) => current.filter((notification) => notification.id !== id));
  }, []);

  const push = useCallback((type: NotificationType, message: string) => {
    setNotifications((current) => {
      // De-dupe: a rapid repeat of the exact same notification collapses into
      // the one already shown (spec edge case: "rapid repeated actions ->
      // de-duplicate / limit stacked toasts").
      const alreadyShown = current.some(
        (notification) => notification.type === type && notification.message === message,
      );
      if (alreadyShown) {
        return current;
      }
      const next = [...current, { id: String(nextId.current++), type, message }];
      // Limit: keep only the most recent MAX_NOTIFICATIONS, dropping the oldest.
      return next.length > MAX_NOTIFICATIONS ? next.slice(next.length - MAX_NOTIFICATIONS) : next;
    });
  }, []);

  const api = useMemo<NotifyApi>(
    () => ({
      success: (message: string) => push('success', message),
      warning: (message: string) => push('warning', message),
      error: (message: string) => push('error', message),
    }),
    [push],
  );

  return (
    <NotificationContext.Provider value={api}>
      {children}
      {/* `role="log"` (not "status"): a stacking list of messages, distinct
       * from the single-message `role="status"` already used by LoadingState/
       * EmptyState/SavedIndicator elsewhere on the same page — using "status"
       * here too would make those ambiguous to query and to assistive tech. */}
      <div className={styles.region} role="log" aria-live="polite">
        {notifications.map((notification) => (
          <Toast key={notification.id} notification={notification} onDismiss={dismiss} />
        ))}
      </div>
    </NotificationContext.Provider>
  );
}

function Toast({
  notification,
  onDismiss,
}: {
  readonly notification: Notification;
  readonly onDismiss: (id: string) => void;
}) {
  const { id, type, message } = notification;

  useEffect(() => {
    if (type !== 'success') {
      // Warnings/errors persist until the user dismisses them (spec edge case:
      // "long-running failure -> transitions to an error message, not a
      // stuck pending state" — the state must stay visible, not vanish on a timer).
      return undefined;
    }
    const timer = setTimeout(() => onDismiss(id), SUCCESS_AUTO_DISMISS_MS);
    return () => clearTimeout(timer);
  }, [id, type, onDismiss]);

  return (
    <div className={styles.toast} data-type={type}>
      <Icon name={TYPE_ICON[type]} size={18} className={styles.icon} />
      <p className={styles.message}>
        <span className={styles.srOnly}>{TYPE_LABEL[type]}: </span>
        {message}
      </p>
      <button
        type="button"
        className={styles.dismiss}
        onClick={() => onDismiss(id)}
        aria-label={`Descartar notificación: ${message}`}
      >
        ×
      </button>
    </div>
  );
}

// Hook lives alongside its provider so consumers get a single import; fast
// refresh still works, it just also reloads this hook when the file changes
// (same tradeoff already accepted by ThemeContext.tsx, FOR-62).
// eslint-disable-next-line react-refresh/only-export-components
export function useNotify(): NotifyApi {
  const ctx = useContext(NotificationContext);
  if (!ctx) {
    throw new Error('useNotify must be used within a NotificationProvider');
  }
  return ctx;
}
