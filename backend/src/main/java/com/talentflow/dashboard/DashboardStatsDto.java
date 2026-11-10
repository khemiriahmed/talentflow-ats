package com.talentflow.dashboard;

import com.talentflow.application.Stage;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record DashboardStatsDto(
        Instant from,
        Instant to,
        Long jobOfferId,
        Granularity granularity,
        Summary summary,
        List<StageTotal> currentStages,
        List<FunnelStep> funnel,
        List<TimePoint> applicationsOverTime,
        List<JobTotal> topJobs,
        List<SourceTotal> bySource,
        TimeToHire timeToHire) {

    public record Summary(long totalApplications, long openJobs, long hired, long rejected, long inProgress) {
    }

    public record StageTotal(Stage stage, long count) {
    }

    /** count = candidatures ayant atteint l'étape ; les pourcentages sont arrondis à 0,1. */
    public record FunnelStep(Stage stage, long count, double percentOfReceived, Double percentOfPrevious) {
    }

    public record TimePoint(LocalDate periodStart, long count) {
    }

    public record JobTotal(Long jobOfferId, String title, long count) {
    }

    public record SourceTotal(String label, long count) {
    }

    public record TimeToHire(long hiredCount, Double averageDays, Double medianDays) {
    }
}
