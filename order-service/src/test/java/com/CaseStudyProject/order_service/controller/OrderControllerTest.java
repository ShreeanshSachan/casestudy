package com.CaseStudyProject.order_service.controller;

import com.CaseStudyProject.order_service.dto.OrderRequest;
import com.CaseStudyProject.order_service.dto.OrderResponse;
import com.CaseStudyProject.order_service.dto.OrderStatusUpdate;
import com.CaseStudyProject.order_service.exception.GlobalExceptionHandler;
import com.CaseStudyProject.order_service.exception.ResourceNotFoundException;
import com.CaseStudyProject.order_service.service.OrderService;
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
public class OrderControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OrderService service;

    @InjectMocks
    private OrderController orderController;

    private ObjectMapper objectMapper = new ObjectMapper();

    private OrderResponse orderResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(orderController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        orderResponse = new OrderResponse();
        orderResponse.setOrderId(1L);
        orderResponse.setUserId(1L);
        orderResponse.setUserName("John Doe");
        orderResponse.setProductId(1L);
        orderResponse.setProductName("Nike Air Max");
        orderResponse.setProductPrice(120.50);
        orderResponse.setQuantity(2);
        orderResponse.setTotalPrice(241.0);
        orderResponse.setStatus("CREATED");
    }

    // ===========================
    // CREATE ORDER TESTS
    // ===========================

    @Test
    void createOrder_Success() throws Exception {
        OrderRequest request = new OrderRequest();
        request.setProductId(1L);
        request.setQuantity(2);

        when(service.createOrder(any(OrderRequest.class), eq(1L)))
                .thenReturn(orderResponse);

        mockMvc.perform(post("/orders")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(1))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.totalPrice").value(241.0));
    }

    // ===========================
    // GET ORDER BY ID TESTS
    // ===========================

    @Test
    void getOrderById_AsAdmin_Success() throws Exception {
        when(service.getOrderById(1L)).thenReturn(orderResponse);

        mockMvc.perform(get("/orders/1")
                        .header("X-User-Id", 1L)
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(1))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    void getOrderById_AsUser_OwnOrder_Success() throws Exception {
        when(service.getOrderById(1L)).thenReturn(orderResponse);

        mockMvc.perform(get("/orders/1")
                        .header("X-User-Id", 1L)
                        .header("X-User-Role", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(1));
    }

    @Test
    void getOrderById_AsUser_OtherOrder_ReturnsError() throws Exception {
        when(service.getOrderById(1L)).thenReturn(orderResponse);

        // userId 2 trying to access order owned by userId 1
        mockMvc.perform(get("/orders/1")
                        .header("X-User-Id", 2L)
                        .header("X-User-Role", "USER"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void getOrderById_NotFound_Returns404() throws Exception {
        when(service.getOrderById(99L))
                .thenThrow(new ResourceNotFoundException("Order not found with id: 99"));

        mockMvc.perform(get("/orders/99")
                        .header("X-User-Id", 1L)
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Order not found with id: 99"));
    }

    // ===========================
    // GET ALL ORDERS TESTS
    // ===========================

    @Test
    void getAllOrders_AsAdmin_Success() throws Exception {
        when(service.getAllOrders()).thenReturn(List.of(orderResponse));

        mockMvc.perform(get("/orders")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderId").value(1))
                .andExpect(jsonPath("$[0].status").value("CREATED"));
    }

    @Test
    void getAllOrders_AsUser_ReturnsError() throws Exception {
        mockMvc.perform(get("/orders")
                        .header("X-User-Role", "USER"))
                .andExpect(status().isInternalServerError());
    }

    // ===========================
    // GET MY ORDERS TESTS
    // ===========================

    @Test
    void getMyOrders_Success() throws Exception {
        when(service.getOrdersByUserId(1L)).thenReturn(List.of(orderResponse));

        mockMvc.perform(get("/orders/my")
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderId").value(1))
                .andExpect(jsonPath("$[0].userId").value(1));
    }

    @Test
    void getMyOrders_EmptyList() throws Exception {
        when(service.getOrdersByUserId(1L)).thenReturn(List.of());

        mockMvc.perform(get("/orders/my")
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ===========================
    // UPDATE ORDER STATUS TESTS
    // ===========================

    @Test
    void updateOrderStatus_AsAdmin_Success() throws Exception {
        OrderStatusUpdate request = new OrderStatusUpdate();
        request.setStatus("CONFIRMED");

        orderResponse.setStatus("CONFIRMED");
        when(service.updateOrderStatus(eq(1L), any(OrderStatusUpdate.class)))
                .thenReturn(orderResponse);

        mockMvc.perform(put("/orders/1/status")
                        .header("X-User-Id", 1L)
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void updateOrderStatus_AsUser_ReturnsError() throws Exception {
        OrderStatusUpdate request = new OrderStatusUpdate();
        request.setStatus("CONFIRMED");

        mockMvc.perform(put("/orders/1/status")
                        .header("X-User-Id", 1L)
                        .header("X-User-Role", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void updateOrderStatus_NotFound_Returns404() throws Exception {
        OrderStatusUpdate request = new OrderStatusUpdate();
        request.setStatus("CONFIRMED");

        when(service.updateOrderStatus(eq(99L), any(OrderStatusUpdate.class)))
                .thenThrow(new ResourceNotFoundException("Order not found with id: 99"));

        mockMvc.perform(put("/orders/99/status")
                        .header("X-User-Id", 1L)
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Order not found with id: 99"));
    }
}
