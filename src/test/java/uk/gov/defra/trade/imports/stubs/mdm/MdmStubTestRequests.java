package uk.gov.defra.trade.imports.stubs.mdm;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

import org.springframework.test.web.servlet.ResultMatcher;

public final class MdmStubTestRequests {

  public static final String KEY_HEADER = "Ocp-Apim-Subscription-Key";
  public static final String ANY_KEY = "any-value-at-all";

  private MdmStubTestRequests() {}

  public static ResultMatcher carriesTheStubTraceHeader() {
    return header().string("x-ms-middleware-request-id", "stub-trace-id");
  }
}
