package spring.abtechzone.common.config;

import java.util.UUID;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

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
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AppInitConfig {

    PasswordEncoder passwordEncoder;

    @NonFinal
    static final String ADMIN_USER_NAME = "admin";

    @NonFinal
    static final String ADMIN_PASSWORD = "admin";

    @Bean
    ApplicationRunner applicationRunner(
            UserRepository userRepository, RoleRepository roleRepository, UserRoleRepository userRoleRepository) {
        log.info("Init ApplicationRunner");
        return args -> {
            if (userRepository.findByUsername(ADMIN_USER_NAME).isEmpty()) {

                roleRepository.save(Role.builder()
                        .name(PredefinedRole.USER_ROLE)
                        .description("User role")
                        .build());

                Role adminRole = roleRepository.save(Role.builder()
                        .name(PredefinedRole.ADMIN_ROLE)
                        .description("Admin role")
                        .build());

                User user = User.builder()
                        .username(ADMIN_USER_NAME)
                        .passwordHash(passwordEncoder.encode(ADMIN_PASSWORD))
                        .build();

                user = userRepository.save(user);

                UUID globalScopeId = UUID.fromString("00000000-0000-0000-0000-000000000000");
                UserRole userRole = UserRole.builder()
                        .id(new UserRoleId(user.getId(), adminRole.getId(), globalScopeId))
                        .user(user)
                        .role(adminRole)
                        .build();
                userRoleRepository.save(userRole);

                log.warn("admin user has been created with default password: admin");
            }
        };
    }
}
