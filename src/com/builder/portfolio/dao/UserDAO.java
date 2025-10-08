package com.builder.portfolio.dao;

import com.builder.portfolio.model.User;
import java.util.List;

public interface UserDAO {
    void addUser(User user);

    User findByEmailAndPassword(String email, String password);

    List<User> findAll();

    void deleteUser(int userId);

    User findById(int userId);
}
