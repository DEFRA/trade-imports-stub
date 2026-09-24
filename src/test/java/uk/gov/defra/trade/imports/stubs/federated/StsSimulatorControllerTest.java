package uk.gov.defra.trade.imports.stubs.federated;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(StsSimulatorController.class)
@Import(SimulatorSigningKeys.class)
@TestPropertySource(properties = {
    "sts-simulator.port=" + StsSimulatorControllerTest.PORT,
    "sts-simulator.issuer=" + StsSimulatorControllerTest.ISSUER,
    "sts-simulator.subject=" + StsSimulatorControllerTest.SUBJECT
})
class StsSimulatorControllerTest {

    static final int PORT = 8098;
    static final String ISSUER = "http://localhost:8098/";
    static final String SUBJECT = "arn:aws:iam::000000000000:role/trade-imports-ins-backend";
    private static final int OTHER_PORT = 9999;
    private static final String TOKEN_PATH = "/";
    private static final String JWKS_PATH = "/.well-known/jwks.json";
    private static final String AUDIENCE = "api://AzureADTokenExchange";
    private static final long DURATION_SECONDS = 900;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SimulatorSigningKeys keys;

    @Test
    void getWebIdentityToken_shouldReturnASignedJwt_whenActionIsGetWebIdentityTokenOnTheRightPort() throws Exception {
        String body = mockMvc.perform(tokenRequest(PORT, "GetWebIdentityToken"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_XML))
            .andReturn().getResponse().getContentAsString();

        assertThat(body).contains("<GetWebIdentityTokenResponse")
            .contains("<WebIdentityToken>")
            .contains("<Expiration>");

        String token = body.substring(
            body.indexOf("<WebIdentityToken>") + "<WebIdentityToken>".length(), body.indexOf("</WebIdentityToken>"));
        SignedJWT jwt = SignedJWT.parse(token);
        assertThat(jwt.verify(new RSASSAVerifier(keys.stsKey().toRSAPublicKey()))).isTrue();
        JWTClaimsSet claims = jwt.getJWTClaimsSet();
        assertThat(claims.getIssuer()).isEqualTo(ISSUER);
        assertThat(claims.getSubject()).isEqualTo(SUBJECT);
        assertThat(claims.getAudience()).containsExactly(AUDIENCE);
        assertThat(Duration.between(claims.getIssueTime().toInstant(), claims.getExpirationTime().toInstant()))
            .isEqualTo(Duration.ofSeconds(DURATION_SECONDS));
    }

    @Test
    void getWebIdentityToken_shouldReturn404_whenHitOnAnotherPort() throws Exception {
        mockMvc.perform(tokenRequest(OTHER_PORT, "GetWebIdentityToken"))
            .andExpect(status().isNotFound());
    }

    @Test
    void getWebIdentityToken_shouldReturn400_whenActionIsNotGetWebIdentityToken() throws Exception {
        mockMvc.perform(tokenRequest(PORT, "AssumeRole"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void jwks_shouldExposeThePublicStsKey_onTheRightPort() throws Exception {
        mockMvc.perform(get(JWKS_PATH).with(onPort(PORT)))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.keys[0].kid").value(keys.stsKey().getKeyID()));
    }

    @Test
    void jwks_shouldReturn404_whenHitOnAnotherPort() throws Exception {
        mockMvc.perform(get(JWKS_PATH).with(onPort(OTHER_PORT)))
            .andExpect(status().isNotFound());
    }

    private static MockHttpServletRequestBuilder tokenRequest(int port, String action) {
        return post(TOKEN_PATH)
            .with(onPort(port))
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .param("Action", action)
            .param("Audience.member.1", AUDIENCE)
            .param("DurationSeconds", String.valueOf(DURATION_SECONDS));
    }

    private static RequestPostProcessor onPort(int port) {
        return request -> {
            request.setLocalPort(port);
            return request;
        };
    }
}
