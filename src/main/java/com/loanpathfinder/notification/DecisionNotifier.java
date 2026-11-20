package com.loanpathfinder.notification;

import com.loanpathfinder.audit.AuditLog;
import com.loanpathfinder.persistence.ApplicationRepository;
import java.time.Clock;
import org.springframework.stereotype.Service;

/** Module: notification. Sends the decision email and retries failures (FR-009, NFR-007). */
@Service
public class DecisionNotifier {

    private final ApplicationRepository applications;
    private final AuditLog auditLog;
    private final EmailGateway emailGateway;
    private final Clock clock;

    public DecisionNotifier(ApplicationRepository applications, AuditLog auditLog,
                            EmailGateway emailGateway, Clock clock) {
        this.applications = applications;
        this.auditLog = auditLog;
        this.emailGateway = emailGateway;
        this.clock = clock;
    }

    /** Sends one email if the applicant gave an address and writes email_sent or email_failed to the audit log. */
    public NotificationResult notifyDecision(String applicationId) {
        throw new UnsupportedOperationException("Not implemented: DecisionNotifier.notifyDecision");
    }

    /** Retries FAILED emails less than 24 hours old; returns how many were sent. */
    public int retryFailed() {
        throw new UnsupportedOperationException("Not implemented: DecisionNotifier.retryFailed");
    }
}
