package com.builder.portfolio.controller;

import com.builder.portfolio.model.Project;
import com.builder.portfolio.model.User;
import com.builder.portfolio.service.ProjectService;
import com.builder.portfolio.service.UserService;

import java.util.List;
import java.util.Scanner;

public class AdminController {
    private final UserService userService;
    private final ProjectService projectService;

    public AdminController(UserService userService, ProjectService projectService) {
        this.userService = userService;
        this.projectService = projectService;
    }

    public void showMenu() {
        Scanner scanner = new Scanner(System.in);
        boolean exit = false;

        while (!exit) {
            System.out.println("\n--- Admin Menu ---");
            System.out.println("1. Add User");
            System.out.println("2. List Users");
            System.out.println("3. Delete User");
            System.out.println("4. List All Projects");
            System.out.println("5. Logout");
            System.out.print("Choose an option: ");

            int choice = Integer.parseInt(scanner.nextLine());

            switch (choice) {
                case 1 -> addUser(scanner);
                case 2 -> listUsers();
                case 3 -> deleteUser(scanner);
                case 4 -> listAllProjects();
                case 5 -> exit = true;
                default -> System.out.println("Invalid option. Try again.");
            }
        }
    }

    private void addUser(Scanner scanner) {
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

    private void deleteUser(Scanner scanner) {
        System.out.print("Enter user ID to delete: ");
        int id = Integer.parseInt(scanner.nextLine());
        userService.deleteUser(id);
        System.out.println("User deleted if existed.");
    }

    private void listAllProjects() {
        List<Project> projects = projectService.listAllProjects();
        if (projects.isEmpty()) {
            System.out.println("No projects available.");
            return;
        }

        System.out.println("--- Projects ---");
        for (Project project : projects) {
            System.out.println(project.getId() + ": " + project.getName() + " | " + project.getStatus());
        }
    }
}
