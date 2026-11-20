# Loan Pathfinder Test Plan

**How to read this plan**
- There's one row per acceptance criterion in spec.md. Rows marked **extra** come from the gap checklist and name the requirement they probe.
- Expected values were worked out by hand from the spec.
- Routes and functions are the ones in contracts.md. Test names follow `test_<ID>_...`.
- "Nothing created" means the officer queue `GET /api/review/queue?status=PENDING` still has the same `total`, or the case still shows status PENDING with no decision.

**Inputs used by the scoring cases (AC-003)**
- Annual income 120,000 (10,000 per month), and a loan of 1,200 over 12 months at 9.0%.
- The estimated payment is 1200 × 0.0075 / (1 − 1.0075^−12) = **104.94**.
- So DTI = (monthly debt + 104.94) / 10,000 × 100.
- For example, a monthly debt of 2,895.06 gives a DTI of 30.0%.

**AC-001 inputs:** income 62,000, debt 850, loan 15,000 over 48 months.
- Payment: 15000 × 0.0075 / (1 − 1.0075^−48) = **373.28**.
- DTI: (850 + 373.28) / 5,166.67 × 100 = 23.676, which rounds to **23.7%**.
- Score 690 is in the **Good** band, so the result is **Likely Qualified**.
- With score 655 the result is Needs Review, and with 540 it's Unlikely Qualified.

