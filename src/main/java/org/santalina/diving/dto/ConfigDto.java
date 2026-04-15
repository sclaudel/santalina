package org.santalina.diving.dto;

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
            List<String> clubs,
            List<String> levels,
            boolean publicAccess,
            boolean selfRegistration,
            int bookingOpenHour,
            int bookingCloseHour,
            List<String> exclusiveSlotTypes,
            int defaultSlotHours,
            String notificationBookingEmail,
            int maxRecurringMonths,
            boolean notifRegistrationEnabled,
            boolean notifApprovedEnabled,
            boolean notifCancelledEnabled,
            boolean notifMovedToWlEnabled,
            boolean notifDpNewRegEnabled,
            boolean notifSafetyReminderEnabled,
            int safetyReminderDelayDays,
            String safetyReminderEmailBody,
            boolean maintenanceMode
    ) {}

    public record UpdateMaxRecurringMonthsRequest(
            @NotNull @Min(1) @jakarta.validation.constraints.Max(24) Integer maxRecurringMonths
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

    public record UpdateBooleanRequest(
            @NotNull Boolean value
    ) {}

    public record UpdateBookingHoursRequest(
            @NotNull @Min(-1) @jakarta.validation.constraints.Max(23) Integer bookingOpenHour,
            @NotNull @Min(-1) @jakarta.validation.constraints.Max(23) Integer bookingCloseHour
    ) {}

    public record UpdateDefaultSlotHoursRequest(
            @NotNull @Min(1) @jakarta.validation.constraints.Max(24) Integer defaultSlotHours
    ) {}

    public record UpdateSlotMaxHoursRequest(
            @NotNull @Min(1) @jakarta.validation.constraints.Max(24) Integer slotMaxHours
    ) {}

    public record UpdateNotificationEmailRequest(
            String email
    ) {}

    public record UpdateNotifSettingsRequest(
            @NotNull Boolean notifRegistrationEnabled,
            @NotNull Boolean notifApprovedEnabled,
            @NotNull Boolean notifCancelledEnabled,
            @NotNull Boolean notifMovedToWlEnabled,
            @NotNull Boolean notifDpNewRegEnabled,
            @NotNull Boolean notifSafetyReminderEnabled,
            @NotNull @Min(1) @jakarta.validation.constraints.Max(30) Integer safetyReminderDelayDays,
            String safetyReminderEmailBody
    ) {}
}
