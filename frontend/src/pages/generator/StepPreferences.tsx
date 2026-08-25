import { Button } from '../../components/Button';
import type { EatingStyle } from '../../api/planGenerator';
import type { IconName } from '../../components/Icon';
import { ChoiceCard, LockedTeaser } from './GeneratorChrome';
import { EATING_STYLE_LABELS, type FunnelState } from './funnelState';
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
const DAYS = [5, 7] as const;

const STYLES: ReadonlyArray<{
  readonly value: EatingStyle;
  readonly icon: IconName;
  readonly title: string;
  readonly description: string;
}> = [
  {
    value: 'ESTANDAR_ESPANOL',
    icon: 'shopping',
    title: EATING_STYLE_LABELS.ESTANDAR_ESPANOL,
    description: 'Alimentos y recetas de aquí',
  },
  {
    value: 'MEDITERRANEA',
    icon: 'leaf',
    title: EATING_STYLE_LABELS.MEDITERRANEA,
    description: 'Más pescado, verdura y aceite de oliva',
  },
];

const MEALS = [3, 4, 5, 6] as const;

/** Los siete días, para pintar cuáles entran. `5 días` es de lunes a viernes. */
const WEEK = ['L', 'M', 'X', 'J', 'V', 'S', 'D'] as const;

/**
 * Cómo queda el día según cuántas comidas se pidan.
 *
 * <p>Sustituye a las cuatro descripciones que llevaban las tarjetas —«Simple»,
 * «Equilibrado», «Óptimo», «Muy frecuente»—, que no decían nada que se pudiera usar
 * para elegir: nadie sabe si quiere «óptimo» hasta que ve que son cinco comidas y
 * cuáles.
 *
 * <p><b>Sin kcal por comida, a propósito.</b> El diseño las pedía, y el generador no
 * tiene reparto: `mealsPerDay` viaja hasta `PlanDraftAccepted` sin que nada lo use
 * para repartir nada. Un «Desayuno · 533 kcal» sería un número inventado en la
 * pantalla que presume de que la fórmula la lleva el servidor.
 *
 * <p><b>Pero las barras sí tienen forma, y hay que saber qué es.</b> `share` es un
 * perfil de reparto típico, escrito aquí y fijo: no sale del servidor, no depende de
 * las respuestas de nadie y NO es el reparto del plan de quien mira. Es un dibujo de
 * cómo cae un día —el desayuno y la comida pesan, lo de en medio no—, y por eso no
 * lleva cifras ni sale del `aria-hidden`: una barra sin número dice «así de grande,
 * más o menos», que es lo único que aquí se puede decir con verdad. El reparto de
 * verdad es lo que vende el candado de «Distribución de porciones por comida», y el
 * día que exista lo mandará el servidor y estos números se irán.
 *
 * <p>Cada fila suma 100, que es la única regla que tienen que cumplir.
 *
 * <p>Los nombres salen del vocabulario que ya usa la app (`MealType`: BREAKFAST,
 * MID_MORNING, LUNCH, SNACK, DINNER). «Recena» es el único que el dominio todavía no
 * tiene; entra aquí porque una sexta comida en España se llama así, y el enum tendrá
 * que aprenderla cuando el generador construya comidas de verdad.
 */
interface DayShapeMeal {
  readonly name: string;
  /** Cuánto del día pesa esta comida, en tanto por ciento. Ilustrativo. Ver arriba. */
  readonly share: number;
}

const DAY_SHAPE: Readonly<Record<number, readonly DayShapeMeal[]>> = {
  3: [
    { name: 'Desayuno', share: 30 },
    { name: 'Comida', share: 40 },
    { name: 'Cena', share: 30 },
  ],
  4: [
    { name: 'Desayuno', share: 25 },
    { name: 'Comida', share: 35 },
    { name: 'Merienda', share: 15 },
    { name: 'Cena', share: 25 },
  ],
  5: [
    { name: 'Desayuno', share: 25 },
    { name: 'Media mañana', share: 10 },
    { name: 'Comida', share: 30 },
    { name: 'Merienda', share: 10 },
    { name: 'Cena', share: 25 },
  ],
  6: [
    { name: 'Desayuno', share: 22 },
    { name: 'Media mañana', share: 10 },
    { name: 'Comida', share: 28 },
    { name: 'Merienda', share: 10 },
    { name: 'Cena', share: 22 },
    { name: 'Recena', share: 8 },
  ],
};

