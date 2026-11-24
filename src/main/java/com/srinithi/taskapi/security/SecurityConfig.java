package com.srinithi.taskapi.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

// TEMPORARY: having spring-boot-starter-security on the classpath locks every endpoint
// behind basic-auth login by default. This opens everything up until Phase 6, where we
// replace this whole class with real JWT + role-based rules.
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    // BCrypt: a deliberately slow, salted hashing algorithm built for passwords.
    // "Deliberately slow" is a feature - it makes brute-forcing every possible
    // password prohibitively expensive, unlike a fast general-purpose hash like SHA-256.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
