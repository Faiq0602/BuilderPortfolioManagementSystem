package com.builder.portfolio.support;

import com.builder.portfolio.dao.ProjectDAO;
import com.builder.portfolio.model.Project;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryProjectDAO implements ProjectDAO {
    private final ConcurrentHashMap<Long, Project> store = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, AtomicLong> versions = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public void addProject(Project project) {
        // Mirror the optimistic version semantics without needing a real database.
        long id = project.getId() > 0 ? project.getId() : idGenerator.getAndIncrement();
        project.setId(Math.toIntExact(id));
        Project copy = copy(project);
        store.put(id, copy);
        versions.put(id, new AtomicLong(copy.getVersion()));
    }

    @Override
    public void updateProject(Project project) {
        long id = project.getId();
        Project copy = copy(project);
        store.put(id, copy);
        versions.computeIfAbsent(id, key -> new AtomicLong(copy.getVersion())).set(copy.getVersion());
    }

    @Override
    public void deleteProject(int projectId, int builderId) {
        Project removed = store.remove((long) projectId);
        if (removed != null) {
            versions.remove((long) projectId);
        }
    }

    @Override
    public List<Project> findProjectsByBuilder(int builderId) {
        return store.values().stream()
                .filter(project -> project.getBuilderId() == builderId)
                .sorted(Comparator.comparingInt(Project::getId))
                .map(this::copy)
                .toList();
    }

    @Override
    public List<Project> findProjectsByClient(int clientId) {
        return store.values().stream()
                .filter(project -> project.getClientId() == clientId)
                .sorted(Comparator.comparingInt(Project::getId))
                .map(this::copy)
                .toList();
    }

    @Override
    public List<Project> findAllProjects() {
        return store.values().stream()
                .sorted(Comparator.comparingInt(Project::getId))
                .map(this::copy)
                .toList();
    }

    @Override
    public Project findById(int projectId) {
        return Optional.ofNullable(store.get((long) projectId)).map(this::copy).orElse(null);
    }

    @Override
    public Project findByIdForUpdate(long projectId) {
        return Optional.ofNullable(store.get(projectId)).map(this::copy).orElse(null);
    }

    @Override
    public boolean conditionalUpdateProject(Project project, long expectedVersion) {
        long id = project.getId();
        AtomicLong current = versions.computeIfAbsent(id, key -> new AtomicLong());
        if (!current.compareAndSet(expectedVersion, expectedVersion + 1)) {
            return false;
        }
        project.setVersion(expectedVersion + 1);
        store.put(id, copy(project));
        return true;
    }

    private Project copy(Project original) {
        Project copy = new Project();
        copy.setId(original.getId());
        copy.setName(original.getName());
        copy.setDescription(original.getDescription());
        copy.setStatus(original.getStatus());
        copy.setBuilderId(original.getBuilderId());
        copy.setClientId(original.getClientId());
        copy.setBudgetPlanned(original.getBudgetPlanned());
        copy.setBudgetUsed(original.getBudgetUsed());
        copy.setStartDate(cloneDate(original.getStartDate()));
        copy.setEndDate(cloneDate(original.getEndDate()));
        copy.setVersion(original.getVersion());
        return copy;
    }

    private LocalDate cloneDate(LocalDate date) {
        return date != null ? LocalDate.from(date) : null;
    }
}
