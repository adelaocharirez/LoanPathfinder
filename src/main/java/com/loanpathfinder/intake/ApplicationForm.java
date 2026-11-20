package com.loanpathfinder.intake;

import java.math.BigDecimal;

/**
 * Applicant form (Domain Model: FinancialProfile and loan request fields).
 * Boxed types so a missing field can be reported as "required".
 */
public record ApplicationForm(
        BigDecimal annualIncome,
        BigDecimal monthlyDebt,
        Integer creditScore,
        BigDecimal loanAmount,
        Integer termMonths,
        Boolean acknowledged,
        String email) {
}
