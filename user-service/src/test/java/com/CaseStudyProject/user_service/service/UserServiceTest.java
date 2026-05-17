package com.CaseStudyProject.user_service.service;

import com.CaseStudyProject.user_service.entity.User;
import com.CaseStudyProject.user_service.exception.BadRequestException;
import com.CaseStudyProject.user_service.exception.ResourceNotFoundException;
import com.CaseStudyProject.user_service.repository.UserRepository;
import com.CaseStudyProject.user_service.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtil jwt;

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setName("John Doe");
        user.setEmail("john@example.com");
        user.setPassword("password123");
        user.setRole("USER");
    }

    // ===========================
    // CREATE USER TESTS
    // ===========================

    @Test
    void createUser_Success() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.empty());
        when(userRepository.save(user)).thenReturn(user);

        User result = userService.createUser(user);

        assertNotNull(result);
        assertEquals("John Doe", result.getName());
        assertEquals("john@example.com", result.getEmail());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void createUser_EmailAlreadyExists_ThrowsBadRequest() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        assertThrows(BadRequestException.class, () -> userService.createUser(user));
        verify(userRepository, never()).save(any());
    }

    // ===========================
    // GET USER BY ID TESTS
    // ===========================

    @Test
    void getUserById_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        User result = userService.getUserById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("John Doe", result.getName());
    }

    @Test
    void getUserById_NotFound_ThrowsResourceNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUserById(99L));
    }

    // ===========================
    // GET ALL USERS TESTS
    // ===========================

    @Test
    void getAllUsers_Success() {
        when(userRepository.findAll()).thenReturn(List.of(user));

        List<User> result = userService.getAllUsers();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // ===========================
    // UPDATE USER TESTS
    // ===========================

    @Test
    void updateUser_AsAdmin_Success() {
        User updatedUser = new User();
        updatedUser.setName("Updated Name");
        updatedUser.setEmail("updated@example.com");
        updatedUser.setPassword("newpassword");
        updatedUser.setRole("ADMIN");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("updated@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        User result = userService.updateUser(1L, updatedUser, "ADMIN");

        assertNotNull(result);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void updateUser_AsUser_CannotChangeRole() {
        User updatedUser = new User();
        updatedUser.setName("Updated Name");
        updatedUser.setEmail("john@example.com"); // same email - no findByEmail needed
        updatedUser.setPassword("newpassword");
        updatedUser.setRole("ADMIN"); // trying to change role

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        // REMOVE: when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(user);

        userService.updateUser(1L, updatedUser, "USER");

        // Role should not change
        verify(userRepository, times(1)).save(argThat(u -> u.getRole().equals("USER")));
    }

    @Test
    void updateUser_EmailAlreadyExists_ThrowsBadRequest() {
        User updatedUser = new User();
        updatedUser.setName("Updated");
        updatedUser.setEmail("existing@example.com");
        updatedUser.setPassword("pass");
        updatedUser.setRole("USER");

        User existingUser = new User();
        existingUser.setEmail("existing@example.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(existingUser));

        assertThrows(BadRequestException.class, () -> userService.updateUser(1L, updatedUser, "ADMIN"));
    }

    // ===========================
    // DELETE USER TESTS
    // ===========================

    @Test
    void deleteUser_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        doNothing().when(userRepository).delete(user);

        userService.deleteUser(1L);

        verify(userRepository, times(1)).delete(user);
    }

    @Test
    void deleteUser_NotFound_ThrowsResourceNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.deleteUser(99L));
    }

    // ===========================
    // LOGIN TESTS
    // ===========================

    @Test
    void login_Success() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(jwt.generateToken(1L, "john@example.com", "USER")).thenReturn("mock-token");

        String token = userService.login("john@example.com", "password123");

        assertNotNull(token);
        assertEquals("mock-token", token);
    }

    @Test
    void login_InvalidEmail_ThrowsResourceNotFoundException() {
        when(userRepository.findByEmail("wrong@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.login("wrong@example.com", "password123"));
    }

    @Test
    void login_InvalidPassword_ThrowsBadRequestException() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));

        assertThrows(BadRequestException.class,
                () -> userService.login("john@example.com", "wrongpassword"));
    }

    // ===========================
    // GET USER BY EMAIL TESTS
    // ===========================

    @Test
    void getUserByEmail_Success() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));

        User result = userService.getUserByEmail("john@example.com");

        assertNotNull(result);
        assertEquals("john@example.com", result.getEmail());
    }

    @Test
    void getUserByEmail_NotFound_ThrowsResourceNotFoundException() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.getUserByEmail("unknown@example.com"));
    }
}