package com.loanpathfinder.review;

/** Thrown when no application has this ID. */
public class CaseNotFoundException extends RuntimeException {
    public CaseNotFoundException(String applicationId) {
        super(applicationId);
    }
}
