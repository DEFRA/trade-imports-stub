package uk.gov.defra.trade.imports.stubs.mdm.countries;

import static uk.gov.defra.trade.imports.stubs.mdm.MdmStubResponses.OCP_APIM_SUBSCRIPTION_KEY;
import static uk.gov.defra.trade.imports.stubs.mdm.MdmStubResponses.withTraceHeader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.stream.StreamSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.defra.trade.imports.utils.FileUtils;

@Slf4j
@RestController
public class CountriesController {
  private static final String COUNTRIES_FIXTURE = "responses/countriesResponse.json";

  private final JsonNode countries;

  public CountriesController(FileUtils fileUtils) {
    this.countries = fileUtils.getObjectFromFile(COUNTRIES_FIXTURE, JsonNode.class);
  }

  @GetMapping(value = "/mdm/geo/countries")
  ResponseEntity<JsonNode> getCountries(
      @RequestHeader(OCP_APIM_SUBSCRIPTION_KEY) String ocpApimSubscriptionKey,
      @RequestParam(value = "system", required = false) String system,
      @RequestParam(value = "blocks", required = false) String blocks) {

    JsonNode body = blocks == null || blocks.isBlank() ? countries : inBlock(countries, blocks);

    return withTraceHeader(ResponseEntity.ok()).body(body);
  }

  private static JsonNode inBlock(JsonNode allCountries, String block) {
    ArrayNode kept = JsonNodeFactory.instance.arrayNode();
    StreamSupport.stream(allCountries.spliterator(), false)
        .filter((country) -> carriesBlock(country, block))
        .forEach(kept::add);
    return kept;
  }

  private static boolean carriesBlock(JsonNode country, String block) {
    return StreamSupport.stream(country.path("blocks").spliterator(), false)
        .anyMatch((entry) -> block.equals(entry.path("name").asText()));
  }
}
