import { useState, type FormEvent, type ReactNode } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { Button } from '../components/Button';
import { ButtonLink } from '../components/ButtonLink';
import { Icon, type IconName } from '../components/Icon';
import { TextField } from '../components/FormField';
import styles from './LandingPage.module.css';

/**
 * Public landing page (FOR-185), a translation of the `docs/0-landing.html`
 * mockup into this app's stack: CSS Modules instead of CDN Tailwind, the
 * shared `Icon` set instead of the Material Symbols webfont, self-hosted fonts
 * (`main.tsx`) instead of a Google Fonts link, and design tokens instead of
 * the inline `tailwind.config` palette.
 *
 * <p>The mockup's top navigation is *not* rendered here: FOR-185 promoted it
 * to the global {@link Topbar}, which now sits above every route (see
 * `layout/RootLayout.tsx`). This file owns everything below the bar.
 *
 * <p>The hero's login card is the same session-aware component the previous
 * landing had — it swaps between the login form, a "session active" shortcut,
 * a loading state and a bootstrap-failure fallback — restyled to the mockup's
 * glass card.
 */

interface Feature {
  readonly id: string;
  readonly icon: IconName;
  readonly tone: string;
  readonly title: string;
  readonly description: string;
}

const FEATURES: readonly Feature[] = [
  {
    id: 'training',
    icon: 'training',
    tone: styles.tonePrimary,
    title: 'Entrenamientos de Élite',
    description:
      'Rutinas adaptadas dinámicamente a tu nivel actual, fatiga y objetivos específicos de rendimiento.',
  },
  {
    id: 'nutrition',
    icon: 'nutrition',
    tone: styles.toneSecondary,
    title: 'Nutrición Inteligente',
    description:
      'Planes de comida automáticos y control de macros simplificado. Sincroniza tus compras con un toque.',
  },
  {
    id: 'analisis',
    icon: 'progress',
    tone: styles.toneTertiary,
    title: 'Análisis Avanzado',
    description:
      'Visualiza tu progreso con gráficas de alta densidad y métricas de composición corporal precisas.',
  },
];

/*
 * Only claim integrations that exist. FORMA imports body measurements from
 * Withings and from nowhere else: `IntegrationService` registers one
 * `ProviderMeasuresGateway` per provider and Withings is the only provider that
 * has one — Google Fit and Apple Health are listed as providers with no gateway
 * behind them, and Garmin appears nowhere in the codebase at all.
 *
 * This block used to promise "datos en tiempo real de tus wearables favoritos
 * (Apple Watch, Garmin, Withings)" and "Sincronización nativa con Withings &
 * HealthKit". Three of those four are integrations FORMA does not have, and the
 * sync that does exist is a button someone presses, not a live feed. A landing
 * page is where a visitor decides whether to trust the product; promising an
 * import that never arrives is the fastest way to spend that trust.
 *
 * <p>The same rule removed two more claims from this section: a "98% Precisión"
 * badge over the phone mockup, which no measurement in this repository backs —
 * a concrete number reads as data, not as enthusiasm — and "Algoritmo de
 * recuperación propietario", which oversells the two rule-based recommendation
 * services that actually run (`TrainingAdherenceRecommendationService`,
 * `PaceDegradationRecommendationService`).
 */
const SYNC_CAPABILITIES = [
  'Sincronización con Withings',
  'Actualizaciones de peso y grasa semanales',
] as const;

const FOOTER_SECTIONS = [
  { title: 'Producto', links: ['Funciones', 'Precios', 'Apps'] },
  { title: 'Comunidad', links: ['Historias de éxito', 'Blog', 'Eventos'] },
  { title: 'Legal', links: ['Privacidad', 'Términos', 'Cookies'] },
] as const;

