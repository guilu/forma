import { Link } from 'react-router-dom';
import { Button } from '../../components/Button';
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
    <div className={styles.wrapper}>
      <Card>
        <div className={styles.ready}>
          <span className={styles.readyIcon} aria-hidden="true">
            <Icon name="checkCircle" size={40} />
          </span>
          <h1 className={styles.readyTitle}>Tenemos tus datos</h1>
          <p className={styles.readyLead}>
            Hemos recogido lo que necesitamos para construir tu plan y lo enviaremos a{' '}
            <strong>{email}</strong>.
          </p>

          <div className={styles.readyNext}>
            <h2 className={styles.readyNextTitle}>Qué puedes hacer ahora</h2>
            <ul className={styles.readyList}>
              <li>
                <strong>Crea tu cuenta.</strong> Es donde vivirá tu plan: podrás activarlo,
                registrar lo que comes y ver si vas cumpliendo.
              </li>
              <li>
                <strong>Ajústalo cuando quieras.</strong> Cambiar un alimento recalcula los macros
                solo, sin que tengas que tocar un número.
              </li>
              <li>
                <strong>Lleva la compra encima.</strong> El plan sabe qué productos necesitas y lo
                que cuestan.
              </li>
            </ul>
          </div>

          <div className={styles.readyActions}>
            <Link className={styles.readyCta} to="/register">
              Crear mi cuenta gratis
            </Link>
            <Button type="button" variant="ghost" onClick={onRestart}>
              Generar otro plan
            </Button>
          </div>
        </div>
      </Card>

      <p className={styles.disclaimer}>
        El plan generado es orientativo y no sustituye la valoración de un profesional de la salud.
      </p>
    </div>
  );
}
