package com.loanpathfinder.security;

/** Thrown when a staff member is not signed in (401) or has the wrong role (403). */
public class AccessDeniedException extends RuntimeException {
    private final boolean authenticated;

    public AccessDeniedException(String message, boolean authenticated) {
        super(message);
        this.authenticated = authenticated;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }
}
