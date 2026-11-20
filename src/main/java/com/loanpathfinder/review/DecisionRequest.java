package com.loanpathfinder.review;

import com.loanpathfinder.persistence.DecisionType;
import com.loanpathfinder.scoring.Recommendation;

/** newOutcome and justification are required for OVERRIDE; note is optional for CONFIRM. */
public record DecisionRequest(
        DecisionType decision,
        Recommendation newOutcome,
        String justification,
        String note) {
}
