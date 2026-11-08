# Loan Pathfinder Architecture

## 1. Overview

Loan Pathfinder is one Spring Boot web application. Applicants fill out a form and get a scored result. Loan officers review those results and confirm or override them. Compliance auditors read and export the audit log.

The app is a **modular monolith**: one program, split into packages that each do one job. All data is stored in one PostgreSQL database. The app talks to two outside systems: the lender's single sign-on (SSO) for staff login, and an email server.

**Implementation constraints**

- Java 21, Spring Boot 3, Maven
- Thymeleaf for the web pages (the server builds the HTML)
- PostgreSQL database, with Flyway to manage database changes
- Spring Security for staff login through the lender's SSO
- Runs as one Docker container on free or student hosting
- Tests: JUnit for code, and Cucumber to run the Gherkin acceptance criteria

## 2. Modules

| Module | What it does | Uses | Why it exists |
|---|---|---|---|
| `applicant.web` | Shows the Application Form and Results pages | `intake`, `security` | FR-001, FR-002, FR-003, FR-007 |
| `intake` | Checks a submission, scores it, and saves it | `scoring`, `rules`, `audit`, `persistence` | FR-001, FR-002, FR-007 |
| `scoring` | Calculates DTI, credit band, recommendation, and reasons (math only, no database) | nothing | FR-002, FR-003, NFR-003 |
| `rules` | Gets the current rule set and adds new versions | `audit`, `persistence` | FR-008, NFR-008 |
| `review` | Runs the review queue and records one decision per application | `audit`, `notification`, `persistence` | FR-004, FR-005 |
| `review.web` | Shows the Review Queue and Case Review pages | `review`, `audit` | FR-004, FR-005 |
| `audit` | Writes audit entries, searches them, and exports CSV | `persistence` | FR-006, FR-010, NFR-002 |
| `audit.web` | Shows the Audit Log page and the CSV download | `audit` | FR-010 |
| `notification` | Sends the decision email and retries if it fails | `audit`, `persistence` | FR-009, NFR-007 |
| `security` | Handles staff login, role checks, "is this your result" checks, and the submission limit | nothing | FR-011, NFR-001, NFR-005 |
| `retention` | Deletes old applicant data once a day | `audit`, `persistence` | FR-011, NFR-009 |
| `persistence` | Saves and loads data from the database | nothing | Every requirement that stores data |

## 3. Request flow

### 3.1 Applicant submits and sees a result (FR-001 to FR-003, AC-001 to AC-003)

1. The applicant clicks "See my result", and the form is sent to the server.
2. `security` checks whether this IP address has sent more than 20 forms in the last hour. If it has, the applicant sees "Too many requests" and nothing else happens.
3. `applicant.web` reads only the expected form fields and ignores anything extra. It checks each field. If any field is wrong, the form is shown again with an error under that field, and nothing is saved (AC-002).
4. `intake` gets the current rule set from `rules`.
5. `scoring` calculates the DTI, credit band, recommendation, and both reason rows.
6. `intake` saves everything in one database transaction: the new application with a random ID, the result, and the rule set version. If this browser session had an older Pending application, it is marked Superseded. The audit entry is saved in the same transaction.
7. The new application ID is added to the applicant's browser session.
8. The browser is sent to the Results page. `security` makes sure this session owns the application, and shows "Result not found" if it doesn't. The page shows the recommendation as text, the reasons, the ID, and the disclaimer (AC-001, AC-003).
9. If steps 4 to 6 fail, nothing is saved, and the applicant sees "We couldn't calculate your result. Please try again."

### 3.2 Loan officer overrides a recommendation (FR-005, FR-006, FR-009, AC-007, AC-008, AC-011)

1. The officer submits a decision on the Case Review page. `security` blocks anyone who isn't a Loan Officer.
2. `review` checks that the case is still Pending, the new outcome is different, and the justification is 10 to 1,000 characters after spaces are trimmed. If not, the page shows an error and nothing is saved (AC-007).
3. `review` saves the decision, the new status, and the audit entry together in one transaction. If another officer already decided the case, the save fails and the officer sees "This case was already decided."
4. After the save, `notification` sends the email if the applicant gave one. If the email fails, the decision still stands, and the app tries the email again later.
5. The officer goes back to the Review Queue, and the case shows "Overridden" (AC-008).

## 4. Design decisions

