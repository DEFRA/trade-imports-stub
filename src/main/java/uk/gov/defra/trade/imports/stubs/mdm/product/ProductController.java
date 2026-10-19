package uk.gov.defra.trade.imports.stubs.mdm.product;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

/**
 * Answers MDM's product search and commodity species from responses saved from TST. It doesn't
 * search like MDM: a term that wasn't saved finds nothing, even where TST would find something.
 */
@Slf4j
@RestController
public class ProductController {

  private static final String OCP_APIM_SUBSCRIPTION_KEY = "Ocp-Apim-Subscription-Key";
  private static final String SEARCH_FIXTURE = "responses/mdmProductSearchResponses.json";
  private static final String SPECIES_FIXTURE = "responses/mdmProductSpeciesResponses.json";
  private static final String TRACE_HEADER = "x-ms-middleware-request-id";
  private static final int MINIMUM_QUERY_LENGTH = 2;

  private final Map<String, JsonNode> searchByTerm = new HashMap<>();
  private final JsonNode speciesByCode;

  public ProductController(FileUtils fileUtils) {
    fileUtils.getObjectFromFile(SEARCH_FIXTURE, JsonNode.class).properties()
        .forEach((entry) -> searchByTerm.put(normalise(entry.getKey()), entry.getValue()));
    this.speciesByCode = fileUtils.getObjectFromFile(SPECIES_FIXTURE, JsonNode.class);
  }

  /** Answers the saved response for {@code q}, matched ignoring case, on page 0 only. */
  @GetMapping(value = "/mdm/trade/product/search")
  ResponseEntity<JsonNode> search(
      @RequestHeader(OCP_APIM_SUBSCRIPTION_KEY) String ocpApimSubscriptionKey,
      @RequestParam(value = "q", required = false) String query,
      @RequestParam(value = "dataset", required = false) String dataset,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "25") int size) {

    if (query == null || query.trim().length() < MINIMUM_QUERY_LENGTH) {
      ObjectNode error = JsonNodeFactory.instance.objectNode()
          .put("error", "q must be at least 2 characters");
      return ResponseEntity.badRequest().header(TRACE_HEADER, "stub-trace-id").body(error);
    }

    JsonNode saved = searchByTerm.get(normalise(query));
    JsonNode body;
    if (saved == null) {
      body = emptyPage(page, size, 0).put("totalPages", 0);
    } else if (page == 0) {
      body = saved;
    } else {
      body = emptyPage(page, size, saved.path("totalElements").asLong())
          .put("totalPages", saved.path("totalPages").asInt());
    }
    return ResponseEntity.ok().header(TRACE_HEADER, "stub-trace-id").body(body);
  }

  /** Answers the saved page for {@code code}; an unknown code or page answers an empty page. */
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
      long total = pages.isEmpty() ? 0 : pages.get(0).path("totalElements").asLong();
      body = emptyPage(page, size, total);
    }
    return ResponseEntity.ok().header(TRACE_HEADER, "stub-trace-id").body(body);
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
