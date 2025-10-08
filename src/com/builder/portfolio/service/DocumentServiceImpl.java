package com.builder.portfolio.service;

import com.builder.portfolio.dao.DocumentDAO;
import com.builder.portfolio.dao.DocumentDAOImpl;
import com.builder.portfolio.model.Document;
import java.util.List;

public class DocumentServiceImpl implements DocumentService {
    private final DocumentDAO documentDAO;

    public DocumentServiceImpl() {
        this.documentDAO = new DocumentDAOImpl();
    }

    public DocumentServiceImpl(DocumentDAO documentDAO) {
        this.documentDAO = documentDAO;
    }

    @Override
    public void addDocument(Document document) {
        documentDAO.addDocument(document);
    }

    @Override
    public List<Document> listDocumentsByProject(int projectId) {
        return documentDAO.findDocumentsByProject(projectId);
    }
}
