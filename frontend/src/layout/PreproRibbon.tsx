import { isPreproHost } from './preproHost';
import styles from './PreproRibbon.module.css';

interface PreproRibbonProps {
  /** Commit desplegado. Vacío cuando el build no pudo averiguarlo. */
  readonly sha?: string;
  /** Sólo para pruebas; en producción lo decide el host real. */
  readonly hostname?: string;
}

/**
 * Banda diagonal que avisa de que esto no es producción.
 *
 * <p>Va en la barra superior, a la derecha de la marca, y toma los colores
 * invertidos del tema: el fondo es el color del texto y el texto es el color
 * del fondo. Así el aviso destaca en claro y en oscuro sin declarar un solo
 * color nuevo, y sigue al tema cuando el usuario lo cambia.
 *
 * <p>El commit, no una versión: `package.json` marca 0.0.1 desde el andamiaje y
 * nadie lo sube, así que un número de versión aquí afirmaría algo que no es
 * cierto. El sha dice exactamente qué hay desplegado.
 */
export function PreproRibbon({ sha = __BUILD_SHA__, hostname }: PreproRibbonProps) {
  const host = hostname ?? window.location.hostname;
  if (!isPreproHost(host)) return null;

  return (
    <div
      className={styles.ribbon}
      role="status"
      aria-label={sha ? `Entorno de preproducción, versión ${sha}` : 'Entorno de preproducción'}
    >
      {/*
       * El contenido va contra-sesgado: el sesgo vive en el contenedor, así que
       * sin esto el texto saldría inclinado con él.
       */}
      <span className={styles.content} aria-hidden="true">
        <span className={styles.label}>PREPRO</span>
        {sha ? (
          <span className={styles.sha} data-testid="prepro-sha">
            {sha}
          </span>
        ) : null}
      </span>
    </div>
  );
}
