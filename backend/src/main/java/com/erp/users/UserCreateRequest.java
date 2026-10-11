package com.erp.users;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
        @NotBlank
        @Size(max = 80)
        String username,

        @NotBlank
        @Size(min = 12, max = 72)
        String password,

        @NotNull
        Role role
) {}
