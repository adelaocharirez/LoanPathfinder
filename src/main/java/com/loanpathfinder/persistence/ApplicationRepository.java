package com.loanpathfinder.persistence;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** Module: persistence. In-memory store for the MVP. */
@Repository
public class ApplicationRepository {

    private final Map<String, LoanApplication> applications = new LinkedHashMap<>();

    /** Inserts or updates; fails with an optimistic-lock error if the version is stale. */
    public synchronized LoanApplication save(LoanApplication application) {
        applications.put(application.id(), application);
        return application;
    }

    public synchronized Optional<LoanApplication> findById(String applicationId) {
        return Optional.ofNullable(applications.get(applicationId));
    }

    public List<LoanApplication> findAll() {
        throw new UnsupportedOperationException("Not implemented: ApplicationRepository.findAll");
    }

    /** Nulls inputs and email; returns how many applications were cleared. */
    public int clearApplicantDataSubmittedBefore(Instant cutoff) {
        throw new UnsupportedOperationException("Not implemented: ApplicationRepository.clearApplicantDataSubmittedBefore");
    }
}
