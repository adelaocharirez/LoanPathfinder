package com.loanpathfinder.scoring;

import org.springframework.stereotype.Component;

/** Module: scoring. Pure calculation, no database or web code (NFR-003). */
@Component
public class ScoringEngine {

    /**
     * Calculates DTI, credit band, recommendation, both reason rows, and the principal reason.
     * Serves FR-002, FR-003, Domain Model: DTI, CreditScoreBand, ScoringRule, ExplanationReason.
     */
    public ScoreResult score(ScoringInput input, RuleSet ruleSet) {
        throw new UnsupportedOperationException("Not implemented: ScoringEngine.score");
    }
}
