package com.assetmgmt.controller;

import com.assetmgmt.dto.AuthDto;
import com.assetmgmt.entity.User;
import com.assetmgmt.repository.UserRepository;
import com.assetmgmt.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    public AuthController(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthDto.LoginResponse> login(@Valid @RequestBody AuthDto.LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthDto.UserDto> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        return ResponseEntity.ok(AuthDto.UserDto.fromUser(user));
    }

    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<List<AuthDto.UserDto>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll().stream().map(AuthDto.UserDto::fromUser).toList());
    }

    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuthDto.UserDto> createUser(@Valid @RequestBody AuthDto.CreateUserRequest request) {
        return ResponseEntity.ok(authService.createUser(request));
    }
}
