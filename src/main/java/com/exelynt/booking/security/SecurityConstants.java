package com.exelynt.booking.security;

public final class SecurityConstants {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final int JWT_PREFIX_LENGTH = BEARER_PREFIX.length(); // 7
    public static final String TOKEN_TYPE = "Bearer";

    private SecurityConstants() {
        // Prevent instantiation
    }
}
