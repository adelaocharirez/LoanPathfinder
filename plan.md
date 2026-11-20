# Loan Pathfinder Implementation Plan

This plan is based on spec.md, architecture.md, contracts.md, test_plan.md, the test files, and the skeleton. No code has been implemented yet.

## Current test results

The last `mvn test` run gave `Tests run: 31, Failures: 27, Errors: 4, Skipped: 0`. Every test fails because the routes return 501 or the functions throw "Not implemented". Here are the failing tests grouped by acceptance criterion:

| Criterion | Failing tests |
|---|---|
| AC-001 | test_AC001_validRequestIsAccepted |
| AC-002 | test_AC002_creditScoreOutOfRangeIsRejected, test_AC002_creditScoreLimitsInsideAndOutside, test_AC002x_fieldLimitsInsideAndOutside, test_AC002x_acknowledgementRequired, test_AC002x_inputTypes, test_AC002x_extraFieldsIgnored, test_AC002x_rateLimit, test_AC002x_resultsArePrivateToSession |
| AC-003 | test_AC003_reasonsMatchScoringRules, test_AC003_dtiThresholdsInsideAndOutside, test_AC003_creditScoreThresholdsInsideAndOutside, test_AC003x_scoringIsDeterministic, test_AC003_resultsPageShowsBothReasonRows |
| AC-004 | test_AC004_editAndResubmitSupersedesOldApplication |
| AC-005 | test_AC005_queueDefaultFilterSearchAndOpen, test_AC005x_queueRequiresLoanOfficer |
| AC-006 | test_AC006_confirmRecommendation |
| AC-007 | test_AC007_overrideWithoutValidJustificationIsBlocked, test_AC007x_overrideToSameOutcomeIsBlocked |
| AC-008 | test_AC008_overrideIsRecordedAndLogged, test_AC008x_secondDecisionIsRejected, test_AC008x_auditorCannotDecide |
| AC-009 | test_AC009_scoringWritesCompleteAuditEntry, test_AC009_auditEntriesCannotBeEditedOrDeleted |
| AC-010 | test_AC010_searchAndExportAuditLog, test_AC010x_auditLogRequiresAuditor |
| AC-011 | test_AC011_decisionEmailSentOnce, test_AC011_unfavorableEmailListsReasons |
| AC-012 | test_AC012_newRuleSetAppliesOnlyToNewApplications |
| AC-013 | test_AC013_applicantDataDeletedAfterRetention |

Most HTTP tests use shared helpers from the test base class. For example, `pendingCount()` calls the officer queue, and `caseView()` and `auditTrail()` call the officer and auditor routes. Because of that, a test can only pass once every route its helpers use is working. The slice order below follows those dependencies.

## Slices

### Slice 1: Thin submit-and-view path
- **Goal:** An applicant can submit a form and view their own result, and no other browser session can view it.
- **Spec sources:** FR-001, FR-002, FR-011; Domain Model (LoanApplication, RuleSet, AuditLogEntry); architecture.md section 3.1.
- **Criteria:** none completed. This slice proves the request flow works end to end.
- **Tests that should turn green:** test_AC002x_resultsArePrivateToSession
- **Skeleton to fill in:**
  - Routes: `POST /api/applications` and `GET /api/applications/{id}/result`
  - `IntakeService.submit` (happy path only) and `IntakeService.getResult`
  - `RuleSetService.getActive` and `RuleSetRepository.findActive`, with version 1 seeded
  - `ScoringEngine.score`: a first version that only returns a result. Slice 2 adds the full rules.
  - `ApplicationRepository.save` and `findById`
  - `AuditLog.append` and `AuditEntryRepository.insert` (the "scored" entry)
  - `SessionOwnership.claim` and `owns`
  - `SubmissionRateLimiter.checkAndRecord` (record only, no limit yet)
- **Depends on:** none
- **Done when:** the listed test passes and no previously passing test fails.
- **Status:** done

### Slice 2: Scoring rules and reasons
- **Goal:** Applications get the correct DTI, credit band, recommendation, reason rows, and principal reason, including at the rounding and threshold boundaries.
- **Spec sources:** FR-002, FR-003; Domain Model (DTI, CreditScoreBand, ScoringRule, ExplanationReason); NFR-003.
- **Criteria:** AC-003
- **Tests that should turn green:** test_AC003_reasonsMatchScoringRules, test_AC003_dtiThresholdsInsideAndOutside, test_AC003_creditScoreThresholdsInsideAndOutside, test_AC003x_scoringIsDeterministic, test_AC003_resultsPageShowsBothReasonRows
- **Skeleton to fill in:**
  - `ScoringEngine.score`, the full version: amortized payment, DTI rounded HALF_UP to one decimal place, credit bands, the three rules applied in order, both reason rows, and the principal reason
  - Route: `GET /api/applications/{id}/reasons`
