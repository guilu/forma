import type { ReactNode } from 'react';
import { Icon, type IconName } from '../../components/Icon';
import styles from './PlanGenerator.module.css';

/**
 * Las piezas que se repiten en los cuatro pasos: el progreso, la cifra viva de
 * la cabecera, las opciones que se eligen y el bloque con candado.
 *
 * <p>FOR-190 rehízo el embudo en UNA columna. Antes cada paso era una rejilla de
 * 4,6fr/1fr con un panel lateral, y ese panel era la mayor fuente de texto de la
 * pantalla: repetía en prosa lo que el formulario ya decía. Ahora el lateral no
 * existe y su trabajo lo hace {@link EnergyHeadline}, una sola cifra arriba que se
 * mueve con cada respuesta — el efecto de contestar se ve, en vez de leerse.
 */
export const STEPS = [
  { key: 'paciente', label: 'Tus datos' },
  { key: 'clinico', label: 'Objetivo' },
  { key: 'preferencias', label: 'Preferencias' },
  { key: 'contacto', label: 'Tu plan' },
] as const;

/**
 * El progreso, como cuatro segmentos y una frase.
 *
 * <p>Era una fila de círculos numerados con su etiqueta debajo, y en móvil las
 * etiquetas se escondían con `display: none` — o sea que el ancho pequeño ya
 * había decidido que sobraban. Cuatro barras dicen lo mismo a cualquier ancho y
 * no piden traducción; «Paso 2 de 4» es la versión que se lee en voz alta, y la
 * que anuncia el lector de pantalla mientras las barras quedan decorativas.
 */
export function Stepper({ current }: { readonly current: number }) {
  return (
    <div className={styles.progress}>
      <p className={styles.progressLabel}>
        Paso {current + 1} de {STEPS.length}
      </p>
      <ol className={styles.progressTrack} aria-hidden="true">
        {STEPS.map((step, index) => (
          <li key={step.key} className={index <= current ? styles.segmentOn : styles.segment} />
        ))}
      </ol>
    </div>
  );
}

/**
 * La cifra viva de la cabecera: lo que costaba entender, en un número.
 *
 * <p>Lo calcula el servidor y esta pantalla lo pinta, igual que antes — que el
 * desglose se haya ido no cambia de quién es la fórmula. Mientras falten datos
 * enseña un guion largo en el sitio exacto donde aparecerá el número, para que la
 * cabecera no dé un salto de altura al llegar la respuesta.
 *
 * <p>`aria-live="polite"` porque el número cambia sin que nadie pulse nada: se
 * recalcula al soltar un deslizador o al elegir un objetivo, y sin anunciarlo el
 * único efecto visible de contestar se lo pierde quien no ve la pantalla.
 */
export function EnergyHeadline({
  eyebrow,
  value,
  unit,
  aside,
  pending,
}: {
  readonly eyebrow: string;
  readonly value: string | undefined;
  readonly unit: string;
  /** La pastilla de la derecha: el ajuste por objetivo, o de dónde sale la cuenta. */
  readonly aside?: ReactNode;
  /** Qué poner cuando todavía no hay número, en una línea. */
  readonly pending: string;
}) {
  return (
    <div className={styles.headline}>
      <div className={styles.headlineMain}>
        <p className={styles.headlineEyebrow}>{eyebrow}</p>
        <p className={styles.headlineValue} aria-live="polite">
          {value === undefined ? (
            <span className={styles.headlinePending}>{pending}</span>
          ) : (
            <>
              <span className={styles.headlineNumber}>{value}</span>
              <span className={styles.headlineUnit}>{unit}</span>
            </>
          )}
        </p>
      </div>
      {aside && <div className={styles.headlineAside}>{aside}</div>}
    </div>
  );
}

/**
 * Una opción que se elige tocándola.
 *
 * <p>Un `radio` de verdad y no un `div` con `onClick`: el teclado y los lectores de
 * pantalla ya saben qué es un grupo de opciones excluyentes, y reimplementarlo peor
 * es la forma más común de dejar fuera a alguien. Eso no cambió con el rediseño —
 * lo único que cambia es el aspecto.
 *
 * <p>`layout` es la forma de la tarjeta, no su contenido: `row` es icono y texto en
 * fila, `stacked` los apila y centra, `compact` es la pastilla de un segmentado
 * (solo texto, sin descripción). El icono llega por nombre y no como emoji, para
 * que herede `currentColor` y se tiña con la tarjeta al elegirla.
 */
