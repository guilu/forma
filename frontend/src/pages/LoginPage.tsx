import { useState, type FormEvent } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { Brand } from '../components/Brand';
import { Button } from '../components/Button';
import { TextField } from '../components/FormField';
import styles from './AuthPage.module.css';

interface AuthLocationState {
  readonly from?: { readonly pathname?: string; readonly search?: string };
}

export function LoginPage() {
  const auth = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const destination = (location.state as AuthLocationState | null)?.from;
  const target = `${destination?.pathname ?? '/'}${destination?.search ?? ''}`;
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string>();

  if (auth.status === 'authenticated') return <Navigate to={target} replace />;

  async function submit(event: FormEvent) {
    event.preventDefault();
    setPending(true);
    setError(undefined);
    try {
      await auth.login({ email, password });
      navigate(target, { replace: true });
    } catch {
      setError('No se pudo iniciar la sesión. Inténtalo de nuevo.');
    } finally {
      setPending(false);
    }
  }

  return (
    <main className={styles.page}>
      <section className={styles.card} aria-labelledby="login-title">
        <header className={styles.header}>
          <Brand />
          <h1 id="login-title" className={styles.title}>
            Iniciar sesión
          </h1>
        </header>
        <form className={styles.form} onSubmit={submit}>
          <TextField
            id="login-email"
            label="Correo electrónico"
            type="email"
            autoComplete="email"
            required
            value={email}
            onChange={(event) => setEmail(event.target.value)}
          />
          <TextField
            id="login-password"
            label="Contraseña"
            type="password"
            autoComplete="current-password"
            required
            value={password}
            onChange={(event) => setPassword(event.target.value)}
          />
          {error && (
            <p className={styles.error} role="alert">
              {error}
            </p>
          )}
          <Button type="submit" loading={pending}>
            Iniciar sesión
          </Button>
        </form>
        <p className={styles.footer}>
          ¿No tienes cuenta?{' '}
          <Link className={styles.link} to="/registro" state={location.state}>
            Crear cuenta
          </Link>
        </p>
      </section>
    </main>
  );
}
