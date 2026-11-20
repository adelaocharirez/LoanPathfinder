package com.loanpathfinder.intake;

/** Thrown when an application ID does not exist or its inputs were deleted by retention. */
public class ApplicationNotFoundException extends RuntimeException {
    public ApplicationNotFoundException(String applicationId) {
        super("Application not found: " + applicationId);
    }
}
