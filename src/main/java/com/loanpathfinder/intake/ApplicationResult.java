package com.loanpathfinder.intake;

import com.loanpathfinder.scoring.CreditBand;
import com.loanpathfinder.scoring.ReasonRow;
import com.loanpathfinder.scoring.Recommendation;
import java.math.BigDecimal;
import java.util.List;

/** What the Results page shows (FR-002, FR-003). */
public record ApplicationResult(
        String applicationId,
        Recommendation recommendation,
        BigDecimal dtiPercent,
        CreditBand creditBand,
        List<ReasonRow> reasons,
        String principalReason,
        String disclaimer,
        int ruleSetVersion) {
}
