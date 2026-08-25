import type { BodyMeasurement } from '../../api/bodyMeasurements';

/**
 * El histórico de mediciones, tal y como lo reciben los widgets que lo pintan.
 *
 * <p>Existe porque tres sitios del panel dibujan la MISMA lista y cada uno se la pedía al
 * servidor por su cuenta: `BodyWidget` a través de la página, `TrendWidget` y
 * `EvolutionWidget` desde sus propios efectos. Tres peticiones idénticas —
 * `listBodyMeasurements()` sin argumentos, las tres— en cada carga del panel.
 *
 * <p>No era solo desperdicio. El panel dispara una decena de llamadas a la vez al abrirse y
 * hay un limitador delante en producción, así que las últimas en llegar se llevaban un 429:
 * dos peticiones sobrantes convertían una ráfaga que cabía en una que no. Y siendo tres
 * lecturas independientes del mismo dato, podían además contestar cosas distintas si alguien
 * guardaba una medición entre medias, y el panel enseñaría dos historiales a la vez.
 *
 * <p>La página lo pide una vez y lo reparte, que es lo que ya hacía `BodyWidget` y lo que su
 * propio comentario justificaba: «una selección no se puede compartir entre componentes que
 * cada uno busca su copia».
 */
export type MeasurementsState =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | { readonly status: 'ready'; readonly history: BodyMeasurement[] };
