import { Button } from '../../components/Button';
import type { EatingStyle } from '../../api/planGenerator';
import { ChoiceCard, LockedTeaser } from './GeneratorChrome';
import type { FunnelState } from './funnelState';
import styles from './PlanGenerator.module.css';

/**
 * Paso 3: cómo se reparte.
 *
 * <p>Estándar español y mediterránea, y nada más. El diseño de partida era mexicano
 * —alimentos y equivalencias SMAE— y aquí la despensa es la nuestra: 23 alimentos del
 * catálogo, todos de supermercado español.
 *
 * <p>Vegetariana y vegana no se ofrecen todavía, y no por olvido: las etiquetas existen
 * desde V50 pero ningún alimento está etiquetado, y un plan vegetariano generado desde
 * este catálogo sería avena, huevos y arroz cinco días seguidos. Va con candado hasta
 * que la despensa dé para cumplirlo.
 */
const DAYS = [
  { value: '5', title: 'Plan de 5 días', description: 'De lunes a viernes' },
  { value: '7', title: 'Plan de 7 días', description: 'Incluye fines de semana' },
] as const;

const STYLES: ReadonlyArray<{
  readonly value: EatingStyle;
  readonly glyph: string;
  readonly title: string;
  readonly description: string;
}> = [
  {
    value: 'ESTANDAR_ESPANOL',
    glyph: '🇪🇸',
    title: 'Estándar español',
    description: 'Alimentos y recetas de aquí',
  },
  {
    value: 'MEDITERRANEA',
    glyph: '🫒',
    title: 'Mediterránea',
    description: 'Más pescado, verdura y aceite de oliva',
  },
];

const MEALS = [
  { value: '3', title: '3 comidas', description: 'Simple' },
  { value: '4', title: '4 comidas', description: 'Equilibrado' },
  { value: '5', title: '5 comidas', description: 'Óptimo' },
  { value: '6', title: '6 comidas', description: 'Muy frecuente' },
] as const;

interface StepPreferencesProps {
  readonly state: FunnelState;
  readonly onChange: (change: Partial<FunnelState>) => void;
  readonly onBack: () => void;
  readonly onNext: () => void;
}

export function StepPreferences({ state, onChange, onBack, onNext }: StepPreferencesProps) {
  return (
    <section className={styles.step2col} aria-labelledby="paso-3">
      <div>
        <h2 className={styles.stepTitle} id="paso-3">
          Estructura del plan
        </h2>
        <p className={styles.stepLead}>Cuántos días, qué estilo y cuántas comidas al día.</p>

        <fieldset className={styles.group}>
          <legend className={styles.legend}>Duración</legend>
          <div className={styles.grid2}>
            {DAYS.map((option) => (
              <ChoiceCard
                key={option.value}
                name="dias"
                value={option.value}
                checked={state.daysPerWeek === Number(option.value)}
                onSelect={(value) => onChange({ daysPerWeek: Number(value) })}
                glyph="📅"
                title={option.title}
                description={option.description}
              />
            ))}
          </div>
        </fieldset>

        <fieldset className={styles.group}>
          <legend className={styles.legend}>Estilo de alimentación</legend>
          <div className={styles.grid2}>
            {STYLES.map((option) => (
              <ChoiceCard
                key={option.value}
                name="estilo"
                value={option.value}
                checked={state.eatingStyle === option.value}
                onSelect={(value) => onChange({ eatingStyle: value as EatingStyle })}
                glyph={option.glyph}
                title={option.title}
                description={option.description}
              />
            ))}
          </div>
        </fieldset>

        <fieldset className={styles.group}>
          <legend className={styles.legend}>Comidas al día</legend>
          <div className={styles.grid4}>
            {MEALS.map((option) => (
              <ChoiceCard
                key={option.value}
                name="comidas"
                value={option.value}
                checked={state.mealsPerDay === Number(option.value)}
                onSelect={(value) => onChange({ mealsPerDay: Number(value) })}
                glyph="🍽️"
                title={option.title}
                description={option.description}
              />
            ))}
          </div>
        </fieldset>

        <LockedTeaser
          title="+ Dietas vegetariana y vegana"
          examples="Cuando el catálogo dé para cumplirlas sin repetir avena cinco días"
        />
        <LockedTeaser
          title="+ Distribución de porciones por comida"
          examples="Decide exactamente cuánto de cada grupo entra en cada comida"
        />

        <div className={styles.actionsSplit}>
          <Button type="button" variant="ghost" onClick={onBack}>
            ← Anterior
          </Button>
          <Button type="button" onClick={onNext}>
            Siguiente →
          </Button>
        </div>
      </div>

      <aside className={styles.aside}>
        <h3 className={styles.asideTitle}>Así lo personalizamos</h3>
        <ul className={styles.asideList}>
          <li>
            <strong>Alimentos reales.</strong> Los del catálogo de FORMA, con sus macros medidos.
          </li>
          <li>
            <strong>Raciones, no gramos sueltos.</strong> «Un plátano» sigue siendo un plátano.
          </li>
          <li>
            <strong>Lista de la compra.</strong> Con productos de Mercadona y sus precios.
          </li>
        </ul>
      </aside>
    </section>
  );
}
