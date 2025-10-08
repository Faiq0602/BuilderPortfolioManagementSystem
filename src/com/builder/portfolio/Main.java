package com.builder.portfolio;

import com.builder.portfolio.controller.AdminController;
import com.builder.portfolio.controller.BuilderController;
import com.builder.portfolio.controller.ClientController;
import com.builder.portfolio.model.User;
import com.builder.portfolio.service.*;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        UserService userService = new UserServiceImpl();
        ProjectService projectService = new ProjectServiceImpl();
        DocumentService documentService = new DocumentServiceImpl();
        Scanner scanner = new Scanner(System.in);

        boolean exit = false;
        while (!exit) {
            System.out.println("\n=== Builder Portfolio Management System ===");
            System.out.println("1. Register");
            System.out.println("2. Login");
            System.out.println("3. Exit");
            System.out.print("Choose an option: ");
            int choice = Integer.parseInt(scanner.nextLine());

            switch (choice) {
                case 1 -> {
                    System.out.println("--- Register User ---");
                    System.out.print("Name: ");
                    String name = scanner.nextLine();
                    System.out.print("Email: ");
                    String email = scanner.nextLine();
                    System.out.print("Password: ");
                    String password = scanner.nextLine();
                    System.out.print("Role (ADMIN/BUILDER/CLIENT): ");
                    String role = scanner.nextLine().toUpperCase();

                    User user = new User();
                    user.setName(name);
                    user.setEmail(email);
                    user.setPassword(password);
                    user.setRole(role);
                    userService.registerUser(user);
                    System.out.println("Registration successful.");
                }
                case 2 -> {
                    System.out.println("--- Login ---");
                    System.out.print("Email: ");
                    String email = scanner.nextLine();
                    System.out.print("Password: ");
                    String password = scanner.nextLine();

                    User user = userService.login(email, password);
                    if (user == null) {
                        System.out.println("Invalid credentials.");
                        break;
                    }

                    System.out.println("Welcome, " + user.getName() + " (" + user.getRole() + ")");
                    switch (user.getRole()) {
                        case "ADMIN" -> new AdminController(userService, projectService).showMenu();
                        case "BUILDER" -> {
                            new BuilderController(projectService, documentService, user.getId()).showMenu();
                        }
                        case "CLIENT" -> new ClientController(projectService, user.getId()).showMenu();
                        default -> System.out.println("Unknown role.");
                    }
                }
                case 3 -> exit = true;
                default -> System.out.println("Invalid option. Try again.");
            }
        }

        System.out.println("Goodbye!");
        scanner.close();
    }
}
