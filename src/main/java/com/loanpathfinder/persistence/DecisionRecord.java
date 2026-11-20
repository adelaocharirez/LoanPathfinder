package com.loanpathfinder.persistence;

import com.loanpathfinder.scoring.Recommendation;
import java.time.Instant;

/** Domain Model: OfficerDecision. One per application. */
public record DecisionRecord(
        String applicationId,
        DecisionType decision,
        Recommendation originalRecommendation,
        Recommendation finalOutcome,
        String justification,
        String note,
        String officerId,
        Instant decidedAt) {
}
