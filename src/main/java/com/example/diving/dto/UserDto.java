package com.example.diving.dto;

import com.example.diving.domain.User;
import com.example.diving.domain.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UserDto {

    public record UserResponse(
            Long id,
            String email,
            String name,
            UserRole role
    ) {
        public static UserResponse from(User user) {
            return new UserResponse(user.id, user.email, user.name, user.role);
        }
    }

    public record UpdateProfileRequest(
            @NotBlank @Size(min = 2, max = 100) String name
    ) {}

    public record UpdateRoleRequest(
            @NotNull UserRole role
    ) {}
}

