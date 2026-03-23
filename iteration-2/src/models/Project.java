package models;

import java.util.ArrayList;
import java.util.List;

public class Project {
    private String projectId;
    private String name;
    private String description;
    private List<Collaborator> collaborators;

    public Project(String projectId, String name) {
        this.projectId = projectId;
        this.name = name;
        this.collaborators = new ArrayList<>();
    }

    public void addCollaborator(Collaborator collaborator) {
        collaborators.add(collaborator);
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
    public List<Collaborator> getCollaborators() { return collaborators; }
}
