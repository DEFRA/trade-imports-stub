package uk.gov.defra.trade.imports.stubs.federated;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class EntraSimulatorControllerTest {

    private static final String TENANT = "11111111-1111-1111-1111-111111111111";
    private static final String ISSUER = "http://localhost:8098/";
    private static final String SUBJECT = "arn:aws:iam::000000000000:role/trade-imports-ins-backend";
    private static final String AUDIENCE = "api://AzureADTokenExchange";
    private static final String CLIENT_ASSERTION_TYPE = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";

    private final SimulatorSigningKeys keys = new SimulatorSigningKeys();
    private final EntraSimulatorController controller = new EntraSimulatorController(keys, ISSUER, SUBJECT);

    @Test
    void token_shouldIssueAnAccessToken_forAValidAssertion() throws Exception {
        String assertion = assertion(keys.stsKey(), ISSUER, SUBJECT, AUDIENCE, Instant.now().plusSeconds(900));

        ResponseEntity<Map<String, Object>> response = controller.token(
            TENANT, "client_credentials", "client-id",
            "api://33333333-3333-3333-3333-333333333333/.default", CLIENT_ASSERTION_TYPE, assertion);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("token_type", "Bearer");
        String accessToken = (String) response.getBody().get("access_token");
        SignedJWT jwt = SignedJWT.parse(accessToken);
        assertThat(jwt.verify(new RSASSAVerifier(keys.entraKey().toRSAPublicKey()))).isTrue();
        assertThat(jwt.getJWTClaimsSet().getAudience()).containsExactly("api://33333333-3333-3333-3333-333333333333");
    }

    @Test
    void token_shouldIssueAV1FormatToken_matchingDev() throws Exception {
        String assertion = assertion(keys.stsKey(), ISSUER, SUBJECT, AUDIENCE, Instant.now().plusSeconds(900));

        ResponseEntity<Map<String, Object>> response = controller.token(
            TENANT, "client_credentials", "client-id",
            "api://33333333-3333-3333-3333-333333333333/.default", CLIENT_ASSERTION_TYPE, assertion);

        JWTClaimsSet claims = SignedJWT.parse((String) response.getBody().get("access_token")).getJWTClaimsSet();
        assertThat(claims.getIssuer()).isEqualTo("https://sts.windows.net/" + TENANT + "/");
        assertThat(claims.getStringClaim("appid")).isEqualTo("client-id");
        assertThat(claims.getClaim("roles")).isEqualTo(List.of());
    }

    @Test
    void token_shouldSetTheAudienceToTheScopeVerbatim_notNormaliseTheApiPrefixAway() throws Exception {
        String assertion = assertion(keys.stsKey(), ISSUER, SUBJECT, AUDIENCE, Instant.now().plusSeconds(900));

        // Real Entra does not normalise: the bare-GUID scope the Azure team's setup instructions
        // give produces a bare-GUID audience, which the gateway policy then rejects. Accepting
        // both forms here would hide that.
        ResponseEntity<Map<String, Object>> response = controller.token(
            TENANT, "client_credentials", "client-id",
            "33333333-3333-3333-3333-333333333333/.default", CLIENT_ASSERTION_TYPE, assertion);

        JWTClaimsSet claims = SignedJWT.parse((String) response.getBody().get("access_token")).getJWTClaimsSet();
        assertThat(claims.getAudience()).containsExactly("33333333-3333-3333-3333-333333333333");
    }

    @Test
    void token_shouldRejectAWrongGrantType() {
        ResponseEntity<Map<String, Object>> response = controller.token(
            "tenant", "authorization_code", "client-id", "scope", CLIENT_ASSERTION_TYPE, "irrelevant");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "unsupported_grant_type");
    }

    @Test
    void token_shouldRejectAMissingAssertion() {
        ResponseEntity<Map<String, Object>> response = controller.token(
            "tenant", "client_credentials", "client-id", "scope", CLIENT_ASSERTION_TYPE, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "invalid_client");
    }

    @Test
    void token_shouldRejectAWrongSignature() throws Exception {
        RSAKey rogueKey = new RSAKeyGenerator(2048).keyID("rogue").generate();
        String assertion = assertion(rogueKey, ISSUER, SUBJECT, AUDIENCE, Instant.now().plusSeconds(900));

        ResponseEntity<Map<String, Object>> response = controller.token(
            "tenant", "client_credentials", "client-id", "scope", CLIENT_ASSERTION_TYPE, assertion);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "invalid_client");
    }

    @Test
    void token_shouldRejectAWrongIssuer() throws Exception {
        String assertion = assertion(keys.stsKey(), "https://not-our-sts/", SUBJECT, AUDIENCE, Instant.now().plusSeconds(900));

        ResponseEntity<Map<String, Object>> response = controller.token(
            "tenant", "client_credentials", "client-id", "scope", CLIENT_ASSERTION_TYPE, assertion);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "invalid_client");
    }

    @Test
    void token_shouldRejectAWrongSubject() throws Exception {
        String assertion = assertion(keys.stsKey(), ISSUER, "arn:aws:iam::000000000000:role/someone-else", AUDIENCE, Instant.now().plusSeconds(900));

        ResponseEntity<Map<String, Object>> response = controller.token(
            "tenant", "client_credentials", "client-id", "scope", CLIENT_ASSERTION_TYPE, assertion);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "invalid_client");
    }

    @Test
    void token_shouldRejectAWrongAudience_withAnAadsts500011StyleMessage() throws Exception {
        String assertion = assertion(keys.stsKey(), ISSUER, SUBJECT, "api://SomethingElse", Instant.now().plusSeconds(900));

        ResponseEntity<Map<String, Object>> response = controller.token(
            "tenant", "client_credentials", "client-id", "scope", CLIENT_ASSERTION_TYPE, assertion);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("error_description").toString()).contains("AADSTS500011");
    }

    @Test
    void token_shouldRejectAnExpiredAssertion() throws Exception {
        String assertion = assertion(keys.stsKey(), ISSUER, SUBJECT, AUDIENCE, Instant.now().minusSeconds(60));

        ResponseEntity<Map<String, Object>> response = controller.token(
            "tenant", "client_credentials", "client-id", "scope", CLIENT_ASSERTION_TYPE, assertion);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "invalid_client");
    }

    @Test
    void jwks_shouldExposeThePublicEntraKey() {
        ResponseEntity<String> response = controller.jwks();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"keys\"").contains(keys.entraKey().getKeyID());
    }

    private static String assertion(RSAKey signingKey, String issuer, String subject, String audience, Instant expiry) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
            .issuer(issuer)
            .subject(subject)
            .audience(audience)
            .issueTime(Date.from(Instant.now()))
            .expirationTime(Date.from(expiry))
            .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(signingKey.getKeyID()).build(), claims);
        jwt.sign(new RSASSASigner(signingKey));
        return jwt.serialize();
    }
}
