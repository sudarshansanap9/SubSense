package com.subsense.user.service;

import com.subsense.user.dto.AuthResponse;
import com.subsense.user.dto.LoginRequest;
import com.subsense.user.dto.RegisterRequest;
import com.subsense.user.entity.Role;
import com.subsense.user.entity.User;
import com.subsense.user.exception.InvalidCredentialsException;
import com.subsense.user.exception.InvalidRefreshTokenException;
import com.subsense.user.exception.UserAlreadyExistsException;
import com.subsense.user.exception.UserNotFoundException;
import com.subsense.user.exception.TokenExpiredException;
import com.subsense.user.repository.UserRepository;
import com.subsense.user.security.JwtUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final long REFRESH_TOKEN_EXPIRATION_MS = 604800000L; // 7 days

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final TokenStoreService tokenStoreService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil, AuthenticationManager authenticationManager, TokenStoreService tokenStoreService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
        this.tokenStoreService = tokenStoreService;
    }

    public AuthResponse register(RegisterRequest request) {
        log.info("Attempting to register user with email {}", request.getEmail());
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed. User with email {} already exists.", request.getEmail());
            throw new UserAlreadyExistsException("Email already registered");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER); // default role

        user = userRepository.save(user);
        log.info("User {} registered successfully.", user.getEmail());

        return generateAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        log.info("Attempting login for user {}", request.getEmail());
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (AuthenticationException e) {
            log.warn("Invalid login attempt for {}", request.getEmail());
            throw new InvalidCredentialsException("Invalid email or password");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        log.info("User {} logged in successfully.", user.getEmail());
        return generateAuthResponse(user);
    }

    public AuthResponse refresh(String refreshToken) {
        try {
            String email = jwtUtil.extractUsername(refreshToken);
            
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UserNotFoundException("User not found for the given token"));
            
            String userId = user.getId().toString();
            
            if (!tokenStoreService.validateRefreshToken(userId, refreshToken)) {
                throw new InvalidRefreshTokenException("Refresh token is invalid or has been revoked");
            }
            
            log.info("Token refreshed successfully for user {}", user.getEmail());
            return generateAuthResponse(user);
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            throw new TokenExpiredException("Refresh token has expired");
        } catch (Exception e) {
            throw new InvalidRefreshTokenException("Invalid refresh token");
        }
    }

    public void logout(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String jwt = authHeader.substring(7);
            
            try {
                String email = jwtUtil.extractUsername(jwt);
                User user = userRepository.findByEmail(email).orElse(null);
                if (user != null) {
                    tokenStoreService.deleteRefreshToken(user.getId().toString());
                }
            } catch (Exception e) {
                // Ignore parsing errors for logout
            }

            long expirationMs = jwtUtil.getRemainingExpiration(jwt);
            if (expirationMs > 0) {
                tokenStoreService.blacklistToken(jwt, expirationMs);
                log.info("User logged out successfully and token blacklisted.");
            }
        }
    }

    private AuthResponse generateAuthResponse(User user) {
        org.springframework.security.core.userdetails.UserDetails userDetails = 
                org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
                .password(user.getPassword())
                .authorities(java.util.Collections.emptyList())
                .build();

        String accessToken = jwtUtil.generateAccessToken(userDetails, user.getId().toString(), user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(userDetails, user.getId().toString());

        tokenStoreService.storeRefreshToken(user.getId().toString(), refreshToken, REFRESH_TOKEN_EXPIRATION_MS);

        return new AuthResponse(
                accessToken,
                refreshToken,
                user.getId().toString(),
                user.getName(),
                user.getEmail(),
                user.getRole().name()
        );
    }
}
