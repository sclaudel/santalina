package com.example.diving.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class ConfigDto {

    public record ConfigResponse(
            int maxDivers,
            int slotMinHours,
            int slotMaxHours,
            int slotResolutionMinutes
    ) {}

    public record UpdateMaxDiversRequest(
            @NotNull @Min(1) Integer maxDivers
    ) {}
}

