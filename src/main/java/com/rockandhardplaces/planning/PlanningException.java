package com.rockandhardplaces.planning;

/** Only fixed public messages cross the API boundary; provider payloads are never attached. */
public class PlanningException extends RuntimeException {
    public enum Reason { NOT_CONFIGURED, UNAVAILABLE, INVALID_RESPONSE }
    private final Reason reason;

    public PlanningException(Reason reason) {
        super(switch (reason) {
            case NOT_CONFIGURED -> "Project planning is not configured";
            case UNAVAILABLE -> "Project planning is temporarily unavailable. Please try again later.";
            case INVALID_RESPONSE -> "Project planning returned an invalid response. Please try again.";
        });
        this.reason = reason;
    }

    public Reason reason() { return reason; }
}
