package com.CaseStudyProject.user_service.service;

import com.CaseStudyProject.user_service.entity.User;
import com.CaseStudyProject.user_service.exception.BadRequestException;
import com.CaseStudyProject.user_service.exception.ResourceNotFoundException;
import com.CaseStudyProject.user_service.repository.UserRepository;
import com.CaseStudyProject.user_service.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    // Repository for DB operations
    private final UserRepository userRepository;

    // Utility for JWT generation
    private final JwtUtil jwt;

    /**
     * Create a new user
     * Validates duplicate email before saving
     */
    public User createUser(User user) {
        log.info("Service: Creating user with email: {}", user.getEmail());

        // Check if email already exists
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            log.warn("User creation failed - Email already exists: {}", user.getEmail());
            throw new BadRequestException("Email already exists");
        }

        User savedUser = userRepository.save(user);

        log.info("User successfully created with ID: {}", savedUser.getId());
        return savedUser;
    }

    /**
     * Fetch all users from DB
     */
    public List<User> getAllUsers() {
        log.info("Service: Fetching all users");

        List<User> users = userRepository.findAll();

        log.info("Total users fetched: {}", users.size());
        return users;
    }

    /**
     * Fetch user by ID
     */
    public User getUserById(Long id) {
        log.info("Service: Fetching user by ID: {}", id);

        return userRepository.findById(id).orElseThrow(() -> {
                    log.warn("User not found with ID: {}", id);
                    return new ResourceNotFoundException("User not found with id: " + id);
        });
    }

    /**
     * Update user details
     * ADMIN → can update everything
     * USER → cannot change role
     */
    public User updateUser(Long id, User updatedUser, String role) {
        log.info("Service: Updating user ID: {} with role: {}", id, role);

        User existing = getUserById(id);

        // Restrict role change for non-admin users
        if (!role.equals("ADMIN")) {
            log.info("Non-admin user attempting update - preserving existing role for user ID: {}", id);
            updatedUser.setRole(existing.getRole());
        }

        // Check if email is being changed and already exists
        if (!existing.getEmail().equals(updatedUser.getEmail()) && userRepository.findByEmail(updatedUser.getEmail()).isPresent()) {
            log.warn("Email update conflict for user ID: {} - Email already exists: {}", id, updatedUser.getEmail());
            throw new BadRequestException("Email already exists");
        }

        // Update fields
        existing.setName(updatedUser.getName());
        existing.setEmail(updatedUser.getEmail());
        existing.setPassword(updatedUser.getPassword());
        existing.setRole(updatedUser.getRole());

        User savedUser = userRepository.save(existing);

        log.info("User updated successfully - ID: {}", savedUser.getId());
        return savedUser;
    }

    /**
     * Delete user by ID
     */
    public void deleteUser(Long id) {
        log.info("Service: Deleting user ID: {}", id);
        User existing = getUserById(id);

        userRepository.delete(existing);
        log.info("User deleted successfully - ID: {}", id);
    }

    /**
     * Fetch user by email
     */
    public User getUserByEmail(String email) {
        log.info("Service: Fetching user by email: {}", email);

        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("User not found with email: {}", email);
                    return new ResourceNotFoundException("User not found with email: " + email);
                });
    }

    /**
     * Authenticate user and generate JWT token
     */
    public String login(String email, String password){
        log.info("Service: Login attempt for email: {}", email);
        User user = userRepository.findByEmail(email).orElseThrow(() -> {
                    log.warn("Login failed - Email not found: {}", email);
                    return new ResourceNotFoundException("Invalid email or password");
        });

        // Validate password
        if(!user.getPassword().equals(password)){
            log.warn("Login failed - Invalid password for email: {}", email);
            throw new BadRequestException("Invalid email or password");
        }

        // Generate JWT token
        String token = jwt.generateToken(user.getId(), email, user.getRole());
        log.info("Login successful - User ID: {}, Role: {}", user.getId(), user.getRole());
        return token;
    }
}