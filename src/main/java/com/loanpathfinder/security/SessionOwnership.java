package com.loanpathfinder.security;

import org.springframework.stereotype.Component;

/** Module: security. Remembers which applications each browser session created (FR-011, NFR-001). */
@Component
public class SessionOwnership {

    /** Records that this session created the application. */
    public void claim(String sessionId, String applicationId) {
        throw new UnsupportedOperationException("Not implemented: SessionOwnership.claim");
    }

    /** True only if this session created the application. */
    public boolean owns(String sessionId, String applicationId) {
        throw new UnsupportedOperationException("Not implemented: SessionOwnership.owns");
    }
}
