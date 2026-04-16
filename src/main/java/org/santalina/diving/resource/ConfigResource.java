package org.santalina.diving.resource;

import org.santalina.diving.domain.User;
import org.santalina.diving.dto.ConfigDto.*;
import org.santalina.diving.mail.RegistrationReportMailer;
import org.santalina.diving.service.ConfigService;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Path("/api/config")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Configuration")
public class ConfigResource {

    @Inject
    ConfigService configService;

    @Inject
    RegistrationReportMailer reportMailer;

    @GET
    @PermitAll
    public ConfigResponse getConfig() {
        return configService.getConfig();
    }

    @PUT
    @Path("/max-divers")
    @RolesAllowed("ADMIN")
    public ConfigResponse updateMaxDivers(@Valid UpdateMaxDiversRequest request) {
        return configService.updateMaxDivers(request.maxDivers());
    }

    @PUT
    @Path("/site-name")
    @RolesAllowed("ADMIN")
    public ConfigResponse updateSiteName(@Valid UpdateSiteNameRequest request) {
        return configService.updateSiteName(request.siteName());
    }

    @PUT
    @Path("/slot-types")
    @RolesAllowed("ADMIN")
    public ConfigResponse updateSlotTypes(@Valid UpdateListRequest request) {
        return configService.updateSlotTypes(request.items());
    }

    @PUT
    @Path("/clubs")
    @RolesAllowed("ADMIN")
    public ConfigResponse updateClubs(@Valid UpdateListRequest request) {
        return configService.updateClubs(request.items());
    }

    @PUT
    @Path("/levels")
    @RolesAllowed("ADMIN")
    public ConfigResponse updateLevels(@Valid UpdateListRequest request) {
        return configService.updateLevels(request.items());
    }

    @PUT
    @Path("/diver-levels")
    @RolesAllowed("ADMIN")
    public ConfigResponse updateDiverLevels(@Valid UpdateListRequest request) {
        return configService.updateDiverLevels(request.items());
    }

    @PUT
    @Path("/dp-levels")
    @RolesAllowed("ADMIN")
    public ConfigResponse updateDpLevels(@Valid UpdateListRequest request) {
        return configService.updateDpLevels(request.items());
    }

    @PUT
    @Path("/prepared-levels")
    @RolesAllowed("ADMIN")
    public ConfigResponse updatePreparedLevels(@Valid UpdateListRequest request) {
        return configService.updatePreparedLevels(request.items());
    }

    @PUT
    @Path("/aptitudes")
    @RolesAllowed("ADMIN")
    public ConfigResponse updateAptitudes(@Valid UpdateListRequest request) {
        return configService.updateAptitudes(request.items());
    }

    @PUT
    @Path("/public-access")
    @RolesAllowed("ADMIN")
    public ConfigResponse updatePublicAccess(@Valid UpdateBooleanRequest request) {
        return configService.updatePublicAccess(request.value());
    }

    @PUT
    @Path("/self-registration")
    @RolesAllowed("ADMIN")
    public ConfigResponse updateSelfRegistration(@Valid UpdateBooleanRequest request) {
        return configService.updateSelfRegistration(request.value());
    }

    @PUT
    @Path("/booking-hours")
    @RolesAllowed("ADMIN")
    public ConfigResponse updateBookingHours(@Valid UpdateBookingHoursRequest request) {
        return configService.updateBookingHours(request.bookingOpenHour(), request.bookingCloseHour());
    }

    @PUT
    @Path("/exclusive-slot-types")
    @RolesAllowed("ADMIN")
    public ConfigResponse updateExclusiveSlotTypes(@Valid UpdateListRequest request) {
        return configService.updateExclusiveSlotTypes(request.items());
    }

    @PUT
    @Path("/default-slot-hours")
    @RolesAllowed("ADMIN")
    public ConfigResponse updateDefaultSlotHours(@Valid UpdateDefaultSlotHoursRequest request) {
        return configService.updateDefaultSlotHours(request.defaultSlotHours());
    }

