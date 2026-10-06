package uk.gov.defra.trade.imports.faults;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

/** What an injected fault does to a request. */
public enum FaultKind {

    /** Waits, then answers normally. */
    SLOW("slow"),

    /** Holds the request far past any sane caller timeout, then drops the connection. */
    HANG("hang"),

    /** Drops the connection at once, without a complete answer. */
    RESET("reset"),

    /** Answers 429 with a {@code Retry-After} header. */
    THROTTLE("throttle"),

    /** Answers a 5xx status. */
    ERROR("error");

    private final String label;

    FaultKind(String label) {
        this.label = label;
    }

    /**
     * The kind's name in JSON.
     *
     * @return the lower-case label, such as {@code slow}
     */
    @JsonValue
    public String label() {
        return label;
    }

    /**
     * Finds a kind by its label.
     *
     * @param label the label, such as {@code hang}
     * @return the kind
     * @throws IllegalArgumentException when no kind has the label
     */
    @JsonCreator
    public static FaultKind fromLabel(String label) {
        return Arrays.stream(values())
            .filter(kind -> kind.label.equals(label))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown fault kind: " + label));
    }
}
