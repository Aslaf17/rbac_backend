package com.rbac.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rbac.dto.ApiError;
import com.rbac.security.CustomUserDetailsService;
import com.rbac.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final CustomUserDetailsService userDetailsService;

    @Value("${app.cors.allowed-origin}")
    private String allowedOrigin;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        // Public
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/ws/**").permitAll()

                        // Authenticated
                        .requestMatchers("/api/user/**").authenticated()

                        // Roles
                        .requestMatchers("/api/student/**").hasAnyRole("STUDENT", "ADMIN")
                        .requestMatchers("/api/teacher/**").hasAnyRole("TEACHER", "ADMIN")
                        .requestMatchers("/api/employer/**").hasAnyRole("EMPLOYER", "ADMIN")
                        .requestMatchers("/api/employee/**").hasAnyRole("EMPLOYEE", "ADMIN")

                        // Attendance
                        .requestMatchers(HttpMethod.POST, "/api/attendance/mark")
                        .hasAnyRole("STUDENT", "TEACHER", "ADMIN")

                        .requestMatchers(HttpMethod.PUT, "/api/attendance/update")
                        .hasAnyRole("TEACHER", "ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/attendance/session/**")
                        .hasAnyRole("TEACHER", "ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/attendance/student/**")
                        .hasAnyRole("STUDENT", "TEACHER", "ADMIN")

                        // Classroom
                        .requestMatchers(HttpMethod.POST, "/api/session/start")
                        .hasAnyRole("TEACHER", "ADMIN")

                        .requestMatchers(HttpMethod.PUT, "/api/session/*/end")
                        .hasAnyRole("TEACHER", "ADMIN")

                        .requestMatchers(HttpMethod.POST, "/api/session/*/join")
                        .hasAnyRole("STUDENT", "TEACHER", "ADMIN")

                        .requestMatchers(HttpMethod.PUT,
                                "/api/session/*/lock",
                                "/api/session/*/unlock")
                        .hasAnyRole("TEACHER", "ADMIN")

                        .requestMatchers("/api/session/*/participants/**")
                        .authenticated()

                        .requestMatchers("/api/session/*/hand/**")
                        .authenticated()

                        .requestMatchers(HttpMethod.PUT,
                                "/api/session/*/media/mic",
                                "/api/session/*/media/camera")
                        .authenticated()

                        .requestMatchers("/api/session/*/media/**")
                        .hasAnyRole("TEACHER", "ADMIN")

                        .requestMatchers("/api/session/*/permissions/**")
                        .hasAnyRole("TEACHER", "ADMIN")

                        .requestMatchers("/api/session/*/waiting-room/**")
                        .hasAnyRole("TEACHER", "ADMIN")

                        .requestMatchers("/api/session/*/logs/**")
                        .hasAnyRole("TEACHER", "ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/session/**")
                        .authenticated()

                        // Chat
                        .requestMatchers(HttpMethod.DELETE, "/api/chat/**")
                        .hasAnyRole("TEACHER", "ADMIN")

                        .requestMatchers("/api/chat/**")
                        .authenticated()

                        // Whiteboard
                        .requestMatchers(HttpMethod.DELETE, "/api/whiteboard/**")
                        .hasAnyRole("TEACHER", "ADMIN")

                        .requestMatchers("/api/whiteboard/**")
                        .authenticated()

                        // Notifications
                        .requestMatchers("/api/notifications/**")
                        .authenticated()

                        // Admin
                        .requestMatchers("/api/admin/**")
                        .hasRole("ADMIN")

                        // Feedback
                        .requestMatchers(HttpMethod.POST, "/api/feedback")
                        .hasRole("STUDENT")

                        .requestMatchers(HttpMethod.GET, "/api/feedback/**")
                        .hasAnyRole("TEACHER", "ADMIN")

                        .anyRequest().authenticated()
                )
                .exceptionHandling(handler -> handler
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler())
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        ObjectMapper mapper = new ObjectMapper();
        return (request, response, authException) -> {
            response.setStatus(401);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(mapper.writeValueAsString(
                    new ApiError(401, "Authentication required")));
        };
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        ObjectMapper mapper = new ObjectMapper();
        return (request, response, accessDeniedException) -> {
            response.setStatus(403);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(mapper.writeValueAsString(
                    new ApiError(403, "You do not have permission to access this resource")));
        };
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(allowedOrigin));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}


