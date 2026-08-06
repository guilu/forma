import { Button } from '../../components/Button';
import { TextField } from '../../components/FormField';
import type { ActivityLevel, EnergyRequirement, Sex } from '../../api/planGenerator';
import { BreakdownRow, ChoiceCard, EnergyBreakdown } from './GeneratorChrome';
import { canCalculate, type FunnelState } from './funnelState';
import styles from './PlanGenerator.module.css';

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
  {
    value: 'SEDENTARY',
    label: 'Sedentario',
    description: 'Poco o ningún ejercicio',
  },
  {
    value: 'LIGHT',
    label: 'Ligero',
    description: 'Ejercicio 1–3 días por semana',
  },
  {
    value: 'MODERATE',
    label: 'Moderado',
    description: 'Ejercicio 3–5 días por semana',
  },
  {
    value: 'ACTIVE',
    label: 'Activo',
    description: 'Ejercicio 6–7 días por semana',
  },
  {
    value: 'VERY_ACTIVE',
    label: 'Atleta',
    description: 'Entrenamiento intenso diario',
  },
];

const NUM = new Intl.NumberFormat('es-ES');

interface StepPatientProps {
  readonly state: FunnelState;
  readonly energy: EnergyRequirement | undefined;
  readonly onChange: (change: Partial<FunnelState>) => void;
  readonly onNext: () => void;
}

export function StepPatient({ state, energy, onChange, onNext }: StepPatientProps) {
  return (
    <section className={styles.step2col} aria-labelledby="paso-1">
      <div>
        <h2 className={styles.stepTitle} id="paso-1">
          Tus datos
        </h2>
        <p className={styles.stepLead}>
          Cuéntanos sobre ti para calcular tu requerimiento calórico.
        </p>

        <fieldset className={styles.group}>
          <legend className={styles.legend}>Sexo</legend>
          <div className={styles.grid2}>
            <ChoiceCard
              name="sexo"
              value="MALE"
              checked={state.sex === 'MALE'}
              onSelect={(value) => onChange({ sex: value as Sex })}
              glyph="🧔"
              title="Hombre"
              stacked
            />
            <ChoiceCard
              name="sexo"
              value="FEMALE"
              checked={state.sex === 'FEMALE'}
              onSelect={(value) => onChange({ sex: value as Sex })}
              glyph="👩"
              title="Mujer"
              stacked
            />
          </div>
        </fieldset>

        <div className={styles.grid3}>
          <TextField
            id="gen-edad"
            label="Edad"
            type="number"
            min="14"
            max="120"
            inputMode="numeric"
            placeholder="25"
            suffix="años"
            value={state.ageYears}
            onChange={(event) => onChange({ ageYears: event.target.value })}
          />
          <TextField
            id="gen-peso"
            label="Peso"
            type="number"
            min="1"
            max="400"
            step="0.1"
            inputMode="decimal"
            placeholder="65"
            suffix="kg"
            value={state.weightKg}
            onChange={(event) => onChange({ weightKg: event.target.value })}
          />
          <TextField
            id="gen-altura"
            label="Altura"
            type="number"
            min="1"
            max="260"
            inputMode="numeric"
            placeholder="179"
            suffix="cm"
            value={state.heightCm}
            onChange={(event) => onChange({ heightCm: event.target.value })}
          />
        </div>

        <fieldset className={styles.group}>
          <legend className={styles.legend}>Nivel de actividad</legend>
          <div className={styles.gridActivity}>
            {ACTIVITY.map((level) => (
              <ChoiceCard
                key={level.value}
                name="actividad"
                value={level.value}
                checked={state.activityLevel === level.value}
                onSelect={(value) => onChange({ activityLevel: value as ActivityLevel })}
                title={level.label}
                description={level.description}
              />
            ))}
          </div>
        </fieldset>

        <div className={styles.actions}>
          <Button type="button" onClick={onNext} disabled={!canCalculate(state)}>
            Siguiente →
          </Button>
        </div>
      </div>

      <aside className={styles.aside}>
        <h3 className={styles.asideTitle}>Requerimiento energético</h3>
        {/* Calculado por el servidor, que es quien usa la misma fórmula para el plan. */}
        {energy ? (
          <EnergyBreakdown
            rows={
              <>
                <BreakdownRow label="GEB (basal)" value={`${NUM.format(energy.basalKcal)} kcal`} />
                <BreakdownRow
                  label="Factor actividad"
                  value={`× ${energy.activityFactor.toString().replace('.', ',')}`}
                />
                <BreakdownRow
                  label="GET (total)"
                  value={`${NUM.format(energy.dailyKcal)} kcal`}
                  total
                />
              </>
            }
          />
        ) : (
          <p className={styles.asideEmpty}>
            Rellena edad, peso y altura y verás aquí tu gasto diario, calculado con la fórmula de
            Mifflin-St Jeor.
          </p>
        )}
      </aside>
    </section>
  );
}
