package com.exelynt.booking.service;

import com.exelynt.booking.dto.PageResponse;
import com.exelynt.booking.dto.ReservationRequest;
import com.exelynt.booking.dto.ReservationResponse;
import com.exelynt.booking.dto.ReservationStatusUpdateRequest;
import com.exelynt.booking.entity.Reservation;
import com.exelynt.booking.entity.ReservationStatus;
import com.exelynt.booking.entity.Resource;
import com.exelynt.booking.entity.User;
import com.exelynt.booking.exception.BadRequestException;
import com.exelynt.booking.exception.ResourceConflictException;
import com.exelynt.booking.exception.ResourceNotFoundException;
import com.exelynt.booking.exception.UnauthorizedException;
import com.exelynt.booking.repository.ReservationRepository;
import com.exelynt.booking.repository.ResourceRepository;
import com.exelynt.booking.repository.UserRepository;
import com.exelynt.booking.security.UserPrincipal;
import com.exelynt.booking.security.ReservationAccessPolicy;
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

    private static final long ALLOWED_START_TIME_SKEW_MINUTES = 5;
    private static final double MINUTES_PER_HOUR = 60.0;
    private static final double MINIMUM_HOURS = 1.0;

    private final ReservationRepository reservationRepository;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;
    private final ReservationAccessPolicy accessPolicy;

    @Autowired
    public ReservationService(ReservationRepository reservationRepository,
                              ResourceRepository resourceRepository,
                              UserRepository userRepository,
                              ReservationAccessPolicy accessPolicy) {
        this.reservationRepository = reservationRepository;
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
        this.accessPolicy = accessPolicy;
    }

    @Transactional
    public ReservationResponse createReservation(ReservationRequest request, UserPrincipal currentUser) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new UnauthorizedException("Authenticated user not found"));

        // Lock the resource before checking availability and overlap. This keeps competing
        // booking requests for the same resource in a single transaction at a time.
        Resource resource = resourceRepository.findByIdForUpdate(request.getResourceId())
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

        User userFilter = accessPolicy.reservationOwnerFilter(currentUser);
        Page<Reservation> reservationPage = reservationRepository.findFilteredReservations(
                userFilter, status, minPrice, maxPrice, pageable);

        return PageResponse.fromPage(reservationPage.map(ReservationResponse::fromEntity));
    }

    @Transactional(readOnly = true)
    public ReservationResponse getReservationById(Long id, UserPrincipal currentUser) {
        Reservation reservation = findReservationOrThrow(id);
        accessPolicy.requireOwnerOrAdmin(reservation, currentUser, "view");
        return ReservationResponse.fromEntity(reservation);
    }

    @Transactional
    public ReservationResponse updateReservationStatus(Long id, ReservationStatusUpdateRequest request, UserPrincipal currentUser) {
        Reservation reservation = findReservationOrThrow(id);

        if (!accessPolicy.isAdmin(currentUser)) {
            accessPolicy.requireOwnerOrAdmin(reservation, currentUser, "update");
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
        accessPolicy.requireOwnerOrAdmin(reservation, currentUser, "delete");
        reservationRepository.delete(reservation);
    }

    private void validateTimeWindow(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime.isAfter(endTime) || startTime.isEqual(endTime)) {
            throw new BadRequestException("Start time must be strictly before end time");
        }
        if (startTime.isBefore(LocalDateTime.now().minusMinutes(ALLOWED_START_TIME_SKEW_MINUTES))) {
            throw new BadRequestException("Reservation start time cannot be in the past");
        }
    }

    private void checkScheduleConflicts(Long resourceId, LocalDateTime startTime, LocalDateTime endTime) {
        List<Reservation> conflicts = reservationRepository
                .findByResource_IdAndStatusNotAndStartTimeLessThanAndEndTimeGreaterThan(
                        resourceId, ReservationStatus.CANCELLED, endTime, startTime);

        if (!conflicts.isEmpty()) {
            throw new ResourceConflictException("Resource is already booked during the requested time interval");
        }
    }

    private BigDecimal calculateReservationPrice(BigDecimal pricePerUnit, LocalDateTime start, LocalDateTime end) {
        long minutes = Duration.between(start, end).toMinutes();
        double hours = Math.max(MINIMUM_HOURS, minutes / MINUTES_PER_HOUR);
        return pricePerUnit.multiply(BigDecimal.valueOf(hours)).setScale(2, RoundingMode.HALF_UP);
    }

    private Reservation findReservationOrThrow(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation with ID " + id + " not found"));
    }
}
