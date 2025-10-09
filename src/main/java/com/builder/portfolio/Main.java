package com.builder.portfolio;

import com.builder.portfolio.controller.AdminController;
import com.builder.portfolio.controller.BuilderController;
import com.builder.portfolio.controller.ClientController;
import com.builder.portfolio.model.User;
import com.builder.portfolio.service.DocumentService;
import com.builder.portfolio.service.DocumentServiceImpl;
import com.builder.portfolio.service.ProjectService;
import com.builder.portfolio.service.ProjectServiceImpl;
import com.builder.portfolio.service.UserService;
import com.builder.portfolio.service.UserServiceImpl;
import com.builder.portfolio.util.ConsoleInput;
import java.util.logging.Level;
import java.util.logging.Logger;

// Entry point for the Builder Portfolio Management System.
// The class wires together the service layer
// The controllers and relies on ConsoleInput for user interaction to keep responsibilities tidy

public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        UserService userService = new UserServiceImpl();
        ProjectService projectService = new ProjectServiceImpl();
        DocumentService documentService = new DocumentServiceImpl();

        boolean exit = false;
        while (!exit) {
            System.out.println("\n=== Builder Portfolio Management System ===");
            System.out.println("1. Register");
            System.out.println("2. Login");
            System.out.println("3. Exit");
            int choice = ConsoleInput.readInt("Choose an option: ");

            try {
                switch (choice) {
                    case 1 -> registerUser(userService);
                    case 2 -> login(userService, projectService, documentService);
                    case 3 -> exit = true;
                    default -> System.out.println("Invalid option. Try again.");
                }
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Unexpected error while processing menu option", ex);
                System.out.println("Something went wrong. Please try again.");
            }
        }

        System.out.println("Goodbye!");
    }

    private static void registerUser(UserService userService) {
        System.out.println("--- Register User ---");
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
        LOGGER.log(Level.INFO, "Registered user with email {0}", email);
        System.out.println("Registration successful.");
    }

    private static void login(UserService userService, ProjectService projectService,
            DocumentService documentService) {
        System.out.println("--- Login ---");
        String email = ConsoleInput.readLine("Email: ");
        String password = ConsoleInput.readPassword("Password: ");

        User user = userService.login(email, password);
        if (user == null) {
            LOGGER.log(Level.WARNING, "Failed login attempt for email {0}", email);
            System.out.println("Invalid credentials.");
            return;
        }

        LOGGER.log(Level.INFO, "User {0} logged in with role {1}", new Object[]{email, user.getRole()});
        System.out.println("Welcome, " + user.getName() + " (" + user.getRole() + ")");
        switch (user.getRole()) {
            case "ADMIN" -> new AdminController(userService, projectService).showMenu();
            case "BUILDER" -> new BuilderController(projectService, documentService, user.getId()).showMenu();
            case "CLIENT" -> new ClientController(projectService, user.getId()).showMenu();
            default -> System.out.println("Unknown role.");
        }
    }
}
