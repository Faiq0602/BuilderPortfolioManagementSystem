package com.builder.portfolio.util;

import com.builder.portfolio.model.Project;
import com.builder.portfolio.model.ProjectSummary;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Thread safe cache of lightweight project summaries.
 */
public final class ProjectCache {
    private static final ProjectCache INSTANCE = new ProjectCache();

    private final ConcurrentHashMap<Long, ProjectSummary> summaryCache = new ConcurrentHashMap<>();

    private ProjectCache() {
    }

    public static ProjectCache getInstance() {
        return INSTANCE;
    }

    public ProjectSummary get(long projectId) {
        return summaryCache.get(projectId);
    }

    public ProjectSummary getOrCompute(long projectId, Function<Long, ProjectSummary> computer) {
        return summaryCache.computeIfAbsent(projectId, computer);
    }

    public void evict(long projectId) {
        summaryCache.remove(projectId);
    }

    public void update(Project project) {
        // Capture a fresh summary whenever the service mutates a project so parallel reports stay fresh.
        summaryCache.compute((long) project.getId(), (id, current) ->
                ProjectSummary.builder()
                        .projectId(id)
                        .name(project.getName())
                        .status(project.getStatus())
                        .plannedBudget(project.getBudgetPlanned())
                        .usedBudget(project.getBudgetUsed())
                        .version(project.getVersion())
                        .capturedAt(Instant.now())
                        .build());
    }

    public Map<Long, ProjectSummary> snapshot() {
        return Collections.unmodifiableMap(new ConcurrentHashMap<>(summaryCache));
    }
}
