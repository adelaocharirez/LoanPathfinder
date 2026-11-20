package com.loanpathfinder.persistence;

import com.loanpathfinder.scoring.RuleSet;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** Module: persistence. Saves and loads data. */
@Repository
public class ApplicationRepository {

    /** Inserts or updates; fails with an optimistic-lock error if the version is stale. */
    public LoanApplication save(LoanApplication application) {
        throw new UnsupportedOperationException("Not implemented: ApplicationRepository.save");
    }

    public Optional<LoanApplication> findById(String applicationId) {
        throw new UnsupportedOperationException("Not implemented: ApplicationRepository.findById");
    }

    public List<LoanApplication> findAll() {
        throw new UnsupportedOperationException("Not implemented: ApplicationRepository.findAll");
    }

    /** Nulls inputs and email; returns how many applications were cleared. */
    public int clearApplicantDataSubmittedBefore(Instant cutoff) {
        throw new UnsupportedOperationException("Not implemented: ApplicationRepository.clearApplicantDataSubmittedBefore");
    }
}
