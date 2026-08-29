package com.exelynt.booking.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class ResourceRequest {

    @NotBlank(message = "Resource name is required")
    private String name;

    private String description;

    @NotBlank(message = "Resource type is required (e.g., ROOM, VEHICLE, EQUIPMENT)")
    private String type;

    @NotNull(message = "Price per unit is required")
    @DecimalMin(value = "0.00", inclusive = false, message = "Price must be greater than zero")
    private BigDecimal pricePerUnit;

    private Boolean available = true;

    public ResourceRequest() {
    }

    public ResourceRequest(String name, String description, String type, BigDecimal pricePerUnit, Boolean available) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.pricePerUnit = pricePerUnit;
        this.available = available != null ? available : true;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BigDecimal getPricePerUnit() {
        return pricePerUnit;
    }

    public void setPricePerUnit(BigDecimal pricePerUnit) {
        this.pricePerUnit = pricePerUnit;
    }

    public Boolean getAvailable() {
        return available;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
    }
}
