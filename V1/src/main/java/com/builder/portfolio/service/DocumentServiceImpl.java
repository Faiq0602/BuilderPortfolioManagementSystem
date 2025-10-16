package com.builder.portfolio.service;

import com.builder.portfolio.dao.DocumentDAO;
import com.builder.portfolio.dao.DocumentDAOImpl;
import com.builder.portfolio.model.Document;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

// Encapsulates document related operations.
// Keeping the DAO interactions here allows for easier testing and future expansion.

public class DocumentServiceImpl implements DocumentService {
    private static final Logger LOGGER = Logger.getLogger(DocumentServiceImpl.class.getName());

    private final DocumentDAO documentDAO;

    public DocumentServiceImpl() {

        this.documentDAO = new DocumentDAOImpl();
    }

    public DocumentServiceImpl(DocumentDAO documentDAO) {

        this.documentDAO = documentDAO;
    }

    @Override
    public void addDocument(Document document) {
        LOGGER.log(Level.FINE, "Saving document metadata for project {0}", document.getProjectId());
        documentDAO.addDocument(document);
    }

    @Override
    public List<Document> listDocumentsByProject(int projectId) {
        return documentDAO.findDocumentsByProject(projectId);
    }
}
