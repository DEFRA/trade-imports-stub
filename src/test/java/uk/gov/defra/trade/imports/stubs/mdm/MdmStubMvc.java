package uk.gov.defra.trade.imports.stubs.mdm;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static uk.gov.defra.trade.imports.stubs.mdm.MdmStubResponses.OCP_APIM_SUBSCRIPTION_KEY;

import org.springframework.test.web.servlet.ResultMatcher;

public final class MdmStubMvc {

  public static final String ANY_KEY = "any-value-at-all";

  private MdmStubMvc() {}

  public static String subscriptionKeyHeader() {
    return OCP_APIM_SUBSCRIPTION_KEY;
  }

  public static ResultMatcher carriesTheStubTraceHeader() {
    return header().string("x-ms-middleware-request-id", "stub-trace-id");
  }
}
