package com.loanpathfinder.scoring;

import java.math.BigDecimal;

/**
 * Domain Model: RuleSet. Thresholds and reference rate for one version.
 * Initial values: rate 9.0, unlikely below score 580 or DTI above 43.0,
 * likely at score 670 with DTI 36.0 or lower, or score 740 with DTI 43.0 or lower.
 */
public record RuleSet(
        int version,
        BigDecimal referenceAnnualRatePercent,
        int unlikelyBelowScore,
        BigDecimal unlikelyAboveDtiPercent,
        int likelyMinScore,
        BigDecimal likelyMaxDtiPercent,
        int strongMinScore,
        BigDecimal strongMaxDtiPercent) {
}
