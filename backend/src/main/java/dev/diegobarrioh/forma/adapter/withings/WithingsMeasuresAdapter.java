package dev.diegobarrioh.forma.adapter.withings;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.diegobarrioh.forma.application.ImportedMeasureGroup;
import dev.diegobarrioh.forma.application.ProviderMeasuresGateway;
import dev.diegobarrioh.forma.application.ProviderSyncException;
import dev.diegobarrioh.forma.domain.BodyMeasurement;
import dev.diegobarrioh.forma.domain.IntegrationProvider;
import dev.diegobarrioh.forma.domain.MeasurementSource;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * The only {@link ProviderMeasuresGateway} this story registers (FOR-132) — the Withings Measure
 * API (<em>Getmeas</em>) call and the payload→{@link BodyMeasurement} mapping both live here,
 * behind the provider-neutral port (ADR-004: "Provider payloads must be mapped at the adapter
 * boundary"). No Withings request/response shape crosses {@link #fetchMeasureGroups}'s return type
 * — only {@link ImportedMeasureGroup}, pairing the mapped measurement with Withings' {@code grpid}
 * for the caller's dedup bookkeeping.
 *
 * <p>Measure-type mapping (spec FOR-132 Functional Requirements): weight ({@code type=1}) →
 * weightKg; fat ratio ({@code type=6}) → bodyFatPercentage; muscle mass ({@code type=76}) →
 * muscleMassKg; hydration ({@code type=77}, reported by Withings as a mass) → waterPercentage,
 * computed as {@code hydrationKg / weightKg * 100} when both are present in the same group, else
 * {@code null} (documented Open Question resolution — see class-level note below). {@code bmi} is
 * always {@code null}: Getmeas does not report it. Unmodeled types (e.g. bone mass, {@code
 * type=88}) are ignored without failing the group.
 *
 * <p><b>Groups without a weight measure are skipped entirely</b> — {@link BodyMeasurement}'s
 * constructor requires {@code weightKg > 0}, so a group that never reports weight cannot become a
 * valid measurement and this adapter never fabricates a placeholder value (spec FOR-132: "never
 * fabricate"). This is the conservative resolution to the spec's "hydration mass but no weight in
 * the group → waterPercentage null" edge case: without a weight, there is no measurement to attach
 * a null {@code waterPercentage} to in the first place.
 *
 * <p><b>{@code measuredAt} / timezone</b> (documented Open Question resolution): Withings' {@code
 * date} field is a Unix epoch timestamp — inherently UTC, unambiguous — so {@link
 * Instant#ofEpochSecond} is used directly with no additional timezone arithmetic; the {@code
 * timezone} field elsewhere in the response describes the user's locale, not the timestamp's
 * offset, and is not needed here.
 *
 * <p><b>Incremental sync / paging</b> (documented Open Question resolution): {@code since} maps to
 * Withings' {@code lastupdate} parameter for incremental syncs; when {@code null} (first sync), the
 * call fetches full history in a single request — this story does not implement Getmeas's {@code
 * more}/{@code offset} paging, a known limitation for accounts with an unusually large measurement
 * history, acceptable for the MVP's expected per-user data volume.
 *
 * <p>Never logs the access token, the request form, or the raw response body (ADR-008) — mirroring
 * {@link WithingsOAuthAdapter}.
 */
@Component
public class WithingsMeasuresAdapter implements ProviderMeasuresGateway {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  /** Withings Measure API type codes this codebase models (spec FOR-132). */
  private static final int TYPE_WEIGHT = 1;

  private static final int TYPE_FAT_RATIO = 6;
  private static final int TYPE_MUSCLE_MASS = 76;
  private static final int TYPE_HYDRATION = 77;

  /**
   * {@code category=1} selects real measures; {@code category=2} is Withings' user-objective/goal
   * entries, never a real measurement (spec FOR-132 makes no mention of importing goals).
   */
  private static final int CATEGORY_REAL_MEASURE = 1;

  private final String measureUrl;
  private final WithingsHttpTransport transport;

  public WithingsMeasuresAdapter(
      @Value("${forma.integrations.withings.measure-url:https://wbsapi.withings.net/measure}")
          String measureUrl,
      WithingsHttpTransport transport) {
    this.measureUrl = measureUrl;
    this.transport = transport;
  }

  @Override
  public IntegrationProvider provider() {
    return IntegrationProvider.WITHINGS;
  }

