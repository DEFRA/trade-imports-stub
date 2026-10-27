package uk.gov.defra.trade.imports.stubs.mdm.product;

import static uk.gov.defra.trade.imports.stubs.mdm.MdmStubResponses.OCP_APIM_SUBSCRIPTION_KEY;
import static uk.gov.defra.trade.imports.stubs.mdm.MdmStubResponses.withTraceHeader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.defra.trade.imports.utils.FileUtils;

@Slf4j
@RestController
public class ProductController {

  private static final String SEARCH_FIXTURE = "responses/mdmProductSearchResponses.json";
  private static final String SPECIES_FIXTURE = "responses/mdmProductSpeciesResponses.json";
  private static final String COMMODITIES_FIXTURE =
      "responses/mdmProductCommoditiesResponses.json";

  private final Map<String, JsonNode> searchByTerm = new HashMap<>();
  private final JsonNode speciesByCode;
  private final JsonNode commodityByCode;

  public ProductController(FileUtils fileUtils) {
    fileUtils.getObjectFromFile(SEARCH_FIXTURE, JsonNode.class).properties()
        .forEach((entry) -> searchByTerm.put(normalise(entry.getKey()), entry.getValue()));
    this.speciesByCode = fileUtils.getObjectFromFile(SPECIES_FIXTURE, JsonNode.class);
    this.commodityByCode = fileUtils.getObjectFromFile(COMMODITIES_FIXTURE, JsonNode.class);
  }

  @GetMapping(value = "/mdm/trade/product/commodities")
  ResponseEntity<JsonNode> getCommodities(
      @RequestHeader(OCP_APIM_SUBSCRIPTION_KEY) String ocpApimSubscriptionKey,
      @RequestParam(value = "codes") String commaSeparatedCodes,
      @RequestParam(value = "dataset", required = false) String dataset) {

    ArrayNode body = JsonNodeFactory.instance.arrayNode();
    Arrays.stream(commaSeparatedCodes.split(","))
        .map(String::trim)
        .filter(this::hasSavedCommodity)
        .forEach(code -> body.add(commodityByCode.get(code)));
    return withTraceHeader(ResponseEntity.ok()).body(body);
  }

  private boolean hasSavedCommodity(String code) {
    if (commodityByCode.has(code)) {
      return true;
    }
    log.info("No saved MDM commodity for code '{}'; leaving it out of /commodities", code);
    return false;
  }

  @GetMapping(value = "/mdm/trade/product/search")
  ResponseEntity<JsonNode> search(
      @RequestHeader(OCP_APIM_SUBSCRIPTION_KEY) String ocpApimSubscriptionKey,
      @RequestParam(value = "q", required = false) String query,
      @RequestParam(value = "dataset", required = false) String dataset,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "25") int size) {
    final int minimumQueryLength = 2;

    if (query == null || query.trim().length() < minimumQueryLength) {
      log.warn("Refusing product search: q '{}' is shorter than {} characters", query,
          minimumQueryLength);
      ObjectNode error = JsonNodeFactory.instance.objectNode()
          .put("error", "q must be at least %d characters".formatted(minimumQueryLength));
      return withTraceHeader(ResponseEntity.badRequest()).body(error);
    }

    JsonNode saved = searchByTerm.get(normalise(query));
    JsonNode body;
    if (saved == null) {
      log.info("No saved MDM product search for q '{}'; answering an empty page", query);
      body = emptySearchPage(page, size, 0, 0);
    } else if (page == 0) {
      body = saved;
    } else {
      log.info("No saved MDM product search page {} for q '{}'; answering an empty page", page,
          query);
      body = emptySearchPage(page, size, saved.path("totalElements").asLong(),
          saved.path("totalPages").asInt());
    }
    return withTraceHeader(ResponseEntity.ok()).body(body);
  }

  @GetMapping(value = "/mdm/trade/product/commodities/{code}/species")
  ResponseEntity<JsonNode> getCommoditySpecies(
      @RequestHeader(OCP_APIM_SUBSCRIPTION_KEY) String ocpApimSubscriptionKey,
      @PathVariable("code") String code,
      @RequestParam(value = "dataset", required = false) String dataset,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "25") int size) {

    JsonNode pages = speciesByCode.path(code);
    JsonNode body;
    if (pages.has(page)) {
      body = pages.get(page);
    } else {
      log.info("No saved MDM species page {} for commodity code '{}'; answering an empty page",
          page, code);
      long total = pages.isEmpty() ? 0 : pages.get(0).path("totalElements").asLong();
      body = emptyPage(page, size, total);
    }
    return withTraceHeader(ResponseEntity.ok()).body(body);
  }

  private static ObjectNode emptySearchPage(
      int page, int size, long totalElements, int totalPages) {
    return emptyPage(page, size, totalElements).put("totalPages", totalPages);
  }

  private static ObjectNode emptyPage(int page, int size, long totalElements) {
    ObjectNode body = JsonNodeFactory.instance.objectNode();
    body.putArray("content");
    return body.put("page", page).put("size", size).put("totalElements", totalElements);
  }

  private static String normalise(String term) {
    return term.trim().toLowerCase(Locale.ROOT);
  }
}
