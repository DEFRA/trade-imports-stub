package uk.gov.defra.trade.imports.stubs.mdm.product;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.defra.trade.imports.stubs.mdm.MdmStubRequests.mdmGet;
import static uk.gov.defra.trade.imports.stubs.mdm.MdmStubResponseMatchers.carriesTheStubTraceHeader;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.defra.trade.imports.stubs.mdm.MdmStubWebMvcTest;

@MdmStubWebMvcTest(ProductController.class)
class ProductControllerTest {

  private static final String SEARCH_PATH = "/mdm/trade/product/search";
  private static final String COMMODITIES_PATH = "/mdm/trade/product/commodities";

  @Autowired
  private MockMvc mockMvc;

  @Test
  void search_shouldAnswerTheSavedResponse_forASavedTerm() throws Exception {
    mockMvc.perform(mdmGet(SEARCH_PATH)
            .param("q", "cat").param("dataset", "gbnag").param("page", "0").param("size", "100"))
        .andExpect(status().isOk())
        .andExpect(carriesTheStubTraceHeader())
        .andExpect(jsonPath("$.totalElements").value(10))
        .andExpect(jsonPath("$.content.length()").value(10))
        .andExpect(jsonPath("$.content[0].speciesName").value("Felis catus"))
        .andExpect(jsonPath("$.content[0].commodityCnCode").value("01061900"));
  }

  @Test
  void search_shouldMatchTheTermIgnoringCase() throws Exception {
    mockMvc.perform(mdmGet(SEARCH_PATH).param("q", "BOS TAURUS"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(3));
  }

  @Test
  void search_shouldAnswerAnEmptyPage_forATermThatWasNotSaved() throws Exception {
    mockMvc.perform(mdmGet(SEARCH_PATH)
            .param("q", "zebra").param("page", "0").param("size", "100"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(0))
        .andExpect(jsonPath("$.totalElements").value(0))
        .andExpect(jsonPath("$.size").value(100));
  }

  @Test
  void search_shouldAnswerAnEmptyPageWithTheSavedTotal_forALaterPage() throws Exception {
    mockMvc.perform(mdmGet(SEARCH_PATH)
            .param("q", "cattle").param("page", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(0))
        .andExpect(jsonPath("$.page").value(1))
        .andExpect(jsonPath("$.totalElements").value(7));
  }

  @Test
  void search_shouldRefuseTheCall_whenTheTermIsShorterThanTwoCharacters() throws Exception {
    mockMvc.perform(mdmGet(SEARCH_PATH).param("q", "c"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("q must be at least 2 characters"));
  }

  @Test
  void search_shouldRefuseTheCall_whenTheSubscriptionKeyHeaderIsMissing() throws Exception {
    mockMvc.perform(get(SEARCH_PATH).param("q", "cat"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void getCommoditySpecies_shouldAnswerTheSavedPage_forASavedCode() throws Exception {
    mockMvc.perform(mdmGet(speciesPath("0102"))
            .param("dataset", "gbnag").param("page", "0").param("size", "100"))
        .andExpect(status().isOk())
        .andExpect(carriesTheStubTraceHeader())
        .andExpect(jsonPath("$.totalElements").value(15))
        .andExpect(jsonPath("$.content.length()").value(15));
  }

  @Test
  void getCommoditySpecies_shouldAnswerAnEmptyPage_forAnUnknownCode() throws Exception {
    mockMvc.perform(mdmGet(speciesPath("030111"))
            .param("page", "0").param("size", "100"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(0))
        .andExpect(jsonPath("$.totalElements").value(0));
  }

  @Test
  void getCommoditySpecies_shouldAnswerAnEmptyPageWithTheSavedTotal_pastTheLastPage()
      throws Exception {
    mockMvc.perform(mdmGet(speciesPath("0101"))
            .param("page", "1").param("size", "100"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(0))
        .andExpect(jsonPath("$.totalElements").value(5));
  }

  @Test
  void getCommodities_shouldAnswerTheSavedCommodityOfEachKnownCode_inTheOrderAsked()
      throws Exception {
    mockMvc.perform(mdmGet(COMMODITIES_PATH)
            .param("codes", "05111000,0102,030111").param("dataset", "gbnag"))
        .andExpect(status().isOk())
        .andExpect(carriesTheStubTraceHeader())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].code").value("05111000"))
        .andExpect(jsonPath("$[0].isGerminal").value(true))
        .andExpect(jsonPath("$[1].code").value("0102"))
        .andExpect(jsonPath("$[1].isGerminal").value(false));
  }

  private static String speciesPath(String code) {
    return "/mdm/trade/product/commodities/%s/species".formatted(code);
  }
}
