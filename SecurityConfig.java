package com.realestate.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * Session based security. Login is handled by AuthController, which stores the user in the HTTP session.
 * /api/admin/** -> ADMIN, /api/agent/** -> AGENT, /api/customer/** -> CUSTOMER.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public SecurityContextRepository securityContextRepository() { return new HttpSessionSecurityContextRepository(); }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, SecurityContextRepository repo) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)   // same-origin app using SameSite session cookie (see README)
            .securityContext(c -> c.securityContextRepository(repo))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/agent/**").hasRole("AGENT")
                .requestMatchers("/api/customer/**").hasRole("CUSTOMER")
                .requestMatchers("/api/notifications/**").authenticated()
                .anyRequest().permitAll())
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) -> {
                    res.setStatus(401);
                    res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    res.getWriter().write("{\"message\":\"Please login to continue\"}");
                })
                .accessDeniedHandler((req, res, ex) -> {
                    res.setStatus(403);
                    res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    res.getWriter().write("{\"message\":\"You are not allowed to access this resource\"}");
                }))
            .headers(h -> h.frameOptions(f -> f.sameOrigin()))
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
