package uk.gov.defra.trade.imports.stubs.federated;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class AddressLookupSimulatorControllerTest {

    private static final String EXPECTED_AUDIENCE = "api://33333333-3333-3333-3333-333333333333";

    private final SimulatorSigningKeys keys = new SimulatorSigningKeys();
    private final AddressLookupSimulatorController controller =
        new AddressLookupSimulatorController(keys, EXPECTED_AUDIENCE);

    @Test
    void addresses_shouldReturnThreeFixtureAddresses_forTheDefaultPostcode() throws Exception {
        ResponseEntity<String> response = controller.addresses(bearer(EXPECTED_AUDIENCE, Instant.now().plusSeconds(900)), "SW1A 1AA", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"totalResults\": \"3\"").contains("DOWNING STREET");
    }

    @Test
    void addresses_shouldReturnAddressesShapedLikeTheRealApi() throws Exception {
        ResponseEntity<String> response = controller.addresses(bearer(EXPECTED_AUDIENCE, Instant.now().plusSeconds(900)), "SW1A 1AA", null);

        // The divergences from the published specification that dev confirmed on 2026-09-17.
        assertThat(response.getBody())
            .contains("\"subBuildingName\": \"BUCKINGHAM PALACE\"")
            .contains("\"country\": \"ENGLAND\"")
            .contains("\"match\": \"1\"")
            .doesNotContain("county");
    }

    @Test
    void addresses_shouldReject_whenTheBearerAudienceIsTheBareGuidRatherThanTheApiUri() throws Exception {
        // The exact mismatch the spike hit in dev: the scope requested as the bare GUID produces
        // this audience, and the gateway policy wants the api:// form.
        ResponseEntity<String> response = controller.addresses(
            bearer("33333333-3333-3333-3333-333333333333", Instant.now().plusSeconds(900)), "SW1A 1AA", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody())
            .isEqualTo("{ \"statusCode\": 401, \"message\": \"Unauthorized. Access token is missing or invalid.\" }");
        // The real gateway sends none, which is why Spring's own authorization failure handler
        // cannot be used to evict a stale token (plan, iteration 1 gaps).
        assertThat(response.getHeaders().getFirst(HttpHeaders.WWW_AUTHENTICATE)).isNull();
    }

    @Test
    void addresses_shouldReturn204_forTheReservedNoResultsPostcode() throws Exception {
        ResponseEntity<String> response = controller.addresses(bearer(EXPECTED_AUDIENCE, Instant.now().plusSeconds(900)), "ZZ1 1ZZ", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void addresses_shouldReturnARejectedPostcode400_forTheReservedInvalidPostcode() throws Exception {
        ResponseEntity<String> response = controller.addresses(bearer(EXPECTED_AUDIENCE, Instant.now().plusSeconds(900)), "QQ1 1QQ", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("Requested postcode");
    }

    @Test
    void addresses_shouldReturn503_forTheReservedThrottledPostcode() throws Exception {
        ResponseEntity<String> response = controller.addresses(bearer(EXPECTED_AUDIENCE, Instant.now().plusSeconds(900)), "XX1 1XX", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void addresses_shouldReturnNonJsonHtml_forTheReservedProxyErrorPostcode() throws Exception {
        ResponseEntity<String> response = controller.addresses(bearer(EXPECTED_AUDIENCE, Instant.now().plusSeconds(900)), "YY1 1YY", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType().toString()).contains("text/html");
    }

    @Test
    void addresses_shouldReject_whenNoBearerPresent() {
        ResponseEntity<String> response = controller.addresses(null, "SW1A 1AA", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void addresses_shouldReject_whenTheBearerAudienceIsWrong() throws Exception {
        ResponseEntity<String> response = controller.addresses(
            bearer("api://SomethingElse", Instant.now().plusSeconds(900)), "SW1A 1AA", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void addresses_shouldReject_whenTheBearerHasExpired() throws Exception {
        ResponseEntity<String> response = controller.addresses(
            bearer(EXPECTED_AUDIENCE, Instant.now().minusSeconds(60)), "SW1A 1AA", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void addresses_shouldReject_whenTheBearerSignatureIsWrong() throws Exception {
        SimulatorSigningKeys otherKeys = new SimulatorSigningKeys();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
            .audience(EXPECTED_AUDIENCE)
            .issueTime(Date.from(Instant.now()))
            .expirationTime(Date.from(Instant.now().plusSeconds(900)))
            .build();
        SignedJWT jwt = new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(otherKeys.entraKey().getKeyID()).build(), claims);
        jwt.sign(new RSASSASigner(otherKeys.entraKey()));

        ResponseEntity<String> response = controller.addresses("Bearer " + jwt.serialize(), "SW1A 1AA", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private String bearer(String audience, Instant expiry) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
            .audience(audience)
            .issueTime(Date.from(Instant.now()))
            .expirationTime(Date.from(expiry))
            .build();
        SignedJWT jwt = new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(keys.entraKey().getKeyID()).build(), claims);
        jwt.sign(new RSASSASigner(keys.entraKey()));
        return "Bearer " + jwt.serialize();
    }
}
