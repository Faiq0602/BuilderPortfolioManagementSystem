package com.builder.portfolio.concurrency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.builder.portfolio.model.Document;
import com.builder.portfolio.model.Project;
import com.builder.portfolio.service.ProjectService;
import com.builder.portfolio.service.ProjectServiceImpl;
import com.builder.portfolio.support.InMemoryDocumentDAO;
import com.builder.portfolio.support.InMemoryProjectDAO;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DocumentUploadConcurrencyTest {
    private static final long PROJECT_ID = 10L;
    private static final int CLIENT_ID = 77;
    private static final int BUILDER_ID = 88;

    private final InMemoryProjectDAO projectDAO = new InMemoryProjectDAO();
    private final InMemoryDocumentDAO documentDAO = new InMemoryDocumentDAO();
    private ProjectService projectService;

    @BeforeEach
    void setUp() {
        projectService = new ProjectServiceImpl(projectDAO, documentDAO);
        Project project = new Project();
        project.setId((int) PROJECT_ID);
        project.setName("Client Center");
        project.setDescription("Office fit-out");
        project.setStatus("PLANNED");
        project.setBuilderId(BUILDER_ID);
        project.setClientId(CLIENT_ID);
        project.setBudgetPlanned(200_000);
        project.setBudgetUsed(50_000);
        project.setVersion(0);
        projectDAO.addProject(project);
    }

    @AfterEach
    void tearDown() {
        projectDAO.deleteProject((int) PROJECT_ID, BUILDER_ID);
    }

    @Test
    void concurrentUploadsAreStored() throws InterruptedException {
        int uploads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(uploads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(uploads);

        // Fan out identical uploads so we exercise the project-level lock under real contention.
        for (int i = 0; i < uploads; i++) {
            int finalI = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    Document document = new Document();
                    document.setDocumentName("spec-" + finalI + ".pdf");
                    document.setDocumentType("PDF");
                    document.setUploadedBy(CLIENT_ID);
                    document.setUploadDate(LocalDate.now());
                    projectService.uploadDocument(PROJECT_ID, document);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "all uploads complete");
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "upload executor shutdown");

        List<Document> documents = documentDAO.findDocumentsByProject((int) PROJECT_ID);
        assertEquals(uploads, documents.size());
        assertEquals(uploads, documents.stream().map(Document::getDocumentName).distinct().count());
    }
}
