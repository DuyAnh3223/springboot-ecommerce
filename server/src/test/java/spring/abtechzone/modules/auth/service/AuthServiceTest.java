package spring.abtechzone.modules.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.modules.auth.dto.request.AuthRequest;
import spring.abtechzone.modules.auth.dto.response.AuthResponse;
import spring.abtechzone.modules.auth.entity.Role;
import spring.abtechzone.modules.auth.entity.UserRole;
import spring.abtechzone.modules.auth.entity.UserRoleId;
import spring.abtechzone.modules.auth.repository.InvalidatedTokenRepository;
import spring.abtechzone.modules.user.entity.User;
import spring.abtechzone.modules.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    InvalidatedTokenRepository invalidatedTokenRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    AuthService authService;

    private static final String TEST_SIGNER_KEY = "1234567890123456789012345678901234567890123456789012345678901234";

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, invalidatedTokenRepository, passwordEncoder);
        ReflectionTestUtils.setField(authService, "signerKey", TEST_SIGNER_KEY);
        ReflectionTestUtils.setField(authService, "validDuration", 3600L);
        ReflectionTestUtils.setField(authService, "refreshableDuration", 7200L);
    }

    @Test
    @DisplayName("authenticate: successfully matches password via injected PasswordEncoder")
    void authenticate_success() {
        // Given
        AuthRequest request = AuthRequest.builder()
                .username("john_doe")
                .password("Password123!")
                .build();

        Role role = Role.builder().name("USER").build();
        User user = User.builder()
                .id(UUID.randomUUID())
                .username("john_doe")
                .passwordHash("$2a$10$hashedPasswordValue")
                .build();
        UserRole userRole = UserRole.builder()
                .id(new UserRoleId(user.getId(), role.getId(), UUID.randomUUID()))
                .user(user)
                .role(role)
                .build();
        user.setRoles(Set.of(userRole));

        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123!", "$2a$10$hashedPasswordValue"))
                .thenReturn(true);

        // When
        AuthResponse response = authService.authenticate(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.isAuthenticated()).isTrue();
        assertThat(response.getToken()).isNotBlank();
        verify(passwordEncoder).matches("Password123!", "$2a$10$hashedPasswordValue");
    }

    @Test
    @DisplayName("authenticate: throws UNAUTHENTICATED when password mismatch")
    void authenticate_passwordMismatch_throwsUnauthenticated() {
        // Given
        AuthRequest request = AuthRequest.builder()
                .username("john_doe")
                .password("WrongPassword")
                .build();

        User user = User.builder()
                .id(UUID.randomUUID())
                .username("john_doe")
                .passwordHash("$2a$10$hashedPasswordValue")
                .build();

        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword", "$2a$10$hashedPasswordValue"))
                .thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.authenticate(request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHENTICATED);

        verify(passwordEncoder).matches("WrongPassword", "$2a$10$hashedPasswordValue");
    }

    @Test
    @DisplayName("authenticate: throws USER_NOT_EXISTED when user not found")
    void authenticate_userNotFound_throwsUserNotExisted() {
        // Given
        AuthRequest request = AuthRequest.builder()
                .username("nonexistent")
                .password("Password123!")
                .build();

        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.authenticate(request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_EXISTED);

        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }
}
