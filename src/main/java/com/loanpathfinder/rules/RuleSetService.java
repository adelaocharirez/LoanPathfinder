package com.loanpathfinder.rules;

import com.loanpathfinder.audit.AuditLog;
import com.loanpathfinder.persistence.RuleSetRepository;
import com.loanpathfinder.scoring.RuleSet;
import java.time.Clock;
import org.springframework.stereotype.Service;

/** Module: rules. Gets the current rule set and adds new versions (FR-008, NFR-008). */
@Service
public class RuleSetService {

    private final RuleSetRepository repository;
    private final AuditLog auditLog;
    private final Clock clock;

    public RuleSetService(RuleSetRepository repository, AuditLog auditLog, Clock clock) {
        this.repository = repository;
        this.auditLog = auditLog;
        this.clock = clock;
    }

    /** The one active version. Version 1 is seeded with the initial thresholds. */
    public RuleSet getActive() {
        return repository.findActive()
                .orElseThrow(() -> new IllegalStateException("No active rule set"));
    }

    /** Saves the draft as the next version, makes it active, and writes a rule_set_published audit entry. */
    public RuleSet publish(RuleSetDraft draft, String actorId) {
        throw new UnsupportedOperationException("Not implemented: RuleSetService.publish");
    }
}
