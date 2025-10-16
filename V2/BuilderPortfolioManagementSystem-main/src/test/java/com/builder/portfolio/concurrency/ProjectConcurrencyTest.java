package com.builder.portfolio.concurrency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.builder.portfolio.model.Project;
import com.builder.portfolio.service.ProjectService;
import com.builder.portfolio.service.ProjectServiceImpl;
import com.builder.portfolio.support.InMemoryDocumentDAO;
import com.builder.portfolio.support.InMemoryProjectDAO;
import java.util.List;
import java.util.ConcurrentModificationException;


import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class ProjectConcurrencyTest {
    private static final long PROJECT_ID = 1L;
    private static final int PARTICIPANTS = 10;

    private final InMemoryProjectDAO projectDAO = new InMemoryProjectDAO();
    private final InMemoryDocumentDAO documentDAO = new InMemoryDocumentDAO();
    private ProjectService projectService;

    @BeforeEach
    void setUp() {
        projectService = new ProjectServiceImpl(projectDAO, documentDAO);
        Project project = new Project();
        project.setId((int) PROJECT_ID);
        project.setName("Skyline Tower");
        project.setDescription("Mixed use high rise");
        project.setStatus("PLANNED");
        project.setBuilderId(42);
        project.setClientId(84);
        project.setBudgetPlanned(500_000);
        project.setBudgetUsed(0);
        project.setVersion(0);
        projectDAO.addProject(project);

        Project second = new Project();
        second.setId((int) (PROJECT_ID + 1));
        second.setName("Harbor Bridge");
        second.setDescription("Bridge expansion");
        second.setStatus("PLANNED");
        second.setBuilderId(42);
        second.setClientId(85);
        second.setBudgetPlanned(120_000);
        second.setBudgetUsed(0);
        second.setVersion(0);
        projectDAO.addProject(second);
    }

    @AfterEach
    void tearDown() {
        projectDAO.deleteProject((int) PROJECT_ID, 42);
        projectDAO.deleteProject((int) (PROJECT_ID + 1), 42);
    }

    @Test
    void budgetUpdatesAreAtomic() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(PARTICIPANTS);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(PARTICIPANTS);
        AtomicInteger retries = new AtomicInteger();

        for (int i = 0; i < PARTICIPANTS; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    boolean updated = false;
                    int attempts = 0;
                    while (!updated && attempts < 20) {
                        attempts++;
                        Project current = projectService.getProject((int) PROJECT_ID);
                        try {
                            projectService.updateProjectBudget(PROJECT_ID, 100, current.getVersion());
                            updated = true;
                        } catch (ConcurrentModificationException ex) {
                            retries.incrementAndGet();
                        }
                    }
                    assertTrue(updated, "budget update should eventually succeed");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS), "all workers completed");
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "executor shutdown");

        Project finalProject = projectService.getProject((int) PROJECT_ID);
        assertEquals(1_000, finalProject.getBudgetUsed(), 0.001);
        assertEquals(PARTICIPANTS, finalProject.getVersion());
        assertTrue(retries.get() > 0, "at least one retry demonstrates contention");
    }

    @Test
    void statusUpdatesRespectVersioning() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        List<String> lifecycle = List.of("PLANNED", "IN_PROGRESS", "COMPLETED");

        for (int i = 0; i < 2; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (String status : lifecycle) {
                        boolean updated = false;
                        while (!updated) {
                            Project current = projectService.getProject((int) PROJECT_ID);
                            try {
                                projectService.updateProjectStatus(PROJECT_ID, status, current.getVersion());
                                updated = true;
                            } catch (ConcurrentModificationException ignored) {
                                // retry until success
                            }
                        }
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS), "status writers finished");
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "status executor shutdown");

        Project result = projectService.getProject((int) PROJECT_ID);
        assertTrue(lifecycle.contains(result.getStatus()));
        assertEquals(2 * lifecycle.size(), result.getVersion());
    }

    @Test
    @Disabled("Demonstrates potential deadlock when lock ordering is violated")
    void deadlockDemo() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);
        Runnable taskA = () -> {
            try {
                projectService.updateProjectBudget(PROJECT_ID, 10, 0);
                projectService.updateProjectBudget(PROJECT_ID + 1, 10, 0);
            } catch (ConcurrentModificationException ignored) {
            } finally {
                latch.countDown();
            }
        };
        executor.submit(taskA);
        executor.submit(taskA);
        latch.await(5, TimeUnit.SECONDS);
        executor.shutdownNow();
    }
}
