import type { ReactNode } from 'react';
import styles from './PrivacyPage.module.css';

/**
 * El texto del aviso de privacidad, sin el marco que lo enseña.
 *
 * <p>Existe porque el paso 4 del generador pide marcar «he leído y acepto el aviso de
 * privacidad» y ese enlace fue un 404 hasta V61. Pedir que alguien acepte un documento que no
 * se puede leer no es un descuido de maquetación: es recoger un consentimiento que no lo es.
 *
 * <p><b>Vive aparte de la página porque se lee en dos sitios.</b> `/privacidad` lo enmarca
 * como página y el paso 4 lo abre en un modal, y el texto que se acepta ahí tiene que ser
 * palabra por palabra el mismo: dos copias serían libres de separarse, y entonces nadie
 * podría decir qué se aceptó. {@link PRIVACY_POLICY_VERSION} sale de aquí por lo mismo.
 *
 * <p>El encabezado lo pone quien lo usa. La página necesita el suyo —un `h1` y su
 * antetítulo—; el modal ya se anuncia con el título del propio diálogo, y repetirlo dentro
 * sería decirle al lector de pantalla dos veces lo mismo.
 *
 * <p><b>Es RGPD, no LFPDPPP.</b> El modelo del que se partió era mexicano —«Aviso de
 * Privacidad» con «Derechos ARCO»— y FORMA es un producto español: catálogo de supermercado
 * de aquí, país por defecto `ES`, y datos de salud que el propio código ya identifica como
 * categoría especial del artículo 9. Traducir aquella estructura habría dejado fuera cuatro
 * cosas que el artículo 13 exige: la base jurídica de cada tratamiento, el plazo de
 * conservación, el derecho a retirar el consentimiento y el de reclamar ante la AEPD.
 *
 * <p><b>Todo lo que dice esto lo hace el código.</b> Cada tratamiento descrito abajo se
 * corresponde con una tabla, y cada plazo con algo que lo cumple: los doce meses del lead los
 * borra `PlanLeadRetentionJob`. Un aviso que promete más de lo que el programa hace es el
 * mismo problema que un botón que no hace nada, con consecuencias peores.
 *
 * <p><b>Los `[COMPLETAR …]` son deliberados.</b> AGENTS.md prohíbe inventar lo que la
 * especificación no dice, y la razón social, el NIF, el domicilio y el proveedor de
 * alojamiento no están en el repositorio. Se marcan como la portada marca `[COMPLETAR
 * PRECIO]`: visibles, imposibles de publicar sin darse cuenta. <b>Esto no está listo para
 * producción hasta que no quede ninguno</b>, y conviene que lo revise alguien de legal antes.
 */

/** Debe coincidir con `PlanLeadService.PRIVACY_POLICY_VERSION`, que es lo que se guarda al aceptar. */
export const PRIVACY_POLICY_VERSION = '2026-08-22';

const PENDING = '[COMPLETAR';

