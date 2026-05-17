package com.CaseStudyProject.user_service.controller;

import com.CaseStudyProject.user_service.dto.LoginRequest;
import com.CaseStudyProject.user_service.dto.LoginResponse;
import com.CaseStudyProject.user_service.entity.User;
import com.CaseStudyProject.user_service.exception.BadRequestException;
import com.CaseStudyProject.user_service.exception.GlobalExceptionHandler;
import com.CaseStudyProject.user_service.exception.ResourceNotFoundException;
import com.CaseStudyProject.user_service.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class UserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private ObjectMapper objectMapper = new ObjectMapper();

    private User user;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

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
    void createUser_Success() throws Exception {
        when(userService.createUser(any(User.class))).thenReturn(user);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"));
    }

    @Test
    void createUser_EmailExists_ReturnsBadRequest() throws Exception {
        when(userService.createUser(any(User.class)))
                .thenThrow(new BadRequestException("Email already exists"));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email already exists"));
    }

    // ===========================
    // GET ALL USERS TESTS
    // ===========================

    @Test
    void getAllUsers_AsAdmin_Success() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of(user));

        mockMvc.perform(get("/users")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("John Doe"));
    }

    @Test
    void getAllUsers_AsUser_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/users")
                        .header("X-User-Role", "USER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only ADMIN can view all users"));
    }

    // ===========================
    // GET USER BY ID TESTS
    // ===========================

    @Test
    void getUserById_Success() throws Exception {
        when(userService.getUserById(1L)).thenReturn(user);

        mockMvc.perform(get("/users/1")
                        .header("X-User-Id", 1L)        // ADD THIS
                        .header("X-User-Role", "ADMIN")) // ADD THIS
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("John Doe"));
    }

    @Test
    void getUserById_NotFound_Returns404() throws Exception {
        when(userService.getUserById(99L))
                .thenThrow(new ResourceNotFoundException("User not found with id: 99"));

        mockMvc.perform(get("/users/99")
                        .header("X-User-Id", 1L)        // ADD THIS
                        .header("X-User-Role", "ADMIN")) // ADD THIS
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found with id: 99"));
    }

    // ===========================
    // UPDATE USER TESTS
    // ===========================

    @Test
    void updateUser_AsAdmin_Success() throws Exception {
        when(userService.updateUser(eq(1L), any(User.class), eq("ADMIN"))).thenReturn(user);

        mockMvc.perform(put("/users/1")
                        .header("X-User-Id", 1L)
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("John Doe"));
    }

    @Test
    void updateUser_AsUser_UpdatingOtherUser_ReturnsBadRequest() throws Exception {
        mockMvc.perform(put("/users/2")
                        .header("X-User-Id", 1L)
                        .header("X-User-Role", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Access Denied"));
    }

    // ===========================
    // DELETE USER TESTS
    // ===========================

    @Test
    void deleteUser_AsAdmin_Success() throws Exception {
        doNothing().when(userService).deleteUser(1L);

        mockMvc.perform(delete("/users/1")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(content().string("User deleted successfully"));
    }

    @Test
    void deleteUser_AsUser_ReturnsBadRequest() throws Exception {
        mockMvc.perform(delete("/users/1")
                        .header("X-User-Role", "USER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only ADMINS can delete user"));
    }

    // ===========================
    // LOGIN TESTS
    // ===========================

    @Test
    void login_Success() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("john@example.com");
        loginRequest.setPassword("password123");

        when(userService.login("john@example.com", "password123"))
                .thenReturn("mock-jwt-token");

        mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock-jwt-token"));
    }

    @Test
    void login_InvalidCredentials_ReturnsBadRequest() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("john@example.com");
        loginRequest.setPassword("wrongpassword");

        when(userService.login(any(), any()))
                .thenThrow(new BadRequestException("Invalid email or password"));

        mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    // ===========================
    // GET USER BY EMAIL TESTS
    // ===========================

    @Test
    void getUserByEmail_AsAdmin_Success() throws Exception {
        when(userService.getUserByEmail("john@example.com")).thenReturn(user);

        mockMvc.perform(get("/users/email")
                        .param("email", "john@example.com")
                        .header("X-User-Id", 1L)
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("john@example.com"));
    }

    @Test
    void getUserByEmail_AsUser_AccessingOtherUser_ReturnsBadRequest() throws Exception {
        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setEmail("other@example.com");

        when(userService.getUserByEmail("other@example.com")).thenReturn(otherUser);

        mockMvc.perform(get("/users/email")
                        .param("email", "other@example.com")
                        .header("X-User-Id", 1L)
                        .header("X-User-Role", "USER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Access denied"));
    }
}