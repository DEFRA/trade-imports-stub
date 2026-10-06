package uk.gov.defra.trade.imports.faults;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.defra.trade.imports.exceptions.GlobalExceptionHandler;
import uk.gov.defra.trade.imports.latency.LatencyConfig;
import uk.gov.defra.trade.imports.latency.StubLatencies;

@WebMvcTest(FaultsController.class)
@Import({LatencyConfig.class, StubLatencies.class, StubFaults.class, GlobalExceptionHandler.class})
class FaultsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void clearFaults() throws Exception {
        mockMvc.perform(delete("/faults")).andExpect(status().isNoContent());
    }

    @Test
    void putFault_shouldAnswerTheIntegrationsReportEntry() throws Exception {
        mockMvc.perform(put("/faults/mdm")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"kind\":\"error\",\"rate\":0.5,\"status\":503,\"expiresInSeconds\":150}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.integration").value("mdm"))
            .andExpect(jsonPath("$.fault.kind").value("error"))
            .andExpect(jsonPath("$.fault.rate").value(0.5))
            .andExpect(jsonPath("$.fault.status").value(503))
            .andExpect(jsonPath("$.injected.error").value(0));
    }

    @Test
    void putFault_shouldAnswerNotFound_forAnUnknownIntegration() throws Exception {
        mockMvc.perform(put("/faults/sqs")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"kind\":\"error\",\"rate\":1,\"expiresInSeconds\":60}"))
            .andExpect(status().isNotFound());
    }

    @Test
    void putFault_shouldAnswerBadRequest_forARateAboveOne() throws Exception {
        mockMvc.perform(put("/faults/mdm")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"kind\":\"error\",\"rate\":2,\"expiresInSeconds\":60}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void putFault_shouldAnswerBadRequest_forAPathOutsideTheIntegration() throws Exception {
        mockMvc.perform(put("/faults/mdm")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"kind\":\"error\",\"rate\":1,\"paths\":[\"/tenant/oauth2/v2.0/token\"],"
                    + "\"expiresInSeconds\":60}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void putFault_shouldAnswerBadRequest_forAnUnknownKind() throws Exception {
        mockMvc.perform(put("/faults/mdm")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"kind\":\"melt\",\"rate\":1,\"expiresInSeconds\":60}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void putFault_shouldAnswerBadRequest_andLeaveNoFault_whenTheRateIsMissing() throws Exception {
        mockMvc.perform(put("/faults/mdm")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"kind\":\"error\",\"expiresInSeconds\":60}"))
            .andExpect(status().isBadRequest());

        mockMvc.perform(get("/faults"))
            .andExpect(jsonPath("$.integrations[1].integration").value("mdm"))
            .andExpect(jsonPath("$.integrations[1].fault").value(nullValue()));
    }

    @Test
    void putFault_shouldAnswerBadRequest_forANullPath() throws Exception {
        mockMvc.perform(put("/faults/mdm")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"kind\":\"error\",\"rate\":1.0,\"expiresInSeconds\":60,"
                    + "\"paths\":[null]}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void deleteFault_shouldAnswerNoContent_forOneIntegration() throws Exception {
        mockMvc.perform(put("/faults/mdm")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"kind\":\"error\",\"rate\":1,\"expiresInSeconds\":60}"))
            .andExpect(status().isOk());

        mockMvc.perform(delete("/faults/mdm")).andExpect(status().isNoContent());

        mockMvc.perform(get("/faults"))
            .andExpect(jsonPath("$.integrations[1].integration").value("mdm"))
            .andExpect(jsonPath("$.integrations[1].fault").value(nullValue()));
    }

    @Test
    void deleteFault_shouldAnswerNotFound_forUnknownIntegration() throws Exception {
        mockMvc.perform(delete("/faults/sqs")).andExpect(status().isNotFound());
    }

    @Test
    void deleteFaults_shouldAnswerNoContent_forEveryIntegration() throws Exception {
        mockMvc.perform(delete("/faults")).andExpect(status().isNoContent());
    }

    @Test
    void getFaults_shouldListTheTradeTokenThenMdmWithEveryInjectedKindAtZero() throws Exception {
        mockMvc.perform(get("/faults"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.stub").value("trade-imports-stub"))
            .andExpect(jsonPath("$.integrations[0].integration").value("trade-token"))
            .andExpect(jsonPath("$.integrations[1].integration").value("mdm"))
            .andExpect(jsonPath("$.integrations[1].injected.slow").value(0))
            .andExpect(jsonPath("$.integrations[1].injected.hang").value(0))
            .andExpect(jsonPath("$.integrations[1].injected.reset").value(0))
            .andExpect(jsonPath("$.integrations[1].injected.throttle").value(0))
            .andExpect(jsonPath("$.integrations[1].injected.error").value(0));
    }
}
