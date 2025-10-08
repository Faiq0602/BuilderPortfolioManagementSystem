package com.builder.portfolio.service;

import com.builder.portfolio.model.User;
import java.util.List;

public interface UserService {
    void registerUser(User user);

    User login(String email, String password);

    List<User> listUsers();

    void deleteUser(int userId);

    User getUser(int userId);
}