- **Depends on:** Slice 1
- **Done when:** the listed tests pass and no previously passing test fails.
- **Status:** not started

### Slice 3: Officer queue access
- **Goal:** Signed-in loan officers can list Pending applications. Requests with no identity get 401, and requests with the wrong role get 403.
- **Spec sources:** FR-004 (default view only), NFR-001; Domain Model (Actors).
- **Criteria:** AC-001
- **Tests that should turn green:** test_AC001_validRequestIsAccepted, test_AC005x_queueRequiresLoanOfficer
- **Skeleton to fill in:**
  - `AccessControl.currentUser` and `requireRole`
  - `ReviewService.listQueue`: the `status` filter (default PENDING), oldest first, and the total count
  - `ApplicationRepository.findAll`
  - Route: `GET /api/review/queue`
- **Depends on:** Slice 2
- **Done when:** the listed tests pass and no previously passing test fails.
- **Status:** not started

### Slice 4: Input validation
- **Goal:** Invalid forms are rejected with a 400 error that names the one bad field, and nothing is saved.
- **Spec sources:** FR-001; Domain Model (FinancialProfile and loan request field limits).
- **Criteria:** AC-002
- **Tests that should turn green:** test_AC002_creditScoreOutOfRangeIsRejected, test_AC002_creditScoreLimitsInsideAndOutside, test_AC002x_fieldLimitsInsideAndOutside, test_AC002x_acknowledgementRequired, test_AC002x_inputTypes
- **Skeleton to fill in:**
  - `IntakeService.submit`: validation that throws `InvalidApplicationException`
  - The `POST /api/applications` route: return `InvalidApplicationException` as a 400 `validation_failed` error with `fieldErrors`
- **Depends on:** Slice 3
- **Done when:** the listed tests pass and no previously passing test fails.
- **Status:** not started

### Slice 5: Submission rate limit
- **Goal:** The 21st submission from one IP address within a rolling hour gets a 429 error and isn't counted.
- **Spec sources:** NFR-005, FR-001.
- **Criteria:** none (this is an extra case)
- **Tests that should turn green:** test_AC002x_rateLimit
- **Skeleton to fill in:**
  - `SubmissionRateLimiter.checkAndRecord`: the full limit, using the injected clock
  - The `POST /api/applications` route: return `RateLimitExceededException` as 429 `rate_limited`
- **Depends on:** Slice 3
- **Done when:** the listed test passes and no previously passing test fails.
- **Status:** not started

### Slice 6: Case view
- **Goal:** Officers can open one case and see the stored application, its decision (if any), and its audit trail. Extra request fields are never stored.
- **Spec sources:** FR-005 (case view), FR-011.
- **Criteria:** none (this is an extra case)
- **Tests that should turn green:** test_AC002x_extraFieldsIgnored
- **Skeleton to fill in:**
  - `ReviewService.getCase`
  - `DecisionRepository.findByApplicationId`
  - `AuditLog.trail` and `AuditEntryRepository.findAll`
  - Route: `GET /api/review/cases/{id}`
- **Depends on:** Slice 3
- **Done when:** the listed test passes and no previously passing test fails.
- **Status:** not started

### Slice 7: Audit trail for auditors
- **Goal:** Compliance auditors can read an application's audit trail, and audit entries can't be edited or deleted.
- **Spec sources:** FR-006, NFR-002.
- **Criteria:** AC-009
- **Tests that should turn green:** test_AC009_scoringWritesCompleteAuditEntry, test_AC009_auditEntriesCannotBeEditedOrDeleted
- **Skeleton to fill in:** Route: `GET /api/audit/applications/{id}/entries`
- **Depends on:** Slice 6
- **Done when:** the listed tests pass and no previously passing test fails.
- **Status:** not started

