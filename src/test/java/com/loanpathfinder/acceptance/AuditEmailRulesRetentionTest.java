package com.loanpathfinder.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.fasterxml.jackson.databind.JsonNode;
import com.loanpathfinder.retention.RetentionJob;
import com.loanpathfinder.rules.RuleSetDraft;
import com.loanpathfinder.rules.RuleSetService;
import com.loanpathfinder.scoring.RuleSet;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

/** test_plan.md rows AC-009 to AC-013. */
class AuditEmailRulesRetentionTest extends AcceptanceTestBase {

    @Autowired
    private RuleSetService ruleSets;

    @Autowired
    private RetentionJob retentionJob;

    @Test
    void test_AC009_scoringWritesCompleteAuditEntry() throws Exception {
        String id = submitOk(new MockHttpSession(), validForm());
        JsonNode e = auditTrail(id).get(0);
        assertEquals("scored", e.get("action").asText());
        assertEquals("system", e.get("actor").asText());
        assertEquals(id, e.get("applicationId").asText());
        assertEquals("LIKELY_QUALIFIED", e.get("afterData").asText());
        assertEquals(1, e.get("ruleSetVersion").asInt());
        assertEquals(TestDoubles.NOW, Instant.parse(e.get("timestamp").asText()));
    }

    @Test
    void test_AC009_auditEntriesCannotBeEditedOrDeleted() throws Exception {
        String id = submitOk(new MockHttpSession(), validForm());
        JsonNode before = auditTrail(id);
        long seq = before.get(0).get("sequence").asLong();
        int put1 = status(mvc.perform(asAuditor(put("/api/audit/entries/{seq}", seq))).andReturn());
        int del1 = status(mvc.perform(asAuditor(delete("/api/audit/entries/{seq}", seq))).andReturn());
        int del2 = status(mvc.perform(asOfficer(delete("/api/audit/entries/{seq}", seq))).andReturn());
        assertTrue(put1 >= 400 && del1 >= 400 && del2 >= 400, "edit and delete should be refused");
        assertEquals(before.toString(), auditTrail(id).toString(), "entries should be unchanged");
    }

    @Test
    void test_AC010_searchAndExportAuditLog() throws Exception {
        MockHttpSession s = new MockHttpSession();
        clock.set(Instant.parse("2026-10-15T15:00:00Z"));
        String oct = submitOk(s, needsReviewForm());
        assertEquals(200, status(decide(oct, override("LIKELY_QUALIFIED", "=SUM(A1:A9) documented payoff"))));
        clock.set(Instant.parse("2026-11-05T15:00:00Z"));
        String nov = submitOk(s, needsReviewForm());
        assertEquals(200, status(decide(nov, override("LIKELY_QUALIFIED", "Applicant documented a pending debt payoff"))));

        MvcResult search = mvc.perform(asAuditor(get("/api/audit/entries")
                .param("action", "override").param("from", "2026-10-01").param("to", "2026-10-31"))).andReturn();
        assertEquals(200, status(search));
        JsonNode entries = body(search).get("entries");
        assertEquals(1, entries.size(), "only the October override should match");
        assertEquals(oct, entries.get(0).get("applicationId").asText());

        MvcResult csv = mvc.perform(asAuditor(get("/api/audit/entries/export")
                .param("action", "override").param("from", "2026-10-01").param("to", "2026-10-31"))).andReturn();
        assertEquals(200, status(csv));
        assertTrue(csv.getResponse().getContentType().startsWith("text/csv"));
        String[] lines = csv.getResponse().getContentAsString().trim().split("\n");
        assertEquals("sequence,timestamp,actor,applicationId,action,ruleSetVersion,beforeData,afterData,comment", lines[0].trim());
        assertEquals(2, lines.length, "header plus one matching entry");
        assertTrue(lines[1].contains("'=SUM(A1:A9) documented payoff"), "formula cell should be escaped with a leading apostrophe");
        assertFalse(lines[1].contains(",=SUM"), "no cell should start with =");
    }

