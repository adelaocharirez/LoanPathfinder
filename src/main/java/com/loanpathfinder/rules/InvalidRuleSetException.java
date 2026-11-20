package com.loanpathfinder.rules;

/** Thrown when a draft rule set has missing or impossible values. */
public class InvalidRuleSetException extends RuntimeException {
    public InvalidRuleSetException(String message) {
        super(message);
    }
}
