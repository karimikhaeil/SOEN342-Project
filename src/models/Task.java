package models;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Task {
    private String taskId;
    private String title;
    private String description;
    private LocalDateTime creationDate;
    private LocalDate dueDate;
    private PriorityLevel priorityLevel;
    private TaskStatus status;
    private boolean isRecurring;

    private RecurrencePattern recurrencePattern;
    private List<TaskOccurrence> occurrences;
    private List<Subtask> subtasks;
    private List<Tag> tags;
    private List<ActivityEntry> history;
    private Project project;

    public Task(String taskId, String title, PriorityLevel priorityLevel) {
        this.taskId = taskId;
        this.title = title;
        this.priorityLevel = priorityLevel;
        this.status = TaskStatus.open;
        this.creationDate = LocalDateTime.now();
        this.isRecurring = false;
        this.subtasks = new ArrayList<>();
        this.tags = new ArrayList<>();
        this.history = new ArrayList<>();
        this.occurrences = new ArrayList<>();
    }

    public void setRecurrencePattern(RecurrencePattern pattern) {
        this.recurrencePattern = pattern;
        this.isRecurring = true;
        this.occurrences = pattern.generateOccurrences(this.title);
    }

    public void addSubtask(Subtask subtask) {
        if (subtask == null) {
            throw new IllegalArgumentException("Subtask is required");
        }
        if (subtasks.size() >= 20) {
            throw new IllegalStateException(
                "Task '" + this.title + "' has reached the maximum of 20 subtasks.");
        }
        subtasks.add(subtask);
    }

    public Subtask addSubtaskForCollaborator(String subtaskId,
                                             Collaborator collaborator) {
        return addSubtaskForCollaborator(subtaskId, this.title, collaborator);
    }

    public Subtask addSubtaskForCollaborator(String subtaskId,
                                             String subtaskTitle,
                                             Collaborator collaborator) {
        if (subtasks.size() >= 20) {
            throw new IllegalStateException(
                "Task '" + this.title + "' has reached the maximum of 20 subtasks.");
        }
        if (!collaborator.canAcceptTask()) {
            throw new IllegalStateException(
                "Collaborator " + collaborator.getName()
                + " has reached their open task limit of "
                + collaborator.getOpenTaskLimit());
        }
        Subtask subtask = new Subtask(subtaskId, subtaskTitle);
        subtask.assignCollaborator(collaborator);
        collaborator.incrementOpenTasks();
        subtasks.add(subtask);
        return subtask;
    }

    public void addTag(Tag tag) {
        if (tag == null || tag.getName() == null || tag.getName().isBlank()) {
            throw new IllegalArgumentException("Tag name is required");
        }
        boolean exists = tags.stream()
            .anyMatch(existing -> existing.getName().equalsIgnoreCase(tag.getName()));
        if (!exists) {
            tags.add(tag);
        }
    }

    public void removeTag(String tagName) {
        if (tagName == null || tagName.isBlank()) {
            return;
        }
        tags.removeIf(tag -> tag.getName().equalsIgnoreCase(tagName));
    }

    public void changeStatus(TaskStatus newStatus) {
        this.status = newStatus;
    }

    public void reopen() {
        this.status = TaskStatus.open;
    }

    public boolean hasDueDate() {
        return dueDate != null;
    }

    public void addActivityEntry(ActivityEntry entry) {
        history.add(entry);
    }

    public String getTaskId() { return taskId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getCreationDate() { return creationDate; }
    public void setCreationDate(LocalDateTime creationDate) { this.creationDate = creationDate; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public PriorityLevel getPriorityLevel() { return priorityLevel; }
    public void setPriorityLevel(PriorityLevel priorityLevel) { this.priorityLevel = priorityLevel; }
    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
    public boolean isRecurring() { return isRecurring; }
    public void setRecurring(boolean recurring) { this.isRecurring = recurring; }
    public RecurrencePattern getRecurrencePattern() { return recurrencePattern; }
    public List<TaskOccurrence> getOccurrences() { return occurrences; }
    public void setOccurrences(List<TaskOccurrence> occurrences) { this.occurrences = occurrences; }
    public List<Subtask> getSubtasks() { return subtasks; }
    public List<Tag> getTags() { return tags; }
    public List<ActivityEntry> getHistory() { return history; }
    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
}
