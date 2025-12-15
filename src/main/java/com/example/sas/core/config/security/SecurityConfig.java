package com.example.sas.core.config.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                // Allow public access to Swagger/OpenAPI endpoints
                .requestMatchers(
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**",
                    "/api-docs/**",
                    "/swagger-resources/**",
                    "/webjars/**"
                ).permitAll()
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .httpBasic(httpBasic -> {})
            .csrf(csrf -> csrf.disable()); // Disable CSRF for API testing (enable in production with proper tokens)

        return http.build();
    }

@Bean
public UserDetailsService userDetailsService(
    @Value("${app.security.default-username:admin}") String username,
    @Value("${app.security.default-password:admin123}") String password,
    @Value("${app.security.default-roles:USER,ADMIN}") String roles
) {
    // In-memory user for development/testing (configured via application properties)
    UserDetails user = User.builder()
        .username(username)
        .password(passwordEncoder().encode(password))
        .roles(roles.split("\\s*,\\s*"))
        .build();

    return new InMemoryUserDetailsManager(user);
}

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

