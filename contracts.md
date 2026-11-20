# Loan Pathfinder Contracts

These are the agreed calling interfaces for the skeleton (Java 21, Spring Boot 3.3). Every public function currently throws `UnsupportedOperationException`, and every route returns HTTP 501 with `{"error": "not_implemented", "message": "..."}`.

**Notes**
- **Injected:** a `java.time.Clock` (from `ClockConfig`, replaced by a test clock in tests) and an `EmailGateway` (SMTP in production, a recording fake in tests).
- **Staff sign-in (temporary):** until SSO (Spring Security OIDC) is added, staff identity comes from the `X-Staff-Id` and `X-Staff-Role` headers. Valid roles are `LOAN_OFFICER`, `COMPLIANCE_AUDITOR`, and `LENDER_ADMIN`.
- **Shared value types:** records and enums such as `RuleSet`, `ReasonRow`, `LoanApplication`, and `AuditEntry` are passed between modules. The "Uses" column in architecture.md refers to which services call which.
- **Not yet in the skeleton:** the PostgreSQL, JPA, and Flyway setup. The repositories are plain classes for now.

## 1. Public functions

| Module | Signature | Success result | How it fails | Spec rule | Injected |
|---|---|---|---|---|---|
| scoring | `ScoreResult ScoringEngine.score(ScoringInput input, RuleSet ruleSet)` | Payment, DTI (rounded HALF_UP to 1 decimal), band, recommendation, 2 reason rows (Credit score, then DTI), principal reason, rule set version | Never fails for validated input | FR-002, FR-003, Domain Model: DTI, ScoringRule, ExplanationReason; NFR-003 | none (pure) |
| rules | `RuleSet RuleSetService.getActive()` | The single active version (v1 is seeded) | none | Domain Model: RuleSet | Clock |
| rules | `RuleSet RuleSetService.publish(RuleSetDraft draft, String actorId)` | The new version, now active; writes a `rule_set_published` audit entry | `InvalidRuleSetException` | FR-008, NFR-008 | Clock |
| intake | `SubmissionResult IntakeService.submit(ApplicationForm form)` | New random ID (10 or more characters), recommendation; saved as PENDING with a `scored` audit entry | `InvalidApplicationException` (field to message) | FR-001, FR-002 | Clock |
| intake | `SubmissionResult IntakeService.resubmit(String previousApplicationId, ApplicationForm form)` | New application; the previous one becomes SUPERSEDED if it was PENDING (`superseded` audit entry) | `InvalidApplicationException`, `ApplicationNotFoundException` | FR-007 | Clock |
| intake | `ApplicationResult IntakeService.getResult(String applicationId)` | ID, recommendation, DTI, band, reasons, principal reason, disclaimer, rule set version | `ApplicationNotFoundException` | FR-002, FR-003 | none |
| intake | `ApplicationForm IntakeService.getSubmittedForm(String applicationId)` | The submitted values | `ApplicationNotFoundException` (also after retention) | FR-007 | none |
| review | `QueuePage ReviewService.listQueue(QueueQuery query)` | Rows (ID, submittedAt, recommendation, principal reason, status) and total; status defaults to PENDING, oldest first | none | FR-004 | none |
| review | `CaseView ReviewService.getCase(String applicationId)` | Application, decision (or null), audit trail | `CaseNotFoundException` | FR-005 | none |
| review | `DecisionRecord ReviewService.recordDecision(String applicationId, DecisionRequest request, String officerId)` | Decision saved, status CONFIRMED or OVERRIDDEN, `confirm` or `override` audit entry in the same transaction, then an email after commit | `InvalidDecisionException` (justification under 10 or over 1,000 characters after trimming, or same outcome), `CaseAlreadyDecidedException`, `CaseNotFoundException` | FR-005, FR-006, Domain Model: OfficerDecision | Clock |
| audit | `AuditEntry AuditLog.append(AuditEvent event)` | Entry with a sequence number and `clock.instant()` timestamp | none | FR-006, NFR-002 | Clock |
| audit | `List<AuditEntry> AuditLog.trail(String applicationId)` | Entries for one application, oldest first | none | FR-006 | none |
| audit | `List<AuditEntry> AuditLog.search(AuditQuery query)` | Entries matching every non-null filter | none | FR-010 | none |
| audit | `String AuditLog.exportCsv(AuditQuery query)` | CSV with header `sequence,timestamp,actor,applicationId,action,ruleSetVersion,beforeData,afterData,comment`; cells starting with `=`, `+`, `-`, or `@` get a leading `'` | none | FR-010, NFR-005 | none |
| notification | `NotificationResult DecisionNotifier.notifyDecision(String applicationId)` | SENT (one email, `email_sent` entry) or SKIPPED (no address) | FAILED status (`email_failed` entry); never throws to the caller | FR-009, NFR-007 | Clock, EmailGateway |
| notification | `int DecisionNotifier.retryFailed()` | Number of failed emails resent (under 24 hours old) | none | NFR-007 | Clock, EmailGateway |
| security | `StaffUser AccessControl.currentUser(String staffIdHeader, String staffRoleHeader)` | The signed-in staff user | `AccessDeniedException(authenticated=false)` mapped to 401 | NFR-001 | none |
| security | `void AccessControl.requireRole(StaffUser user, Role required)` | returns normally | `AccessDeniedException(authenticated=true)` mapped to 403 | NFR-001 | none |
| security | `void SessionOwnership.claim(String sessionId, String applicationId)` / `boolean owns(...)` | Remembers which applications each session created, and checks it | none | FR-011, NFR-001 | none |
| security | `void SubmissionRateLimiter.checkAndRecord(String clientIp)` | Records the submission | `RateLimitExceededException` on the 21st in a rolling hour (rejected requests are not counted) | NFR-005 | Clock |
| retention | `int RetentionJob.purgeExpired()` | Count of applications whose inputs and email were cleared; audit entries are kept | none | FR-011, NFR-009 | Clock |
| persistence | `ApplicationRepository.save / findById / findAll / clearApplicantDataSubmittedBefore`, `DecisionRepository.insert / findByApplicationId`, `AuditEntryRepository.insert / findAll`, `RuleSetRepository.findActive / insertAndActivate` | Stored or loaded records. No update or delete for audit entries | Unique-key or optimistic-lock failure (shown as 409 by review) | NFR-002, Domain Model | none |

