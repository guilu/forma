import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { Brand } from '../components/Brand';
import { Button } from '../components/Button';
import { TextField } from '../components/FormField';
import { ThemeToggle } from '../components/ThemeToggle';
import styles from './LandingPage.module.css';

const BENEFITS = [
  {
    title: 'Composición corporal',
    description:
      'Registra medidas y observa tendencias para entender tu progreso más allá del peso.',
  },
  {
    title: 'Planificación conectada',
    description: 'Organiza entrenamiento y nutrición con una visión común de tus días y objetivos.',
  },
  {
    title: 'Decisiones explicables',
    description:
      'Reúne compra, progreso e insights claros para decidir qué acción tiene sentido después.',
  },
] as const;

export function LandingPage() {
  const auth = useAuth();

  return (
    <div className={styles.page}>
      <a className={styles.skipLink} href="#contenido-principal">
        Saltar al contenido principal
      </a>
      <header className={styles.header}>
        <nav className={styles.nav} aria-label="Navegación pública">
          <Link className={styles.brandLink} to="/" aria-label="FORMA, inicio">
            <Brand />
          </Link>
          <div className={styles.anchorLinks}>
            <a href="#beneficios">Beneficios</a>
            <a href="#producto">Producto</a>
          </div>
          <div className={styles.navActions}>
            <ThemeToggle />
            {auth.status === 'authenticated' ? (
              <Link className={styles.primaryLink} to="/app">
                Ir a la aplicación
              </Link>
            ) : (
              <a className={styles.secondaryLink} href="#acceso">
                Iniciar sesión
              </a>
            )}
          </div>
        </nav>
      </header>

      <main id="contenido-principal">
        <section className={styles.hero} aria-labelledby="landing-title">
          <div className={styles.heroCopy}>
            <p className={styles.eyebrow}>Tu sistema personal de salud y forma física</p>
            <h1 id="landing-title">Entiende tu progreso. Organiza lo que viene.</h1>
            <p className={styles.lead}>
              FORMA reúne composición corporal, entrenamiento, nutrición, compra e insights
              explicables en un único lugar.
            </p>
            <a className={styles.textLink} href="#beneficios">
              Descubre cómo funciona
            </a>
          </div>
          <AccessCard />
        </section>

        <section id="beneficios" className={styles.section} aria-labelledby="benefits-title">
          <div className={styles.sectionHeading}>
            <p className={styles.eyebrow}>Beneficios</p>
            <h2 id="benefits-title">Una visión completa, sin falsas promesas</h2>
          </div>
          <div className={styles.benefitGrid}>
            {BENEFITS.map((benefit, index) => (
              <article className={styles.benefitCard} key={benefit.title}>
                <span className={styles.cardIndex} aria-hidden="true">
                  0{index + 1}
                </span>
                <h3>{benefit.title}</h3>
                <p>{benefit.description}</p>
              </article>
            ))}
          </div>
        </section>

        <section id="producto" className={styles.product} aria-labelledby="product-title">
          <div className={styles.productCopy}>
            <p className={styles.eyebrow}>Producto</p>
            <h2 id="product-title">La información importante, conectada</h2>
            <p>
              Consulta tu semana, revisa tendencias y convierte tus planes en acciones concretas.
              Cada módulo mantiene su propósito y comparte el contexto que necesitas.
            </p>
            <ul className={styles.capabilityList}>
              <li>Mediciones y progreso en el tiempo</li>
              <li>Entrenamiento y nutrición por día</li>
              <li>Lista de compra e insights semanales</li>
            </ul>
          </div>
          <ProductPreview />
        </section>

        <section className={styles.cta} aria-labelledby="cta-title">
          <div>
            <p className={styles.eyebrow}>Empieza con FORMA</p>
            <h2 id="cta-title">Pon orden en tu progreso personal</h2>
            <p>Empieza con tus datos reales y construye una visión que puedas explicar.</p>
          </div>
          {auth.status === 'authenticated' ? (
            <Link className={styles.primaryLink} to="/app">
              Ir a la aplicación
            </Link>
          ) : (
            <Link className={styles.primaryLink} to="/registro">
              Crear tu cuenta
            </Link>
          )}
        </section>
      </main>

      <footer className={styles.footer}>
        <div>
          <Brand />
          <p>Salud, entrenamiento y nutrición con decisiones explicables.</p>
        </div>
        <nav aria-label="Navegación del pie">
          <a href="#beneficios">Beneficios</a>
          <a href="#producto">Producto</a>
          {auth.status === 'authenticated' ? (
            <Link to="/app">Aplicación</Link>
          ) : (
            <Link to="/login">Iniciar sesión</Link>
          )}
        </nav>
        <p>© {new Date().getFullYear()} FORMA</p>
      </footer>
    </div>
  );
}

