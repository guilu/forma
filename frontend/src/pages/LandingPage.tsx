import { useState, type FormEvent, type ReactNode } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { Button } from '../components/Button';
import { ButtonLink } from '../components/ButtonLink';
import { Icon, type IconName } from '../components/Icon';
import { GitHubMark } from '../components/GitHubMark';
import { TextField } from '../components/FormField';
import { MuscleSilhouette } from '../components/MuscleSilhouette';
import type { MuscleCode, MuscleRole } from './trainingMuscleOverlay';
import styles from './LandingPage.module.css';

/**
 * Public landing page.
 *
 * <p>The page is a rewrite of the FOR-185 translation of `docs/0-landing.html`.
 * Two things drove it. First, the muscle overlay — the one thing FORMA draws
 * that no other training app on the Spanish market draws — was buried in the
 * application while the hero showed a stock photo of a phone. It is now the
 * hero. Second, the copy promised a product that does not exist; the block
 * below lists what came off, and `LandingPage.test.tsx` is the guard that keeps
 * it gone.
 *
 * <p>The mockup's top navigation is *not* rendered here: FOR-185 promoted it to
 * the global {@link Topbar}, which sits above every route (`layout/RootLayout`).
 * That bar links to `/#training`, `/#nutrition` and `/#plans`, so those three
 * ids are a contract this page owes it, not decoration.
 *
 * <p>The session-aware access card — login form, active session, loading and
 * bootstrap failure — survives the redesign, moved from the hero to the closing
 * section. The hero now sells; the foot of the page converts. Deleting working
 * authentication to match a mockup was never on the table.
 */

/*
 * The claims this page used to make, kept as a written record because a comment
 * is cheaper to read than a git blame:
 *
 * - "+10,000 atletas" and a row of stock avatars. No such number exists.
 * - "98% Precisión" over the phone mockup. Nothing in this repository measures
 *   precision, and a concrete percentage reads as data rather than enthusiasm.
 * - "14 días de prueba gratuita". There is no pricing and no trial in the code.
 * - "Algoritmo de recuperación propietario". What runs is two rule-based
 *   services (`TrainingAdherenceRecommendationService`,
 *   `PaceDegradationRecommendationService`) — explainable by design, which is
 *   the opposite of what "propietario" sells.
 * - "Apple Watch, Garmin, Withings" and "HealthKit". `IntegrationService`
 *   registers one `ProviderMeasuresGateway` per provider and Withings is the
 *   only provider that has one.
 *
 * Sample figures that *are* on the page (session, shopping list, body
 * composition) are labelled as samples in the UI itself. A visitor has to be
 * able to tell a screenshot from a promise.
 */
/**
 * The session the hero draws: a push day, the most legible example of the
 * primary/secondary distinction because the triceps genuinely work twice —
 * directly on the dips and as support on the presses.
 *
 * <p>Codes, not labels: this is the same vocabulary
 * {@link ./trainingMuscleOverlay} translates the catalog into, so the hero and
 * the real session detail light exactly the same muscles.
 */
const HERO_SESSION: Readonly<Partial<Record<MuscleCode, MuscleRole>>> = {
  PECTORAL: 'primary',
  DELTOID_FRONT: 'primary',
  TRICEPS: 'primary',
  ABS: 'secondary',
  DELTOID_REAR: 'secondary',
  TRAPEZIUS: 'secondary',
};

const HERO_MUSCLE_COUNT = Object.keys(HERO_SESSION).length;

interface Step {
  readonly id: string;
  readonly tone: string;
  readonly title: string;
}

/*
 * Titles only. Each of these steps has a section of its own further down the
 * page that explains it properly; repeating a paragraph of it here made the
 * funnel — the one thing a visitor should be able to take in at a glance —
 * the densest block on the page.
 */
const STEPS: readonly Step[] = [
  { id: 'punto-de-partida', tone: styles.tonePrimary, title: 'Tu punto de partida' },
  { id: 'semana-de-entreno', tone: styles.toneSecondary, title: 'Tu semana de entreno' },
  { id: 'comida-del-dia', tone: styles.toneTertiary, title: 'Comida según el día' },
  { id: 'compra-resuelta', tone: styles.tonePrimary, title: 'La compra, resuelta' },
];

