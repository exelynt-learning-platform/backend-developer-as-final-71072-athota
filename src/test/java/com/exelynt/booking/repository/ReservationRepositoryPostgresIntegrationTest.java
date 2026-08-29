package com.exelynt.booking.repository;

import com.exelynt.booking.entity.Reservation;
import com.exelynt.booking.entity.ReservationStatus;
import com.exelynt.booking.entity.Resource;
import com.exelynt.booking.entity.Role;
import com.exelynt.booking.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(properties = "app.seed.enabled=false")
class ReservationRepositoryPostgresIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private UserRepository userRepository;

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @BeforeEach
    void resetDatabase() {
        reservationRepository.deleteAll();
        resourceRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void findByResourceAndTimeWindow_whenActiveReservationOverlaps_returnsIt() {
        User user = userRepository.save(new User(
                "postgres-user", "postgres-user@example.com", "encoded-password", Role.ROLE_USER));
        Resource resource = resourceRepository.save(new Resource(
                "PostgreSQL test room", "Database-backed overlap test", "ROOM", new BigDecimal("50.00"), true));
        LocalDateTime start = LocalDateTime.now().plusDays(1).withSecond(0).withNano(0);
        LocalDateTime end = start.plusHours(2);
        reservationRepository.save(new Reservation(
                user, resource, start, end, new BigDecimal("100.00"), ReservationStatus.CONFIRMED, "integration test"));

        assertThat(reservationRepository.findByResource_IdAndStatusNotAndStartTimeLessThanAndEndTimeGreaterThan(
                resource.getId(), ReservationStatus.CANCELLED, end.plusMinutes(30), start.plusMinutes(30)))
                .hasSize(1);
    }
}
