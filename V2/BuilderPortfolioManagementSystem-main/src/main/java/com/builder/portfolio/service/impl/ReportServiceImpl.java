package com.builder.portfolio.service.impl;

import com.builder.portfolio.model.PortfolioReport;
import com.builder.portfolio.model.ProjectSummary;
import com.builder.portfolio.service.ProjectService;
import com.builder.portfolio.service.ReportService;
import com.builder.portfolio.util.BackgroundTaskManager;
import com.builder.portfolio.util.ProjectCache;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportServiceImpl implements ReportService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReportServiceImpl.class);

    private final ProjectService projectService;
    private final BackgroundTaskManager taskManager;
    private final ProjectCache projectCache;
    private final AtomicReference<PortfolioReport> cachedReport = new AtomicReference<>();
    private final ScheduledFuture<?> refresher;

    public ReportServiceImpl(ProjectService projectService) {
        this(projectService, ProjectCache.getInstance(), BackgroundTaskManager.getInstance());
    }

    public ReportServiceImpl(ProjectService projectService, ProjectCache projectCache,
            BackgroundTaskManager taskManager) {
        this.projectService = Objects.requireNonNull(projectService, "projectService");
        this.projectCache = Objects.requireNonNull(projectCache, "projectCache");
        this.taskManager = Objects.requireNonNull(taskManager, "taskManager");
        // Run a lightweight timer so dashboards and demos always have a warm report ready to display.
        this.refresher = this.taskManager.getScheduledPool().scheduleAtFixedRate(() -> {
            try {
                List<Long> projectIds = new ArrayList<>(this.projectCache.snapshot().keySet());
                if (!projectIds.isEmpty()) {
                    cachedReport.set(generatePortfolioReportParallel(projectIds));
                    LOGGER.debug("Refreshed cached report for {} projects", projectIds.size());
                }
            } catch (Exception ex) {
                LOGGER.warn("Scheduled portfolio refresh failed", ex);
            }
        }, Duration.ofMinutes(1).toMillis(), Duration.ofMinutes(1).toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    @Override
    public PortfolioReport generatePortfolioReportParallel(List<Long> projectIds) {
        Objects.requireNonNull(projectIds, "projectIds");
        if (projectIds.isEmpty()) {
            return new PortfolioReport(Collections.emptyList(), 0, 0, Instant.now());
        }
        long start = System.nanoTime();
        List<CompletableFuture<ProjectSummary>> futures = projectIds.stream()
                .map(id -> taskManager.supplyAsync(() -> projectService.getProjectSummary(id)))
                .toList();
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        List<ProjectSummary> summaries = futures.stream().map(CompletableFuture::join).toList();
        PortfolioReport report = buildReport(summaries);
        LOGGER.info("Parallel report for {} projects generated in {} ms", projectIds.size(),
                (System.nanoTime() - start) / 1_000_000); // NOSONAR
        cachedReport.set(report);
        return report;
    }

    @Override
    public PortfolioReport generatePortfolioReportSequential(List<Long> projectIds) {
        Objects.requireNonNull(projectIds, "projectIds");
        if (projectIds.isEmpty()) {
            return new PortfolioReport(Collections.emptyList(), 0, 0, Instant.now());
        }
        long start = System.nanoTime();
        List<ProjectSummary> summaries = projectIds.stream()
                .map(projectService::getProjectSummary)
                .collect(Collectors.toCollection(ArrayList::new));
        PortfolioReport report = buildReport(summaries);
        LOGGER.info("Sequential report for {} projects generated in {} ms", projectIds.size(),
                (System.nanoTime() - start) / 1_000_000); // NOSONAR
        cachedReport.set(report);
        return report;
    }

    public PortfolioReport getCachedReport() {
        return cachedReport.get();
    }

    private PortfolioReport buildReport(List<ProjectSummary> summaries) {
        double totalPlanned = summaries.stream().mapToDouble(ProjectSummary::getPlannedBudget).sum();
        double totalUsed = summaries.stream().mapToDouble(ProjectSummary::getUsedBudget).sum();
        return new PortfolioReport(summaries, totalPlanned, totalUsed, Instant.now());
    }

    @Override
    public void close() {
        refresher.cancel(true);
    }
}
