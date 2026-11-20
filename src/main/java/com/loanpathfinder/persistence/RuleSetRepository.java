package com.loanpathfinder.persistence;

import com.loanpathfinder.scoring.RuleSet;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** Module: persistence. In-memory store for the MVP (version 1 is seeded with the initial thresholds). */
@Repository
public class RuleSetRepository {

    private final List<RuleSet> versions = new ArrayList<>();

    public RuleSetRepository() {
        versions.add(new RuleSet(1, new BigDecimal("9.0"),
                580, new BigDecimal("43.0"),
                670, new BigDecimal("36.0"),
                740, new BigDecimal("43.0")));
    }

    public synchronized Optional<RuleSet> findActive() {
        return versions.isEmpty() ? Optional.empty() : Optional.of(versions.get(versions.size() - 1));
    }

    /** Older versions are kept and never changed. */
    public RuleSet insertAndActivate(RuleSet ruleSet) {
        throw new UnsupportedOperationException("Not implemented: RuleSetRepository.insertAndActivate");
    }
}
