package com.builder.portfolio.model;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Aggregated output used by the portfolio report.
 */
public final class PortfolioReport {
    private final List<ProjectSummary> summaries;
    private final double totalPlanned;
    private final double totalUsed;
    private final Instant generatedAt;

    public PortfolioReport(List<ProjectSummary> summaries, double totalPlanned, double totalUsed, Instant generatedAt) {
        this.summaries = Collections.unmodifiableList(List.copyOf(summaries));
        this.totalPlanned = totalPlanned;
        this.totalUsed = totalUsed;
        this.generatedAt = Objects.requireNonNull(generatedAt, "generatedAt");
    }

    public List<ProjectSummary> getSummaries() {
        return summaries;
    }

    public double getTotalPlanned() {
        return totalPlanned;
    }

    public double getTotalUsed() {
        return totalUsed;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }
}