const TRAINING_CAPABILITIES = [
  'Series, repeticiones y descanso por ejercicio, no un PDF genérico.',
  'Vista frontal y posterior, silueta masculina y femenina.',
  'Los días de carrera y de descanso son parte del plan, no huecos.',
] as const;

const NUTRITION_CAPABILITIES = [
  'Macros distintos para día de entreno y día de descanso.',
  'Catálogo real de productos, con precio y pasillo.',
  'Vas tachando desde el móvil mientras compras.',
] as const;

interface SampleExercise {
  readonly name: string;
  readonly primary: string;
  readonly secondary?: string;
  readonly volume: string;
}

const SAMPLE_SESSION: readonly SampleExercise[] = [
  { name: 'Press banca con barra', primary: 'Pecho', secondary: 'Tríceps', volume: '4 × 8' },
  { name: 'Press militar de pie', primary: 'Hombro', secondary: 'Trapecio', volume: '4 × 6' },
  { name: 'Fondos en paralelas', primary: 'Tríceps', secondary: 'Pecho', volume: '3 × 10' },
  { name: 'Elevaciones laterales', primary: 'Hombro', volume: '3 × 12' },
];

interface SampleShoppingItem {
  readonly product: string;
  readonly aisle: string;
  readonly price: string;
  readonly done?: boolean;
}

const SAMPLE_SHOPPING: readonly SampleShoppingItem[] = [
  {
    product: 'Pechuga de pollo · 1 kg',
    aisle: 'Pasillo 12 · Carnicería',
    price: '6.95 €',
    done: true,
  },
  { product: 'Arroz basmati · 1 kg', aisle: 'Pasillo 4 · Arroz y pasta', price: '2.15 €' },
  { product: 'Yogur griego natural · 4 uds', aisle: 'Pasillo 18 · Refrigerados', price: '2.40 €' },
  { product: 'Brócoli fresco · 500 g', aisle: 'Pasillo 1 · Fruta y verdura', price: '1.79 €' },
];

interface SampleMetric {
  readonly label: string;
  readonly value: string;
  readonly unit: string;
  readonly delta: string;
  readonly improving?: boolean;
}

const SAMPLE_METRICS: readonly SampleMetric[] = [
  { label: 'Peso', value: '78,4', unit: 'kg', delta: '−1,2 kg en 30 días', improving: true },
  { label: 'Grasa', value: '18,1', unit: '%', delta: '−0,9 pts en 30 días', improving: true },
  { label: 'Músculo', value: '60,9', unit: 'kg', delta: '+0,4 kg en 30 días', improving: true },
  { label: 'Agua', value: '56,2', unit: '%', delta: 'Estable' },
];

interface Insight {
  readonly id: string;
  readonly icon: IconName;
  readonly tone: string;
  readonly title: string;
  readonly quote: string;
}

/*
 * Both quotes paraphrase what the two recommendation services that actually run
 * produce. Neither invents a capability: adherence counts completed sessions,
 * pace degradation compares recent runs.
 */
const INSIGHTS: readonly Insight[] = [
  {
    id: 'adherencia',
    icon: 'progress',
    tone: styles.tonePrimary,
    title: 'Adherencia al plan',
    quote:
      '«Has completado 2 de 4 sesiones estas dos semanas. Baja a 3 días o el plan dejará de encajar contigo.»',
  },
  {
    id: 'ritmo',
    icon: 'clock',
    tone: styles.toneWarning,
    title: 'Ritmo de carrera',
    quote:
      '«Tu ritmo medio ha caído 14 s/km en tres salidas seguidas. Suele ser descanso, no forma física.»',
  },
];

interface Question {
  readonly id: string;
  readonly question: string;
  readonly answer: string;
}

/*
 * Long-tail search intent, answered honestly. The ordering is the order a
 * visitor asks them in: can I try it, does it work with my kit, does it work
 * where I shop, does it work with my week, what does it cost, what about my
 * data.
 */
