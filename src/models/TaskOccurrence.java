package models;

import java.time.LocalDate;

public class TaskOccurrence {
    private String occurrenceId;
    private String title;
    private LocalDate dueDate;
    private TaskStatus status;

    public TaskOccurrence(String occurrenceId, String title, LocalDate dueDate) {
        this.occurrenceId = occurrenceId;
        this.title = title;
        this.dueDate = dueDate;
        this.status = TaskStatus.open;
    }

    public void complete() {
        this.status = TaskStatus.completed;
    }

    public String getOccurrenceId() { return occurrenceId; }
    public String getTitle() { return title; }
    public LocalDate getDueDate() { return dueDate; }
    public TaskStatus getStatus() { return status; }
}
