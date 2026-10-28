package uk.gov.defra.trade.imports.stubs.mdm;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static uk.gov.defra.trade.imports.stubs.mdm.MdmStubResponses.OCP_APIM_SUBSCRIPTION_KEY;

import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

public final class MdmStubRequests {

  private MdmStubRequests() {}

  public static MockHttpServletRequestBuilder mdmGet(String path) {
    return get(path).header(OCP_APIM_SUBSCRIPTION_KEY, "any-value-at-all");
  }

  public static ResultMatcher carriesTheStubTraceHeader() {
    return header().string("x-ms-middleware-request-id", "stub-trace-id");
  }
}
