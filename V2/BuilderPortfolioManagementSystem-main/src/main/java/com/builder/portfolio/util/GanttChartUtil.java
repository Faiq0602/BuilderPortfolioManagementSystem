package com.builder.portfolio.util;

import com.builder.portfolio.model.Project;

public final class GanttChartUtil {
    private GanttChartUtil() {
    }

    // Mock GanttChart to Display

    public static void printSimpleGantt(Project project) {
        System.out.println("Timeline for project: " + project.getName());
        System.out.println("Design     |#######........|");
        System.out.println("Permits    |....####.......|");
        System.out.println("Build      |.......########|");
        System.out.println("Testing    |..........#####|");
    }
}