/**
 * De tanto por ciento a píxeles de barra.
 *
 * <p>La escala es la MISMA para las cuatro formas, no una por cuenta de comidas: así
 * la barra de la comida encoge al partir el día en seis, que es lo que de verdad pasa.
 * Normalizando cada fila a su propio máximo, las cuatro se verían iguales y el
 * selector no enseñaría nada al cambiar de opción.
 *
 * <p>El suelo de 10 px es para que la comida más pequeña —un 8 % en la forma de seis—
 * siga siendo una barra y no una línea: por debajo, los 8 px de `--radius-md` del
 * remate se comen la pieza entera y deja de leerse como parte de la misma serie.
 *
 * <p>Va en el `style` del elemento y no en `.dayShapeBar` porque es un dato, no una
 * decisión de estilo: la hoja no puede saber cuánto pesa cada comida. Es el mismo
 * camino que ya usa `ActivityScale` para el alto de sus barritas. La regla CSS
 * conserva un alto fijo, que es el que se ve si algún día se pinta una barra suelta.
 */
const BAR_MIN_PX = 10;
const BAR_PX_PER_SHARE = 1.1;

function barHeight(share: number): string {
  return `${Math.max(BAR_MIN_PX, Math.round(share * BAR_PX_PER_SHARE))}px`;
}

interface StepPreferencesProps {
  readonly state: FunnelState;
  readonly onChange: (change: Partial<FunnelState>) => void;
  readonly onBack: () => void;
  readonly onNext: () => void;
}

export function StepPreferences({ state, onChange, onBack, onNext }: StepPreferencesProps) {
  return (
    <section className={styles.step} aria-labelledby="paso-3">
      <h2 className={styles.stepTitle} id="paso-3">
        ¿Cómo lo repartimos?
      </h2>

      <fieldset className={styles.group}>
        <legend className={styles.legend}>Duración</legend>
        <div className={styles.segmented}>
          {DAYS.map((days) => (
            <ChoiceCard
              key={days}
              name="dias"
              value={String(days)}
              checked={state.daysPerWeek === days}
              onSelect={(value) => onChange({ daysPerWeek: Number(value) })}
              title={`${days} días`}
              layout="compact"
            />
          ))}
        </div>
        {/* Decorativo: lo que se elige y se anuncia es «5 días», no siete letras. */}
        <ol className={styles.week} aria-hidden="true">
          {WEEK.map((day, index) => (
            <li key={day} className={index < state.daysPerWeek ? styles.weekDayOn : styles.weekDay}>
              {day}
            </li>
          ))}
        </ol>
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
              icon={option.icon}
              title={option.title}
              description={option.description}
            />
          ))}
        </div>
      </fieldset>

      <fieldset className={styles.group}>
        <legend className={styles.legend}>Comidas al día</legend>
        <div className={styles.segmented}>
          {MEALS.map((meals) => (
            <ChoiceCard
              key={meals}
              name="comidas"
              value={String(meals)}
              checked={state.mealsPerDay === meals}
              onSelect={(value) => onChange({ mealsPerDay: Number(value) })}
              title={String(meals)}
              srLabel={`${meals} comidas`}
              layout="compact"
            />
          ))}
        </div>
        <ol className={styles.dayShape} aria-hidden="true">
          {(DAY_SHAPE[state.mealsPerDay] ?? []).map((meal) => (
            <li key={meal.name} className={styles.dayShapeSlot}>
              <span className={styles.dayShapeBar} style={{ height: barHeight(meal.share) }} />
              <span className={styles.dayShapeName}>{meal.name}</span>
            </li>
          ))}
        </ol>
      </fieldset>

      <div className={styles.lockedGroup}>
        <LockedTeaser title="Dietas vegetariana y vegana" />
        <LockedTeaser title="Distribución de porciones por comida" />
      </div>

      <div className={styles.actionsSplit}>
        <Button type="button" variant="ghost" onClick={onBack}>
          ← Anterior
        </Button>
        <Button type="button" onClick={onNext}>
          Siguiente →
        </Button>
      </div>
    </section>
  );
}
