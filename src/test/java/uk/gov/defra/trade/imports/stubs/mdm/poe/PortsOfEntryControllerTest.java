package uk.gov.defra.trade.imports.stubs.mdm.poe;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
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

@WebMvcTest(PortsOfEntryController.class)
@Import({LatencyConfig.class, StubLatencies.class, StubFaults.class, GlobalExceptionHandler.class,
    FileUtils.class})
class PortsOfEntryControllerTest {

  private static final String KEY_HEADER = "Ocp-Apim-Subscription-Key";
  private static final String ANY_KEY = "any-value-at-all";

  @Autowired
  private MockMvc mockMvc;

  @Test
  void getPortsOfEntry_shouldAnswerMdmsPortRecordsWholeInMdmOrder() throws Exception {
    mockMvc.perform(get("/mdm/trade/bcp/poes").header(KEY_HEADER, ANY_KEY))
        .andExpect(status().isOk())
        .andExpect(header().string("x-ms-middleware-request-id", "stub-trace-id"))
        .andExpect(jsonPath("$.result.length()").value(78))
        .andExpect(jsonPath("$.result[0].code").value("GB DYC"))
        .andExpect(jsonPath("$.result[0].traffic").value("Airport"))
        .andExpect(jsonPath("$.result[0].townCity").value("Aberdeen"))
        .andExpect(jsonPath("$.result[0].status.description").value("Approved-Live"));
  }

  @Test
  void getPortsOfEntry_shouldCarryEveryPortsTraffic() throws Exception {
    mockMvc.perform(get("/mdm/trade/bcp/poes").header(KEY_HEADER, ANY_KEY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result[?(@.traffic == 'Rail')].code").value(contains("GB FOL")))
        .andExpect(jsonPath("$.result[?(@.traffic == 'Airport')]", hasSize(18)));
  }

  @Test
  void getPortsOfEntry_shouldRefuseTheCall_whenTheSubscriptionKeyHeaderIsMissing()
      throws Exception {
    mockMvc.perform(get("/mdm/trade/bcp/poes"))
        .andExpect(status().isBadRequest());
  }
}
