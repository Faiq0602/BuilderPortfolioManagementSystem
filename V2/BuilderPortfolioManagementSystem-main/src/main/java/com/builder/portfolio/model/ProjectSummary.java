package com.builder.portfolio.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable snapshot of lightweight project details used for cache friendly reads and reporting.
 */
public final class ProjectSummary {
    private final long projectId;
    private final String name;
    private final String status;
    private final double plannedBudget;
    private final double usedBudget;
    private final long version;
    private final Instant capturedAt;

    private ProjectSummary(Builder builder) {
        this.projectId = builder.projectId;
        this.name = builder.name;
        this.status = builder.status;
        this.plannedBudget = builder.plannedBudget;
        this.usedBudget = builder.usedBudget;
        this.version = builder.version;
        this.capturedAt = builder.capturedAt;
    }

    public long getProjectId() {
        return projectId;
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }

    public double getPlannedBudget() {
        return plannedBudget;
    }

    public double getUsedBudget() {
        return usedBudget;
    }

    public long getVersion() {
        return version;
    }

    public Instant getCapturedAt() {
        return capturedAt;
    }

    public Builder toBuilder() {
        return builder()
                .projectId(projectId)
                .name(name)
                .status(status)
                .plannedBudget(plannedBudget)
                .usedBudget(usedBudget)
                .version(version)
                .capturedAt(capturedAt);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private long projectId;
        private String name;
        private String status;
        private double plannedBudget;
        private double usedBudget;
        private long version;
        private Instant capturedAt = Instant.now();

        private Builder() {
        }

        public Builder projectId(long projectId) {
            this.projectId = projectId;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder plannedBudget(double plannedBudget) {
            this.plannedBudget = plannedBudget;
            return this;
        }

        public Builder usedBudget(double usedBudget) {
            this.usedBudget = usedBudget;
            return this;
        }

        public Builder version(long version) {
            this.version = version;
            return this;
        }

        public Builder capturedAt(Instant capturedAt) {
            this.capturedAt = Objects.requireNonNull(capturedAt, "capturedAt");
            return this;
        }

        public ProjectSummary build() {
            return new ProjectSummary(this);
        }
    }
}
