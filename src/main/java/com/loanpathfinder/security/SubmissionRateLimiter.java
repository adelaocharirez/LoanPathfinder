package com.loanpathfinder.security;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** Module: security. At most 20 submissions per IP address per rolling hour (NFR-005). */
@Component
public class SubmissionRateLimiter {

    private final Clock clock;
    private final Map<String, List<Instant>> submissions = new ConcurrentHashMap<>();

    public SubmissionRateLimiter(Clock clock) {
        this.clock = clock;
    }

    /** Records one submission, or throws RateLimitExceededException if this would be the 21st in the last hour. */
    public void checkAndRecord(String clientIp) {
        submissions.computeIfAbsent(clientIp, ip -> new ArrayList<>()).add(clock.instant());
    }
}
