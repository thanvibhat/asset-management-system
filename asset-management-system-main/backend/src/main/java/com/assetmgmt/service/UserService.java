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

    public UserService(UserRepository userRepository, 
                       RoleRepository roleRepository, 
                       PasswordEncoder passwordEncoder,
                       AuthService authService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
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

    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        userRepository.delete(user);
    }

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
}