export function LandingPage() {
  const auth = useAuth();

  return (
    <>
      <main id="main-content" tabIndex={-1} className={styles.page}>
        <section className={styles.hero} aria-labelledby="landing-title">
          {/* Decorative grid mesh behind the hero (template: `.hero-mesh`). */}
          <div className={styles.heroMesh} aria-hidden="true" />
          <div className={styles.heroGrid}>
            <div className={styles.heroCopy}>
              <p className={styles.badge}>
                <span className={styles.badgeDot} aria-hidden="true" />
                {/* Wrapped rather than left as a bare text node: JSX collapses
                    the newline into a single space, which reads ambiguously
                    next to a self-closing sibling (sonar typescript:S6772) and
                    would make the label an anonymous flex item. */}
                {/* Una capacidad real del producto, no un número de versión: el
                    badge decía "Nueva Versión 4.0 Disponible" con APP_VERSION en
                    0.0.1, y un anuncio que el propio producto desmiente vale
                    menos que nada. La lista de la compra sí existe —catálogo,
                    precios y pasillos de Mercadona— y es lo que de verdad
                    distingue a FORMA de otra app de entrenos. */}
                <span>Lista de la compra de Mercadona</span>
              </p>
              <h1 id="landing-title" className={styles.title}>
                Entrena. Nutre.
                <br />
                <span className={styles.accentText}>Evoluciona.</span>
              </h1>
              <p className={styles.lead}>
                La plataforma definitiva de fitness que integra entrenamientos personalizados,
                planes de nutrición y seguimiento biométrico avanzado. Tu mejor versión empieza
                aquí.
              </p>
              <div className={styles.heroActions}>
                {/* El embudo: cuatro pasos, sin cuenta y sin tarjeta. Pedir registro antes de
                    enseñar nada es lo que hace que nadie empiece. */}
                <ButtonLink className={styles.ctaPrimary} to="/plan">
                  Crea tu plan gratis
                </ButtonLink>
              </div>
              <div className={styles.socialProof}>
                <div className={styles.avatars} aria-hidden="true">
                  <img className={styles.avatar} src="/landing/athlete-1.jpg" alt="" />
                  <img className={styles.avatar} src="/landing/athlete-2.jpg" alt="" />
                  <img className={styles.avatar} src="/landing/athlete-3.jpg" alt="" />
                </div>
                <p className={styles.socialProofCopy}>
                  Únete a <span className={styles.accentStrong}>+10,000</span> atletas optimizando
                  su rendimiento.
                </p>
              </div>
            </div>
            <AccessCard />
          </div>
        </section>

        <section className={styles.features} aria-labelledby="features-title">
          <div className={styles.sectionHeading}>
            <h2 id="features-title" className={styles.sectionTitle}>
              Ecosistema Elite
            </h2>
            <p className={styles.sectionLead}>
              Diseñado para la precisión. Construido para el rendimiento.
            </p>
          </div>
          <div className={styles.featureGrid}>
            {FEATURES.map((feature) => (
              <article
                className={[styles.featureCard, feature.tone].join(' ')}
                id={feature.id}
                key={feature.id}
              >
                <span className={styles.featureGlow} aria-hidden="true" />
                <span className={styles.featureIcon}>
                  <Icon name={feature.icon} size={28} />
                </span>
                <h3 className={styles.featureTitle}>{feature.title}</h3>
                <p className={styles.featureText}>{feature.description}</p>
              </article>
            ))}
          </div>
        </section>

        <section id="producto" className={styles.product} aria-labelledby="product-title">
          {/* The frame is the grid column itself: the wrapper that used to hold it
              existed only to anchor the removed "98% Precisión" badge. */}
          <div className={styles.productFrame}>
            <img
              className={styles.productImage}
              src="/landing/app-preview.jpg"
              alt="La aplicación FORMA en un móvil, mostrando frecuencia cardiaca, objetivos de calorías y el resumen semanal."
            />
          </div>
          <div className={styles.productCopy}>
            <h2 id="product-title" className={styles.displayTitle}>
              Tu cuerpo no miente.
              <br />
              <span className={styles.accentText}>Escúchalo.</span>
            </h2>
            <p className={styles.sectionLead}>
              Conecta tu cuenta de Withings y tus medidas entran solas: peso, grasa, músculo y agua,
              sin apuntar un número a mano. Con esos datos la aplicación ajusta tu carga de
              entrenamiento en lugar de adivinarla.
            </p>
            <ul className={styles.capabilityList}>
              {SYNC_CAPABILITIES.map((capability) => (
                <li className={styles.capability} key={capability}>
                  <Icon name="checkCircle" className={styles.capabilityIcon} />
                  {capability}
                </li>
              ))}
            </ul>
            <a className={styles.inlineLink} href="#plans">
              Ver cómo funciona
              <Icon name="arrowRight" className={styles.inlineLinkIcon} />
            </a>
          </div>
        </section>

        <section id="plans" className={styles.cta} aria-labelledby="cta-title">
          <div className={styles.ctaCard}>
            <h2 id="cta-title" className={styles.displayTitle}>
              ¿Listo para el siguiente nivel?
            </h2>
            <p className={styles.sectionLead}>
              Únete a la élite. Empieza tu transformación hoy mismo con 14 días de prueba gratuita.
            </p>
            <ButtonLink
              className={[styles.ctaPrimary, styles.ctaPill].join(' ')}
              to={auth.status === 'authenticated' ? '/app' : '/register'}
            >
              {auth.status === 'authenticated' ? 'Ir a la aplicación' : 'Unirme a FORMA'}
            </ButtonLink>
          </div>
        </section>
      </main>

      <footer className={styles.footer}>
        <div className={styles.footerGrid}>
          <div className={styles.footerBrand}>
            <span className={styles.footerLockup}>
              <img className={styles.footerMark} src="/logo.svg" alt="" aria-hidden="true" />
              <span className={styles.footerWordmark}>FORMA</span>
            </span>
            <p className={styles.footerTagline}>Elite Coach. Data Driven. Performance Obsessed.</p>
          </div>
          {FOOTER_SECTIONS.map((section) => (
            <nav aria-label={section.title} key={section.title}>
              <h2 className={styles.footerHeading}>{section.title}</h2>
              <ul className={styles.footerList}>
                {section.links.map((label) => (
                  <li key={label}>
                    <a className={styles.footerLink} href="#producto">
                      {label}
                    </a>
                  </li>
                ))}
              </ul>
            </nav>
          ))}
        </div>
        <div className={styles.footerBottom}>
          <p className={styles.footerCopyright}>
            © {new Date().getFullYear()} FORMA Elite Coach. Todos los derechos reservados.
          </p>
          <div className={styles.footerSocial}>
            <a className={styles.footerIconLink} href="#producto" aria-label="Compartir">
              <Icon name="share" />
            </a>
            <a className={styles.footerIconLink} href="#producto" aria-label="Sitio web">
              <Icon name="globe" />
            </a>
          </div>
        </div>
      </footer>
    </>
  );
}

