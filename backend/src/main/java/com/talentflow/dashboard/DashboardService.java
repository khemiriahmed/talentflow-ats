package com.talentflow.dashboard;

import com.talentflow.application.Stage;
import com.talentflow.common.BadRequestException;
import com.talentflow.dashboard.DashboardStatsDto.FunnelStep;
import com.talentflow.dashboard.DashboardStatsDto.JobTotal;
import com.talentflow.dashboard.DashboardStatsDto.SourceTotal;
import com.talentflow.dashboard.DashboardStatsDto.StageTotal;
import com.talentflow.dashboard.DashboardStatsDto.Summary;
import com.talentflow.dashboard.DashboardStatsDto.TimePoint;
import com.talentflow.dashboard.DashboardStatsDto.TimeToHire;
import com.talentflow.job.JobOfferRepository;
import com.talentflow.job.JobStatus;
import com.talentflow.notification.AppMailProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final List<Stage> FUNNEL = List.of(Stage.RECEIVED, Stage.SCREENING, Stage.HR_INTERVIEW,
            Stage.TECH_INTERVIEW, Stage.OFFER, Stage.HIRED);
    private static final int TOP_JOBS = 10;
    private static final Duration DEFAULT_RANGE = Duration.ofDays(90);
    private static final long MAX_RANGE_DAYS = 366;

    private final DashboardRepository repository;
    private final JobOfferRepository jobOfferRepository;
    private final AppMailProperties appProps;   // fuseau d'affichage (découpage par jour / semaine / mois)

    @Transactional(readOnly = true)
    public DashboardStatsDto stats(Instant from, Instant to, Long jobOfferId, Granularity granularity) {
        Instant end = to != null ? to : Instant.now();
        Instant start = from != null ? from : end.minus(DEFAULT_RANGE);
        if (!end.isAfter(start)) {
            throw new BadRequestException("'to' must be after 'from'");
        }
        if (Duration.between(start, end).toDays() > MAX_RANGE_DAYS) {
            throw new BadRequestException("The range must not exceed " + MAX_RANGE_DAYS + " days");
        }
        long jobId = jobOfferId == null ? 0L : jobOfferId;
        Granularity bucket = granularity != null ? granularity : autoGranularity(start, end);
        ZoneId zone = ZoneId.of(appProps.timezone());

        // --- répartition actuelle + résumé ---
        Map<Stage, Long> current = new EnumMap<>(Stage.class);
        repository.countByCurrentStage(start, end, jobId).forEach(c -> current.put(c.stage(), c.count()));
        long total = current.values().stream().mapToLong(Long::longValue).sum();
        long hired = current.getOrDefault(Stage.HIRED, 0L);
        long rejected = current.getOrDefault(Stage.REJECTED, 0L);
        Summary summary = new Summary(total, jobOfferRepository.countByStatus(JobStatus.PUBLISHED),
                hired, rejected, total - hired - rejected);

        List<StageTotal> currentStages = new ArrayList<>();
        for (Stage stage : Stage.values()) {
            currentStages.add(new StageTotal(stage, current.getOrDefault(stage, 0L)));
        }

        // --- entonnoir de conversion ---
        Map<Stage, Long> reached = new EnumMap<>(Stage.class);
        repository.countReachedStage(start, end, jobId).forEach(c -> reached.put(c.stage(), c.count()));
        long received = reached.getOrDefault(Stage.RECEIVED, 0L);
        List<FunnelStep> funnel = new ArrayList<>();
        Long previous = null;
        for (Stage stage : FUNNEL) {
            long count = reached.getOrDefault(stage, 0L);
            funnel.add(new FunnelStep(stage, count, percent(count, received),
                    previous == null ? null : percent(count, previous)));
            previous = count;
        }

        // --- candidatures dans le temps (zéros inclus pour un graphique continu) ---
        TreeMap<LocalDate, Long> perBucket = new TreeMap<>();
        for (Instant created : repository.applicationDates(start, end, jobId)) {
            perBucket.merge(bucketStart(created.atZone(zone).toLocalDate(), bucket), 1L, Long::sum);
        }
        List<TimePoint> overTime = new ArrayList<>();
        LocalDate cursor = bucketStart(start.atZone(zone).toLocalDate(), bucket);
        LocalDate last = bucketStart(end.minusNanos(1).atZone(zone).toLocalDate(), bucket);
        while (!cursor.isAfter(last)) {
            overTime.add(new TimePoint(cursor, perBucket.getOrDefault(cursor, 0L)));
            cursor = next(cursor, bucket);
        }

        // --- offres les plus demandées, sources ---
        List<JobTotal> topJobs = repository.countByJob(start, end, jobId, PageRequest.of(0, TOP_JOBS)).stream()
                .map(j -> new JobTotal(j.jobOfferId(), j.title(), j.count())).toList();
        List<SourceTotal> bySource = repository.countBySource(start, end, jobId).stream()
                .map(s -> new SourceTotal(s.label() == null ? "unknown" : s.label(), s.count())).toList();

        return new DashboardStatsDto(start, end, jobOfferId, bucket, summary, currentStages, funnel,
                overTime, topJobs, bySource, timeToHire(start, end, jobId));
    }

    // ---------- helpers ----------

    private TimeToHire timeToHire(Instant start, Instant end, long jobId) {
        List<Double> days = repository.hireTimings(start, end, jobId).stream()
                .map(t -> Duration.between(t.applied(), t.hired()).toMinutes() / 1440.0)
                .sorted().toList();
        if (days.isEmpty()) {
            return new TimeToHire(0, null, null);
        }
        double average = days.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        int mid = days.size() / 2;
        double median = days.size() % 2 == 1 ? days.get(mid) : (days.get(mid - 1) + days.get(mid)) / 2;
        return new TimeToHire(days.size(), round1(average), round1(median));
    }

    private static Granularity autoGranularity(Instant start, Instant end) {
        long days = Duration.between(start, end).toDays();
        return days <= 31 ? Granularity.DAY : days <= 180 ? Granularity.WEEK : Granularity.MONTH;
    }

    private static LocalDate bucketStart(LocalDate date, Granularity granularity) {
        return switch (granularity) {
            case DAY -> date;
            case WEEK -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MONTH -> date.withDayOfMonth(1);
        };
    }

    private static LocalDate next(LocalDate date, Granularity granularity) {
        return switch (granularity) {
            case DAY -> date.plusDays(1);
            case WEEK -> date.plusWeeks(1);
            case MONTH -> date.plusMonths(1);
        };
    }

    private static double percent(long count, long base) {
        return base == 0 ? 0.0 : round1(count * 100.0 / base);
    }

    private static double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }
}
