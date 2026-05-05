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
  private final String OCP_APIM_SUBSCRIPTION_KEY = "Ocp-Apim-Subscription-Key";

  private FileUtils fileUtils;

  @GetMapping(value = "/mdm/geo/countries")
  ResponseEntity<List<MdmCountry>> getCountries(
      @RequestHeader(OCP_APIM_SUBSCRIPTION_KEY) String ocpApimSubscriptionKey,
      @RequestParam(value = "system", required = false) String system,
      @RequestParam(value = "classifier", required = false) String classifier){

    List<MdmCountry> countries = fileUtils.getObjectFromFile(
        "responses/countriesResponse.json"
    );

    return ResponseEntity.ok()
        .header("x-ms-middleware-request-id", "stub-trace-id")
        .body(countries);
  };
}