    /** Extra (identity): loan officers cannot read or export the audit log. */
    @Test
    void test_AC010x_auditLogRequiresAuditor() throws Exception {
        assertEquals(403, status(mvc.perform(asOfficer(get("/api/audit/entries"))).andReturn()));
        assertEquals(200, status(mvc.perform(asAuditor(get("/api/audit/entries"))).andReturn()));
    }

    @Test
    void test_AC011_decisionEmailSentOnce() throws Exception {
        MockHttpSession s = new MockHttpSession();
        String withEmail = submitOk(s, with(needsReviewForm(), "email", "dana@example.com"));
        assertEquals(200, status(decide(withEmail, confirm())));
        assertEquals(1, emails.sent().size(), "exactly one email");
        RecordingEmailGateway.SentEmail sent = emails.sent().get(0);
        assertEquals("dana@example.com", sent.to());
        assertTrue(sent.body().contains(withEmail));
        assertTrue(sent.body().contains("Needs Review"));
        assertTrue(sent.body().contains(DISCLAIMER));
        assertEquals("email_sent", auditTrail(withEmail).get(auditTrail(withEmail).size() - 1).get("action").asText());

        String noEmail = submitOk(s, needsReviewForm());
        assertEquals(200, status(decide(noEmail, confirm())));
        assertEquals(1, emails.sent().size(), "no email when no address was given");
    }

    @Test
    void test_AC011_unfavorableEmailListsReasons() throws Exception {
        String id = submitOk(new MockHttpSession(), with(unlikelyForm(), "email", "dana@example.com"));
        assertEquals(200, status(decide(id, confirm())));
        String b = emails.sent().get(0).body();
        assertTrue(b.contains("Unlikely Qualified"));
        assertTrue(b.contains("Credit score") && b.contains("540"));
        assertTrue(b.contains("Debt-to-income ratio") && b.contains("23.7%"));
    }

    @Test
    void test_AC012_newRuleSetAppliesOnlyToNewApplications() throws Exception {
        MockHttpSession s = new MockHttpSession();
        String a = submitOk(s, validForm());
        RuleSet v2 = ruleSets.publish(new RuleSetDraft(new BigDecimal("9.0"), 580, new BigDecimal("43.0"),
                700, new BigDecimal("36.0"), 740, new BigDecimal("43.0")), "admin-1");
        assertEquals(2, v2.version());
        String b = submitOk(s, validForm());

        JsonNode rb = body(result(s, b));
        assertEquals(2, rb.get("ruleSetVersion").asInt());
        assertEquals("NEEDS_REVIEW", rb.get("recommendation").asText(), "score 690 is below the new 700 threshold");
        JsonNode ra = body(result(s, a));
        assertEquals(1, ra.get("ruleSetVersion").asInt());
        assertEquals("LIKELY_QUALIFIED", ra.get("recommendation").asText());

        MvcResult published = mvc.perform(asAuditor(get("/api/audit/entries").param("action", "rule_set_published"))).andReturn();
        assertEquals(1, body(published).get("entries").size());
    }

    @Test
    void test_AC013_applicantDataDeletedAfterRetention() throws Exception {
        MockHttpSession s = new MockHttpSession();
        clock.set(Instant.parse("2024-09-15T15:00:00Z"));
        String expired = submitOk(s, with(validForm(), "email", "old@example.com"));
        clock.set(Instant.parse("2024-10-25T15:00:00Z"));
        String kept = submitOk(s, with(validForm(), "email", "recent@example.com"));
        clock.set(TestDoubles.NOW);

        assertEquals(1, retentionJob.purgeExpired());

        JsonNode e = caseView(expired).get("application");
        assertTrue(e.get("annualIncome").isNull() && e.get("email").isNull(), "inputs and email deleted");
        assertTrue(auditTrail(expired).size() > 0, "audit entries remain");
        JsonNode k = caseView(kept).get("application");
        assertEquals("recent@example.com", k.get("email").asText(), "inside the retention period: kept");
    }
}
