package com.loanpathfinder.security;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** Module: security. Remembers which applications each browser session created (FR-011, NFR-001). */
@Component
public class SessionOwnership {

    private final Map<String, Set<String>> owned = new ConcurrentHashMap<>();

    /** Records that this session created the application. */
    public void claim(String sessionId, String applicationId) {
        owned.computeIfAbsent(sessionId, id -> ConcurrentHashMap.newKeySet()).add(applicationId);
    }

    /** True only if this session created the application. */
    public boolean owns(String sessionId, String applicationId) {
        return sessionId != null && owned.getOrDefault(sessionId, Set.of()).contains(applicationId);
    }
}
