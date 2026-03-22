package services;

import models.*;
import java.io.*;
import java.util.List;

public class CSVExporter {

    private static final String HEADER =
        "TaskName,Description,Subtask,Status,Priority,"
      + "DueDate,ProjectName,ProjectDescription,"
      + "Collaborator,CollaboratorCategory";

    public void export(List<Task> tasks, String filePath) throws IOException {
        try (PrintWriter writer =
                new PrintWriter(new FileWriter(filePath))) {
            writer.println(HEADER);
            for (Task task : tasks) {
                if (task.getSubtasks().isEmpty()) {
                    writer.println(buildRow(task, null));
                } else {
                    for (Subtask sub : task.getSubtasks()) {
                        writer.println(buildRow(task, sub));
                    }
                }
            }
        }
        System.out.println("Export complete → " + filePath);
    }

    private String buildRow(Task task, Subtask subtask) {
        String projectName = "";
        String projectDesc = "";
        if (task.getProject() != null) {
            projectName = escape(task.getProject().getName());
            projectDesc = escape(task.getProject().getDescription());
        }

        String subtaskTitle = "";
        String collaborator = "";
        String collaboratorCat = "";
        if (subtask != null) {
            subtaskTitle = escape(subtask.getTitle());
            if (subtask.getAssignedTo() != null) {
                collaborator    = escape(subtask.getAssignedTo().getName());
                collaboratorCat = subtask.getAssignedTo().getCategory().name();
            }
        }

        return escape(task.getTitle()) + ","
             + escape(task.getDescription()) + ","
             + subtaskTitle + ","
             + task.getStatus() + ","
             + task.getPriorityLevel() + ","
             + (task.getDueDate() != null ? task.getDueDate() : "") + ","
             + projectName + ","
             + projectDesc + ","
             + collaborator + ","
             + collaboratorCat;
    }

    private String escape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"")
                || value.contains("\n"))
            return "\"" + value.replace("\"", "\"\"") + "\"";
        return value;
    }
}
