package uk.gov.defra.trade.imports.faults;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.defra.trade.imports.faults.FaultsResponse.IntegrationFaultReport;

/** Switches faults on and off for each integration, and reports what was injected. */
@RestController
public class FaultsController {

    private static final String STUB_NAME = "trade-imports-stub";

    private final StubFaults stubFaults;

    /**
     * Creates the controller.
     *
     * @param stubFaults the faults that can be injected
     */
    public FaultsController(StubFaults stubFaults) {
        this.stubFaults = stubFaults;
    }

    /**
     * Reads every integration's active fault and counters.
     *
     * @return the report
     */
    @GetMapping(value = "/faults", produces = MediaType.APPLICATION_JSON_VALUE)
    public FaultsResponse getFaults() {
        return new FaultsResponse(STUB_NAME, stubFaults.report());
    }

    /**
     * Switches a fault on for one integration, replacing any active one.
     *
     * @param integration the integration's name
     * @param request the fault
     * @return the integration's report
     */
    @PutMapping(
        value = "/faults/{integration}",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public IntegrationFaultReport putFault(
        @PathVariable String integration,
        @Valid @RequestBody FaultRequest request) {
        return stubFaults.apply(integration, request);
    }

    /**
     * Switches one integration's fault off.
     *
     * @param integration the integration's name
     */
    @DeleteMapping("/faults/{integration}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFault(@PathVariable String integration) {
        stubFaults.clear(integration);
    }

    /** Switches every integration's fault off. */
    @DeleteMapping("/faults")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFaults() {
        stubFaults.clearAll();
    }

    /**
     * Answers 400 for a body that is not valid JSON or names an unknown fault kind. Without this
     * the global handler would answer 500.
     *
     * @param exception what was wrong
     * @return the problem
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadableBody(HttpMessageNotReadableException exception) {
        String detail =
            "The fault could not be read: " + exception.getMostSpecificCause().getMessage();
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
    }

    /**
     * Answers 400 for a fault naming a path its integration does not answer.
     *
     * @param exception what was wrong
     * @return the problem
     */
    @ExceptionHandler(UnknownFaultPathException.class)
    public ProblemDetail handleUnknownPath(UnknownFaultPathException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }
}
