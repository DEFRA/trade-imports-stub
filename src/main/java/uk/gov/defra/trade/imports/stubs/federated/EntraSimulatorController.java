package uk.gov.defra.trade.imports.stubs.federated;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Simulates the Entra {@code client_credentials} token endpoint, authenticating the caller via
 * {@code client_assertion} rather than a secret (plan Step 6). Validates the STS-issued
 * assertion's signature (against {@link SimulatorSigningKeys}, standing in for the STS JWKS),
 * issuer, subject, audience and expiry, and rejects the way Azure does — an
 * {@code {error, error_description}} body with an AADSTS-shaped message — so a wrong audience,
 * scope or expiry is a real negative test rather than a stub that only ever returns data.
 */
@RestController
@Slf4j
class EntraSimulatorController {

    private static final String CLIENT_ASSERTION_TYPE = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";

    private final SimulatorSigningKeys keys;
    private final String expectedIssuer;
    private final String expectedSubject;

    EntraSimulatorController(
        SimulatorSigningKeys keys,
        @Value("${sts-simulator.issuer}") String expectedIssuer,
        @Value("${sts-simulator.subject}") String expectedSubject) {
        this.keys = keys;
        this.expectedIssuer = expectedIssuer;
        this.expectedSubject = expectedSubject;
    }

    @PostMapping(value = "/simulator/entra/{tenant}/oauth2/v2.0/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    ResponseEntity<Map<String, Object>> token(
        @PathVariable String tenant,
        @RequestParam("grant_type") String grantType,
        @RequestParam("client_id") String clientId,
        @RequestParam(value = "scope", required = false) String scope,
        @RequestParam(value = "client_assertion_type", required = false) String clientAssertionType,
        @RequestParam(value = "client_assertion", required = false) String clientAssertion) {

        if (!"client_credentials".equals(grantType)) {
            return azureError(HttpStatus.BAD_REQUEST, "unsupported_grant_type",
                "AADSTS9002327: Tokens issued for this application require this grant to be client_credentials.");
        }
        if (clientAssertion == null || !CLIENT_ASSERTION_TYPE.equals(clientAssertionType)) {
            return azureError(HttpStatus.BAD_REQUEST, "invalid_client",
                "AADSTS7000215: Invalid client secret or assertion provided.");
        }

        SignedJWT assertion;
        try {
            assertion = SignedJWT.parse(clientAssertion);
        } catch (ParseException ex) {
            return azureError(HttpStatus.BAD_REQUEST, "invalid_client",
                "AADSTS7000215: The client assertion is not a well-formed JWT.");
        }

        ResponseEntity<Map<String, Object>> rejection = verifyAssertion(assertion);
        if (rejection != null) {
            return rejection;
        }

        try {
            Instant now = Instant.now();
            Instant expiry = now.plusSeconds(3600);
            // v1 token format, matching dev: the address lookup API's registration leaves
            // requestedAccessTokenVersion null, so even though the request goes to the v2.0 token
            // endpoint the token comes back with the v1 sts.windows.net issuer and an appid claim
            // rather than azp. An empty roles claim, again as in dev — the gateway policy checks
            // aud and iss only, and requires no role.
            JWTClaimsSet accessTokenClaims = new JWTClaimsSet.Builder()
                .issuer("https://sts.windows.net/" + tenant + "/")
                .audience(resourceAudience(scope))
                .subject(clientId)
                .claim("appid", clientId)
                .claim("roles", List.of())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(expiry))
                .build();
            SignedJWT accessToken = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(keys.entraKey().getKeyID()).build(), accessTokenClaims);
            accessToken.sign(new RSASSASigner(keys.entraKey()));

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("token_type", "Bearer");
            body.put("expires_in", 3600);
            body.put("ext_expires_in", 3600);
            body.put("access_token", accessToken.serialize());
            return ResponseEntity.ok(body);
        } catch (JOSEException ex) {
            throw new IllegalStateException("Unable to sign the simulated Entra access token", ex);
        }
    }

    private ResponseEntity<Map<String, Object>> verifyAssertion(SignedJWT assertion) {
        try {
            if (!assertion.verify(new RSASSAVerifier(keys.stsKey().toRSAPublicKey()))) {
                return azureError(HttpStatus.BAD_REQUEST, "invalid_client",
                    "AADSTS7000215: Invalid client assertion signature.");
            }
        } catch (JOSEException ex) {
            return azureError(HttpStatus.BAD_REQUEST, "invalid_client",
                "AADSTS7000215: Unable to verify the client assertion signature.");
        }

        JWTClaimsSet claims;
        try {
            claims = assertion.getJWTClaimsSet();
        } catch (ParseException ex) {
            return azureError(HttpStatus.BAD_REQUEST, "invalid_client",
                "AADSTS7000215: The client assertion claims could not be parsed.");
        }

        if (!expectedIssuer.equals(claims.getIssuer())) {
            log.warn("Entra simulator rejected an assertion with issuer {}", claims.getIssuer());
            return azureError(HttpStatus.BAD_REQUEST, "invalid_client",
                "AADSTS7000215: No matching federated identity record found for presented issuer.");
        }
        if (claims.getSubject() == null || !expectedSubject.equals(claims.getSubject())) {
            log.warn("Entra simulator rejected an assertion with subject {}", claims.getSubject());
            return azureError(HttpStatus.BAD_REQUEST, "invalid_client",
                "AADSTS7000215: No matching federated identity record found for presented subject.");
        }
        if (claims.getAudience() == null || !claims.getAudience().contains("api://AzureADTokenExchange")) {
            log.warn("Entra simulator rejected an assertion with audience {}", claims.getAudience());
            return azureError(HttpStatus.BAD_REQUEST, "invalid_client",
                "AADSTS500011: The resource principal named api://AzureADTokenExchange was not found in the tenant.");
        }
        if (claims.getExpirationTime() == null || claims.getExpirationTime().before(new Date())) {
            return azureError(HttpStatus.BAD_REQUEST, "invalid_client",
                "AADSTS7000215: The provided client assertion has expired.");
        }
        return null;
    }

    @GetMapping(value = "/simulator/entra/.well-known/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<String> jwks() {
        return ResponseEntity.ok(keys.entraJwkSet().toString());
    }

    private ResponseEntity<Map<String, Object>> azureError(HttpStatus status, String error, String description) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", error);
        body.put("error_description", description);
        return ResponseEntity.status(status).body(body);
    }

    /**
     * The audience follows the scope <em>verbatim</em>, minus {@code /.default} — asking for
     * {@code <guid>/.default} yields {@code aud = <guid>}, and asking for
     * {@code api://<guid>/.default} yields {@code aud = api://<guid>}. Confirmed against real
     * Entra in CDP dev on 2026-09-17, and it is the whole reason the spike spent a day on a 401:
     * the gateway's validate-jwt policy wants the {@code api://} form, while the Azure team's
     * setup instructions say to request the bare GUID. A simulator that normalised the two forms
     * together would let that mismatch through.
     */
    private static String resourceAudience(String scope) {
        if (scope == null) {
            return null;
        }
        return scope.endsWith("/.default") ? scope.substring(0, scope.length() - "/.default".length()) : scope;
    }
}
