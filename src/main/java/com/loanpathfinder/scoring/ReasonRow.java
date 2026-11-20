package com.loanpathfinder.scoring;

/**
 * Domain Model: ExplanationReason. One row per factor.
 * factor is "Credit score" or "Debt-to-income ratio"; applicantValue is e.g. "750" or "30.0%".
 */
public record ReasonRow(
        String factor,
        String applicantValue,
        String guideline,
        boolean metGuideline,
        String ruleId) {
}
