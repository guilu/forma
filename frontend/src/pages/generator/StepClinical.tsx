import { Button } from '../../components/Button';
import type { EnergyRequirement, PlanObjective } from '../../api/planGenerator';
import type { IconName } from '../../components/Icon';
import { ChoiceCard, EnergyHeadline, LockedTeaser } from './GeneratorChrome';
import { OBJECTIVE_LABELS, type FunnelState } from './funnelState';
import styles from './PlanGenerator.module.css';

/**
 * Paso 2: qué le pides al plan.
 *
 * <p>Solo el objetivo. Las patologías y las restricciones alimentarias van con candado,
 * como en el diseño de partida — y eso no es solo un gancho comercial: son datos de
 * salud, categoría especial del RGPD, y hoy nada en el modelo sabe convertir
 * «hipertensión» en «menos sodio». Recogerlos para no usarlos sería lo peor de las dos
 * opciones. Cuando existan las reglas, dejarán de tener candado.
 *
 * <p><b>Por qué la cifra está arriba y no en cada tarjeta.</b> El diseño de FOR-190
 * quería que cada objetivo enseñara las kcal que produce, para que el número explicara
 * lo que explicaba la frase. No se puede sin copiar aquí la tabla de factores del
 * servidor, que es justo lo que prohíbe la cabecera de `api/planGenerator`: cuatro
 * cifras calculadas en React serían libres de separarse de la que construye el plan.
 * El servidor manda `objectiveFactor` del objetivo ELEGIDO, así que la consecuencia se
 * enseña una vez, arriba, y se mueve al cambiar de tarjeta.
 */
const OBJECTIVES: ReadonlyArray<{
  readonly value: PlanObjective;
  readonly icon: IconName;
  readonly label: string;
  readonly description: string;
}> = [
  {
    value: 'WEIGHT_LOSS',
    icon: 'trendDown',
    label: OBJECTIVE_LABELS.WEIGHT_LOSS,
    description: 'Déficit calórico',
  },
  {
    value: 'MUSCLE_GAIN',
    icon: 'training',
    label: OBJECTIVE_LABELS.MUSCLE_GAIN,
    description: 'Superávit calórico',
  },
  {
    value: 'MAINTENANCE',
    icon: 'balance',
    label: OBJECTIVE_LABELS.MAINTENANCE,
    description: 'Sin cambios',
  },
  {
    value: 'HEALTHY_EATING',
    icon: 'nutrition',
    label: OBJECTIVE_LABELS.HEALTHY_EATING,
    description: 'Hábitos y bienestar',
  },
];

const NUM = new Intl.NumberFormat('es-ES');

/** El ajuste por objetivo, como porcentaje. Lo dice el servidor; aquí solo se formatea. */
function adjustmentLabel(factor: number): string {
  const percent = Math.round((factor - 1) * 100);
  if (percent === 0) return 'sin ajuste';
  return percent > 0 ? `+${percent} %` : `${percent} %`;
}

function adjustmentClass(factor: number): string {
  const percent = Math.round((factor - 1) * 100);
  if (percent === 0) return styles.deltaFlat;
  return percent > 0 ? styles.deltaUp : styles.deltaDown;
}

interface StepClinicalProps {
  readonly state: FunnelState;
  readonly energy: EnergyRequirement | undefined;
  readonly onChange: (change: Partial<FunnelState>) => void;
  readonly onBack: () => void;
  readonly onNext: () => void;
}

export function StepClinical({ state, energy, onChange, onBack, onNext }: StepClinicalProps) {
  const chosen = state.objective !== '' && energy !== undefined;
  return (
    <section className={styles.step} aria-labelledby="paso-2">
      <h2 className={styles.stepTitle} id="paso-2">
        ¿Qué quieres conseguir?
      </h2>

      <EnergyHeadline
        eyebrow="Requerimiento del plan"
        value={chosen ? NUM.format(energy.planKcal) : undefined}
        unit="kcal"
        pending="Elige un objetivo"
        aside={
          chosen ? (
            <>
              <span className={adjustmentClass(energy.objectiveFactor)}>
                {adjustmentLabel(energy.objectiveFactor)}
              </span>
              <span className={styles.headlineFoot}>sobre {NUM.format(energy.dailyKcal)} kcal</span>
            </>
          ) : undefined
        }
      />

      <fieldset className={styles.group}>
        <legend className={styles.legend}>Objetivo principal</legend>
        <div className={styles.grid2}>
          {OBJECTIVES.map((objective) => (
            <ChoiceCard
              key={objective.value}
              name="objetivo"
              value={objective.value}
              checked={state.objective === objective.value}
              onSelect={(value) => onChange({ objective: value as PlanObjective })}
              icon={objective.icon}
              title={objective.label}
              description={objective.description}
            />
          ))}
        </div>
      </fieldset>

      <div className={styles.lockedGroup}>
        <LockedTeaser title="12 objetivos clínicos y patologías" />
        <LockedTeaser title="Restricciones alimentarias y patrones dietéticos" />
      </div>

      <div className={styles.actionsSplit}>
        <Button type="button" variant="ghost" onClick={onBack}>
          ← Anterior
        </Button>
        <Button type="button" onClick={onNext} disabled={state.objective === ''}>
          Siguiente →
        </Button>
      </div>
    </section>
  );
}
