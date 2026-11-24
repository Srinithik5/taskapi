package com.srinithi.taskapi.service;

import com.srinithi.taskapi.dto.UserResponse;
import com.srinithi.taskapi.entity.User;
import com.srinithi.taskapi.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Registration/login live in AuthService (Phase 5) since they're about proving
// identity, not managing user records. This service holds admin-only user listing.
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // ADMIN ONLY - called only from AdminController, which SecurityConfig already
    // restricts to ROLE_ADMIN.
    @Transactional(readOnly = true)
    public Page<UserResponse> listAll(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::toResponse);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getName(), user.getRole(), user.getCreatedAt());
    }
}
