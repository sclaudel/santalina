package org.santalina.diving.resource;

import org.santalina.diving.domain.DiveSlot;
import org.santalina.diving.domain.SlotDiver;
import org.santalina.diving.domain.User;
import org.santalina.diving.dto.StatsDto.*;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Path("/api/stats")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMIN")
@Tag(name = "Statistiques")
public class StatsResource {

    @Inject
    JsonWebToken jwt;

    private static final String[] DAYS_FR = {
        "Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche"
    };

    @GET
    public StatsResponse getStats(
            @QueryParam("from") String fromParam,
            @QueryParam("to")   String toParam) {

        LocalDate from = (fromParam != null) ? LocalDate.parse(fromParam) : LocalDate.of(2000, 1, 1);
        LocalDate to   = (toParam   != null) ? LocalDate.parse(toParam)   : LocalDate.of(2100, 12, 31);

        List<DiveSlot> slots = DiveSlot.findByDateRange(from, to);

        // Charge tous les plongeurs en une seule requête
        List<Long> slotIds = slots.stream().map(s -> s.id).toList();
        List<SlotDiver> allDivers = SlotDiver.findBySlotIds(slotIds);

        // Map slot.id → nb plongeurs inscrits
        Map<Long, Integer> diversBySlot = new HashMap<>();
        for (SlotDiver sd : allDivers) {
            diversBySlot.merge(sd.slot.id, 1, Integer::sum);
        }

        // Map slot.id → DiveSlot (pour les stats DP)
        Map<Long, DiveSlot> slotMap = slots.stream()
                .collect(Collectors.toMap(s -> s.id, s -> s));

        // ── Par mois ──────────────────────────────────────────────────────────
        Map<String, int[]> byMonthMap = new TreeMap<>();
        for (DiveSlot s : slots) {
            String key = s.slotDate.getYear() + "-" + String.format("%02d", s.slotDate.getMonthValue());
            byMonthMap.computeIfAbsent(key, k -> new int[2]);
            byMonthMap.get(key)[0]++;
            byMonthMap.get(key)[1] += diversBySlot.getOrDefault(s.id, 0);
        }
        List<PeriodStat> statsByMonth = byMonthMap.entrySet().stream()
                .map(e -> new PeriodStat(e.getKey(), e.getValue()[0], e.getValue()[1]))
                .toList();

        // ── Par année ─────────────────────────────────────────────────────────
        Map<String, int[]> byYearMap = new TreeMap<>();
        for (DiveSlot s : slots) {
            String key = String.valueOf(s.slotDate.getYear());
            byYearMap.computeIfAbsent(key, k -> new int[2]);
            byYearMap.get(key)[0]++;
            byYearMap.get(key)[1] += diversBySlot.getOrDefault(s.id, 0);
        }
        List<PeriodStat> statsByYear = byYearMap.entrySet().stream()
                .map(e -> new PeriodStat(e.getKey(), e.getValue()[0], e.getValue()[1]))
                .toList();

        // ── Par club ──────────────────────────────────────────────────────────
        Map<String, int[]> byClubMap = new LinkedHashMap<>();
        for (DiveSlot s : slots) {
            String key = (s.club != null && !s.club.isBlank()) ? s.club : "— Sans club —";
            byClubMap.computeIfAbsent(key, k -> new int[2]);
            byClubMap.get(key)[0]++;
            byClubMap.get(key)[1] += diversBySlot.getOrDefault(s.id, 0);
        }
        List<GroupStat> statsByClub = byClubMap.entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<String, int[]> e) -> e.getValue()[1]).reversed())
                .map(e -> new GroupStat(e.getKey(), e.getValue()[0], e.getValue()[1]))
                .collect(Collectors.toList());

        // ── Par type ──────────────────────────────────────────────────────────
        Map<String, int[]> byTypeMap = new LinkedHashMap<>();
        for (DiveSlot s : slots) {
            String key = (s.slotType != null && !s.slotType.isBlank()) ? s.slotType : "— Sans type —";
            byTypeMap.computeIfAbsent(key, k -> new int[2]);
            byTypeMap.get(key)[0]++;
            byTypeMap.get(key)[1] += diversBySlot.getOrDefault(s.id, 0);
        }
        List<GroupStat> statsByType = byTypeMap.entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<String, int[]> e) -> e.getValue()[1]).reversed())
                .map(e -> new GroupStat(e.getKey(), e.getValue()[0], e.getValue()[1]))
                .collect(Collectors.toList());

        // ── Par jour de la semaine ────────────────────────────────────────────
        int[][] byDow = new int[7][2]; // [0]=slots, [1]=divers
        for (DiveSlot s : slots) {
            int dow = s.slotDate.getDayOfWeek().getValue() - 1; // 0=Lundi
            byDow[dow][0]++;
            byDow[dow][1] += diversBySlot.getOrDefault(s.id, 0);
        }
        List<PeriodStat> statsByDayOfWeek = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            statsByDayOfWeek.add(new PeriodStat(DAYS_FR[i], byDow[i][0], byDow[i][1]));
        }

        // ── Par niveau ────────────────────────────────────────────────────────
        Map<String, Integer> levelMap = new LinkedHashMap<>();
        for (SlotDiver sd : allDivers) {
            String lvl = (sd.level != null && !sd.level.isBlank()) ? sd.level : "— Inconnu —";
            levelMap.merge(lvl, 1, Integer::sum);
        }
        List<GroupStat> statsByLevel = levelMap.entrySet().stream()
                .sorted(Comparator.comparingInt(Map.Entry<String, Integer>::getValue).reversed())
                .map(e -> new GroupStat(e.getKey(), 0, e.getValue()))
                .collect(Collectors.toList());

        // ── Par Directeur de Plongée ──────────────────────────────────────────
        // Groupement par nom (prénom + nom) des plongeurs marqués isDirector
        Map<String, List<Long>> slotIdsByDp = new LinkedHashMap<>();
        for (SlotDiver sd : allDivers) {
            if (!sd.isDirector) continue;
            String dpName = sd.firstName + " " + sd.lastName;
            slotIdsByDp.computeIfAbsent(dpName, k -> new ArrayList<>()).add(sd.slot.id);
        }

        List<DpStat> statsByDp = new ArrayList<>();
        for (Map.Entry<String, List<Long>> entry : slotIdsByDp.entrySet()) {
            String dpName      = entry.getKey();
            List<Long> dpSlots = entry.getValue();
            int totalDir       = dpSlots.size();
            int totalDpDivers  = dpSlots.stream()
                    .mapToInt(id -> diversBySlot.getOrDefault(id, 0)).sum();
            double avgDivers   = totalDir > 0 ? (double) totalDpDivers / totalDir : 0.0;

            // Par année
            Map<String, int[]> dpYearMap = new TreeMap<>();
            for (Long sid : dpSlots) {
                DiveSlot s = slotMap.get(sid);
                if (s == null) continue;
                String key = String.valueOf(s.slotDate.getYear());
                dpYearMap.computeIfAbsent(key, k -> new int[2]);
                dpYearMap.get(key)[0]++;
                dpYearMap.get(key)[1] += diversBySlot.getOrDefault(sid, 0);
            }
            List<DpPeriodStat> dpByYear = dpYearMap.entrySet().stream()
                    .map(e -> new DpPeriodStat(
                            e.getKey(), e.getValue()[0],
                            e.getValue()[0] > 0 ? (double) e.getValue()[1] / e.getValue()[0] : 0.0))
                    .toList();

            // Par mois
            Map<String, int[]> dpMonthMap = new TreeMap<>();
            for (Long sid : dpSlots) {
                DiveSlot s = slotMap.get(sid);
                if (s == null) continue;
                String key = s.slotDate.getYear() + "-" + String.format("%02d", s.slotDate.getMonthValue());
                dpMonthMap.computeIfAbsent(key, k -> new int[2]);
                dpMonthMap.get(key)[0]++;
                dpMonthMap.get(key)[1] += diversBySlot.getOrDefault(sid, 0);
            }
            List<DpPeriodStat> dpByMonth = dpMonthMap.entrySet().stream()
                    .map(e -> new DpPeriodStat(
                            e.getKey(), e.getValue()[0],
                            e.getValue()[0] > 0 ? (double) e.getValue()[1] / e.getValue()[0] : 0.0))
                    .toList();

            statsByDp.add(new DpStat(dpName, totalDir, avgDivers, dpByYear, dpByMonth));
        }
        statsByDp.sort(Comparator.comparingInt(DpStat::totalDirections).reversed());

        // ── Totaux ───────────────────────────────────────────────────────────
        int totalSlots  = slots.size();
        int totalDivers = diversBySlot.values().stream().mapToInt(Integer::intValue).sum();
        int totalClubs  = (int) slots.stream()
                .map(s -> (s.club != null && !s.club.isBlank()) ? s.club : null)
                .filter(Objects::nonNull).distinct().count();
        double avgDiversPerSlot = totalSlots > 0 ? (double) totalDivers / totalSlots : 0.0;
        int totalCapacity = slots.stream().mapToInt(s -> s.diverCount).sum();
        double fillRate = totalCapacity > 0 ? (double) totalDivers / totalCapacity * 100.0 : 0.0;

        // ── Record du jour ────────────────────────────────────────────────────
        Map<String, Integer> diversByDay = new TreeMap<>();
        for (DiveSlot s : slots) {
            String dayKey = s.slotDate.toString();
            diversByDay.merge(dayKey, diversBySlot.getOrDefault(s.id, 0), Integer::sum);
        }
        String bestDayDate   = "";
        int    bestDayDivers = 0;
        for (Map.Entry<String, Integer> e : diversByDay.entrySet()) {
            if (e.getValue() > bestDayDivers) { bestDayDivers = e.getValue(); bestDayDate = e.getKey(); }
        }

        // ── Meilleur mois ─────────────────────────────────────────────────────
        String bestMonthLabel   = "";
        int    bestMonthDivers  = 0;
        for (PeriodStat ps : statsByMonth) {
            if (ps.divers() > bestMonthDivers) { bestMonthDivers = ps.divers(); bestMonthLabel = ps.label(); }
        }

        // ── Jours distincts avec créneau ──────────────────────────────────────
        int totalUniqueDays = (int) slots.stream().map(s -> s.slotDate).distinct().count();

        return new StatsResponse(
                statsByMonth, statsByYear, statsByClub, statsByType,
                totalSlots, totalDivers, totalClubs, avgDiversPerSlot,
                statsByDayOfWeek, statsByLevel, statsByDp,
                totalCapacity, fillRate,
                bestDayDate, bestDayDivers, bestMonthLabel, bestMonthDivers, totalUniqueDays);
    }

    // ── Statistiques personnelles du Directeur de Plongée ─────────────────────
    @GET
    @Path("/my")
    @RolesAllowed({"ADMIN", "DIVE_DIRECTOR"})
    public MyStatsResponse getMyStats(
            @QueryParam("from") String fromParam,
            @QueryParam("to")   String toParam) {

        User currentUser = User.findByEmail(jwt.getName());
        if (currentUser == null) throw new NotAuthorizedException("Utilisateur non trouvé");

        LocalDate from = (fromParam != null) ? LocalDate.parse(fromParam) : LocalDate.of(2000, 1, 1);
        LocalDate to   = (toParam   != null) ? LocalDate.parse(toParam)   : LocalDate.of(2100, 12, 31);

        List<DiveSlot> slots = DiveSlot.findByCreatorAndDateRange(currentUser.id, from, to);

        List<Long> slotIds = slots.stream().map(s -> s.id).toList();
        List<SlotDiver> allDivers = SlotDiver.findBySlotIds(slotIds);

        Map<Long, Integer> diversBySlot = new HashMap<>();
        for (SlotDiver sd : allDivers) {
            diversBySlot.merge(sd.slot.id, 1, Integer::sum);
        }

        // ── Par mois ──────────────────────────────────────────────────────────
        Map<String, int[]> byMonthMap = new TreeMap<>();
        for (DiveSlot s : slots) {
            String key = s.slotDate.getYear() + "-" + String.format("%02d", s.slotDate.getMonthValue());
            byMonthMap.computeIfAbsent(key, k -> new int[2]);
            byMonthMap.get(key)[0]++;
            byMonthMap.get(key)[1] += diversBySlot.getOrDefault(s.id, 0);
        }
        List<PeriodStat> myByMonth = byMonthMap.entrySet().stream()
                .map(e -> new PeriodStat(e.getKey(), e.getValue()[0], e.getValue()[1]))
                .toList();

        // ── Par année ─────────────────────────────────────────────────────────
        Map<String, int[]> byYearMap = new TreeMap<>();
        for (DiveSlot s : slots) {
            String key = String.valueOf(s.slotDate.getYear());
            byYearMap.computeIfAbsent(key, k -> new int[2]);
            byYearMap.get(key)[0]++;
            byYearMap.get(key)[1] += diversBySlot.getOrDefault(s.id, 0);
        }
        List<PeriodStat> myByYear = byYearMap.entrySet().stream()
                .map(e -> new PeriodStat(e.getKey(), e.getValue()[0], e.getValue()[1]))
                .toList();

        // ── Par club ──────────────────────────────────────────────────────────
        Map<String, int[]> byClubMap = new LinkedHashMap<>();
        for (DiveSlot s : slots) {
            String key = (s.club != null && !s.club.isBlank()) ? s.club : "— Sans club —";
            byClubMap.computeIfAbsent(key, k -> new int[2]);
            byClubMap.get(key)[0]++;
            byClubMap.get(key)[1] += diversBySlot.getOrDefault(s.id, 0);
        }
        List<GroupStat> myByClub = byClubMap.entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<String, int[]> e) -> e.getValue()[1]).reversed())
                .map(e -> new GroupStat(e.getKey(), e.getValue()[0], e.getValue()[1]))
                .collect(Collectors.toList());

        // ── Par type ──────────────────────────────────────────────────────────
        Map<String, int[]> byTypeMap = new LinkedHashMap<>();
        for (DiveSlot s : slots) {
            String key = (s.slotType != null && !s.slotType.isBlank()) ? s.slotType : "— Sans type —";
            byTypeMap.computeIfAbsent(key, k -> new int[2]);
            byTypeMap.get(key)[0]++;
            byTypeMap.get(key)[1] += diversBySlot.getOrDefault(s.id, 0);
        }
        List<GroupStat> myByType = byTypeMap.entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<String, int[]> e) -> e.getValue()[1]).reversed())
                .map(e -> new GroupStat(e.getKey(), e.getValue()[0], e.getValue()[1]))
                .collect(Collectors.toList());

        // ── Par jour de la semaine ────────────────────────────────────────────
        int[][] byDow = new int[7][2];
        for (DiveSlot s : slots) {
            int dow = s.slotDate.getDayOfWeek().getValue() - 1;
            byDow[dow][0]++;
            byDow[dow][1] += diversBySlot.getOrDefault(s.id, 0);
        }
        List<PeriodStat> myByDayOfWeek = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            myByDayOfWeek.add(new PeriodStat(DAYS_FR[i], byDow[i][0], byDow[i][1]));
        }

        // ── Par niveau ────────────────────────────────────────────────────────
        Map<String, Integer> levelMap = new LinkedHashMap<>();
        for (SlotDiver sd : allDivers) {
            String lvl = (sd.level != null && !sd.level.isBlank()) ? sd.level : "— Inconnu —";
            levelMap.merge(lvl, 1, Integer::sum);
        }
        List<GroupStat> myByLevel = levelMap.entrySet().stream()
                .sorted(Comparator.comparingInt(Map.Entry<String, Integer>::getValue).reversed())
                .map(e -> new GroupStat(e.getKey(), 0, e.getValue()))
                .collect(Collectors.toList());

        // ── Totaux ───────────────────────────────────────────────────────────
        int myTotalSlots  = slots.size();
        int myTotalDivers = diversBySlot.values().stream().mapToInt(Integer::intValue).sum();
        double myAvgDiversPerSlot = myTotalSlots > 0 ? (double) myTotalDivers / myTotalSlots : 0.0;
        int myTotalCapacity = slots.stream().mapToInt(s -> s.diverCount).sum();
        double myFillRate = myTotalCapacity > 0 ? (double) myTotalDivers / myTotalCapacity * 100.0 : 0.0;

        // ── Record du jour ────────────────────────────────────────────────────
        Map<String, Integer> myDiversByDay = new TreeMap<>();
        for (DiveSlot s : slots) {
            String dayKey = s.slotDate.toString();
            myDiversByDay.merge(dayKey, diversBySlot.getOrDefault(s.id, 0), Integer::sum);
        }
        String myBestDayDate   = "";
        int    myBestDayDivers = 0;
        for (Map.Entry<String, Integer> e : myDiversByDay.entrySet()) {
            if (e.getValue() > myBestDayDivers) { myBestDayDivers = e.getValue(); myBestDayDate = e.getKey(); }
        }

        // ── Meilleur mois ─────────────────────────────────────────────────────
        String myBestMonthLabel  = "";
        int    myBestMonthDivers = 0;
        for (PeriodStat ps : myByMonth) {
            if (ps.divers() > myBestMonthDivers) { myBestMonthDivers = ps.divers(); myBestMonthLabel = ps.label(); }
        }

        // ── Jours distincts avec créneau ──────────────────────────────────────
        int myTotalUniqueDays = (int) slots.stream().map(s -> s.slotDate).distinct().count();

        return new MyStatsResponse(
                myByMonth, myByYear, myByClub, myByType,
                myTotalSlots, myTotalDivers, myAvgDiversPerSlot,
                myByDayOfWeek, myByLevel,
                myTotalCapacity, myFillRate,
                myBestDayDate, myBestDayDivers, myBestMonthLabel, myBestMonthDivers, myTotalUniqueDays);
    }
}
