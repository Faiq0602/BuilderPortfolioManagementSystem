package com.builder.portfolio.util;

import com.builder.portfolio.model.Project;

public final class BudgetUtil {
    private BudgetUtil() {
    }

    public static double calculateVariance(Project project) {
        return project.getBudgetPlanned() - project.getBudgetUsed();
    }

    public static String determineBudgetHealth(Project project) {
        double variance = calculateVariance(project);
        if (variance > 0) {
            return StatusConstants.BUDGET_UNDER;
        }
        if (variance < 0) {
            return StatusConstants.BUDGET_OVER;
        }
        return StatusConstants.BUDGET_ON_TRACK;
    }
}
