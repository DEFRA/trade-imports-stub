package uk.gov.defra.trade.imports.latency;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Delays requests to a profiled integration by a draw from its latency profile, and records how
 * long the stub took to answer. Requests to any other path pass straight through.
 */
public class LatencyFilter extends OncePerRequestFilter {

    private final StubLatencies stubLatencies;

    /**
     * Creates the filter.
     *
     * @param stubLatencies the profiled integrations
     */
    public LatencyFilter(StubLatencies stubLatencies) {
        this.stubLatencies = stubLatencies;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {

        Optional<IntegrationLatency> integration = stubLatencies.forPath(request.getRequestURI());
        if (integration.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }

        IntegrationLatency profiled = integration.get();
        long startedAt = System.nanoTime();
        try {
            delay(stubLatencies.nextDelayMs(profiled));
            chain.doFilter(request, response);
        } finally {
            profiled.recordAnswered(
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt));
        }
    }

    private static void delay(long millis) {
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(Duration.ofMillis(millis));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
