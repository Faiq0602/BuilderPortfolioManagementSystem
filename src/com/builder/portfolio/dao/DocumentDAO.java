package com.builder.portfolio.dao;

import com.builder.portfolio.model.Document;
import java.util.List;

public interface DocumentDAO {
    void addDocument(Document document);

    List<Document> findDocumentsByProject(int projectId);
}
