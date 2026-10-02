package uk.gov.defra.trade.imports.latency;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LatencyProfilesController.class)
@Import({LatencyConfig.class, StubLatencies.class})
@TestPropertySource(properties = "stub-latency.integrations.mdm.profile=sla")
class LatencyProfilesControllerTest {

    private static final String MDM = "$.integrations[?(@.integration=='mdm')]";
    private static final String TRADE_TOKEN = "$.integrations[?(@.integration=='trade-token')]";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getLatencyProfiles_shouldReportTheMdmProfileAndItsMetadata() throws Exception {
        mockMvc.perform(get("/latency-profiles"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.stub").value("trade-imports-stub"))
            .andExpect(jsonPath("$.integrations.length()").value(2))
            .andExpect(jsonPath(MDM + ".interface").isNotEmpty())
            .andExpect(jsonPath(MDM + ".owner").value("MDM / data platform team"))
            .andExpect(jsonPath(MDM + ".serviceLevelSource").isNotEmpty())
            .andExpect(jsonPath(MDM + ".agreed").value(false))
            .andExpect(jsonPath(MDM + ".lastConformed", contains(nullValue())))
            .andExpect(jsonPath(MDM + ".profile").value("sla"))
            .andExpect(jsonPath(MDM + ".slaTargets.p95Ms").value(400))
            .andExpect(jsonPath(MDM + ".targets.p95Ms").value(400))
            .andExpect(jsonPath(MDM + ".fitted.p95Ms").value(470))
            .andExpect(jsonPath(MDM + ".answered.count").value(0));
    }

    @Test
    void clearAnswered_shouldRespondNoContent() throws Exception {
        mockMvc.perform(delete("/latency-profiles/answered"))
            .andExpect(status().isNoContent());
    }

    @Test
    void getLatencyProfiles_shouldReportTheTradeTokenAtZeroDelay() throws Exception {
        mockMvc.perform(get("/latency-profiles"))
            .andExpect(status().isOk())
            .andExpect(jsonPath(TRADE_TOKEN + ".profile").value("zero-delay"))
            .andExpect(jsonPath(TRADE_TOKEN + ".targets.p50Ms").value(0))
            .andExpect(jsonPath(TRADE_TOKEN + ".targets.p95Ms").value(0))
            .andExpect(jsonPath(TRADE_TOKEN + ".targets.p99Ms").value(0));
    }
}
