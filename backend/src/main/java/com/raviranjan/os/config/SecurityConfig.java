package com.raviranjan.os.config;

import com.raviranjan.os.security.CustomUserDetailsService;
import com.raviranjan.os.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /** Kill-switch: set APP_SECURITY_ENFORCE_ADMIN_AUTH=false only while debugging a login problem. */
    @Value("${app.security.enforce-admin-auth:true}")
    private boolean enforceAdminAuth;

    /** Comma-separated list of allowed browser origins, e.g. https://oneaios.vercel.app. "*" = any origin. */
    @Value("${app.cors.allowed-origins:*}")
    private String corsAllowedOrigins;

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(CustomUserDetailsService userDetailsService, JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.userDetailsService = userDetailsService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .authorizeHttpRequests(auth -> {
                // Public & static endpoints
                auth.requestMatchers(
                    "/",
                    "/index.html",
                    "/admin",
                    "/admin_cms.html",
                    "/*.js",
                    "/*.css",
                    "/api",
                    "/error",
                    "/favicon.ico",
                    "/api/v1/auth/**",
                    "/api/projects/**",
                    "/api/skills/**",
                    "/api/articles/**",
                    "/api/research/**",
                    "/api/lab/**",
                    "/api/experiences/**",
                    "/api/education/**",
                    "/api/achievements/**",
                    "/api/certifications/**",
                    "/api/stories/**",
                    "/api/activity/**",
                    "/api/dsa/**",
                    "/api/profile/**",
                    "/api/contact/**",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/actuator/health",
                    "/api/settings/**",
                    "/api/analytics/**",
                    "/api/ai/**",
                    "/api/integrations/webhook/**"
                ).permitAll();
                // Admin API: a valid admin JWT is REQUIRED (it used to be permitAll)
                if (enforceAdminAuth) {
                    auth.requestMatchers("/api/admin/**").hasAnyRole("SUPER_ADMIN", "ADMIN");
                } else {
                    auth.requestMatchers("/api/admin/**").permitAll();
                }
                auth.anyRequest().permitAll();
            });

        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = java.util.Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim).filter(o -> !o.isEmpty()).toList();
        boolean wildcard = origins.isEmpty() || origins.contains("*");
        configuration.setAllowedOriginPatterns(wildcard ? List.of("*") : origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        // Auth uses a Bearer header, not cookies, so credentials are only needed for an explicit origin list.
        configuration.setAllowCredentials(!wildcard);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
