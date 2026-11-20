package com.loanpathfinder.audit;

/** What a module asks AuditLog to record. Timestamp and sequence are added by AuditLog. */
public record AuditEvent(
        String actor,
        String applicationId,
        String action,
        Integer ruleSetVersion,
        String beforeData,
        String afterData,
        String comment) {
}
