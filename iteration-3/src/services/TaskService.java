package services;

import gateway.ICalGateway;
import models.*;
import persistence.TaskRepository;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;


public class TaskService {

    private List<Task>           tasks           = new ArrayList<>();
    private Map<String, Project> projectRegistry = new HashMap<>();

    private TaskRepository taskRepository;
    private ICalGateway    icalGateway;

    public TaskService() {}

    public TaskService(TaskRepository repo, ICalGateway gateway) {
        this.taskRepository = repo;
        this.icalGateway    = gateway;
    }

    // Persistence 

    public void save() throws IOException {
        if (taskRepository == null) return;
        taskRepository.save(tasks, projectRegistry);
        System.out.println("Data saved successfully.");
    }

    public void load() throws IOException {
        if (taskRepository == null) return;
        List<Task>           loaded         = taskRepository.loadTasks();
        Map<String, Project> loadedProjects = taskRepository.loadProjects();
        this.projectRegistry = loadedProjects;

        // Re-link each task to its project and resolve collaborator placeholders
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
                    .filter(p -> p.getProjectId().equals(pid))
                    .findFirst()
                    .ifPresent(task::setProject);
            }

            if (task.getProject() != null) {
                for (Subtask sub : task.getSubtasks()) {
                    Collaborator ph = sub.getAssignedTo();
                    if (ph != null && "__unresolved__".equals(ph.getName())) {
                        String cid = ph.getCollaboratorId();
                        task.getProject().getCollaborators().stream()
                            .filter(c -> c.getCollaboratorId().equals(cid))
                            .findFirst()
                            .ifPresent(sub::assignCollaborator);
                    }
                }
            }
        }
        this.tasks = loaded;
        System.out.println("Loaded " + tasks.size() + " task(s) and "
            + loadedProjects.size() + " project(s).");
    }

    // iCal Export 

    /** Export a single task to an .ics file. */
    public void exportTaskToICal(Task task, String filePath) throws IOException {
        requireGateway();
        icalGateway.exportTask(task, filePath);
    }

    /** Export all tasks in a named project to an .ics file. */
    public void exportProjectToICal(String projectName, String filePath)
            throws IOException {
        requireGateway();
        Project project = projectRegistry.get(projectName.toLowerCase().trim());
        if (project == null)
            throw new IllegalArgumentException("Project not found: " + projectName);
        List<Task> projectTasks = tasks.stream()
            .filter(t -> t.getProject() != null
                      && t.getProject().getProjectId()
                          .equals(project.getProjectId()))
            .collect(Collectors.toList());
        icalGateway.exportTasks(projectTasks, filePath);
    }

    /** Export a filtered list of tasks to an .ics file. */
    public void exportFilteredToICal(SearchCriteria criteria, String filePath)
            throws IOException {
        requireGateway();
        icalGateway.exportTasks(searchTasks(criteria), filePath);
    }

    private void requireGateway() {
        if (icalGateway == null)
            throw new IllegalStateException("No ICalGateway configured.");
    }

    // Overload Detection 

  
    public List<Collaborator> listOverloadedCollaborators() {
        return projectRegistry.values().stream()
            .flatMap(p -> p.getCollaborators().stream())
            .distinct()
            .filter(Collaborator::isOverloaded)
            .collect(Collectors.toList());
    }

    //  Search

    public List<Task> searchTasks(SearchCriteria criteria) {
        if (criteria == null) {
            return tasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.open)
                .sorted(Comparator.comparing(
                    t -> t.getDueDate() != null ? t.getDueDate() : LocalDate.MAX))
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
                t -> t.getDueDate() != null ? t.getDueDate() : LocalDate.MAX))
            .collect(Collectors.toList());
    }

    // Task Creation 

    /**
     * OCL Constraint 2: no more than 50 open tasks without a due date.
     */
    public Task createTask(String title, String description,
                           PriorityLevel priority, LocalDate dueDate) {
        if (title == null || title.isBlank())
            throw new IllegalArgumentException("Task title is required.");

        if (dueDate == null) {
            long count = tasks.stream()
                .filter(t -> t.getDueDate() == null
                          && t.getStatus() == TaskStatus.open)
                .count();
            if (count >= 50)
                throw new IllegalStateException(
                    "Limit of 50 open tasks without a due date has been reached.");
        }

        String id = UUID.randomUUID().toString();
        Task task = new Task(id, title, priority);
        task.setDescription(description);
        task.setDueDate(dueDate);
        task.addActivityEntry(
            new ActivityEntry(UUID.randomUUID().toString(), ActionType.created));
        tasks.add(task);
        return task;
    }

    /**
     * OCL Constraint 1: a task cannot have more than 20 sub-tasks.
     */
    public Subtask addSubtask(Task task, String subtaskTitle,
                              Collaborator collaborator) {
        if (task.getSubtasks().size() >= 20)
            throw new IllegalStateException(
                "A task cannot have more than 20 sub-tasks.");
        return task.addSubtaskForCollaborator(
            UUID.randomUUID().toString(), subtaskTitle, collaborator);
    }

    public Task createRecurringTask(String title, String description,
                                    PriorityLevel priority,
                                    RecurrencePattern pattern) {
        Task task = createTask(title, description, priority, null);
        task.setRecurrencePattern(pattern);
        return task;
    }

    // Project Registry 

    public Project resolveProject(String name) {
        return projectRegistry.computeIfAbsent(
            name.toLowerCase().trim(),
            k -> new Project(UUID.randomUUID().toString(), name));
    }

    // Accessors 

    public void addTask(Task task)                      { tasks.add(task); }
    public List<Task> getAllTasks()                     { return tasks; }
    public Map<String, Project> getProjectRegistry()   { return projectRegistry; }
    public void setICalGateway(ICalGateway gw)         { this.icalGateway = gw; }
    public void setTaskRepository(TaskRepository repo) { this.taskRepository = repo; }
}
