package com.CaseStudyProject.payment_service.service;

import com.CaseStudyProject.payment_service.client.OrderClient;
import com.CaseStudyProject.payment_service.dto.*;
import com.CaseStudyProject.payment_service.entity.Payment;
import com.CaseStudyProject.payment_service.entity.PaymentStatus;
import com.CaseStudyProject.payment_service.exception.BadRequestException;
import com.CaseStudyProject.payment_service.exception.ResourceNotFoundException;
import com.CaseStudyProject.payment_service.repository.PaymentRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository repo;

    @Mock
    private OrderClient orderClient;

    @InjectMocks
    private PaymentService paymentService;

    private PaymentRequest request;
    private OrderResponseDTO order;

    private final Long USER_ID = 101L;
    private final Long ORDER_ID = 1L;

    @BeforeEach
    void setup() {
        request = new PaymentRequest();
        request.setOrderId(ORDER_ID);
        request.setAmount(500.0);
        request.setPaymentMethod("UPI");

        order = new OrderResponseDTO();
        order.setOrderId(ORDER_ID);
        order.setUserId(USER_ID);
        order.setTotalPrice(500.0);
    }

    // =========================================
    // ✅ SUCCESS FLOW
    // =========================================
    @Test
    void processPayment_success() {

        when(orderClient.getOrderById(ORDER_ID, USER_ID, "USER"))
                .thenReturn(order);

        when(repo.findByOrderId(ORDER_ID))
                .thenReturn(Optional.empty());

        when(repo.save(any(Payment.class)))
                .thenAnswer(invocation -> {
                    Payment p = invocation.getArgument(0);
                    p.setId(1L);
                    return p;
                });

        // Force simulation to SUCCESS using reflection hack
        setSimulationMode("success");

        PaymentResponse response = paymentService.processPayment(request, USER_ID);

        assertNotNull(response);
        assertEquals(PaymentStatus.SUCCESS, response.getStatus());
        assertEquals(ORDER_ID, response.getOrderId());

        verify(orderClient).updateOrderStatus(eq(ORDER_ID), any(), eq(USER_ID), eq("ADMIN"));
    }

    // =========================================
    // ❌ ORDER NOT FOUND (Feign failure)
    // =========================================
    @Test
    void processPayment_orderNotFound() {

        when(orderClient.getOrderById(ORDER_ID, USER_ID, "USER"))
                .thenThrow(new RuntimeException("Feign error"));

        assertThrows(ResourceNotFoundException.class,
                () -> paymentService.processPayment(request, USER_ID));

        verify(repo, never()).save(any());
    }

    // =========================================
    // ❌ UNAUTHORIZED USER
    // =========================================
    @Test
    void processPayment_unauthorizedUser() {

        order.setUserId(999L); // mismatch

        when(orderClient.getOrderById(ORDER_ID, USER_ID, "USER"))
                .thenReturn(order);

        assertThrows(BadRequestException.class,
                () -> paymentService.processPayment(request, USER_ID));
    }

    // =========================================
    // ❌ DUPLICATE PAYMENT
    // =========================================
    @Test
    void processPayment_duplicatePayment() {

        when(orderClient.getOrderById(ORDER_ID, USER_ID, "USER"))
                .thenReturn(order);

        when(repo.findByOrderId(ORDER_ID))
                .thenReturn(Optional.of(new Payment()));

        assertThrows(BadRequestException.class,
                () -> paymentService.processPayment(request, USER_ID));
    }

    // =========================================
    // ❌ AMOUNT MISMATCH
    // =========================================
    @Test
    void processPayment_amountMismatch() {

        request.setAmount(999.0);

        when(orderClient.getOrderById(ORDER_ID, USER_ID, "USER"))
                .thenReturn(order);

        when(repo.findByOrderId(ORDER_ID))
                .thenReturn(Optional.empty());

        assertThrows(BadRequestException.class,
                () -> paymentService.processPayment(request, USER_ID));
    }

    // =========================================
    // ❌ ORDER STATUS UPDATE FAILURE (should NOT fail payment)
    // =========================================
    @Test
    void processPayment_orderUpdateFails_shouldStillReturnResponse() {

        when(orderClient.getOrderById(ORDER_ID, USER_ID, "USER"))
                .thenReturn(order);

        when(repo.findByOrderId(ORDER_ID))
                .thenReturn(Optional.empty());

        when(repo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        doThrow(new RuntimeException("Order service down"))
                .when(orderClient)
                .updateOrderStatus(anyLong(), any(), anyLong(), anyString());

        setSimulationMode("success");

        PaymentResponse response = paymentService.processPayment(request, USER_ID);

        assertNotNull(response);
        assertEquals(PaymentStatus.SUCCESS, response.getStatus());
    }

    // =========================================
    // ✅ GET PAYMENT BY ORDER ID
    // =========================================
    @Test
    void getByOrderId_success() {

        Payment payment = Payment.builder()
                .id(1L)
                .orderId(ORDER_ID)
                .userId(USER_ID)
                .amount(500.0)
                .status(PaymentStatus.SUCCESS)
                .build();

        when(repo.findByOrderId(ORDER_ID))
                .thenReturn(Optional.of(payment));

        PaymentResponse response = paymentService.getByOrderId(ORDER_ID, USER_ID);

        assertNotNull(response);
        assertEquals(PaymentStatus.SUCCESS, response.getStatus());
    }

    // =========================================
    // ❌ GET PAYMENT - NOT FOUND
    // =========================================
    @Test
    void getByOrderId_notFound() {

        when(repo.findByOrderId(ORDER_ID))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> paymentService.getByOrderId(ORDER_ID, USER_ID));
    }

    // =========================================
    // ❌ GET PAYMENT - UNAUTHORIZED
    // =========================================
    @Test
    void getByOrderId_unauthorized() {

        Payment payment = Payment.builder()
                .orderId(ORDER_ID)
                .userId(999L)
                .build();

        when(repo.findByOrderId(ORDER_ID))
                .thenReturn(Optional.of(payment));

        assertThrows(BadRequestException.class,
                () -> paymentService.getByOrderId(ORDER_ID, USER_ID));
    }

    // =========================================
    // ✅ GET PAYMENTS BY USER
    // =========================================
    @Test
    void getPaymentsByUser_success() {

        List<Payment> payments = List.of(
                Payment.builder().orderId(1L).userId(USER_ID).status(PaymentStatus.SUCCESS).build(),
                Payment.builder().orderId(2L).userId(USER_ID).status(PaymentStatus.FAILED).build()
        );

        when(repo.findByUserId(USER_ID)).thenReturn(payments);

        List<PaymentResponse> result = paymentService.getPaymentsByUser(USER_ID);

        assertEquals(2, result.size());
    }

    // =========================================
    // 🔧 Helper: Force simulation mode
    // =========================================
    private void setSimulationMode(String mode) {
        try {
            java.lang.reflect.Field field = PaymentService.class.getDeclaredField("simMode");
            field.setAccessible(true);
            field.set(paymentService, mode);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}