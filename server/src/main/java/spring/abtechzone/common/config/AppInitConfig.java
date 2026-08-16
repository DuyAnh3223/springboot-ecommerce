package spring.abtechzone.common.config;

import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import spring.abtechzone.common.constant.PredefinedRole;
import spring.abtechzone.modules.auth.entity.Role;
import spring.abtechzone.modules.auth.entity.UserRole;
import spring.abtechzone.modules.auth.entity.UserRoleId;
import spring.abtechzone.modules.auth.repository.RoleRepository;
import spring.abtechzone.modules.auth.repository.UserRoleRepository;
import spring.abtechzone.modules.user.entity.User;
import spring.abtechzone.modules.user.repository.UserRepository;

@Configuration
@ConditionalOnProperty(prefix = "app.bootstrap-admin", name = "enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AppInitConfig {

    PasswordEncoder passwordEncoder;

    @NonFinal
    @Value("${app.bootstrap-admin.username:}")
    String bootstrapUsername;

    @NonFinal
    @Value("${app.bootstrap-admin.password:}")
    String bootstrapPassword;

    @NonFinal
    @Value("${app.bootstrap-admin.email:admin@abtechzone.com}")
    String bootstrapEmail;

    private static final Set<String> REJECTED_PASSWORDS =
            Set.of("admin", "password", "123456", "12345678", "admin123", "administrator");

    @Bean
    ApplicationRunner applicationRunner(
            UserRepository userRepository, RoleRepository roleRepository, UserRoleRepository userRoleRepository) {
        return args -> {
            validateBootstrapCredentials(bootstrapUsername, bootstrapPassword);

            if (userRepository.findByUsername(bootstrapUsername).isPresent()) {
                log.info("Bootstrap admin user '{}' already exists, skipping initialization.", bootstrapUsername);
                return;
            }

            Role adminRole = roleRepository
                    .findByName(PredefinedRole.ADMIN_ROLE)
                    .orElseThrow(() -> new IllegalStateException("Admin role '" + PredefinedRole.ADMIN_ROLE
                            + "' does not exist. Role schema seed belongs to database migration scripts."));

            User user = User.builder()
                    .username(bootstrapUsername)
                    .email(bootstrapEmail)
                    .passwordHash(passwordEncoder.encode(bootstrapPassword))
                    .isActive(true)
                    .build();

            user = userRepository.save(user);

            UUID globalScopeId = UUID.fromString("00000000-0000-0000-0000-000000000000");
            UserRole userRole = UserRole.builder()
                    .id(new UserRoleId(user.getId(), adminRole.getId(), globalScopeId))
                    .user(user)
                    .role(adminRole)
                    .build();
            userRoleRepository.save(userRole);

            log.info("Bootstrap admin user '{}' created successfully.", bootstrapUsername);
        };
    }

    public static void validateBootstrapCredentials(String username, String password) {
        if (!StringUtils.hasText(username)) {
            throw new IllegalStateException(
                    "Bootstrap admin configuration is invalid: username is required and cannot be blank.");
        }

        if (!StringUtils.hasText(password) || password.length() < 12) {
            throw new IllegalStateException(
                    "Bootstrap admin configuration is invalid: password must be at least 12 characters long.");
        }

        boolean hasUpper = password.matches(".*[A-Z].*");
        boolean hasLower = password.matches(".*[a-z].*");
        boolean hasDigit = password.matches(".*[0-9].*");
        boolean hasSpecial = password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*");

        if (!hasUpper || !hasLower || !hasDigit || !hasSpecial) {
            throw new IllegalStateException(
                    "Bootstrap admin configuration is invalid: password must contain uppercase, lowercase, digit, and special character.");
        }

        String lowerPass = password.toLowerCase();
        String lowerUser = username.toLowerCase();
        boolean containsRejected = REJECTED_PASSWORDS.stream().anyMatch(lowerPass::contains);
        if (containsRejected || lowerPass.contains(lowerUser)) {
            throw new IllegalStateException(
                    "Bootstrap admin configuration is invalid: password cannot be a known default or match username.");
        }
    }
}
