package com.exelynt.booking.service;

import com.exelynt.booking.dto.*;
import com.exelynt.booking.entity.*;
import com.exelynt.booking.exception.*;
import com.exelynt.booking.repository.ReservationRepository;
import com.exelynt.booking.repository.ResourceRepository;
import com.exelynt.booking.repository.UserRepository;
import com.exelynt.booking.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;

    @Autowired
    public ReservationService(ReservationRepository reservationRepository,
                              ResourceRepository resourceRepository,
                              UserRepository userRepository) {
        this.reservationRepository = reservationRepository;
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ReservationResponse createReservation(ReservationRequest request, UserPrincipal currentUser) {
        // Enforce USER identity is strictly derived from JWT security context (currentUser)
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new UnauthorizedException("Authenticated user not found"));

        Resource resource = resourceRepository.findById(request.getResourceId())
                .orElseThrow(() -> new ResourceNotFoundException("Resource with ID " + request.getResourceId() + " not found"));

        if (Boolean.FALSE.equals(resource.getAvailable())) {
            throw new BadRequestException("Resource '" + resource.getName() + "' is currently unavailable for booking");
        }

        // Validate time window
        if (request.getStartTime().isAfter(request.getEndTime()) || request.getStartTime().isEqual(request.getEndTime())) {
            throw new BadRequestException("Start time must be strictly before end time");
        }

        if (request.getStartTime().isBefore(LocalDateTime.now().minusMinutes(5))) {
            throw new BadRequestException("Reservation start time cannot be in the past");
        }

        // Check for conflicting reservations on the same resource
        List<Reservation> conflicts = reservationRepository.findConflictingReservations(
                resource.getId(),
                request.getStartTime(),
                request.getEndTime()
        );

        if (!conflicts.isEmpty()) {
            throw new ResourceConflictException("Resource is already booked during the requested time interval");
        }

        // Compute price if not explicitly provided
        BigDecimal calculatedPrice = request.getPrice();
        if (calculatedPrice == null || calculatedPrice.compareTo(BigDecimal.ZERO) <= 0) {
            long minutes = Duration.between(request.getStartTime(), request.getEndTime()).toMinutes();
            double hours = Math.max(1.0, (double) minutes / 60.0);
            calculatedPrice = resource.getPricePerUnit()
                    .multiply(BigDecimal.valueOf(hours))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        Reservation reservation = new Reservation(
                user,
                resource,
                request.getStartTime(),
                request.getEndTime(),
                calculatedPrice,
                ReservationStatus.PENDING,
                request.getNotes()
        );

        Reservation saved = reservationRepository.save(reservation);
        return ReservationResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReservationResponse> getReservations(
            ReservationStatus status,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable,
            UserPrincipal currentUser) {

        User userFilter = null;

        // If regular user, filter only their own reservations
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(Role.ROLE_ADMIN.name()));

        if (!isAdmin) {
            userFilter = userRepository.findById(currentUser.getId())
                    .orElseThrow(() -> new UnauthorizedException("Authenticated user not found"));
        }

        Page<Reservation> reservationPage = reservationRepository.findFilteredReservations(
                userFilter,
                status,
                minPrice,
                maxPrice,
                pageable
        );

        return PageResponse.fromPage(reservationPage.map(ReservationResponse::fromEntity));
    }

    @Transactional(readOnly = true)
    public ReservationResponse getReservationById(Long id, UserPrincipal currentUser) {
        Reservation reservation = findReservationOrThrow(id);

        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(Role.ROLE_ADMIN.name()));

        if (!isAdmin && !reservation.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("Access denied: you can only view your own reservations");
        }

        return ReservationResponse.fromEntity(reservation);
    }

    @Transactional
    public ReservationResponse updateReservationStatus(Long id, ReservationStatusUpdateRequest request, UserPrincipal currentUser) {
        Reservation reservation = findReservationOrThrow(id);

        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(Role.ROLE_ADMIN.name()));

        // Users can only cancel their own reservations; Admins can set any status
        if (!isAdmin) {
            if (!reservation.getUser().getId().equals(currentUser.getId())) {
                throw new UnauthorizedException("Access denied: you can only update your own reservations");
            }
            if (request.getStatus() != ReservationStatus.CANCELLED) {
                throw new BadRequestException("Regular users can only set status to CANCELLED");
            }
        }

        reservation.setStatus(request.getStatus());
        Reservation updated = reservationRepository.save(reservation);
        return ReservationResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteReservation(Long id, UserPrincipal currentUser) {
        Reservation reservation = findReservationOrThrow(id);

        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(Role.ROLE_ADMIN.name()));

        if (!isAdmin && !reservation.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("Access denied: you can only delete your own reservations");
        }

        reservationRepository.delete(reservation);
    }

    private Reservation findReservationOrThrow(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation with ID " + id + " not found"));
    }
}
