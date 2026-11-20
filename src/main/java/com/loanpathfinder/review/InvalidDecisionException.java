package com.loanpathfinder.review;

/** Thrown when an override has a missing or short justification or the same outcome (FR-005). */
public class InvalidDecisionException extends RuntimeException {
    public InvalidDecisionException(String message) {
        super(message);
    }
}
