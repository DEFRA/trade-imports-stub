package uk.gov.defra.trade.imports.stubs.federated;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class StsSimulatorControllerTest {

    private static final int PORT = 8098;
    private static final String ISSUER = "http://localhost:8098/";
    private static final String SUBJECT = "arn:aws:iam::000000000000:role/trade-imports-ins-backend";

    private final SimulatorSigningKeys keys = new SimulatorSigningKeys();
    private final StsSimulatorController controller = new StsSimulatorController(keys, PORT, ISSUER, SUBJECT);

    @Test
    void getWebIdentityToken_shouldReturnASignedJwt_whenActionIsGetWebIdentityTokenOnTheRightPort() throws Exception {
        HttpServletRequest request = requestOnPort(PORT);

        ResponseEntity<String> response = controller.getWebIdentityToken(
            request, "GetWebIdentityToken", "api://AzureADTokenExchange", 900);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String body = response.getBody();
        assertThat(body).contains("<GetWebIdentityTokenResponse")
            .contains("<WebIdentityToken>")
            .contains("<Expiration>");

        String token = body.substring(
            body.indexOf("<WebIdentityToken>") + "<WebIdentityToken>".length(), body.indexOf("</WebIdentityToken>"));
        SignedJWT jwt = SignedJWT.parse(token);
        assertThat(jwt.verify(new RSASSAVerifier(keys.stsKey().toRSAPublicKey()))).isTrue();
        assertThat(jwt.getJWTClaimsSet().getIssuer()).isEqualTo(ISSUER);
        assertThat(jwt.getJWTClaimsSet().getSubject()).isEqualTo(SUBJECT);
        assertThat(jwt.getJWTClaimsSet().getAudience()).containsExactly("api://AzureADTokenExchange");
    }

    @Test
    void getWebIdentityToken_shouldReturn404_whenHitOnAnotherPort() {
        HttpServletRequest request = requestOnPort(9999);

        ResponseEntity<String> response = controller.getWebIdentityToken(
            request, "GetWebIdentityToken", "api://AzureADTokenExchange", 900);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getWebIdentityToken_shouldReturn400_whenActionIsNotGetWebIdentityToken() {
        HttpServletRequest request = requestOnPort(PORT);

        ResponseEntity<String> response = controller.getWebIdentityToken(
            request, "AssumeRole", "api://AzureADTokenExchange", 900);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void jwks_shouldExposeThePublicStsKey_onTheRightPort() {
        ResponseEntity<String> response = controller.jwks(requestOnPort(PORT));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"keys\"").contains(keys.stsKey().getKeyID());
    }

    @Test
    void jwks_shouldReturn404_whenHitOnAnotherPort() {
        ResponseEntity<String> response = controller.jwks(requestOnPort(9999));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private static HttpServletRequest requestOnPort(int port) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getLocalPort()).thenReturn(port);
        return request;
    }
}
