package com.loanpathfinder.review;

/** Thrown when the application already has a decision (FR-005, Domain Model: OfficerDecision). */
public class CaseAlreadyDecidedException extends RuntimeException {
    public CaseAlreadyDecidedException(String applicationId) {
        super(applicationId);
    }
}
