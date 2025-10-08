package com.builder.portfolio.dao;

import com.builder.portfolio.model.Document;
import com.builder.portfolio.util.DBConnectionUtil;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DocumentDAOImpl implements DocumentDAO {
    private static final Logger LOGGER = Logger.getLogger(DocumentDAOImpl.class.getName());

    private static final String INSERT_SQL = "INSERT INTO documents (project_id, document_name, document_type, uploaded_by, upload_date) VALUES (?, ?, ?, ?, ?)";
    private static final String SELECT_BY_PROJECT_SQL = "SELECT * FROM documents WHERE project_id = ? ORDER BY id";

    @Override
    public void addDocument(Document document) {
        try (Connection connection = DBConnectionUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
            statement.setInt(1, document.getProjectId());
            statement.setString(2, document.getDocumentName());
            statement.setString(3, document.getDocumentType());
            statement.setInt(4, document.getUploadedBy());
            if (document.getUploadDate() != null) {
                statement.setDate(5, Date.valueOf(document.getUploadDate()));
            } else {
                statement.setNull(5, java.sql.Types.DATE);
            }
            statement.executeUpdate();
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error adding document", ex);
        }
    }

    @Override
    public List<Document> findDocumentsByProject(int projectId) {
        List<Document> documents = new ArrayList<>();
        try (Connection connection = DBConnectionUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_PROJECT_SQL)) {
            statement.setInt(1, projectId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    documents.add(mapRowToDocument(resultSet));
                }
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error loading documents", ex);
        }
        return documents;
    }

    private Document mapRowToDocument(ResultSet resultSet) throws SQLException {
        Document document = new Document();
        document.setId(resultSet.getInt("id"));
        document.setProjectId(resultSet.getInt("project_id"));
        document.setDocumentName(resultSet.getString("document_name"));
        document.setDocumentType(resultSet.getString("document_type"));
        document.setUploadedBy(resultSet.getInt("uploaded_by"));
        Date uploadDate = resultSet.getDate("upload_date");
        if (uploadDate != null) {
            document.setUploadDate(uploadDate.toLocalDate());
        }
        return document;
    }
}
