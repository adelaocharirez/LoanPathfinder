package com.loanpathfinder.persistence;

import com.loanpathfinder.scoring.CreditBand;
import com.loanpathfinder.scoring.ReasonRow;
import com.loanpathfinder.scoring.Recommendation;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Domain Model: LoanApplication. Input fields and email become null after retention (NFR-009). */
public record LoanApplication(
        String id,
        Instant submittedAt,
        BigDecimal annualIncome,
        BigDecimal monthlyDebt,
        Integer creditScore,
        BigDecimal loanAmount,
        Integer termMonths,
        String email,
        BigDecimal dtiPercent,
        CreditBand creditBand,
        Recommendation recommendation,
        List<ReasonRow> reasons,
        String principalReason,
        int ruleSetVersion,
        ApplicationStatus status,
        long version) {
}
