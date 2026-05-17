package com.CaseStudyProject.user_service.controller;

import com.CaseStudyProject.user_service.dto.LoginRequest;
import com.CaseStudyProject.user_service.dto.LoginResponse;
import com.CaseStudyProject.user_service.entity.User;
import com.CaseStudyProject.user_service.exception.BadRequestException;
import com.CaseStudyProject.user_service.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    // Service layer dependency (business logic handled here)
    private final UserService userService;

    /**
     * Create a new user
     */
    @PostMapping
    public User createUser(@RequestBody User user) {
        log.info("POST /users - Request received to create user with email: {}", user.getEmail());

        User createdUser = userService.createUser(user);

        log.info("User created successfully with ID: {}", createdUser.getId());
        return createdUser;
    }

    /**
     * Fetch all users (ADMIN only)
     */
    @GetMapping
    public List<User> getAllUsers(@RequestHeader("X-User-Role") String role) {
        log.info("GET /users - Fetch all users request received with role: {}", role);

        // Authorization check
        if (!role.equals("ADMIN")) {
            log.warn("Unauthorized access attempt to fetch all users with role: {}", role);
            throw new BadRequestException("Only ADMIN can view all users");
        }

        List<User> users = userService.getAllUsers();
        log.info("Successfully fetched {} users", users.size());
        return users;
    }

    /**
     * Fetch user by ID
     */
    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id, @RequestHeader("X-User-Id") Long userId, @RequestHeader("X-User-Role") String role) {
        log.info("GET /users/{} - Fetch request by userId: {} with role: {}", id, userId, role);

        // Authorization check
        if (role.equals("USER") && !userId.equals(id)) {
            log.warn("Access denied - User {} attempted to access user {}", userId, id);
            throw new BadRequestException("Access denied");
        }

        User user = userService.getUserById(id);
        log.info("User found - ID: {}, Name: {}", user.getId(), user.getName());
        return user;
    }
    /**
     * Update user details
     * USER can update only their own data
     * ADMIN can update any user
     */
    @PutMapping("/{id}")
    public User updateUser(@PathVariable Long id, @RequestBody User user, @RequestHeader("X-User-Id") Long userId, @RequestHeader("X-User-Role") String role) {
        log.info("PUT /users/{} - Update request by userId: {} with role: {}", id, userId, role);

        // Authorization check
        if (role.equals("USER") && !userId.equals(id)) {
            log.warn("Access denied - User {} attempted to update user {}", userId, id);
            throw new BadRequestException("Access Denied");
        }

        User updatedUser = userService.updateUser(id, user, role);
        log.info("User updated successfully - ID: {}", updatedUser.getId());
        return updatedUser;
    }

    /**
     * Delete user (ADMIN only)
     */
    @DeleteMapping("/{id}")
    public String deleteUser(@PathVariable Long id, @RequestHeader("X-User-Role") String role) {
        log.info("DELETE /users/{} - Delete request with role: {}", id, role);

        // Authorization check
        if (!role.equals("ADMIN")) {
            log.warn("Unauthorized delete attempt for user {} with role: {}", id, role);
            throw new BadRequestException("Only ADMINS can delete user");
        }

        userService.deleteUser(id);
        log.info("User deleted successfully - ID: {}", id);
        return "User deleted successfully";
    }

    /**
     * Fetch user by email
     * USER can only access their own data
     */
    @GetMapping("/email")
    public User getByEmail(@RequestParam String email, @RequestHeader("X-User-Id") Long userId, @RequestHeader("X-User-Role") String role) {
        log.info("GET /users/email - Fetch request for email: {} by userId: {} with role: {}", email, userId, role);

        User user = userService.getUserByEmail(email);

        if (role.equals("USER") && !user.getId().equals(userId)) {
            log.warn("Access denied - User {} attempted to access data of user {}", userId, user.getId());
            throw new BadRequestException("Access denied");
        }

        log.info("User fetched successfully - ID: {}, Email: {}", user.getId(), user.getEmail());
        return user;
    }

    /**
     * User login endpoint
     * Returns authentication token
     */
    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        log.info("POST /users/login - Login attempt for email: {}", request.getEmail());

        String token = userService.login(request.getEmail(), request.getPassword());

        log.info("Login successful for email: {}", request.getEmail());
        return new LoginResponse(token);
    }
}