package uk.gov.defra.trade.imports.stubs.trade;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@AllArgsConstructor
public class TradeAuthController {

  @PostMapping(value = "/tenant/oauth2/v2.0/token")
  public Token getTradeAuthToken() {

    long expiresOn = Instant.now().plusSeconds(3600).toEpochMilli();
    return new Token(expiresOn, "stub-token");
  }
}
