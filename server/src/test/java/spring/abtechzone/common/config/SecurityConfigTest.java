package spring.abtechzone.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

class SecurityConfigTest {

    @Test
    @DisplayName("passwordEncoder bean returns BCryptPasswordEncoder")
    void passwordEncoder_returnsBCryptPasswordEncoder() {
        CustomJwtDecoder customJwtDecoder = mock(CustomJwtDecoder.class);
        SecurityConfig securityConfig = new SecurityConfig(customJwtDecoder);

        PasswordEncoder encoder = securityConfig.passwordEncoder();
        assertThat(encoder).isNotNull();

        String raw = "StrongSecret123!";
        String encoded = encoder.encode(raw);
        assertThat(encoder.matches(raw, encoded)).isTrue();
        assertThat(encoder.matches("WrongSecret", encoded)).isFalse();
    }

    @Test
    @DisplayName("corsConfigurationSource parses multiple comma-separated allowed origins")
    void corsConfigurationSource_parsesMultipleOrigins() {
        CustomJwtDecoder customJwtDecoder = mock(CustomJwtDecoder.class);
        SecurityConfig securityConfig = new SecurityConfig(customJwtDecoder);
        ReflectionTestUtils.setField(
                securityConfig,
                "corsAllowedOrigins",
                "http://localhost:3000, https://abtechzone.com, https://admin.abtechzone.com");

        UrlBasedCorsConfigurationSource source = securityConfig.corsConfigurationSource();
        assertThat(source).isNotNull();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/catalog/products");
        CorsConfiguration config = source.getCorsConfiguration(request);

        assertThat(config).isNotNull();
        assertThat(config.getAllowedOrigins())
                .containsExactly("http://localhost:3000", "https://abtechzone.com", "https://admin.abtechzone.com");
        assertThat(config.getAllowCredentials()).isTrue();
        assertThat(config.getAllowedMethods()).contains("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    }

    @Test
    @DisplayName("corsConfigurationSource handles empty allowed origins gracefully")
    void corsConfigurationSource_emptyOrigins_handlesGracefully() {
        CustomJwtDecoder customJwtDecoder = mock(CustomJwtDecoder.class);
        SecurityConfig securityConfig = new SecurityConfig(customJwtDecoder);
        ReflectionTestUtils.setField(securityConfig, "corsAllowedOrigins", "");

        UrlBasedCorsConfigurationSource source = securityConfig.corsConfigurationSource();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/catalog/products");
        CorsConfiguration config = source.getCorsConfiguration(request);

        assertThat(config).isNotNull();
        assertThat(config.getAllowedOrigins()).isNull();
    }
}
