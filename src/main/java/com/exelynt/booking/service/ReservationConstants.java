package com.exelynt.booking.service;

import java.math.BigDecimal;

/**
 * Shared reservation business-rule constants.
 * Centralising these values makes it easy to adjust policy (e.g. minimum billing
 * duration, clock-skew tolerance) without hunting through service logic.
 */
public final class ReservationConstants {

    /** Minutes of clock-skew tolerated when a client supplies a start time in the past. */
    public static final long ALLOWED_START_TIME_SKEW_MINUTES = 5;

    /** Number of minutes in one billing hour, used for duration-to-hours conversion. */
    public static final BigDecimal MINUTES_PER_HOUR = BigDecimal.valueOf(60);

    /** Minimum billable duration in hours; bookings shorter than one hour are billed as one hour. */
    public static final BigDecimal MINIMUM_HOURS = BigDecimal.ONE;

    private ReservationConstants() {
        // utility class — not instantiable
    }
}
