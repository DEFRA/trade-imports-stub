package uk.gov.defra.trade.imports.faults;

/** Thrown when a fault names a path that is not one of its integration's paths. */
public class UnknownFaultPathException extends RuntimeException {

    /**
     * Creates the exception.
     *
     * @param message what was wrong
     */
    public UnknownFaultPathException(String message) {
        super(message);
    }
}
