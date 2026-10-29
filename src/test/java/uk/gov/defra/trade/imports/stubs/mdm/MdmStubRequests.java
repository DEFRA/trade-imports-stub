package uk.gov.defra.trade.imports.stubs.mdm;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static uk.gov.defra.trade.imports.stubs.mdm.MdmStubResponses.OCP_APIM_SUBSCRIPTION_KEY;

import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

public final class MdmStubRequests {

  private MdmStubRequests() {}

  public static HttpHeaders mdmCallerHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.add(OCP_APIM_SUBSCRIPTION_KEY, "any-value-at-all");
    return headers;
  }

  public static MockHttpServletRequestBuilder mdmGet(String path) {
    return get(path).headers(mdmCallerHeaders());
  }
}
