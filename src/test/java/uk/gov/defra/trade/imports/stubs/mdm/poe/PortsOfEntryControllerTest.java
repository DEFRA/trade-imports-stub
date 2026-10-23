package uk.gov.defra.trade.imports.stubs.mdm.poe;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.defra.trade.imports.stubs.mdm.MdmStubTestRequests.ANY_KEY;
import static uk.gov.defra.trade.imports.stubs.mdm.MdmStubTestRequests.KEY_HEADER;
import static uk.gov.defra.trade.imports.stubs.mdm.MdmStubTestRequests.carriesTheStubTraceHeader;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.defra.trade.imports.stubs.mdm.MdmStubWebMvcTest;

@MdmStubWebMvcTest(PortsOfEntryController.class)
class PortsOfEntryControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void getPortsOfEntry_shouldAnswerMdmsPortRecordsWholeInMdmOrder() throws Exception {
    mockMvc.perform(get("/mdm/trade/bcp/poes").header(KEY_HEADER, ANY_KEY))
        .andExpect(status().isOk())
        .andExpect(carriesTheStubTraceHeader())
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
