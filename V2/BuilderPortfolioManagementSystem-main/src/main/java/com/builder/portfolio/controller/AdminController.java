package com.builder.portfolio.controller;

import com.builder.portfolio.model.User;
import com.builder.portfolio.service.ProjectService;
import com.builder.portfolio.service.UserService;
import com.builder.portfolio.util.ConsoleInput;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

// Handles administrative workflows such as user management and portfolio oversight.
// The controller relies on ConsoleInput for interactions so that all menus share the same input rules.

public class AdminController {
    private static final Logger LOGGER = Logger.getLogger(AdminController.class.getName());

    private final UserService userService;
    private final ProjectService projectService;

    public AdminController(UserService userService, ProjectService projectService) {
        this.userService = userService;
        this.projectService = projectService;
    }

    public void showMenu() {
        boolean exit = false;

        while (!exit) {
            System.out.println("\n--- Admin Menu ---");
            System.out.println("1. Add User");
            System.out.println("2. List Users");
            System.out.println("3. Delete User");
            System.out.println("4. List All Projects");
            System.out.println("5. Logout");

            int choice = ConsoleInput.readInt("Choose an option: ");

            switch (choice) {
                case 1 -> addUser();
                case 2 -> listUsers();
                case 3 -> deleteUser();
                case 4 -> listAllProjects();
                case 5 -> exit = true;
                default -> System.out.println("Invalid option. Try again.");
            }
        }
    }

    private void addUser() {
        String name = ConsoleInput.readLine("Name: ");
        String email = ConsoleInput.readLine("Email: ");
        String password = ConsoleInput.readPassword("Password: ");
        String role = ConsoleInput.readLine("Role (ADMIN/BUILDER/CLIENT): ").toUpperCase();

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(password);
        user.setRole(role);

        userService.registerUser(user);
        LOGGER.log(Level.INFO, "Admin added user with email {0}", email);
        System.out.println("User added successfully.");
    }

    private void listUsers() {
        List<User> users = userService.listUsers();
        if (users.isEmpty()) {
            System.out.println("No users found.");
            return;
        }

        System.out.println("--- Users ---");
        for (User user : users) {
            System.out.println(user.getId() + ": " + user.getName() + " | " + user.getEmail() + " | " + user.getRole());
        }
    }

    private void deleteUser() {
        int id = ConsoleInput.readInt("Enter user ID to delete: ");
        userService.deleteUser(id);
        LOGGER.log(Level.INFO, "Admin requested deletion for user id {0}", id);
        System.out.println("User deleted if existed.");
    }

    private void listAllProjects() {
        var projects = projectService.listAllProjects();
        if (projects.isEmpty()) {
            System.out.println("No projects available.");
            return;
        }

        projects.forEach(project ->
                System.out.println(project.getId() + ": " + project.getName() + " | " + project.getStatus()));
    }
}
