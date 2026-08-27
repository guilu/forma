import { useRef, type KeyboardEvent } from 'react';
import { Button } from '../../components/Button';
import { Chip } from '../../components/Chip';
import { Icon } from '../../components/Icon';
import { IconButton } from '../../components/IconButton';
import type { BodyMeasurement } from '../../api/bodyMeasurements';
import { formatShortDate } from '../dateLabel';
import { localIsoDate } from '../localIsoDate';
import { closestIndexTo, indexAfterJump } from './measurementNavigation';
import styles from './MeasurementNavigator.module.css';

/**
 * El selector de fecha de la cabecera del panel (`docs/selector-fecha.html`).
 *
 * <p>Antes eran dos flechas y nada más: con 900 mediciones, llegar a la de hace
 * un año eran novecientas pulsaciones. Ahora la misma barra ofrece tres formas
 * de moverse, de más gruesa a más fina — saltos rápidos, calendario, y las dos
 * flechas de siempre para el ajuste de una en una.
 *
 * <p>Se navega por las fechas que TIENEN medición, no por el calendario, así
 * que ni el calendario ni los saltos aterrizan en una fecha exacta: caen en la
 * medición más cercana a la pedida. Ver {@link closestIndexTo}.
 *
 * <p><b>«Última» es una pastilla y los otros dos son botones</b>, aunque los
 * tres se parezcan. Estar en la última medición es un estado en el que se puede
 * estar, y la pastilla lo reporta con `aria-pressed`; «-30 d» es un salto
 * relativo, una acción que se ejecuta y se acabó. En la maqueta los tres eran
 * chips y a los dos de salto se les encendía el estado 200 ms como respuesta
 * al toque — un estado falso que un lector de pantalla anunciaría como tal.
 * Esa respuesta al toque ya la da `:active` sin mentir. Es la distinción que
 * `Chip` documenta: la pastilla informa de una elección, el botón invita a una
 * acción.
 *
 * <p>El calendario del sistema se abre desde un `input type="date"` escondido,
 * que es la única forma de tener el selector nativo de cada plataforma sin
 * escribir uno. El botón visible es el que se ve y el que recibe el foco.
 */
export function MeasurementNavigator({
  history,
  selected,
  onSelect,
}: {
  /** Newest first, as the API returns it. */
  readonly history: readonly BodyMeasurement[];
  readonly selected: number;
  readonly onSelect: (index: number) => void;
}) {
  const picker = useRef<HTMLInputElement>(null);
  const current = new Date(history[selected].measuredAt);
  const onNewest = selected === 0;
  const onOldest = selected >= history.length - 1;

  /*
   * Las flechas responden también a izquierda y derecha, pero sólo con el foco
   * dentro de la barra. La maqueta las escuchaba en `window`: eso se lleva por
   * delante las flechas de toda la página, que es demasiado para un control de
   * la cabecera.
   */
  function onKeyDown(event: KeyboardEvent<HTMLDivElement>) {
    if (event.key === 'ArrowLeft' && !onOldest) {
      event.preventDefault();
      onSelect(selected + 1);
    }
    if (event.key === 'ArrowRight' && !onNewest) {
      event.preventDefault();
      onSelect(selected - 1);
    }
  }

  return (
    <div className={styles.controls}>
      <div className={styles.jumps} role="group" aria-label="Saltos rápidos">
        <Chip size="sm" selected={onNewest} onClick={() => onSelect(0)}>
          Última
        </Chip>
        <Button
          variant="secondary"
          className={styles.jump}
          disabled={onOldest}
          onClick={() => onSelect(indexAfterJump(history, selected, 'back30d'))}
        >
          -30 d
        </Button>
        <Button
          variant="secondary"
          className={styles.jump}
          disabled={onOldest}
          onClick={() => onSelect(indexAfterJump(history, selected, 'back1y'))}
        >
          -1 año
        </Button>
      </div>

      <div className={styles.stepper} onKeyDown={onKeyDown}>
        <IconButton
          variant="ghost"
          size="sm"
          label="Medición anterior"
          // `history` is newest-first, so "previous" walks the index up.
          disabled={onOldest}
          onClick={() => onSelect(selected + 1)}
        >
          <Icon name="chevron" size={16} className={styles.arrowPrev} />
        </IconButton>

        <button type="button" className={styles.dateTrigger} onClick={openPicker}>
          <Icon name="calendar" size={15} className={styles.calendarIcon} />
          {/*
           * La fecha es el nombre accesible del botón, no una etiqueta aparte:
           * un `aria-label` de «elegir fecha» la habría tapado justo para quien
           * no puede leerla. `aria-live` la anuncia al cambiar, que es lo que
           * hace útiles a las flechas sin mirar.
           */}
          <span className={styles.dateText} aria-live="polite">
            {formatShortDate(current)}
          </span>
          <span className={styles.srOnly}>Elegir otra fecha</span>
        </button>

        <IconButton
          variant="ghost"
          size="sm"
          label="Medición siguiente"
          disabled={onNewest}
          onClick={() => onSelect(selected - 1)}
        >
          <Icon name="chevron" size={16} />
        </IconButton>

        {/*
         * Escondido pero real: es él quien abre el calendario del sistema. No
         * lleva foco ni nombre — el botón de al lado es el control, y dos
         * paradas de tabulación para una sola cosa sobran.
         */}
        <input
          ref={picker}
          type="date"
          tabIndex={-1}
          aria-hidden="true"
          className={styles.hiddenPicker}
          value={localIsoDate(current)}
          onChange={(event) => {
            if (event.target.value === '') return;
            onSelect(closestIndexTo(history, Date.parse(`${event.target.value}T12:00:00`)));
          }}
        />
      </div>
    </div>
  );

  function openPicker() {
    const input = picker.current;
    if (!input) return;
    /*
     * `showPicker` es lo que abre el calendario nativo sin que el `input` se
     * vea. Donde no exista, el foco al menos deja el control alcanzable en vez
     * de dejar el botón sin hacer nada.
     */
    try {
      input.showPicker();
    } catch {
      input.focus();
    }
  }
}
