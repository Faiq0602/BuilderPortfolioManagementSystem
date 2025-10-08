package com.builder.portfolio.dao;

import com.builder.portfolio.model.Project;
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

public class ProjectDAOImpl implements ProjectDAO {
    private static final Logger LOGGER = Logger.getLogger(ProjectDAOImpl.class.getName());

    private static final String INSERT_SQL = "INSERT INTO projects (name, description, status, builder_id, client_id, budget_planned, budget_used, start_date, end_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_SQL = "UPDATE projects SET name = ?, description = ?, status = ?, client_id = ?, budget_planned = ?, budget_used = ?, start_date = ?, end_date = ? WHERE id = ? AND builder_id = ?";
    private static final String DELETE_SQL = "DELETE FROM projects WHERE id = ? AND builder_id = ?";
    private static final String SELECT_BY_BUILDER_SQL = "SELECT * FROM projects WHERE builder_id = ? ORDER BY id";
    private static final String SELECT_BY_CLIENT_SQL = "SELECT * FROM projects WHERE client_id = ? ORDER BY id";
    private static final String SELECT_ALL_SQL = "SELECT * FROM projects ORDER BY id";
    private static final String SELECT_BY_ID_SQL = "SELECT * FROM projects WHERE id = ?";

    @Override
    public void addProject(Project project) {
        try (Connection connection = DBConnectionUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
            statement.setString(1, project.getName());
            statement.setString(2, project.getDescription());
            statement.setString(3, project.getStatus());
            statement.setInt(4, project.getBuilderId());
            statement.setInt(5, project.getClientId());
            statement.setDouble(6, project.getBudgetPlanned());
            statement.setDouble(7, project.getBudgetUsed());
            if (project.getStartDate() != null) {
                statement.setDate(8, Date.valueOf(project.getStartDate()));
            } else {
                statement.setNull(8, java.sql.Types.DATE);
            }
            if (project.getEndDate() != null) {
                statement.setDate(9, Date.valueOf(project.getEndDate()));
            } else {
                statement.setNull(9, java.sql.Types.DATE);
            }
            statement.executeUpdate();
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error adding project", ex);
        }
    }

    @Override
    public void updateProject(Project project) {
        try (Connection connection = DBConnectionUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
            statement.setString(1, project.getName());
            statement.setString(2, project.getDescription());
            statement.setString(3, project.getStatus());
            statement.setInt(4, project.getClientId());
            statement.setDouble(5, project.getBudgetPlanned());
            statement.setDouble(6, project.getBudgetUsed());
            if (project.getStartDate() != null) {
                statement.setDate(7, Date.valueOf(project.getStartDate()));
            } else {
                statement.setNull(7, java.sql.Types.DATE);
            }
            if (project.getEndDate() != null) {
                statement.setDate(8, Date.valueOf(project.getEndDate()));
            } else {
                statement.setNull(8, java.sql.Types.DATE);
            }
            statement.setInt(9, project.getId());
            statement.setInt(10, project.getBuilderId());
            statement.executeUpdate();
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error updating project", ex);
        }
    }

    @Override
    public void deleteProject(int projectId, int builderId) {
        try (Connection connection = DBConnectionUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {
            statement.setInt(1, projectId);
            statement.setInt(2, builderId);
            statement.executeUpdate();
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error deleting project", ex);
        }
    }

    @Override
    public List<Project> findProjectsByBuilder(int builderId) {
        return findProjectsByParameter(builderId, SELECT_BY_BUILDER_SQL);
    }

    @Override
    public List<Project> findProjectsByClient(int clientId) {
        return findProjectsByParameter(clientId, SELECT_BY_CLIENT_SQL);
    }

    @Override
    public List<Project> findAllProjects() {
        List<Project> projects = new ArrayList<>();
        try (Connection connection = DBConnectionUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_ALL_SQL);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                projects.add(mapRowToProject(resultSet));
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error listing projects", ex);
        }
        return projects;
    }

    @Override
    public Project findById(int projectId) {
        try (Connection connection = DBConnectionUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_ID_SQL)) {
            statement.setInt(1, projectId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRowToProject(resultSet);
                }
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error finding project", ex);
        }
        return null;
    }

    private List<Project> findProjectsByParameter(int id, String query) {
        List<Project> projects = new ArrayList<>();
        try (Connection connection = DBConnectionUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    projects.add(mapRowToProject(resultSet));
                }
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error loading projects", ex);
        }
        return projects;
    }

    private Project mapRowToProject(ResultSet resultSet) throws SQLException {
        Project project = new Project();
        project.setId(resultSet.getInt("id"));
        project.setName(resultSet.getString("name"));
        project.setDescription(resultSet.getString("description"));
        project.setStatus(resultSet.getString("status"));
        project.setBuilderId(resultSet.getInt("builder_id"));
        project.setClientId(resultSet.getInt("client_id"));
        project.setBudgetPlanned(resultSet.getDouble("budget_planned"));
        project.setBudgetUsed(resultSet.getDouble("budget_used"));
        Date startDate = resultSet.getDate("start_date");
        Date endDate = resultSet.getDate("end_date");
        if (startDate != null) {
            project.setStartDate(startDate.toLocalDate());
        }
        if (endDate != null) {
            project.setEndDate(endDate.toLocalDate());
        }
        return project;
    }
}
