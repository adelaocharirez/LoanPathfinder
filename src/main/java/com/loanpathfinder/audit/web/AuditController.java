package com.loanpathfinder.audit.web;

import com.loanpathfinder.audit.AuditLog;
import com.loanpathfinder.security.AccessControl;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Module: audit.web. Compliance auditor Audit Log page and CSV download. Every route returns 501 until the module behind it is implemented. */
@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditLog auditLog;
    private final AccessControl accessControl;

    public AuditController(AuditLog auditLog, AccessControl accessControl) {
        this.auditLog = auditLog;
        this.accessControl = accessControl;
    }

    /** GET /api/audit/applications/{id}/entries -> AccessControl.currentUser, AccessControl.requireRole(COMPLIANCE_AUDITOR), AuditLog.trail (FR-006) */
    @GetMapping("/applications/{applicationId}/entries")
    public ResponseEntity<Map<String, String>> trail(@PathVariable String applicationId, @RequestHeader(value = "X-Staff-Id", required = false) String staffId, @RequestHeader(value = "X-Staff-Role", required = false) String staffRole) {
        return notImplemented("GET /api/audit/applications/{id}/entries -> AccessControl.currentUser, AccessControl.requireRole(COMPLIANCE_AUDITOR), AuditLog.trail (FR-006)");
    }

    /** GET /api/audit/entries -> AccessControl.currentUser, AccessControl.requireRole(COMPLIANCE_AUDITOR), AuditLog.search (FR-010) */
    @GetMapping("/entries")
    public ResponseEntity<Map<String, String>> search(@RequestHeader(value = "X-Staff-Id", required = false) String staffId, @RequestHeader(value = "X-Staff-Role", required = false) String staffRole, @RequestParam(required = false) String applicationId, @RequestParam(required = false) String from, @RequestParam(required = false) String to, @RequestParam(required = false) String action, @RequestParam(required = false) String actor) {
        return notImplemented("GET /api/audit/entries -> AccessControl.currentUser, AccessControl.requireRole(COMPLIANCE_AUDITOR), AuditLog.search (FR-010)");
    }

    /** GET /api/audit/entries/export -> AccessControl.currentUser, AccessControl.requireRole(COMPLIANCE_AUDITOR), AuditLog.exportCsv (FR-010) */
    @GetMapping("/entries/export")
    public ResponseEntity<Map<String, String>> export(@RequestHeader(value = "X-Staff-Id", required = false) String staffId, @RequestHeader(value = "X-Staff-Role", required = false) String staffRole, @RequestParam(required = false) String applicationId, @RequestParam(required = false) String from, @RequestParam(required = false) String to, @RequestParam(required = false) String action, @RequestParam(required = false) String actor) {
        return notImplemented("GET /api/audit/entries/export -> AccessControl.currentUser, AccessControl.requireRole(COMPLIANCE_AUDITOR), AuditLog.exportCsv (FR-010)");
    }

    private static ResponseEntity<Map<String, String>> notImplemented(String message) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(Map.of("error", "not_implemented", "message", message));
    }
}
