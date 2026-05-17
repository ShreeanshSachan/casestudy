package com.CaseStudyProject.order_service.service;

import com.CaseStudyProject.order_service.client.ProductClient;
import com.CaseStudyProject.order_service.client.UserClient;
import com.CaseStudyProject.order_service.dto.*;
import com.CaseStudyProject.order_service.entity.Order;
import com.CaseStudyProject.order_service.exception.BadRequestException;
import com.CaseStudyProject.order_service.exception.ResourceNotFoundException;
import com.CaseStudyProject.order_service.repository.OrderRepository;
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
public class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductClient productClient;

    @Mock
    private UserClient userClient;

    @InjectMocks
    private OrderService orderService;

    private Order order;
    private ProductDTO productDTO;
    private UserDTO userDTO;
    private OrderRequest orderRequest;

    @BeforeEach
    void setUp() {
        order = new Order();
        order.setId(1L);
        order.setUserId(1L);
        order.setProductId(1L);
        order.setQuantity(2);
        order.setTotalPrice(241.0);
        order.setStatus("CREATED");

        productDTO = new ProductDTO();
        productDTO.setId(1L);
        productDTO.setName("Nike Air Max");
        productDTO.setPrice(120.50);

        userDTO = new UserDTO();
        userDTO.setId(1L);
        userDTO.setName("John Doe");
        userDTO.setEmail("john@example.com");
        userDTO.setRole("USER");

        orderRequest = new OrderRequest();
        orderRequest.setProductId(1L);
        orderRequest.setQuantity(2);
    }

    // ===========================
    // CREATE ORDER TESTS
    // ===========================

    @Test
    void createOrder_Success() {
        when(productClient.getProductById(1L)).thenReturn(productDTO);
        when(userClient.getUserById(1L)).thenReturn(userDTO);
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        OrderResponse result = orderService.createOrder(orderRequest, 1L);

        assertNotNull(result);
        assertEquals(1L, result.getOrderId());
        assertEquals("CREATED", result.getStatus());
        assertEquals(241.0, result.getTotalPrice());
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void createOrder_InvalidQuantity_ThrowsBadRequest() {
        orderRequest.setQuantity(0);

        assertThrows(BadRequestException.class,
                () -> orderService.createOrder(orderRequest, 1L));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_NullQuantity_ThrowsBadRequest() {
        orderRequest.setQuantity(null);

        assertThrows(BadRequestException.class,
                () -> orderService.createOrder(orderRequest, 1L));
        verify(orderRepository, never()).save(any());
    }

    // ===========================
    // GET ORDER BY ID TESTS
    // ===========================

    @Test
    void getOrderById_Success() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(productClient.getProductById(1L)).thenReturn(productDTO);
        when(userClient.getUserById(1L)).thenReturn(userDTO);

        OrderResponse result = orderService.getOrderById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getOrderId());
        assertEquals("CREATED", result.getStatus());
    }

    @Test
    void getOrderById_NotFound_ThrowsResourceNotFoundException() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> orderService.getOrderById(99L));
    }

    // ===========================
    // GET ALL ORDERS TESTS
    // ===========================

    @Test
    void getAllOrders_Success() {
        when(orderRepository.findAll()).thenReturn(List.of(order));
        when(productClient.getProductById(1L)).thenReturn(productDTO);
        when(userClient.getUserById(1L)).thenReturn(userDTO);

        List<OrderResponse> result = orderService.getAllOrders();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getOrderId());
    }

    @Test
    void getAllOrders_EmptyList() {
        when(orderRepository.findAll()).thenReturn(List.of());

        List<OrderResponse> result = orderService.getAllOrders();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ===========================
    // GET ORDERS BY USER ID TESTS
    // ===========================

    @Test
    void getOrdersByUserId_Success() {
        when(userClient.getUserById(1L)).thenReturn(userDTO);
        when(orderRepository.findByUserId(1L)).thenReturn(List.of(order));
        when(productClient.getProductById(1L)).thenReturn(productDTO);

        List<OrderResponse> result = orderService.getOrdersByUserId(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getOrdersByUserId_NoOrders_ReturnsEmptyList() {
        when(userClient.getUserById(1L)).thenReturn(userDTO);
        when(orderRepository.findByUserId(1L)).thenReturn(List.of());

        List<OrderResponse> result = orderService.getOrdersByUserId(1L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ===========================
    // UPDATE ORDER STATUS TESTS
    // ===========================

    @Test
    void updateOrderStatus_AsUser_Success() {
        OrderStatusUpdate request = new OrderStatusUpdate();
        request.setStatus("CONFIRMED");

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(productClient.getProductById(1L)).thenReturn(productDTO);
        when(userClient.getUserById(1L)).thenReturn(userDTO);

        // Now takes only id and request (no userId)
        OrderResponse result = orderService.updateOrderStatus(1L, request);

        assertNotNull(result);
        verify(orderRepository, times(1)).save(any(Order.class));
    }



    @Test
    void updateOrderStatus_InvalidStatus_ThrowsBadRequest() {
        OrderStatusUpdate request = new OrderStatusUpdate();
        request.setStatus("INVALID_STATUS");

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(BadRequestException.class,
                () -> orderService.updateOrderStatus(1L, request));  // Remove userId
    }

    @Test
    void updateOrderStatus_EmptyStatus_ThrowsBadRequest() {
        OrderStatusUpdate request = new OrderStatusUpdate();
        request.setStatus("");

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(BadRequestException.class,
                () -> orderService.updateOrderStatus(1L, request));  // Remove userId
    }

    @Test
    void updateOrderStatus_OrderNotFound_ThrowsResourceNotFoundException() {
        OrderStatusUpdate request = new OrderStatusUpdate();
        request.setStatus("CONFIRMED");

        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> orderService.updateOrderStatus(99L, request));  // Remove userId
    }
}