package uk.gov.defra.trade.imports.faults;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * A fault switched on for one integration.
 *
 * @param kind what the fault does
 * @param rate the probability a request is faulted
 * @param delayMs how long a slow fault waits and a hang fault holds the request
 * @param status the status an error fault answers
 * @param retryAfterSeconds the {@code Retry-After} a throttle fault answers
 * @param paths the paths the fault applies to
 * @param expiresAt when the fault stops applying
 */
public record ActiveFault(
    FaultKind kind,
    double rate,
    long delayMs,
    int status,
    int retryAfterSeconds,
    List<String> paths,
    Instant expiresAt) {

    private static final int DEFAULT_STATUS = 503;
    private static final int DEFAULT_RETRY_AFTER_SECONDS = 1;

    public ActiveFault {
        Objects.requireNonNull(kind, "kind");
        paths = List.copyOf(Objects.requireNonNull(paths, "paths"));
        Objects.requireNonNull(expiresAt, "expiresAt");
    }

    /**
     * Builds the fault a request asks for, with the defaults filled in.
     *
     * @param request the request body
     * @param integrationPaths every path the integration answers, used when the request names none
     * @param now the current time
     * @return the fault, expiring {@code expiresInSeconds} after {@code now}
     */
    public static ActiveFault from(
        FaultRequest request, List<String> integrationPaths, Instant now) {
        List<String> paths = request.paths() == null || request.paths().isEmpty()
            ? integrationPaths
            : request.paths();
        return new ActiveFault(
            request.kind(),
            request.rate().doubleValue(),
            request.delayMs() == null ? 0 : request.delayMs(),
            request.status() == null ? DEFAULT_STATUS : request.status(),
            request.retryAfterSeconds() == null
                ? DEFAULT_RETRY_AFTER_SECONDS
                : request.retryAfterSeconds(),
            paths,
            now.plusSeconds(request.expiresInSeconds()));
    }

    /**
     * Whether the fault applies to a request.
     *
     * @param path the request path
     * @param now the current time
     * @return true while the fault is unexpired and names the path
     */
    public boolean appliesTo(String path, Instant now) {
        return now.isBefore(expiresAt) && paths.contains(path);
    }
}
