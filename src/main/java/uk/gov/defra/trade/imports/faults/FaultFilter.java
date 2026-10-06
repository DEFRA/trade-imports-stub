package uk.gov.defra.trade.imports.faults;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Injects the fault switched on for an integration into requests to its paths. Requests to any
 * other path, including {@code /faults} itself, pass straight through.
 */
public class FaultFilter extends OncePerRequestFilter {

    private static final int DROP_DECLARED_LENGTH = 1024;
    private static final byte[] DROP_PARTIAL_BODY =
        "{\"partial\":true,\"".getBytes(StandardCharsets.UTF_8);

    private final StubFaults stubFaults;

    /**
     * Creates the filter.
     *
     * @param stubFaults the faults that can be injected
     */
    public FaultFilter(StubFaults stubFaults) {
        this.stubFaults = stubFaults;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {

        Optional<ActiveFault> decision = stubFaults.decide(request.getRequestURI());
        if (decision.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }

        ActiveFault fault = decision.get();
        switch (fault.kind()) {
            case SLOW -> {
                pause(fault.delayMs());
                chain.doFilter(request, response);
            }
            case HANG -> {
                pause(fault.delayMs());
                dropConnection(response);
            }
            case RESET -> dropConnection(response);
            case THROTTLE -> {
                response.setHeader(
                    HttpHeaders.RETRY_AFTER, String.valueOf(fault.retryAfterSeconds()));
                answer(response, HttpStatus.TOO_MANY_REQUESTS.value(), "throttle");
            }
            case ERROR -> answer(response, fault.status(), "error");
        }
    }

    private static void answer(HttpServletResponse response, int status, String fault)
        throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"fault\":\"%s\"}".formatted(fault));
    }

    /**
     * Ends the exchange with a response that promises more body than it sends and asks for the
     * connection to close, so the client reads a premature end of body.
     */
    private static void dropConnection(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setHeader(HttpHeaders.CONNECTION, "close");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setContentLength(DROP_DECLARED_LENGTH);
        response.getOutputStream().write(DROP_PARTIAL_BODY);
        response.flushBuffer();
    }

    private static void pause(long millis) {
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
