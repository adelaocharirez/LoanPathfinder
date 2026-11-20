package com.loanpathfinder.review;

import com.loanpathfinder.persistence.AuditEntry;
import com.loanpathfinder.persistence.DecisionRecord;
import com.loanpathfinder.persistence.LoanApplication;
import java.util.List;

/** Case Review page data: inputs, scoring breakdown, decision (if any), and audit trail (FR-005). */
public record CaseView(
        LoanApplication application,
        DecisionRecord decision,
        List<AuditEntry> auditTrail) {
}
