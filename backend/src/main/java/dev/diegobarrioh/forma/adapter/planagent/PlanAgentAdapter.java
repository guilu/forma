package dev.diegobarrioh.forma.adapter.planagent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.diegobarrioh.forma.application.FoodServing;
import dev.diegobarrioh.forma.application.GeneratedNutritionPlan;
import dev.diegobarrioh.forma.application.PlanCatalogEntry;
import dev.diegobarrioh.forma.application.PlanDay;
import dev.diegobarrioh.forma.application.PlanGenerationCommand;
import dev.diegobarrioh.forma.application.PlanGenerationException;
import dev.diegobarrioh.forma.application.PlanGenerationGateway;
import dev.diegobarrioh.forma.application.PlanItem;
import dev.diegobarrioh.forma.application.PlanMeal;
import dev.diegobarrioh.forma.application.PlanTargets;
import dev.diegobarrioh.forma.application.UnmatchedItem;
import dev.diegobarrioh.forma.domain.MacroTargets;
import dev.diegobarrioh.forma.domain.MainGoal;
import dev.diegobarrioh.forma.domain.MealType;
import dev.diegobarrioh.forma.domain.NutritionDayType;
import dev.diegobarrioh.forma.domain.UnmatchedResolution;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * The only {@link PlanGenerationGateway} this slice registers (ADR-015 decision 10) — serializing
 * the outbound request and parsing the returned plan, per {@code
 * docs/FORMA_Contrato_Agente_Plan.md}, both live here, behind the provider-neutral port (ADR-004).
 * No agent-shaped type — no {@link JsonNode}, no raw string field name — crosses {@link
 * #generate}'s signature; only {@link PlanGenerationCommand} in, {@link GeneratedNutritionPlan}
 * out.
 *
 * <p>{@link #serializeCommand} and {@link #parseGeneratedPlan} are package-private and {@code
 * static}, mirroring {@code adapter.withings.WithingsMeasuresAdapter#parseMeasureGroups}: the
 * mapping is unit-testable against recorded fixtures, independent of the transport.
 *
 * <p><b>Unconfigured fails loudly at the point of use, not silently.</b> A blank {@code
 * forma.plan-agent.base-url} makes {@link #generate} throw {@link PlanGenerationException}
 * immediately, before the transport is ever touched — the same posture ADR-015's Consequences
 * section states for the dispatcher ("fail loudly at the point of use... rather than accepting a
 * request nothing will ever answer"), applied here at the adapter's own boundary. The application
 * itself still boots with every {@code forma.plan-agent.*} property empty (ADR-014's lesson).
 *
 * <p>Never logs the API key, the request body or the response body (ADR-008).
 */
@Component
public class PlanAgentAdapter implements PlanGenerationGateway {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final String baseUrl;
  private final String apiKey;
  private final PlanAgentTransport transport;

  public PlanAgentAdapter(
      @Value("${forma.plan-agent.base-url:}") String baseUrl,
      @Value("${forma.plan-agent.api-key:}") String apiKey,
      PlanAgentTransport transport) {
    this.baseUrl = baseUrl;
    this.apiKey = apiKey;
    this.transport = transport;
  }

  @Override
  public GeneratedNutritionPlan generate(PlanGenerationCommand command) {
    Objects.requireNonNull(command, "command must not be null");
    if (baseUrl == null || baseUrl.isBlank()) {
      throw new PlanGenerationException(
          "El agente de planes no está configurado (forma.plan-agent.base-url vacío).");
    }
    String requestBody = serializeCommand(command);
    String responseBody;
    try {
      responseBody = transport.post(baseUrl, apiKey, requestBody);
    } catch (RuntimeException ex) {
      throw new PlanGenerationException("No se pudo contactar con el agente de planes.", ex);
    }
    return parseGeneratedPlan(responseBody, command.planRequestId());
  }

  // ---------------------------------------------------------------------------------------------
  // Outbound: PlanGenerationCommand -> JSON, per "La petición" (docs/FORMA_Contrato_Agente_Plan.md)
  // ---------------------------------------------------------------------------------------------

  static String serializeCommand(PlanGenerationCommand command) {
    ObjectNode root = MAPPER.createObjectNode();
    root.put("contractVersion", command.contractVersion());
    root.put("planRequestId", command.planRequestId().toString());
    root.put("catalogVersion", command.catalogVersion());

    ObjectNode person = root.putObject("person");
    person.put("sex", command.person().sex().name());
    person.put("ageYears", command.person().ageYears());
    person.put("weightKg", command.person().weightKg());
    person.put("heightCm", command.person().heightCm());
    person.put("activityLevel", command.person().activityLevel().name());

    ObjectNode goal = root.putObject("goal");
    goal.put("mainGoal", command.goal().mainGoal().name());
    goal.put("planObjective", command.goal().planObjective().name());

    ObjectNode targets = root.putObject("targets");
    targets.put("planKcal", command.targets().planKcal());
    putNullableDouble(targets, "proteinG", command.targets().proteinG());
    putNullableDouble(targets, "carbsG", command.targets().carbsG());
    putNullableDouble(targets, "fatG", command.targets().fatG());

    ObjectNode training = root.putObject("training");
    training.put("daysPerWeek", command.training().daysPerWeek());
    ArrayNode weekdays = training.putArray("weekdays");
    command.training().weekdays().stream().sorted().forEach(day -> weekdays.add(day.name()));
    ArrayNode equipment = training.putArray("equipment");
    command.training().equipment().stream().map(Enum::name).sorted().forEach(equipment::add);

    ObjectNode preferences = root.putObject("preferences");
    preferences.put("mealsPerDay", command.preferences().mealsPerDay());
    preferences.put("dietPattern", command.preferences().dietPattern().name());
    preferences.put("cuisineStyle", command.preferences().cuisineStyle().name());

    ObjectNode plan = root.putObject("plan");
    plan.put("weeks", command.plan().weeks());
    if (command.plan().startDate() != null) {
      plan.put("startDate", command.plan().startDate().toString());
    } else {
      plan.putNull("startDate");
    }

    ArrayNode foods = root.putObject("catalog").putArray("foods");
    for (PlanCatalogEntry entry : command.catalog()) {
      foods.add(foodNode(entry));
    }

    try {
      return MAPPER.writeValueAsString(root);
    } catch (JsonProcessingException ex) {
      // ObjectNode built only from primitives/strings above cannot actually fail to serialize;
      // this exists only so writeValueAsString's checked exception does not force a `throws`
      // clause onto every caller of this method.
      throw new IllegalStateException("No se pudo serializar la petición al agente de planes", ex);
    }
  }

  private static ObjectNode foodNode(PlanCatalogEntry entry) {
    ObjectNode node = MAPPER.createObjectNode();
    node.put("id", entry.food().id());
    node.put("name", entry.food().name());
    ObjectNode per100g = node.putObject("per100g");
    per100g.put("kcal", entry.food().kcalPer100g());
    per100g.put("proteinG", entry.food().proteinPer100g());
    per100g.put("carbsG", entry.food().carbsPer100g());
    per100g.put("fatG", entry.food().fatPer100g());
    if (entry.preparation() != null) {
      node.put("preparation", entry.preparation());
    } else {
      node.putNull("preparation");
    }
    ArrayNode servings = node.putArray("servings");
    for (FoodServing serving : entry.servings()) {
      ObjectNode servingNode = MAPPER.createObjectNode();
      servingNode.put("id", serving.id());
      if (serving.name() != null) {
        servingNode.put("name", serving.name());
      } else {
        servingNode.putNull("name");
      }
      servingNode.put("grams", serving.grams().doubleValue());
      servingNode.put("isDefault", serving.isDefault());
      servings.add(servingNode);
    }
    return node;
  }

  private static void putNullableDouble(ObjectNode node, String field, Double value) {
    if (value != null) {
      node.put(field, value);
    } else {
      node.putNull(field);
    }
  }

  // ---------------------------------------------------------------------------------------------
  // Inbound: JSON -> GeneratedNutritionPlan, per "El plan" (docs/FORMA_Contrato_Agente_Plan.md)
  // ---------------------------------------------------------------------------------------------

  static GeneratedNutritionPlan parseGeneratedPlan(
      String responseBody, UUID expectedPlanRequestId) {
    JsonNode root;
    try {
      root = MAPPER.readTree(responseBody);
    } catch (JsonProcessingException ex) {
      throw new PlanGenerationException(
          "No se pudo interpretar la respuesta del agente de planes.");
    }
    if (root == null || !root.isObject()) {
      throw new PlanGenerationException(
          "La respuesta del agente de planes está vacía o no es un objeto JSON.");
    }
    if (root.has("error")) {
      throw new PlanGenerationException(
          "El agente de planes devolvió un error: " + root.path("error").asText("desconocido"));
    }

    String responsePlanRequestId = textOrNull(root.path("planRequestId"));
    if (expectedPlanRequestId != null
        && (responsePlanRequestId == null
            || !responsePlanRequestId.equals(expectedPlanRequestId.toString()))) {
      throw new PlanGenerationException(
          "La respuesta del agente de planes no corresponde a la petición enviada.");
    }

    JsonNode planNode = root.path("plan");
    if (!planNode.isObject()) {
      throw new PlanGenerationException("La respuesta del agente de planes no incluye un plan.");
    }

    try {
      String name = textOrNull(planNode.path("name"));
      String description = textOrNull(planNode.path("description"));
      MainGoal objective = enumOrNull(planNode.path("objective"), MainGoal.class);
      LocalDate startDate = dateOrNull(planNode.path("startDate"));
      LocalDate endDate = dateOrNull(planNode.path("endDate"));
      PlanTargets targets = parseTargets(planNode.path("targets"));

      List<PlanDay> days = new ArrayList<>();
      for (JsonNode dayNode : planNode.path("days")) {
        days.add(parseDay(dayNode));
      }

      List<UnmatchedItem> unmatched = new ArrayList<>();
      for (JsonNode unmatchedNode : root.path("unmatched")) {
        unmatched.add(parseUnmatched(unmatchedNode));
      }

      String agentModel = textOrNull(root.path("agent").path("model"));
      Instant agentGeneratedAt = instantOrNull(root.path("agent").path("generatedAt"));

      return new GeneratedNutritionPlan(
          textOrNull(root.path("contractVersion")),
          expectedPlanRequestId,
          textOrNull(root.path("catalogVersion")),
          name,
          description,
          objective,
          startDate,
          endDate,
          targets,
          days,
          unmatched,
          agentModel,
          agentGeneratedAt);
    } catch (RuntimeException ex) {
      throw new PlanGenerationException(
          "La respuesta del agente de planes no se pudo interpretar: estructura de plan inválida.",
          ex);
    }
  }

  private static PlanTargets parseTargets(JsonNode node) {
    if (!node.isObject()) {
      return PlanTargets.none();
    }
    return new PlanTargets(
        intOrNull(node.path("kcalMin")),
        intOrNull(node.path("kcalMax")),
        doubleOrNull(node.path("proteinG")),
        doubleOrNull(node.path("carbsG")),
        doubleOrNull(node.path("fatG")));
  }

  private static PlanDay parseDay(JsonNode node) {
    int weekNumber = node.path("weekNumber").asInt(1);
    int dayNumber = node.path("dayNumber").asInt();
    NutritionDayType dayType = enumOrNull(node.path("dayType"), NutritionDayType.class);
    MacroTargets targets = parseMacroTargets(node.path("targets"));
    String notes = textOrNull(node.path("notes"));
    List<PlanMeal> meals = new ArrayList<>();
    for (JsonNode mealNode : node.path("meals")) {
      meals.add(parseMeal(mealNode));
    }
    return new PlanDay(null, weekNumber, dayNumber, dayType, targets, notes, meals);
  }

  private static PlanMeal parseMeal(JsonNode node) {
    MealType mealType = enumOrNull(node.path("mealType"), MealType.class);
    String name = textOrNull(node.path("name"));
    LocalTime scheduledTime = timeOrNull(node.path("scheduledTime"));
    MacroTargets targets = parseMacroTargets(node.path("targets"));
    String instructions = textOrNull(node.path("instructions"));
    boolean optional = node.path("optional").asBoolean(false);
    List<PlanItem> items = new ArrayList<>();
    for (JsonNode itemNode : node.path("items")) {
      items.add(parseItem(itemNode));
    }
    return new PlanMeal(
        null, mealType, name, scheduledTime, targets, instructions, optional, items);
  }

  private static PlanItem parseItem(JsonNode node) {
    String foodId = textOrNull(node.path("foodId"));
    String recipeId = textOrNull(node.path("recipeId"));
    String servingId = textOrNull(node.path("servingId"));
    double amount = node.path("amount").asDouble();
    String preparationNotes = textOrNull(node.path("preparationNotes"));
    boolean optional = node.path("optional").asBoolean(false);
    return new PlanItem(null, foodId, recipeId, servingId, amount, preparationNotes, optional);
  }

  private static UnmatchedItem parseUnmatched(JsonNode node) {
    String wanted = textOrNull(node.path("wanted"));
    String where = textOrNull(node.path("where"));
    UnmatchedResolution resolution = enumOrNull(node.path("resolution"), UnmatchedResolution.class);
    String usedInstead = textOrNull(node.path("usedInstead"));
    String note = textOrNull(node.path("note"));
    return new UnmatchedItem(wanted, where, resolution, usedInstead, note);
  }

  private static MacroTargets parseMacroTargets(JsonNode node) {
    if (!node.isObject()) {
      return MacroTargets.none();
    }
    return new MacroTargets(
        intOrNull(node.path("calories")),
        doubleOrNull(node.path("proteinG")),
        doubleOrNull(node.path("carbsG")),
        doubleOrNull(node.path("fatG")));
  }

  private static <E extends Enum<E>> E enumOrNull(JsonNode node, Class<E> type) {
    return node.isTextual() ? Enum.valueOf(type, node.asText()) : null;
  }

  private static String textOrNull(JsonNode node) {
    return node.isTextual() ? node.asText() : null;
  }

  private static Integer intOrNull(JsonNode node) {
    return node.isNumber() ? node.asInt() : null;
  }

  private static Double doubleOrNull(JsonNode node) {
    return node.isNumber() ? node.asDouble() : null;
  }

  private static LocalDate dateOrNull(JsonNode node) {
    return node.isTextual() ? LocalDate.parse(node.asText()) : null;
  }

  private static LocalTime timeOrNull(JsonNode node) {
    return node.isTextual() ? LocalTime.parse(node.asText()) : null;
  }

  private static Instant instantOrNull(JsonNode node) {
    return node.isTextual() ? Instant.parse(node.asText()) : null;
  }
}
