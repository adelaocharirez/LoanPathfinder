# Description

Loan Pathfinder is a web application that tells a person whether they are likely to qualify for a loan before they file a formal application. The applicant enters their annual income, monthly debt payments, self-reported credit score, and the loan amount and term they want. The system scores the request with two published, versioned rules (debt-to-income ratio and credit score band) and returns one of three recommendations: Likely Qualified, Needs Review, or Unlikely Qualified. Every recommendation comes with the specific reasons behind it, generated from the same rules that produced it. A loan officer reviews every recommendation and makes the final decision, and can override the system only by writing a justification. Every recommendation, rule version, and officer decision is written to an append-only audit log so a compliance auditor can reconstruct why any decision was made, as required by ECOA and Regulation B. The system does not pull credit reports, does not collect protected characteristics, and its result is not a loan offer or a final decision.

# Goals

1. **Learn likelihood of loan qualification (G1):** Applicants find out, before a formal application and without a credit inquiry, whether a loan is realistic and what they could change to improve their chances.
2. **Make sound final lending decisions (G2):** Loan officers reach consistent, defensible final decisions using the system's recommendation without being bound by it.
3. **Provide transparent pre-qualification (G3):** Every recommendation is produced from documented, versioned rules and explained factor by factor.
4. **Ensure ECOA compliance (G4):** The lender can show that decisions follow ECOA and Regulation B, including specific and accurate reasons for unfavorable outcomes and a complete record of who decided what and why.

# Domain Model

## Actors

- **Applicant:** A consumer checking whether they are likely to qualify for a loan. Uses the system without an account. Can only see applications created in their own browser session.
- **Loan Officer:** Lender employee who reviews scored applications and confirms or overrides the recommendation. Signs in through the lender's SSO with the Loan Officer role. Any loan officer can open any pending case.
- **Compliance Auditor:** Lender employee who searches and exports the audit log. Signs in through SSO with the Compliance Auditor role. Read-only access, and cannot record decisions.
- **Lender Administrator:** Publishes new rule set versions (post-MVP, FR-008). Until then, rule sets are changed through configuration by the development team.
- **Loan Pathfinder System:** Validates and scores applications, generates explanations, manages the review queue, sends decision emails, and writes the audit log.

## Objects

- **LoanApplication:** One pre-qualification request.
  - Has a random, non-guessable application ID (shown to the applicant on the Results page), a submission timestamp, a FinancialProfile, a loan amount and term, an optional email, a computed DTI, one Recommendation, and the rule set version used.
  - Status is one of: Pending (waiting for an officer), Confirmed, Overridden, or Superseded (the applicant edited and resubmitted before an officer decided).
  - Only Pending applications appear as actionable in the review queue.
- **FinancialProfile:** The applicant's inputs. All fields are validated on the server with the same rules as the form, and any extra fields in a request are ignored and never stored.
  - Annual gross income: required, number greater than 0, up to 2 decimal places, at most 999,999,999.
  - Total monthly debt payments: required, number 0 or greater, up to 2 decimal places, at most 9,999,999.
  - Credit score: required, whole number from 300 to 850, self-reported.
- **Loan request fields:**
  - Loan amount: required, number greater than 0, up to 2 decimal places, at most 9,999,999.
  - Loan term: required, one of 12, 24, 36, 48, 60, or 72 months.
  - Acknowledgement: required checkbox confirming the result is a pre-qualification and not an offer.
  - Email: optional, must be a valid email format, at most 254 characters.
- **CreditScoreBand:** Excellent (740 and above), Good (670 to 739), Fair (580 to 669), Poor (below 580).
- **Debt-to-income ratio (DTI):** (monthly debt payments + estimated monthly loan payment) / (annual gross income / 12) x 100.
  - The estimated monthly payment uses standard amortization: P x r / (1 - (1 + r)^-n), where P is the loan amount, n is the term in months, and r is the rule set's reference annual rate divided by 12 (initial reference rate 9.0%).
  - DTI is rounded to one decimal place (half up) before it is compared with any threshold, and it is shown to the applicant with one decimal place.
- **RuleSet:** A numbered, versioned set of thresholds and the reference rate. Exactly one version is active at a time. An application is always scored with the version that was active when it was submitted, and the version is stored with it. Old versions are never changed or deleted. Client requests cannot choose a version.
- **ScoringRule (DebtToIncomeRule, CreditScoreRule):** Evaluated in this order, and the first match wins:
  1. **Unlikely Qualified:** credit score below 580, OR DTI above 43.0%.
  2. **Likely Qualified:** (credit score 670 or higher AND DTI 36.0% or lower) OR (credit score 740 or higher AND DTI 43.0% or lower).
  3. **Needs Review:** any other combination.
