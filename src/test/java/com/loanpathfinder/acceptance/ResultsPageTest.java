package com.loanpathfinder.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

/** test_plan.md row AC-003 through the REST routes (Results page). */
class ResultsPageTest extends AcceptanceTestBase {

    @Test
    void test_AC003_resultsPageShowsBothReasonRows() throws Exception {
        MockHttpSession session = new MockHttpSession();
        // Income 120,000, debt 3,995.06, loan 1,200 over 12 months: DTI 41.0%, score 655: Needs Review
        String id = submitOk(session, form("120000", "3995.06", 655, "1200", 12));

        MvcResult shown = reasons(session, id);
        assertEquals(200, status(shown));
        JsonNode b = body(shown);
        assertEquals("Credit score", b.get("principalReason").asText());
        JsonNode rows = b.get("reasons");
        assertEquals(2, rows.size());
        assertEquals("Credit score", rows.get(0).get("factor").asText());
        assertEquals("655", rows.get(0).get("applicantValue").asText());
        assertEquals(false, rows.get(0).get("metGuideline").asBoolean());
        assertEquals("Debt-to-income ratio", rows.get(1).get("factor").asText());
        assertEquals("41.0%", rows.get(1).get("applicantValue").asText());
        assertEquals(false, rows.get(1).get("metGuideline").asBoolean());

        JsonNode r = body(result(session, id));
        assertEquals("NEEDS_REVIEW", r.get("recommendation").asText());
    }
}
