package com.exelynt.booking.controller;

import com.exelynt.booking.dto.LoginRequest;
import com.exelynt.booking.dto.ReservationRequest;
import com.exelynt.booking.dto.ReservationStatusUpdateRequest;
import com.exelynt.booking.entity.ReservationStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String userToken;
    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        MvcResult userResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("user", "User@123"))))
                .andReturn();
        this.userToken = objectMapper.readTree(userResult.getResponse().getContentAsString()).get("token").asText();

        MvcResult adminResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("admin", "Admin@123"))))
                .andReturn();
        this.adminToken = objectMapper.readTree(adminResult.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    @DisplayName("POST /reservations - Creates reservation taking user identity strictly from JWT")
    void testCreateReservation() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(10).withHour(10).withMinute(0);
        LocalDateTime end = LocalDateTime.now().plusDays(10).withHour(12).withMinute(0);

        ReservationRequest request = new ReservationRequest(
                1L,
                start,
                end,
                "Product strategy discussion"
        );

        mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.username").value("user"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.price").value(300.00));
    }

    @Test
    @DisplayName("GET /reservations - Multi-criteria filtering by status, minPrice, maxPrice, and pagination")
    void testGetReservationsWithFilterAndPagination() throws Exception {
        mockMvc.perform(get("/reservations")
                        .param("status", "CONFIRMED")
                        .param("minPrice", "100.00")
                        .param("maxPrice", "500.00")
                        .param("page", "0")
                        .param("size", "5")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(5));
    }

    @Test
    @DisplayName("GET /reservations - Caps oversized page requests")
    void getReservations_whenPageSizeExceedsLimit_capsAtConfiguredMaximum() throws Exception {
        mockMvc.perform(get("/reservations")
                        .param("size", "1000")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageSize").value(100));
    }

    @Test
    @DisplayName("PUT /reservations/{id}/status - USER can cancel their own reservation")
    void testUserCanCancelReservation() throws Exception {
        ReservationStatusUpdateRequest cancelReq = new ReservationStatusUpdateRequest(ReservationStatus.CANCELLED);

        mockMvc.perform(put("/reservations/1/status")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
