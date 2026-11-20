package com.loanpathfinder.security;

import java.time.Clock;
import org.springframework.stereotype.Component;

/** Module: security. At most 20 submissions per IP address per rolling hour (NFR-005). */
@Component
public class SubmissionRateLimiter {

    private final Clock clock;

    public SubmissionRateLimiter(Clock clock) {
        this.clock = clock;
    }

    /** Records one submission, or throws RateLimitExceededException if this would be the 21st in the last hour. */
    public void checkAndRecord(String clientIp) {
        throw new UnsupportedOperationException("Not implemented: SubmissionRateLimiter.checkAndRecord");
    }
}
