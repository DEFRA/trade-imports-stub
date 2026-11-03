package uk.gov.defra.trade.imports.stubs.mdm.countries;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.defra.trade.imports.stubs.mdm.MdmStubRequests.mdmGet;
import static uk.gov.defra.trade.imports.stubs.mdm.MdmStubResponseMatchers.carriesTheStubTraceHeader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.defra.trade.imports.stubs.mdm.MdmStubWebMvcTest;

@MdmStubWebMvcTest(CountriesController.class)
class CountriesControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void getCountries_shouldAnswerAllTwoHundredAndFiftyMdmCountriesInMdmOrder_whenNoBlockIsAsked()
      throws Exception {
    mockMvc.perform(mdmGet("/mdm/geo/countries"))
        .andExpect(status().isOk())
        .andExpect(carriesTheStubTraceHeader())
        .andExpect(jsonPath("$.length()").value(250))
        .andExpect(jsonPath("$[0].alpha2.value").value("AW"))
        .andExpect(jsonPath("$[249]").exists());
  }

  @Test
  void getCountries_shouldAnswerTheThirtyTwoCountriesInTheBlock_whenGbnagSpsExIsAsked()
      throws Exception {
    mockMvc.perform(mdmGet("/mdm/geo/countries")
            .param("system", "GBNAG")
            .param("blocks", "GBNAG_SPS_EX"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(32))
        .andExpect(jsonPath("$[*].alpha2.value").value(contains(
            "HU", "LV", "CY", "GR", "CZ", "DK", "MT", "CH", "SK", "PL", "DE", "NO", "FR", "PT",
            "IT", "NL", "FI", "IS", "IE", "BE", "LU", "EE", "HR", "RO", "LT", "BG", "LI", "GB",
            "ES", "SE", "SI", "AT")));
  }

  @Test
  void getCountries_shouldIncludeACountryWhoseBlockEntryExcludesIt() throws Exception {
    mockMvc.perform(mdmGet("/mdm/geo/countries")
            .param("blocks", "GBNAG_SPS_EX"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].alpha2.value").value(hasItem("GB")))
        .andExpect(jsonPath("$[?(@.alpha2.value=='GB')].blocks[?(@.name=='GBNAG_SPS_EX')].includeCountry")
            .value(contains(false)));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " "})
  void getCountries_shouldAnswerAllTwoHundredAndFiftyCountries_whenTheBlocksParameterIsBlank(
      String blocks) throws Exception {
    mockMvc.perform(mdmGet("/mdm/geo/countries")
            .param("blocks", blocks))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(250));
  }

  @Test
  void getCountries_shouldAnswerNoCountries_whenTheBlockIsUnknown() throws Exception {
    mockMvc.perform(mdmGet("/mdm/geo/countries")
            .param("blocks", "PERF_TEST_NO_SUCH_BLOCK"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").value(hasSize(0)));
  }

  @Test
  void getCountries_shouldServeFieldsNoStubModelNames() throws Exception {
    mockMvc.perform(mdmGet("/mdm/geo/countries"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].countryUUID.content").exists())
        .andExpect(jsonPath("$[0].ISONumeric.value").exists());
  }

  @Test
  void getCountries_shouldRefuseTheCall_whenTheSubscriptionKeyHeaderIsMissing() throws Exception {
    mockMvc.perform(get("/mdm/geo/countries"))
        .andExpect(status().isBadRequest());
  }
}
