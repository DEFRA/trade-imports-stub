package uk.gov.defra.trade.imports.stubs.federated;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Simulates AWS STS's {@code GetWebIdentityToken} (plan Step 6). Answers only on the dedicated
 * {@code sts-simulator.port} — see {@link StsSimulatorPortConfig} for why. The response shape
 * (form-encoded request, AWS-Query-protocol XML response with the operation result wrapped) is
 * exactly the plan's Step 0 finding, verified against the real AWS SDK marshaller/unmarshaller.
 */
@RestController
@Slf4j
class StsSimulatorController {

    private final SimulatorSigningKeys keys;
    private final int simulatorPort;
    private final String issuer;
    private final String subject;

    StsSimulatorController(
        SimulatorSigningKeys keys,
        @Value("${sts-simulator.port}") int simulatorPort,
        @Value("${sts-simulator.issuer}") String issuer,
        @Value("${sts-simulator.subject}") String subject) {
        this.keys = keys;
        this.simulatorPort = simulatorPort;
        this.issuer = issuer;
        this.subject = subject;
    }

    @PostMapping(value = "/", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    ResponseEntity<String> getWebIdentityToken(
        HttpServletRequest request,
        @RequestParam("Action") String action,
        @RequestParam(value = "Audience.member.1", required = false) String audience,
        @RequestParam(value = "DurationSeconds", required = false, defaultValue = "900") long durationSeconds) {
        if (request.getLocalPort() != simulatorPort) {
            return ResponseEntity.notFound().build();
        }
        if (!"GetWebIdentityToken".equals(action)) {
            log.warn("STS simulator received an unsupported Action: {}", action);
            return ResponseEntity.badRequest().build();
        }

        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(durationSeconds);
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject(subject)
                .audience(audience)
                .issueTime(java.util.Date.from(now))
                .expirationTime(java.util.Date.from(expiry))
                .build();
            SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(keys.stsKey().getKeyID()).build(), claims);
            jwt.sign(new RSASSASigner(keys.stsKey()));

            String xml = """
                <GetWebIdentityTokenResponse xmlns="https://sts.amazonaws.com/doc/2011-06-15/">
                  <GetWebIdentityTokenResult>
                    <WebIdentityToken>%s</WebIdentityToken>
                    <Expiration>%s</Expiration>
                  </GetWebIdentityTokenResult>
                  <ResponseMetadata>
                    <RequestId>%s</RequestId>
                  </ResponseMetadata>
                </GetWebIdentityTokenResponse>
                """.formatted(jwt.serialize(), DateTimeFormatter.ISO_INSTANT.format(expiry), UUID.randomUUID());
            return ResponseEntity.ok().contentType(MediaType.TEXT_XML).body(xml);
        } catch (JOSEException ex) {
            throw new IllegalStateException("Unable to sign the simulated web identity token", ex);
        }
    }

    @GetMapping(value = "/.well-known/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<String> jwks(HttpServletRequest request) {
        if (request.getLocalPort() != simulatorPort) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(keys.stsJwkSet().toString());
    }
}
