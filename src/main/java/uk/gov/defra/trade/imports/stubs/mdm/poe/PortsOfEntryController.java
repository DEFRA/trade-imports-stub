package uk.gov.defra.trade.imports.stubs.mdm.poe;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.defra.trade.imports.utils.FileUtils;

@Slf4j
@RestController
public class PortsOfEntryController {

  private static final String OCP_APIM_SUBSCRIPTION_KEY = "Ocp-Apim-Subscription-Key";
  private static final String PORTS_FIXTURE = "responses/portsOfEntryResponse.json";
  private static final String TRACE_HEADER = "x-ms-middleware-request-id";

  private final JsonNode ports;

  public PortsOfEntryController(FileUtils fileUtils) {
    this.ports = fileUtils.getObjectFromFile(PORTS_FIXTURE, JsonNode.class);
  }

  /** Answers MDM's own port records, served whole and in MDM's order. */
  @GetMapping(value = "/mdm/trade/bcp/poes")
  ResponseEntity<JsonNode> getPortsOfEntry(
      @RequestHeader(OCP_APIM_SUBSCRIPTION_KEY) String ocpApimSubscriptionKey,
      @RequestParam(value = "system", required = false) String system) {

    return ResponseEntity.ok()
        .header(TRACE_HEADER, "stub-trace-id")
        .body(ports);
  }
}
