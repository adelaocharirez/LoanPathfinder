package com.loanpathfinder.rules;

import java.math.BigDecimal;

/** Thresholds for a new rule set version (FR-008). The version number is assigned on publish. */
public record RuleSetDraft(
        BigDecimal referenceAnnualRatePercent,
        int unlikelyBelowScore,
        BigDecimal unlikelyAboveDtiPercent,
        int likelyMinScore,
        BigDecimal likelyMaxDtiPercent,
        int strongMinScore,
        BigDecimal strongMaxDtiPercent) {
}
