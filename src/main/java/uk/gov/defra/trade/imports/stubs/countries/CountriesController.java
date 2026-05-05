package uk.gov.defra.trade.imports.stubs.countries;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.defra.trade.imports.utils.FileUtils;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/countries")
public class CountriesController {

  private FileUtils fileUtils;

  @GetMapping()
  public ResponseEntity<List<Country>> getCountries(
      @RequestParam(required = false, value = "classifier") List<String> classifiers) {

    List<Country> countries = fileUtils.getObjectFromFile(
        "responses/countriesResponse.json"
    );

    return ResponseEntity.ok(countries);
  }
}
