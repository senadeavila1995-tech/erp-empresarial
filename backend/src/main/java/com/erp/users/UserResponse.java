package com.erp.users;

public record UserResponse(
        Long id,
        String username,
        Role role,
        boolean activo
) {
    public static UserResponse from(AppUser user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.isActivo()
        );
    }
}
