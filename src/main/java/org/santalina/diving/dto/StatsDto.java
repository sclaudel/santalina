package org.santalina.diving.dto;

import java.util.List;

public class StatsDto {

    public record PeriodStat(
            String label,    // "2026-03" ou "2026"
            int slots,       // nombre de créneaux
            int divers       // nombre de plongées inscrites
    ) {}

    public record GroupStat(
            String label,    // nom du club ou du type
            int slots,
            int divers
    ) {}

    public record DpPeriodStat(
            String label,
            int    directions,
            double avgDivers
    ) {}

    public record DpStat(
            String             name,
            int                totalDirections,
            double             avgDiversPerSlot,
            List<DpPeriodStat> byYear,
            List<DpPeriodStat> byMonth
    ) {}

    public record StatsResponse(
            List<PeriodStat> byMonth,
            List<PeriodStat> byYear,
            List<GroupStat>  byClub,
            List<GroupStat>  byType,
            int              totalSlots,
            int              totalDivers,
            int              totalClubs,
            double           avgDiversPerSlot,
            List<PeriodStat> byDayOfWeek,
            List<GroupStat>  byLevel,
            List<DpStat>     byDiveDirector
    ) {}

    public record MyStatsResponse(
            List<PeriodStat> byMonth,
            List<PeriodStat> byYear,
            List<GroupStat>  byClub,
            List<GroupStat>  byType,
            int              totalSlots,
            int              totalDivers,
            double           avgDiversPerSlot,
            List<PeriodStat> byDayOfWeek,
            List<GroupStat>  byLevel
    ) {}
}
