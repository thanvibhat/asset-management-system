package com.assetmgmt.dto;

import com.assetmgmt.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

public class AuthDto {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class LoginRequest {
        @NotBlank(message = "Username is required")
        private String username;

        @NotBlank(message = "Password is required")
        private String password;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class LoginResponse {
        private String token;
        private String type = "Bearer";
        private Long id;
        private String username;
        private String email;
        private String fullName;
        private java.util.List<String> roles;
        private java.util.List<String> permissions;
        private boolean passwordResetRequired;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class UserDto {
        private Long id;
        private String username;
        private String email;
        private String fullName;
        private String department;
        private String employeeId;
        private java.util.List<String> roles;
        private boolean enabled;
        private boolean passwordResetRequired;

        public static UserDto fromUser(User user) {
            return UserDto.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .fullName(user.getFullName())
                    .department(user.getDepartment())
                    .employeeId(user.getEmployeeId())
                    .roles(user.getRoles().stream().map(com.assetmgmt.entity.Role::getName).collect(java.util.stream.Collectors.toList()))
                    .enabled(user.isEnabled())
                    .passwordResetRequired(user.isPasswordResetRequired())
                    .build();
        }
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class CreateUserRequest {
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be 3-50 characters")
        private String username;

        private String password;

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        private String email;

        @NotBlank(message = "Full name is required")
        private String fullName;

        @NotBlank(message = "Role is required")
        private String role;

        private String department;
        private String employeeId;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class UpdateUserRequest {
        private String fullName;
        private String email;
        private String department;
        private String employeeId;
        private String role;
        private Boolean enabled;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class ChangePasswordRequest {
        @NotBlank(message = "Old password is required")
        private String oldPassword;

        @NotBlank(message = "New password is required")
        @Size(min = 6, message = "New password must be at least 6 characters")
        private String newPassword;

        @NotBlank(message = "Confirm new password is required")
        private String confirmNewPassword;
    }
}
