package uk.gov.defra.trade.imports.stubs.mdm.countries;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.defra.trade.imports.utils.FileUtils;

@ExtendWith(MockitoExtension.class)
class CountriesControllerTest {

  private static final String SUBSCRIPTION_KEY = "test-key";

  @Mock
  private FileUtils fileUtils;

  private CountriesController controller;

  @BeforeEach
  void setUp() {
    controller = new CountriesController(fileUtils);
  }

  @Test
  void getCountries_returnsIsoFixture_whenSystemIsIso() {
    MdmCountry france = MdmCountry.builder()
        .effectiveAlpha2("FR")
        .effectiveAlias("France")
        .system("iso")
        .build();
    when(fileUtils.getObjectFromFile("responses/isoCountriesResponse.json", MdmCountry[].class))
        .thenReturn(new MdmCountry[] {france});

    ResponseEntity<List<MdmCountry>> response =
        controller.getCountries(SUBSCRIPTION_KEY, "ISO", null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getHeaders().getFirst("x-ms-middleware-request-id"))
        .isEqualTo("stub-trace-id");
    assertThat(response.getBody()).extracting(MdmCountry::getEffectiveAlpha2).containsExactly("FR");
    verify(fileUtils).getObjectFromFile("responses/isoCountriesResponse.json", MdmCountry[].class);
  }

  @Test
  void getCountries_returnsIsoFixture_whenSystemIsIsoCaseInsensitive() {
    when(fileUtils.getObjectFromFile("responses/isoCountriesResponse.json", MdmCountry[].class))
        .thenReturn(new MdmCountry[] {});

    controller.getCountries(SUBSCRIPTION_KEY, "iso", null);

    verify(fileUtils).getObjectFromFile("responses/isoCountriesResponse.json", MdmCountry[].class);
  }

  @Test
  void getCountries_returnsGbnagFixture_whenSystemIsGbnag() {
    MdmCountry austria = MdmCountry.builder()
        .effectiveAlpha2("AT")
        .effectiveAlias("Austria")
        .build();
    when(fileUtils.getObjectFromFile("responses/countriesResponse.json", MdmCountry[].class))
        .thenReturn(new MdmCountry[] {austria});

    ResponseEntity<List<MdmCountry>> response =
        controller.getCountries(SUBSCRIPTION_KEY, "GBNAG", "GBNAG_SPS_EX");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).extracting(MdmCountry::getEffectiveAlpha2).containsExactly("AT");
    verify(fileUtils).getObjectFromFile("responses/countriesResponse.json", MdmCountry[].class);
  }

  @Test
  void getCountries_returnsGbnagFixture_whenSystemIsAbsent() {
    when(fileUtils.getObjectFromFile("responses/countriesResponse.json", MdmCountry[].class))
        .thenReturn(new MdmCountry[] {});

    controller.getCountries(SUBSCRIPTION_KEY, null, null);

    verify(fileUtils).getObjectFromFile("responses/countriesResponse.json", MdmCountry[].class);
  }

  @Test
  void isoCountriesFixture_deserializesAndIncludesTransitTestCountries() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    FileUtils realFileUtils = new FileUtils(mapper);

    MdmCountry[] countries = realFileUtils.getObjectFromFile(
        "responses/isoCountriesResponse.json", MdmCountry[].class);

    assertThat(countries).isNotEmpty();
    assertThat(countries).extracting(MdmCountry::getEffectiveAlpha2)
        .contains("FR", "DE", "NL", "US", "TR")
        .doesNotContain("GB");
    assertThat(countries).extracting(MdmCountry::getEffectiveAlias)
        .contains("France", "Germany", "Netherlands");
  }
}