| ID | Requirement | Cases (request or call, then the expected status, body fields, and side effects) | Inside/outside | Control |
|---|---|---|---|---|
| AC-001 | FR-001, FR-002 | `POST /api/applications` with the AC-001 inputs and `acknowledged:true` should return **201** with an `applicationId` of 10 or more characters. Then `GET /result` should return **200** within 3 seconds with `recommendation=LIKELY_QUALIFIED`, `dtiPercent=23.7`, `creditBand=GOOD`, `ruleSetVersion=1`, and `disclaimer="This is a pre-qualification, not a loan offer or final decision."`. Side effect: the Pending count is 1. | Not applicable | Not applicable |
| AC-002 | FR-001 | `creditScore=900` should return **400** with `error=validation_failed` and `fieldErrors.creditScore="Credit score must be between 300 and 850"`, and nothing created. | 300 and 850 are accepted with **201**. 299 and 851 return **400** on `creditScore`, with nothing created. | The same form with `creditScore=850` returns **201**. |
| AC-002 (extra) | FR-001, Domain Model field limits | Each request is wrong in one field only and should return **400** naming that field, with nothing created. | `annualIncome`: 0.01 and 999,999,999 are accepted; 0 and 1,000,000,000 are rejected. `monthlyDebt`: 0 and 9,999,999 are accepted; −0.01 and 10,000,000 are rejected. `loanAmount`: 0.01 and 9,999,999 are accepted; 0 and 10,000,000 are rejected. `termMonths`: 12 and 72 are accepted; 13 and 84 are rejected. `email`: 254 characters is accepted; 255 characters and `not-an-email` are rejected. | The valid AC-001 form with only that field changed back to an inside value. |
| AC-002 (extra) | FR-001 (acknowledgement) | `acknowledged:false` should return **400** on `acknowledged`. A missing `acknowledged` should also return **400**. Nothing created. | Not applicable | The valid form returns **201**. |
| AC-002 (extra) | FR-001, input types | `creditScore:700.5` should return **400**. A missing `monthlyDebt` should return **400** on `monthlyDebt`. Nothing created. | Not applicable | `creditScore:700` returns **201**. |
| AC-002 (extra) | FR-011, extra fields | The valid form plus `"zip_code":"68102"` should return **201**. The case view should not contain `zip_code` or `68102`. | Not applicable | Not applicable |
| AC-002 (extra) | NFR-005, rate limit | 20 submissions from one IP should each return **201**. The 21st should return **429** with `error=rate_limited` and `message="Too many requests, please try again later"`, and the Pending count stays 20. | 20th accepted, 21st rejected. | After 61 minutes, the same request returns **201**. |
| AC-002 (extra) | FR-011, NFR-001 (identity) | Session A submits. Session B's `GET /result` should return **404** with `message="Result not found"`. | Not applicable | Session A's `GET /result` returns **200**. |
| AC-003 | FR-002, FR-003 | Call `ScoringEngine.score` with each row below. Each result should have 2 reason rows: "Credit score" with the score, and "Debt-to-income ratio" with the DTI and a "%" sign. Each row cites a `ruleId`. The four outline rows: 750 with debt 2,895.06 gives DTI **30.0**, EXCELLENT, Likely Qualified, principal "Meets guidelines". 655 with debt 3,995.06 gives **41.0**, FAIR, Needs Review, principal "Credit score". 540 with debt 2,895.06 gives **30.0**, POOR, Unlikely Qualified, principal "Credit score". 700 with debt 4,895.06 gives **50.0**, GOOD, Unlikely Qualified, principal "Debt-to-income ratio". Through the API, submitting 655 with debt 3,995.06 and calling `GET /reasons` should return both rows with `metGuideline=false` and the values "655" and "41.0%". | DTI 36.0% at score 670: debt 3,499.06 gives raw 36.04, which rounds to 36.0 and is Likely Qualified; debt 3,500.06 gives raw 36.05, which rounds to 36.1 and is Needs Review. DTI 43.0% at score 740: debt 4,199.06 gives 43.0 and is Likely Qualified; debt 4,200.06 gives 43.1 and is Unlikely Qualified. Score limits at DTI 30.0: 580 is Needs Review and 579 is Unlikely Qualified; 670 is Likely Qualified and 669 is Needs Review. At DTI 40.0: 740 is Likely Qualified and 739 is Needs Review. | Not applicable (no rejection) |
| AC-003 (extra) | NFR-003 (determinism) | Scoring the same input twice should give equal results. | Not applicable | Not applicable |
| AC-004 | FR-007 | Submit with debt 850, then `GET /{id}/form` should return **200** with `monthlyDebt=850`. `POST /{id}/resubmit` with debt 300 should return **201** with a new ID and `supersededApplicationId` set to the old ID. The old case should have status SUPERSEDED, and the default queue should show only the new ID. | Not applicable | Not applicable |
| AC-005 | FR-004 | Create p1 (655, Needs Review) at time T and p2 (750, Likely Qualified) at T+1h, plus one confirmed and one overridden application. The default queue should return **200** with `total=2` and rows in order [p1, p2]. Each row should have `applicationId`, `submittedAt`, `recommendation`, `principalReason`, and `status`. `?recommendation=NEEDS_REVIEW` should return 1 row. `?search=<p2>` should return only p2. `GET /cases/<p1>` should return **200** with `application.id=p1`. | Not applicable | Not applicable |
| AC-005 (extra) | NFR-001 (identity) | Calling the queue with no headers should return **401**. Calling it as an auditor should return **403**. | Not applicable | As an officer it returns **200**. |
| AC-006 | FR-005, FR-006 | A Needs Review case: `POST /decision {CONFIRM}` should return **200** with `finalOutcome=NEEDS_REVIEW`. The case status should be CONFIRMED, and the last audit entry should have `action=confirm` and `actor=jordan.lee`. | Not applicable | Not applicable |
| AC-007 | FR-005 | An override to Likely Qualified with justification "" or "too short" (9 characters) should return **400** with `fieldErrors.justification="A justification of at least 10 characters is required to override"`. The case stays PENDING with `decision=null`. | Trimming: "   too short   " is 9 characters after trimming and is rejected. "abcdefghij" (10 characters) is accepted. 1,000 characters is accepted and 1,001 is rejected. | The same override with "abcdefghij" returns **200**. |
| AC-007 (extra) | FR-005, Domain Model: OfficerDecision | An override to Needs Review (the same as the recommendation) should return **400** on `newOutcome` and leave the case PENDING. | Not applicable | An override to Unlikely Qualified on the same case returns **200**. |
| AC-008 | FR-005, FR-006 | An override to Likely Qualified with the justification "Applicant documented a pending debt payoff" should return **200**. The status should be OVERRIDDEN, `application.recommendation` should still be NEEDS_REVIEW, and `decision.finalOutcome` should be LIKELY_QUALIFIED. The last audit entry should have `action=override`, `actor=jordan.lee`, `beforeData=NEEDS_REVIEW`, `afterData=LIKELY_QUALIFIED`, and `comment` equal to the justification. | Not applicable | Not applicable |
| AC-008 (extra) | FR-005 (state, concurrency) | Officer 1 confirms and gets **200**. Then officer 2 confirms the same case and should get **409** with "This case was already decided". `decision.officerId` should still be jordan.lee. | Not applicable | The first decision is the control. |
| AC-008 (extra) | Domain Model: Compliance Auditor is read-only | An auditor posting a decision should get **403**, and the case stays PENDING. | Not applicable | An officer's confirm returns **200**. |
| AC-009 | FR-006 | Submit, then the auditor's trail should have a first entry with `action=scored`, `actor=system`, the application ID, `afterData=LIKELY_QUALIFIED`, `ruleSetVersion=1`, and `timestamp=2026-11-20T15:00:00Z` (from the test clock). `PUT` and `DELETE /api/audit/entries/{seq}`, as an auditor or an officer, should return **4xx**, and the trail is unchanged. | Not applicable | Not applicable |
| AC-010 | FR-010 | An override on 2026-10-15 with a justification starting with "=SUM(A1:A9)", and another override on 2026-11-05. `GET /entries?action=override&from=2026-10-01&to=2026-10-31` should return 1 entry (the October one). The export should return **200** with `text/csv`, the header `sequence,timestamp,actor,applicationId,action,ruleSetVersion,beforeData,afterData,comment`, and 1 data row whose comment cell is `'=SUM(A1:A9) documented payoff`. No cell should start with `=`. | Date range: 10-15 is inside, 11-05 is outside. | Not applicable |
| AC-010 (extra) | NFR-001 (identity) | An officer calling `GET /entries` should get **403**. | Not applicable | An auditor gets **200**. |
| AC-011 | FR-009 | Confirm a case submitted with `dana@example.com`. There should be exactly 1 email, to dana@example.com, containing the ID, "Needs Review", and the disclaimer. The last audit entry should be `email_sent`. Confirming a case submitted with no email should send no new email. For a case with score 540 (Unlikely Qualified), the email should contain "Credit score", "540", "Debt-to-income ratio", and "23.7%". | Not applicable | The case with no email is the control for "exactly one". |
| AC-012 | FR-008 | Submit A under v1, which gives Likely Qualified. Then call `RuleSetService.publish` with `likelyMinScore=700`, which should return version 2. Then submit B with the same inputs. B's result should have `ruleSetVersion=2` and NEEDS_REVIEW, because 690 is below 700 and DTI is at most 43. A's result should still have `ruleSetVersion=1` and LIKELY_QUALIFIED. `GET /entries?action=rule_set_published` should return 1 entry. | Not applicable | Not applicable |
| AC-013 | FR-011, NFR-009 | Retention is 25 months and now is 2026-11-20, so the cutoff is 2024-10-20. Application X was submitted 2024-09-15 with an email, and Y was submitted 2024-10-25. `RetentionJob.purgeExpired()` should return **1**. X should have `annualIncome=null` and `email=null`, and its audit trail should still have entries. Y should keep its email. | Y (inside the retention period) is kept, and X (outside it) is cleared. | Not applicable |

