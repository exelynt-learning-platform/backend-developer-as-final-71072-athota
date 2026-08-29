package com.exelynt.booking.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class ReservationRequest {

    @NotNull(message = "Resource ID is required")
    private Long resourceId;

    @NotNull(message = "Start time is required")
    @FutureOrPresent(message = "Start time must be in the present or future")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    private LocalDateTime endTime;

    private String notes;

    public ReservationRequest() {
    }

    public ReservationRequest(Long resourceId, LocalDateTime startTime, LocalDateTime endTime, String notes) {
        this.resourceId = resourceId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.notes = notes;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
