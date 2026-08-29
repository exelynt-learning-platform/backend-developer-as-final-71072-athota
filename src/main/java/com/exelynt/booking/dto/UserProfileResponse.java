package com.exelynt.booking.dto;

import com.exelynt.booking.security.UserPrincipal;

/** A deliberately small view of the authenticated user; it never serializes credentials. */
public record UserProfileResponse(Long id, String username, String email, String role) {

    public static UserProfileResponse fromPrincipal(UserPrincipal principal) {
        String role = principal.getAuthorities().stream()
                .findFirst()
                .map(authority -> authority.getAuthority())
                .orElse("");
        return new UserProfileResponse(principal.getId(), principal.getUsername(), principal.getEmail(), role);
    }
}
