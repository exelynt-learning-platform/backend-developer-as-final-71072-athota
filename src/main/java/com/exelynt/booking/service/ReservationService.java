package com.exelynt.booking.service;

import com.exelynt.booking.dto.PageResponse;
import com.exelynt.booking.dto.ReservationRequest;
import com.exelynt.booking.dto.ReservationResponse;
import com.exelynt.booking.dto.ReservationStatusUpdateRequest;
import com.exelynt.booking.entity.Reservation;
import com.exelynt.booking.entity.ReservationStatus;
import com.exelynt.booking.entity.Resource;
import com.exelynt.booking.entity.Role;
import com.exelynt.booking.entity.User;
import com.exelynt.booking.exception.BadRequestException;
import com.exelynt.booking.exception.ForbiddenException;
import com.exelynt.booking.exception.ResourceConflictException;
import com.exelynt.booking.exception.ResourceNotFoundException;
import com.exelynt.booking.exception.UnauthorizedException;
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
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new UnauthorizedException("Authenticated user not found"));

        Resource resource = resourceRepository.findById(request.getResourceId())
                .orElseThrow(() -> new ResourceNotFoundException("Resource with ID " + request.getResourceId() + " not found"));

        if (Boolean.FALSE.equals(resource.getAvailable())) {
            throw new BadRequestException("Resource '" + resource.getName() + "' is currently unavailable for booking");
        }

        validateTimeWindow(request.getStartTime(), request.getEndTime());
        checkScheduleConflicts(resource.getId(), request.getStartTime(), request.getEndTime());

        BigDecimal calculatedPrice = calculateReservationPrice(
                resource.getPricePerUnit(),
                request.getStartTime(),
                request.getEndTime());

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
        if (!isAdmin(currentUser)) {
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
        validateOwnershipOrAdmin(reservation, currentUser, "view");
        return ReservationResponse.fromEntity(reservation);
    }

    @Transactional
    public ReservationResponse updateReservationStatus(Long id, ReservationStatusUpdateRequest request, UserPrincipal currentUser) {
        Reservation reservation = findReservationOrThrow(id);

        if (!isAdmin(currentUser)) {
            validateOwnershipOrAdmin(reservation, currentUser, "update");
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
        validateOwnershipOrAdmin(reservation, currentUser, "delete");
        reservationRepository.delete(reservation);
    }

    private void validateTimeWindow(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime.isAfter(endTime) || startTime.isEqual(endTime)) {
            throw new BadRequestException("Start time must be strictly before end time");
        }
        if (startTime.isBefore(LocalDateTime.now().minusMinutes(5))) {
            throw new BadRequestException("Reservation start time cannot be in the past");
        }
    }

    private void checkScheduleConflicts(Long resourceId, LocalDateTime startTime, LocalDateTime endTime) {
        List<Reservation> conflicts = reservationRepository.findConflictingReservations(
                resourceId, startTime, endTime);

        if (!conflicts.isEmpty()) {
            throw new ResourceConflictException("Resource is already booked during the requested time interval");
        }
    }

    private BigDecimal calculateReservationPrice(BigDecimal pricePerUnit, LocalDateTime start, LocalDateTime end) {
        long minutes = Duration.between(start, end).toMinutes();
        double hours = Math.max(1.0, (double) minutes / 60.0);
        return pricePerUnit.multiply(BigDecimal.valueOf(hours)).setScale(2, RoundingMode.HALF_UP);
    }

    private boolean isAdmin(UserPrincipal user) {
        return user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(Role.ROLE_ADMIN.name()));
    }

    private void validateOwnershipOrAdmin(Reservation reservation, UserPrincipal currentUser, String action) {
        if (!isAdmin(currentUser) && !reservation.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Access denied: you can only " + action + " your own reservations");
        }
    }

    private Reservation findReservationOrThrow(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation with ID " + id + " not found"));
    }
}