const QUESTIONS: readonly Question[] = [
  {
    id: 'sin-cuenta',
    question: '¿Necesito una cuenta para ver mi plan?',
    answer:
      'No. El generador son cuatro pasos y te enseña el plan al final. La cuenta solo hace falta si quieres guardarlo y seguir tu progreso.',
  },
  {
    id: 'integraciones',
    question: '¿Qué básculas y wearables se conectan?',
    answer:
      'Hoy solo Withings, y solo para medidas de composición corporal. No prometemos más integraciones hasta que existan de verdad.',
  },
  {
    id: 'supermercado',
    question: '¿La lista de la compra solo funciona con Mercadona?',
    answer:
      'Sí. El catálogo con precios y pasillos que FORMA usa es el de Mercadona. Si compras en otro sitio la lista te sirve igual, pero sin precio ni pasillo.',
  },
  {
    id: 'dos-dias',
    question: '¿Sirve si entreno solo dos días a la semana?',
    answer:
      'Sí. El plan se reparte sobre los días que le digas que tienes, e incluye descanso y carrera como parte del plan, no como huecos vacíos.',
  },
  {
    id: 'precio',
    question: '¿Cuánto cuesta?',
    answer: '[COMPLETAR PRECIO]. Crear y ver tu plan es gratis y no pedimos tarjeta para ello.',
  },
  {
    id: 'datos',
    question: '¿Qué pasa con mis datos?',
    answer:
      'Son tuyos. Puedes exportarlos o borrar la cuenta cuando quieras, y FORMA no es un producto de diagnóstico médico.',
  },
];

const FOOTER_SECTIONS = [
  {
    title: 'Producto',
    links: [
      { label: 'Cómo funciona', href: '#como' },
      { label: 'Entrenamiento', href: '#training' },
      { label: 'Nutrición y compra', href: '#nutrition' },
      { label: 'Composición corporal', href: '#progreso' },
    ],
  },
  {
    title: 'Empezar',
    links: [
      { label: 'Crear un plan', href: '#plans' },
      { label: 'Preguntas frecuentes', href: '#faq' },
    ],
  },
] as const;

/**
 * Where to send someone who wants FORMA to keep existing.
 *
 * <p>Three destinations rather than one because they ask for different things:
 * a recurring sponsorship, a one-off tip, and nothing at all beyond a look at
 * the source. The third is not funding and is the one most visitors will use;
 * dropping it would leave a support row that only asks for money.
 *
 * <p>The hue of each pill comes from its destination — see `--color-sponsor`
 * and `--color-coffee` in `theme.css` for why they are not Forma's green.
 */
const SUPPORT_LINKS = [
  {
    href: 'https://github.com/sponsors/guilu',
    label: 'Sponsor',
    /* «Patrocinar a FORMA en GitHub» and not just «Sponsor»: read out of
       context, a lone English verb says nothing about where it leads. */
    srLabel: 'Patrocinar a FORMA en GitHub Sponsors',
    tone: 'sponsor',
  },
  {
    href: 'https://buymeacoffee.com/diegobarrioh',
    label: 'Buy me a coffee',
    srLabel: 'Invitar a un café en Buy Me a Coffee',
    tone: 'coffee',
  },
  {
    href: 'https://github.com/guilu/forma',
    label: 'GitHub',
    srLabel: 'Ver el código de FORMA en GitHub',
    tone: 'code',
  },
] as const;

export function LandingPage() {
  return (
    <>
      <main id="main-content" tabIndex={-1} className={styles.page}>
        <Hero />
        <HowItWorks />
        <Training />
        <Nutrition />
        <Progress />
        <Faq />
        <Closing />
      </main>
      <SiteFooter />
    </>
  );
}

