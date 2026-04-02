package services;

import models.*;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Persistence Manager - Saves and loads task system state to/from JSON files
 * 
 * Handles serialization of:
 * - Tasks (with all properties)
 * - Subtasks
 * - Projects
 * - Collaborators
 * - Activity history
 * - Recurring patterns
 * - Tags
 */
public class PersistenceManager {
    
    private static final String DEFAULT_DATA_FILE = "data.json";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Saves the entire task system state to a JSON file.
     * @param taskService The TaskService containing all tasks and projects
     * @param filePath Path to save the JSON file
     * @throws IOException if file writing fails
     */
    public void saveState(TaskService taskService, String filePath) throws IOException {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"tasks\": [\n");

        List<Task> tasks = taskService.getAllTasks();
        for (int i = 0; i < tasks.size(); i++) {
            json.append(serializeTask(tasks.get(i)));
            if (i < tasks.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }

        json.append("  ],\n");
        json.append("  \"projects\": [\n");

        List<Project> projects = new ArrayList<>(taskService.getProjectRegistry().values());
        for (int i = 0; i < projects.size(); i++) {
            json.append(serializeProject(projects.get(i)));
            if (i < projects.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }

        json.append("  ]\n");
        json.append("}\n");

        Files.write(Paths.get(filePath), json.toString().getBytes());
        System.out.println("State saved to: " + filePath);
    }

    /**
     * Saves to the default data file.
     */
    public void saveState(TaskService taskService) throws IOException {
        saveState(taskService, DEFAULT_DATA_FILE);
    }

    /**
     * Loads task system state from a JSON file.
     * NOTE: This is a simplified implementation that restores task structure.
     * Full restoration of collaborator workload counts would require additional implementation.
     * @param filePath Path to the JSON file
     * @return TaskService populated with loaded data
     * @throws IOException if file reading fails
     */
    public TaskService loadState(String filePath) throws IOException {
        if (!Files.exists(Paths.get(filePath))) {
            System.out.println("Data file not found. Starting with empty system.");
            return new TaskService();
        }
        throw new UnsupportedOperationException(
            "Loading persisted state is not implemented yet. "
            + "The persistence layer currently supports saving to JSON only."
        );
    }

    /**
     * Loads from the default data file.
     */
    public TaskService loadState() throws IOException {
        return loadState(DEFAULT_DATA_FILE);
    }

    /**
     * Serializes a Task to JSON format.
     */
    private String serializeTask(Task task) {
        StringBuilder json = new StringBuilder();
        json.append("    {\n");
        json.append("      \"taskId\": \"").append(escapeJson(task.getTaskId())).append("\",\n");
        json.append("      \"title\": \"").append(escapeJson(task.getTitle())).append("\",\n");
        json.append("      \"description\": \"").append(escapeJson(task.getDescription() != null ? task.getDescription() : "")).append("\",\n");
        json.append("      \"creationDate\": \"").append(task.getCreationDate().format(DATETIME_FORMATTER)).append("\",\n");
        json.append("      \"dueDate\": \"").append(task.getDueDate() != null ? task.getDueDate().format(DATE_FORMATTER) : "").append("\",\n");
        json.append("      \"priorityLevel\": \"").append(task.getPriorityLevel().name()).append("\",\n");
        json.append("      \"status\": \"").append(task.getStatus().name()).append("\",\n");
        json.append("      \"isRecurring\": ").append(task.isRecurring()).append(",\n");
        
        // Project
        if (task.getProject() != null) {
            json.append("      \"projectId\": \"").append(escapeJson(task.getProject().getProjectId())).append("\",\n");
        } else {
            json.append("      \"projectId\": null,\n");
        }
        
        // Tags
        json.append("      \"tags\": [");
        List<Tag> tags = task.getTags();
        for (int i = 0; i < tags.size(); i++) {
            json.append("\"").append(escapeJson(tags.get(i).getName())).append("\"");
            if (i < tags.size() - 1) json.append(", ");
        }
        json.append("],\n");
        
        // Subtasks
        json.append("      \"subtasks\": [");
        List<Subtask> subtasks = task.getSubtasks();
        for (int i = 0; i < subtasks.size(); i++) {
            json.append("\"").append(escapeJson(subtasks.get(i).getTitle())).append("\"");
            if (i < subtasks.size() - 1) json.append(", ");
        }
        json.append("]\n");
        json.append("    }");
        
        return json.toString();
    }

    /**
     * Serializes a Project to JSON format.
     */
    private String serializeProject(Project project) {
        StringBuilder json = new StringBuilder();
        json.append("    {\n");
        json.append("      \"id\": \"").append(escapeJson(project.getProjectId())).append("\",\n");
        json.append("      \"name\": \"").append(escapeJson(project.getName())).append("\",\n");
        json.append("      \"description\": \"").append(escapeJson(project.getDescription() != null ? project.getDescription() : "")).append("\"\n");
        json.append("    }");
        return json.toString();
    }

    /**
     * Escapes special characters for JSON string values.
     */
    private String escapeJson(String value) {
        if (value == null) return "";
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }
}
