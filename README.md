# Builder Portfolio Management System (BPMS)

A modular Java application for managing construction projects across roles — **Admin**, **Builder**, and **Client**. BPMS centralizes projects, budgets, timelines, and document metadata using a layered architecture (**MVC + Service + DAO**) backed by **PostgreSQL**.

This repository contains two versions:

- **v1** — Baseline single-threaded implementation  
- **v2** — Concurrency-enabled implementation with multithreading, optimistic locking, per-project locks, background executors, caching, and parallel reporting

---

## Table of Contents

- [Overview](#overview)
- [Repository Structure](#repository-structure)
- [Choose a Version](#choose-a-version)
- [Features](#features)
  - [Common (v1 and v2)](#common-v1-and-v2)
  - [Additional in v2](#additional-in-v2)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Database Schema](#database-schema)
  - [Common schema (v1)](#common-schema-v1)
  - [Concurrency addition (v2)](#concurrency-addition-v2)
  - [Relationships](#relationships)
- [Setup](#setup)
  - [Prerequisites](#prerequisites)
  - [Database](#database)
- [Configuration](#configuration)
- [Build and Run](#build-and-run)
  - [IntelliJ IDEA](#intellij-idea)
  - [Command Line (Maven)](#command-line-maven)
- [Usage by Role](#usage-by-role)
  - [Admin](#admin)
  - [Builder](#builder)
  - [Client](#client)
- [Concurrency & Multithreading (v2)](#concurrency--multithreading-v2)
  - [Objectives](#objectives)
  - [Design](#design)
  - [Example optimistic update (SQL)](#example-optimistic-update-sql)
  - [Service-level retry sketch](#service-level-retry-sketch)
  - [Console concurrency demo](#console-concurrency-demo)
- [Testing](#testing)
- [Monitoring & Profiling](#monitoring--profiling)
- [Diagrams](#diagrams)
- [Notes & Limitations](#notes--limitations)

---

## Overview

The Builder Portfolio Management System helps construction teams track projects from initiation through completion. It provides role-based menus, clearly separated layers, and realistic workflows: project creation, status updates, portfolio viewing, budget reporting, and mock document uploads (metadata only).

- **v1** focuses on clean architecture and core CRUD workflows.  
- **v2** adds a concurrency layer for safe multi-user access and performance under parallel workloads.

---

## Repository Structure

```
BuilderPortfolioManagementSystem/
├── v1/                 # Legacy single-threaded implementation
│   └── src/...
└── v2/                 # Concurrency-enabled implementation (recommended)
    └── src/...
```

---

## Choose a Version

Use `v1` if you need a minimal, single-threaded reference or want to study the core architecture without concurrency concerns.

Use `v2` for real-world scenarios with multiple simultaneous users, safer updates, and better performance under load.

---

## Features

### Common (v1 and v2)

- Role-based access: Admin, Builder, Client
- Project lifecycle: add, update, delete, list
- Portfolio views: builder-specific, client-specific, and admin-wide
- Budget reporting: planned vs. used, variance, health classification
- Timeline visualization: text-based Gantt when status is IN_PROGRESS or COMPLETED
- Document management: mock “file upload” by storing document metadata only
- Clean layering for maintainability and testing

### Additional in v2

- Per-project read/write locks via a lock registry
- Optimistic version checks to prevent stale writes
- Thread-safe cache of project snapshots
- Background task manager with fixed and scheduled executors
- Parallel portfolio report generation
- Structured logging around lock waits, retries, and durations
- Console concurrency demo to visualize background work

---

## Architecture

Pattern: MVC (controllers) + Service (business logic) + DAO (persistence) + Model (entities/DTOs) + Util (+ Concurrency in v2)

- Controllers: console menus and input handling (AdminController, BuilderController, ClientController)
- Services: validation, orchestration, defaults (UserServiceImpl, ProjectServiceImpl, DocumentServiceImpl, ReportServiceImpl)
- DAOs: JDBC CRUD, isolated SQL via DBConnectionUtil (UserDAOImpl, ProjectDAOImpl, DocumentDAOImpl)
- Models/DTOs: User, Project, Document, BudgetReport
- Utilities: DBConnectionUtil, BudgetUtil, StatusConstants, GanttChartUtil
- Concurrency (v2): LockRegistry, BackgroundTaskManager, ProjectCache

---

## Tech Stack

- Java 17
- PostgreSQL
- JDBC (via DBConnectionUtil)
- Logging: java.util.logging (v1) / SLF4J or JUL-compatible (v2)
- IDE/Build: IntelliJ IDEA, Maven

---

## Project Structure

Example layout (v2 shown; v1 is similar without the concurrent package):

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
          ReportService.java
          ReportServiceImpl.java
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
        concurrent/                 # v2 only
          LockRegistry.java
          BackgroundTaskManager.java
          ProjectCache.java
  test/
    java/
      ... (unit and concurrency tests in v2)
```

---

## Database Schema

### Common schema (v1)

```sql
CREATE DATABASE builder_portfolio_db;
\c builder_portfolio_db;

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

### Concurrency addition (v2)

```sql
ALTER TABLE projects
  ADD COLUMN IF NOT EXISTS version INT NOT NULL DEFAULT 0;  -- for optimistic locking
```

### Relationships

- User (builder) → Projects (1:N)
- User (client) → Projects (1:N)
- Project → Documents (1:N)

---

## Setup

### Prerequisites

- Java 17+
- PostgreSQL running locally
- IntelliJ IDEA (recommended)

### Database

Create the database and tables using the SQL above. For v2, ensure the `version` column exists on `projects`.

---

## Configuration

Configure DB connection (via a `config.properties` read by `DBConnectionUtil`, or inline constants in `DBConnectionUtil`):

properties file example:

```
jdbc.url=jdbc:postgresql://localhost:5432/builder_portfolio_db
jdbc.username=postgres
jdbc.password=your_password
```

Alternatively, a minimal `config.properties` can be plain lines:

```
jdbc:postgresql://localhost:5432/builder_portfolio_db
postgres
your_password
```

---

## Build and Run

### IntelliJ IDEA

1. Open the project (select `v1/` or `v2/`) as a Maven project.
2. Ensure the database is created and schema applied.
3. Run `com.builder.portfolio.Main`.

### Command Line (Maven)

From the `v1/` or `v2/` directory:

```bash
mvn -q -DskipTests package
java -cp target/<artifact-name>.jar com.builder.portfolio.Main
```

---

## Usage by Role

### Admin

- Register users
- List users
- Delete users
- View all projects

### Builder

- Add project (status defaults to `UPCOMING` if omitted)
- Update project (status, budget, dates)
- Delete own project
- View own projects (portfolio)
- Add document metadata (mock upload)
- View budget report (variance and health)
- View simple Gantt when status is `IN_PROGRESS` or `COMPLETED`

### Client

- View assigned projects

---

## Concurrency & Multithreading (v2)

### Objectives

- Prevent lost updates and race conditions under concurrent edits.
- Improve responsiveness by offloading heavy work to background executors.
- Provide predictable, observable behavior under load.

### Design

- Per-project coordination: `LockRegistry` supplies a `ReadWriteLock` per project. Readers use read lock; mutating actions acquire the write lock.
- Optimistic versioning: DAO updates guard on `WHERE id=? AND version=?` and increment version on success; service retries on conflict.
- Document uploads: Routed via `ProjectService.uploadDocument`, reusing the same write lock and logging timing.
- Caching: `ProjectCache` keeps thread-safe, immutable snapshots for fast reads.
- Background execution: `BackgroundTaskManager` provides a fixed thread pool and a scheduler, with graceful shutdown.
- Parallel reporting: `ReportServiceImpl.generatePortfolioReportParallel` fans out per-project computations using `CompletableFuture` with timeouts.
- Observability: Structured logs around lock waits, retries, and durations for profiling and diagnosis.
- Deadlock discipline: Acquire multiple project locks in ascending `projectId` order.

### Example optimistic update (SQL)

```sql
UPDATE projects
SET status = ?, version = version + 1
WHERE id = ? AND version = ?;
```

### Service-level retry sketch

```java
boolean updated = false;
int attempts = 0;

while (!updated && attempts++ < 3) {
  int rows = projectDao.updateStatusWithVersion(id, newStatus, expectedVersion);
  if (rows == 1) {
    updated = true;
  } else {
    project = projectDao.findById(id);           // refresh
    expectedVersion = project.getVersion();      // retry with new version
  }
}

if (!updated) {
  throw new ConcurrentModificationException("Concurrent modification detected");
}
```

### Console concurrency demo

The Builder menu includes a demo that kicks off asynchronous mock jobs so users can observe concurrent progress in real time (lock acquisition, retries, and completion).

---

## Testing

Unit tests: CRUD and service-level validation.

Concurrency tests (v2):

- `ProjectConcurrencyTest` — concurrent status/budget updates; verifies retries and final state
- `DocumentUploadConcurrencyTest` — parallel metadata inserts on the same project; verifies serialization
- `ParallelReportPerfTest` — compares sequential vs. parallel reporting

Run tests:

```bash
mvn -q -DskipTests=false test
```

---

## Monitoring & Profiling

The following tools ship with the JDK (no extra installs required):

- JConsole (GUI): Live heap, GC, threads

```bash
jconsole
```

- jstat (CLI): GC/heap stats sampling

```bash
jstat -gcutil <PID> 1000 10
```

- jcmd (CLI): Heap info, thread dumps, flags

```bash
jcmd <PID> GC.heap_info
jcmd <PID> Thread.print
```

- Java Flight Recorder (headless): Quick profile capture

```bash
jcmd <PID> JFR.start name=quick settings=profile duration=20s filename=recording.jfr
```

---

## Diagrams

Include or accompany this repository with:

- ER Diagram — database relationships
- Class Diagram — layered architecture
- Sequence Diagrams — Add Project, Update Status, View Portfolio
- Workflow Diagrams — Login/Register, Project Management, Budget & Timeline Tracking


---

## Notes & Limitations

- Passwords are stored in plaintext for demonstration (use BCrypt/Argon2 in production).
- Document “uploads” are mocked: only metadata is stored, not actual files.
- The console UI is minimal by design to highlight architecture and workflows.
- v2 introduces an additional `version` column and lock/executor components that require careful lifecycle management.


---


Quick start: Clone and open either `v1/` or `v2/` as a Maven project in IntelliJ, apply the schema, configure DB credentials, and run `com.builder.portfolio.Main`.
