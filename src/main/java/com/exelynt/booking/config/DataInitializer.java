package com.exelynt.booking.config;

import com.exelynt.booking.entity.*;
import com.exelynt.booking.repository.ReservationRepository;
import com.exelynt.booking.repository.ResourceRepository;
import com.exelynt.booking.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final ResourceRepository resourceRepository;
    private final ReservationRepository reservationRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           ResourceRepository resourceRepository,
                           ReservationRepository reservationRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.resourceRepository = resourceRepository;
        this.reservationRepository = reservationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        logger.info("Initializing Seed Data for Resource Booking System...");

        // 1. Seed Users
        User admin = userRepository.findByUsername("admin").orElseGet(() -> {
            User user = new User("admin", "admin@booking.com", passwordEncoder.encode("Admin@123"), Role.ROLE_ADMIN);
            return userRepository.save(user);
        });

        User regularUser = userRepository.findByUsername("user").orElseGet(() -> {
            User user = new User("user", "user@booking.com", passwordEncoder.encode("User@123"), Role.ROLE_USER);
            return userRepository.save(user);
        });

        User johnDoe = userRepository.findByUsername("johndoe").orElseGet(() -> {
            User user = new User("johndoe", "john@booking.com", passwordEncoder.encode("Password@123"), Role.ROLE_USER);
            return userRepository.save(user);
        });

        // 2. Seed Resources if empty
        if (resourceRepository.count() == 0) {
            Resource confRoomA = resourceRepository.save(new Resource(
                    "Executive Boardroom A",
                    "High-tech boardroom equipped with 4K display, 12 seats, video conferencing, and soundproofing.",
                    "ROOM",
                    new BigDecimal("150.00"),
                    true
            ));

            Resource confRoomB = resourceRepository.save(new Resource(
                    "Conference Hall B",
                    "Spacious 50-seat conference hall with dual projection screens and surround audio.",
                    "ROOM",
                    new BigDecimal("300.00"),
                    true
            ));

            Resource teslaCar = resourceRepository.save(new Resource(
                    "Company EV - Tesla Model 3",
                    "Executive electric sedan available for client visits and airport transit.",
                    "VEHICLE",
                    new BigDecimal("80.00"),
                    true
            ));

            Resource projector4k = resourceRepository.save(new Resource(
                    "Ultra HD 4K Laser Projector",
                    "Portable high-lumen 4K laser projector with HDMI and wireless casting capabilities.",
                    "EQUIPMENT",
                    new BigDecimal("45.00"),
                    true
            ));

            Resource vrKit = resourceRepository.save(new Resource(
                    "VR Demo Station & Headset",
                    "Meta Quest Pro headset with spatial tracking and development workstation.",
                    "EQUIPMENT",
                    new BigDecimal("60.00"),
                    true
            ));

            // 3. Seed Sample Reservations
            LocalDateTime now = LocalDateTime.now();

            reservationRepository.save(new Reservation(
                    regularUser,
                    confRoomA,
                    now.plusDays(1).withHour(9).withMinute(0),
                    now.plusDays(1).withHour(11).withMinute(0),
                    new BigDecimal("300.00"),
                    ReservationStatus.CONFIRMED,
                    "Quarterly sprint review meeting"
            ));

            reservationRepository.save(new Reservation(
                    regularUser,
                    teslaCar,
                    now.plusDays(2).withHour(14).withMinute(0),
                    now.plusDays(2).withHour(18).withMinute(0),
                    new BigDecimal("320.00"),
                    ReservationStatus.PENDING,
                    "Client visit to corporate campus"
            ));

            reservationRepository.save(new Reservation(
                    johnDoe,
                    projector4k,
                    now.plusDays(3).withHour(10).withMinute(0),
                    now.plusDays(3).withHour(16).withMinute(0),
                    new BigDecimal("270.00"),
                    ReservationStatus.CONFIRMED,
                    "Design systems workshop"
            ));

            logger.info("Seed data successfully initialized: 3 Users (ADMIN, USER), 5 Resources, 3 Reservations.");
        }
    }
}
