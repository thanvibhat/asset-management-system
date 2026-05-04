package com.assetmgmt.dto;

import com.assetmgmt.entity.Permission;
import com.assetmgmt.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.stream.Collectors;

public class RoleDto {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PermissionResponse {
        private Long id;
        private String name;
        private String description;

        public static PermissionResponse fromPermission(Permission p) {
            return PermissionResponse.builder()
                    .id(p.getId())
                    .name(p.getName())
                    .description(p.getDescription())
                    .build();
        }
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class RoleResponse {
        private Long id;
        private String name;
        private String description;
        private List<PermissionResponse> permissions;

        public static RoleResponse fromRole(Role r) {
            return RoleResponse.builder()
                    .id(r.getId())
                    .name(r.getName())
                    .description(r.getDescription())
                    .permissions(r.getPermissions().stream()
                            .map(PermissionResponse::fromPermission)
                            .collect(Collectors.toList()))
                    .build();
        }
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class CreateRoleRequest {
        @NotBlank(message = "Role name is required")
        private String name;
        private String description;
        private List<Long> permissionIds;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class UpdateRoleRequest {
        private String description;
        private List<Long> permissionIds;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class AssignPermissionsRequest {
        @NotNull(message = "Permission IDs are required")
        private List<Long> permissionIds;
    }
}
