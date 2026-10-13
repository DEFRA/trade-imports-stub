package uk.gov.defra.trade.imports.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FileUtilsTest {

  private static final List<String> TRAFFIC_TYPES = List.of("Airport", "Port", "Rail");

  private FileUtils fileUtils;

  @BeforeEach
  void setUp() {
    fileUtils = new FileUtils(new ObjectMapper());
  }

  @Test
  void getObjectFromFile_readsAllTwoHundredAndFiftyMdmCountriesInMdmOrder() {
    // Given / When
    JsonNode countries = fileUtils.getObjectFromFile(
        "responses/countriesResponse.json", JsonNode.class);

    // Then
    assertThat(countries.isArray()).isTrue();
    assertThat(countries).hasSize(250);
    assertThat(countries.get(0).path("alpha2").path("value").asText()).isEqualTo("AW");
    assertThat(countries.get(0).path("effectiveAlias").asText()).isEqualTo("Aruba");
  }

  @Test
  void getObjectFromFile_keepsSpainsCanaryIslandsSubdivision() {
    // Given
    JsonNode countries = fileUtils.getObjectFromFile(
        "responses/countriesResponse.json", JsonNode.class);

    // When
    JsonNode spain = StreamSupport.stream(countries.spliterator(), false)
        .filter((country) -> "ES".equals(country.path("effectiveAlpha2").asText()))
        .findFirst()
        .orElseThrow();

    // Then
    assertThat(spain.path("subDivisions")).hasSize(1);
    JsonNode canaryIslands = spain.path("subDivisions").get(0);
    assertThat(canaryIslands.path("code").path("value").asText()).isEqualTo("ES-CN");
    assertThat(canaryIslands.path("name").asText()).isEqualTo("Canary Islands");
    assertThat(canaryIslands.path("blocs")).extracting(JsonNode::asText)
        .containsExactly("OMR", "GBNAG_SPS_EX");
  }

  @Test
  void getObjectFromFile_fillsInNoMaskedValue() {
    // Given / When
    JsonNode countries = fileUtils.getObjectFromFile(
        "responses/countriesResponse.json", JsonNode.class);
    JsonNode ports = fileUtils.getObjectFromFile(
        "responses/portsOfEntryResponse.json", JsonNode.class);

    // Then
    assertThat(leafTexts(countries)).noneMatch((text) -> text.contains("******"));
    assertThat(leafTexts(ports)).noneMatch((text) -> text.contains("******"));
  }

  @Test
  void getObjectFromFile_readsTheSeventyEightMdmPortsWithTheirTraffic() {
    // Given / When
    JsonNode response = fileUtils.getObjectFromFile(
        "responses/portsOfEntryResponse.json", JsonNode.class);
    JsonNode ports = response.path("result");

    // Then
    assertThat(ports).hasSize(78);
    assertThat(ports.get(0).path("code").asText()).isEqualTo("GB DYC");
    assertThat(ports.get(0).path("name").asText()).isEqualTo("Aberdeen Airport");
    assertThat(ports.get(0).path("traffic").asText()).isEqualTo("Airport");
    assertThat(ports).extracting((port) -> port.path("code").asText())
        .contains("GB EMA", "GB EDI", "GB DVR");
    assertThat(ports).extracting((port) -> port.path("traffic").asText())
        .allMatch(TRAFFIC_TYPES::contains);
  }

  @Test
  void getObjectFromFile_throwsRuntimeException_whenFileNotFound() {
    assertThatThrownBy(
            () -> fileUtils.getObjectFromFile("responses/does-not-exist.json", JsonNode.class))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Failed to read from json file");
  }

  private static Stream<String> leafTexts(JsonNode node) {
    return node.isValueNode()
        ? Stream.of(node.asText())
        : StreamSupport.stream(node.spliterator(), false).flatMap(FileUtilsTest::leafTexts);
  }
}
