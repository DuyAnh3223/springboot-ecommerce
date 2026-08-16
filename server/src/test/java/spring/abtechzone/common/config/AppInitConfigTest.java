package spring.abtechzone.common.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import spring.abtechzone.common.constant.PredefinedRole;
import spring.abtechzone.modules.auth.entity.Role;
import spring.abtechzone.modules.auth.entity.UserRole;
import spring.abtechzone.modules.auth.repository.RoleRepository;
import spring.abtechzone.modules.auth.repository.UserRoleRepository;
import spring.abtechzone.modules.user.entity.User;
import spring.abtechzone.modules.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AppInitConfigTest {

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    UserRepository userRepository;

    @Mock
    RoleRepository roleRepository;

    @Mock
    UserRoleRepository userRoleRepository;

    @Test
    @DisplayName("validateBootstrapCredentials: throws exception when username is blank")
    void validate_blankUsername_throwsException() {
        assertThatThrownBy(() -> AppInitConfig.validateBootstrapCredentials("", "StrongPass123!"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("username is required");

        assertThatThrownBy(() -> AppInitConfig.validateBootstrapCredentials(null, "StrongPass123!"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("username is required");
    }

    @Test
    @DisplayName("validateBootstrapCredentials: throws exception when password is shorter than 12 characters")
    void validate_shortPassword_throwsException() {
        assertThatThrownBy(() -> AppInitConfig.validateBootstrapCredentials("admin_user", "Short1!"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 12 characters");
    }

    @Test
    @DisplayName("validateBootstrapCredentials: throws exception when password lacks complexity")
    void validate_weakComplexity_throwsException() {
        // No uppercase
        assertThatThrownBy(() -> AppInitConfig.validateBootstrapCredentials("admin_user", "weakpassword123!"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("uppercase, lowercase, digit, and special character");

        // No lowercase
        assertThatThrownBy(() -> AppInitConfig.validateBootstrapCredentials("admin_user", "WEAKPASSWORD123!"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("uppercase, lowercase, digit, and special character");

        // No digit
        assertThatThrownBy(() -> AppInitConfig.validateBootstrapCredentials("admin_user", "WeakPasswordSpecial!"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("uppercase, lowercase, digit, and special character");

        // No special character
        assertThatThrownBy(() -> AppInitConfig.validateBootstrapCredentials("admin_user", "WeakPassword1234"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("uppercase, lowercase, digit, and special character");
    }

    @Test
    @DisplayName("validateBootstrapCredentials: throws exception when password is a known default or equals username")
    void validate_knownDefaultOrUsernameMatch_throwsException() {
        assertThatThrownBy(() -> AppInitConfig.validateBootstrapCredentials("Admin_User", "Admin_User1234!"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("password cannot be a known default or match username");

        assertThatThrownBy(() -> AppInitConfig.validateBootstrapCredentials("superuser", "Admin1234567!@#"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("password cannot be a known default or match username");
    }

    @Test
    @DisplayName("validateBootstrapCredentials: succeeds with valid strong credentials")
    void validate_strongCredentials_succeeds() {
        AppInitConfig.validateBootstrapCredentials("system_admin", "Secur3P@ssw0rd!2026");
    }

    @Test
    @DisplayName("runner: skips initialization when admin user already exists")
    void runner_userAlreadyExists_skipsCreation() throws Exception {
        AppInitConfig config = new AppInitConfig(passwordEncoder);
        ReflectionTestUtils.setField(config, "bootstrapUsername", "system_admin");
        ReflectionTestUtils.setField(config, "bootstrapPassword", "Secur3P@ssw0rd!2026");
        ReflectionTestUtils.setField(config, "bootstrapEmail", "admin@abtechzone.com");

        when(userRepository.findByUsername("system_admin"))
                .thenReturn(Optional.of(User.builder().username("system_admin").build()));

        ApplicationRunner runner = config.applicationRunner(userRepository, roleRepository, userRoleRepository);
        runner.run(mock(ApplicationArguments.class));

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    @DisplayName("runner: throws IllegalStateException when admin role is missing in database")
    void runner_missingAdminRole_throwsIllegalStateException() {
        AppInitConfig config = new AppInitConfig(passwordEncoder);
        ReflectionTestUtils.setField(config, "bootstrapUsername", "system_admin");
        ReflectionTestUtils.setField(config, "bootstrapPassword", "Secur3P@ssw0rd!2026");
        ReflectionTestUtils.setField(config, "bootstrapEmail", "admin@abtechzone.com");

        when(userRepository.findByUsername("system_admin")).thenReturn(Optional.empty());
        when(roleRepository.findByName(PredefinedRole.ADMIN_ROLE)).thenReturn(Optional.empty());

        ApplicationRunner runner = config.applicationRunner(userRepository, roleRepository, userRoleRepository);

        assertThatThrownBy(() -> runner.run(mock(ApplicationArguments.class)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Admin role 'ADMIN' does not exist");
    }

    @Test
    @DisplayName("runner: successfully creates admin user and assigns admin role")
    void runner_validCredentialsAndExistingRole_createsAdmin() throws Exception {
        AppInitConfig config = new AppInitConfig(passwordEncoder);
        ReflectionTestUtils.setField(config, "bootstrapUsername", "system_admin");
        ReflectionTestUtils.setField(config, "bootstrapPassword", "Secur3P@ssw0rd!2026");
        ReflectionTestUtils.setField(config, "bootstrapEmail", "admin@abtechzone.com");

        Role adminRole = Role.builder().name(PredefinedRole.ADMIN_ROLE).build();
        ReflectionTestUtils.setField(adminRole, "id", 1L);

        when(userRepository.findByUsername("system_admin")).thenReturn(Optional.empty());
        when(roleRepository.findByName(PredefinedRole.ADMIN_ROLE)).thenReturn(Optional.of(adminRole));
        when(passwordEncoder.encode("Secur3P@ssw0rd!2026")).thenReturn("$2a$10$encodedSecurePassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            ReflectionTestUtils.setField(u, "id", UUID.randomUUID());
            return u;
        });

        ApplicationRunner runner = config.applicationRunner(userRepository, roleRepository, userRoleRepository);
        runner.run(mock(ApplicationArguments.class));

        verify(userRepository).save(any(User.class));
        verify(userRoleRepository).save(any(UserRole.class));
        verify(passwordEncoder).encode("Secur3P@ssw0rd!2026");
    }
}