## Gap checklist coverage
- **Boundaries:** the AC-002 extras and the AC-003, AC-007, and AC-013 inside/outside cases.
- **Time:** AC-001 (3 seconds), the rate-limit hour, the AC-010 date range, AC-013 retention, and the test clock for audit timestamps.
- **Identity:** session privacy, 401/403 checks, and the auditor not being allowed to decide.
- **State:** AC-004 superseding, AC-008 second decision.
- **Concurrency:** the second decision returns 409. True simultaneous requests aren't tested with MockMvc.
- **Input types:** the AC-002 input-type extra.
- **Errors and precedence:** each rejection is wrong in one way only, and every rejection has a control.
- **Listing:** AC-005 sorting, filtering, and search, and AC-010 search.
- **What is returned:** the body fields listed in every row.

## Spec gaps (not resolved here)
1. **FR-008 is post-MVP, but AC-012 tests it,** and the spec has no route or SSO role for the Lender Administrator. The test calls `RuleSetService.publish` directly.
2. **The DTI guideline text in a reason row isn't defined** (36.0% or 43.0% for scores of 740 and above), so the tests don't check the `guideline` text.
3. **The spec doesn't say whether the estimated payment is rounded before DTI is calculated.** The plan uses the unrounded payment, and none of the expected values change either way.
4. **A reference rate of 0% makes the amortization formula divide by zero.** The spec doesn't cover it.
5. **The audit entry's fields for an override's new outcome and justification aren't defined.** contracts.md adds a `comment` field so AC-008 and AC-010 can be tested.
6. **The exact CSV escaping method isn't stated.** The plan assumes a leading apostrophe.
7. **What happens to an application at exactly 25 months isn't stated,** so that case isn't tested.
8. **FR-011's encryption and HTTPS parts, and NFR-004 (performance) and NFR-006 (accessibility), have no acceptance criteria.** Only the session privacy, extra field, and retention parts of FR-011 are tested, as extras.
9. **The acknowledgement and rate-limit rules come from the Domain Model and NFR-005,** not from any acceptance criterion, so they're tested as extras.
