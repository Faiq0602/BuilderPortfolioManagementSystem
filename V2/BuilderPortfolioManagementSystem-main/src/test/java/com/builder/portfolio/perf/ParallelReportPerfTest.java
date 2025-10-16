package com.builder.portfolio.perf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.builder.portfolio.model.PortfolioReport;
import com.builder.portfolio.model.Project;
import com.builder.portfolio.service.ProjectService;
import com.builder.portfolio.service.ProjectServiceImpl;
import com.builder.portfolio.service.ReportService;
import com.builder.portfolio.service.impl.ReportServiceImpl;
import com.builder.portfolio.support.InMemoryDocumentDAO;
import com.builder.portfolio.support.InMemoryProjectDAO;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ParallelReportPerfTest {
    private ProjectService projectService;
    private ReportService reportService;
    private InMemoryProjectDAO projectDAO;

    @BeforeEach
    void setUp() {
        projectDAO = new InMemoryProjectDAO();
        projectService = new ProjectServiceImpl(projectDAO, new InMemoryDocumentDAO());
        reportService = new ReportServiceImpl(projectService);

        for (int i = 0; i < 60; i++) {
            Project project = new Project();
            project.setId(i + 1);
            project.setName("Project-" + i);
            project.setDescription("Load test project " + i);
            project.setStatus("IN_PROGRESS");
            project.setBuilderId(100 + i);
            project.setClientId(200 + i);
            project.setBudgetPlanned(100_000 + i * 1_000);
            project.setBudgetUsed(50_000 + i * 500);
            project.setVersion(0);
            projectService.addProject(project);
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        reportService.close();
    }

    @Test
    void parallelReportIsNotSlowerThanSequential() {
        List<Long> projectIds = new ArrayList<>();
        projectDAO.findAllProjects().forEach(project -> projectIds.add((long) project.getId()));

        long sequentialStart = System.nanoTime();
        PortfolioReport sequential = reportService.generatePortfolioReportSequential(projectIds);
        long sequentialDuration = System.nanoTime() - sequentialStart;

        long parallelStart = System.nanoTime();
        PortfolioReport parallel = reportService.generatePortfolioReportParallel(projectIds);
        long parallelDuration = System.nanoTime() - parallelStart;

        assertEquals(projectIds.size(), sequential.getSummaries().size());
        assertEquals(sequential.getTotalPlanned(), parallel.getTotalPlanned(), 0.001);
        assertEquals(sequential.getTotalUsed(), parallel.getTotalUsed(), 0.001);

        boolean parallelFaster = parallelDuration <= sequentialDuration;
        long diff = Math.abs(parallelDuration - sequentialDuration);
        long tolerance = TimeUnit.MILLISECONDS.toNanos(15);
        assertTrue(parallelFaster || diff < tolerance,
                String.format("Parallel duration %d vs sequential %d (diff %d ns)",
                        parallelDuration, sequentialDuration, diff));
    }
}
