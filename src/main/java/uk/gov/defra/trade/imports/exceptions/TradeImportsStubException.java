package uk.gov.defra.trade.imports.exceptions;

/**
 * Exception thrown when a requested resource is not found.
 * Will be mapped to 404 Not Found by GlobalExceptionHandler.
 */
public class TradeImportsStubException extends RuntimeException {

    public TradeImportsStubException(String message) {
        super(message);
    }
}
