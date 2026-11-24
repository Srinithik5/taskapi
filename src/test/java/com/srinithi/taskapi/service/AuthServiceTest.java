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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService);
    }

    @Test
    void register_savesUser_withHashedPassword() {
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("plaintext123")).thenReturn("bcrypt-hash");

        UserResponse response = authService.register(new RegisterRequest("new@example.com", "plaintext123", "New Person"));

        assertThat(response.email()).isEqualTo("new@example.com");
        assertThat(response.role()).isEqualTo(Role.USER);
        verify(userRepository).save(argThat(u -> u.getPasswordHash().equals("bcrypt-hash")));
    }

    @Test
    void register_throwsConflict_whenEmailAlreadyRegistered() {
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("taken@example.com", "password123", "X")))
                .isInstanceOf(ConflictException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_returnsToken_withCorrectPassword() {
        User user = existingUser();
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct-password", "stored-hash")).thenReturn(true);
        when(jwtService.generateToken("alice@example.com", "USER")).thenReturn("signed.jwt.token");

        AuthResponse response = authService.login(new LoginRequest("alice@example.com", "correct-password"));

        assertThat(response.token()).isEqualTo("signed.jwt.token");
        assertThat(response.role()).isEqualTo(Role.USER);
    }

    @Test
    void login_throwsUnauthorized_withWrongPassword() {
        User user = existingUser();
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "stored-hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("alice@example.com", "wrong-password")))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void login_throwsUnauthorized_withSameMessage_whenEmailDoesNotExist() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        // Deliberately the same exception/message as a wrong password (see
        // AuthService.login) - this test locks in that anti-enumeration behaviour.
        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@example.com", "whatever")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid email or password");
    }

    private User existingUser() {
        User user = new User("alice@example.com", "stored-hash", "Alice", Role.USER);
        ReflectionTestUtils.setField(user, "id", 1L);
        return user;
    }
}
