import { useState, type FormEvent } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { Button } from '../components/Button';
import { TextField } from '../components/FormField';
import styles from './AuthPage.module.css';
import { resolveAuthDestination, type AuthDestination } from '../auth/authDestination';

export function RegisterPage() {
  const auth = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const state = location.state as { from?: AuthDestination } | null;
  const target = resolveAuthDestination(state?.from);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmation, setConfirmation] = useState('');
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string>();
  const [confirmationError, setConfirmationError] = useState<string>();

  if (auth.status === 'authenticated') return <Navigate to={target} replace />;

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (password !== confirmation) {
      setConfirmationError('Las contraseñas no coinciden.');
      return;
    }
    setConfirmationError(undefined);
    setPending(true);
    setError(undefined);
    try {
      await auth.register({ email, password });
      navigate(target, { replace: true });
    } catch {
      setError('No se pudo crear la cuenta. Revisa los datos e inténtalo de nuevo.');
    } finally {
      setPending(false);
    }
  }

  return (
    <main id="main-content" tabIndex={-1} className={styles.page}>
      <section className={styles.card} aria-labelledby="register-title">
        {/* FOR-185: no brand lockup here — the global navigation bar above the
            page already carries it. */}
        <header className={styles.header}>
          <h1 id="register-title" className={styles.title}>
            Crear cuenta
          </h1>
        </header>
        <form className={styles.form} onSubmit={submit}>
          <TextField
            id="register-email"
            label="Correo electrónico"
            type="email"
            autoComplete="email"
            required
            value={email}
            onChange={(event) => setEmail(event.target.value)}
          />
          <TextField
            id="register-password"
            label="Contraseña"
            type="password"
            autoComplete="new-password"
            minLength={12}
            required
            value={password}
            onChange={(event) => setPassword(event.target.value)}
          />
          <TextField
            id="register-confirm-password"
            label="Confirmar contraseña"
            type="password"
            autoComplete="new-password"
            required
            value={confirmation}
            error={confirmationError}
            onChange={(event) => setConfirmation(event.target.value)}
          />
          {error && (
            <p className={styles.error} role="alert">
              {error}
            </p>
          )}
          <Button type="submit" loading={pending}>
            Crear cuenta
          </Button>
        </form>
        <p className={styles.footer}>
          ¿Ya tienes cuenta?{' '}
          <Link className={styles.link} to="/login" state={location.state}>
            Iniciar sesión
          </Link>
        </p>
      </section>
    </main>
  );
}
