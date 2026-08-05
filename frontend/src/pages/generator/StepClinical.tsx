import { Button } from '../../components/Button';
import type { EnergyRequirement, PlanObjective } from '../../api/planGenerator';
import { BreakdownRow, ChoiceCard, EnergyBreakdown, LockedTeaser } from './GeneratorChrome';
import type { FunnelState } from './funnelState';
import styles from './PlanGenerator.module.css';

/**
 * Paso 2: qué le pides al plan.
 *
 * <p>Solo el objetivo. Las patologías y las restricciones alimentarias van con candado,
 * como en el diseño de partida — y eso no es solo un gancho comercial: son datos de
 * salud, categoría especial del RGPD, y hoy nada en el modelo sabe convertir
 * «hipertensión» en «menos sodio». Recogerlos para no usarlos sería lo peor de las dos
 * opciones. Cuando existan las reglas, dejarán de tener candado.
 */
const OBJECTIVES: ReadonlyArray<{
  readonly value: PlanObjective;
  readonly glyph: string;
  readonly label: string;
  readonly description: string;
}> = [
  {
    value: 'WEIGHT_LOSS',
    glyph: '🔥',
    label: 'Pérdida de peso',
    description: 'Déficit calórico para reducir grasa corporal',
  },
  {
    value: 'MUSCLE_GAIN',
    glyph: '💪',
    label: 'Ganancia muscular',
    description: 'Superávit calórico para aumentar masa muscular',
  },
  {
    value: 'MAINTENANCE',
    glyph: '⚖️',
    label: 'Mantenimiento',
    description: 'Mantener peso y composición actual',
  },
  {
    value: 'HEALTHY_EATING',
    glyph: '🥗',
    label: 'Comer sano',
    description: 'Mejorar hábitos y bienestar general',
  },
];

const NUM = new Intl.NumberFormat('es-ES');

interface StepClinicalProps {
  readonly state: FunnelState;
  readonly energy: EnergyRequirement | undefined;
  readonly onChange: (change: Partial<FunnelState>) => void;
  readonly onBack: () => void;
  readonly onNext: () => void;
}

export function StepClinical({ state, energy, onChange, onBack, onNext }: StepClinicalProps) {
  return (
    <section className={styles.step2col} aria-labelledby="paso-2">
      <div>
        <h2 className={styles.stepTitle} id="paso-2">
          Tu objetivo
        </h2>
        <p className={styles.stepLead}>Qué quieres conseguir con el plan.</p>

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
                glyph={objective.glyph}
                title={objective.label}
                description={objective.description}
              />
            ))}
          </div>
        </fieldset>

        <LockedTeaser
          title="+ 12 objetivos clínicos y patologías"
          examples="Diabetes, hipertensión, embarazo, renal, SOP, hiperuricemia…"
        />
        <LockedTeaser
          title="+ Restricciones alimentarias y patrones dietéticos"
          examples="Vegano, vegetariano, sin lactosa, sin gluten, alergias por ingrediente"
        />

        <div className={styles.actionsSplit}>
          <Button type="button" variant="ghost" onClick={onBack}>
            ← Anterior
          </Button>
          <Button type="button" onClick={onNext} disabled={state.objective === ''}>
            Siguiente →
          </Button>
        </div>
      </div>

      <aside className={styles.aside}>
        <h3 className={styles.asideTitle}>Requerimiento del plan</h3>
        {energy ? (
          <EnergyBreakdown
            rows={
              <>
                <BreakdownRow label="GET (total)" value={`${NUM.format(energy.dailyKcal)} kcal`} />
                <BreakdownRow
                  label="Ajuste por objetivo"
                  value={`× ${energy.objectiveFactor.toString().replace('.', ',')}`}
                />
                <BreakdownRow
                  label="Requerimiento del plan"
                  value={`${NUM.format(energy.planKcal)} kcal`}
                  total
                />
              </>
            }
          />
        ) : (
          <p className={styles.asideEmpty}>Elige un objetivo para ver tu requerimiento.</p>
        )}
        <p className={styles.asideNote}>
          El ajuste por objetivo es un punto de partida habitual, no una prescripción.
        </p>
      </aside>
    </section>
  );
}
