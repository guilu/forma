package dev.diegobarrioh.forma.adapter.withings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import dev.diegobarrioh.forma.application.ImportedMeasureGroup;
import dev.diegobarrioh.forma.application.ProviderSyncException;
import dev.diegobarrioh.forma.domain.BodyMeasurement;
import dev.diegobarrioh.forma.domain.IntegrationProvider;
import dev.diegobarrioh.forma.domain.MeasurementSource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Adapter tests for {@link WithingsMeasuresAdapter} (FOR-132 tests.md Mapping Tests). Never calls
 * the live Withings API — {@link FakeWithingsHttpTransport} stands in for the real HTTP transport,
 * and every Getmeas response is a recorded/representative fixture under {@code
 * src/test/resources/fixtures/withings/} (see each fixture's {@code _comment}).
 */
class WithingsMeasuresAdapterTest {

  private static final String MEASURE_URL = "https://wbsapi.withings.net/measure";

  private final FakeWithingsHttpTransport transport = new FakeWithingsHttpTransport();
  private final WithingsMeasuresAdapter adapter =
      new WithingsMeasuresAdapter(MEASURE_URL, transport);

  @Test
  void providerIsWithings() {
    assertThat(adapter.provider()).isEqualTo(IntegrationProvider.WITHINGS);
  }

  @Test
  void fetchMeasureGroupsUsesBearerAuthenticationAgainstTheConfiguredMeasureUrl() {
    transport.nextResponse = fixture("getmeas-empty.json");

    adapter.fetchMeasureGroups("the-access-token", null);

    assertThat(transport.lastUrl).isEqualTo(MEASURE_URL);
    assertThat(transport.lastAccessToken).isEqualTo("the-access-token");
    assertThat(transport.lastForm.get("action")).isEqualTo("getmeas");
  }

  @Test
  void fetchMeasureGroupsIncludesLastupdateOnlyWhenSinceIsProvided() {
    transport.nextResponse = fixture("getmeas-empty.json");
    Instant since = Instant.parse("2026-07-01T00:00:00Z");

    adapter.fetchMeasureGroups("the-access-token", since);

    assertThat(transport.lastForm.get("lastupdate"))
        .isEqualTo(Long.toString(since.getEpochSecond()));
  }

  @Test
  void fetchMeasureGroupsOmitsLastupdateOnAFirstSync() {
    transport.nextResponse = fixture("getmeas-empty.json");

    adapter.fetchMeasureGroups("the-access-token", null);

    assertThat(transport.lastForm).doesNotContainKey("lastupdate");
  }

  @Test
  void mapsAMultiTypeGroupToAFullyPopulatedBodyMeasurement() {
    transport.nextResponse = fixture("getmeas-multi-type.json");

    List<ImportedMeasureGroup> groups = adapter.fetchMeasureGroups("token", null);

    assertThat(groups).hasSize(1);
    ImportedMeasureGroup group = groups.get(0);
    assertThat(group.externalGroupId()).isEqualTo(400000001L);
    BodyMeasurement measurement = group.measurement();
    assertThat(measurement.source()).isEqualTo(MeasurementSource.WITHINGS);
    assertThat(measurement.measuredAt()).isEqualTo(Instant.ofEpochSecond(1752595200L));
    assertThat(measurement.weightKg()).isEqualTo(72.5, within(0.001));
    assertThat(measurement.bodyFatPercentage()).isEqualTo(18.2, within(0.001));
    assertThat(measurement.muscleMassKg()).isEqualTo(32.0, within(0.001));
    // hydration 43.5kg / weight 72.5kg * 100 = 60.0%
    assertThat(measurement.waterPercentage()).isEqualTo(60.0, within(0.001));
    assertThat(measurement.bmi()).isNull();
  }

  @Test
  void mapsAPartialWeightOnlyGroupWithoutFabricatingOtherFields() {
    transport.nextResponse = fixture("getmeas-partial-weight-only.json");

    List<ImportedMeasureGroup> groups = adapter.fetchMeasureGroups("token", null);

    assertThat(groups).hasSize(1);
    BodyMeasurement measurement = groups.get(0).measurement();
    assertThat(measurement.weightKg()).isEqualTo(71.8, within(0.001));
    assertThat(measurement.bodyFatPercentage()).isNull();
    assertThat(measurement.muscleMassKg()).isNull();
    assertThat(measurement.waterPercentage()).isNull();
    assertThat(measurement.bmi()).isNull();
  }

  @Test
  void ignoresAnUnmodeledMeasureTypeWithoutCrashingAndStillMapsTheModeledOnes() {
    transport.nextResponse = fixture("getmeas-unmodeled-type.json");

    List<ImportedMeasureGroup> groups = adapter.fetchMeasureGroups("token", null);

    assertThat(groups).hasSize(1);
    assertThat(groups.get(0).measurement().weightKg()).isEqualTo(73.1, within(0.001));
  }

  @Test
  void skipsAGroupWithHydrationButNoWeightRatherThanFabricatingAPlaceholderWeight() {
    transport.nextResponse = fixture("getmeas-hydration-without-weight.json");

    List<ImportedMeasureGroup> groups = adapter.fetchMeasureGroups("token", null);

    assertThat(groups).isEmpty();
  }

  @Test
  void anEmptyResponseYieldsNoGroups() {
    transport.nextResponse = fixture("getmeas-empty.json");

    List<ImportedMeasureGroup> groups = adapter.fetchMeasureGroups("token", null);

    assertThat(groups).isEmpty();
  }

  @Test
  void twoGroupsFixtureMapsBothIndependently() {
    transport.nextResponse = fixture("getmeas-two-groups.json");

    List<ImportedMeasureGroup> groups = adapter.fetchMeasureGroups("token", null);

    assertThat(groups).hasSize(2);
    assertThat(groups)
        .extracting(ImportedMeasureGroup::externalGroupId)
        .containsExactlyInAnyOrder(400000010L, 400000011L);
  }

  @Test
  void onAWithingsErrorStatusThrowsProviderSyncExceptionWithNoTokenInTheMessage() {
    transport.nextResponse = fixture("getmeas-error.json");

    assertThatThrownBy(() -> adapter.fetchMeasureGroups("the-access-token", null))
        .isInstanceOf(ProviderSyncException.class)
        .satisfies(ex -> assertThat(ex.getMessage()).doesNotContain("the-access-token"));
  }

  @Test
  void onAMalformedResponseThrowsWithoutLeakingTheRawBody() {
    transport.nextResponse = fixture("getmeas-malformed.json");

    assertThatThrownBy(() -> adapter.fetchMeasureGroups("token", null))
        .isInstanceOf(ProviderSyncException.class)
        .satisfies(ex -> assertThat(ex.getMessage()).doesNotContain("not valid json"));
  }

  @Test
  void whenTheTransportFailsWrapsItWithoutLeakingTheAccessToken() {
    transport.failWith = new RuntimeException("Connection refused: wbsapi.withings.net");

    assertThatThrownBy(() -> adapter.fetchMeasureGroups("the-access-token", null))
        .isInstanceOf(ProviderSyncException.class)
        .satisfies(ex -> assertThat(ex.getMessage()).doesNotContain("the-access-token"));
  }

  private static String fixture(String name) {
    try {
      return Files.readString(
          Path.of("src/test/resources/fixtures/withings/" + name), StandardCharsets.UTF_8);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  /** Fake HTTP transport — records the last call, never performs a real network request. */
  private static class FakeWithingsHttpTransport implements WithingsHttpTransport {
    String nextResponse;
    RuntimeException failWith;
    String lastUrl;
    String lastAccessToken;
    Map<String, String> lastForm;

    @Override
    public String post(String url, Map<String, String> formParams) {
      throw new UnsupportedOperationException(
          "WithingsMeasuresAdapter only calls the bearer-authenticated transport method");
    }

    @Override
    public String postAuthenticated(
        String url, Map<String, String> formParams, String accessToken) {
      lastUrl = url;
      lastForm = formParams;
      lastAccessToken = accessToken;
      if (failWith != null) {
        throw failWith;
      }
      return nextResponse;
    }
  }
}