export function ChoiceCard({
  name,
  value,
  checked,
  onSelect,
  icon,
  title,
  description,
  disabled,
  layout = 'row',
  srLabel,
}: {
  readonly name: string;
  readonly value: string;
  readonly checked: boolean;
  readonly onSelect: (value: string) => void;
  readonly icon?: IconName;
  readonly title: string;
  readonly description?: string;
  readonly disabled?: boolean;
  readonly layout?: 'row' | 'stacked' | 'compact';
  /**
   * Nombre accesible, cuando el texto visible se queda corto a propósito. Las
   * pastillas de «comidas al día» enseñan «5» —cuatro veces «N comidas» no cabe
   * en un móvil y parte en dos líneas—, pero quien navega a oídas tiene que oír
   * «5 comidas», no «5».
   */
  readonly srLabel?: string;
}) {
  const shape =
    layout === 'stacked'
      ? styles.choiceStacked
      : layout === 'compact'
        ? styles.choiceCompact
        : styles.choiceRow;
  return (
    <label
      className={`${checked ? styles.choiceOn : styles.choice} ${shape}`}
      data-disabled={disabled}
    >
      <input
        type="radio"
        className={styles.choiceInput}
        name={name}
        value={value}
        checked={checked}
        disabled={disabled}
        aria-label={srLabel}
        onChange={() => onSelect(value)}
      />
      {icon && (
        <span className={styles.choiceGlyph}>
          <Icon name={icon} size={layout === 'stacked' ? 24 : 22} />
        </span>
      )}
      <span className={styles.choiceText}>
        <span className={styles.choiceTitle}>{title}</span>
        {description && <span className={styles.choiceDescription}>{description}</span>}
      </span>
      {checked && layout !== 'compact' && (
        <span className={styles.choiceCheck} aria-hidden="true">
          <Icon name="check" size={14} />
        </span>
      )}
    </label>
  );
}

/**
 * La escala de actividad: cinco niveles dibujados como cinco barras.
 *
 * <p>Cada nivel traía su frase («Ejercicio 3–5 días por semana») y las cinco juntas
 * eran cinco líneas de texto para elegir una cosa. Las barras dicen «esto es más
 * que lo anterior» sin leerse, que es lo único que hay que entender para elegir, y
 * la frase del nivel elegido se lee UNA vez debajo de la fila.
 *
 * <p>Las barras son decorativas: el nombre del nivel sigue siendo la etiqueta del
 * `radio`, así que quien no ve la pantalla oye «Moderado», no «tres de cinco».
 */
export function ActivityScale({
  name,
  levels,
  selected,
  onSelect,
}: {
  readonly name: string;
  readonly levels: ReadonlyArray<{
    readonly value: string;
    readonly label: string;
    readonly description: string;
  }>;
  readonly selected: string;
  readonly onSelect: (value: string) => void;
}) {
  const current = levels.find((level) => level.value === selected);
  return (
    <>
      <div className={styles.scale}>
        {levels.map((level, index) => {
          const checked = level.value === selected;
          return (
            <label key={level.value} className={checked ? styles.scaleStepOn : styles.scaleStep}>
              <input
                type="radio"
                className={styles.choiceInput}
                name={name}
                value={level.value}
                checked={checked}
                onChange={() => onSelect(level.value)}
              />
              <span className={styles.scaleBars} aria-hidden="true">
                {[0, 1, 2, 3, 4].map((bar) => (
                  <span
                    key={bar}
                    className={bar <= index ? styles.scaleBarOn : styles.scaleBar}
                    style={{ height: `${5 + bar * 2.75}px` }}
                  />
                ))}
              </span>
              <span className={styles.scaleLabel}>{level.label}</span>
            </label>
          );
        })}
      </div>
      {current && <p className={styles.scaleHint}>{current.description}</p>}
    </>
  );
}

/**
 * Lo que se desbloquea al suscribirse.
 *
 * <p>Enseña que existe y no lo pide, que es exactamente para lo que sirve. En el
 * caso de las patologías eso resuelve además el problema legal: son datos de salud,
 * y no se recogen — se enseña que el plan de pago los tiene en cuenta.
 *
 * <p>No es un botón ni un desplegable: no hay nada que abrir. Un candado que al
 * pulsarlo no hace nada enfada más que un candado que se ve cerrado.
 *
 * <p>FOR-190 lo bajó de dos cajas por paso a una fila. Dos cajas punteadas seguidas
 * ocupaban más que las opciones que sí se pueden elegir, y un paso donde lo
 * bloqueado pesa más que lo disponible se lee como un muro, no como un adelanto. El
 * contenido no se ha recortado: las dos listas caben en una frase.
 */
export function LockedTeaser({ title }: { readonly title: string }) {
  return (
    <p className={styles.locked}>
      <span className={styles.lockedIcon}>
        <Icon name="lock" size={16} />
      </span>
      <span className={styles.lockedText}>{title}</span>
      <span className={styles.lockedBadge}>PRO</span>
    </p>
  );
}
