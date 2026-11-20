package com.loanpathfinder.notification;

/** status is SENT, FAILED, or SKIPPED (no email address given). */
public record NotificationResult(String applicationId, String status) {
}
