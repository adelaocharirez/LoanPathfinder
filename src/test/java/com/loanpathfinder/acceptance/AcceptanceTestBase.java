package com.loanpathfinder.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Shared setup and helpers. Every test calls only the routes listed in contracts.md. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestDoubles.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
abstract class AcceptanceTestBase {

    static final String DISCLAIMER = "This is a pre-qualification, not a loan offer or final decision.";
    static final String OFFICER = "jordan.lee";
    static final String OFFICER_2 = "sam.ortiz";
    static final String AUDITOR = "casey.ng";

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected MutableClock clock;

    @Autowired
    protected RecordingEmailGateway emails;

    protected final ObjectMapper json = new ObjectMapper();

    // ---------- forms

    /** A form with every field valid and the acknowledgement checked. */
    protected Map<String, Object> form(String income, String debt, int score, String loan, int term) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("annualIncome", new BigDecimal(income));
        f.put("monthlyDebt", new BigDecimal(debt));
        f.put("creditScore", score);
        f.put("loanAmount", new BigDecimal(loan));
        f.put("termMonths", term);
        f.put("acknowledged", true);
        return f;
    }

    /** AC-001 inputs: DTI 23.7%, Good band, Likely Qualified. */
    protected Map<String, Object> validForm() {
        return form("62000", "850", 690, "15000", 48);
    }

    /** Same inputs with credit score 655 (Fair band): Needs Review, principal reason "Credit score". */
    protected Map<String, Object> needsReviewForm() {
        return form("62000", "850", 655, "15000", 48);
    }

    /** Same inputs with credit score 540 (Poor band): Unlikely Qualified. */
    protected Map<String, Object> unlikelyForm() {
        return form("62000", "850", 540, "15000", 48);
    }

    /** Copy of a form with one field changed (or removed when value is null). */
    protected Map<String, Object> with(Map<String, Object> base, String field, Object value) {
        Map<String, Object> f = new LinkedHashMap<>(base);
        if (value == null) {
            f.remove(field);
        } else {
            f.put(field, value);
        }
        return f;
    }

    // ---------- applicant routes

    protected MvcResult submit(MockHttpSession session, Map<String, Object> form) throws Exception {
        return mvc.perform(post("/api/applications").session(session)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(form))).andReturn();
    }

    protected MvcResult submitRaw(MockHttpSession session, String rawJson) throws Exception {
        return mvc.perform(post("/api/applications").session(session)
                .contentType(MediaType.APPLICATION_JSON).content(rawJson)).andReturn();
    }

    /** Submits and asserts 201; returns the new application ID. */
    protected String submitOk(MockHttpSession session, Map<String, Object> form) throws Exception {
        MvcResult r = submit(session, form);
        assertEquals(201, r.getResponse().getStatus(), "submit should return 201 Created");
        return body(r).get("applicationId").asText();
    }

    protected MvcResult result(MockHttpSession session, String applicationId) throws Exception {
        return mvc.perform(get("/api/applications/{id}/result", applicationId).session(session)).andReturn();
    }

    protected MvcResult reasons(MockHttpSession session, String applicationId) throws Exception {
        return mvc.perform(get("/api/applications/{id}/reasons", applicationId).session(session)).andReturn();
    }

    // ---------- staff routes

    protected MockHttpServletRequestBuilder asOfficer(MockHttpServletRequestBuilder b) {
        return b.header("X-Staff-Id", OFFICER).header("X-Staff-Role", "LOAN_OFFICER");
    }

    protected MockHttpServletRequestBuilder asOfficer2(MockHttpServletRequestBuilder b) {
        return b.header("X-Staff-Id", OFFICER_2).header("X-Staff-Role", "LOAN_OFFICER");
    }

    protected MockHttpServletRequestBuilder asAuditor(MockHttpServletRequestBuilder b) {
        return b.header("X-Staff-Id", AUDITOR).header("X-Staff-Role", "COMPLIANCE_AUDITOR");
    }

    protected Map<String, Object> confirm() {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("decision", "CONFIRM");
        return d;
    }

    protected Map<String, Object> override(String newOutcome, String justification) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("decision", "OVERRIDE");
        d.put("newOutcome", newOutcome);
        d.put("justification", justification);
        return d;
    }

    protected MvcResult decide(String applicationId, Map<String, Object> decision) throws Exception {
        return mvc.perform(asOfficer(post("/api/review/cases/{id}/decision", applicationId))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(decision))).andReturn();
    }

    /** GET /api/review/queue as a loan officer with an optional query string; asserts 200. */
    protected JsonNode queue(String query) throws Exception {
        MvcResult r = mvc.perform(asOfficer(get("/api/review/queue" + query))).andReturn();
        assertEquals(200, r.getResponse().getStatus(), "queue should return 200");
        return body(r);
    }

    /** Number of Pending applications. Used to check that a rejected request created nothing. */
    protected long pendingCount() throws Exception {
        return queue("?status=PENDING").get("total").asLong();
    }

    /** GET /api/review/cases/{id} as a loan officer; asserts 200. */
    protected JsonNode caseView(String applicationId) throws Exception {
        MvcResult r = mvc.perform(asOfficer(get("/api/review/cases/{id}", applicationId))).andReturn();
        assertEquals(200, r.getResponse().getStatus(), "case view should return 200");
        return body(r);
    }

    /** GET /api/audit/applications/{id}/entries as a compliance auditor; asserts 200; returns the entries array. */
    protected JsonNode auditTrail(String applicationId) throws Exception {
        MvcResult r = mvc.perform(asAuditor(get("/api/audit/applications/{id}/entries", applicationId))).andReturn();
        assertEquals(200, r.getResponse().getStatus(), "audit trail should return 200");
        return body(r).get("entries");
    }

    protected JsonNode body(MvcResult r) throws Exception {
        return json.readTree(r.getResponse().getContentAsString());
    }

    protected int status(MvcResult r) {
        return r.getResponse().getStatus();
    }

    /** Asserts a 400 validation error naming exactly this field. */
    protected void assertFieldRejected(MvcResult r, String field) throws Exception {
        assertEquals(400, status(r), "expected 400 for invalid " + field);
        JsonNode b = body(r);
        assertEquals("validation_failed", b.get("error").asText());
        assertEquals(1, b.get("fieldErrors").size(), "exactly one field should be wrong");
        assertEquals(true, b.get("fieldErrors").has(field), "fieldErrors should name " + field);
    }
}