function Hero() {
  return (
    <section className={styles.hero} aria-labelledby="landing-title">
      {/* Decorative grid mesh behind the hero (template: `.hero-mesh`). */}
      <div className={styles.heroMesh} aria-hidden="true" />
      <div className={styles.heroGrid}>
        <div className={styles.heroCopy}>
          <p className={styles.badge}>
            {/* Wrapped rather than left as a bare text node: JSX collapses the
                newline into a single space, which reads ambiguously next to a
                self-closing sibling (sonar typescript:S6772) and would make the
                label an anonymous flex item. */}
            <span className={styles.badgeDot} aria-hidden="true" />
            <span>Sin cuenta · sin tarjeta · 4 pasos</span>
          </p>
          <h1 id="landing-title" className={styles.title}>
            Entrenamiento y nutrición con la{' '}
            {/* The brand gradient runs teal -> amber -> lime. It reads as a
                sweep across a short burst and as a highlighter accident across
                a long one, so it carries the differentiator alone rather than
                the whole second half of the sentence. */}
            <span className={styles.accentText}>compra ya hecha.</span>
          </h1>
          <p className={styles.lead}>
            FORMA reparte tus sesiones de fuerza, carrera y descanso, te enseña qué músculos trabaja
            cada una, y convierte el plan de comidas en una lista de la compra de Mercadona con
            precio y pasillo.
          </p>
          <div className={styles.heroActions}>
            {/* El embudo: cuatro pasos, sin cuenta y sin tarjeta. Pedir registro
                antes de enseñar nada es lo que hace que nadie empiece. */}
            <ButtonLink className={styles.ctaPrimary} to="/plan">
              Crear mi plan gratis
            </ButtonLink>
            <a className={styles.ctaSecondary} href="#como">
              Ver cómo funciona
              <Icon name="chevron" className={styles.ctaSecondaryIcon} />
            </a>
          </div>
          <ul className={styles.trustList}>
            <li className={styles.trustItem}>
              <Icon name="checkCircle" className={styles.capabilityIcon} />
              Ves tu plan antes de registrarte
            </li>
            <li className={styles.trustItem}>
              <Icon name="checkCircle" className={styles.capabilityIcon} />
              Tus datos son tuyos y puedes borrarlos
            </li>
          </ul>
        </div>
        <SessionMap />
      </div>
    </section>
  );
}

/**
 * The hero's right-hand panel: the muscle overlay the application already draws
 * for a real session, shown here as the product's own screenshot.
 *
 * <p>It renders {@link MuscleSilhouette} rather than a picture of it, so the
 * landing and the session detail can never drift apart, and so the highlight
 * follows the active theme like everywhere else.
 */
function SessionMap() {
  return (
    <section className={styles.sessionMap} aria-labelledby="session-map-title">
      <div className={styles.sessionMapHeader}>
        {/* The label and the duration share a line so the title below can have
            the card's full width: beside the chip it wrapped onto two lines,
            and every line it takes comes off the height left for the drawing. */}
        <div className={styles.sessionMapMeta}>
          <p className={styles.eyebrow}>Sesión de hoy · Empuje</p>
          <span className={styles.sessionMapDuration}>45 min</span>
        </div>
        <h2 id="session-map-title" className={styles.sessionMapTitle}>
          Mapa muscular · {HERO_MUSCLE_COUNT} grupos activos
        </h2>
      </div>
      <div className={styles.sessionMapViews}>
        <div className={styles.sessionMapView}>
          <span className={styles.sessionMapViewLabel}>Frontal</span>
          <MuscleSilhouette
            className={styles.sessionMapBody}
            sex="male"
            view="front"
            muscles={HERO_SESSION}
            label="Vista frontal: pectoral y deltoides anterior trabajados de forma directa, abdomen como apoyo."
          />
        </div>
        <div className={styles.sessionMapView}>
          <span className={styles.sessionMapViewLabel}>Posterior</span>
          <MuscleSilhouette
            className={styles.sessionMapBody}
            sex="male"
            view="back"
            muscles={HERO_SESSION}
            label="Vista posterior: tríceps trabajado de forma directa, deltoides posterior y trapecio como apoyo."
          />
        </div>
      </div>
      <div className={styles.sessionMapLegend}>
        <span className={styles.legendItem}>
          <span
            className={[styles.legendSwatch, styles.legendPrimary].join(' ')}
            aria-hidden="true"
          />
          Primario
        </span>
        <span className={styles.legendItem}>
          <span
            className={[styles.legendSwatch, styles.legendSecondary].join(' ')}
            aria-hidden="true"
          />
          Secundario
        </span>
        <span className={styles.legendGroups}>Pecho · Hombro · Tríceps · Trapecio · Abdomen</span>
      </div>
    </section>
  );
}

function HowItWorks() {
  return (
    <section id="como" className={styles.steps} aria-labelledby="steps-title">
      <div className={styles.sectionHeading}>
        <p className={styles.eyebrow}>Cómo funciona</p>
        <h2 id="steps-title" className={styles.sectionTitle}>
          De cuatro preguntas a la cesta de la compra
        </h2>
        <p className={styles.sectionLead}>
          Sin onboarding infinito. Respondes lo mínimo para que el plan tenga sentido y lo ves
          entero antes de decidir si te quedas.
        </p>
      </div>
      <ol className={styles.stepGrid}>
        {STEPS.map((step, index) => (
          <li className={[styles.stepItem, step.tone].join(' ')} id={step.id} key={step.id}>
            {/* The number is decorative: the list is already ordered, so a
                screen reader announces "1 of 4" without this repeating it. The
                rail joining the circles is drawn by CSS for the same reason. */}
            <span className={styles.stepNumber} aria-hidden="true">
              {index + 1}
            </span>
            <h3 className={styles.stepTitle}>{step.title}</h3>
          </li>
        ))}
      </ol>
    </section>
  );
}

