package com.loanpathfinder.review.web;

import com.loanpathfinder.review.DecisionRequest;
import com.loanpathfinder.review.ReviewService;
import com.loanpathfinder.security.AccessControl;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Module: review.web. Loan officer Review Queue and Case Review pages. Every route returns 501 until the module behind it is implemented. */
@RestController
@RequestMapping("/api/review")
public class ReviewController {

    private final ReviewService reviewService;
    private final AccessControl accessControl;

    public ReviewController(ReviewService reviewService, AccessControl accessControl) {
        this.reviewService = reviewService;
        this.accessControl = accessControl;
    }

    /** GET /api/review/queue -> AccessControl.currentUser, AccessControl.requireRole(LOAN_OFFICER), ReviewService.listQueue (FR-004) */
    @GetMapping("/queue")
    public ResponseEntity<Map<String, String>> queue(@RequestHeader(value = "X-Staff-Id", required = false) String staffId, @RequestHeader(value = "X-Staff-Role", required = false) String staffRole, @RequestParam(required = false) String status, @RequestParam(required = false) String recommendation, @RequestParam(required = false) String from, @RequestParam(required = false) String to, @RequestParam(required = false) String search, @RequestParam(required = false) String sort, @RequestParam(defaultValue = "1") int page) {
        return notImplemented("GET /api/review/queue -> AccessControl.currentUser, AccessControl.requireRole(LOAN_OFFICER), ReviewService.listQueue (FR-004)");
    }

    /** GET /api/review/cases/{id} -> AccessControl.currentUser, AccessControl.requireRole(LOAN_OFFICER), ReviewService.getCase (FR-005) */
    @GetMapping("/cases/{applicationId}")
    public ResponseEntity<Map<String, String>> caseView(@PathVariable String applicationId, @RequestHeader(value = "X-Staff-Id", required = false) String staffId, @RequestHeader(value = "X-Staff-Role", required = false) String staffRole) {
        return notImplemented("GET /api/review/cases/{id} -> AccessControl.currentUser, AccessControl.requireRole(LOAN_OFFICER), ReviewService.getCase (FR-005)");
    }

    /** POST /api/review/cases/{id}/decision -> AccessControl.currentUser, AccessControl.requireRole(LOAN_OFFICER), ReviewService.recordDecision (FR-005) */
    @PostMapping("/cases/{applicationId}/decision")
    public ResponseEntity<Map<String, String>> decide(@PathVariable String applicationId, @RequestBody DecisionRequest request, @RequestHeader(value = "X-Staff-Id", required = false) String staffId, @RequestHeader(value = "X-Staff-Role", required = false) String staffRole) {
        return notImplemented("POST /api/review/cases/{id}/decision -> AccessControl.currentUser, AccessControl.requireRole(LOAN_OFFICER), ReviewService.recordDecision (FR-005)");
    }

    private static ResponseEntity<Map<String, String>> notImplemented(String message) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(Map.of("error", "not_implemented", "message", message));
    }
}
