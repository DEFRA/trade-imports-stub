package uk.gov.defra.trade.imports.stubs.federated;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(EntraSimulatorController.class)
@Import(SimulatorSigningKeys.class)
@TestPropertySource(properties = {
    "sts-simulator.issuer=" + EntraSimulatorControllerTest.ISSUER,
    "sts-simulator.subject=" + EntraSimulatorControllerTest.SUBJECT
})
class EntraSimulatorControllerTest {

    static final String ISSUER = "http://localhost:8098/";
    static final String SUBJECT = "arn:aws:iam::000000000000:role/trade-imports-ins-backend";
    private static final String TENANT = "11111111-1111-1111-1111-111111111111";
    private static final String TOKEN_PATH = "/simulator/entra/{tenant}/oauth2/v2.0/token";
    private static final String JWKS_PATH = "/simulator/entra/.well-known/jwks.json";
    private static final String AUDIENCE = "api://AzureADTokenExchange";
    private static final String CLIENT_ASSERTION_TYPE = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";
    private static final String SCOPE = "api://33333333-3333-3333-3333-333333333333/.default";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SimulatorSigningKeys keys;

    @Test
    void token_shouldIssueAnAccessToken_forAValidAssertion() throws Exception {
        String assertion = assertion(keys.stsKey(), ISSUER, SUBJECT, AUDIENCE, Instant.now().plusSeconds(900));

        String body = mockMvc.perform(tokenRequest("client_credentials", SCOPE, assertion))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.token_type").value("Bearer"))
            .andReturn().getResponse().getContentAsString();

        SignedJWT jwt = SignedJWT.parse(JsonPath.<String>read(body, "$.access_token"));
        assertThat(jwt.verify(new RSASSAVerifier(keys.entraKey().toRSAPublicKey()))).isTrue();
        assertThat(jwt.getJWTClaimsSet().getAudience()).containsExactly("api://33333333-3333-3333-3333-333333333333");
    }

    @Test
    void token_shouldIssueAV1FormatToken_matchingDev() throws Exception {
        String assertion = assertion(keys.stsKey(), ISSUER, SUBJECT, AUDIENCE, Instant.now().plusSeconds(900));

        String body = mockMvc.perform(tokenRequest("client_credentials", SCOPE, assertion))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        JWTClaimsSet claims = SignedJWT.parse(JsonPath.<String>read(body, "$.access_token")).getJWTClaimsSet();
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
        String body = mockMvc.perform(tokenRequest(
                "client_credentials", "33333333-3333-3333-3333-333333333333/.default", assertion))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        JWTClaimsSet claims = SignedJWT.parse(JsonPath.<String>read(body, "$.access_token")).getJWTClaimsSet();
        assertThat(claims.getAudience()).containsExactly("33333333-3333-3333-3333-333333333333");
    }

    @Test
    void token_shouldRejectAWrongGrantType() throws Exception {
        mockMvc.perform(tokenRequest("authorization_code", SCOPE, "irrelevant"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("unsupported_grant_type"));
    }

    @Test
    void token_shouldRejectAMissingAssertion() throws Exception {
        mockMvc.perform(post(TOKEN_PATH, TENANT)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "client_credentials")
                .param("client_id", "client-id")
                .param("scope", SCOPE)
                .param("client_assertion_type", CLIENT_ASSERTION_TYPE))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("invalid_client"));
    }

    @Test
    void token_shouldRejectAWrongSignature() throws Exception {
        RSAKey rogueKey = new RSAKeyGenerator(2048).keyID("rogue").generate();
        String assertion = assertion(rogueKey, ISSUER, SUBJECT, AUDIENCE, Instant.now().plusSeconds(900));

        mockMvc.perform(tokenRequest("client_credentials", SCOPE, assertion))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("invalid_client"));
    }

    @Test
    void token_shouldRejectAWrongIssuer() throws Exception {
        String assertion = assertion(keys.stsKey(), "https://not-our-sts/", SUBJECT, AUDIENCE, Instant.now().plusSeconds(900));

        mockMvc.perform(tokenRequest("client_credentials", SCOPE, assertion))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("invalid_client"));
    }

    @Test
    void token_shouldRejectAWrongSubject() throws Exception {
        String assertion = assertion(keys.stsKey(), ISSUER, "arn:aws:iam::000000000000:role/someone-else", AUDIENCE, Instant.now().plusSeconds(900));

        mockMvc.perform(tokenRequest("client_credentials", SCOPE, assertion))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("invalid_client"));
    }

    @Test
    void token_shouldRejectAWrongAudience_withAnAadsts500011StyleMessage() throws Exception {
        String assertion = assertion(keys.stsKey(), ISSUER, SUBJECT, "api://SomethingElse", Instant.now().plusSeconds(900));

        mockMvc.perform(tokenRequest("client_credentials", SCOPE, assertion))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error_description").value(containsString("AADSTS500011")));
    }

    @Test
    void token_shouldRejectAnExpiredAssertion() throws Exception {
        String assertion = assertion(keys.stsKey(), ISSUER, SUBJECT, AUDIENCE, Instant.now().minusSeconds(60));

        mockMvc.perform(tokenRequest("client_credentials", SCOPE, assertion))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("invalid_client"));
    }

    @Test
    void jwks_shouldExposeThePublicEntraKey() throws Exception {
        mockMvc.perform(get(JWKS_PATH))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.keys[0].kid").value(keys.entraKey().getKeyID()));
    }

    private static MockHttpServletRequestBuilder tokenRequest(String grantType, String scope, String assertion) {
        return post(TOKEN_PATH, TENANT)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .param("grant_type", grantType)
            .param("client_id", "client-id")
            .param("scope", scope)
            .param("client_assertion_type", CLIENT_ASSERTION_TYPE)
            .param("client_assertion", assertion);
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