- **Recommendation (LikelyQualified, NeedsReview, UnlikelyQualified):** The system's output for one application. Scoring is deterministic: the same inputs and rule set version always give the same recommendation and reasons. A Recommendation is never changed after it is created.
- **ExplanationReason:** One row per factor (Credit score, Debt-to-income ratio). Each row shows the applicant's value, the guideline, and whether the factor met the guideline, and it cites the rule that produced it.
  - Both factor rows are always shown, for every outcome.
  - Reasons use fixed plain-language templates (ninth-grade reading level or lower) and never use vague phrases like "did not meet our internal standards".
  - **Principal reason:** the first factor that did not meet the Likely Qualified guideline, checking credit score first and then DTI. For Likely Qualified, the principal reason is "Meets guidelines".
- **OfficerDecision (ConfirmedDecision, OverrideDecision):** The final decision on a Pending application, made by a Loan Officer.
  - An override must pick a new outcome different from the recommendation, and it needs a justification that is at least 10 characters after trimming spaces, with a maximum of 1,000 characters.
  - Each application can have only one decision. The first decision submitted wins, a later one is rejected, and decisions are final.
  - A confirm may include an optional note.
- **DecisionNotice:** An email sent when a decision is recorded, if the applicant gave an email address. It includes the application ID, the final outcome, a statement that it is a pre-qualification and not an offer, and, for any outcome other than Likely Qualified, the specific reasons. For an override, the reasons shown are the factor rows plus the officer's justification.
- **AuditLogEntry:** An append-only record. The system writes one when an application is submitted and scored, when a decision is recorded, when an application is superseded, when a decision email is sent, and when a rule set version is published.
  - Each entry contains a timestamp, the acting user or system component, the application ID (if any), the action, the rule set version, and before and after data where relevant.
  - No user, including administrators, can edit or delete an entry through the application.
- **Retention:** Applicant inputs and email addresses are kept for a configurable retention period (default 25 months, never shorter), and then a scheduled system job deletes them. Audit entries are kept as long as the lender's record-keeping policy requires, and they do not store the applicant's email address.
- **Screens:** Application Form (S1), Results page (S2), Review Queue (S3), Case Review page (S4), Audit Log page (S5).

# Functional Requirements

- **FR-001:** As an applicant, I want to enter my income, monthly debts, credit score, and loan amount and term, so that I can find out whether I am likely to qualify.
- **FR-002:** As an applicant, I want to see a clear recommendation right after I submit, so that I know where I stand.
- **FR-003:** As an applicant, I want to see the specific reasons for my recommendation, with my value and the guideline for each factor, so that I understand it and know what I could change.
- **FR-004:** As a loan officer, I want a queue of applications showing the recommendation and principal reason, with sorting and filtering, so that I can review cases efficiently.
- **FR-005:** As a loan officer, I want to confirm or override a recommendation and be required to explain any override, so that my decision is consistent and defensible.
- **FR-006:** As a compliance auditor, I want every recommendation, rule version, and officer decision recorded in an append-only log, so that I can reconstruct why any decision was made.
- **FR-007:** As an applicant, I want to change my inputs and run the check again, so that I can see how lowering debt or raising my score would change the result.
- **FR-008 (post-MVP):** As a lender administrator, I want to publish updated rule thresholds as a new version without a software release, so that policy changes are quick and traceable.
- **FR-009:** As an applicant who gave an email address, I want to receive the final decision and its reasons by email, so that I have the result in writing.
- **FR-010:** As a compliance auditor, I want to search the audit log and export results to CSV, so that I can review decisions for fairness and specific reasons.
- **FR-011:** As an applicant, I want the system to collect only the information needed for scoring and protect it, so that sharing my financial data is safe.

# Nonfunctional Requirements

In priority order:

