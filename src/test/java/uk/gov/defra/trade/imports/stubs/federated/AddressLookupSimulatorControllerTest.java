package uk.gov.defra.trade.imports.stubs.federated;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AddressLookupSimulatorController.class)
@Import(SimulatorSigningKeys.class)
@TestPropertySource(properties = "address-lookup-simulator.expected-audience="
    + AddressLookupSimulatorControllerTest.EXPECTED_AUDIENCE)
class AddressLookupSimulatorControllerTest {

    static final String EXPECTED_AUDIENCE = "api://33333333-3333-3333-3333-333333333333";
    private static final String ADDRESSES_PATH = "/simulator/address-lookup/v2.1/addresses";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SimulatorSigningKeys keys;

    @Test
    void addresses_shouldReturnThreeFixtureAddresses_forTheDefaultPostcode() throws Exception {
        mockMvc.perform(get(ADDRESSES_PATH)
                .header(HttpHeaders.AUTHORIZATION, bearer(EXPECTED_AUDIENCE, Instant.now().plusSeconds(900)))
                .param("postcode", "SW1A 1AA"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(content().string(containsString("\"totalResults\": \"3\"")))
            .andExpect(content().string(containsString("DOWNING STREET")));
    }

    @Test
    void addresses_shouldReturnAddressesShapedLikeTheRealApi() throws Exception {
        // The divergences from the published specification that dev confirmed on 2026-09-17.
        mockMvc.perform(get(ADDRESSES_PATH)
                .header(HttpHeaders.AUTHORIZATION, bearer(EXPECTED_AUDIENCE, Instant.now().plusSeconds(900)))
                .param("postcode", "SW1A 1AA"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("\"subBuildingName\": \"BUCKINGHAM PALACE\"")))
            .andExpect(content().string(containsString("\"country\": \"ENGLAND\"")))
            .andExpect(content().string(containsString("\"match\": \"1\"")))
            .andExpect(content().string(not(containsString("county"))));
    }

    @Test
    void addresses_shouldReject_whenTheBearerAudienceIsTheBareGuidRatherThanTheApiUri() throws Exception {
        // The exact mismatch the spike hit in dev: the scope requested as the bare GUID produces
        // this audience, and the gateway policy wants the api:// form.
        mockMvc.perform(get(ADDRESSES_PATH)
                .header(HttpHeaders.AUTHORIZATION,
                    bearer("33333333-3333-3333-3333-333333333333", Instant.now().plusSeconds(900)))
                .param("postcode", "SW1A 1AA"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().string(
                "{ \"statusCode\": 401, \"message\": \"Unauthorized. Access token is missing or invalid.\" }"))
            // The real gateway sends none, which is why Spring's own authorization failure handler
            // cannot be used to evict a stale token (plan, iteration 1 gaps).
            .andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE));
    }

    @Test
    void addresses_shouldServeTheReservedFreeTextTerms_soFindCanBeDemonstrated() throws Exception {
        mockMvc.perform(get(ADDRESSES_PATH)
                .header(HttpHeaders.AUTHORIZATION, bearer(EXPECTED_AUDIENCE, Instant.now().plusSeconds(900)))
                .param("find", "Buckingham Palace"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("BUCKINGHAM PALACE")));
    }

    @Test
    void addresses_shouldReturnNoResults_forFreeTextThatIsNotReserved() throws Exception {
        // The reserved terms are fixtures, not a model of what find matches.
        mockMvc.perform(get(ADDRESSES_PATH)
                .header(HttpHeaders.AUTHORIZATION, bearer(EXPECTED_AUDIENCE, Instant.now().plusSeconds(900)))
                .param("find", "Buckingham"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("\"totalResults\": \"0\"")));
    }

    @Test
    void addresses_shouldReturn400_whenNeitherPostcodeNorFindIsGiven() throws Exception {
        mockMvc.perform(get(ADDRESSES_PATH)
                .header(HttpHeaders.AUTHORIZATION, bearer(EXPECTED_AUDIENCE, Instant.now().plusSeconds(900))))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Either postcode or find is required")));
    }

    @Test
    void addresses_shouldReturn204_forTheReservedNoResultsPostcode() throws Exception {
        mockMvc.perform(get(ADDRESSES_PATH)
                .header(HttpHeaders.AUTHORIZATION, bearer(EXPECTED_AUDIENCE, Instant.now().plusSeconds(900)))
                .param("postcode", "ZZ1 1ZZ"))
            .andExpect(status().isNoContent());
    }

    @Test
    void addresses_shouldReturnARejectedPostcode400_forTheReservedInvalidPostcode() throws Exception {
        mockMvc.perform(get(ADDRESSES_PATH)
                .header(HttpHeaders.AUTHORIZATION, bearer(EXPECTED_AUDIENCE, Instant.now().plusSeconds(900)))
                .param("postcode", "QQ1 1QQ"))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Requested postcode")));
    }

    @Test
    void addresses_shouldReturn503_forTheReservedThrottledPostcode() throws Exception {
        mockMvc.perform(get(ADDRESSES_PATH)
                .header(HttpHeaders.AUTHORIZATION, bearer(EXPECTED_AUDIENCE, Instant.now().plusSeconds(900)))
                .param("postcode", "XX1 1XX"))
            .andExpect(status().isServiceUnavailable());
    }

    @Test
    void addresses_shouldReturnNonJsonHtml_forTheReservedProxyErrorPostcode() throws Exception {
        mockMvc.perform(get(ADDRESSES_PATH)
                .header(HttpHeaders.AUTHORIZATION, bearer(EXPECTED_AUDIENCE, Instant.now().plusSeconds(900)))
                .param("postcode", "YY1 1YY"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
    }

    @Test
    void addresses_shouldReject_whenNoBearerPresent() throws Exception {
        mockMvc.perform(get(ADDRESSES_PATH)
                .param("postcode", "SW1A 1AA"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void addresses_shouldReject_whenTheBearerAudienceIsWrong() throws Exception {
        mockMvc.perform(get(ADDRESSES_PATH)
                .header(HttpHeaders.AUTHORIZATION, bearer("api://SomethingElse", Instant.now().plusSeconds(900)))
                .param("postcode", "SW1A 1AA"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void addresses_shouldReject_whenTheBearerHasExpired() throws Exception {
        mockMvc.perform(get(ADDRESSES_PATH)
                .header(HttpHeaders.AUTHORIZATION, bearer(EXPECTED_AUDIENCE, Instant.now().minusSeconds(60)))
                .param("postcode", "SW1A 1AA"))
            .andExpect(status().isUnauthorized());
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

        mockMvc.perform(get(ADDRESSES_PATH)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt.serialize())
                .param("postcode", "SW1A 1AA"))
            .andExpect(status().isUnauthorized());
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
