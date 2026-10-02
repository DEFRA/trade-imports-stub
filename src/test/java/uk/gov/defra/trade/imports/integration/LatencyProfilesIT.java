package uk.gov.defra.trade.imports.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = {
    "stub-latency.integrations.mdm.profile=sla",
    "stub-latency.integrations.mdm.sla.p50-ms=200",
    "stub-latency.integrations.mdm.sla.p95-ms=200",
    "stub-latency.integrations.mdm.sla.p99-ms=200"
})
class LatencyProfilesIT extends IntegrationBase {

    private static final long MDM_DELAY_MS = 200;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void latencyProfiles_shouldApplyTheSlaDelayAndReportWhatWasAnswered() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Ocp-Apim-Subscription-Key", "stub-key");

        long startedAt = System.nanoTime();
        ResponseEntity<String> countries = restTemplate.exchange(
            "/mdm/geo/countries", HttpMethod.GET, new HttpEntity<>(headers), String.class);
        long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;

        assertThat(countries.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(elapsedMs).isGreaterThanOrEqualTo(MDM_DELAY_MS);

        JsonNode mdm = integration("mdm");
        assertThat(mdm.get("profile").asText()).isEqualTo("sla");
        assertThat(mdm.get("answered").get("count").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(mdm.get("answered").get("p50Ms").asLong()).isGreaterThanOrEqualTo(MDM_DELAY_MS);
        assertThat(mdm.get("answered").get("peakPerSecond").asInt()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void latencyProfiles_shouldReportTheTokenEndpointAtZeroDelay() {
        ResponseEntity<String> token =
            restTemplate.postForEntity("/tenant/oauth2/v2.0/token", null, String.class);
        assertThat(token.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode tradeToken = integration("trade-token");
        assertThat(tradeToken.get("profile").asText()).isEqualTo("zero-delay");
        assertThat(tradeToken.get("targets").get("p50Ms").asLong()).isZero();
        assertThat(tradeToken.get("targets").get("p95Ms").asLong()).isZero();
        assertThat(tradeToken.get("targets").get("p99Ms").asLong()).isZero();
        assertThat(tradeToken.get("answered").get("count").asInt()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void latencyProfiles_shouldNotCountHealthChecks() {
        int mdmBefore = integration("mdm").get("answered").get("count").asInt();
        int tokenBefore = integration("trade-token").get("answered").get("count").asInt();

        ResponseEntity<String> health = restTemplate.getForEntity("/health", String.class);
        assertThat(health.getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(integration("mdm").get("answered").get("count").asInt()).isEqualTo(mdmBefore);
        assertThat(integration("trade-token").get("answered").get("count").asInt())
            .isEqualTo(tokenBefore);
    }

    @Test
    void latencyProfiles_shouldForgetAnsweredLatencies_whenTheyAreCleared() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Ocp-Apim-Subscription-Key", "stub-key");
        restTemplate.exchange(
            "/mdm/geo/countries", HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertThat(integration("mdm").get("answered").get("count").asInt()).isGreaterThanOrEqualTo(1);

        ResponseEntity<Void> cleared = restTemplate.exchange(
            "/latency-profiles/answered", HttpMethod.DELETE, null, Void.class);

        assertThat(cleared.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(integration("mdm").get("answered").get("count").asInt()).isZero();
        assertThat(integration("mdm").get("answered").get("peakPerSecond").asInt()).isZero();
        assertThat(integration("trade-token").get("answered").get("count").asInt()).isZero();
    }

    private JsonNode integration(String name) {
        JsonNode body = restTemplate.getForObject("/latency-profiles", JsonNode.class);
        assertThat(body.get("stub").asText()).isEqualTo("trade-imports-stub");
        for (JsonNode integration : body.get("integrations")) {
            if (name.equals(integration.get("integration").asText())) {
                return integration;
            }
        }
        throw new AssertionError("No integration named " + name);
    }
}
