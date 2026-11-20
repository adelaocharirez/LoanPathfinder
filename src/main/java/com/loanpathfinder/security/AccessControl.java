package com.loanpathfinder.security;

import org.springframework.stereotype.Component;

/**
 * Module: security. Staff identity and role checks (NFR-001).
 * Skeleton note: until SSO (Spring Security OIDC) is added, identity comes from
 * the X-Staff-Id and X-Staff-Role request headers.
 */
@Component
public class AccessControl {

    /** Builds the signed-in staff user, or throws AccessDeniedException(authenticated=false) if missing. */
    public StaffUser currentUser(String staffIdHeader, String staffRoleHeader) {
        throw new UnsupportedOperationException("Not implemented: AccessControl.currentUser");
    }

    /** Throws AccessDeniedException(authenticated=true) if the user does not have the required role. */
    public void requireRole(StaffUser user, Role required) {
        throw new UnsupportedOperationException("Not implemented: AccessControl.requireRole");
    }
}
