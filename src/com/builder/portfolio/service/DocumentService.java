package com.builder.portfolio.service;

import com.builder.portfolio.model.Document;
import java.util.List;

public interface DocumentService {
    void addDocument(Document document);

    List<Document> listDocumentsByProject(int projectId);
}
