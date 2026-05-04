package com.assetmgmt.service;

import com.assetmgmt.dto.AuthDto;
import com.assetmgmt.entity.User;
import com.assetmgmt.exception.BusinessException;
import com.assetmgmt.repository.RoleRepository;
import com.assetmgmt.repository.UserRepository;
import com.assetmgmt.security.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AuthenticationManager authenticationManager,
                       UserRepository userRepository,
                       RoleRepository roleRepository,
                       JwtUtil jwtUtil,
                       PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthDto.LoginResponse login(AuthDto.LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException("User not found"));
        
        String token = jwtUtil.generateToken(user);
        
        java.util.List<String> roles = user.getRoles().stream()
                .map(com.assetmgmt.entity.Role::getName)
                .collect(java.util.stream.Collectors.toList());
                
        java.util.List<String> permissions = user.getRoles().stream()
                .flatMap(r -> r.getPermissions().stream())
                .map(com.assetmgmt.entity.Permission::getName)
                .distinct()
                .collect(java.util.stream.Collectors.toList());

        return AuthDto.LoginResponse.builder()
                .token(token)
                .type("Bearer")
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .roles(roles)
                .permissions(permissions)
                .build();
    }

    public AuthDto.UserDto createUser(AuthDto.CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("Username already taken: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email already in use: " + request.getEmail());
        }
        
        com.assetmgmt.entity.Role role = roleRepository.findByName(request.getRole())
                .orElseThrow(() -> new BusinessException("Role not found: " + request.getRole()));

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .fullName(request.getFullName())
                .roles(new java.util.HashSet<>(java.util.List.of(role)))
                .enabled(true)
                .build();
        return AuthDto.UserDto.fromUser(userRepository.save(user));
    }
}
