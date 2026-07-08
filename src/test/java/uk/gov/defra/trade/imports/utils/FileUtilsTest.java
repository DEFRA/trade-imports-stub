package uk.gov.defra.trade.imports.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.defra.trade.imports.stubs.mdm.poe.MdmPortOfEntry;
import uk.gov.defra.trade.imports.stubs.mdm.poe.MdmPortsResponse;

class FileUtilsTest {

  private FileUtils fileUtils;

  @BeforeEach
  void setUp() {
    fileUtils = new FileUtils(new ObjectMapper());
  }

  @Test
  @SuppressWarnings("unchecked")
  void getObjectFromFile_deserializesCountriesFixtureAsList() {
    // TypeReference diamond inference erases to Object at runtime; actual runtime
    // type is List<LinkedHashMap>. The stub re-serialises this via Jackson without
    // accessing typed fields, which is why the countries endpoint works in production.
    // Callers must NOT access typed elements from this method for concrete (non-List) types
    // — use getObjectFromFile(String, Class<T>) instead.
    List<Map<String, Object>> countries = fileUtils.getObjectFromFile("responses/countriesResponse.json");

    assertThat(countries).isNotEmpty();
    assertThat(countries.get(0)).containsKey("name");
    assertThat(countries.get(0)).containsKey("effectiveAlpha2");
    assertThat(countries.get(0).get("name")).isNotNull();
  }

  @Test
  void getObjectFromFile_withClass_deserializesMdmPortsResponseFromFixture() {
    // MdmPortsResponse is a concrete (non-generic) type — the Class<T> overload is
    // required here. TypeReference diamond inference erases to Object, causing a
    // ClassCastException on assignment (unlike List, where erasure is silent).
    MdmPortsResponse response = fileUtils.getObjectFromFile(
        "responses/portsOfEntryResponse.json", MdmPortsResponse.class);

    assertThat(response).isNotNull();
    assertThat(response.getResult()).isNotNull().isNotEmpty();
  }

  @Test
  void getObjectFromFile_withClass_returnsCorrectPortData() {
    MdmPortsResponse response = fileUtils.getObjectFromFile(
        "responses/portsOfEntryResponse.json", MdmPortsResponse.class);

    List<MdmPortOfEntry> ports = response.getResult();
    assertThat(ports).hasSize(3);
    assertThat(ports).extracting(MdmPortOfEntry::getCode)
        .containsExactly("GBABE", "GBEMA", "GBEDI");
    assertThat(ports).extracting(MdmPortOfEntry::getName)
        .containsExactly("Aberdeen", "East Midlands Airport", "Edinburgh");
  }

  @Test
  void getObjectFromFile_throwsRuntimeException_whenFileNotFound() {
    assertThatThrownBy(() -> fileUtils.getObjectFromFile("responses/does-not-exist.json"))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Failed to read from json file");
  }

  @Test
  void getObjectFromFile_withClass_throwsRuntimeException_whenFileNotFound() {
    assertThatThrownBy(
            () -> fileUtils.getObjectFromFile("responses/does-not-exist.json", MdmPortsResponse.class))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Failed to read from json file");
  }
}
