import { Link } from 'react-router-dom';
import { PrivacyNotice } from './PrivacyNotice';
import styles from './PrivacyPage.module.css';

/**
 * El aviso de privacidad, en `/privacidad`.
 *
 * <p>El texto vive en {@link PrivacyNotice}, que es el mismo que abre el paso 4 del embudo
 * en un modal. Aquí queda la página que lo enmarca: su encabezado y la vuelta al generador.
 */
export function PrivacyPage() {
  return (
    <main className={styles.page}>
      <article className={styles.doc}>
        <PrivacyNotice
          heading={
            <>
              <p className={styles.eyebrow}>Aviso de privacidad</p>
              <h1 className={styles.title}>Qué hacemos con tus datos</h1>
            </>
          }
        />

        <footer className={styles.foot}>
          <Link to="/plan">← Volver al generador de plan</Link>
        </footer>
      </article>
    </main>
  );
}
