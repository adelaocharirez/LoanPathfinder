package com.loanpathfinder.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

/** test_plan.md rows AC-004 to AC-008 and their extras. */
class ReviewAndDecisionTest extends AcceptanceTestBase {

    @Test
    void test_AC004_editAndResubmitSupersedesOldApplication() throws Exception {
        MockHttpSession session = new MockHttpSession();
        String oldId = submitOk(session, validForm());

        MvcResult prefill = mvc.perform(get("/api/applications/{id}/form", oldId).session(session)).andReturn();
        assertEquals(200, status(prefill));
        assertEquals(0, body(prefill).get("monthlyDebt").decimalValue().compareTo(new java.math.BigDecimal("850")));

        MvcResult resubmitted = mvc.perform(post("/api/applications/{id}/resubmit", oldId).session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(with(validForm(), "monthlyDebt", new java.math.BigDecimal("300"))))).andReturn();
        assertEquals(201, status(resubmitted));
        JsonNode b = body(resubmitted);
        String newId = b.get("applicationId").asText();
        assertTrue(!newId.equals(oldId), "resubmitting should create a new ID");
        assertEquals(oldId, b.get("supersededApplicationId").asText());

        assertEquals("SUPERSEDED", caseView(oldId).get("application").get("status").asText());
        JsonNode rows = queue("").get("rows");
        assertEquals(1, rows.size(), "only the new application should be in the default queue");
        assertEquals(newId, rows.get(0).get("applicationId").asText());
    }

    @Test
    void test_AC005_queueDefaultFilterSearchAndOpen() throws Exception {
        MockHttpSession s = new MockHttpSession();
        String p1 = submitOk(s, needsReviewForm());
        clock.advance(Duration.ofHours(1));
        String p2 = submitOk(s, form("62000", "850", 750, "15000", 48));
        String confirmed = submitOk(s, needsReviewForm());
        String overridden = submitOk(s, needsReviewForm());
        assertEquals(200, status(decide(confirmed, confirm())));
        assertEquals(200, status(decide(overridden, override("LIKELY_QUALIFIED", "Applicant documented a pending debt payoff"))));

        JsonNode q = queue("");
        assertEquals(2, q.get("total").asInt(), "default queue shows only Pending");
        assertEquals(p1, q.get("rows").get(0).get("applicationId").asText(), "oldest first");
        assertEquals(p2, q.get("rows").get(1).get("applicationId").asText());
        JsonNode row = q.get("rows").get(0);
        for (String field : new String[] {"applicationId", "submittedAt", "recommendation", "principalReason", "status"}) {
            assertTrue(row.has(field), "row should show " + field);
        }

        JsonNode filtered = queue("?recommendation=NEEDS_REVIEW");
        assertEquals(1, filtered.get("total").asInt());
        assertEquals("NEEDS_REVIEW", filtered.get("rows").get(0).get("recommendation").asText());

        JsonNode searched = queue("?search=" + p2);
        assertEquals(1, searched.get("total").asInt());
        assertEquals(p2, searched.get("rows").get(0).get("applicationId").asText());

        assertEquals(p1, caseView(p1).get("application").get("id").asText());
    }

    /** Extra (identity, NFR-001): staff routes need a signed-in user with the right role. */
    @Test
    void test_AC005x_queueRequiresLoanOfficer() throws Exception {
        assertEquals(401, status(mvc.perform(get("/api/review/queue")).andReturn()));
        assertEquals(403, status(mvc.perform(asAuditor(get("/api/review/queue"))).andReturn()));
        assertEquals(200, status(mvc.perform(asOfficer(get("/api/review/queue"))).andReturn()));
    }

    @Test
    void test_AC006_confirmRecommendation() throws Exception {
        String id = submitOk(new MockHttpSession(), needsReviewForm());
        MvcResult r = decide(id, confirm());
        assertEquals(200, status(r));
        assertEquals("NEEDS_REVIEW", body(r).get("finalOutcome").asText());
        assertEquals("CONFIRMED", caseView(id).get("application").get("status").asText());
        JsonNode last = lastEntry(auditTrail(id));
        assertEquals("confirm", last.get("action").asText());
        assertEquals(OFFICER, last.get("actor").asText());
    }

    @Test
    void test_AC007_overrideWithoutValidJustificationIsBlocked() throws Exception {
        MockHttpSession s = new MockHttpSession();
        String[] bad = {"", "too short", "   too short   ", "x".repeat(1001)};
        for (String text : bad) {
            String id = submitOk(s, needsReviewForm());
            MvcResult r = decide(id, override("LIKELY_QUALIFIED", text));
            assertFieldRejected(r, "justification");
            if (text.length() <= 1000) {
                assertEquals("A justification of at least 10 characters is required to override",
                        body(r).get("fieldErrors").get("justification").asText());
            }
            JsonNode c = caseView(id);
            assertEquals("PENDING", c.get("application").get("status").asText(), "no decision should be recorded");
            assertTrue(c.get("decision").isNull(), "no decision should be recorded");
        }
        // Controls and inside limits: exactly 10 characters and exactly 1,000 characters are accepted.
        assertEquals(200, status(decide(submitOk(s, needsReviewForm()), override("LIKELY_QUALIFIED", "abcdefghij"))));
        assertEquals(200, status(decide(submitOk(s, needsReviewForm()), override("LIKELY_QUALIFIED", "x".repeat(1000)))));
    }

    /** Extra (FR-005, Domain Model: OfficerDecision): the new outcome must differ from the recommendation. */
    @Test
    void test_AC007x_overrideToSameOutcomeIsBlocked() throws Exception {
        String id = submitOk(new MockHttpSession(), needsReviewForm());
        assertFieldRejected(decide(id, override("NEEDS_REVIEW", "Applicant documented a pending debt payoff")), "newOutcome");
        assertEquals("PENDING", caseView(id).get("application").get("status").asText());
        assertEquals(200, status(decide(id, override("UNLIKELY_QUALIFIED", "Applicant documented a pending debt payoff"))));
    }

    @Test
    void test_AC008_overrideIsRecordedAndLogged() throws Exception {
        String id = submitOk(new MockHttpSession(), needsReviewForm());
        String justification = "Applicant documented a pending debt payoff";
        assertEquals(200, status(decide(id, override("LIKELY_QUALIFIED", justification))));

        JsonNode c = caseView(id);
        assertEquals("OVERRIDDEN", c.get("application").get("status").asText());
        assertEquals("NEEDS_REVIEW", c.get("application").get("recommendation").asText(), "original recommendation unchanged");
        assertEquals("LIKELY_QUALIFIED", c.get("decision").get("finalOutcome").asText());

        JsonNode last = lastEntry(auditTrail(id));
        assertEquals("override", last.get("action").asText());
        assertEquals(OFFICER, last.get("actor").asText());
        assertEquals("NEEDS_REVIEW", last.get("beforeData").asText());
        assertEquals("LIKELY_QUALIFIED", last.get("afterData").asText());
        assertEquals(justification, last.get("comment").asText());
    }

    /** Extra (concurrency/state, FR-005): a second decision on the same case is rejected. */
    @Test
    void test_AC008x_secondDecisionIsRejected() throws Exception {
        String id = submitOk(new MockHttpSession(), needsReviewForm());
        assertEquals(200, status(decide(id, confirm())));
        MvcResult second = mvc.perform(asOfficer2(post("/api/review/cases/{id}/decision", id))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(confirm()))).andReturn();
        assertEquals(409, status(second));
        assertEquals("This case was already decided", body(second).get("message").asText());
        assertEquals(OFFICER, caseView(id).get("decision").get("officerId").asText(), "first decision wins");
    }

    /** Extra (identity, Domain Model: Compliance Auditor is read-only): auditors cannot decide. */
    @Test
    void test_AC008x_auditorCannotDecide() throws Exception {
        String id = submitOk(new MockHttpSession(), needsReviewForm());
        MvcResult r = mvc.perform(asAuditor(post("/api/review/cases/{id}/decision", id))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(confirm()))).andReturn();
        assertEquals(403, status(r));
        assertEquals("PENDING", caseView(id).get("application").get("status").asText());
        assertEquals(200, status(decide(id, confirm())), "control: a loan officer can decide");
    }

    private JsonNode lastEntry(JsonNode entries) {
        assertTrue(entries.size() > 0, "audit trail should not be empty");
        return entries.get(entries.size() - 1);
    }
}
