package com.loanpathfinder.scoring;

import java.util.List;
import org.springframework.stereotype.Component;

/** Module: scoring. Pure calculation, no database or web code (NFR-003). */
@Component
public class ScoringEngine {

    /**
     * Calculates DTI, credit band, recommendation, both reason rows, and the principal reason.
     * Serves FR-002, FR-003, Domain Model: DTI, CreditScoreBand, ScoringRule, ExplanationReason.
     * Slice 1: placeholder result only. The real rules are added in slice 2.
     */
    public ScoreResult score(ScoringInput input, RuleSet ruleSet) {
        return new ScoreResult(null, null, null, Recommendation.NEEDS_REVIEW, List.of(), null, ruleSet.version());
    }
}