function AccessCard() {
  const auth = useAuth();

  if (auth.status === 'authenticated') {
    return (
      <div role="region" id="acceso" className={styles.accessCard} aria-labelledby="access-title">
        <p className={styles.eyebrow}>Sesión activa</p>
        <h2 id="access-title">Tu espacio está preparado</h2>
        <p className={styles.account}>{auth.user?.email}</p>
        <Link className={styles.primaryLink} to="/app">
          Ir a la aplicación
        </Link>
      </div>
    );
  }

  if (auth.bootstrapError) {
    return (
      <div role="region" id="acceso" className={styles.accessCard} aria-labelledby="access-title">
        <h2 id="access-title">Accede a FORMA</h2>
        <p role="status">No pudimos comprobar tu sesión.</p>
        <p className={styles.muted}>Puedes iniciar sesión igualmente desde la página de acceso.</p>
        <Link className={styles.primaryLink} to="/login">
          Ir a iniciar sesión
        </Link>
      </div>
    );
  }

  if (auth.status === 'loading') {
    return (
      <div
        role="region"
        id="acceso"
        className={styles.accessCard}
        aria-labelledby="access-title"
        aria-busy="true"
      >
        <h2 id="access-title">Accede a FORMA</h2>
        <p role="status">Comprobando tu sesión…</p>
        <p className={styles.muted}>Mientras tanto, puedes explorar todo el contenido público.</p>
      </div>
    );
  }

  return <LoginCard />;
}

function LoginCard() {
  const auth = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string>();

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (pending) return;
    setPending(true);
    setError(undefined);
    try {
      await auth.login({ email, password });
      navigate('/app');
    } catch {
      setError('No se pudo iniciar la sesión. Inténtalo de nuevo.');
    } finally {
      setPending(false);
    }
  }

  return (
    <div role="region" id="acceso" className={styles.accessCard} aria-labelledby="access-title">
      <p className={styles.eyebrow}>Acceso personal</p>
      <h2 id="access-title">Inicia sesión</h2>
      <form className={styles.loginForm} onSubmit={submit} aria-busy={pending || undefined}>
        <TextField
          id="landing-email"
          label="Correo electrónico"
          type="email"
          autoComplete="email"
          required
          disabled={pending}
          value={email}
          onChange={(event) => setEmail(event.target.value)}
        />
        <TextField
          id="landing-password"
          label="Contraseña"
          type="password"
          autoComplete="current-password"
          required
          disabled={pending}
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
      <p className={styles.registerPrompt}>
        ¿Aún no tienes cuenta? <Link to="/registro">Crear cuenta</Link>
      </p>
    </div>
  );
}

function ProductPreview() {
  return (
    <div className={styles.preview} aria-label="Vista conceptual de FORMA">
      <div className={styles.previewHeader}>
        <span>Resumen semanal</span>
        <span className={styles.status}>En curso</span>
      </div>
      <div className={styles.previewGrid}>
        <div className={styles.previewCard}>
          <span>Entrenamiento</span>
          <strong>Semana organizada</strong>
          <div className={styles.progressTrack} aria-hidden="true">
            <span className={styles.progressValue} />
          </div>
        </div>
        <div className={styles.previewCard}>
          <span>Nutrición</span>
          <strong>Plan de hoy</strong>
          <p>Objetivos y comidas en contexto</p>
        </div>
        <div className={styles.previewCard}>
          <span>Progreso</span>
          <strong>Tendencias visibles</strong>
          <div className={styles.bars} aria-hidden="true">
            <i />
            <i />
            <i />
            <i />
            <i />
          </div>
        </div>
      </div>
    </div>
  );
}
