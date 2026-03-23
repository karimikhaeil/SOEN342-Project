package services;

import models.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class TaskService {
    private List<Task> tasks = new ArrayList<>();
    private Map<String, Project> projectRegistry = new HashMap<>();

    // Search 

    public List<Task> searchTasks(SearchCriteria criteria) {
        // No criteria = all open tasks sorted by dueDate ascending
        if (criteria == null) {
            return tasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.open)
                .sorted(Comparator.comparing(
                    t -> t.getDueDate() != null
                        ? t.getDueDate() : LocalDate.MAX))
                .collect(Collectors.toList());
        }

        return tasks.stream()
            .filter(t -> criteria.getStatus() == null
                      || t.getStatus() == criteria.getStatus())
            .filter(t -> criteria.getTitleKeyword() == null
                      || criteria.getTitleKeyword().isBlank()
                      || t.getTitle().toLowerCase()
                          .contains(criteria.getTitleKeyword().toLowerCase()))
            .filter(t -> criteria.getStartDate() == null
                      || t.getDueDate() == null
                      || !t.getDueDate().isBefore(criteria.getStartDate()))
            .filter(t -> criteria.getEndDate() == null
                      || t.getDueDate() == null
                      || !t.getDueDate().isAfter(criteria.getEndDate()))
            .filter(t -> criteria.getDayOfWeek() == null
                      || criteria.getDayOfWeek().isBlank()
                      || (t.getDueDate() != null && t.getDueDate()
                          .getDayOfWeek().name()
                          .equalsIgnoreCase(criteria.getDayOfWeek())))
            .sorted(Comparator.comparing(
                t -> t.getDueDate() != null
                    ? t.getDueDate() : LocalDate.MAX))
            .collect(Collectors.toList());
    }

    // Task creation 

    public Task createTask(String title, String description,
                           PriorityLevel priority, LocalDate dueDate) {
        if (title == null || title.isBlank())
            throw new IllegalArgumentException("Task title is required");

        String id = UUID.randomUUID().toString();
        Task task = new Task(id, title, priority);
        task.setDescription(description);
        task.setDueDate(dueDate);

        ActivityEntry entry = new ActivityEntry(
            UUID.randomUUID().toString(), ActionType.created);
        task.addActivityEntry(entry);
        tasks.add(task);
        return task;
    }

    public Task createRecurringTask(String title, String description,
                                    PriorityLevel priority,
                                    RecurrencePattern pattern) {
        Task task = createTask(title, description, priority, null);
        task.setRecurrencePattern(pattern);
        return task;
    }

    // Project registry 

    public Project resolveProject(String name) {
        // Project names are unique — reuse existing if found
        return projectRegistry.computeIfAbsent(
            name.toLowerCase().trim(),
            k -> new Project(UUID.randomUUID().toString(), name));
    }

    // General

    public void addTask(Task task)         { tasks.add(task); }
    public List<Task> getAllTasks()        { return tasks; }
    public Map<String, Project> getProjectRegistry() { return projectRegistry; }
}