- **NFR-001 Security and privacy:** All traffic uses HTTPS. Stored applicant data is encrypted. Staff must log in through SSO and can only use their role's features: officers make decisions, and auditors only read and export the log. Applicants can only see results from their own browser session. (FR-011, G4)
- **NFR-002 Audit integrity:** The database itself prevents audit entries from being changed or deleted. Each audit entry is saved together with the change it records, so a decision can never exist without its audit entry. (FR-006, G4)
- **NFR-003 Scoring accuracy:** Money and DTI use exact decimal math, never floating point. Re-scoring a saved application with its saved rule set version always gives the same result. Every rule and threshold boundary has an automated test. (FR-002, FR-003, G3)
- **NFR-004 Performance:** For 95% of submissions, results appear within 3 seconds with up to 50 people submitting at once. Queue and audit searches finish within 2 seconds with up to 100,000 applications. (FR-002, FR-004, FR-010)
- **NFR-005 Abuse resistance:** One IP address can submit at most 20 times per hour. All input is checked on the server, and all text people type is made safe before it's shown or exported. (FR-001, FR-010, FR-011)
- **NFR-006 Accessibility:** Applicant pages meet WCAG 2.1 AA, never use color alone to show the result, and work on phones and computers. Reasons are written at a ninth-grade reading level or lower. (FR-002, FR-003, G1)
- **NFR-007 Reliability:** If an email fails, the officer's decision still stands. The email is retried for up to 24 hours, and each try is logged. The system aims to be up 99.5% of the time each month. (FR-005, FR-009)
- **NFR-008 Modifiability:** Rule thresholds and the reference rate are stored as versioned data, so a new rule set can be added without code changes. (FR-008, G3)
- **NFR-009 Data retention:** Old applicant data is deleted within 24 hours after the retention period ends. (FR-011)

# Out of Scope

- Credit bureau lookups. The credit score is self-reported, so no credit inquiry happens.
- Final underwriting, loan origination, funding, or servicing.
- Machine learning or other trained scoring models.
- Integration with a lender's loan origination system (future work).
- Native mobile apps. The web interface works in mobile browsers instead.
- Applicant accounts or sign-in.

# Acceptance Criteria

**AC-001: A valid request is accepted (FR-001, FR-002)**

```gherkin
Feature: Submit a pre-qualification request

  Scenario: A valid request is accepted
    Given I am on the Application Form
    And I have entered an income of "62000"
    And I have entered monthly debt of "850"
    And I have entered a credit score of "690"
    And I have entered a loan amount of "15000" and a term of "48"
    And I have checked the pre-qualification acknowledgement
    When I click "See my result"
    Then I should see the Results page within 3 seconds
    And I should see my application ID
    And I should see "This is a pre-qualification, not a loan offer or final decision."
```

**AC-002: A credit score outside the allowed range is rejected (FR-001)**

```gherkin
  Scenario: A credit score outside the allowed range is rejected
    Given I am on the Application Form
    When I enter a credit score of "900"
    And I click "See my result"
    Then I should stay on the Application Form
    And I should see the error "Credit score must be between 300 and 850"
    And no application should be saved
```

**AC-003: Reasons match the scoring rules (FR-002, FR-003)**

```gherkin
Feature: Explain the recommendation

  Scenario Outline: Reasons match the scoring rules
    Given I submitted an application with credit score <score>
    And my debt-to-income ratio is <dti>
    When the Results page is displayed
    Then I should see the recommendation "<outcome>"
    And I should see a reason row for "Credit score" showing <score>
    And I should see a reason row for "Debt-to-income ratio" showing <dti>

    Examples:
      | score | dti   | outcome            |
      | 750   | 30.0% | Likely Qualified   |
      | 655   | 41.0% | Needs Review       |
      | 540   | 30.0% | Unlikely Qualified |
      | 700   | 50.0% | Unlikely Qualified |
```

**AC-004: Edit and resubmit (FR-007)**

```gherkin
Feature: Edit and resubmit

  Scenario: Editing my answers creates a new application
    Given I submitted application "A-100" with monthly debt of "850"
    And "A-100" is Pending
    When I click "Edit my answers"
    Then I should see the Application Form with my previous values filled in
    When I change monthly debt to "300"
    And I click "See my result"
    Then I should see a Results page with a new application ID
    And application "A-100" should have the status "Superseded"
    And "A-100" should not appear in the default Review Queue
```

**AC-005: Review queue (FR-004)**

```gherkin
Feature: Review queue

  Background:
    Given I am signed in as a loan officer

  Scenario: The default queue shows pending applications, oldest first
    Given there are Pending, Confirmed, and Overridden applications
    When I open the Review Queue
    Then I should see only Pending applications
    And they should be sorted by submission date, oldest first
    And each row should show the application ID, date, recommendation, principal reason, and status

  Scenario: Filtering and searching
    Given I am on the Review Queue
    When I filter by recommendation "Needs Review"
    Then every row should show the recommendation "Needs Review"
    When I search for application ID "A-1042"
    Then I should see only the row for "A-1042"

  Scenario: Opening a case
    Given I am on the Review Queue
    When I click "Open" on the row for "A-1042"
    Then I should see the Case Review page for "A-1042"
```

