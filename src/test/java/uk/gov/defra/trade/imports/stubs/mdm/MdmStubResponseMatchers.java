package uk.gov.defra.trade.imports.stubs.mdm;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

import org.springframework.test.web.servlet.ResultMatcher;

public final class MdmStubResponseMatchers {

  private MdmStubResponseMatchers() {}

  public static ResultMatcher carriesTheStubTraceHeader() {
    return header().string("x-ms-middleware-request-id", "stub-trace-id");
  }
}
