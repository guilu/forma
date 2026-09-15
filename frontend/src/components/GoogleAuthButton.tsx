import { getApiBaseUrl } from '../api/client';
import buttonStyles from './Button.module.css';
import styles from './GoogleAuthButton.module.css';

/**
 * "Continuar con Google" (ADR-014), shown on both {@code LoginPage} and
 * {@code RegisterPage} above the email/password form. A real top-level
 * navigation to the backend's authorization endpoint — not a `fetch`, so it
 * is unaffected by the API client's CSRF/CORS handling — which is why this is
 * a plain `<a href>` rather than a click handler.
 *
 * <p>Composes {@link buttonStyles}' `surface` variant directly (docs/ui-guidelines.md:
 * an external `<a href>` "cannot be a ButtonLink") — same visual family as
 * every other action in the app, at full width.
 */
export function GoogleAuthButton() {
  const href = `${getApiBaseUrl()}/api/oauth2/authorization/google`;

  return (
    <a
      href={href}
      className={[buttonStyles.button, buttonStyles.surface, styles.googleButton].join(' ')}
    >
      <svg
        className={styles.googleIcon}
        viewBox="0 0 24 24"
        xmlns="http://www.w3.org/2000/svg"
        aria-hidden="true"
      >
        <path
          d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
          fill="#4285F4"
        />
        <path
          d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
          fill="#34A853"
        />
        <path
          d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l3.66-2.84z"
          fill="#FBBC05"
        />
        <path
          d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
          fill="#EA4335"
        />
      </svg>
      Continuar con Google
    </a>
  );
}

/** The "o" divider between the Google button and the email/password form. */
export function OrDivider() {
  return (
    <div className={styles.divider} role="separator">
      <span className={styles.dividerLine} />
      <span>o</span>
      <span className={styles.dividerLine} />
    </div>
  );
}
