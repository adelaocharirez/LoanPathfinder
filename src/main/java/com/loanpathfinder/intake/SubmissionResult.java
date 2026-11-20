package com.loanpathfinder.intake;

import com.loanpathfinder.scoring.Recommendation;

/** Returned after a successful submit or resubmit. supersededApplicationId is null for a first submit. */
public record SubmissionResult(
        String applicationId,
        Recommendation recommendation,
        String supersededApplicationId) {
}
