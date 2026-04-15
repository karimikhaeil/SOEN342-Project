package models;

import java.util.ArrayList;
import java.util.List;

public class Project {
    private String projectId;
    private String name;
    private String description;
    private List<Task> tasks;
    private List<Collaborator> collaborators;

    public Project(String projectId, String name) {
        this.projectId = projectId;
        this.name = name;
        this.tasks = new ArrayList<>();
        this.collaborators = new ArrayList<>();
    }

    public void addTask(Task task) {
        if (task != null && !tasks.contains(task)) {
            tasks.add(task);
        }
    }

    public void removeTask(Task task) {
        tasks.remove(task);
    }

    public void addCollaborator(Collaborator collaborator) {
        if (collaborator != null && !collaborators.contains(collaborator)) {
            collaborators.add(collaborator);
        }
    }

    public Collaborator findCollaboratorByName(String collaboratorName) {
        for (Collaborator collaborator : collaborators) {
            if (collaborator.getName().equalsIgnoreCase(collaboratorName)) {
                return collaborator;
            }
        }
        return null;
    }

    public String getProjectId() { return projectId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<Task> getTasks() { return tasks; }
    public List<Collaborator> getCollaborators() { return collaborators; }
}
