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
        this.taskId       = taskId;
        this.title        = title;
        this.priorityLevel = priorityLevel;
        this.status       = TaskStatus.open;
        this.creationDate = LocalDateTime.now();
        this.isRecurring  = false;
        this.subtasks     = new ArrayList<>();
        this.tags         = new ArrayList<>();
        this.history      = new ArrayList<>();
        this.occurrences  = new ArrayList<>();
    }

    public void setRecurrencePattern(RecurrencePattern p) {
        this.recurrencePattern = p;
        this.isRecurring = true;
        this.occurrences = p.generateOccurrences(this.title);
    }

    public Subtask addSubtaskForCollaborator(String subtaskId,
                                             Collaborator collaborator) {
        return addSubtaskForCollaborator(subtaskId, this.title, collaborator);
    }

    public Subtask addSubtaskForCollaborator(String subtaskId,
                                             String subtaskTitle,
                                             Collaborator collaborator) {
        // OCL Constraint: A task cannot have more than 20 sub-tasks
        if (subtasks.size() >= 20) {
            throw new IllegalStateException(
                "Task '" + this.title 
                + "' has reached the maximum of 20 subtasks.");
        }
        if (!collaborator.canAcceptTask()) {
            throw new IllegalStateException(
                "Collaborator " + collaborator.getName()
                + " has reached their open task limit of "
                + collaborator.getOpenTaskLimit());
        }
        Subtask s = new Subtask(subtaskId, subtaskTitle);
        s.assignCollaborator(collaborator);
        collaborator.incrementOpenTasks();
        subtasks.add(s);
        return s;
    }

    public void addActivityEntry(ActivityEntry entry) {
        history.add(entry);
    }

    // Getters and setters
    public String getTaskId()             { return taskId; }
    public String getTitle()              { return title; }
    public void setTitle(String t)        { this.title = t; }
    public String getDescription()        { return description; }
    public void setDescription(String d)  { this.description = d; }
    public LocalDateTime getCreationDate(){ return creationDate; }
    public LocalDate getDueDate()         { return dueDate; }
    public void setDueDate(LocalDate d)   { this.dueDate = d; }
    public PriorityLevel getPriorityLevel()          { return priorityLevel; }
    public void setPriorityLevel(PriorityLevel p)    { this.priorityLevel = p; }
    public TaskStatus getStatus()                    { return status; }
    public void setStatus(TaskStatus s)              { this.status = s; }
    public boolean isRecurring()                     { return isRecurring; }
    public RecurrencePattern getRecurrencePattern()  { return recurrencePattern; }
    public List<TaskOccurrence> getOccurrences()     { return occurrences; }
    public List<Subtask> getSubtasks()               { return subtasks; }
    public List<Tag> getTags()                       { return tags; }
    public List<ActivityEntry> getHistory()          { return history; }
    public Project getProject()                      { return project; }
    public void setProject(Project p)                { this.project = p; }
}
