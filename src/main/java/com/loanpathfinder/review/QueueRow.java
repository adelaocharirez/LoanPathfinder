package com.loanpathfinder.review;

import com.loanpathfinder.persistence.ApplicationStatus;
import com.loanpathfinder.scoring.Recommendation;
import java.time.Instant;

public record QueueRow(
        String applicationId,
        Instant submittedAt,
        Recommendation recommendation,
        String principalReason,
        ApplicationStatus status) {
}
