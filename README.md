# Builder Portfolio Management System

A modular Java application for managing construction projects across roles—Admin, Builder, and Client.  
It centralizes projects, budgets, timelines, and document metadata using a clean, layered architecture (MVC + Service + DAO) with PostgreSQL.

---

## Table of Contents
1. [Overview](#overview)  
2. [Features](#features)  
3. [Architecture](#architecture)  
4. [Tech Stack](#tech-stack)  
5. [Project Structure](#project-structure)  
6. [Database Schema](#database-schema)  
7. [Setup](#setup)  
8. [Configuration](#configuration)  
9. [Run](#run)  
10. [Usage by Role](#usage-by-role)  
11. [Diagrams](#diagrams)  
12. [Notes & Limitations](#notes--limitations)  
13. [Roadmap](#roadmap)  
14. [Contributing](#contributing)  
15. [License](#license)

---

## Overview
The Builder Portfolio Management System (BPMS) helps construction teams track projects from initiation through completion.  
It provides role-based menus, clearly separated layers, and realistic workflows: project creation, status updates, portfolio viewing, budget reporting, and mock document uploads (metadata only).

**Repository:**  
`https://github.com/Faiq0602/BuilderPortfolioManagementSystem`

---

## Features
- Role-based access: Admin, Builder, Client
- Project lifecycle: add, update, delete, list
- Portfolio views: builder-specific, client-specific, and admin-wide
- Budget reporting: planned vs. used, variance, health classification
- Timeline visualization: simple text-based Gantt on progress/complete
- Document management: mock “file upload” by storing document metadata
- Clean layering for maintainability and testing

---

## Architecture
Pattern: MVC (controllers) + Service (business logic) + DAO (persistence) + Model (entities/DTOs) + Util.

- **Controllers**: console menus and input handling  
  `AdminController`, `BuilderController`, `ClientController`
- **Services**: validation, orchestration, defaults (e.g., status)  
  `UserServiceImpl`, `ProjectServiceImpl`, `DocumentServiceImpl`
- **DAOs**: JDBC CRUD, isolated SQL via `DBConnectionUtil`  
  `UserDAOImpl`, `ProjectDAOImpl`, `DocumentDAOImpl`
- **Models/DTOs**: `User`, `Project`, `Document`, `BudgetReport`
- **Utilities**: `DBConnectionUtil`, `BudgetUtil`, `StatusConstants`, `GanttChartUtil`

---

## Tech Stack
- Java 17  
- PostgreSQL  
- JDBC (via `DBConnectionUtil`)  
- Logging: `java.util.logging`  
- Build/IDE: IntelliJ (Maven optional)

---

## Project Structure
```
src/
  main/
    java/
      com/builder/portfolio/
        Main.java
        controller/
          AdminController.java
          BuilderController.java
          ClientController.java
        service/
          UserService.java
          UserServiceImpl.java
          ProjectService.java
          ProjectServiceImpl.java
          DocumentService.java
          DocumentServiceImpl.java
        dao/
          UserDAO.java
          UserDAOImpl.java
          ProjectDAO.java
          ProjectDAOImpl.java
          DocumentDAO.java
          DocumentDAOImpl.java
        model/
          User.java
          Project.java
          Document.java
          BudgetReport.java
        util/
          DBConnectionUtil.java
          BudgetUtil.java
          StatusConstants.java
          GanttChartUtil.java
```

---

## Database Schema
Run on a local PostgreSQL instance:

```sql
CREATE DATABASE builder_portfolio_db;
\\c builder_portfolio_db;

CREATE TABLE users (
  id SERIAL PRIMARY KEY,
  name VARCHAR(100),
  email VARCHAR(100) UNIQUE,
  password VARCHAR(100),
  role VARCHAR(20)
);

CREATE TABLE projects (
  id SERIAL PRIMARY KEY,
  name VARCHAR(100),
  description TEXT,
  status VARCHAR(20),
  builder_id INT REFERENCES users(id),
  client_id INT REFERENCES users(id),
  budget_planned DOUBLE PRECISION,
  budget_used DOUBLE PRECISION,
  start_date DATE,
  end_date DATE
);

CREATE TABLE documents (
  id SERIAL PRIMARY KEY,
  project_id INT REFERENCES projects(id),
  document_name VARCHAR(100),
  document_type VARCHAR(50),
  uploaded_by INT REFERENCES users(id),
  upload_date DATE
);
```

Relationships:
- User (builder) → Projects (1:N)  
- User (client) → Projects (1:N)  
- Project → Documents (1:N)

---

## Setup

### Prerequisites
- Java 17+
- PostgreSQL running locally
- IntelliJ IDEA (recommended)

### Create Database
1. Create DB and tables using the SQL above.
2. Verify `builder_portfolio_db` is accessible.

---

## Configuration
Configure DB connection (either via a `config.properties` read by `DBConnectionUtil`, or edit constants in `DBConnectionUtil`):

```
jdbc:postgresql://localhost:5432/builder_portfolio_db
username: postgres
password: your_password
```

---

## Run

### IntelliJ
1. Open the project.
2. Ensure DB is created and schema applied.
3. Run `com.builder.portfolio.Main`.

### Command Line (if using Maven for packaging)
```
mvn -q -DskipTests package
java -cp target/<your-jar>.jar com.builder.portfolio.Main
```

---

## Usage by Role

### Admin
- Register users
- List users
- Delete users
- View all projects

### Builder
- Add project (status defaults to UPCOMING if omitted)
- Update project (status, budget, dates)
- Delete own project
- View own projects (portfolio)
- Add document metadata (mock upload)
- View budget report (variance and health)
- View simple Gantt when status is IN_PROGRESS or COMPLETED

### Client
- View assigned projects

---

## Diagrams
This repository is accompanied by:
- ER Diagram – database relationships
- Class Diagram – layered architecture
- Sequence Diagrams – Add Project, Update Status, View Portfolio
- Workflow Diagrams – Login/Register, Project Management, Budget & Timeline Tracking

If you keep PlantUML sources in the repo, they can be rendered with any PlantUML-compatible tool. For submissions, exported images are placed into `UMLDiagrams.pdf`.

---

## Notes & Limitations
- Passwords are stored in plaintext for demonstration only (use BCrypt/Argon2 in production).
- Document “uploads” are mocked: only metadata is stored, not actual files.
- Console-based UI is intentionally minimal to focus on architecture and workflows.

---

## Roadmap
- Password hashing and stricter role-based authorization
- REST API (Spring Boot) and a web UI (React or Thymeleaf)
- Real file uploads with storage integration and permissions
- Rich Gantt visualization and milestones
- Tests (JUnit + Mockito) and CI workflow

---

## Contributing
Contributions are welcome:
1. Fork the repository
2. Create a feature branch
3. Commit changes with clear messages
4. Open a pull request describing the change and rationale

---