function Training() {
  return (
    <section id="training" className={styles.split} aria-labelledby="training-title">
      <div className={styles.splitCopy}>
        <p className={styles.eyebrow}>Entrenamiento</p>
        <h2 id="training-title" className={styles.sectionTitle}>
          Cada sesión te dice qué trabaja{' '}
          <span className={styles.accentText}>y qué solo acompaña.</span>
        </h2>
        <p className={styles.sectionLead}>
          FORMA separa el músculo primario del secundario en cada ejercicio. Ves de un vistazo si la
          semana está descompensada antes de acumular tres meses tirando siempre del mismo lado.
        </p>
        <ul className={styles.capabilityList}>
          {TRAINING_CAPABILITIES.map((capability) => (
            <li className={styles.capability} key={capability}>
              <Icon name="checkCircle" className={styles.capabilityIcon} />
              {capability}
            </li>
          ))}
        </ul>
      </div>

      <div className={styles.panel}>
        <div className={styles.panelHeader}>
          <div className={styles.panelHeading}>
            <p className={styles.panelTitle}>Empuje · Torso superior</p>
            <p className={styles.panelMeta}>Miércoles · 45 min · 4 ejercicios</p>
          </div>
          <span className={styles.panelTag}>Fuerza</span>
        </div>
        <ul className={styles.rowList}>
          {SAMPLE_SESSION.map((exercise) => (
            <li className={styles.row} key={exercise.name}>
              <span className={styles.rowMain}>
                <span className={styles.rowTitle}>{exercise.name}</span>
                <span className={styles.tagRow}>
                  <span className={[styles.tag, styles.tagPrimary].join(' ')}>
                    {exercise.primary}
                  </span>
                  {exercise.secondary && <span className={styles.tag}>{exercise.secondary}</span>}
                </span>
              </span>
              <span className={styles.rowValue}>{exercise.volume}</span>
            </li>
          ))}
        </ul>
        <p className={styles.panelNote}>Sesión de ejemplo.</p>
      </div>
    </section>
  );
}

function Nutrition() {
  return (
    <section
      id="nutrition"
      className={[styles.split, styles.splitReversed].join(' ')}
      aria-labelledby="nutrition-title"
    >
      <div className={styles.splitCopy}>
        <p className={styles.eyebrow}>Nutrición y compra</p>
        <h2 id="nutrition-title" className={styles.sectionTitle}>
          El plan no sirve de nada{' '}
          <span className={styles.accentText}>si no llega a la nevera.</span>
        </h2>
        <p className={styles.sectionLead}>
          Aquí es donde casi todas las apps te sueltan. FORMA convierte el plan de comidas en una
          lista agrupada por pasillo, con el precio del producto, para que la compra deje de ser el
          paso donde abandonas.
        </p>
        <ul className={styles.capabilityList}>
          {NUTRITION_CAPABILITIES.map((capability) => (
            <li className={styles.capability} key={capability}>
              <Icon name="checkCircle" className={styles.capabilityIcon} />
              {capability}
            </li>
          ))}
        </ul>
      </div>

      <div className={styles.panel}>
        <div className={styles.panelHeader}>
          <p className={styles.panelTitle}>Lista de la compra · Semana 12</p>
          <span className={styles.panelTotal}>42.85 €</span>
        </div>
        <ul className={styles.rowList}>
          {SAMPLE_SHOPPING.map((item) => (
            <li className={styles.row} key={item.product}>
              <span
                className={[styles.checkbox, item.done ? styles.checkboxDone : '']
                  .filter(Boolean)
                  .join(' ')}
                aria-hidden="true"
              >
                {item.done && <Icon name="check" size={14} />}
              </span>
              <span className={styles.rowMain}>
                <span className={item.done ? styles.rowTitleDone : styles.rowTitle}>
                  {item.product}
                </span>
                <span className={styles.rowMeta}>{item.aisle}</span>
              </span>
              <span className={styles.rowValue}>{item.price}</span>
            </li>
          ))}
        </ul>
        <p className={styles.panelNote}>Precios de ejemplo. FORMA no está asociada a Mercadona.</p>
      </div>
    </section>
  );
}

