package com.loanpathfinder.intake;

import com.loanpathfinder.audit.AuditEvent;
import com.loanpathfinder.audit.AuditLog;
import com.loanpathfinder.persistence.ApplicationRepository;
import com.loanpathfinder.persistence.ApplicationStatus;
import com.loanpathfinder.persistence.LoanApplication;
import com.loanpathfinder.rules.RuleSetService;
import com.loanpathfinder.scoring.RuleSet;
import com.loanpathfinder.scoring.ScoreResult;
import com.loanpathfinder.scoring.ScoringEngine;
import com.loanpathfinder.scoring.ScoringInput;
import java.security.SecureRandom;
import java.time.Clock;
import org.springframework.stereotype.Service;

/** Module: intake. Checks a submission, scores it, and saves it (FR-001, FR-002, FR-007). */
@Service
public class IntakeService {

    public static final String DISCLAIMER = "This is a pre-qualification, not a loan offer or final decision.";
    private static final String ID_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

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
    public synchronized SubmissionResult submit(ApplicationForm form) {
        RuleSet ruleSet = ruleSetService.getActive();
        ScoreResult score = scoringEngine.score(new ScoringInput(form.annualIncome(), form.monthlyDebt(),
                form.creditScore(), form.loanAmount(), form.termMonths()), ruleSet);

        LoanApplication application = new LoanApplication(newApplicationId(), clock.instant(),
                form.annualIncome(), form.monthlyDebt(), form.creditScore(), form.loanAmount(), form.termMonths(),
                form.email(), score.dtiPercent(), score.creditBand(), score.recommendation(), score.reasons(),
                score.principalReason(), score.ruleSetVersion(), ApplicationStatus.PENDING, 0L);
        applications.save(application);
        auditLog.append(new AuditEvent("system", application.id(), "scored", score.ruleSetVersion(),
                null, score.recommendation().name(), null));
        return new SubmissionResult(application.id(), application.recommendation(), null);
    }

    /** Same as submit, and marks previousApplicationId SUPERSEDED if it is still PENDING (FR-007). */
    public SubmissionResult resubmit(String previousApplicationId, ApplicationForm form) {
        throw new UnsupportedOperationException("Not implemented: IntakeService.resubmit");
    }

    /** Recommendation, reasons, application ID, and disclaimer for the Results page (FR-002, FR-003). */
    public ApplicationResult getResult(String applicationId) {
        LoanApplication a = applications.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException(applicationId));
        return new ApplicationResult(a.id(), a.recommendation(), a.dtiPercent(), a.creditBand(), a.reasons(),
                a.principalReason(), DISCLAIMER, a.ruleSetVersion());
    }

    /** The values the applicant submitted, used to pre-fill the form (FR-007). */
    public ApplicationForm getSubmittedForm(String applicationId) {
        throw new UnsupportedOperationException("Not implemented: IntakeService.getSubmittedForm");
    }

    /** "A-" plus 10 random characters, so IDs can't be guessed (Domain Model: LoanApplication). */
    private String newApplicationId() {
        StringBuilder id = new StringBuilder("A-");
        for (int i = 0; i < 10; i++) {
            id.append(ID_CHARS.charAt(random.nextInt(ID_CHARS.length())));
        }
        return id.toString();
    }
}
