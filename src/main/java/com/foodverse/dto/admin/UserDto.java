package com.foodverse.dto.admin;

import com.foodverse.model.enums.Role;

public record UserDto(
        Long id,
        String username,
        String email,
        Role role,
        boolean enabled
) {}
