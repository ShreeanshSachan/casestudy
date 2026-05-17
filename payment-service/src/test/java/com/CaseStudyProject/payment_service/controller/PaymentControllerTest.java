package com.CaseStudyProject.payment_service.controller;

import com.CaseStudyProject.payment_service.dto.*;
import com.CaseStudyProject.payment_service.entity.PaymentStatus;
import com.CaseStudyProject.payment_service.exception.BadRequestException;
import com.CaseStudyProject.payment_service.exception.ResourceNotFoundException;
import com.CaseStudyProject.payment_service.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc(addFilters = false) // ⚠️ disables security filters (important)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @Autowired
    private ObjectMapper objectMapper;

    private final Long USER_ID = 101L;

    // =========================================
    // ✅ POST /payments/process
    // =========================================
    @Test
    void processPayment_success() throws Exception {

        PaymentRequest request = new PaymentRequest();
        request.setOrderId(1L);
        request.setAmount(500.0);
        request.setPaymentMethod("UPI");

        PaymentResponse response = PaymentResponse.builder()
                .id(1L)
                .orderId(1L)
                .userId(USER_ID)
                .amount(500.0)
                .status(PaymentStatus.SUCCESS)
                .build();

        Mockito.when(paymentService.processPayment(Mockito.any(), Mockito.eq(USER_ID)))
                .thenReturn(response);

        mockMvc.perform(post("/payments/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", USER_ID)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(1L))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    // =========================================
    // ❌ POST /payments/process (Service throws)
    // =========================================
    @Test
    void processPayment_failure() throws Exception {

        PaymentRequest request = new PaymentRequest();  // ✅ ADD THIS
        request.setOrderId(1L);
        request.setAmount(500.0);
        request.setPaymentMethod("UPI");

        Mockito.when(paymentService.processPayment(Mockito.any(), Mockito.eq(USER_ID)))
                .thenThrow(new BadRequestException("Payment failed"));

        mockMvc.perform(post("/payments/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", USER_ID)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // =========================================
    // ❌ POST /payments/process (Validation fail)
    // =========================================
    @Test
    void processPayment_validationFail() throws Exception {

        PaymentRequest request = new PaymentRequest();
        // Missing required fields

        mockMvc.perform(post("/payments/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", USER_ID)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // =========================================
    // ✅ GET /payments/{orderId}
    // =========================================
    @Test
    void getPayment_success() throws Exception {

        PaymentResponse response = PaymentResponse.builder()
                .orderId(1L)
                .userId(USER_ID)
                .status(PaymentStatus.SUCCESS)
                .build();

        Mockito.when(paymentService.getByOrderId(1L, USER_ID))
                .thenReturn(response);

        mockMvc.perform(get("/payments/1")
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    // =========================================
    // ❌ GET /payments/{orderId}
    // =========================================
    @Test
    void getPayment_notFound() throws Exception {

        Mockito.when(paymentService.getByOrderId(1L, USER_ID))
                .thenThrow(new ResourceNotFoundException("Not found"));

        mockMvc.perform(get("/payments/1")
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isNotFound());
    }

    // =========================================
    // ✅ GET /payments/user
    // =========================================
    @Test
    void getUserPayments_success() throws Exception {

        List<PaymentResponse> responses = List.of(
                PaymentResponse.builder().orderId(1L).status(PaymentStatus.SUCCESS).build(),
                PaymentResponse.builder().orderId(2L).status(PaymentStatus.FAILED).build()
        );

        Mockito.when(paymentService.getPaymentsByUser(USER_ID))
                .thenReturn(responses);

        mockMvc.perform(get("/payments/user")
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }
}