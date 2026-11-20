package com.loanpathfinder.persistence;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

/** Module: persistence. In-memory, insert-only store for the MVP. */
@Repository
public class AuditEntryRepository {

    private final List<AuditEntry> entries = new ArrayList<>();

    /** Insert only. There is no update or delete (NFR-002). */
    public synchronized AuditEntry insert(AuditEntry entry) {
        entries.add(entry);
        return entry;
    }

    public List<AuditEntry> findAll() {
        throw new UnsupportedOperationException("Not implemented: AuditEntryRepository.findAll");
    }
}