/**
 * The hero's right-hand card. Anonymous visitors get the real login form; the
 * other three branches keep the rest of the public page usable instead of
 * blocking on a session check that has not resolved (or has failed).
 */
function AccessCard() {
  const auth = useAuth();

  if (auth.status === 'authenticated') {
    return (
      <AccessShell title="Tu espacio está preparado" subtitle="Sesión activa">
        <p className={styles.account}>{auth.user?.email}</p>
        <ButtonLink className={[styles.ctaPrimary, styles.ctaBlock].join(' ')} to="/app">
          Ir a la aplicación
        </ButtonLink>
      </AccessShell>
    );
  }

  if (auth.bootstrapError) {
    return (
      <AccessShell title="Accede a FORMA" subtitle="Acceso personal">
        <p role="status">No pudimos comprobar tu sesión.</p>
        <p className={styles.muted}>Puedes iniciar sesión igualmente desde la página de acceso.</p>
        <ButtonLink className={[styles.ctaPrimary, styles.ctaBlock].join(' ')} to="/login">
          Ir a iniciar sesión
        </ButtonLink>
      </AccessShell>
    );
  }

  if (auth.status === 'loading') {
    return (
      <AccessShell title="Accede a FORMA" subtitle="Acceso personal" busy>
        <p role="status">Comprobando tu sesión…</p>
        <p className={styles.muted}>Mientras tanto, puedes explorar todo el contenido público.</p>
      </AccessShell>
    );
  }

  return <LoginCard />;
}

function AccessShell({
  title,
  subtitle,
  busy,
  children,
}: {
  readonly title: string;
  readonly subtitle: string;
  readonly busy?: boolean;
  readonly children: ReactNode;
}) {
  return (
    <div className={styles.accessWrapper} id="acceso">
      {/* Template: a blurred accent halo pulsing behind the card. */}
      <span className={styles.accessHalo} aria-hidden="true" />
      <section
        className={styles.accessCard}
        aria-labelledby="access-title"
        aria-busy={busy || undefined}
      >
        <div className={styles.accessHeader}>
          <h2 id="access-title" className={styles.accessTitle}>
            {title}
          </h2>
          <p className={styles.accessSubtitle}>{subtitle}</p>
        </div>
        {children}
      </section>
    </div>
  );
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
    <AccessShell title="Iniciar Sesión" subtitle="Accede a tu panel personalizado">
      <form className={styles.loginForm} onSubmit={submit} aria-busy={pending || undefined}>
        <TextField
          id="landing-email"
          label="Correo electrónico"
          type="email"
          autoComplete="email"
          placeholder="diego@forma.coach"
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
          placeholder="••••••••"
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
        <Button className={styles.loginSubmit} type="submit" loading={pending}>
          Iniciar sesión
        </Button>
      </form>
      <p className={styles.registerPrompt}>
        ¿No tienes cuenta?{' '}
        <Link className={styles.accentStrong} to="/register">
          Crear cuenta
        </Link>
      </p>
    </AccessShell>
  );
}