| Decision | Reason |
|---|---|
| One app instead of many small services | Nothing in the spec needs separate services. One app makes it easy to save the audit entry together with the change (NFR-002). |
| The server builds the pages (Thymeleaf), with no separate front-end app | All validation and safety checks stay on the server (NFR-005). **Preference.** |
| One PostgreSQL database | We need transactions, unique rules, and database permissions (NFR-002, AC-008). Picking PostgreSQL specifically is a **preference**. |
| Staff log in through the lender's SSO, and their role comes from the login | The spec says staff use SSO (Domain Model, Actors) and only see their own functions (NFR-001). |
| Applicants have no account. The server remembers which results each browser session created. | The spec says applicants don't have accounts and can only see their own results (FR-011, NFR-001). |
| Application IDs are random and hard to guess | Stops people from guessing someone else's ID (Domain Model, LoanApplication). |
| Scoring is plain math code with no database or web code | Makes scoring easy to test for every rule and boundary (NFR-003, AC-003). |
| Money and DTI use exact decimals (`BigDecimal`), rounded to one decimal place | Avoids rounding errors at 36.0% and 43.0% (Domain Model, DTI; NFR-003). |
| Rule sets are stored as versioned rows in the database. For now, new versions are added by the developer through a database update. | The admin screen (FR-008) comes after the MVP, and rules must be data, not code (NFR-008). |
| The app's database account can only add and read audit entries, never change or delete them | The audit log must be append-only (NFR-002, AC-009). |
| Only one decision per application, enforced by the database. If two officers submit at once, the first one wins. | Domain Model, OfficerDecision. This is where **concurrency** is handled. |
| Email is sent after the decision is saved, and failed emails are retried for 24 hours | A failed email must not block the decision (NFR-007, FR-009). |
| The 20-per-hour limit is counted in the app's memory | NFR-005. This works because there's only one copy of the app (see section 5). |
| Times are saved in UTC and shown in the lender's local time | Audit entries and retention need exact times (FR-006, NFR-009). This is where **time** is handled. Local display is a **preference**. |
| A daily scheduled job deletes old applicant data | Domain Model, Retention; NFR-009. |
| Encryption at rest and HTTPS come from the hosting platform | NFR-001. Using the platform instead of building it ourselves is a **preference**. |
| The Gherkin acceptance criteria run as Cucumber tests | Keeps the tests matched to the spec. **Preference.** |

## 5. Constraints and known limits

**Constraints:** the technology choices and free hosting listed in section 1.

**What this design doesn't handle:**

- **Running more than one copy of the app.** The submission counter lives in memory, so each copy would count separately. The counter would need to move to the database.
- **Applicants who lose their session.** If the browser session ends, the applicant can't get back to old results, and an older application won't be marked Superseded. Fixing this would need applicant accounts, which are out of scope.
- **Long email outages.** Retries stop after 24 hours, and the failure is only recorded in the audit log.
- **Changing rules without a developer.** Until FR-008 is built, a new rule set needs a database update and a redeploy.
- **Fair lending reports.** The only report is the audit CSV export.

**If the requirements grew:**

- Connecting to a loan origination system would need a new module that sends decisions out.
- Using real credit reports would need a credit bureau connection and FCRA notices.
- More traffic would need several copies of the app, with the session and counter data moved into the database.

## 6. Out of scope

Copied from spec.md:

- Credit bureau lookups. The credit score is self-reported, so no credit inquiry happens.
- Final underwriting, loan origination, funding, or servicing.
- Machine learning or other trained scoring models.
- Integration with a lender's loan origination system (future work).
- Native mobile apps. The web interface works in mobile browsers instead.
- Applicant accounts or sign-in.

## Spec issues

1. **AC-012 tests FR-008, which is post-MVP,** and the spec never adds a Lender Administrator role to SSO.
2. **Audit entries may keep personal data after it should be deleted.** Audit entries keep "before and after data", which can include income and debts. The spec doesn't say whether that data is removed when the retention period ends.
3. **"Sent once" isn't fully possible with email.** If the email server times out after it already accepted a message, a retry could send a second copy.
4. **It's unclear which DTI guideline to show for credit scores of 740 and up.** The DTI guideline depends on the credit score (36.0% or 43.0%), and the spec doesn't say which one the reason row should show.
5. **Superseding depends on the browser session.** If the session expired, the old application stays in the queue, and officers might review the same person twice.
6. **Performance (NFR-004) and accessibility (NFR-006) have no acceptance criteria.**
