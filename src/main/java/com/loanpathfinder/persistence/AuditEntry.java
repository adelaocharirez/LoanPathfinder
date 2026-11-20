package com.loanpathfinder.persistence;

import java.time.Instant;

/**
 * Domain Model: AuditLogEntry. Append-only.
 * action is one of: scored, superseded, confirm, override, email_sent, email_failed, rule_set_published.
 * comment holds free text such as an override justification or confirm note.
 */
public record AuditEntry(
        long sequence,
        Instant timestamp,
        String actor,
        String applicationId,
        String action,
        Integer ruleSetVersion,
        String beforeData,
        String afterData,
        String comment) {
}
