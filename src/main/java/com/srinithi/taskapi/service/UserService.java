package com.srinithi.taskapi.service;

import com.srinithi.taskapi.repository.UserRepository;
import org.springframework.stereotype.Service;

// Business-logic layer for users: registration, lookups, admin listing.
// Filled in across Phase 5 (registration) and Phase 6 (admin listing).
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}
