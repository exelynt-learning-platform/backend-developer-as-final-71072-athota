package com.exelynt.booking.controller;

import com.exelynt.booking.dto.*;
import com.exelynt.booking.entity.ReservationStatus;
import com.exelynt.booking.security.UserPrincipal;
import com.exelynt.booking.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/reservations")
@Tag(name = "Reservations", description = "Endpoints for creating and managing bookings (JWT user context enforced)")
@SecurityRequirement(name = "BearerAuth")
public class ReservationController {

    private final ReservationService reservationService;

    @Autowired
    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    @Operation(summary = "Create a new reservation (User ID strictly extracted from JWT token)")
    public ResponseEntity<ReservationResponse> createReservation(
            @Valid @RequestBody ReservationRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        ReservationResponse response = reservationService.createReservation(request, currentUser);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get reservations with multi-criteria filtering, pagination, and sorting (ADMIN sees all, USER sees only own)")
    public ResponseEntity<PageResponse<ReservationResponse>> getReservations(
            @Parameter(description = "Filter by status: PENDING, CONFIRMED, CANCELLED")
            @RequestParam(required = false) ReservationStatus status,
            @Parameter(description = "Minimum reservation price filter")
            @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Maximum reservation price filter")
            @RequestParam(required = false) BigDecimal maxPrice,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        PageResponse<ReservationResponse> response = reservationService.getReservations(status, minPrice, maxPrice, pageable, currentUser);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get reservation by ID (ADMIN or reservation owner)")
    public ResponseEntity<ReservationResponse> getReservationById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        ReservationResponse response = reservationService.getReservationById(id, currentUser);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update reservation status (ADMIN can set any status; USER can set to CANCELLED)")
    public ResponseEntity<ReservationResponse> updateReservationStatus(
            @PathVariable Long id,
            @Valid @RequestBody ReservationStatusUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        ReservationResponse response = reservationService.updateReservationStatus(id, request, currentUser);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete reservation (ADMIN or reservation owner)")
    public ResponseEntity<Void> deleteReservation(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        reservationService.deleteReservation(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
