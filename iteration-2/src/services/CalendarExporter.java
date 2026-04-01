package services;

import models.*;
import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Calendar Exporter - Exports tasks to iCalendar (.ics) format
 * Implements Gateway pattern: abstracts iCalendar format details from domain
 * 
 * Features:
 * - Only exports tasks WITH due dates (ignores tasks without due date)
 * - Includes task title, description, due date, status, priority, project name
 * - If task has subtasks, includes subtask summary in description
 * - Subtasks are NOT exported as separate calendar entries
 * 
 * Supported exports:
 * - Single task
 * - All project tasks
 * - Filtered task list
 */
public class CalendarExporter {

    /**
     * Exports a single task to iCalendar format.
     * @param task Task to export (must have a due date)
     * @param filePath Output .ics file path
     * @throws IOException if file writing fails
     * @throws IllegalArgumentException if task has no due date
     */
    public void exportTask(Task task, String filePath) throws IOException {
        if (task.getDueDate() == null) {
            throw new IllegalArgumentException(
                "Task '" + task.getTitle() + "' has no due date. "
                + "Only tasks with due dates can be exported to calendar.");
        }
        exportTasks(List.of(task), filePath);
    }

    /**
     * Exports all tasks from a project to iCalendar format.
     * Only includes tasks with due dates.
     * @param projectName Name of the project
     * @param allTasks List of all tasks to filter from
     * @param filePath Output .ics file path
     * @throws IOException if file writing fails
     */
    public void exportProjectTasks(String projectName, List<Task> allTasks, 
                                    String filePath) throws IOException {
        List<Task> projectTasks = allTasks.stream()
            .filter(t -> t.getProject() != null 
                      && t.getProject().getName().equals(projectName)
                      && t.getDueDate() != null)
            .toList();
        
        if (projectTasks.isEmpty()) {
            throw new IllegalArgumentException(
                "No tasks with due dates found for project '" + projectName + "'");
        }
        exportTasks(projectTasks, filePath);
    }

    /**
     * Exports a filtered list of tasks to iCalendar format.
     * Only includes tasks with due dates.
     * @param filteredTasks List of tasks to export
     * @param filePath Output .ics file path
     * @throws IOException if file writing fails
     */
    public void exportFilteredTasks(List<Task> filteredTasks, String filePath) 
            throws IOException {
        List<Task> tasksWithDueDate = filteredTasks.stream()
            .filter(t -> t.getDueDate() != null)
            .toList();
        
        if (tasksWithDueDate.isEmpty()) {
            throw new IllegalArgumentException(
                "No tasks with due dates in the filtered list.");
        }
        exportTasks(tasksWithDueDate, filePath);
    }

    /**
     * Core export method: writes tasks in iCalendar format.
     * @param tasks List of tasks to export (assumed to have due dates)
     * @param filePath Output .ics file path
     * @throws IOException if file writing fails
     */
    private void exportTasks(List<Task> tasks, String filePath) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            // iCalendar header
            writer.println("BEGIN:VCALENDAR");
            writer.println("VERSION:2.0");
            writer.println("PRODID:-//Task Management System//EN");
            writer.println("CALSCALE:GREGORIAN");
            writer.println("METHOD:PUBLISH");
            writer.println("X-WR-CALNAME:Tasks Export");
            writer.println("X-WR-TIMEZONE:UTC");
            writer.println();

            // Create calendar entry for each task
            for (Task task : tasks) {
                writeTaskEvent(writer, task);
            }

            // iCalendar footer
            writer.println("END:VCALENDAR");
        }
        System.out.println("Calendar export complete: " + filePath);
    }

    /**
     * Writes a single task as a VEVENT in iCalendar format.
     * @param writer PrintWriter to write to
     * @param task Task to write
     */
    private void writeTaskEvent(PrintWriter writer, Task task) {
        writer.println("BEGIN:VEVENT");
        
        // Unique ID (UID) - required by iCalendar spec
        writer.println("UID:" + task.getTaskId() + "@tasksystem");
        
        // Summary (title) - required
        writer.println("SUMMARY:" + escapeText(task.getTitle()));
        
        // Description - includes subtask summary if any
        String description = buildDescription(task);
        writer.println("DESCRIPTION:" + escapeText(description));
        
        // Due date (DTDUE) in YYYYMMDD format
        String dueDateStr = task.getDueDate()
            .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        writer.println("DTDUE;VALUE=DATE:" + dueDateStr);
        
        // Creation date
        String createdStr = task.getCreationDate()
            .format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"));
        writer.println("DTSTAMP:" + createdStr);
        
        // Status mapping: open -> TODO, completed -> COMPLETED, cancelled -> CANCELLED
        String icsStatus = mapTaskStatusToICS(task.getStatus());
        writer.println("STATUS:" + icsStatus);
        
        // Priority mapping: high -> 1, medium -> 5, low -> 9 (iCalendar uses 1-9)
        int priority = mapPriorityToICS(task.getPriorityLevel());
        writer.println("PRIORITY:" + priority);
        
        // Project name (custom field)
        if (task.getProject() != null) {
            writer.println("X-PROJECT-NAME:" + escapeText(task.getProject().getName()));
        }
        
        // Categories (tags)
        if (!task.getTags().isEmpty()) {
            String tags = task.getTags().stream()
                .map(Tag::getName)
                .reduce((a, b) -> a + "," + b)
                .orElse("");
            writer.println("CATEGORIES:" + escapeText(tags));
        }
        
        writer.println("END:VEVENT");
        writer.println();
    }

    /**
     * Builds the description field, including subtask summary if present.
     */
    private String buildDescription(Task task) {
        StringBuilder desc = new StringBuilder();
        
        if (task.getDescription() != null && !task.getDescription().isEmpty()) {
            desc.append(task.getDescription());
        }
        
        // Include subtask summary if task has subtasks
        if (!task.getSubtasks().isEmpty()) {
            if (desc.length() > 0) {
                desc.append("\\n\\n");
            }
            desc.append("Subtasks:\\n");
            for (Subtask subtask : task.getSubtasks()) {
                desc.append("- ").append(subtask.getTitle())
                    .append(" [").append(subtask.getStatus().name().toUpperCase())
                    .append("]\\n");
            }
        }
        
        return desc.toString();
    }

    /**
     * Maps TaskStatus to iCalendar STATUS values.
     */
    private String mapTaskStatusToICS(TaskStatus status) {
        return switch (status) {
            case open -> "TODO";
            case completed -> "COMPLETED";
            case cancelled -> "CANCELLED";
        };
    }

    /**
     * Maps PriorityLevel to iCalendar PRIORITY values (1-9, where 1 is highest).
     */
    private int mapPriorityToICS(PriorityLevel priority) {
        return switch (priority) {
            case high -> 1;      // High priority
            case medium -> 5;    // Medium priority
            case low -> 9;       // Low priority
        };
    }

    /**
     * Escapes special characters for iCalendar format.
     * In iCalendar, newlines are represented as \n and commas/semicolons in text values
     * must be escaped with backslash.
     */
    private String escapeText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return text
            .replace("\\", "\\\\")      // Backslash first
            .replace(",", "\\,")        // Comma
            .replace(";", "\\;")        // Semicolon
            .replace("\n", "\\n")       // Newline
            .replace("\r", "");         // Remove carriage return
    }
}
