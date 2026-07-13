package uk.gov.defra.trade.imports.stubs.mdm.countries;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.defra.trade.imports.utils.FileUtils;

@Slf4j
@RestController
@AllArgsConstructor
public class CountriesController {
  private static final String OCP_APIM_SUBSCRIPTION_KEY = "Ocp-Apim-Subscription-Key";
  private static final String ISO_SYSTEM = "ISO";
  private static final String ISO_COUNTRIES_FIXTURE = "responses/isoCountriesResponse.json";
  private static final String GBNAG_COUNTRIES_FIXTURE = "responses/countriesResponse.json";

  private FileUtils fileUtils;

  /**
   * MDM countries stub used by trade-imports-reference-data.
   *
   * <ul>
   *   <li>{@code system=ISO} — full ISO country catalogue (transited countries)
   *   <li>otherwise — GBNAG catalogue with SPS blocks (origin of import)
   * </ul>
   */
  @GetMapping(value = "/mdm/geo/countries")
  ResponseEntity<List<MdmCountry>> getCountries(
      @RequestHeader(OCP_APIM_SUBSCRIPTION_KEY) String ocpApimSubscriptionKey,
      @RequestParam(value = "system", required = false) String system,
      @RequestParam(value = "blocks", required = false) String blocks) {

    String fixture = isIsoSystem(system) ? ISO_COUNTRIES_FIXTURE : GBNAG_COUNTRIES_FIXTURE;
    log.info("Serving MDM countries stub fixture={} system={} blocks={}", fixture, system, blocks);

    List<MdmCountry> countries = List.of(
        fileUtils.getObjectFromFile(fixture, MdmCountry[].class));

    return ResponseEntity.ok()
        .header("x-ms-middleware-request-id", "stub-trace-id")
        .body(countries);
  }

  private static boolean isIsoSystem(String system) {
    return system != null && ISO_SYSTEM.equalsIgnoreCase(system);
  }
}
