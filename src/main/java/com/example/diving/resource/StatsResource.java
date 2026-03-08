package com.example.diving.resource;

import com.example.diving.domain.DiveSlot;
import com.example.diving.domain.SlotDiver;
import com.example.diving.dto.StatsDto.*;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Path("/api/stats")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMIN")
@Tag(name = "Statistiques")
public class StatsResource {

    @GET
    public StatsResponse getStats(
            @QueryParam("from") String fromParam,
            @QueryParam("to")   String toParam) {

        LocalDate from = (fromParam != null) ? LocalDate.parse(fromParam) : LocalDate.of(2000, 1, 1);
        LocalDate to   = (toParam   != null) ? LocalDate.parse(toParam)   : LocalDate.of(2100, 12, 31);

        List<DiveSlot> slots = DiveSlot.findByDateRange(from, to);

        // Pour chaque créneau, on compte les vrais plongeurs inscrits
        Map<Long, Integer> diversBySlot = new HashMap<>();
        for (DiveSlot s : slots) {
            diversBySlot.put(s.id, (int) SlotDiver.countBySlot(s.id));
        }

        // --- Par mois ---
        Map<String, int[]> byMonth = new TreeMap<>();
        for (DiveSlot s : slots) {
            String key = s.slotDate.getYear() + "-" + String.format("%02d", s.slotDate.getMonthValue());
            byMonth.computeIfAbsent(key, k -> new int[2]);
            byMonth.get(key)[0]++;
            byMonth.get(key)[1] += diversBySlot.getOrDefault(s.id, 0);
        }
        List<PeriodStat> statsByMonth = byMonth.entrySet().stream()
                .map(e -> new PeriodStat(e.getKey(), e.getValue()[0], e.getValue()[1]))
                .toList();

        // --- Par année ---
        Map<String, int[]> byYear = new TreeMap<>();
        for (DiveSlot s : slots) {
            String key = String.valueOf(s.slotDate.getYear());
            byYear.computeIfAbsent(key, k -> new int[2]);
            byYear.get(key)[0]++;
            byYear.get(key)[1] += diversBySlot.getOrDefault(s.id, 0);
        }
        List<PeriodStat> statsByYear = byYear.entrySet().stream()
                .map(e -> new PeriodStat(e.getKey(), e.getValue()[0], e.getValue()[1]))
                .toList();

        // --- Par club ---
        Map<String, int[]> byClub = new LinkedHashMap<>();
        for (DiveSlot s : slots) {
            String key = (s.club != null && !s.club.isBlank()) ? s.club : "— Sans club —";
            byClub.computeIfAbsent(key, k -> new int[2]);
            byClub.get(key)[0]++;
            byClub.get(key)[1] += diversBySlot.getOrDefault(s.id, 0);
        }
        List<GroupStat> statsByClub = byClub.entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<String, int[]> e) -> e.getValue()[1]).reversed())
                .map(e -> new GroupStat(e.getKey(), e.getValue()[0], e.getValue()[1]))
                .collect(Collectors.toList());

        // --- Par type ---
        Map<String, int[]> byType = new LinkedHashMap<>();
        for (DiveSlot s : slots) {
            String key = (s.slotType != null && !s.slotType.isBlank()) ? s.slotType : "— Sans type —";
            byType.computeIfAbsent(key, k -> new int[2]);
            byType.get(key)[0]++;
            byType.get(key)[1] += diversBySlot.getOrDefault(s.id, 0);
        }
        List<GroupStat> statsByType = byType.entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<String, int[]> e) -> e.getValue()[1]).reversed())
                .map(e -> new GroupStat(e.getKey(), e.getValue()[0], e.getValue()[1]))
                .collect(Collectors.toList());

        int totalSlots  = slots.size();
        int totalDivers = diversBySlot.values().stream().mapToInt(Integer::intValue).sum();

        return new StatsResponse(statsByMonth, statsByYear, statsByClub, statsByType, totalSlots, totalDivers);
    }
}
