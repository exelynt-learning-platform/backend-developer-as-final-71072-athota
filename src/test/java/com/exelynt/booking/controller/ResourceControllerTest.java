package com.exelynt.booking.controller;

import com.exelynt.booking.dto.LoginRequest;
import com.exelynt.booking.dto.ResourceRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class ResourceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        // Authenticate Admin
        MvcResult adminResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("admin", "Admin@123"))))
                .andReturn();
        JsonNode adminJson = objectMapper.readTree(adminResult.getResponse().getContentAsString());
        this.adminToken = adminJson.get("token").asText();

        // Authenticate User
        MvcResult userResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("user", "User@123"))))
                .andReturn();
        JsonNode userJson = objectMapper.readTree(userResult.getResponse().getContentAsString());
        this.userToken = userJson.get("token").asText();
    }

    @Test
    @DisplayName("GET /resources - Accessible by authenticated USER")
    void testGetResourcesAsUser() throws Exception {
        mockMvc.perform(get("/resources")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    @Test
    @DisplayName("POST /resources - ADMIN can create resource (201 Created)")
    void testCreateResourceAsAdmin() throws Exception {
        ResourceRequest request = new ResourceRequest(
                "Podcast Studio Pro",
                "Acoustically isolated podcast and voice recording studio with Shure microphones.",
                "ROOM",
                new BigDecimal("95.00"),
                true
        );

        mockMvc.perform(post("/resources")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Podcast Studio Pro"))
                .andExpect(jsonPath("$.pricePerUnit").value(95.00));
    }

    @Test
    @DisplayName("POST /resources - Regular USER cannot create resource (403 Forbidden)")
    void testCreateResourceAsUserForbidden() throws Exception {
        ResourceRequest request = new ResourceRequest(
                "Unauthorized Room",
                "Description",
                "ROOM",
                new BigDecimal("50.00"),
                true
        );

        mockMvc.perform(post("/resources")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Access denied"))
                .andExpect(jsonPath("$.path").value("/resources"));
    }

    @Test
    @DisplayName("DELETE /resources/{id} - Resource with reservations returns 409 Conflict")
    void deleteResourceWithReservations_returnsConflict() throws Exception {
        MvcResult reservationsResult = mockMvc.perform(get("/reservations")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        long resourceId = objectMapper.readTree(reservationsResult.getResponse().getContentAsString())
                .path("content").get(0).path("resourceId").asLong();

        mockMvc.perform(delete("/resources/{id}", resourceId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Cannot delete resource with existing reservations"));
    }

    @Test
    @DisplayName("CORS only permits configured frontend origins")
    void corsConfiguration_allowsOnlyConfiguredOrigins() throws Exception {
        mockMvc.perform(options("/resources")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));

        mockMvc.perform(options("/resources")
                        .header("Origin", "https://untrusted.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }
}
