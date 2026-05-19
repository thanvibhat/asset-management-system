package com.assetmgmt.service;

import com.assetmgmt.dto.AuthDto;
import com.assetmgmt.entity.Role;
import com.assetmgmt.entity.User;
import com.assetmgmt.exception.BusinessException;
import com.assetmgmt.exception.ResourceNotFoundException;
import com.assetmgmt.repository.RoleRepository;
import com.assetmgmt.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final com.assetmgmt.repository.AllocationRepository allocationRepository;
    private final com.assetmgmt.repository.AssetRepository assetRepository;
    private final com.assetmgmt.repository.MaintenanceRepository maintenanceRepository;
    private final com.assetmgmt.repository.ProcurementRepository procurementRepository;
    private final com.assetmgmt.repository.DocumentRepository documentRepository;
    private final com.assetmgmt.repository.NotificationRepository notificationRepository;
    private final EmailService emailService;

    public UserService(UserRepository userRepository, 
                       RoleRepository roleRepository, 
                       PasswordEncoder passwordEncoder,
                       AuthService authService,
                       com.assetmgmt.repository.AllocationRepository allocationRepository,
                       com.assetmgmt.repository.AssetRepository assetRepository,
                       com.assetmgmt.repository.MaintenanceRepository maintenanceRepository,
                       com.assetmgmt.repository.ProcurementRepository procurementRepository,
                       com.assetmgmt.repository.DocumentRepository documentRepository,
                       com.assetmgmt.repository.NotificationRepository notificationRepository,
                       EmailService emailService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
        this.allocationRepository = allocationRepository;
        this.assetRepository = assetRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.procurementRepository = procurementRepository;
        this.documentRepository = documentRepository;
        this.notificationRepository = notificationRepository;
        this.emailService = emailService;
    }

    public Page<AuthDto.UserDto> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(AuthDto.UserDto::fromUser);
    }

    public List<AuthDto.UserDto> searchUsers(String q) {
        return userRepository.search(q).stream()
                .map(AuthDto.UserDto::fromUser)
                .collect(java.util.stream.Collectors.toList());
    }

    public AuthDto.UserDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        return AuthDto.UserDto.fromUser(user);
    }

    @Transactional
    public AuthDto.UserDto updateUser(Long id, AuthDto.UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }

        if (request.getEmail() != null) {
            userRepository.findByEmail(request.getEmail()).ifPresent(existingUser -> {
                if (!existingUser.getId().equals(user.getId())) {
                    throw new BusinessException("Email already in use: " + request.getEmail());
                }
            });
            user.setEmail(request.getEmail());
        }

        if (request.getDepartment() != null) {
            user.setDepartment(request.getDepartment());
        }

        if (request.getEmployeeId() != null) {
            user.setEmployeeId(request.getEmployeeId());
        }

        if (request.getRole() != null) {
            Role role = roleRepository.findByName(request.getRole())
                    .orElseThrow(() -> new BusinessException("Role not found: " + request.getRole()));
            user.setRoles(new HashSet<>(List.of(role)));
        }

        if (request.getEnabled() != null) {
            user.setEnabled(request.getEnabled());
        }

        return AuthDto.UserDto.fromUser(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));

        // Check if user is associated with any transaction/activity:
        if (allocationRepository.existsByUserId(id) || allocationRepository.existsByAllocatedById(id)) {
            throw new BusinessException("Cannot delete user as they have associated allocations. You can deactivate them instead.");
        }

        if (assetRepository.existsByCreatedById(id)) {
            throw new BusinessException("Cannot delete user as they have created assets. You can deactivate them instead.");
        }

        if (maintenanceRepository.existsByCreatedById(id)) {
            throw new BusinessException("Cannot delete user as they have created maintenance records. You can deactivate them instead.");
        }

        if (procurementRepository.existsByCreatedById(id)) {
            throw new BusinessException("Cannot delete user as they have created procurements. You can deactivate them instead.");
        }

        if (documentRepository.existsByUploadedById(id)) {
            throw new BusinessException("Cannot delete user as they have uploaded documents. You can deactivate them instead.");
        }

        // Safe to delete. First clear notifications.
        notificationRepository.deleteByUserId(id);
        userRepository.delete(user);
    }

    @Transactional
    public void toggleUserStatus(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        user.setEnabled(!user.isEnabled());
        userRepository.save(user);
    }

    public List<AuthDto.UserDto> bulkCreateUsers(List<AuthDto.CreateUserRequest> requests) {
        List<AuthDto.UserDto> createdUsers = new ArrayList<>();
        for (AuthDto.CreateUserRequest request : requests) {
            try {
                createdUsers.add(authService.createUser(request));
            } catch (BusinessException e) {
                // Skip duplicates
            }
        }
        return createdUsers;
    }

    @Transactional
    public void resetPassword(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));

        String newPassword = generateRandomPassword();
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordResetRequired(true);
        userRepository.save(user);

        emailService.sendPasswordResetEmail(user.getEmail(), "http://localhost:4200/login", user.getUsername(), newPassword);
    }

    @Transactional
    public AuthDto.UserDto changePassword(String username, AuthDto.ChangePasswordRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("User not found"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException("Incorrect current password");
        }

        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new BusinessException("New password and confirmation do not match");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordResetRequired(false);
        return AuthDto.UserDto.fromUser(userRepository.save(user));
    }

    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        java.security.SecureRandom random = new java.security.SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
