package com.loanpathfinder.intake;

import com.loanpathfinder.audit.AuditLog;
import com.loanpathfinder.persistence.ApplicationRepository;
import com.loanpathfinder.rules.RuleSetService;
import com.loanpathfinder.scoring.ScoringEngine;
import java.security.SecureRandom;
import java.time.Clock;
import org.springframework.stereotype.Service;

/** Module: intake. Checks a submission, scores it, and saves it (FR-001, FR-002, FR-007). */
@Service
public class IntakeService {

    private final ScoringEngine scoringEngine;
    private final RuleSetService ruleSetService;
    private final AuditLog auditLog;
    private final ApplicationRepository applications;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public IntakeService(ScoringEngine scoringEngine, RuleSetService ruleSetService, AuditLog auditLog,
                         ApplicationRepository applications, Clock clock) {
        this.scoringEngine = scoringEngine;
        this.ruleSetService = ruleSetService;
        this.auditLog = auditLog;
        this.applications = applications;
        this.clock = clock;
    }

    /** Validates, scores with the active rule set, saves as PENDING, and writes a "scored" audit entry in one transaction. */
    public SubmissionResult submit(ApplicationForm form) {
        throw new UnsupportedOperationException("Not implemented: IntakeService.submit");
    }

    /** Same as submit, and marks previousApplicationId SUPERSEDED if it is still PENDING (FR-007). */
    public SubmissionResult resubmit(String previousApplicationId, ApplicationForm form) {
        throw new UnsupportedOperationException("Not implemented: IntakeService.resubmit");
    }

    /** Recommendation, reasons, application ID, and disclaimer for the Results page (FR-002, FR-003). */
    public ApplicationResult getResult(String applicationId) {
        throw new UnsupportedOperationException("Not implemented: IntakeService.getResult");
    }

    /** The values the applicant submitted, used to pre-fill the form (FR-007). */
    public ApplicationForm getSubmittedForm(String applicationId) {
        throw new UnsupportedOperationException("Not implemented: IntakeService.getSubmittedForm");
    }
}
