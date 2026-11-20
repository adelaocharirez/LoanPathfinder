package com.loanpathfinder.retention;

import com.loanpathfinder.audit.AuditLog;
import com.loanpathfinder.persistence.ApplicationRepository;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Module: retention. Deletes applicant inputs and emails past the retention period (FR-011, NFR-009). */
@Component
public class RetentionJob {

    private final ApplicationRepository applications;
    private final AuditLog auditLog;
    private final Clock clock;
    private final int retentionMonths;

    public RetentionJob(ApplicationRepository applications, AuditLog auditLog, Clock clock,
                        @Value("${loanpathfinder.retention-months:25}") int retentionMonths) {
        this.applications = applications;
        this.auditLog = auditLog;
        this.clock = clock;
        this.retentionMonths = retentionMonths;
    }

    /** Clears inputs and email of applications submitted more than retentionMonths ago. Returns the count. Runs daily. */
    public int purgeExpired() {
        throw new UnsupportedOperationException("Not implemented: RetentionJob.purgeExpired");
    }
}
