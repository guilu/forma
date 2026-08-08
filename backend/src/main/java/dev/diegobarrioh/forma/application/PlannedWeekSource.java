package dev.diegobarrioh.forma.application;

import java.util.List;
import java.util.UUID;

/**
 * La semana del plan que alguien sigue, para quien la necesita entera.
 *
 * <p>Un puerto y no la clase que lo cumple, como {@link PlannedDaySource}: la lista de la compra
 * necesita saber qué se come en los siete días, no cómo se resuelve un plan contra el catálogo. Con
 * la clase concreta, probar la lista obligaba a construir un lector de planes con sus cinco
 * dependencias detrás.
 */
public interface PlannedWeekSource {

  /**
   * Los días de la semana natural en curso del plan activo de esa cuenta, resueltos. Vacío cuando
   * no sigue ninguno o la fecha actual cae fuera de las semanas representadas por el plan.
   */
  List<ResolvedDay> activePlanDays(UUID userId);
}
