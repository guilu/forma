import { Link } from 'react-router-dom';
import { Button } from '../../components/Button';
import { TextField } from '../../components/FormField';
import { Icon } from '../../components/Icon';
import type { EnergyRequirement } from '../../api/planGenerator';
import { ChoiceCard } from './GeneratorChrome';
import { EATING_STYLE_LABELS, OBJECTIVE_LABELS, type FunnelState } from './funnelState';
import styles from './PlanGenerator.module.css';

/**
 * Paso 4: dónde te lo mandamos.
 *
 * <p>NO se crea cuenta aquí, y el texto lo dice. El diseño de partida capturaba el correo
 * y mandaba el plan con instrucciones para registrarse; prometer «sin registro» y crear
 * una cuenta a la vez es el tipo de cosa por la que la gente deja de fiarse de un
 * producto. Sin tarjeta es cierto y vende igual.
 *
 * <p>La casilla del aviso de privacidad es obligatoria y empieza sin marcar: un
 * consentimiento que puede estar a falso no es consentimiento, y uno premarcado no es
 * una decisión de nadie. La de novedades es aparte, porque aceptar recibir lo que has
 * pedido y aceptar que te escriban después son dos preguntas.
 *
 * <p>FOR-190 cambió el gancho: donde había una lista de cuatro promesas («Recibirás
 * gratis tu plan de N días con: …») ahora hay el resumen de lo que se acaba de elegir.
 * Prometer en abstracto y enseñar lo concreto cuestan lo mismo en pantalla, y solo uno
 * de los dos se puede comprobar.
 */
const SOURCES = [
  'Un amigo o conocido',
  'Redes sociales',
  'Buscando en internet',
  'Un profesional de la salud',
  'Otro',
];

/** Lo que se lleva quien termina el embudo. Cuatro, cortos, con su marca de visto. */
const DELIVERABLES = ['Menú completo', 'Macros por comida', 'Lista de la compra', 'Equivalencias'];

const NUM = new Intl.NumberFormat('es-ES');

interface StepContactProps {
  readonly state: FunnelState;
  readonly energy: EnergyRequirement | undefined;
  readonly pending: boolean;
  readonly onChange: (change: Partial<FunnelState>) => void;
  readonly onBack: () => void;
  readonly onSubmit: () => void;
}

export function StepContact({
  state,
  energy,
  pending,
  onChange,
  onBack,
  onSubmit,
}: StepContactProps) {
  const ready =
    state.fullName.trim() !== '' && state.email.trim() !== '' && state.acceptsPrivacyPolicy;

  return (
    <section className={styles.step} aria-labelledby="paso-4">
      <h2 className={styles.stepTitle} id="paso-4">
        ¿Dónde te lo enviamos?
      </h2>

      <div className={styles.summary}>
        <div className={styles.summaryHead}>
          <div>
            <p className={styles.headlineEyebrow}>Tu plan, listo</p>
            <p className={styles.headlineValue}>
              {energy ? (
                <>
                  <span className={styles.headlineNumber}>{NUM.format(energy.planKcal)}</span>
                  <span className={styles.headlineUnit}>kcal/día</span>
                </>
              ) : (
                <span className={styles.headlinePending}>Sin calcular</span>
              )}
            </p>
          </div>
          <ul className={styles.summaryChips}>
            {state.objective !== '' && <li>{OBJECTIVE_LABELS[state.objective]}</li>}
            <li>{state.daysPerWeek} días</li>
            <li>{state.mealsPerDay} comidas</li>
            <li>{EATING_STYLE_LABELS[state.eatingStyle]}</li>
          </ul>
        </div>
        <ul className={styles.summaryList}>
          {DELIVERABLES.map((item) => (
            <li key={item}>
              <Icon name="check" size={15} />
              {item}
            </li>
          ))}
        </ul>
      </div>

      <div className={styles.fields}>
        <TextField
          id="gen-nombre"
          label="Nombre"
          autoComplete="name"
          value={state.fullName}
          required
          onChange={(event) => onChange({ fullName: event.target.value })}
        />
        <TextField
          id="gen-email"
          label="Email"
          type="email"
          autoComplete="email"
          placeholder="tu@email.com"
          value={state.email}
          required
          onChange={(event) => onChange({ email: event.target.value })}
        />
      </div>

      {/*
       * Era un desplegable. Es opcional y son cinco opciones cortas: en el móvil un
       * `select` abre una rueda del sistema para elegir entre cinco cosas que caben en
       * dos filas. «Prefiero no decirlo» sigue siendo una opción de verdad y viene
       * marcada, igual que era el valor vacío del desplegable.
       */}
      <fieldset className={styles.group}>
        <legend className={styles.legend}>¿Cómo nos encontraste? (opcional)</legend>
        <div className={styles.chips}>
          <ChoiceCard
            name="origen"
            value=""
            checked={state.heardAboutUs === ''}
            onSelect={() => onChange({ heardAboutUs: '' })}
            title="Prefiero no decirlo"
            layout="compact"
          />
          {SOURCES.map((source) => (
            <ChoiceCard
              key={source}
              name="origen"
              value={source}
              checked={state.heardAboutUs === source}
              onSelect={(value) => onChange({ heardAboutUs: value })}
              title={source}
              layout="compact"
            />
          ))}
        </div>
      </fieldset>

      <label className={styles.check}>
        <input
          type="checkbox"
          checked={state.wantsMarketing}
          onChange={(event) => onChange({ wantsMarketing: event.target.checked })}
        />
        <span>Quiero recetas y novedades. Puedes darte de baja cuando quieras.</span>
      </label>

      <label className={styles.check}>
        <input
          type="checkbox"
          checked={state.acceptsPrivacyPolicy}
          required
          onChange={(event) => onChange({ acceptsPrivacyPolicy: event.target.checked })}
        />
        <span>
          {/*
            `Link` y no `<a href>`: hasta V61 esto apuntaba a una ruta que NO EXISTÍA, así que la
            casilla obligaba a aceptar un documento que devolvía un 404 — un consentimiento sobre
            un texto ilegible no es un consentimiento. Con el router, además, leerlo no recarga la
            página y no se pierde el embudo a medias.
          */}
          He leído y acepto el <Link to="/privacidad">aviso de privacidad</Link>. Solo para
          generarte el plan y enviártelo.
        </span>
      </label>

      <div className={styles.actionsSplit}>
        <Button type="button" variant="ghost" onClick={onBack} disabled={pending}>
          ← Anterior
        </Button>
        <Button type="button" onClick={onSubmit} disabled={!ready} loading={pending}>
          Generar mi plan →
        </Button>
      </div>
      <p className={styles.sendNote}>Sin tarjeta · Te enviaremos el plan por email</p>
    </section>
  );
}
