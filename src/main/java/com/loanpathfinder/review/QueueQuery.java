package com.loanpathfinder.review;

import com.loanpathfinder.persistence.ApplicationStatus;
import com.loanpathfinder.scoring.Recommendation;
import java.time.LocalDate;

/** FR-004 filters. status defaults to PENDING; sort is "date" (oldest first) or "recommendation". */
public record QueueQuery(
        ApplicationStatus status,
        Recommendation recommendation,
        LocalDate from,
        LocalDate to,
        String applicationIdSearch,
        String sort,
        int page) {
}
