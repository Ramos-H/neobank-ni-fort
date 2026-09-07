package com.neobank.auth_service.service;

import com.neobank.auth_service.dto.*;
import com.neobank.auth_service.entity.User;
import com.neobank.auth_service.repository.UserRepository;
import com.neobank.auth_service.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService);
    }

    @Test
    void register_success_returnsCustomerInfo() {
        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(userRepository.existsByEmail("john@gmail.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        RegisterRequest req = new RegisterRequest();
        req.setUsername("john");
        req.setEmail("john@gmail.com");
        req.setPassword("Password123!");

        RegisterResponse resp = authService.register(req);

        assertThat(resp.getMessage()).isEqualTo("Registration successful");
        assertThat(resp.getUsername()).isEqualTo("john");
        assertThat(resp.getEmail()).isEqualTo("john@gmail.com");
        assertThat(resp.getCustomerId()).startsWith("CUS-");
    }

    @Test
    void register_passwordIsBcryptHashed() {
        when(userRepository.existsByUsername(any())).thenReturn(false);
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            assertThat(u.getPassword()).startsWith("$2a$");
            assertThat(u.getPassword()).isNotEqualTo("Password123!");
            return u;
        });

        RegisterRequest req = new RegisterRequest();
        req.setUsername("john");
        req.setEmail("john@gmail.com");
        req.setPassword("Password123!");
        authService.register(req);
    }

    @Test
    void register_passwordNeverReturned() {
        when(userRepository.existsByUsername(any())).thenReturn(false);
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        RegisterRequest req = new RegisterRequest();
        req.setUsername("john");
        req.setEmail("john@gmail.com");
        req.setPassword("Password123!");

        RegisterResponse resp = authService.register(req);
        // RegisterResponse has no password field — compile-time guarantee
        assertThat(resp).isNotNull();
    }

    @Test
    void register_customerIdGenerated() {
        when(userRepository.existsByUsername(any())).thenReturn(false);
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        RegisterRequest req = new RegisterRequest();
        req.setUsername("john");
        req.setEmail("john@gmail.com");
        req.setPassword("Password123!");

        RegisterResponse resp = authService.register(req);
        assertThat(resp.getCustomerId()).matches("CUS-[A-F0-9]{8}");
    }

    @Test
    void register_duplicateUsername_throws409() {
        when(userRepository.existsByUsername("john")).thenReturn(true);

        RegisterRequest req = new RegisterRequest();
        req.setUsername("john");
        req.setEmail("john@gmail.com");
        req.setPassword("Password123!");

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Username already exists");
    }

    @Test
    void register_duplicateEmail_throws409() {
        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(userRepository.existsByEmail("john@gmail.com")).thenReturn(true);

        RegisterRequest req = new RegisterRequest();
        req.setUsername("john");
        req.setEmail("john@gmail.com");
        req.setPassword("Password123!");

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Email already exists");
    }

    @Test
    void login_validCredentials_returnsToken() {
        User user = new User();
        user.setUsername("john");
        user.setCustomerId("CUS-A82F91BC");
        user.setPassword(passwordEncoder.encode("Password123!"));

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(jwtService.generateToken("john", "CUS-A82F91BC")).thenReturn("mock.jwt.token");

        LoginRequest req = new LoginRequest();
        req.setUsername("john");
        req.setPassword("Password123!");

        LoginResponse resp = authService.login(req);
        assertThat(resp.getToken()).isEqualTo("mock.jwt.token");
        assertThat(resp.getCustomerId()).isEqualTo("CUS-A82F91BC");
        assertThat(resp.getUsername()).isEqualTo("john");
        assertThat(resp.getType()).isEqualTo("Bearer");
    }

    @Test
    void login_invalidPassword_throws401() {
        User user = new User();
        user.setUsername("john");
        user.setPassword(passwordEncoder.encode("Password123!"));

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));

        LoginRequest req = new LoginRequest();
        req.setUsername("john");
        req.setPassword("WrongPassword!");

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid credentials");
    }

    @Test
    void login_unknownUsername_throws401() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        LoginRequest req = new LoginRequest();
        req.setUsername("unknown");
        req.setPassword("Password123!");

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid credentials");
    }
}
