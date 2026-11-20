package com.loanpathfinder.persistence;

import com.loanpathfinder.scoring.RuleSet;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** Module: persistence. Saves and loads data. */
@Repository
public class AuditEntryRepository {

    /** Insert only. There is no update or delete (NFR-002). */
    public AuditEntry insert(AuditEntry entry) {
        throw new UnsupportedOperationException("Not implemented: AuditEntryRepository.insert");
    }

    public List<AuditEntry> findAll() {
        throw new UnsupportedOperationException("Not implemented: AuditEntryRepository.findAll");
    }
}