### Slice 8: Confirm a recommendation
- **Goal:** A loan officer can confirm a recommendation. The status becomes CONFIRMED, a "confirm" audit entry is written in the same transaction, and auditors can't record decisions.
- **Spec sources:** FR-005, FR-006; Domain Model (OfficerDecision).
- **Criteria:** AC-006
- **Tests that should turn green:** test_AC006_confirmRecommendation, test_AC008x_auditorCannotDecide
- **Skeleton to fill in:**
  - `ReviewService.recordDecision` (the CONFIRM path)
  - `DecisionRepository.insert`
  - `DecisionNotifier.notifyDecision`, the no-email path only (it returns SKIPPED)
  - Route: `POST /api/review/cases/{id}/decision`
- **Depends on:** Slices 6 and 7
- **Done when:** the listed tests pass and no previously passing test fails.
- **Status:** not started

### Slice 9: Override decisions
- **Goal:** A loan officer can override a recommendation with a valid justification and a different outcome. Invalid overrides are rejected and nothing is recorded.
- **Spec sources:** FR-005, FR-006; Domain Model (OfficerDecision: justification of 10 to 1,000 characters after trimming, and an outcome different from the recommendation).
- **Criteria:** AC-007, AC-008
- **Tests that should turn green:** test_AC007_overrideWithoutValidJustificationIsBlocked, test_AC007x_overrideToSameOutcomeIsBlocked, test_AC008_overrideIsRecordedAndLogged
- **Skeleton to fill in:**
  - `ReviewService.recordDecision`: the OVERRIDE path and its validation
  - The decision route: return `InvalidDecisionException` as a 400 error
- **Depends on:** Slice 8
- **Done when:** the listed tests pass and no previously passing test fails.
- **Status:** not started

### Slice 10: One decision per case
- **Goal:** Only the first decision on a case counts. A second decision gets a 409 error and changes nothing.
- **Spec sources:** Domain Model (OfficerDecision: the first decision wins); architecture.md section 4 (unique constraint plus optimistic lock).
- **Criteria:** none (this is an extra case)
- **Tests that should turn green:** test_AC008x_secondDecisionIsRejected
- **Skeleton to fill in:**
  - `ReviewService.recordDecision`: throw `CaseAlreadyDecidedException`
  - `DecisionRepository.insert`: enforce one decision per application
  - The `ApplicationRepository.save` version check
  - The decision route: return a 409 `already_decided` error
- **Depends on:** Slice 9
- **Done when:** the listed test passes and no previously passing test fails.
- **Status:** not started

### Slice 11: Queue filters and search
- **Goal:** Officers can filter the queue by recommendation and date, search by application ID, and sort.
- **Spec sources:** FR-004.
- **Criteria:** AC-005
- **Tests that should turn green:** test_AC005_queueDefaultFilterSearchAndOpen
- **Skeleton to fill in:** `ReviewService.listQueue`: the recommendation, date, and search filters and sorting
- **Depends on:** Slices 9 and 10
- **Done when:** the listed test passes and no previously passing test fails.
- **Status:** not started

### Slice 12: Edit and resubmit
- **Goal:** An applicant can reload their earlier answers and resubmit. This creates a new application and marks the old Pending one as Superseded.
- **Spec sources:** FR-007.
- **Criteria:** AC-004
- **Tests that should turn green:** test_AC004_editAndResubmitSupersedesOldApplication
- **Skeleton to fill in:**
  - `IntakeService.getSubmittedForm` and `IntakeService.resubmit`
  - Routes: `GET /api/applications/{id}/form` and `POST /api/applications/{id}/resubmit`
- **Depends on:** Slices 4 and 6
- **Done when:** the listed test passes and no previously passing test fails.
- **Status:** not started

### Slice 13: Decision emails
- **Goal:** After a decision, applicants who gave an email address get exactly one email with the outcome, the disclaimer, and (for unfavorable outcomes) the reasons. Each send is recorded in the audit log.
- **Spec sources:** FR-009, NFR-007; Domain Model (DecisionNotice).
- **Criteria:** AC-011
- **Tests that should turn green:** test_AC011_decisionEmailSentOnce, test_AC011_unfavorableEmailListsReasons
- **Skeleton to fill in:**
  - `DecisionNotifier.notifyDecision`: the full version (SENT and FAILED)
  - `DecisionNotifier.retryFailed` (no test covers it, see the coverage check)
  - `SmtpEmailGateway.send`
- **Depends on:** Slice 8
- **Done when:** the listed tests pass and no previously passing test fails.
- **Status:** not started

