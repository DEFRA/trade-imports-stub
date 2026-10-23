package uk.gov.defra.trade.imports.stubs.mdm;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;
import uk.gov.defra.trade.imports.exceptions.GlobalExceptionHandler;
import uk.gov.defra.trade.imports.faults.StubFaults;
import uk.gov.defra.trade.imports.latency.LatencyConfig;
import uk.gov.defra.trade.imports.latency.StubLatencies;
import uk.gov.defra.trade.imports.utils.FileUtils;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@WebMvcTest
@Import({LatencyConfig.class, StubLatencies.class, StubFaults.class, GlobalExceptionHandler.class,
    FileUtils.class})
public @interface MdmStubWebMvcTest {

  @AliasFor(annotation = WebMvcTest.class, attribute = "controllers")
  Class<?>[] value();
}
