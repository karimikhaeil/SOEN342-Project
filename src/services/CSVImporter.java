package services;

import models.*;
import java.io.*;
import java.time.LocalDate;

public class CSVImporter {

    private TaskService taskService;

    public CSVImporter(TaskService taskService) {
        this.taskService = taskService;
    }

    public int importTasks(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new FileNotFoundException("File not found: " + filePath);
        }

        int count = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            reader.readLine();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
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
        String[] cols = splitCsvLine(line);

        String taskName = get(cols, 0);
        String description = get(cols, 1);
        String subtaskName = get(cols, 2);
        String status = get(cols, 3);
        String priority = get(cols, 4);
        String dueDate = get(cols, 5);
        String projectName = get(cols, 6);
        String projectDesc = get(cols, 7);
        String collaboratorName = get(cols, 8);
        String collaboratorCategory = get(cols, 9);

        if (taskName.isBlank()) {
            throw new IllegalArgumentException("TaskName is required");
        }

        PriorityLevel priorityLevel;
        try {
            priorityLevel = PriorityLevel.valueOf(priority.toLowerCase());
        } catch (Exception e) {
            priorityLevel = PriorityLevel.medium;
        }

        LocalDate parsedDueDate = null;
        if (!dueDate.isBlank()) {
            try {
                parsedDueDate = LocalDate.parse(dueDate);
            } catch (Exception e) {
                System.out.println("Invalid date format for task: "
                    + taskName + ", skipping date.");
            }
        }

        Task task = taskService.createTask(taskName, description, priorityLevel, parsedDueDate);

        try {
            taskService.changeTaskStatus(task.getTaskId(), TaskStatus.valueOf(status.toLowerCase()));
        } catch (Exception e) {
            taskService.changeTaskStatus(task.getTaskId(), TaskStatus.open);
        }

        if (!projectName.isBlank()) {
            Project project = taskService.createProject(projectName, projectDesc);
            taskService.assignTaskToProject(task.getTaskId(), project.getName());

            if (!collaboratorName.isBlank()) {
                CollaboratorCategory category = CollaboratorCategory.Junior;
                try {
                    category = CollaboratorCategory.valueOf(capitalize(collaboratorCategory));
                } catch (Exception ignored) {
                }

                Collaborator collaborator = taskService.createCollaborator(collaboratorName, category);
                taskService.assignCollaboratorToProject(
                    collaborator.getCollaboratorId(), project.getName());

                if (collaborator.canAcceptTask()) {
                    taskService.assignCollaboratorToTask(
                        task.getTaskId(),
                        subtaskName.isBlank() ? taskName : subtaskName,
                        collaborator.getCollaboratorId());
                } else {
                    System.out.println("Warning: collaborator "
                        + collaboratorName + " is at task limit, skipping link.");
                }
            }
        } else if (!subtaskName.isBlank()) {
            taskService.addSubtaskToTask(task.getTaskId(), subtaskName);
        }
    }

    private String[] splitCsvLine(String line) {
        return line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
    }

    private String get(String[] cols, int index) {
        if (index < 0 || index >= cols.length) {
            return "";
        }

        String value = cols[index].trim();
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1).replace("\"\"", "\"");
        }
        return value;
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.trim().toLowerCase();
        return Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
    }
}