function Progress() {
  return (
    <section id="progreso" className={styles.progress} aria-labelledby="progress-title">
      <div className={styles.sectionHeading}>
        <p className={styles.eyebrow}>Composición corporal</p>
        <h2 id="progress-title" className={styles.sectionTitle}>
          El peso solo no cuenta la película entera
        </h2>
        <p className={styles.sectionLead}>
          Conecta tu báscula Withings y las medidas entran solas: peso, grasa, músculo y agua, sin
          apuntar un número a mano. Es la única integración que FORMA tiene hoy, y funciona.
        </p>
      </div>

      <ul className={styles.metricGrid}>
        {SAMPLE_METRICS.map((metric) => (
          <li className={styles.metricCard} key={metric.label}>
            <span className={styles.metricLabel}>{metric.label}</span>
            <span className={styles.metricValue}>
              {metric.value}
              <span className={styles.metricUnit}>{metric.unit}</span>
            </span>
            <span className={metric.improving ? styles.metricDeltaGood : styles.metricDelta}>
              {metric.delta}
            </span>
          </li>
        ))}
      </ul>

      <div className={styles.insightGrid}>
        {INSIGHTS.map((insight) => (
          <article className={[styles.insightCard, insight.tone].join(' ')} key={insight.id}>
            <span className={styles.insightIcon}>
              <Icon name={insight.icon} size={22} />
            </span>
            <div className={styles.insightBody}>
              <h3 className={styles.insightTitle}>{insight.title}</h3>
              <p className={styles.insightText}>{insight.quote}</p>
            </div>
          </article>
        ))}
      </div>

      <p className={styles.footnote}>
        Datos de ejemplo. Los avisos llevan una regla explicable detrás, no una caja negra, y FORMA
        no ofrece diagnóstico médico.
      </p>
    </section>
  );
}

function Faq() {
  /*
   * A set rather than a single open id: a FAQ is read by comparing answers, not
   * by working through them in order, so opening one does not shut the last.
   */
  const [open, setOpen] = useState<ReadonlySet<string>>(new Set());

  function toggle(id: string) {
    setOpen((current) => {
      const next = new Set(current);
      if (!next.delete(id)) next.add(id);
      return next;
    });
  }

  return (
    <section id="faq" className={styles.faq} aria-labelledby="faq-title">
      {/* The band runs edge to edge; the grid inside it caps like every other
          section. Same split as `.steps` / `.stepGrid`. */}
      <div className={styles.faqGrid}>
        <div className={styles.faqIntro}>
          <p className={styles.eyebrow}>Preguntas frecuentes</p>
          <h2 id="faq-title" className={styles.sectionTitle}>
            Lo que la gente pregunta antes de empezar
          </h2>
          <p className={styles.sectionLead}>Si falta la tuya, escríbenos y la añadimos aquí.</p>
        </div>
        <div className={styles.faqList}>
          {QUESTIONS.map((entry) => {
            const expanded = open.has(entry.id);
            const panelId = `faq-answer-${entry.id}`;
            return (
              <article className={styles.faqCard} key={entry.id}>
                {/*
                 * The button lives *inside* the heading rather than replacing
                 * it: a screen reader user navigating by headings still gets
                 * the six questions as landmarks, and gets a control to operate
                 * once they land on one. Swapping the h3 for a bare button
                 * would take the first half away.
                 */}
                <h3 className={styles.faqQuestion}>
                  <button
                    type="button"
                    className={styles.faqTrigger}
                    aria-expanded={expanded}
                    aria-controls={panelId}
                    onClick={() => toggle(entry.id)}
                  >
                    {entry.question}
                    <Icon name="chevron" className={styles.faqChevron} />
                  </button>
                </h3>
                {/*
                 * `hidden` rather than unmounting: the answers stay in the
                 * document for a crawler and for the browser's own find-in-page,
                 * which is most of why a FAQ is worth writing at all.
                 */}
                <div id={panelId} className={styles.faqPanel} hidden={!expanded}>
                  <p className={styles.faqAnswer}>{entry.answer}</p>
                </div>
              </article>
            );
          })}
        </div>
      </div>
    </section>
  );
}

