package uk.gov.defra.trade.imports.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.defra.trade.imports.stubs.mdm.countries.MdmCountry;
import uk.gov.defra.trade.imports.stubs.mdm.poe.MdmPortOfEntry;
import uk.gov.defra.trade.imports.stubs.mdm.poe.MdmPortsResponse;

class FileUtilsTest {

  private FileUtils fileUtils;

  @BeforeEach
  void setUp() {
    fileUtils = new FileUtils(new ObjectMapper());
  }

  @Test
  void getObjectFromFile_deserializesIsoCountriesFixtureAsArray() {
    MdmCountry[] countries = fileUtils.getObjectFromFile(
        "responses/isoCountriesResponse.json", MdmCountry[].class);

    assertThat(countries).isNotEmpty();
    assertThat(countries[0].getName()).isNotBlank();
    assertThat(countries[0].getEffectiveAlpha2()).isNotBlank();
    assertThat(countries).extracting(MdmCountry::getEffectiveAlpha2)
        .contains("FR", "DE")
        .doesNotContain("GB");
  }

  @Test
  void getObjectFromFile_deserializesCountriesFixtureAsArray() {
    MdmCountry[] countries = fileUtils.getObjectFromFile(
        "responses/countriesResponse.json", MdmCountry[].class);

    assertThat(countries).isNotEmpty();
    assertThat(countries[0].getName()).isNotBlank();
    assertThat(countries[0].getEffectiveAlpha2()).isNotBlank();
  }

  @Test
  void getObjectFromFile_deserializesMdmPortsResponseFromFixture() {
    MdmPortsResponse response = fileUtils.getObjectFromFile(
        "responses/portsOfEntryResponse.json", MdmPortsResponse.class);

    assertThat(response).isNotNull();
    assertThat(response.getResult()).isNotNull().isNotEmpty();
  }

  @Test
  void getObjectFromFile_returnsCorrectPortData() {
    MdmPortsResponse response = fileUtils.getObjectFromFile(
        "responses/portsOfEntryResponse.json", MdmPortsResponse.class);

    List<MdmPortOfEntry> ports = response.getResult();
    assertThat(ports).hasSize(78);
    assertThat(ports.get(0).getCode()).isEqualTo("GB DYC");
    assertThat(ports.get(0).getName()).isEqualTo("Aberdeen Airport");
    assertThat(ports).extracting(MdmPortOfEntry::getCode)
        .contains("GB EMA", "GB EDI", "GB DVR");
  }

  @Test
  void getObjectFromFile_throwsRuntimeException_whenFileNotFound() {
    assertThatThrownBy(
            () -> fileUtils.getObjectFromFile("responses/does-not-exist.json", MdmCountry[].class))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Failed to read from json file");
  }
}