### Slice 14: Audit search and CSV export
- **Goal:** Auditors can search the audit log by action, date range, application, and actor, and export the matches as CSV with formula cells escaped.
- **Spec sources:** FR-010, NFR-005.
- **Criteria:** AC-010
- **Tests that should turn green:** test_AC010_searchAndExportAuditLog, test_AC010x_auditLogRequiresAuditor
- **Skeleton to fill in:**
  - `AuditLog.search` and `AuditLog.exportCsv`
  - Routes: `GET /api/audit/entries` and `GET /api/audit/entries/export`
- **Depends on:** Slice 9
- **Done when:** the listed tests pass and no previously passing test fails.
- **Status:** not started

### Slice 15: Rule set versions
- **Goal:** Publishing a new rule set version affects only applications submitted after it, and the publish is audited.
- **Spec sources:** FR-008, NFR-008; Domain Model (RuleSet).
- **Criteria:** AC-012
- **Tests that should turn green:** test_AC012_newRuleSetAppliesOnlyToNewApplications
- **Skeleton to fill in:** `RuleSetService.publish` and `RuleSetRepository.insertAndActivate`
- **Depends on:** Slices 2 and 14
- **Done when:** the listed test passes and no previously passing test fails.
- **Status:** not started

### Slice 16: Data retention
- **Goal:** Applicant inputs and email addresses older than the retention period are cleared, and their audit entries are kept.
- **Spec sources:** FR-011, NFR-009; Domain Model (Retention).
- **Criteria:** AC-013
- **Tests that should turn green:** test_AC013_applicantDataDeletedAfterRetention
- **Skeleton to fill in:** `RetentionJob.purgeExpired` and `ApplicationRepository.clearApplicantDataSubmittedBefore`
- **Depends on:** Slices 6 and 7
- **Done when:** the listed test passes and no previously passing test fails.
- **Status:** not started

## Coverage check
- **Criteria with no slice:** none. Each of AC-001 to AC-013 appears in exactly one slice.
- **Tests with no slice:** none. All 31 tests appear in exactly one slice.
- **Functions or routes no slice fills:** none.
- **Functions no test covers:** `DecisionNotifier.retryFailed` is assigned to Slice 13, but no test checks it.
- **Functions filled across more than one slice:** `ScoringEngine.score` (Slices 1 and 2), `IntakeService.submit` (Slices 1 and 4), `SubmissionRateLimiter.checkAndRecord` (Slices 1 and 5), `ReviewService.listQueue` (Slices 3 and 11), `ReviewService.recordDecision` (Slices 8, 9, and 10), and `DecisionNotifier.notifyDecision` (Slices 8 and 13). Each slice adds one rule to these functions, so a failing test points to that one rule.

## Design questions
1. **Storage for the MVP.** Decided: data is kept in memory inside the repository classes for now. PostgreSQL, JPA, and Flyway (architecture.md) come later.
2. **Where HTTP errors are mapped.** Exceptions could be turned into 400, 401, 403, 404, 409, and 429 responses inside each controller with `@ExceptionHandler`, or in one shared `@RestControllerAdvice` class. A shared class would be a new file that architecture.md doesn't list.
3. **Seeding rule set version 1.** Version 1 could be created inside `RuleSetRepository` or by a Flyway migration. This depends on question 1.
4. **Scoring in Slice 1.** Slice 1 needs `ScoringEngine.score` to return something before Slice 2 adds the real rules. Is a minimal first version acceptable, or should the full scoring move into Slice 1?
5. **Case view after retention.** contracts.md says `getSubmittedForm` returns 404 after retention, but it doesn't say whether `getCase` still returns the application with its inputs set to null. test_AC013 assumes it does.
6. **Guideline text in reason rows.** The spec doesn't define which DTI guideline (36.0% or 43.0%) to show for credit scores of 740 and above. This is spec gap 2 in test_plan.md.

## Test issues
- **test_AC002x_inputTypes:** the `700.5` check already gets a 400 from the JSON setting before any app code runs, and it only checks the status code, not the error body.
- **test_AC009_auditEntriesCannotBeEditedOrDeleted:** it passes as long as there are no PUT or DELETE routes, so Spring's own 404 or 405 response satisfies it. It doesn't check the database-level rule in NFR-002.
- **test_AC008x_secondDecisionIsRejected:** it sends the two decisions one after the other, not at the same time. It may already pass after Slice 8 if that slice checks for a Pending status.
- **test_AC001_validRequestIsAccepted:** the 3-second check runs in MockMvc with no load, so it doesn't really test NFR-004.

## Next action
Start **Slice 1, "Thin submit-and-view path"**, and make **test_AC002x_resultsArePrivateToSession** pass. Answer design question 1 (storage) first.
