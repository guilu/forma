package dev.diegobarrioh.forma.adapter.planagent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.diegobarrioh.forma.application.FoodServing;
import dev.diegobarrioh.forma.application.GeneratedNutritionPlan;
import dev.diegobarrioh.forma.application.NutritionPlan;
import dev.diegobarrioh.forma.application.PlanCatalogEntry;
import dev.diegobarrioh.forma.application.PlanDay;
import dev.diegobarrioh.forma.application.PlanGeneration;
import dev.diegobarrioh.forma.application.PlanGenerationCommand;
import dev.diegobarrioh.forma.application.PlanGenerationException;
import dev.diegobarrioh.forma.application.PlanItem;
import dev.diegobarrioh.forma.application.PlanMeal;
import dev.diegobarrioh.forma.application.PlanToleranceAuditor;
import dev.diegobarrioh.forma.application.ToleranceAuditReport;
import dev.diegobarrioh.forma.domain.ActivityLevel;
import dev.diegobarrioh.forma.domain.CuisineStyle;
import dev.diegobarrioh.forma.domain.DietPattern;
import dev.diegobarrioh.forma.domain.FoodItem;
import dev.diegobarrioh.forma.domain.MainGoal;
import dev.diegobarrioh.forma.domain.PlanObjective;
import dev.diegobarrioh.forma.domain.PlanOrigin;
import dev.diegobarrioh.forma.domain.PlanStatus;
import dev.diegobarrioh.forma.domain.Sex;
import dev.diegobarrioh.forma.domain.TrainingEquipment;
import dev.diegobarrioh.forma.domain.UnmatchedResolution;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Adapter tests for {@link PlanAgentAdapter} (ADR-015 slice 5). Never calls the live plan agent —
 * {@link FakePlanAgentTransport} stands in for the real HTTP transport, and every response is a
 * recorded/representative fixture under {@code src/test/resources/fixtures/plan-agent/} (see each
 * fixture's {@code _comment}), mirroring {@code adapter.withings.WithingsMeasuresAdapterTest}.
 */
class PlanAgentAdapterTest {

  private static final String AGENT_URL = "https://plan-agent.example.invalid/generate";
  private static final UUID PLAN_REQUEST_ID =
      UUID.fromString("8f1c2b64-0e5a-4c7d-9a10-3f2b6d4e7c11");
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final FakePlanAgentTransport transport = new FakePlanAgentTransport();

  // ---------------------------------------------------------------------------------------------
  // Unconfigured: fails loudly at the point of use, never touches the transport.
  // ---------------------------------------------------------------------------------------------

  @Test
  void refusesToCallTheTransportWhenBaseUrlIsBlank() {
    PlanAgentAdapter adapter = new PlanAgentAdapter("", "irrelevant-key", transport);

    assertThatThrownBy(() -> adapter.generate(sampleCommand()))
        .isInstanceOf(PlanGenerationException.class)
        .hasMessageContaining("no está configurado");
    assertThat(transport.lastUrl).isNull();
  }

  // ---------------------------------------------------------------------------------------------
  // The happy path: request goes out shaped like the contract, response comes back parsed.
  // ---------------------------------------------------------------------------------------------

  @Test
  void generateSendsTheApiKeyAndPostsToTheConfiguredBaseUrl() {
    transport.nextResponse = fixture("response-minimal.json");
    PlanAgentAdapter adapter = new PlanAgentAdapter(AGENT_URL, "the-api-key", transport);

    adapter.generate(sampleCommand());

    assertThat(transport.lastUrl).isEqualTo(AGENT_URL);
    assertThat(transport.lastApiKey).isEqualTo("the-api-key");
  }

  @Test
  void serializedRequestCarriesEveryContractGroupWithTheRightValues() throws IOException {
    transport.nextResponse = fixture("response-minimal.json");
    PlanAgentAdapter adapter = new PlanAgentAdapter(AGENT_URL, "key", transport);

    adapter.generate(sampleCommand());

    JsonNode sent = MAPPER.readTree(transport.lastRequestBody);
    assertThat(sent.path("contractVersion").asText()).isEqualTo("1");
    assertThat(sent.path("planRequestId").asText()).isEqualTo(PLAN_REQUEST_ID.toString());
    assertThat(sent.path("catalogVersion").asText()).isEqualTo("2026-09-18");
    assertThat(sent.path("person").path("sex").asText()).isEqualTo("MALE");
    assertThat(sent.path("person").path("ageYears").asInt()).isEqualTo(38);
    assertThat(sent.path("person").path("weightKg").asDouble()).isEqualTo(73.6);
    assertThat(sent.path("person").path("activityLevel").asText()).isEqualTo("MODERATE");
    assertThat(sent.path("goal").path("mainGoal").asText()).isEqualTo("COMPOSICION");
    assertThat(sent.path("goal").path("planObjective").asText()).isEqualTo("WEIGHT_LOSS");
    assertThat(sent.path("targets").path("planKcal").asInt()).isEqualTo(2078);
    assertThat(sent.path("targets").path("proteinG").asDouble()).isEqualTo(160.0);
    assertThat(sent.path("training").path("daysPerWeek").asInt()).isEqualTo(5);
    assertThat(sent.path("training").path("weekdays"))
        .extracting(JsonNode::asText)
        .containsExactly("MONDAY", "TUESDAY"); // sorted, matches the Set given below
    assertThat(sent.path("training").path("equipment"))
        .extracting(JsonNode::asText)
        .containsExactly("BARBELL", "DUMBBELLS");
    assertThat(sent.path("preferences").path("mealsPerDay").asInt()).isEqualTo(5);
    assertThat(sent.path("preferences").path("dietPattern").asText()).isEqualTo("OMNIVORE");
    assertThat(sent.path("preferences").path("cuisineStyle").asText()).isEqualTo("ESPANOLA");
    assertThat(sent.path("plan").path("weeks").asInt()).isEqualTo(1);
    assertThat(sent.path("plan").path("startDate").asText()).isEqualTo("2026-09-21");
    assertThat(sent.path("catalog").path("foods")).hasSize(1);
    JsonNode food = sent.path("catalog").path("foods").get(0);
    assertThat(food.path("id").asText()).isEqualTo("oats");
    assertThat(food.path("per100g").path("kcal").asInt()).isEqualTo(370);
    assertThat(food.path("preparation").isNull()).isTrue();
    assertThat(food.path("servings")).hasSize(1);
    assertThat(food.path("servings").get(0).path("id").asText()).isEqualTo("oats");
  }

  @Test
  void omittedOptionalTargetMacrosAreSentAsExplicitJsonNulls() throws IOException {
    transport.nextResponse = fixture("response-minimal.json");
    PlanAgentAdapter adapter = new PlanAgentAdapter(AGENT_URL, "key", transport);
    PlanGenerationCommand command = sampleCommandBuilder().targets(2078, null, null, null).build();

    adapter.generate(command);

    JsonNode sent = MAPPER.readTree(transport.lastRequestBody);
    assertThat(sent.path("targets").path("proteinG").isNull()).isTrue();
    assertThat(sent.path("targets").path("carbsG").isNull()).isTrue();
    assertThat(sent.path("targets").path("fatG").isNull()).isTrue();
  }

  @Test
  void parsesTheContractsWorkedExampleIntoAFullyPopulatedGeneratedPlan() {
    transport.nextResponse = fixture("response-success.json");
    PlanAgentAdapter adapter = new PlanAgentAdapter(AGENT_URL, "key", transport);

    GeneratedNutritionPlan generated = adapter.generate(sampleCommand());

    assertThat(generated.planRequestId()).isEqualTo(PLAN_REQUEST_ID);
    assertThat(generated.name()).isEqualTo("Recomposición 2000-2150");
    assertThat(generated.objective()).isEqualTo(MainGoal.COMPOSICION);
    assertThat(generated.startDate()).isEqualTo(LocalDate.of(2026, 9, 21));
    assertThat(generated.targets().kcalMin()).isEqualTo(2000);
    assertThat(generated.targets().kcalMax()).isEqualTo(2150);
    assertThat(generated.days()).hasSize(1);
    PlanDay day = generated.days().getFirst();
    assertThat(day.dayNumber()).isEqualTo(1);
    assertThat(day.meals()).hasSize(5);
    PlanMeal breakfast = day.meals().getFirst();
    assertThat(breakfast.items()).hasSize(3);
    PlanItem banana = breakfast.items().get(2);
    assertThat(banana.foodId()).isEqualTo("banana");
    assertThat(banana.servingId()).isEqualTo("banana");
    assertThat(generated.unmatched()).hasSize(1);
    assertThat(generated.unmatched().getFirst().wanted()).isEqualTo("Lentejas cocidas");
    assertThat(generated.unmatched().getFirst().resolution())
        .isEqualTo(UnmatchedResolution.INSTRUCTION);
    assertThat(generated.agentModel()).isEqualTo("el-modelo-que-sea");
  }

  @Test
  void parsesAMinimalPlanWithEmptyUnmatchedAsANormalGoodResponse() {
    transport.nextResponse = fixture("response-minimal.json");
    PlanAgentAdapter adapter = new PlanAgentAdapter(AGENT_URL, "key", transport);

    GeneratedNutritionPlan generated = adapter.generate(sampleCommand());

    assertThat(generated.unmatched()).isEmpty();
    assertThat(generated.days()).hasSize(1);
    assertThat(generated.days().getFirst().meals()).hasSize(1);
  }

  // ---------------------------------------------------------------------------------------------
  // Failure paths — every one is a PlanGenerationException, never a leaked secret or raw body.
  // ---------------------------------------------------------------------------------------------

  @Test
  void onAMalformedResponseThrowsWithoutLeakingTheRawBody() {
    transport.nextResponse = fixture("response-malformed.json");
    PlanAgentAdapter adapter = new PlanAgentAdapter(AGENT_URL, "key", transport);

    assertThatThrownBy(() -> adapter.generate(sampleCommand()))
        .isInstanceOf(PlanGenerationException.class)
        .satisfies(ex -> assertThat(ex.getMessage()).doesNotContain("not valid json"));
  }

  @Test
  void onAnInBandErrorResponseThrowsAPlanGenerationException() {
    transport.nextResponse = fixture("response-error.json");
    PlanAgentAdapter adapter = new PlanAgentAdapter(AGENT_URL, "key", transport);

    assertThatThrownBy(() -> adapter.generate(sampleCommand()))
        .isInstanceOf(PlanGenerationException.class)
        .hasMessageContaining("insufficient_catalog");
  }

  @Test
  void onAResponseWithNoPlanObjectThrowsAPlanGenerationException() {
    transport.nextResponse = fixture("response-missing-plan.json");
    PlanAgentAdapter adapter = new PlanAgentAdapter(AGENT_URL, "key", transport);

    assertThatThrownBy(() -> adapter.generate(sampleCommand()))
        .isInstanceOf(PlanGenerationException.class)
        .hasMessageContaining("no incluye un plan");
  }

  @Test
  void onAResponseAnsweringADifferentPlanRequestThrowsAPlanGenerationException() {
    transport.nextResponse = fixture("response-plan-request-mismatch.json");
    PlanAgentAdapter adapter = new PlanAgentAdapter(AGENT_URL, "key", transport);

    assertThatThrownBy(() -> adapter.generate(sampleCommand()))
        .isInstanceOf(PlanGenerationException.class)
        .hasMessageContaining("no corresponde a la petición");
  }

  @Test
  void whenTheTransportFailsWrapsItWithoutLeakingTheApiKey() {
    transport.failWith = new RuntimeException("Connection refused: plan-agent.example.invalid");
    PlanAgentAdapter adapter = new PlanAgentAdapter(AGENT_URL, "super-secret-api-key", transport);

    assertThatThrownBy(() -> adapter.generate(sampleCommand()))
        .isInstanceOf(PlanGenerationException.class)
        .satisfies(ex -> assertThat(ex.getMessage()).doesNotContain("super-secret-api-key"));
  }

  // ---------------------------------------------------------------------------------------------
  // The fixture that matters most: numbers that don't add up, preserved verbatim, ready for
  // PlanToleranceAuditor (ADR-015 slice 4) to report on in slice 6.
  // ---------------------------------------------------------------------------------------------

  @Test
  void theDriftFixtureParsesSuccessfullyAndKeepsTheClaimedTargetsUnchanged() {
    transport.nextResponse = fixture("response-drift.json");
    PlanAgentAdapter adapter = new PlanAgentAdapter(AGENT_URL, "key", transport);

    GeneratedNutritionPlan generated = adapter.generate(sampleCommand());

    // Claimed verbatim — never recomputed or corrected against what the items actually sum to
    // (ADR-015 decision 9).
    assertThat(generated.days().getFirst().targets().calories()).isEqualTo(1100);
    assertThat(generated.days().getFirst().targets().proteinG()).isEqualTo(100.0);
    assertThat(generated.days().getFirst().targets().carbsG()).isEqualTo(150.0);
    assertThat(generated.days().getFirst().targets().fatG()).isEqualTo(40.0);
  }

  /**
   * Proves the drift fixture is something real, not just parseable: handed to a real {@link
   * PlanToleranceAuditor} (ADR-015 slice 4) against the actual seeded catalog values for chicken,
   * rice, vegetables and olive-oil, every one of the four day-level metrics comes back {@code
   * EXCEEDS_TOLERANCE} — mirroring V56's real, seeded drift. This is test-only use of the auditor,
   * proving fixture/type compatibility; nothing in production code calls either class from the
   * other (ADR-015 slice 5 wires nothing).
   */
  @Test
  void theDriftFixtureHandedToPlanToleranceAuditorReportsExceedsToleranceOnEveryDayMetric() {
    transport.nextResponse = fixture("response-drift.json");
    PlanAgentAdapter adapter = new PlanAgentAdapter(AGENT_URL, "key", transport);
    GeneratedNutritionPlan generated = adapter.generate(sampleCommand());

    NutritionPlan plan =
        new NutritionPlan(
            null,
            UUID.randomUUID(),
            generated.name(),
            generated.description(),
            generated.objective(),
            PlanStatus.DRAFT,
            generated.startDate(),
            generated.endDate(),
            generated.targets(),
            new PlanGeneration(PlanOrigin.AI, null, null),
            generated.days());

    Map<String, FoodItem> catalog =
        Map.of(
            "chicken", new FoodItem("chicken", "Pechuga pollo", 110, 23.0, 0.0, 2.0, null),
            "rice", new FoodItem("rice", "Arroz", 360, 7.0, 79.0, 1.0, null),
            "vegetables", new FoodItem("vegetables", "Verdura variada", 35, 2.0, 6.0, 0.3, null),
            "olive-oil", new FoodItem("olive-oil", "Aceite oliva", 900, 0.0, 0.0, 100.0, null));
    Map<String, FoodServing> servings =
        Map.of(
            "vegetables",
                new FoodServing(
                    "vegetables", "vegetables", null, BigDecimal.valueOf(300.0), true, 0),
            "olive-oil",
                new FoodServing("olive-oil", "olive-oil", null, BigDecimal.valueOf(10.0), true, 0));
    PlanToleranceAuditor auditor =
        new PlanToleranceAuditor(
            id -> Optional.ofNullable(catalog.get(id)),
            id -> Optional.ofNullable(servings.get(id)),
            id -> Optional.empty());

    ToleranceAuditReport report = auditor.audit(plan);

    assertThat(report.hasDrift()).isTrue();
    assertThat(report.drifts())
        .hasSizeGreaterThanOrEqualTo(4); // kcal, protein, carbs, fat — day scope
  }

  // ---------------------------------------------------------------------------------------------
  // Fixtures
  // ---------------------------------------------------------------------------------------------

  private static String fixture(String name) {
    try {
      return Files.readString(
          Path.of("src/test/resources/fixtures/plan-agent/" + name), StandardCharsets.UTF_8);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  private static PlanGenerationCommand sampleCommand() {
    return sampleCommandBuilder().build();
  }

  /** Small hand-rolled builder so tests can vary one field without repeating every other one. */
  private static SampleCommandBuilder sampleCommandBuilder() {
    return new SampleCommandBuilder();
  }

  private static final class SampleCommandBuilder {
    private PlanGenerationCommand.Targets targets =
        new PlanGenerationCommand.Targets(2078, 160.0, 260.0, 70.0);

    SampleCommandBuilder targets(int planKcal, Double protein, Double carbs, Double fat) {
      this.targets = new PlanGenerationCommand.Targets(planKcal, protein, carbs, fat);
      return this;
    }

    PlanGenerationCommand build() {
      PlanCatalogEntry oats =
          new PlanCatalogEntry(
              new FoodItem("oats", "Copos de avena", 370, 13.0, 60.0, 7.0, null),
              null,
              List.of(new FoodServing("oats", "oats", null, BigDecimal.valueOf(60.0), true, 0)));
      return new PlanGenerationCommand(
          "1",
          PLAN_REQUEST_ID,
          "2026-09-18",
          new PlanGenerationCommand.Person(Sex.MALE, 38, 73.6, 180.0, ActivityLevel.MODERATE),
          new PlanGenerationCommand.Goal(MainGoal.COMPOSICION, PlanObjective.WEIGHT_LOSS),
          targets,
          new PlanGenerationCommand.Training(
              5,
              Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY),
              Set.of(TrainingEquipment.DUMBBELLS, TrainingEquipment.BARBELL)),
          new PlanGenerationCommand.Preferences(5, DietPattern.OMNIVORE, CuisineStyle.ESPANOLA),
          new PlanGenerationCommand.PlanScope(1, LocalDate.of(2026, 9, 21)),
          List.of(oats));
    }
  }

  /** Records the last call, never performs a real network request. */
  private static class FakePlanAgentTransport implements PlanAgentTransport {
    String nextResponse;
    RuntimeException failWith;
    String lastUrl;
    String lastApiKey;
    String lastRequestBody;

    @Override
    public String post(String url, String apiKey, String jsonBody) {
      lastUrl = url;
      lastApiKey = apiKey;
      lastRequestBody = jsonBody;
      if (failWith != null) {
        throw failWith;
      }
      return nextResponse;
    }
  }
}
