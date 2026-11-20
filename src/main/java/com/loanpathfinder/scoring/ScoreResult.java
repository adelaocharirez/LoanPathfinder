package com.loanpathfinder.scoring;

import java.math.BigDecimal;
import java.util.List;

/** Output of ScoringEngine.score. dtiPercent is already rounded HALF_UP to one decimal place. */
public record ScoreResult(
        BigDecimal estimatedMonthlyPayment,
        BigDecimal dtiPercent,
        CreditBand creditBand,
        Recommendation recommendation,
        List<ReasonRow> reasons,
        String principalReason,
        int ruleSetVersion) {
}
