package com.loanpathfinder.audit;

import java.time.LocalDate;

/** FR-010 search filters. Any field may be null. Dates are inclusive, in the lender's time zone. */
public record AuditQuery(
        String applicationId,
        LocalDate from,
        LocalDate to,
        String action,
        String actor) {
}
