import { Button } from '../../components/Button';
import { SelectField, TextField } from '../../components/FormField';
import { Icon } from '../../components/Icon';
import type { EnergyRequirement } from '../../api/planGenerator';
import type { FunnelState } from './funnelState';
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
 */
const SOURCES = [
  'Un amigo o conocido',
  'Redes sociales',
  'Buscando en internet',
  'Un profesional de la salud',
  'Otro',
];

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
    <section className={styles.step2col} aria-labelledby="paso-4">
      <div>
        <h2 className={styles.stepTitle} id="paso-4">
          Dónde te lo mandamos
        </h2>
        <p className={styles.stepLead}>Para generar tu plan y enviarte el PDF.</p>

        <div className={styles.gift}>
          <span className={styles.giftIcon} aria-hidden="true">
            🎁
          </span>
          <div>
            <p className={styles.giftTitle}>
              Recibirás gratis tu plan de {state.daysPerWeek} días con:
            </p>
            <ul className={styles.giftList}>
              <li>Plan de comidas completo</li>
              <li>Distribución de macros por comida</li>
              <li>Lista de la compra con precios</li>
              <li>Equivalencias para intercambiar alimentos</li>
            </ul>
          </div>
        </div>

        <div className={styles.grid2}>
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

        <SelectField
          id="gen-origen"
          label="¿Cómo nos encontraste? (opcional)"
          value={state.heardAboutUs}
          onChange={(event) => onChange({ heardAboutUs: event.target.value })}
        >
          <option value="">Prefiero no decirlo</option>
          {SOURCES.map((source) => (
            <option key={source} value={source}>
              {source}
            </option>
          ))}
        </SelectField>

        <label className={styles.check}>
          <input
            type="checkbox"
            checked={state.wantsMarketing}
            onChange={(event) => onChange({ wantsMarketing: event.target.checked })}
          />
          <span>
            Quiero recibir consejos, recetas y novedades de FORMA.
            <span className={styles.checkNote}>Sin spam. Puedes darte de baja cuando quieras.</span>
          </span>
        </label>

        <label className={styles.check}>
          <input
            type="checkbox"
            checked={state.acceptsPrivacyPolicy}
            required
            onChange={(event) => onChange({ acceptsPrivacyPolicy: event.target.checked })}
          />
          <span>
            He leído y acepto el <a href="/privacidad">aviso de privacidad</a>.
            <span className={styles.checkNote}>
              Usamos tus datos solo para generarte el plan y enviártelo.
            </span>
          </span>
        </label>

        <div className={styles.actionsSplit}>
          <Button type="button" variant="ghost" onClick={onBack} disabled={pending}>
            ← Anterior
          </Button>
          <Button type="button" onClick={onSubmit} disabled={!ready || pending}>
            {pending ? 'Generando…' : 'Generar mi plan →'}
          </Button>
        </div>
        <p className={styles.sendNote}>Te enviaremos el plan por email.</p>
      </div>

      <aside className={styles.aside}>
        <h3 className={styles.asideTitle}>Tu plan</h3>
        {energy && (
          <p className={styles.asideBig}>
            {NUM.format(energy.planKcal)} <span>kcal/día</span>
          </p>
        )}
        <ul className={styles.asideList}>
          <li>
            <Icon name="check" size={14} /> {state.daysPerWeek} días · {state.mealsPerDay} comidas
            al día
          </li>
          <li>
            <Icon name="check" size={14} /> Sin tarjeta de crédito
          </li>
          <li>
            <Icon name="check" size={14} /> Tu primer plan es gratis
          </li>
        </ul>
      </aside>
    </section>
  );
}