export function PrivacyNotice({ heading }: { readonly heading?: ReactNode }) {
  return (
    <>
      <header className={styles.head}>
        {heading}
        <p className={styles.lead}>
          En corto: los usamos para construirte el plan que pides y para que la aplicación funcione.
          No los vendemos, no los cedemos a nadie que no sea necesario para prestarte el servicio, y
          no llevamos ninguna herramienta de analítica ni de rastreo.
        </p>
        <p className={styles.version}>Última actualización: {PRIVACY_POLICY_VERSION}</p>
      </header>

      <section className={styles.section} aria-labelledby="responsable">
        <h2 className={styles.h2} id="responsable">
          1. Quién es responsable
        </h2>
        <dl className={styles.definitions}>
          <div>
            <dt>Responsable</dt>
            <dd>{PENDING} RAZÓN SOCIAL]</dd>
          </div>
          <div>
            <dt>NIF</dt>
            <dd>{PENDING} NIF]</dd>
          </div>
          <div>
            <dt>Domicilio</dt>
            <dd>{PENDING} DOMICILIO]</dd>
          </div>
          <div>
            <dt>Correo de contacto</dt>
            <dd>{PENDING} CORREO DE CONTACTO]</dd>
          </div>
          <div>
            <dt>Delegado de protección de datos</dt>
            <dd>{PENDING} SI PROCEDE — no es obligatorio para todas las empresas]</dd>
          </div>
        </dl>
      </section>

      <section className={styles.section} aria-labelledby="datos">
        <h2 className={styles.h2} id="datos">
          2. Qué datos tratamos
        </h2>

        <h3 className={styles.h3}>Si solo pides un plan, sin cuenta</h3>
        <p>
          El generador de la portada recoge tu <strong>nombre</strong> y tu <strong>correo</strong>{' '}
          —para poder enviarte el plan—, tu <strong>país</strong> y, si quieres decirlo,{' '}
          <strong>cómo nos encontraste</strong>. Y las respuestas con las que se calcula el plan:{' '}
          <strong>sexo, edad, peso, altura, nivel de actividad, objetivo</strong>, cuántos días y
          cuántas comidas quieres y qué estilo de alimentación prefieres. Guardamos también el
          requerimiento calórico que te enseñamos, para que el plan que recibas se pueda comparar
          con la cifra que viste.
        </p>
        <p className={styles.note}>
          El generador{' '}
          <strong>no pregunta por patologías, alergias ni restricciones alimentarias</strong>, y no
          hay ningún sitio donde guardarlas. Son datos de salud y no los recogemos en un formulario
          público.
        </p>

        <h3 className={styles.h3}>Si te creas una cuenta</h3>
        <p>
          Tu <strong>correo</strong> y tu <strong>contraseña</strong> —que se guarda cifrada, no en
          claro—, y los datos de perfil que rellenes: nombre, fecha de nacimiento, sexo, altura y
          los objetivos que te marques.
        </p>

        <h3 className={styles.h3}>Lo que registras usando la aplicación</h3>
        <p>
          Medidas corporales (peso, porcentaje de grasa y las demás que apuntes),{' '}
          <strong>fotos de progreso</strong>, lo que comes y bebes, tus sesiones de entrenamiento y
          su seguimiento, tus objetivos y tus listas de la compra.
        </p>
        <p className={styles.note}>
          Buena parte de esto son <strong>datos de salud</strong>: categoría especial del artículo 9
          del RGPD. Los tratamos solo con tu consentimiento explícito, que es lo que das al
          registrarlos, y puedes retirarlo borrándolos o cerrando la cuenta.
        </p>

        <h3 className={styles.h3}>Si conectas una báscula o un servicio externo</h3>
        <p>
          Si autorizas la conexión con Withings, recibimos las medidas que tú autorizas en ese
          proceso. Los permisos de acceso se guardan <strong>cifrados</strong>. La conexión la
          inicias tú y puedes deshacerla cuando quieras desde la aplicación.
        </p>
      </section>

      <section className={styles.section} aria-labelledby="finalidades">
        <h2 className={styles.h2} id="finalidades">
          3. Para qué, y con qué base legal
        </h2>
        <div className={styles.tableWrap}>
          <table className={styles.table}>
            <thead>
              <tr>
                <th scope="col">Para qué</th>
                <th scope="col">Base legal (art. 6 RGPD)</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>Construirte y enviarte el plan que pides en el generador</td>
                <td>Tu consentimiento, y las gestiones previas a un contrato que tú inicias</td>
              </tr>
              <tr>
                <td>Darte la cuenta y que la aplicación funcione</td>
                <td>La ejecución del contrato de servicio</td>
              </tr>
              <tr>
                <td>Guardar tus medidas, fotos y registros de salud</td>
                <td>Tu consentimiento explícito (art. 9.2.a)</td>
              </tr>
              <tr>
                <td>Mandarte recetas y novedades</td>
                <td>Tu consentimiento, que es una casilla aparte y puedes retirar</td>
              </tr>
              <tr>
                <td>Mantener el servicio seguro y evitar abusos</td>
                <td>Nuestro interés legítimo en que el servicio no se caiga ni se use para spam</td>
              </tr>
            </tbody>
          </table>
        </div>
        <p className={styles.note}>
          La casilla de novedades es <strong>independiente</strong> de la de privacidad a propósito:
          aceptar que te mandemos lo que has pedido y aceptar que te escribamos después son dos
          preguntas distintas, y puedes decir que sí a una y que no a la otra.
        </p>
      </section>

      <section className={styles.section} aria-labelledby="plazos">
        <h2 className={styles.h2} id="plazos">
          4. Cuánto tiempo los guardamos
        </h2>
        <dl className={styles.definitions}>
          <div>
            <dt>Si pides un plan y no te registras</dt>
            <dd>
              <strong>12 meses</strong> desde que lo pides. Pasado ese plazo se borran solos.
            </dd>
          </div>
          <div>
            <dt>Si tienes cuenta</dt>
            <dd>
              Mientras la cuenta exista. Si la cierras, se borran, salvo lo que haya que conservar
              por una obligación legal y durante el plazo que esa obligación imponga.
            </dd>
          </div>
          <div>
            <dt>Consentimientos</dt>
            <dd>
              Guardamos qué aceptaste, cuándo y qué versión de este aviso estaba en vigor. Es lo que
              el artículo 7.1 nos exige poder demostrar.
            </dd>
          </div>
        </dl>
      </section>

      <section className={styles.section} aria-labelledby="destinatarios">
        <h2 className={styles.h2} id="destinatarios">
          5. Quién más los ve
        </h2>
        <p>
          <strong>No vendemos tus datos ni los cedemos con fines comerciales.</strong> Los ven
          únicamente quienes hacen falta para prestarte el servicio:
        </p>
        <ul className={styles.list}>
          <li>
            <strong>{PENDING} PROVEEDOR DE ALOJAMIENTO]</strong>, donde se ejecuta la aplicación y
            vive la base de datos.
          </li>
          <li>
            <strong>Withings</strong>, y solo si tú conectas la integración: en ese caso el
            intercambio de datos es entre tu cuenta de Withings y la tuya aquí.
          </li>
        </ul>
        <p className={styles.note}>
          <strong>Transferencias fuera del Espacio Económico Europeo:</strong> {PENDING} INDICAR
          SEGÚN EL PROVEEDOR DE ALOJAMIENTO — si hay transferencia, hay que declarar la garantía que
          la ampara].
        </p>
      </section>

      <section className={styles.section} aria-labelledby="derechos">
        <h2 className={styles.h2} id="derechos">
          6. Qué puedes exigirnos
        </h2>
        <p>
          Escribiendo a <strong>{PENDING} CORREO DE CONTACTO]</strong> puedes ejercer, gratis:
        </p>
        <ul className={styles.list}>
          <li>
            <strong>Acceso:</strong> que te digamos qué tenemos tuyo.
          </li>
          <li>
            <strong>Rectificación:</strong> que corrijamos lo que esté mal.
          </li>
          <li>
            <strong>Supresión:</strong> que lo borremos.
          </li>
          <li>
            <strong>Limitación:</strong> que lo conservemos pero dejemos de usarlo.
          </li>
          <li>
            <strong>Portabilidad:</strong> que te lo demos en un formato que puedas llevarte.
          </li>
          <li>
            <strong>Oposición:</strong> que dejemos de tratarlo.
          </li>
          <li>
            <strong>Retirar tu consentimiento</strong> en cualquier momento, sin que eso afecte a lo
            que hicimos antes de retirarlo.
          </li>
        </ul>
        <p>
          Y si crees que no lo hacemos bien, puedes reclamar ante la{' '}
          <strong>Agencia Española de Protección de Datos</strong> (
          <a href="https://www.aepd.es" target="_blank" rel="noreferrer">
            aepd.es
          </a>
          ). Preferiríamos que nos lo dijeras a nosotros primero, pero es tu derecho y no hace falta
          que pases por aquí.
        </p>
      </section>

      <section className={styles.section} aria-labelledby="cookies">
        <h2 className={styles.h2} id="cookies">
          7. Cookies y qué guarda tu navegador
        </h2>
        <p>
          <strong>No usamos analítica, ni publicidad, ni rastreo de terceros.</strong> Ni Google
          Analytics, ni píxeles, ni nada parecido. Por eso no verás un banner de cookies pidiéndote
          permiso: lo que guardamos es estrictamente necesario para que la aplicación funcione, y
          para eso el permiso no hace falta.
        </p>
        <ul className={styles.list}>
          <li>
            Una <strong>cookie de sesión</strong>, para mantenerte dentro mientras navegas.
          </li>
          <li>
            Una cookie <strong>anti-CSRF</strong>, que evita que otra web haga acciones en tu
            nombre.
          </li>
          <li>
            En tu navegador, tu <strong>preferencia de tema</strong> (claro u oscuro) y el avance
            del cuestionario inicial si lo dejas a medias. Eso no sale de tu equipo.
          </li>
        </ul>
      </section>

      <section className={styles.section} aria-labelledby="menores">
        <h2 className={styles.h2} id="menores">
          8. Menores de edad
        </h2>
        <p>
          El servicio no está dirigido a menores de 14 años, que es la edad a partir de la cual la
          ley española permite consentir el tratamiento de los propios datos. El generador no acepta
          una edad por debajo de 14. Si detectamos datos de alguien más joven, los borramos.
        </p>
      </section>

      <section className={styles.section} aria-labelledby="seguridad">
        <h2 className={styles.h2} id="seguridad">
          9. Cómo los protegemos
        </h2>
        <p>
          Las contraseñas se guardan cifradas y no se pueden recuperar en claro, ni por nosotros.
          Los permisos de acceso a servicios externos se guardan cifrados. Las comunicaciones van
          por HTTPS. El acceso a la base de datos está restringido a quien hace falta para operar el
          servicio.
        </p>
        <p className={styles.note}>
          Ninguna medida es infalible, y decir lo contrario sería mentir. Si alguna vez ocurriera
          una brecha que pueda afectarte, te lo diremos.
        </p>
      </section>

      <section className={styles.section} aria-labelledby="automatizado">
        <h2 className={styles.h2} id="automatizado">
          10. Decisiones automáticas
        </h2>
        <p>
          El requerimiento calórico y el plan se calculan automáticamente, con una fórmula estándar
          (Mifflin-St Jeor). Es un cálculo, no una decisión sobre ti con efectos jurídicos, y como
          decimos en el propio generador:{' '}
          <strong>
            el plan es orientativo y no sustituye la valoración de un profesional de la salud
          </strong>
          .
        </p>
      </section>

      <section className={styles.section} aria-labelledby="cambios">
        <h2 className={styles.h2} id="cambios">
          11. Si esto cambia
        </h2>
        <p>
          Si cambiamos algo importante —una finalidad nueva, un destinatario nuevo— actualizaremos
          esta página y su fecha. Cuando aceptaste, guardamos qué versión estaba en vigor, así que
          siempre se puede saber qué texto aceptaste.
        </p>
      </section>
    </>
  );
}
