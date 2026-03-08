package com.example.diving.dto;

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

    public record StatsResponse(
            List<PeriodStat> byMonth,
            List<PeriodStat> byYear,
            List<GroupStat>  byClub,
            List<GroupStat>  byType,
            int totalSlots,
            int totalDivers
    ) {}
}
