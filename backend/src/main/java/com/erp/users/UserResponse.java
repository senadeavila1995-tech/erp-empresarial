package com.erp.users;

public record UserResponse(
        Long id,
        String username,
        Role role
) {
    public static UserResponse from(AppUser user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getRole()
        );
    }
}
