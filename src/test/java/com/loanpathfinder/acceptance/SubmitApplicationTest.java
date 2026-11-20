package com.loanpathfinder.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

/** test_plan.md rows AC-001, AC-002 and the FR-001 / FR-011 extras. */
class SubmitApplicationTest extends AcceptanceTestBase {

    @Test
    void test_AC001_validRequestIsAccepted() throws Exception {
        MockHttpSession session = new MockHttpSession();
        long start = System.nanoTime();
        MvcResult submitted = submit(session, validForm());
        assertEquals(201, status(submitted));
        String id = body(submitted).get("applicationId").asText();
        assertTrue(id.length() >= 10, "application ID should be random and at least 10 characters");

        MvcResult shown = result(session, id);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        assertEquals(200, status(shown));
        assertTrue(elapsedMs < 3000, "result should appear within 3 seconds");
        JsonNode r = body(shown);
        assertEquals(id, r.get("applicationId").asText());
        assertEquals(DISCLAIMER, r.get("disclaimer").asText());
        assertEquals("LIKELY_QUALIFIED", r.get("recommendation").asText());
        assertEquals(0, new BigDecimal("23.7").compareTo(r.get("dtiPercent").decimalValue()), "DTI should be 23.7");
        assertEquals("GOOD", r.get("creditBand").asText());
        assertEquals(1, r.get("ruleSetVersion").asInt());
        assertEquals(1, pendingCount(), "exactly one Pending application should exist");
    }

    @Test
    void test_AC002_creditScoreOutOfRangeIsRejected() throws Exception {
        MockHttpSession session = new MockHttpSession();
        MvcResult rejected = submit(session, with(validForm(), "creditScore", 900));
        assertFieldRejected(rejected, "creditScore");
        assertEquals("Credit score must be between 300 and 850",
                body(rejected).get("fieldErrors").get("creditScore").asText());
        assertEquals(0, pendingCount(), "no application should be saved");

        // Control: same form with an in-range score is accepted.
        assertEquals(201, status(submit(session, with(validForm(), "creditScore", 850))));
    }

    @Test
    void test_AC002_creditScoreLimitsInsideAndOutside() throws Exception {
        MockHttpSession session = new MockHttpSession();
        assertFieldRejected(submit(session, with(validForm(), "creditScore", 299)), "creditScore");
        assertFieldRejected(submit(session, with(validForm(), "creditScore", 851)), "creditScore");
        assertEquals(0, pendingCount(), "rejected requests should create nothing");
        assertEquals(201, status(submit(session, with(validForm(), "creditScore", 300))));
        assertEquals(201, status(submit(session, with(validForm(), "creditScore", 850))));
        assertEquals(2, pendingCount());
    }

    /** Extra (FR-001, Domain Model field limits): each field just inside and just outside its limit. */
    @Test
    void test_AC002x_fieldLimitsInsideAndOutside() throws Exception {
        MockHttpSession session = new MockHttpSession();
        Object[][] outside = {
                {"annualIncome", new BigDecimal("0")},
                {"annualIncome", new BigDecimal("1000000000")},
                {"monthlyDebt", new BigDecimal("-0.01")},
                {"monthlyDebt", new BigDecimal("10000000")},
                {"loanAmount", new BigDecimal("0")},
                {"loanAmount", new BigDecimal("10000000")},
                {"termMonths", 13},
                {"termMonths", 84},
                {"email", "a".repeat(243) + "@example.com"},
                {"email", "not-an-email"},
        };
        for (Object[] c : outside) {
            assertFieldRejected(submit(session, with(validForm(), (String) c[0], c[1])), (String) c[0]);
        }
        assertEquals(0, pendingCount(), "rejected requests should create nothing");

        Object[][] inside = {
                {"annualIncome", new BigDecimal("0.01")},
                {"annualIncome", new BigDecimal("999999999")},
                {"monthlyDebt", new BigDecimal("0")},
                {"monthlyDebt", new BigDecimal("9999999")},
                {"loanAmount", new BigDecimal("0.01")},
                {"loanAmount", new BigDecimal("9999999")},
                {"termMonths", 12},
                {"termMonths", 72},
                {"email", "a".repeat(242) + "@example.com"},
        };
        for (Object[] c : inside) {
            assertEquals(201, status(submit(session, with(validForm(), (String) c[0], c[1]))),
                    c[0] + "=" + c[1] + " should be accepted");
        }
    }

    /** Extra (FR-001): the acknowledgement is required. */
    @Test
    void test_AC002x_acknowledgementRequired() throws Exception {
        MockHttpSession session = new MockHttpSession();
        assertFieldRejected(submit(session, with(validForm(), "acknowledged", false)), "acknowledged");
        assertFieldRejected(submit(session, with(validForm(), "acknowledged", null)), "acknowledged");
        assertEquals(0, pendingCount());
        assertEquals(201, status(submit(session, validForm())));
    }

    /** Extra (FR-001 input types): a decimal credit score and a missing field are rejected. */
    @Test
    void test_AC002x_inputTypes() throws Exception {
        MockHttpSession session = new MockHttpSession();
        MvcResult decimalScore = submitRaw(session, "{\"annualIncome\":62000,\"monthlyDebt\":850,\"creditScore\":700.5,"
                + "\"loanAmount\":15000,\"termMonths\":48,\"acknowledged\":true}");
        assertEquals(400, status(decimalScore), "credit score 700.5 should be rejected");
        assertFieldRejected(submit(session, with(validForm(), "monthlyDebt", null)), "monthlyDebt");
        assertEquals(0, pendingCount());
        assertEquals(201, status(submit(session, with(validForm(), "creditScore", 700))));
    }

    /** Extra (FR-011): extra fields like zip_code are ignored and never stored. */
    @Test
    void test_AC002x_extraFieldsIgnored() throws Exception {
        MockHttpSession session = new MockHttpSession();
        String id = submitOk(session, with(validForm(), "zip_code", "68102"));
        JsonNode stored = caseView(id).get("application");
        assertFalse(stored.has("zip_code"), "zip_code should not be stored");
        assertFalse(stored.toString().contains("68102"), "zip code value should not appear anywhere");
    }

    /** Extra (NFR-005, FR-001): the 21st submission from one IP in an hour is rejected. */
    @Test
    void test_AC002x_rateLimit() throws Exception {
        MockHttpSession session = new MockHttpSession();
        for (int i = 1; i <= 20; i++) {
            assertEquals(201, status(submit(session, validForm())), "submission " + i + " should be accepted");
        }
        MvcResult limited = submit(session, validForm());
        assertEquals(429, status(limited));
        assertEquals("rate_limited", body(limited).get("error").asText());
        assertEquals("Too many requests, please try again later", body(limited).get("message").asText());
        assertEquals(20, pendingCount(), "the rejected submission should create nothing");

        // Control: after the hour has passed, the same request is accepted.
        clock.advance(Duration.ofMinutes(61));
        assertEquals(201, status(submit(session, validForm())));
    }

    /** Extra (FR-011, NFR-001 identity): another browser session cannot see my result. */
    @Test
    void test_AC002x_resultsArePrivateToSession() throws Exception {
        MockHttpSession mine = new MockHttpSession();
        MockHttpSession other = new MockHttpSession();
        String id = submitOk(mine, validForm());

        MvcResult denied = result(other, id);
        assertEquals(404, status(denied));
        assertEquals("Result not found", body(denied).get("message").asText());

        // Control: the session that created it can see it.
        assertEquals(200, status(result(mine, id)));
    }
}
