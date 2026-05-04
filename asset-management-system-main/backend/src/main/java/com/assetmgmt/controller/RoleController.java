package com.assetmgmt.controller;

import com.assetmgmt.dto.RoleDto;
import com.assetmgmt.service.RoleService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@PreAuthorize("hasRole('ADMIN')")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    public ResponseEntity<List<RoleDto.RoleResponse>> getAllRoles() {
        return ResponseEntity.ok(roleService.getAllRoles());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoleDto.RoleResponse> getRoleById(@PathVariable Long id) {
        return ResponseEntity.ok(roleService.getRoleById(id));
    }

    @PostMapping
    public ResponseEntity<RoleDto.RoleResponse> createRole(@Valid @RequestBody RoleDto.CreateRoleRequest request) {
        return ResponseEntity.ok(roleService.createRole(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoleDto.RoleResponse> updateRole(@PathVariable Long id, @Valid @RequestBody RoleDto.UpdateRoleRequest request) {
        return ResponseEntity.ok(roleService.updateRole(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/permissions")
    public ResponseEntity<RoleDto.RoleResponse> assignPermissions(@PathVariable Long id, @Valid @RequestBody RoleDto.AssignPermissionsRequest request) {
        return ResponseEntity.ok(roleService.assignPermissions(id, request));
    }

    @GetMapping("/permissions")
    public ResponseEntity<List<RoleDto.PermissionResponse>> getAllPermissions() {
        return ResponseEntity.ok(roleService.getAllPermissions());
    }
}
