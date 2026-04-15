package services;

import gateway.ICalGateway;
import models.*;
import persistence.TaskRepository;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class TaskService {

    private List<Task> tasks = new ArrayList<>();
    private Map<String, Project> projectRegistry = new LinkedHashMap<>();
    private Map<String, Collaborator> collaboratorRegistry = new LinkedHashMap<>();

    private TaskRepository taskRepository;
    private ICalGateway icalGateway;

    public TaskService() {}

    public TaskService(TaskRepository repo, ICalGateway gateway) {
        this.taskRepository = repo;
        this.icalGateway = gateway;
    }

    public void save() throws IOException {
        if (taskRepository == null) return;
        taskRepository.save(tasks, projectRegistry);
        System.out.println("Data saved successfully.");
    }

    public void load() throws IOException {
        if (taskRepository == null) return;
        List<Task> loaded = taskRepository.loadTasks();
        Map<String, Project> loadedProjects = taskRepository.loadProjects();
        this.projectRegistry = loadedProjects;
        this.collaboratorRegistry = new LinkedHashMap<>();

        for (Project project : loadedProjects.values()) {
            for (Collaborator collaborator : project.getCollaborators()) {
                collaboratorRegistry.put(collaborator.getCollaboratorId(), collaborator);
            }
        }

        for (Task task : loaded) {
            String projectId = null;
            List<Tag> toRemove = new ArrayList<>();
            for (Tag tag : task.getTags()) {
                if (tag.getName().startsWith("__projectId__:")) {
                    projectId = tag.getName().substring("__projectId__:".length());
                    toRemove.add(tag);
                }
            }
            task.getTags().removeAll(toRemove);

            if (projectId != null) {
                final String pid = projectId;
                loadedProjects.values().stream()
                    .filter(project -> project.getProjectId().equals(pid))
                    .findFirst()
                    .ifPresent(project -> {
                        task.setProject(project);
                        project.addTask(task);
                    });
            }

            if (task.getProject() != null) {
                for (Subtask subtask : task.getSubtasks()) {
                    Collaborator placeholder = subtask.getAssignedTo();
                    if (placeholder != null && "__unresolved__".equals(placeholder.getName())) {
                        String collaboratorId = placeholder.getCollaboratorId();
                        task.getProject().getCollaborators().stream()
                            .filter(collaborator -> collaborator.getCollaboratorId().equals(collaboratorId))
                            .findFirst()
                            .ifPresent(subtask::assignCollaborator);
                    }
                }
            }
        }

        this.tasks = loaded;
        System.out.println("Loaded " + tasks.size() + " task(s) and "
            + loadedProjects.size() + " project(s).");
    }

    public void exportTaskToICal(Task task, String filePath) throws IOException {
        requireGateway();
        icalGateway.exportTask(task, filePath);
    }

    public void exportProjectToICal(String projectName, String filePath)
        throws IOException {
        requireGateway();
        Project project = projectRegistry.get(projectName.toLowerCase().trim());
        if (project == null) {
            throw new IllegalArgumentException("Project not found: " + projectName);
        }
        List<Task> projectTasks = tasks.stream()
            .filter(task -> task.getProject() != null
                && task.getProject().getProjectId().equals(project.getProjectId()))
            .collect(Collectors.toList());
        icalGateway.exportTasks(projectTasks, filePath);
    }

    public void exportFilteredToICal(SearchCriteria criteria, String filePath)
        throws IOException {
        requireGateway();
        icalGateway.exportTasks(searchTasks(criteria), filePath);
    }

    private void requireGateway() {
        if (icalGateway == null) {
            throw new IllegalStateException("No ICalGateway configured.");
        }
    }

    public List<Collaborator> listOverloadedCollaborators() {
        return getAllCollaborators().stream()
            .filter(Collaborator::isOverloaded)
            .collect(Collectors.toList());
    }

    public List<Task> searchTasks(SearchCriteria criteria) {
        if (criteria == null) {
            return tasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.open)
                .sorted(Comparator.comparing(
                    task -> task.getDueDate() != null ? task.getDueDate() : LocalDate.MAX))
                .collect(Collectors.toList());
        }

        return tasks.stream()
            .filter(task -> criteria.getStatus() == null
                || task.getStatus() == criteria.getStatus())
            .filter(task -> criteria.getTitleKeyword() == null
                || criteria.getTitleKeyword().isBlank()
                || task.getTitle().toLowerCase()
                    .contains(criteria.getTitleKeyword().toLowerCase()))
            .filter(task -> criteria.getStartDate() == null
                || task.getDueDate() == null
                || !task.getDueDate().isBefore(criteria.getStartDate()))
            .filter(task -> criteria.getEndDate() == null
                || task.getDueDate() == null
                || !task.getDueDate().isAfter(criteria.getEndDate()))
            .filter(task -> criteria.getDayOfWeek() == null
                || criteria.getDayOfWeek().isBlank()
                || (task.getDueDate() != null
                && task.getDueDate().getDayOfWeek().name()
                    .equalsIgnoreCase(criteria.getDayOfWeek())))
            .sorted(Comparator.comparing(
                task -> task.getDueDate() != null ? task.getDueDate() : LocalDate.MAX))
            .collect(Collectors.toList());
    }

    public Task createTask(String title, String description,
                           PriorityLevel priority, LocalDate dueDate) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Task title is required.");
        }

        if (dueDate == null) {
            long count = tasks.stream()
                .filter(task -> task.getDueDate() == null
                    && task.getStatus() == TaskStatus.open)
                .count();
            if (count >= 50) {
                throw new IllegalStateException(
                    "Limit of 50 open tasks without a due date has been reached.");
            }
        }

        Task task = new Task(UUID.randomUUID().toString(), title, priority);
        task.setDescription(description);
        task.setDueDate(dueDate);
        task.addActivityEntry(new ActivityEntry(
            UUID.randomUUID().toString(), ActionType.created));
        tasks.add(task);
        return task;
    }

    public Task createRecurringTask(String title, String description,
                                    PriorityLevel priority,
                                    RecurrencePattern pattern) {
        if (pattern == null) {
            throw new IllegalArgumentException("Recurrence pattern is required");
        }
        Task task = createTask(title, description, priority, pattern.getStartDate());
        task.setRecurrencePattern(pattern);
        task.addActivityEntry(new ActivityEntry(
            UUID.randomUUID().toString(), ActionType.updated));
        return task;
    }

    public Task updateTask(String taskId, String title, String description,
                           PriorityLevel priority, LocalDate dueDate) {
        Task task = findTaskById(taskId);
        if (title != null && !title.isBlank()) {
            task.setTitle(title);
        }
        task.setDescription(description);
        if (priority != null) {
            task.setPriorityLevel(priority);
        }
        task.setDueDate(dueDate);
        task.addActivityEntry(new ActivityEntry(
            UUID.randomUUID().toString(), ActionType.updated));
        return task;
    }

    public Task changeTaskStatus(String taskId, TaskStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Task status is required");
        }
        Task task = findTaskById(taskId);
        task.changeStatus(status);
        task.addActivityEntry(new ActivityEntry(
            UUID.randomUUID().toString(),
            status == TaskStatus.completed ? ActionType.completed : ActionType.updated));
        return task;
    }

    public Task reopenTask(String taskId) {
        Task task = findTaskById(taskId);
        task.reopen();
        task.addActivityEntry(new ActivityEntry(
            UUID.randomUUID().toString(), ActionType.updated));
        return task;
    }

    public Tag addTagToTask(String taskId, String tagName) {
        Task task = findTaskById(taskId);
        Tag tag = new Tag(tagName);
        task.addTag(tag);
        task.addActivityEntry(new ActivityEntry(
            UUID.randomUUID().toString(), ActionType.updated));
        return tag;
    }

    public void removeTagFromTask(String taskId, String tagName) {
        Task task = findTaskById(taskId);
        task.removeTag(tagName);
        task.addActivityEntry(new ActivityEntry(
            UUID.randomUUID().toString(), ActionType.updated));
    }

    public Subtask addSubtask(Task task, String subtaskTitle,
                              Collaborator collaborator) {
        if (task.getSubtasks().size() >= 20) {
            throw new IllegalStateException(
                "A task cannot have more than 20 sub-tasks.");
        }
        Subtask subtask = task.addSubtaskForCollaborator(
            UUID.randomUUID().toString(), subtaskTitle, collaborator);
        task.addActivityEntry(new ActivityEntry(
            UUID.randomUUID().toString(), ActionType.updated));
        return subtask;
    }

    public Subtask addSubtaskToTask(String taskId, String subtaskTitle) {
        Task task = findTaskById(taskId);
        Subtask subtask = new Subtask(UUID.randomUUID().toString(), subtaskTitle);
        task.addSubtask(subtask);
        task.addActivityEntry(new ActivityEntry(
            UUID.randomUUID().toString(), ActionType.updated));
        return subtask;
    }

    public Subtask assignCollaboratorToTask(String taskId, String subtaskTitle,
                                            String collaboratorId) {
        Task task = findTaskById(taskId);
        Collaborator collaborator = findCollaboratorById(collaboratorId);
        Subtask subtask = task.addSubtaskForCollaborator(
            UUID.randomUUID().toString(), subtaskTitle, collaborator);
        task.addActivityEntry(new ActivityEntry(
            UUID.randomUUID().toString(), ActionType.updated));
        return subtask;
    }

    public Project createProject(String name, String description) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Project name is required");
        }
        Project project = resolveProject(name);
        project.setDescription(description);
        return project;
    }

    public Project resolveProject(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Project name is required");
        }
        return projectRegistry.computeIfAbsent(
            name.toLowerCase().trim(),
            key -> new Project(UUID.randomUUID().toString(), name));
    }

    public void assignTaskToProject(String taskId, String projectName) {
        Task task = findTaskById(taskId);
        if (task.getProject() != null) {
            task.getProject().removeTask(task);
        }
        Project project = resolveProject(projectName);
        task.setProject(project);
        project.addTask(task);
        task.addActivityEntry(new ActivityEntry(
            UUID.randomUUID().toString(), ActionType.updated));
    }

    public void removeTaskFromProject(String taskId) {
        Task task = findTaskById(taskId);
        if (task.getProject() != null) {
            task.getProject().removeTask(task);
            task.setProject(null);
            task.addActivityEntry(new ActivityEntry(
                UUID.randomUUID().toString(), ActionType.updated));
        }
    }

    public Collaborator createCollaborator(String name, CollaboratorCategory category) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Collaborator name is required");
        }
        if (category == null) {
            throw new IllegalArgumentException("Collaborator category is required");
        }

        Collaborator existing = collaboratorRegistry.values().stream()
            .filter(collaborator -> collaborator.getName().equalsIgnoreCase(name))
            .findFirst()
            .orElse(null);
        if (existing != null) {
            return existing;
        }

        Collaborator collaborator = new Collaborator(
            UUID.randomUUID().toString(), name, category);
        collaboratorRegistry.put(collaborator.getCollaboratorId(), collaborator);
        return collaborator;
    }

    public void assignCollaboratorToProject(String collaboratorId, String projectName) {
        Collaborator collaborator = findCollaboratorById(collaboratorId);
        Project project = resolveProject(projectName);
        project.addCollaborator(collaborator);
    }

    public Task findTaskById(String taskId) {
        return tasks.stream()
            .filter(task -> task.getTaskId().equals(taskId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
    }

    public Collaborator findCollaboratorById(String collaboratorId) {
        Collaborator collaborator = collaboratorRegistry.get(collaboratorId);
        if (collaborator == null) {
            throw new IllegalArgumentException("Collaborator not found: " + collaboratorId);
        }
        return collaborator;
    }

    public List<ActivityEntry> getTaskActivityHistory(String taskId) {
        return findTaskById(taskId).getHistory();
    }

    public List<Collaborator> getAllCollaborators() {
        Map<String, Collaborator> combined = new LinkedHashMap<>(collaboratorRegistry);
        tasks.stream()
            .flatMap(task -> task.getSubtasks().stream())
            .map(Subtask::getAssignedTo)
            .filter(Objects::nonNull)
            .forEach(collaborator -> combined.putIfAbsent(
                collaborator.getCollaboratorId(), collaborator));
        return new ArrayList<>(combined.values());
    }

    public List<Project> getAllProjects() {
        return new ArrayList<>(projectRegistry.values());
    }

    public void addTask(Task task) { tasks.add(task); }
    public List<Task> getAllTasks() { return tasks; }
    public Map<String, Project> getProjectRegistry() { return projectRegistry; }
    public void setICalGateway(ICalGateway gateway) { this.icalGateway = gateway; }
    public void setTaskRepository(TaskRepository repo) { this.taskRepository = repo; }
}
