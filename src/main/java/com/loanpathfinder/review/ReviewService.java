package com.loanpathfinder.review;

import com.loanpathfinder.audit.AuditLog;
import com.loanpathfinder.notification.DecisionNotifier;
import com.loanpathfinder.persistence.ApplicationRepository;
import com.loanpathfinder.persistence.DecisionRecord;
import com.loanpathfinder.persistence.DecisionRepository;
import java.time.Clock;
import org.springframework.stereotype.Service;

/** Module: review. Runs the review queue and records one decision per application (FR-004, FR-005). */
@Service
public class ReviewService {

    private final ApplicationRepository applications;
    private final DecisionRepository decisions;
    private final AuditLog auditLog;
    private final DecisionNotifier notifier;
    private final Clock clock;

    public ReviewService(ApplicationRepository applications, DecisionRepository decisions, AuditLog auditLog,
                         DecisionNotifier notifier, Clock clock) {
        this.applications = applications;
        this.decisions = decisions;
        this.auditLog = auditLog;
        this.notifier = notifier;
        this.clock = clock;
    }

    /** Matching applications, oldest first by default (FR-004). */
    public QueuePage listQueue(QueueQuery query) {
        throw new UnsupportedOperationException("Not implemented: ReviewService.listQueue");
    }

    /** Inputs, scoring breakdown, decision, and audit trail for one application (FR-005). */
    public CaseView getCase(String applicationId) {
        throw new UnsupportedOperationException("Not implemented: ReviewService.getCase");
    }

    /**
     * Saves the decision, updates the status, and writes the audit entry in one transaction,
     * then asks DecisionNotifier to send the email after commit (FR-005, FR-006, FR-009).
     */
    public DecisionRecord recordDecision(String applicationId, DecisionRequest request, String officerId) {
        throw new UnsupportedOperationException("Not implemented: ReviewService.recordDecision");
    }
}
