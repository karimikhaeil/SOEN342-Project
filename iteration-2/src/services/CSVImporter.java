package services;

import models.*;
import java.io.*;
import java.time.LocalDate;
import java.util.UUID;

public class CSVImporter {

    private TaskService taskService;

    public CSVImporter(TaskService taskService) {
        this.taskService = taskService;
    }

    public int importTasks(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists())
            throw new FileNotFoundException("File not found: " + filePath);

        int count = 0;
        try (BufferedReader reader =
                new BufferedReader(new FileReader(file))) {

            String line = reader.readLine(); // skip header row
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                try {
                    processRow(line);
                    count++;
                } catch (Exception e) {
                    System.out.println("Skipping invalid row: "
                        + line + " | Reason: " + e.getMessage());
                }
            }
        }
        System.out.println("Import complete. Tasks imported: " + count);
        return count;
    }

    private void processRow(String line) {
        // Columns: TaskName,Description,Subtask,Status,Priority,
        //          DueDate,ProjectName,ProjectDescription,
        //          Collaborator,CollaboratorCategory
        String[] cols = splitCSVLine(line);

        String taskName    = get(cols, 0);
        String description = get(cols, 1);
        String status      = get(cols, 3);
        String priority    = get(cols, 4);
        String dueDate     = get(cols, 5);
        String projectName = get(cols, 6);
        String projectDesc = get(cols, 7);
        String collabName  = get(cols, 8);
        String collabCat   = get(cols, 9);

        if (taskName.isBlank())
            throw new IllegalArgumentException("TaskName is required");

        // Resolve priority
        PriorityLevel pl;
        try { pl = PriorityLevel.valueOf(priority.toLowerCase()); }
        catch (Exception e) { pl = PriorityLevel.medium; }

        // Create task
        Task task = new Task(UUID.randomUUID().toString(), taskName, pl);
        task.setDescription(description);

        // Set status
        try { task.setStatus(TaskStatus.valueOf(status.toLowerCase())); }
        catch (Exception e) { task.setStatus(TaskStatus.open); }

        // Set due date
        if (!dueDate.isBlank()) {
            try { task.setDueDate(LocalDate.parse(dueDate)); }
            catch (Exception e) {
                System.out.println("Invalid date format for task: "
                    + taskName + ", skipping date.");
            }
        }

        // Resolve project (unique by name)
        if (!projectName.isBlank()) {
            Project project = taskService.resolveProject(projectName);
            if (!projectDesc.isBlank())
                project.setDescription(projectDesc);
            task.setProject(project);

            // Resolve collaborator under that project
            if (!collabName.isBlank()) {
                Collaborator collab =
                    project.findCollaboratorByName(collabName);
                if (collab == null) {
                    CollaboratorCategory cat = CollaboratorCategory.Junior;
                    try {
                        cat = CollaboratorCategory.valueOf(
                            capitalize(collabCat));
                    } catch (Exception ignored) {}
                    collab = new Collaborator(
                        UUID.randomUUID().toString(), collabName, cat);
                    project.addCollaborator(collab);
                }
                // Automatically create subtask for collaborator
                if (collab.canAcceptTask()) {
                    task.addSubtaskForCollaborator(
                        UUID.randomUUID().toString(), collab);
                } else {
                    System.out.println("Warning: collaborator "
                        + collabName + " is at task limit, skipping link.");
                }
            }
        }

        taskService.addTask(task);
    }

    // Handles quoted CSV fields correctly
    private String[] splitCSVLine(String line) {
        java.util.List<String> result = new java.util.ArrayList<>();
        boolean inQuotes = false;
        StringBuilder sb = new StringBuilder();
        for (char c : line.toCharArray()) {
            if (c == '"') { inQuotes = !inQuotes; }
            else if (c == ',' && !inQuotes) {
                result.add(sb.toString().trim());
                sb.setLength(0);
            } else { sb.append(c); }
        }
        result.add(sb.toString().trim());
        return result.toArray(new String[0]);
    }

    private String get(String[] cols, int i) {
        return (i < cols.length) ? cols[i].trim() : "";
    }

    private String capitalize(String s) {
        if (s == null || s.isBlank()) return s;
        return s.substring(0, 1).toUpperCase()
             + s.substring(1).toLowerCase();
    }
}
