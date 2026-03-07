package com.example.diving.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public class ConfigDto {

    public record ConfigResponse(
            int maxDivers,
            int slotMinHours,
            int slotMaxHours,
            int slotResolutionMinutes,
            String siteName,
            List<String> slotTypes,
            List<String> clubs
    ) {}

    public record UpdateMaxDiversRequest(
            @NotNull @Min(1) Integer maxDivers
    ) {}

    public record UpdateSiteNameRequest(
            @NotBlank @Size(min = 2, max = 100) String siteName
    ) {}

    public record UpdateListRequest(
            @NotNull List<@NotBlank String> items
    ) {}
}
