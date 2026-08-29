package com.exelynt.booking.security;

import com.exelynt.booking.entity.Reservation;
import com.exelynt.booking.entity.Role;
import com.exelynt.booking.entity.User;
import com.exelynt.booking.exception.ForbiddenException;
import com.exelynt.booking.exception.UnauthorizedException;
import com.exelynt.booking.repository.UserRepository;
import org.springframework.stereotype.Component;

/** Keeps reservation authorization rules separate from booking operations. */
@Component
public class ReservationAccessPolicy {

    private final UserRepository userRepository;

    public ReservationAccessPolicy(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User reservationOwnerFilter(UserPrincipal currentUser) {
        if (isAdmin(currentUser)) {
            return null;
        }
        return userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new UnauthorizedException("Authenticated user not found"));
    }

    public void requireOwnerOrAdmin(Reservation reservation, UserPrincipal currentUser, String action) {
        if (!isAdmin(currentUser) && !reservation.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Access denied: you can only " + action + " your own reservations");
        }
    }

    public boolean isAdmin(UserPrincipal currentUser) {
        return currentUser.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals(Role.ROLE_ADMIN.name()));
    }
}