function Closing() {
  return (
    <section id="plans" className={styles.closing} aria-labelledby="closing-title">
      <div className={styles.closingCopy}>
        <h2 id="closing-title" className={styles.displayTitle}>
          Empieza por saber qué toca hoy
        </h2>
        <p className={styles.sectionLead}>
          Cuatro preguntas y tienes tu semana de entreno, tus comidas y tu lista de la compra. Sin
          cuenta y sin tarjeta.
        </p>
        <ButtonLink className={[styles.ctaPrimary, styles.ctaPill].join(' ')} to="/plan">
          Crear mi plan gratis
        </ButtonLink>
      </div>
      <AccessCard />
    </section>
  );
}

function SiteFooter() {
  return (
    <footer className={styles.footer}>
      <div className={styles.footerGrid}>
        <div className={styles.footerBrand}>
          <span className={styles.footerLockup}>
            <img className={styles.footerMark} src="/logo.svg" alt="" aria-hidden="true" />
            <span className={styles.footerWordmark}>FORMA</span>
          </span>
          <p className={styles.footerTagline}>
            Entrenamiento, nutrición y compra en un mismo plan. Recomendaciones explicables, sin
            precisión falsa.
          </p>
        </div>
        {FOOTER_SECTIONS.map((section) => (
          <nav aria-label={section.title} key={section.title}>
            <h2 className={styles.footerHeading}>{section.title}</h2>
            <ul className={styles.footerList}>
              {section.links.map((link) => (
                <li key={link.label}>
                  <a className={styles.footerLink} href={link.href}>
                    {link.label}
                  </a>
                </li>
              ))}
            </ul>
          </nav>
        ))}
      </div>
      <div className={styles.footerBottom}>
        {/*
         * The two legal lines stack on the left, and the support row takes the
         * right. They used to be the two ends of the same row, which read as a
         * choice between them — the disclaimer is a footnote to the copyright,
         * not its counterweight.
         */}
        <div className={styles.footerLegal}>
          <p className={styles.footerCopyright}>
            © {new Date().getFullYear()} FORMA. Todos los derechos reservados.
          </p>
          <p className={styles.footerCopyright}>FORMA no ofrece diagnóstico médico.</p>
        </div>
        <SupportLinks />
      </div>
    </footer>
  );
}

const SUPPORT_TONE: Record<(typeof SUPPORT_LINKS)[number]['tone'], string> = {
  sponsor: styles.supportSponsor,
  coffee: styles.supportCoffee,
  code: styles.supportCode,
};

function SupportLinks() {
  return (
    /*
     * A `nav` with its own name: three links to other sites, grouped, is
     * exactly what a landmark is for — and the visible «Apoyar» is the label,
     * so a screen reader announces the same word a sighted visitor reads.
     */
    <nav className={styles.support} aria-labelledby="support-heading">
      <h2 className={styles.supportHeading} id="support-heading">
        Apoyar
      </h2>
      <ul className={styles.supportList}>
        {SUPPORT_LINKS.map((link) => (
          <li key={link.label}>
            <a
              className={[styles.supportLink, SUPPORT_TONE[link.tone]].join(' ')}
              href={link.href}
              /*
               * `noreferrer` alongside `noopener`: these are the only outbound
               * links on the page, and there is no reason to hand a funding
               * page the URL the visitor came from.
               */
              rel="noopener noreferrer"
              target="_blank"
              aria-label={link.srLabel}
            >
              {link.tone === 'code' ? (
                <GitHubMark />
              ) : (
                <Icon name={link.tone === 'sponsor' ? 'heart' : 'coffee'} size={14} />
              )}
              {link.label}
            </a>
          </li>
        ))}
      </ul>
    </nav>
  );
}

/**
 * The closing section's access panel. Anonymous visitors get the real login
 * form; the other three branches keep the rest of the public page usable
 * instead of blocking on a session check that has not resolved (or has failed).
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
          <h3 id="access-title" className={styles.accessTitle}>
            {title}
          </h3>
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
    <AccessShell title="Ya tengo cuenta" subtitle="Entra en tu panel">
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