**AC-006: Confirming a recommendation (FR-005, FR-006)**

```gherkin
Feature: Officer decision

  Background:
    Given I am signed in as a loan officer
    And I am viewing Pending case "A-1042" on the Case Review page
    And the system recommendation is "Needs Review"

  Scenario: Confirming a recommendation
    When I select "Confirm"
    And I click "Submit decision"
    Then I should return to the Review Queue
    And case "A-1042" should have the status "Confirmed"
    And the audit trail for "A-1042" should show a confirm entry with my name
```

**AC-007: An override without a valid justification is blocked (FR-005)**

```gherkin
  Scenario Outline: An override without a valid justification is blocked
    When I select "Override"
    And I choose the new outcome "Likely Qualified"
    And I enter the justification "<text>"
    And I click "Submit decision"
    Then I should stay on the Case Review page
    And I should see the error "A justification of at least 10 characters is required to override"
    And no decision should be recorded for "A-1042"

    Examples:
      | text      |
      |           |
      | too short |
```

**AC-008: An override with a justification is recorded and logged (FR-005, FR-006)**

```gherkin
  Scenario: An override with a justification is recorded and logged
    When I select "Override"
    And I choose the new outcome "Likely Qualified"
    And I enter the justification "Applicant documented a pending debt payoff"
    And I click "Submit decision"
    Then I should return to the Review Queue
    And case "A-1042" should have the status "Overridden"
    And the original recommendation for "A-1042" should still be "Needs Review"
    And the audit trail for "A-1042" should show an override entry
    And that entry should include my name, the new outcome, and the justification
```

**AC-009: Audit entries are complete and cannot be changed (FR-006)**

```gherkin
Feature: Audit log

  Scenario: Scoring writes a complete audit entry
    Given an applicant submitted application "A-2001"
    When I view the audit log for "A-2001" as a compliance auditor
    Then I should see a "scored" entry
    And it should include the timestamp, "system", "A-2001", the recommendation, and the rule set version

  Scenario: Audit entries cannot be edited or deleted
    Given I am signed in as any user
    When I try to edit or delete an audit entry
    Then the request should be refused
    And the entry should be unchanged
```

**AC-010: Audit search and export (FR-010)**

```gherkin
  Scenario: Searching and exporting the audit log
    Given I am signed in as a compliance auditor
    And I am on the Audit Log page
    When I search for action "override" between "2026-10-01" and "2026-10-31"
    Then I should see only override entries from that date range
    When I click "Export CSV"
    Then I should download a CSV file with every field of each matching entry

  Scenario: Formula-like text is escaped in the export
    Given an override justification begins with "=SUM(A1:A9)"
    When I export the audit log to CSV
    Then that cell should be escaped so it is treated as text
```

**AC-011: Decision email (FR-009)**

```gherkin
Feature: Decision email

  Scenario: An applicant with an email receives the decision once
    Given application "A-3001" was submitted with the email "dana@example.com"
    When a loan officer records a decision for "A-3001"
    Then exactly one email should be sent to "dana@example.com"
    And it should include "A-3001", the final outcome, and the pre-qualification disclaimer
    And an "email sent" entry should be added to the audit log

  Scenario: No email is sent when no address was given
    Given application "A-3002" was submitted without an email
    When a loan officer records a decision for "A-3002"
    Then no email should be sent

  Scenario: An unfavorable decision email includes the specific reasons
    Given application "A-3003" was submitted with an email and the recommendation "Unlikely Qualified"
    When a loan officer confirms the recommendation
    Then the email should list each factor with my value and the guideline
```

**AC-012: Publishing a new rule set version (FR-008)**

```gherkin
Feature: Rule set versions

  Scenario: A new version applies only to new applications
    Given application "A-4001" was scored with rule set version 1
    When a lender administrator publishes rule set version 2
    And an applicant submits application "A-4002"
    Then "A-4002" should be scored with rule set version 2
    And "A-4001" should still show rule set version 1 and its original recommendation
    And a "rule set published" entry should be added to the audit log
```

**AC-013: Data retention (FR-011)**

```gherkin
Feature: Data retention

  Scenario: Applicant data is deleted after the retention period
    Given the retention period is 25 months
    And application "A-5001" was submitted 26 months ago
    When the scheduled retention job runs
    Then the inputs and email for "A-5001" should be deleted
    And the audit entries for "A-5001" should still exist
```
