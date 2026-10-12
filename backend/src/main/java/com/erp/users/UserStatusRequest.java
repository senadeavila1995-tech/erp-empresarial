package com.erp.users;

import jakarta.validation.constraints.NotNull;

public record UserStatusRequest(
        @NotNull Boolean activo
) {}
