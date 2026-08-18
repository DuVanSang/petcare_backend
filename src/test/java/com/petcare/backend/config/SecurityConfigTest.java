package com.petcare.backend.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.petcare.backend.security.JwtAuthenticationFilter;
import com.petcare.backend.security.RestAccessDeniedHandler;
import com.petcare.backend.security.RestAuthenticationEntryPoint;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

class SecurityConfigTest {
    private final JwtAuthenticationFilter jwtFilter = mock(JwtAuthenticationFilter.class);
    private final RestAuthenticationEntryPoint entryPoint = mock(RestAuthenticationEntryPoint.class);
    private final RestAccessDeniedHandler deniedHandler = mock(RestAccessDeniedHandler.class);
    private final UserDetailsService userDetails = mock(UserDetailsService.class);
    private final SecurityConfig config = new SecurityConfig(jwtFilter, entryPoint, deniedHandler, userDetails);

    @Test
    void corsPasswordAndAuthenticationProviderBeans_HaveExpectedSettings() {
        CorsConfigurationSource source = config.corsConfigurationSource();
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/v1/pets");
        request.setRequestURI("/api/v1/pets");
        CorsConfiguration cors = source.getCorsConfiguration(request);
        PasswordEncoder encoder = config.passwordEncoder();
        AuthenticationProvider provider = config.authenticationProvider();

        assertThat(cors.getAllowedOriginPatterns()).containsExactly("*");
        assertThat(cors.getAllowedMethods()).containsExactly("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
        assertThat(cors.getAllowedHeaders()).containsExactly("*");
        assertThat(cors.getAllowCredentials()).isTrue();
        assertThat(cors.getMaxAge()).isEqualTo(3600L);
        assertThat(encoder.matches("secret", encoder.encode("secret"))).isTrue();
        assertThat(encoder.matches("wrong", encoder.encode("secret"))).isFalse();
        assertThat(provider).isNotNull();
    }

    @Test
    void authenticationManager_DelegatesToSpringConfiguration() throws Exception {
        AuthenticationConfiguration configuration = mock(AuthenticationConfiguration.class);
        AuthenticationManager manager = mock(AuthenticationManager.class);
        when(configuration.getAuthenticationManager()).thenReturn(manager);

        assertThat(config.authenticationManager(configuration)).isSameAs(manager);
    }

    @Test
    void securityFilterChain_BuildsStatelessChainWithJwtFilter() throws Exception {
        ObjectPostProcessor<Object> postProcessor = new ObjectPostProcessor<>() {
            @Override public <O> O postProcess(O object) { return object; }
        };
        AuthenticationManagerBuilder builder = new AuthenticationManagerBuilder(postProcessor);
        Map<Class<?>, Object> sharedObjects = new HashMap<>();
        ApplicationContext context = new StaticApplicationContext();
        sharedObjects.put(ApplicationContext.class, context);
        HttpSecurity http = new HttpSecurity(postProcessor, builder, sharedObjects);

        SecurityFilterChain chain = config.securityFilterChain(http);

        assertThat(chain).isNotNull();
        assertThat(chain.getFilters()).contains(jwtFilter);
    }
}
