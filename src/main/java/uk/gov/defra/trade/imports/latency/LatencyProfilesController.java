package uk.gov.defra.trade.imports.latency;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Reports each integration's latency profile and the latency it actually answered with. */
@RestController
public class LatencyProfilesController {

    private static final String STUB_NAME = "trade-imports-stub";

    private final StubLatencies stubLatencies;

    /**
     * Creates the controller.
     *
     * @param stubLatencies the profiled integrations
     */
    public LatencyProfilesController(StubLatencies stubLatencies) {
        this.stubLatencies = stubLatencies;
    }

    /**
     * Reads every integration's profile, metadata and answered latency.
     *
     * @return the report
     */
    @GetMapping(value = "/latency-profiles", produces = MediaType.APPLICATION_JSON_VALUE)
    public LatencyProfilesResponse getLatencyProfiles() {
        return new LatencyProfilesResponse(
            STUB_NAME,
            stubLatencies.all().stream().map(IntegrationLatency::report).toList());
    }

    /** Forgets every integration's answered latencies, so the next read covers only what follows. */
    @DeleteMapping("/latency-profiles/answered")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearAnswered() {
        stubLatencies.clearAnswered();
    }
}