## 2. REST API

**Error body:** `{"error": code, "message": text}`, plus `"fieldErrors": {field: message}` for code `validation_failed`.

**Error codes:**
- 400 `validation_failed`
- 401 `unauthenticated`
- 403 `forbidden`
- 404 `not_found` (the applicant sees "Result not found")
- 409 `already_decided` ("This case was already decided")
- 429 `rate_limited` ("Too many requests, please try again later")

| Requirement | Method and path | Calls | Success | Errors |
|---|---|---|---|---|
| FR-001 | `POST /api/applications` (JSON `ApplicationForm`) | `SubmissionRateLimiter.checkAndRecord`, `IntakeService.submit`, `SessionOwnership.claim` | 201 `{applicationId, recommendation, supersededApplicationId:null}` | 400, 429 |
| FR-002 | `GET /api/applications/{id}/result` | `SessionOwnership.owns`, `IntakeService.getResult` | 200 `ApplicationResult` | 404 |
| FR-003 | `GET /api/applications/{id}/reasons` | `SessionOwnership.owns`, `IntakeService.getResult` | 200 `{applicationId, principalReason, reasons[]}` | 404 |
| FR-007 | `GET /api/applications/{id}/form` | `SessionOwnership.owns`, `IntakeService.getSubmittedForm` | 200 `ApplicationForm` | 404 |
| FR-007 | `POST /api/applications/{id}/resubmit` | `SubmissionRateLimiter.checkAndRecord`, `SessionOwnership.owns`, `IntakeService.resubmit`, `SessionOwnership.claim` | 201 `{applicationId, recommendation, supersededApplicationId}` | 400, 404, 429 |
| FR-004 | `GET /api/review/queue?status&recommendation&from&to&search&sort&page` | `AccessControl` (LOAN_OFFICER), `ReviewService.listQueue` | 200 `{rows[], page, total}` | 401, 403 |
| FR-005 | `GET /api/review/cases/{id}` | `AccessControl` (LOAN_OFFICER), `ReviewService.getCase` | 200 `{application, decision, auditTrail[]}` | 401, 403, 404 |
| FR-005 | `POST /api/review/cases/{id}/decision` (JSON `DecisionRequest`) | `AccessControl` (LOAN_OFFICER), `ReviewService.recordDecision` | 200 `DecisionRecord` | 400, 401, 403, 404, 409 |
| FR-006 | `GET /api/audit/applications/{id}/entries` | `AccessControl` (COMPLIANCE_AUDITOR), `AuditLog.trail` | 200 `{entries[]}` | 401, 403 |
| FR-010 | `GET /api/audit/entries?applicationId&from&to&action&actor` | `AccessControl` (COMPLIANCE_AUDITOR), `AuditLog.search` | 200 `{entries[]}` | 401, 403 |
| FR-010 | `GET /api/audit/entries/export?...same filters` | `AccessControl` (COMPLIANCE_AUDITOR), `AuditLog.exportCsv` | 200 `text/csv` | 401, 403 |

**Requirements with no route, on purpose:**
- **FR-008:** post-MVP. In the MVP, new rule sets are published by the developer (architecture.md section 4), so tests call `RuleSetService.publish` directly.
- **FR-009:** the email is triggered by recording a decision, not by a request.
- **FR-011:** this requirement is enforced inside the other routes (validation, session ownership) and by the daily `RetentionJob`.

## 3. Start the application

```bash
# needs Java 21 and Maven 3.9 or newer (macOS: brew install openjdk@21 maven)
mvn spring-boot:run
# in another terminal: every route returns 501 for now
curl -i -X POST localhost:8080/api/applications -H 'Content-Type: application/json' -d '{"annualIncome":62000,"monthlyDebt":850,"creditScore":690,"loanAmount":15000,"termMonths":48,"acknowledged":true}'
# run the tests
mvn test
```
