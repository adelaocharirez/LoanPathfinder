package com.loanpathfinder.persistence;

import com.loanpathfinder.scoring.RuleSet;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** Module: persistence. Saves and loads data. */
@Repository
public class DecisionRepository {

    /** Fails if a decision already exists for the application (unique application_id). */
    public DecisionRecord insert(DecisionRecord decision) {
        throw new UnsupportedOperationException("Not implemented: DecisionRepository.insert");
    }

    public Optional<DecisionRecord> findByApplicationId(String applicationId) {
        throw new UnsupportedOperationException("Not implemented: DecisionRepository.findByApplicationId");
    }
}
