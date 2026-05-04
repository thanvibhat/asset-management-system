package com.assetmgmt.service;

import com.assetmgmt.dto.RoleDto;
import com.assetmgmt.entity.Permission;
import com.assetmgmt.entity.Role;
import com.assetmgmt.exception.BusinessException;
import com.assetmgmt.exception.ResourceNotFoundException;
import com.assetmgmt.repository.PermissionRepository;
import com.assetmgmt.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public RoleService(RoleRepository roleRepository, PermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    @Transactional(readOnly = true)
    public List<RoleDto.RoleResponse> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(RoleDto.RoleResponse::fromRole)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RoleDto.RoleResponse getRoleById(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role", id));
        return RoleDto.RoleResponse.fromRole(role);
    }

    @Transactional(readOnly = true)
    public List<RoleDto.PermissionResponse> getAllPermissions() {
        return permissionRepository.findAll().stream()
                .map(RoleDto.PermissionResponse::fromPermission)
                .collect(Collectors.toList());
    }

    public RoleDto.RoleResponse createRole(RoleDto.CreateRoleRequest request) {
        if (roleRepository.findByName(request.getName()).isPresent()) {
            throw new BusinessException("Role name already exists: " + request.getName());
        }

        Set<Permission> permissions = new HashSet<>();
        if (request.getPermissionIds() != null) {
            for (Long pId : request.getPermissionIds()) {
                permissions.add(permissionRepository.findById(pId)
                        .orElseThrow(() -> new BusinessException("Permission not found with id: " + pId)));
            }
        }

        Role role = Role.builder()
                .name(request.getName())
                .description(request.getDescription())
                .permissions(permissions)
                .build();

        return RoleDto.RoleResponse.fromRole(roleRepository.save(role));
    }

    public RoleDto.RoleResponse updateRole(Long id, RoleDto.UpdateRoleRequest request) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role", id));

        if (request.getDescription() != null) {
            role.setDescription(request.getDescription());
        }

        if (request.getPermissionIds() != null) {
            Set<Permission> permissions = new HashSet<>();
            for (Long pId : request.getPermissionIds()) {
                permissions.add(permissionRepository.findById(pId)
                        .orElseThrow(() -> new BusinessException("Permission not found with id: " + pId)));
            }
            role.setPermissions(permissions);
        }

        return RoleDto.RoleResponse.fromRole(roleRepository.save(role));
    }

    public void deleteRole(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role", id));

        if ("ROLE_ADMIN".equals(role.getName())) {
            throw new BusinessException("Cannot delete ROLE_ADMIN");
        }

        roleRepository.delete(role);
    }

    public RoleDto.RoleResponse assignPermissions(Long roleId, RoleDto.AssignPermissionsRequest request) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role", roleId));

        Set<Permission> permissions = new HashSet<>();
        for (Long pId : request.getPermissionIds()) {
            permissions.add(permissionRepository.findById(pId)
                    .orElseThrow(() -> new BusinessException("Permission not found with id: " + pId)));
        }
        role.setPermissions(permissions);

        return RoleDto.RoleResponse.fromRole(roleRepository.save(role));
    }
}