  @Override
  public List<ImportedMeasureGroup> fetchMeasureGroups(String accessToken, Instant since) {
    Map<String, String> form = new LinkedHashMap<>();
    form.put("action", "getmeas");
    form.put(
        "meastypes",
        TYPE_WEIGHT + "," + TYPE_FAT_RATIO + "," + TYPE_MUSCLE_MASS + "," + TYPE_HYDRATION);
    form.put("category", Integer.toString(CATEGORY_REAL_MEASURE));
    if (since != null) {
      form.put("lastupdate", Long.toString(since.getEpochSecond()));
    }

    String responseBody;
    try {
      responseBody = transport.postAuthenticated(measureUrl, form, accessToken);
    } catch (RuntimeException ex) {
      throw new ProviderSyncException(
          "No se pudo contactar a Withings para obtener las medidas.", ex);
    }
    return parseMeasureGroups(responseBody);
  }

  /**
   * Package-private and static so the parsing/mapping logic (spec FOR-132 tests.md Mapping Tests)
   * is directly unit-testable against recorded fixtures, independent of the transport.
   */
  static List<ImportedMeasureGroup> parseMeasureGroups(String responseBody) {
    JsonNode root;
    try {
      root = MAPPER.readTree(responseBody);
    } catch (JsonProcessingException ex) {
      throw new ProviderSyncException("No se pudo interpretar la respuesta de Withings.");
    }

    int status = root.path("status").asInt(-1);
    if (status != 0) {
      throw new ProviderSyncException(
          "Withings devolvió un error al obtener las medidas (status=" + status + ").");
    }

    List<ImportedMeasureGroup> groups = new ArrayList<>();
    for (JsonNode groupNode : root.path("body").path("measuregrps")) {
      if (groupNode.path("category").asInt(CATEGORY_REAL_MEASURE) != CATEGORY_REAL_MEASURE) {
        continue;
      }
      ImportedMeasureGroup group = mapGroup(groupNode);
      if (group != null) {
        groups.add(group);
      }
    }
    return groups;
  }

  private static ImportedMeasureGroup mapGroup(JsonNode groupNode) {
    long grpid = groupNode.path("grpid").asLong();
    long dateEpochSeconds = groupNode.path("date").asLong();

    Double weightKg = null;
    Double fatPercent = null;
    Double muscleKg = null;
    Double hydrationKg = null;

    for (JsonNode measureNode : groupNode.path("measures")) {
      int type = measureNode.path("type").asInt();
      double scaledValue =
          measureNode.path("value").asDouble() * Math.pow(10, measureNode.path("unit").asInt());
      switch (type) {
        case TYPE_WEIGHT -> weightKg = scaledValue;
        case TYPE_FAT_RATIO -> fatPercent = scaledValue;
        case TYPE_MUSCLE_MASS -> muscleKg = scaledValue;
        case TYPE_HYDRATION -> hydrationKg = scaledValue;
        default -> {
          // Unmodeled Withings measure type (e.g. bone mass, 88) — ignored, never crashes the
          // group.
        }
      }
    }

    if (weightKg == null) {
      // No weight measure in this group: cannot construct a valid BodyMeasurement (weightKg must be
      // strictly positive) — skip rather than fabricate a placeholder (spec FOR-132 Edge Cases).
      return null;
    }

    Double waterPercentage = null;
    if (hydrationKg != null) {
      double computed = hydrationKg / weightKg * 100;
      // Defensive bound check (not explicitly required by spec but consistent with "never
      // fabricate"): a computed percentage outside BodyMeasurement's valid [0, 100] range would
      // otherwise throw and drop the whole group, including its valid weight — treat as
      // undeterminable instead of crashing the sync over one bad measure.
      waterPercentage = (computed >= 0 && computed <= 100) ? computed : null;
    }

    try {
      BodyMeasurement measurement =
          new BodyMeasurement(
              Instant.ofEpochSecond(dateEpochSeconds),
              MeasurementSource.WITHINGS,
              weightKg,
              fatPercent,
              null,
              muscleKg,
              waterPercentage,
              null);
      return new ImportedMeasureGroup(grpid, measurement);
    } catch (IllegalArgumentException ex) {
      // A malformed upstream value (e.g. fat% out of [0, 100]) must not crash the whole sync —
      // skip this one group, never fabricate, never propagate (spec FOR-132: "no crash").
      return null;
    }
  }
}
