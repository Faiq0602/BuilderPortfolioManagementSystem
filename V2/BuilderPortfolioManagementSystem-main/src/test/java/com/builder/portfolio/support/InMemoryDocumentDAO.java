package com.builder.portfolio.support;

import com.builder.portfolio.dao.DocumentDAO;
import com.builder.portfolio.model.Document;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class InMemoryDocumentDAO implements DocumentDAO {
    private final ConcurrentHashMap<Integer, CopyOnWriteArrayList<Document>> store = new ConcurrentHashMap<>();

    @Override
    public void addDocument(Document document) {
        store.computeIfAbsent(document.getProjectId(), key -> new CopyOnWriteArrayList<>())
                .add(copy(document));
    }

    @Override
    public List<Document> findDocumentsByProject(int projectId) {
        return store.getOrDefault(projectId, new CopyOnWriteArrayList<>()).stream()
                .map(this::copy)
                .collect(Collectors.toList());
    }

    private Document copy(Document original) {
        Document copy = new Document();
        copy.setId(original.getId());
        copy.setProjectId(original.getProjectId());
        copy.setDocumentName(original.getDocumentName());
        copy.setDocumentType(original.getDocumentType());
        copy.setUploadedBy(original.getUploadedBy());
        LocalDate uploadDate = original.getUploadDate();
        copy.setUploadDate(uploadDate != null ? LocalDate.from(uploadDate) : null);
        return copy;
    }
}
