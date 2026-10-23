package uk.gov.defra.trade.imports.stubs.mdm;

import org.springframework.http.ResponseEntity;

public final class MdmStubResponses {

  public static final String OCP_APIM_SUBSCRIPTION_KEY = "Ocp-Apim-Subscription-Key";

  private MdmStubResponses() {}

  public static ResponseEntity.BodyBuilder withTraceHeader(ResponseEntity.BodyBuilder response) {
    return response.header("x-ms-middleware-request-id", "stub-trace-id");
  }
}
