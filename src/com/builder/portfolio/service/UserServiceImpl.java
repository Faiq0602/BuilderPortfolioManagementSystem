package com.builder.portfolio.service;

import com.builder.portfolio.dao.UserDAO;
import com.builder.portfolio.dao.UserDAOImpl;
import com.builder.portfolio.model.User;
import java.util.List;

public class UserServiceImpl implements UserService {
    private final UserDAO userDAO;

    public UserServiceImpl() {
        this.userDAO = new UserDAOImpl();
    }

    public UserServiceImpl(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    @Override
    public void registerUser(User user) {
        userDAO.addUser(user);
    }

    @Override
    public User login(String email, String password) {
        return userDAO.findByEmailAndPassword(email, password);
    }

    @Override
    public List<User> listUsers() {
        return userDAO.findAll();
    }

    @Override
    public void deleteUser(int userId) {
        userDAO.deleteUser(userId);
    }

    @Override
    public User getUser(int userId) {
        return userDAO.findById(userId);
    }
}
