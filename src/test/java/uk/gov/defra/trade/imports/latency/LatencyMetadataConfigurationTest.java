package uk.gov.defra.trade.imports.latency;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import uk.gov.defra.trade.imports.latency.LatencyProfilesResponse.IntegrationReport;

class LatencyMetadataConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withInitializer(new ConfigDataApplicationContextInitializer())
        .withUserConfiguration(Properties.class);

    @Test
    void report_shouldBeUnagreedWithNoConformanceDate_whenNoEnvironmentVariablesAreSet() {
        runner.run(context -> {
            IntegrationReport tradeToken = report(context, "trade-token");
            IntegrationReport mdm = report(context, "mdm");

            assertThat(tradeToken.agreed()).isFalse();
            assertThat(tradeToken.lastConformed()).isNull();
            assertThat(mdm.agreed()).isFalse();
            assertThat(mdm.lastConformed()).isNull();
        });
    }

    @Test
    void report_shouldUseTheTradeTokenEnvironmentVariables_whenTheyAreSet() {
        runner
            .withPropertyValues(
                "STUB_LATENCY_TRADE_TOKEN_AGREED=true",
                "STUB_LATENCY_TRADE_TOKEN_LAST_CONFORMED=2026-09-29")
            .run(context -> {
                IntegrationReport tradeToken = report(context, "trade-token");
                IntegrationReport mdm = report(context, "mdm");

                assertThat(tradeToken.agreed()).isTrue();
                assertThat(tradeToken.lastConformed()).isEqualTo(LocalDate.of(2026, 9, 29));
                assertThat(mdm.agreed()).isFalse();
                assertThat(mdm.lastConformed()).isNull();
            });
    }

    @Test
    void report_shouldUseTheMdmEnvironmentVariables_whenTheyAreSet() {
        runner
            .withPropertyValues(
                "STUB_LATENCY_MDM_AGREED=true",
                "STUB_LATENCY_MDM_LAST_CONFORMED=2026-09-30")
            .run(context -> {
                IntegrationReport mdm = report(context, "mdm");
                IntegrationReport tradeToken = report(context, "trade-token");

                assertThat(mdm.agreed()).isTrue();
                assertThat(mdm.lastConformed()).isEqualTo(LocalDate.of(2026, 9, 30));
                assertThat(tradeToken.agreed()).isFalse();
                assertThat(tradeToken.lastConformed()).isNull();
            });
    }

    @Test
    void report_shouldRunZeroDelayForBothIntegrations_whenNoProfileIsSet() {
        runner.run(context -> {
            assertThat(report(context, "mdm").profile()).isEqualTo(ProfileName.ZERO_DELAY);
            assertThat(report(context, "trade-token").profile()).isEqualTo(ProfileName.ZERO_DELAY);
        });
    }

    @Test
    void report_shouldRunSlaForBothIntegrations_whenTheStubWideProfileIsSla() {
        runner
            .withPropertyValues("STUB_LATENCY_PROFILE=sla")
            .run(context -> {
                assertThat(report(context, "mdm").profile()).isEqualTo(ProfileName.SLA);
                assertThat(report(context, "trade-token").profile()).isEqualTo(ProfileName.SLA);
            });
    }

    @Test
    void report_shouldLetTheMdmProfileOverrideTheStubWideProfile_whenBothAreSet() {
        runner
            .withPropertyValues(
                "STUB_LATENCY_PROFILE=sla",
                "STUB_LATENCY_MDM_PROFILE=zero-delay")
            .run(context -> {
                assertThat(report(context, "mdm").profile()).isEqualTo(ProfileName.ZERO_DELAY);
                assertThat(report(context, "trade-token").profile()).isEqualTo(ProfileName.SLA);
            });
    }

    @Test
    void report_shouldUseTheMdmSlaTarget_whenOnlyTheMdmP95IsSet() {
        runner
            .withPropertyValues("STUB_LATENCY_MDM_P95_MS=470")
            .run(context -> {
                assertThat(report(context, "mdm").slaTargets().p95Ms()).isEqualTo(470);
                assertThat(report(context, "trade-token").slaTargets().p95Ms()).isEqualTo(400);
            });
    }

    private static IntegrationReport report(AssertableApplicationContext context, String name) {
        return context.getBean(StubLatencies.class).all().stream()
            .filter(integration -> integration.name().equals(name))
            .findFirst()
            .orElseThrow()
            .report();
    }

    @EnableConfigurationProperties(StubLatencyProperties.class)
    static class Properties {

        @Bean
        StubLatencies stubLatencies(StubLatencyProperties properties) {
            return new StubLatencies(properties);
        }
    }
}
