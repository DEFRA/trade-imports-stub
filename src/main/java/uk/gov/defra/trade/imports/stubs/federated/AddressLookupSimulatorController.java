package uk.gov.defra.trade.imports.stubs.federated;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.text.ParseException;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Simulates {@code GET /addresses} on the DEFRA address lookup gateway (plan Step 6). Checks the
 * bearer token the way a real resource server would — signature, audience, expiry — before
 * serving the reserved-postcode fixtures (plan "Simulator fixtures and triggers"). Only the
 * fixtures iteration 1 needs are wired here; {@code find} and the truncation/find fixtures are
 * iteration 2.
 */
@RestController
@Slf4j
class AddressLookupSimulatorController {

    private final SimulatorSigningKeys keys;
    private final String expectedAudience;

    AddressLookupSimulatorController(
        SimulatorSigningKeys keys,
        @Value("${address-lookup-simulator.expected-audience}") String expectedAudience) {
        this.keys = keys;
        this.expectedAudience = expectedAudience;
    }

    @GetMapping("/simulator/address-lookup/v2.1/addresses")
    ResponseEntity<String> addresses(
        @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
        @RequestParam(required = false) String postcode,
        @RequestParam(required = false) String find) {
        ResponseEntity<String> rejection = validateBearer(authorization);
        if (rejection != null) {
            return rejection;
        }

        // Deliberately identical for both parameters. What find really matches — a postcode, a
        // building name or number, a street, a town — is a question only the real gateway can
        // answer, and a simulator that guessed would look like an answer. Local runs prove the
        // plumbing carries the parameter through; dev settles the behaviour.
        String term = postcode != null ? postcode : find;
        if (term == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                .body("{ \"statusCode\": 400, \"message\": \"Either postcode or find is required\" }");
        }
        return switch (term) {
            case "SW1A 1AA" -> jsonOk(THREE_ADDRESSES_FIXTURE);
            case "ZZ1 1ZZ" -> ResponseEntity.noContent().build();
            case "QQ1 1QQ" -> ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                .body("{\"statusCode\":400,\"message\":\"Requested postcode is not valid\"}");
            case "XX1 1XX" -> ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).contentType(MediaType.APPLICATION_JSON)
                .body("{\"statusCode\":503,\"message\":\"Service Unavailable\"}");
            case "YY1 1YY" -> ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body("<html><body>Bad Gateway</body></html>");
            case "TT1 1TT" -> delayedOk();
            // Free-text fixtures, so the find half of the spike page can be demonstrated without
            // deploying. These are reserved terms like the postcodes above, NOT a model of what
            // find matches — that is the question the spike exists to answer, and only the real
            // gateway can. A search here returning results says nothing about whether the same
            // search would return results in dev.
            case "Buckingham Palace", "Downing Street" -> jsonOk(THREE_ADDRESSES_FIXTURE);
            default -> jsonOk(NO_RESULTS_FIXTURE);
        };
    }

    private ResponseEntity<String> delayedOk() {
        try {
            Thread.sleep(15_000);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        return jsonOk(THREE_ADDRESSES_FIXTURE);
    }

    private ResponseEntity<String> jsonOk(String body) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(body);
    }

    private ResponseEntity<String> validateBearer(String authorization) {
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return unauthorized();
        }
        String token = authorization.substring(7).trim();

        SignedJWT jwt;
        try {
            jwt = SignedJWT.parse(token);
        } catch (ParseException ex) {
            return unauthorized();
        }

        try {
            if (!jwt.verify(new RSASSAVerifier(keys.entraKey().toRSAPublicKey()))) {
                return unauthorized();
            }
        } catch (JOSEException ex) {
            return unauthorized();
        }

        JWTClaimsSet claims;
        try {
            claims = jwt.getJWTClaimsSet();
        } catch (ParseException ex) {
            return unauthorized();
        }

        if (claims.getAudience() == null || !claims.getAudience().contains(expectedAudience)) {
            log.warn("Address lookup simulator rejected a bearer with audience {}", claims.getAudience());
            return unauthorized();
        }
        if (claims.getExpirationTime() == null || claims.getExpirationTime().before(new Date())) {
            return unauthorized();
        }
        return null;
    }

    /**
     * Word for word what the real gateway's {@code validate-jwt} policy returned in dev, taken from
     * the logged body of the 401s on 2026-09-17 — the response a developer will compare against, so
     * the simulator answers with the same one, {@code statusCode} field included. The gateway sends
     * no {@code WWW-Authenticate} header with it, and neither does this.
     */
    private ResponseEntity<String> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON)
            .body("{ \"statusCode\": 401, \"message\": \"Unauthorized. Access token is missing or invalid.\" }");
    }

    /**
     * Shaped like the real dev response of 2026-09-17, not like the published v2.1 specification:
     * everything upper case, {@code country} free text rather than an ISO code, no county field,
     * {@code match} a string, and counts as strings. The three results are the three ways a name
     * lands, which is what makes composing address line 1 awkward — the first is the Buckingham
     * Palace case dev actually returned, where {@code subBuildingName} carries the whole name and
     * {@code buildingName}, {@code buildingNumber} and {@code street} are all null.
     */
    private static final String THREE_ADDRESSES_FIXTURE = """
        {
          "header": { "totalResults": "3" },
          "results": [
            { "addressLine": "BUCKINGHAM PALACE, LONDON, SW1A 1AA", "subBuildingName": "BUCKINGHAM PALACE", "buildingName": null, "buildingNumber": null, "street": null, "town": "LONDON", "postcode": "SW1A 1AA", "country": "ENGLAND", "uprn": "100023336901", "match": "1", "matchDescription": "EXACT", "language": "EN" },
            { "addressLine": "1 DOWNING STREET, LONDON, SW1A 1AA", "subBuildingName": null, "buildingName": null, "buildingNumber": "1", "street": "DOWNING STREET", "town": "LONDON", "postcode": "SW1A 1AA", "country": "ENGLAND", "uprn": "100023336902", "match": "1", "matchDescription": "EXACT", "language": "EN" },
            { "addressLine": "UNIT 1, DOWNING HOUSE, DOWNING STREET, LONDON, SW1A 1AA", "subBuildingName": "UNIT 1", "buildingName": "DOWNING HOUSE", "buildingNumber": null, "street": "DOWNING STREET", "town": "LONDON", "postcode": "SW1A 1AA", "country": "ENGLAND", "uprn": "100023336903", "match": "1", "matchDescription": "EXACT", "language": "EN" }
          ]
        }
        """;

    private static final String NO_RESULTS_FIXTURE = """
        { "header": { "totalResults": "0" }, "results": [] }
        """;
}
