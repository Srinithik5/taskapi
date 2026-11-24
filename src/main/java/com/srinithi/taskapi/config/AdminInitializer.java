package com.srinithi.taskapi.config;

import com.srinithi.taskapi.entity.Role;
import com.srinithi.taskapi.entity.User;
import com.srinithi.taskapi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// Runs once at startup: creates the first ADMIN account from env vars, if one doesn't
// already exist. This is the ONLY way an ADMIN ever gets created - there is
// deliberately no "promote to admin" API endpoint, since a USER being able to grant
// themselves admin would defeat the whole point of having roles.
@Component
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public AdminInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.email:}") String adminEmail,
            @Value("${app.admin.password:}") String adminPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            return;
        }
        if (userRepository.existsByEmail(adminEmail)) {
            return;
        }
        User admin = new User(adminEmail, passwordEncoder.encode(adminPassword), "Admin", Role.ADMIN);
        userRepository.save(admin);
    }
}
