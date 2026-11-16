package com.filebridge.service;

import com.filebridge.dto.AuthResponse;
import com.filebridge.dto.LoginRequest;
import com.filebridge.dto.RegisterRequest;
import com.filebridge.dto.UserProfileResponse;
import com.filebridge.model.User;
import com.filebridge.repository.UserRepository;
import com.filebridge.security.JwtProvider;
import com.filebridge.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtProvider jwtProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtProvider = jwtProvider;
    }

    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalArgumentException("An account with this email address already exists.");
        }

        User user = User.builder()
            .name(request.getName().trim())
            .email(normalizedEmail)
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .role("ROLE_USER") // Enforced safe default role; client cannot select role
            .createdAt(Instant.now())
            .build();

        User savedUser = userRepository.save(user);
        log.info("Registered new user with id: {}", savedUser.getId());

        UserPrincipal principal = new UserPrincipal(
            savedUser.getId(),
            savedUser.getName(),
            savedUser.getEmail(),
            savedUser.getPasswordHash(),
            savedUser.getRole()
        );

        String token = jwtProvider.generateToken(principal);

        return new AuthResponse(
            token,
            new UserProfileResponse(savedUser.getId(), savedUser.getName(), savedUser.getEmail())
        );
    }

    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword())
            );
        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("Invalid email or password.");
        }

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String token = jwtProvider.generateToken(principal);

        return new AuthResponse(
            token,
            new UserProfileResponse(principal.getId(), principal.getName(), principal.getEmail())
        );
    }

    public UserProfileResponse getProfile(UserPrincipal principal) {
        return new UserProfileResponse(principal.getId(), principal.getName(), principal.getEmail());
    }

    private String normalizeEmail(String email) {
        if (email == null) return "";
        return email.trim().toLowerCase();
    }
}
