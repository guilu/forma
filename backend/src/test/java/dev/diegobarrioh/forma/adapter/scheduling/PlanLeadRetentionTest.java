package dev.diegobarrioh.forma.adapter.scheduling;

import static org.assertj.core.api.Assertions.assertThat;

import dev.diegobarrioh.forma.application.PlanLead;
import dev.diegobarrioh.forma.application.PlanLeadRepository;
import dev.diegobarrioh.forma.domain.ActivityLevel;
import dev.diegobarrioh.forma.domain.PlanObjective;
import dev.diegobarrioh.forma.domain.Sex;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * El plazo de conservación, comprobado.
 *
 * <p>El aviso de privacidad declara doce meses, y el artículo 13.2(a) convierte eso en una promesa.
 * Lo único que la hace verdad es que algo borre — sin esto, el plazo declarado sería exactamente el
 * tipo de frase que este repositorio se pasa el día quitando de la interfaz.
 *
 * <p>Se comprueba el borde en los dos sentidos: lo que ya pasó de doce meses se va, y lo que aún no
 * se queda. Un test que solo prueba el primero pasa igual con un `DELETE FROM plan_lead`.
 */
@SpringBootTest
@ActiveProfiles("test")
class PlanLeadRetentionTest {

  @Autowired private PlanLeadRepository repository;
  @Autowired private JdbcTemplate jdbcTemplate;

  private static PlanLead leadFor(String email) {
    return new PlanLead(
        UUID.randomUUID(),
        "Retención Prueba",
        email,
        "ES",
        null,
        Sex.MALE,
        45,
        75,
        182,
        ActivityLevel.MODERATE,
        PlanObjective.WEIGHT_LOSS,
        5,
        5,
        "ESTANDAR_ESPANOL",
        2068,
        "2026-08-22",
        false);
  }

  private int countFor(String email) {
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM plan_lead WHERE email = ?", Integer.class, email);
    return count == null ? 0 : count;
  }

  @Test
  void borraLoQueYaPasoElPlazoYConservaLoQueNo() {
    Instant now = Instant.parse("2026-08-22T00:00:00Z");
    Instant cutoff = now.atZone(ZoneOffset.UTC).minusMonths(12).toInstant();

    String viejo = "viejo@retencion.test";
    String reciente = "reciente@retencion.test";
    jdbcTemplate.update("DELETE FROM plan_lead WHERE email IN (?, ?)", viejo, reciente);

    // Un día por fuera del plazo, y un día por dentro.
    repository.save(leadFor(viejo), cutoff.minusSeconds(86_400));
    repository.save(leadFor(reciente), cutoff.plusSeconds(86_400));

    int deleted = repository.deleteOlderThan(cutoff);

    assertThat(deleted).isEqualTo(1);
    assertThat(countFor(viejo)).isZero();
    assertThat(countFor(reciente)).isEqualTo(1);

    jdbcTemplate.update("DELETE FROM plan_lead WHERE email = ?", reciente);
  }

  /**
   * Volver a pasar el borrado no rompe nada.
   *
   * <p>Es lo que permite que el trabajo programado no lleve elección de líder: si dos instancias lo
   * lanzan a la vez, la segunda encuentra menos que hacer y ya está. Sin esta propiedad haría falta
   * un cerrojo distribuido para una consulta que borra filas caducadas.
   */
  @Test
  void repetirElBorradoEsInofensivo() {
    Instant cutoff = Instant.parse("2026-08-22T00:00:00Z");
    String email = "idempotente@retencion.test";
    jdbcTemplate.update("DELETE FROM plan_lead WHERE email = ?", email);

    repository.save(leadFor(email), cutoff.minusSeconds(86_400));

    assertThat(repository.deleteOlderThan(cutoff)).isEqualTo(1);
    assertThat(repository.deleteOlderThan(cutoff)).isZero();
    assertThat(countFor(email)).isZero();
  }
}
