package com.builder.portfolio.dao;

import com.builder.portfolio.model.User;
import com.builder.portfolio.util.DBConnectionUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserDAOImpl implements UserDAO {
    private static final Logger LOGGER = Logger.getLogger(UserDAOImpl.class.getName());

    private static final String INSERT_SQL = "INSERT INTO users (name, email, password, role) VALUES (?, ?, ?, ?)";
    private static final String SELECT_LOGIN_SQL = "SELECT id, name, email, password, role FROM users WHERE email = ? AND password = ?";
    private static final String SELECT_ALL_SQL = "SELECT id, name, email, password, role FROM users ORDER BY id";
    private static final String DELETE_SQL = "DELETE FROM users WHERE id = ?";
    private static final String SELECT_BY_ID_SQL = "SELECT id, name, email, password, role FROM users WHERE id = ?";

    @Override
    public void addUser(User user) {
        try (Connection connection = DBConnectionUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
            statement.setString(1, user.getName());
            statement.setString(2, user.getEmail());
            statement.setString(3, user.getPassword());
            statement.setString(4, user.getRole());
            statement.executeUpdate();
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error adding user", ex);
        }
    }

    @Override
    public User findByEmailAndPassword(String email, String password) {
        try (Connection connection = DBConnectionUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_LOGIN_SQL)) {
            statement.setString(1, email);
            statement.setString(2, password);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRowToUser(resultSet);
                }
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error finding user", ex);
        }
        return null;
    }

    @Override
    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        try (Connection connection = DBConnectionUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_ALL_SQL);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                users.add(mapRowToUser(resultSet));
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error listing users", ex);
        }
        return users;
    }

    @Override
    public void deleteUser(int userId) {
        try (Connection connection = DBConnectionUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {
            statement.setInt(1, userId);
            statement.executeUpdate();
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error deleting user", ex);
        }
    }

    @Override
    public User findById(int userId) {
        try (Connection connection = DBConnectionUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_ID_SQL)) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRowToUser(resultSet);
                }
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error finding user by id", ex);
        }
        return null;
    }

    private User mapRowToUser(ResultSet resultSet) throws SQLException {
        User user = new User();
        user.setId(resultSet.getInt("id"));
        user.setName(resultSet.getString("name"));
        user.setEmail(resultSet.getString("email"));
        user.setPassword(resultSet.getString("password"));
        user.setRole(resultSet.getString("role"));
        return user;
    }
}