    @PUT
    @Path("/slot-max-hours")
    @RolesAllowed("ADMIN")
    public ConfigResponse updateSlotMaxHours(@Valid UpdateSlotMaxHoursRequest request) {
        return configService.updateSlotMaxHours(request.slotMaxHours());
    }

    @PUT
    @Path("/notification-email")
    @RolesAllowed("ADMIN")
    public ConfigResponse updateNotificationEmail(UpdateNotificationEmailRequest request) {
        return configService.updateNotificationBookingEmail(request.email());
    }

    @PUT
    @Path("/max-recurring-months")
    @RolesAllowed("ADMIN")
    public ConfigResponse updateMaxRecurringMonths(@Valid UpdateMaxRecurringMonthsRequest request) {
        return configService.updateMaxRecurringMonths(request.maxRecurringMonths());
    }

    @PUT
    @Path("/notification-settings")
    @RolesAllowed("ADMIN")
    public ConfigResponse updateNotifSettings(@Valid UpdateNotifSettingsRequest request) {
        return configService.updateNotifSettings(
                request.notifRegistrationEnabled(),
                request.notifApprovedEnabled(),
                request.notifCancelledEnabled(),
                request.notifMovedToWlEnabled(),
                request.notifDpNewRegEnabled(),
                request.notifSafetyReminderEnabled(),
                request.safetyReminderDelayDays(),
                request.safetyReminderEmailBody()
        );
    }

    @PUT
    @Path("/maintenance-mode")
    @RolesAllowed("ADMIN")
    public ConfigResponse updateMaintenanceMode(@Valid UpdateBooleanRequest request) {
        return configService.updateMaintenanceMode(request.value());
    }

    @PUT
    @Path("/report-email-settings")
    @RolesAllowed("ADMIN")
    public ConfigResponse updateReportEmailSettings(@Valid UpdateReportEmailSettingsRequest request) {
        return configService.updateReportEmailSettings(
                request.reportEmailEnabled(),
                request.reportEmailPeriodDays(),
                request.reportEmailRecipients()
        );
    }

    @POST
    @Path("/report-email-send")
    @RolesAllowed("ADMIN")
    @Transactional
    public Response sendManualReport(@Valid ManualReportSendRequest request) {
        LocalDateTime fromDt = LocalDate.parse(request.fromDate()).atStartOfDay();
        LocalDateTime toDt   = LocalDate.parse(request.toDate()).plusDays(1).atStartOfDay();

        List<User> users = User.<User>find(
                "activated = true AND createdAt >= ?1 AND createdAt < ?2", fromDt, toDt
        ).list();
        users = filterByClub(users, request.club());

        String recipients = (request.recipients() != null && !request.recipients().isBlank())
                ? request.recipients()
                : configService.getReportEmailRecipients();

        reportMailer.sendReport(fromDt, users, recipients);

        return Response.ok("{\"count\":" + users.size() + "}").type(MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/report-email-download")
    @Produces("text/csv")
    @RolesAllowed("ADMIN")
    @Transactional
    public Response downloadReport(@QueryParam("from") String from, @QueryParam("to") String to,
                                    @QueryParam("club") String club) {
        LocalDateTime fromDt = LocalDate.parse(from).atStartOfDay();
        LocalDateTime toDt   = LocalDate.parse(to).plusDays(1).atStartOfDay();

        List<User> users = User.<User>find(
                "activated = true AND createdAt >= ?1 AND createdAt < ?2", fromDt, toDt
        ).list();
        users = filterByClub(users, club);

        byte[] csvBytes = reportMailer.buildCsvBytes(users);
        String clubSuffix = (club != null && !club.isBlank()) ? "_" + club.replaceAll("[^a-zA-Z0-9_-]", "_") : "";
        String filename = "inscriptions_" + from + "_" + to + clubSuffix + ".csv";

        return Response.ok(csvBytes)
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .header("Content-Type", "text/csv; charset=UTF-8")
                .build();
    }

    private List<User> filterByClub(List<User> users, String club) {
        if (club == null || club.isBlank()) return users;
        return users.stream()
                .filter(u -> club.equalsIgnoreCase(u.club))
                .collect(java.util.stream.Collectors.toList());
    }
}
