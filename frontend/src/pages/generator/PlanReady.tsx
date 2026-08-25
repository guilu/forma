import { Button } from '../../components/Button';
import { ButtonLink } from '../../components/ButtonLink';
import { Card } from '../../components/Card';
import { Icon } from '../../components/Icon';
import styles from './PlanGenerator.module.css';

/**
 * La pantalla final.
 *
 * <p>Dice lo que ha pasado de verdad y nada más. Hoy no se genera plan, no se manda
 * correo y no se guarda nada: el endpoint valida el embudo y responde. Así que esta
 * pantalla NO ofrece descargar un PDF que no existe ni promete un correo que no sale
 * — enseña el siguiente paso real, que es registrarse.
 *
 * <p>Cuando el generador, el PDF y el correo existan, aquí llegan el botón de descarga
 * y la frase de «revisa tu bandeja». Ponerlos antes sería enseñar botones que no hacen
 * nada, que es la forma más rápida de que alguien deje de creerse el resto.
 */
export function PlanReady({
  email,
  onRestart,
}: {
  readonly email: string;
  readonly onRestart: () => void;
}) {
  return (
    <div className={styles.readyWrapper}>
      <Card>
        <div className={styles.ready}>
          {/* Fila 1: lo que ha pasado. */}
          <div className={styles.readyHead}>
            <span className={styles.readyIcon} aria-hidden="true">
              <Icon name="checkCircle" size={40} />
            </span>
            <h1 className={styles.readyTitle}>Tenemos tus datos</h1>
            <p className={styles.readyLead}>
              Hemos recogido lo que necesitamos para construir tu plan y lo enviaremos a{' '}
              <strong>{email}</strong>.
            </p>
          </div>

          {/*
            La raya separa dos cosas distintas: arriba el acuse de recibo, abajo lo que
            viene después. Decorativa —`role="presentation"`— porque el salto ya lo
            anuncia el encabezado de la sección de abajo, y un `separator` más un
            encabezado son dos avisos del mismo corte para quien navega escuchando.
          */}
          <hr className={styles.readyRule} role="presentation" />

          {/* Fila 2: lo que puede hacer ahora. */}
          <div className={styles.readyNext}>
            <h2 className={styles.readyNextTitle}>Qué puedes hacer ahora</h2>
            <ul className={styles.readyBubbles}>
              <li className={styles.readyBubble}>
                <p className={styles.readyBubbleTitle}>Crea tu cuenta</p>
                <p className={styles.readyBubbleText}>
                  Es donde vivirá tu plan: podrás activarlo, registrar lo que comes y ver si vas
                  cumpliendo.
                </p>
              </li>
              <li className={styles.readyBubble}>
                <p className={styles.readyBubbleTitle}>Ajústalo cuando quieras</p>
                <p className={styles.readyBubbleText}>
                  Cambiar un alimento recalcula los macros solo, sin que tengas que tocar un número.
                </p>
              </li>
              <li className={styles.readyBubble}>
                <p className={styles.readyBubbleTitle}>Lleva la compra encima</p>
                <p className={styles.readyBubbleText}>
                  El plan sabe qué productos necesitas y lo que cuestan.
                </p>
              </li>
            </ul>
          </div>

          {/*
            El de volver a empezar va PRIMERO en el orden del documento, que es el que
            recorre un teclado, y a la izquierda. El de crear cuenta es el que se quiere
            pulsar y se queda con el acento: dos botones con el mismo peso no serían una
            recomendación, serían una pregunta.
          */}
          <div className={styles.readyActions}>
            <Button type="button" variant="secondary" onClick={onRestart}>
              Generar otro plan
            </Button>
            <ButtonLink variant="accent" to="/register">
              Crear mi cuenta gratis
            </ButtonLink>
          </div>
        </div>
      </Card>

      <p className={styles.disclaimer}>
        El plan generado es orientativo y no sustituye la valoración de un profesional de la salud.
      </p>
    </div>
  );
}
