package com.loanpathfinder.persistence;

import com.loanpathfinder.scoring.RuleSet;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** Module: persistence. Saves and loads data. */
@Repository
public class RuleSetRepository {

    public Optional<RuleSet> findActive() {
        throw new UnsupportedOperationException("Not implemented: RuleSetRepository.findActive");
    }

    /** Older versions are kept and never changed. */
    public RuleSet insertAndActivate(RuleSet ruleSet) {
        throw new UnsupportedOperationException("Not implemented: RuleSetRepository.insertAndActivate");
    }
}
