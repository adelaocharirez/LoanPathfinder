package com.loanpathfinder.scoring;

import java.math.BigDecimal;

public record ScoringInput(
        BigDecimal annualIncome,
        BigDecimal monthlyDebt,
        int creditScore,
        BigDecimal loanAmount,
        int termMonths) {
}
