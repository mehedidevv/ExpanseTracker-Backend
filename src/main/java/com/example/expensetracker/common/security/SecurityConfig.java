package com.example.expensetracker.common.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private static final String[] PUBLIC_POST = {
            "/api/v1/auth/register", "/api/v1/auth/verify-email", "/api/v1/auth/resend-otp",
            "/api/v1/auth/login", "/api/v1/auth/refresh",
            "/api/v1/auth/forgot-password", "/api/v1/auth/verify-reset-otp", "/api/v1/auth/reset-password"
    };

    private static final String[] PUBLIC_GET = {
            "/actuator/health/**", "/actuator/info",
            "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**"
    };

    private final JwtAuthFilter jwtAuthFilter;
    private final SecurityErrorHandler securityErrorHandler;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, SecurityErrorHandler securityErrorHandler) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.securityErrorHandler = securityErrorHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, PUBLIC_POST).permitAll()
                        .requestMatchers(HttpMethod.GET, PUBLIC_GET).permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(securityErrorHandler)
                        .accessDeniedHandler(securityErrorHandler))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
