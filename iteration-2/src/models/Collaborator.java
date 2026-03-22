package models;

public class Collaborator {
    private String collaboratorId;
    private String name;
    private CollaboratorCategory category;
    private int openTaskCount;

    public Collaborator(String collaboratorId, String name,
                        CollaboratorCategory category) {
        this.collaboratorId = collaboratorId;
        this.name           = name;
        this.category       = category;
        this.openTaskCount  = 0;
    }

    public int getOpenTaskLimit() {
        switch (category) {
            case Senior:       return 2;
            case Intermediate: return 5;
            case Junior:       return 10;
            default:           return 10;
        }
    }

    public boolean canAcceptTask() {
        return openTaskCount < getOpenTaskLimit();
    }

    public void incrementOpenTasks() { openTaskCount++; }

    public void decrementOpenTasks() {
        if (openTaskCount > 0) openTaskCount--;
    }

    public String getCollaboratorId()    { return collaboratorId; }
    public String getName()              { return name; }
    public CollaboratorCategory getCategory() { return category; }
    public void setCategory(CollaboratorCategory c) { this.category = c; }
    public int getOpenTaskCount()        { return openTaskCount; }
}
