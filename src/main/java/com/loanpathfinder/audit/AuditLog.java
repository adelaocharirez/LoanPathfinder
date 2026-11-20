package com.loanpathfinder.audit;

import com.loanpathfinder.persistence.AuditEntry;
import com.loanpathfinder.persistence.AuditEntryRepository;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;

/** Module: audit. Writes, searches, and exports audit entries (FR-006, FR-010, NFR-002). */
@Service
public class AuditLog {

    private final AuditEntryRepository repository;
    private final Clock clock;

    public AuditLog(AuditEntryRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /** Appends one entry stamped with clock.instant(). Must run inside the caller's transaction. */
    public AuditEntry append(AuditEvent event) {
        throw new UnsupportedOperationException("Not implemented: AuditLog.append");
    }

    /** All entries for one application, oldest first (FR-006). */
    public List<AuditEntry> trail(String applicationId) {
        throw new UnsupportedOperationException("Not implemented: AuditLog.trail");
    }

    /** Entries matching every non-null filter, oldest first (FR-010). */
    public List<AuditEntry> search(AuditQuery query) {
        throw new UnsupportedOperationException("Not implemented: AuditLog.search");
    }

    /** CSV of every field of each matching entry; cells starting with =, +, -, @ are escaped (FR-010). */
    public String exportCsv(AuditQuery query) {
        throw new UnsupportedOperationException("Not implemented: AuditLog.exportCsv");
    }
}
