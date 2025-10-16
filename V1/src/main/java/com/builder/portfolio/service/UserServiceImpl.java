package com.builder.portfolio.service;

import com.builder.portfolio.dao.UserDAO;
import com.builder.portfolio.dao.UserDAOImpl;
import com.builder.portfolio.model.User;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


 // Concrete implementation of UserService delegating persistence work to UserDAO

public class UserServiceImpl implements UserService {
    private static final Logger LOGGER = Logger.getLogger(UserServiceImpl.class.getName());

    private final UserDAO userDAO;

    public UserServiceImpl() {
        this.userDAO = new UserDAOImpl();
    }

    public UserServiceImpl(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    @Override
    public void registerUser(User user) {
        LOGGER.log(Level.FINE, "Registering user {0}", user.getEmail());
        userDAO.addUser(user);
    }

    @Override
    public User login(String email, String password) {
        LOGGER.log(Level.FINE, "Attempting login for {0}", email);
        return userDAO.findByEmailAndPassword(email, password);
    }

    @Override
    public List<User> listUsers() {
        return userDAO.findAll();
    }

    @Override
    public void deleteUser(int userId) {
        LOGGER.log(Level.FINE, "Deleting user id {0}", userId);
        userDAO.deleteUser(userId);
    }

    @Override
    public User getUser(int userId) {
        return userDAO.findById(userId);
    }
}
