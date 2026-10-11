package com.erp.auth;

public record LoginResponse(
        String token,
        String tokenType,
        String username,
        String role
) {}
