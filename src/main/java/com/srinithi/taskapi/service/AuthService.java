package com.srinithi.taskapi.service;

import com.srinithi.taskapi.dto.AuthResponse;
import com.srinithi.taskapi.dto.LoginRequest;
import com.srinithi.taskapi.dto.RegisterRequest;
import com.srinithi.taskapi.dto.UserResponse;
import com.srinithi.taskapi.entity.Role;
import com.srinithi.taskapi.entity.User;
import com.srinithi.taskapi.exception.ConflictException;
import com.srinithi.taskapi.exception.UnauthorizedException;
import com.srinithi.taskapi.repository.UserRepository;
import com.srinithi.taskapi.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already registered");
        }

        User user = new User(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.name(),
                Role.USER
        );
        userRepository.save(user);
        return toUserResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        // BCrypt hashes are one-way: we can't decrypt the stored hash, only re-hash the
        // attempt with the same embedded salt and compare the two hashes for equality.
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        String token = jwtService.generateToken(user.getEmail(), user.getRole().name());
        return new AuthResponse(token, user.getRole());
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getName(), user.getRole(), user.getCreatedAt());
    }
}
