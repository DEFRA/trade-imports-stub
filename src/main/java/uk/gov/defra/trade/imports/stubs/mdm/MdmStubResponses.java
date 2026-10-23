package uk.gov.defra.trade.imports.stubs.mdm;

import org.springframework.http.ResponseEntity;

public final class MdmStubResponses {

  public static final String OCP_APIM_SUBSCRIPTION_KEY = "Ocp-Apim-Subscription-Key";
  private static final String TRACE_HEADER = "x-ms-middleware-request-id";
  private static final String TRACE_ID = "stub-trace-id";

  private MdmStubResponses() {}

  public static ResponseEntity.BodyBuilder withTraceHeader(ResponseEntity.BodyBuilder response) {
    return response.header(TRACE_HEADER, TRACE_ID);
  }
}
