package com.loanpathfinder.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.loanpathfinder.scoring.CreditBand;
import com.loanpathfinder.scoring.ReasonRow;
import com.loanpathfinder.scoring.Recommendation;
import com.loanpathfinder.scoring.RuleSet;
import com.loanpathfinder.scoring.ScoreResult;
import com.loanpathfinder.scoring.ScoringEngine;
import com.loanpathfinder.scoring.ScoringInput;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * test_plan.md row AC-003, through the ScoringEngine.score contract.
 * All cases use annual income 120,000 (10,000 per month) and a 1,200 loan over 12 months at 9.0%,
 * so the estimated payment is 104.94 and DTI = (monthly debt + 104.94) / 10,000 x 100.
 */
class ScoringRulesTest {

    private final ScoringEngine engine = new ScoringEngine();

    private static final RuleSet V1 = new RuleSet(1, new BigDecimal("9.0"),
            580, new BigDecimal("43.0"), 670, new BigDecimal("36.0"), 740, new BigDecimal("43.0"));

    private ScoreResult score(int creditScore, String monthlyDebt) {
        return engine.score(new ScoringInput(new BigDecimal("120000"), new BigDecimal(monthlyDebt),
                creditScore, new BigDecimal("1200"), 12), V1);
    }

    private void check(int creditScore, String debt, String dti, CreditBand band, Recommendation rec, String principal) {
        ScoreResult r = score(creditScore, debt);
        String label = "score " + creditScore + ", debt " + debt;
        assertEquals(0, new BigDecimal(dti).compareTo(r.dtiPercent()), label + ": DTI should be " + dti);
        assertEquals(band, r.creditBand(), label);
        assertEquals(rec, r.recommendation(), label);
        assertEquals(principal, r.principalReason(), label);
        assertEquals(1, r.ruleSetVersion(), label);
        assertEquals(2, r.reasons().size(), label + ": both factor rows are always shown");
        ReasonRow scoreRow = r.reasons().get(0);
        ReasonRow dtiRow = r.reasons().get(1);
        assertEquals("Credit score", scoreRow.factor());
        assertEquals(String.valueOf(creditScore), scoreRow.applicantValue());
        assertEquals("Debt-to-income ratio", dtiRow.factor());
        assertEquals(dti + "%", dtiRow.applicantValue());
        assertTrue(scoreRow.ruleId() != null && dtiRow.ruleId() != null, label + ": every reason cites a rule");
    }

    @Test
    void test_AC003_reasonsMatchScoringRules() {
        check(750, "2895.06", "30.0", CreditBand.EXCELLENT, Recommendation.LIKELY_QUALIFIED, "Meets guidelines");
        check(655, "3995.06", "41.0", CreditBand.FAIR, Recommendation.NEEDS_REVIEW, "Credit score");
        check(540, "2895.06", "30.0", CreditBand.POOR, Recommendation.UNLIKELY_QUALIFIED, "Credit score");
        check(700, "4895.06", "50.0", CreditBand.GOOD, Recommendation.UNLIKELY_QUALIFIED, "Debt-to-income ratio");
    }

    @Test
    void test_AC003_dtiThresholdsInsideAndOutside() {
        // 36.0% limit for Likely Qualified at score 670: 36.04 rounds to 36.0, 36.05 rounds to 36.1
        check(670, "3499.06", "36.0", CreditBand.GOOD, Recommendation.LIKELY_QUALIFIED, "Meets guidelines");
        check(670, "3500.06", "36.1", CreditBand.GOOD, Recommendation.NEEDS_REVIEW, "Debt-to-income ratio");
        // 43.0% limit at score 740: 43.04 rounds to 43.0, 43.05 rounds to 43.1
        check(740, "4199.06", "43.0", CreditBand.EXCELLENT, Recommendation.LIKELY_QUALIFIED, "Meets guidelines");
        check(740, "4200.06", "43.1", CreditBand.EXCELLENT, Recommendation.UNLIKELY_QUALIFIED, "Debt-to-income ratio");
    }

    @Test
    void test_AC003_creditScoreThresholdsInsideAndOutside() {
        check(580, "2895.06", "30.0", CreditBand.FAIR, Recommendation.NEEDS_REVIEW, "Credit score");
        check(579, "2895.06", "30.0", CreditBand.POOR, Recommendation.UNLIKELY_QUALIFIED, "Credit score");
        check(670, "2895.06", "30.0", CreditBand.GOOD, Recommendation.LIKELY_QUALIFIED, "Meets guidelines");
        check(669, "2895.06", "30.0", CreditBand.FAIR, Recommendation.NEEDS_REVIEW, "Credit score");
        check(740, "3895.06", "40.0", CreditBand.EXCELLENT, Recommendation.LIKELY_QUALIFIED, "Meets guidelines");
        check(739, "3895.06", "40.0", CreditBand.GOOD, Recommendation.NEEDS_REVIEW, "Debt-to-income ratio");
    }

    /** Extra (NFR-003): the same inputs and rule set always give the same result. */
    @Test
    void test_AC003x_scoringIsDeterministic() {
        assertEquals(score(655, "3995.06"), score(655, "3995.06"));
    }
}
