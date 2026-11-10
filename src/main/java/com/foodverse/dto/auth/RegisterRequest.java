package com.foodverse.dto.auth;

import com.foodverse.model.enums.Role;
import jakarta.validation.constraints.*;

public record RegisterRequest(

        @NotBlank String username,

        @NotBlank @Size(min = 8) String password,

        @Email @NotBlank String email,

        @NotNull Role role,

        String adminKey
) {
    public RegisterRequest(String username, String password, String email, Role role) {
        this(username, password, email, role, null);
    }
}