import { Button } from '../../components/Button';
import type { ActivityLevel, EnergyRequirement, Sex } from '../../api/planGenerator';
import { ActivityScale, ChoiceCard, EnergyHeadline } from './GeneratorChrome';
import { canCalculate, type FunnelState } from './funnelState';
import styles from './PlanGenerator.module.css';
import { measure } from '../../format/measures';

/**
 * Paso 1: quién eres, y qué gastas.
 *
 * <p>El nivel de actividad se pregunta AQUÍ y en ningún otro sitio. El diseño de partida
 * lo tenía solo en este paso, y con razón: es lo que multiplica el metabolismo basal y
 * el número se mueve delante de quien lo elige. Preguntarlo otra vez más adelante sería
 * el mismo hecho en dos pantallas, libres de discrepar.
 */
const ACTIVITY: ReadonlyArray<{
  readonly value: ActivityLevel;
  readonly label: string;
  readonly description: string;
}> = [
  { value: 'SEDENTARY', label: 'Sedentario', description: 'Poco o ningún ejercicio' },
  { value: 'LIGHT', label: 'Ligero', description: 'Ejercicio 1–3 días por semana' },
  { value: 'MODERATE', label: 'Moderado', description: 'Ejercicio 3–5 días por semana' },
  { value: 'ACTIVE', label: 'Activo', description: 'Ejercicio 6–7 días por semana' },
  { value: 'VERY_ACTIVE', label: 'Atleta', description: 'Entrenamiento intenso diario' },
];

/**
 * Una medida, con las dos formas de darla.
 *
 * <p>El deslizador es lo que hace el paso rápido en el móvil: tres medidas seguidas
 * son tres teclados que se abren y se cierran. Pero un deslizador SOLO no sirve —
 * 74,5 kg con el pulgar es lotería, y no todo el mundo apunta con un pulgar. Así que
 * los dos controles escriben el mismo dato: el número se teclea y la barra se arrastra.
 *
 * <p>El campo numérico es el que lleva la etiqueta; la barra se presenta aparte para
 * que no haya dos controles con el mismo nombre en el árbol de accesibilidad. Mientras
 * el campo esté vacío la barra se pinta apagada y se anuncia «sin definir»: dejar el
 * pulgar en un punto cualquiera diría que ya hay un valor, y no lo hay.
 */
function Metric({
  id,
  label,
  unit,
  min,
  max,
  step,
  value,
  onChange,
}: {
  readonly id: string;
  readonly label: string;
  readonly unit: string;
  readonly min: number;
  readonly max: number;
  readonly step: number;
  readonly value: string;
  readonly onChange: (next: string) => void;
}) {
  const filled = value !== '' && Number.isFinite(Number(value));
  const numeric = filled ? Number(value) : Math.round((min + max) / 2);
  return (
    <div className={styles.metric}>
      <label className={styles.metricLabel} htmlFor={id}>
        {label}
      </label>
      <span className={styles.metricValue}>
        <input
          id={id}
          className={styles.metricInput}
          type="number"
          inputMode="decimal"
          min={min}
          max={max}
          step={step}
          placeholder="--"
          value={value}
          onChange={(event) => onChange(event.target.value)}
        />
        <span className={styles.metricUnit} aria-hidden="true">
          {unit}
        </span>
      </span>
      <input
        className={filled ? styles.metricSlider : styles.metricSliderEmpty}
        type="range"
        min={min}
        max={max}
        step={step}
        value={numeric}
        aria-label={`${label} (deslizador)`}
        aria-valuetext={filled ? `${measure(numeric)} ${unit}` : 'sin definir'}
        onChange={(event) => onChange(event.target.value)}
      />
    </div>
  );
}

interface StepPatientProps {
  readonly state: FunnelState;
  readonly energy: EnergyRequirement | undefined;
  readonly onChange: (change: Partial<FunnelState>) => void;
  readonly onNext: () => void;
}

export function StepPatient({ state, energy, onChange, onNext }: StepPatientProps) {
  return (
    <section className={styles.step} aria-labelledby="paso-1">
      <h2 className={styles.stepTitle} id="paso-1">
        ¿Quién eres?
      </h2>

      <fieldset className={styles.group}>
        <legend className={styles.legend}>Sexo</legend>
        <div className={styles.grid2}>
          <ChoiceCard
            name="sexo"
            value="MALE"
            checked={state.sex === 'MALE'}
            onSelect={(value) => onChange({ sex: value as Sex })}
            icon="male"
            title="Hombre"
            layout="stacked"
          />
          <ChoiceCard
            name="sexo"
            value="FEMALE"
            checked={state.sex === 'FEMALE'}
            onSelect={(value) => onChange({ sex: value as Sex })}
            icon="female"
            title="Mujer"
            layout="stacked"
          />
        </div>
      </fieldset>

      <div className={styles.metrics}>
        <Metric
          id="gen-edad"
          label="Edad"
          unit="años"
          min={14}
          max={120}
          step={1}
          value={state.ageYears}
          onChange={(next) => onChange({ ageYears: next })}
        />
        <Metric
          id="gen-peso"
          label="Peso"
          unit="kg"
          min={35}
          max={200}
          step={0.5}
          value={state.weightKg}
          onChange={(next) => onChange({ weightKg: next })}
        />
        <Metric
          id="gen-altura"
          label="Altura"
          unit="cm"
          min={130}
          max={220}
          step={1}
          value={state.heightCm}
          onChange={(next) => onChange({ heightCm: next })}
        />
      </div>

      <fieldset className={styles.group}>
        <legend className={styles.legend}>Nivel de actividad</legend>
        <ActivityScale
          name="actividad"
          levels={ACTIVITY}
          selected={state.activityLevel}
          onSelect={(value) => onChange({ activityLevel: value as ActivityLevel })}
        />
      </fieldset>

      {/*
        Calculado por el servidor, que es quien usa la misma fórmula para el plan.

        Va al FINAL del paso, no arriba: la cifra no existe hasta que hay edad, peso,
        altura y actividad, así que arriba lo primero que se veía era un hueco pidiendo
        datos que aún no se habían pedido. Aquí llega como resultado — se rellena el
        formulario y el número aparece justo debajo, encima del botón que sigue.
      */}
      <EnergyHeadline
        eyebrow="Tu gasto diario"
        value={energy ? `${measure(energy.dailyKcal)}` : undefined}
        unit="kcal"
        pending="Rellena edad, peso y altura"
        aside={<span className={styles.headlineTag}>Mifflin-St Jeor</span>}
      />

      <div className={styles.actions}>
        <Button type="button" onClick={onNext} disabled={!canCalculate(state)}>
          Siguiente →
        </Button>
      </div>
    </section>
  );
}
