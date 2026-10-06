package uk.gov.defra.trade.imports.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;

class FaultsIT extends IntegrationBase {

    private static final String COUNTRIES = "/mdm/geo/countries";
    private static final String POES = "/mdm/trade/bcp/poes";
    private static final String TOKEN = "/tenant/oauth2/v2.0/token";
    private static final long DELAY_MS = 300;

    @Autowired
    private TestRestTemplate restTemplate;

    @AfterEach
    void clearFaults() {
        restTemplate.exchange("/faults", HttpMethod.DELETE, null, Void.class);
    }

    @Test
    void error_shouldAnswerTheStatusAndBeCounted() {
        putFault("mdm", Map.of("kind", "error", "rate", 1, "status", 503, "expiresInSeconds", 60));

        ResponseEntity<String> countries = getCountries();

        assertThat(countries.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(countries.getBody()).isEqualTo("{\"fault\":\"error\"}");
        assertThat(integration("mdm").get("injected").get("error").asInt()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void error_shouldFaultOnlyTheNamedPath() {
        putFault("mdm", Map.of(
            "kind", "error", "rate", 1.0, "expiresInSeconds", 60,
            "paths", List.of(POES)));

        ResponseEntity<String> poes = getFrom(POES);
        ResponseEntity<String> countries = getCountries();

        assertThat(poes.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(poes.getBody()).isEqualTo("{\"fault\":\"error\"}");
        assertThat(countries.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(countries.getBody()).isNotEqualTo("{\"fault\":\"error\"}");
    }

    @Test
    void throttle_shouldAnswer429WithRetryAfter() {
        putFault("trade-token", Map.of(
            "kind", "throttle", "rate", 1, "retryAfterSeconds", 5, "expiresInSeconds", 60));

        ResponseEntity<String> token = restTemplate.postForEntity(TOKEN, null, String.class);

        assertThat(token.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(token.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("5");
        assertThat(token.getBody()).isEqualTo("{\"fault\":\"throttle\"}");
    }

    @Test
    void slow_shouldWaitThenAnswerNormally() {
        putFault("mdm", Map.of("kind", "slow", "rate", 1, "delayMs", DELAY_MS, "expiresInSeconds", 60));

        long startedAt = System.nanoTime();
        ResponseEntity<String> countries = getCountries();
        long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;

        assertThat(countries.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(elapsedMs).isGreaterThanOrEqualTo(DELAY_MS);
    }

    @Test
    void hang_shouldHoldTheRequestThenDropTheConnection() {
        putFault("mdm", Map.of("kind", "hang", "rate", 1, "delayMs", DELAY_MS, "expiresInSeconds", 60));

        long startedAt = System.nanoTime();
        assertThatThrownBy(this::getCountries)
            .isInstanceOf(RestClientException.class)
            .hasRootCauseInstanceOf(IOException.class);
        long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;

        assertThat(elapsedMs).isGreaterThanOrEqualTo(DELAY_MS);
    }

    @Test
    void reset_shouldDropTheConnectionWithoutACompleteAnswer() {
        putFault("mdm", Map.of("kind", "reset", "rate", 1, "expiresInSeconds", 60));

        assertThatThrownBy(this::getCountries)
            .isInstanceOf(RestClientException.class)
            .hasRootCauseInstanceOf(IOException.class);
        assertThat(integration("mdm").get("injected").get("reset").asInt()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void delete_shouldLetTheIntegrationAnswerNormallyAgain() {
        putFault("mdm", Map.of("kind", "error", "rate", 1, "expiresInSeconds", 60));
        assertThat(getCountries().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);

        ResponseEntity<Void> cleared =
            restTemplate.exchange("/faults/mdm", HttpMethod.DELETE, null, Void.class);

        assertThat(cleared.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(getCountries().getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void health_shouldStillAnswer_whileAnMdmFaultIsOn() {
        putFault("mdm", Map.of("kind", "error", "rate", 1, "expiresInSeconds", 60));

        ResponseEntity<String> health = restTemplate.getForEntity("/health", String.class);

        assertThat(health.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private ResponseEntity<String> getCountries() {
        return getFrom(COUNTRIES);
    }

    private ResponseEntity<String> getFrom(String path) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Ocp-Apim-Subscription-Key", "stub-key");
        return restTemplate.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    private void putFault(String integration, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = restTemplate.exchange(
            "/faults/" + integration, HttpMethod.PUT, new HttpEntity<>(body, headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private JsonNode integration(String name) {
        JsonNode body = restTemplate.getForObject("/faults", JsonNode.class);
        assertThat(body.get("stub").asText()).isEqualTo("trade-imports-stub");
        for (JsonNode integration : body.get("integrations")) {
            if (name.equals(integration.get("integration").asText())) {
                return integration;
            }
        }
        throw new AssertionError("No integration named " + name);
    }
}
