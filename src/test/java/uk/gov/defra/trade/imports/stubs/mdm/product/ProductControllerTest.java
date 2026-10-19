package uk.gov.defra.trade.imports.stubs.mdm.product;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.defra.trade.imports.exceptions.GlobalExceptionHandler;
import uk.gov.defra.trade.imports.faults.StubFaults;
import uk.gov.defra.trade.imports.latency.LatencyConfig;
import uk.gov.defra.trade.imports.latency.StubLatencies;
import uk.gov.defra.trade.imports.utils.FileUtils;

@WebMvcTest(ProductController.class)
@Import({LatencyConfig.class, StubLatencies.class, StubFaults.class, GlobalExceptionHandler.class,
    FileUtils.class})
class ProductControllerTest {

  private static final String KEY_HEADER = "Ocp-Apim-Subscription-Key";
  private static final String ANY_KEY = "any-value-at-all";
  private static final String SEARCH = "/mdm/trade/product/search";

  @Autowired
  private MockMvc mockMvc;

  @Test
  void search_shouldAnswerTheSavedResponse_forASavedTerm() throws Exception {
    mockMvc.perform(get(SEARCH).header(KEY_HEADER, ANY_KEY)
            .param("q", "cat").param("dataset", "gbnag").param("page", "0").param("size", "100"))
        .andExpect(status().isOk())
        .andExpect(header().string("x-ms-middleware-request-id", "stub-trace-id"))
        .andExpect(jsonPath("$.totalElements").value(13))
        .andExpect(jsonPath("$.content.length()").value(13))
        .andExpect(jsonPath("$.content[0].speciesName").value("Felis"))
        .andExpect(jsonPath("$.content[0].commodityCnCode").value("01061900"));
  }

  @Test
  void search_shouldMatchTheTermIgnoringCase() throws Exception {
    mockMvc.perform(get(SEARCH).header(KEY_HEADER, ANY_KEY).param("q", "BOS TAURUS"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(4));
  }

  @Test
  void search_shouldAnswerAnEmptyPage_forATermThatWasNotSaved() throws Exception {
    mockMvc.perform(get(SEARCH).header(KEY_HEADER, ANY_KEY)
            .param("q", "zebra").param("page", "0").param("size", "100"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(0))
        .andExpect(jsonPath("$.totalElements").value(0))
        .andExpect(jsonPath("$.size").value(100));
  }

  @Test
  void search_shouldAnswerAnEmptyPageWithTheSavedTotal_forALaterPage() throws Exception {
    mockMvc.perform(get(SEARCH).header(KEY_HEADER, ANY_KEY).param("q", "cattle").param("page", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(0))
        .andExpect(jsonPath("$.page").value(1))
        .andExpect(jsonPath("$.totalElements").value(8));
  }

  @Test
  void search_shouldRefuseTheCall_whenTheTermIsShorterThanTwoCharacters() throws Exception {
    mockMvc.perform(get(SEARCH).header(KEY_HEADER, ANY_KEY).param("q", "c"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("q must be at least 2 characters"));
  }

  @Test
  void search_shouldRefuseTheCall_whenTheSubscriptionKeyHeaderIsMissing() throws Exception {
    mockMvc.perform(get(SEARCH).param("q", "cat"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void getCommoditySpecies_shouldAnswerTheSavedPage_forASavedCode() throws Exception {
    mockMvc.perform(get("/mdm/trade/product/commodities/0102/species").header(KEY_HEADER, ANY_KEY)
            .param("dataset", "gbnag").param("page", "0").param("size", "100"))
        .andExpect(status().isOk())
        .andExpect(header().string("x-ms-middleware-request-id", "stub-trace-id"))
        .andExpect(jsonPath("$.totalElements").value(15))
        .andExpect(jsonPath("$.content.length()").value(15));
  }

  @Test
  void getCommoditySpecies_shouldAnswerAnEmptyPage_forAnUnknownCode() throws Exception {
    mockMvc.perform(get("/mdm/trade/product/commodities/030111/species").header(KEY_HEADER, ANY_KEY)
            .param("page", "0").param("size", "100"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(0))
        .andExpect(jsonPath("$.totalElements").value(0));
  }

  @Test
  void getCommoditySpecies_shouldAnswerAnEmptyPageWithTheSavedTotal_pastTheLastPage()
      throws Exception {
    mockMvc.perform(get("/mdm/trade/product/commodities/0101/species").header(KEY_HEADER, ANY_KEY)
            .param("page", "1").param("size", "100"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(0))
        .andExpect(jsonPath("$.totalElements").value(5));
  }
}
