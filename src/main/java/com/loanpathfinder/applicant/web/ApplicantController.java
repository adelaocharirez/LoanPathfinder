package com.loanpathfinder.applicant.web;

import com.loanpathfinder.intake.ApplicationForm;
import com.loanpathfinder.intake.IntakeService;
import com.loanpathfinder.security.SessionOwnership;
import com.loanpathfinder.security.SubmissionRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Module: applicant.web. Applicant Application Form and Results pages. Every route returns 501 until the module behind it is implemented. */
@RestController
@RequestMapping("/api/applications")
public class ApplicantController {

    private final IntakeService intakeService;
    private final SessionOwnership sessionOwnership;
    private final SubmissionRateLimiter rateLimiter;

    public ApplicantController(IntakeService intakeService, SessionOwnership sessionOwnership, SubmissionRateLimiter rateLimiter) {
        this.intakeService = intakeService;
        this.sessionOwnership = sessionOwnership;
        this.rateLimiter = rateLimiter;
    }

    /** POST /api/applications -> SubmissionRateLimiter.checkAndRecord, IntakeService.submit, SessionOwnership.claim (FR-001) */
    @PostMapping
    public ResponseEntity<Map<String, String>> submit(@RequestBody ApplicationForm form, HttpSession session, HttpServletRequest request) {
        return notImplemented("POST /api/applications -> SubmissionRateLimiter.checkAndRecord, IntakeService.submit, SessionOwnership.claim (FR-001)");
    }

    /** GET /api/applications/{id}/result -> SessionOwnership.owns, IntakeService.getResult (FR-002) */
    @GetMapping("/{applicationId}/result")
    public ResponseEntity<Map<String, String>> result(@PathVariable String applicationId, HttpSession session) {
        return notImplemented("GET /api/applications/{id}/result -> SessionOwnership.owns, IntakeService.getResult (FR-002)");
    }

    /** GET /api/applications/{id}/reasons -> SessionOwnership.owns, IntakeService.getResult (FR-003) */
    @GetMapping("/{applicationId}/reasons")
    public ResponseEntity<Map<String, String>> reasons(@PathVariable String applicationId, HttpSession session) {
        return notImplemented("GET /api/applications/{id}/reasons -> SessionOwnership.owns, IntakeService.getResult (FR-003)");
    }

    /** GET /api/applications/{id}/form -> SessionOwnership.owns, IntakeService.getSubmittedForm (FR-007) */
    @GetMapping("/{applicationId}/form")
    public ResponseEntity<Map<String, String>> submittedForm(@PathVariable String applicationId, HttpSession session) {
        return notImplemented("GET /api/applications/{id}/form -> SessionOwnership.owns, IntakeService.getSubmittedForm (FR-007)");
    }

    /** POST /api/applications/{id}/resubmit -> SubmissionRateLimiter.checkAndRecord, SessionOwnership.owns, IntakeService.resubmit, SessionOwnership.claim (FR-007) */
    @PostMapping("/{applicationId}/resubmit")
    public ResponseEntity<Map<String, String>> resubmit(@PathVariable String applicationId, @RequestBody ApplicationForm form, HttpSession session, HttpServletRequest request) {
        return notImplemented("POST /api/applications/{id}/resubmit -> SubmissionRateLimiter.checkAndRecord, SessionOwnership.owns, IntakeService.resubmit, SessionOwnership.claim (FR-007)");
    }

    private static ResponseEntity<Map<String, String>> notImplemented(String message) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(Map.of("error", "not_implemented", "message", message));
    }
}
