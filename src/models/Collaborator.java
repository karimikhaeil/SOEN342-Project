package models;

public class Collaborator {
    private String collaboratorId;
    private String name;
    private CollaboratorCategory category;
    private int openTaskCount;

    // OCL Constraint 3: limits must be positive integers
    private static final int SENIOR_LIMIT       = 2;
    private static final int INTERMEDIATE_LIMIT = 5;
    private static final int JUNIOR_LIMIT       = 10;

    public Collaborator(String collaboratorId, String name,
                        CollaboratorCategory category) {
        this.collaboratorId = collaboratorId;
        this.name           = name;
        this.category       = category;
        this.openTaskCount  = 0;
    }

    public int getOpenTaskLimit() {
        switch (category) {
            case Senior:       return SENIOR_LIMIT;
            case Intermediate: return INTERMEDIATE_LIMIT;
            case Junior:       return JUNIOR_LIMIT;
            default:           return JUNIOR_LIMIT;
        }
    }

    public boolean canAcceptTask() {
        return openTaskCount < getOpenTaskLimit();
    }

    /** OCL Constraint 4: true when this collaborator is overloaded. */
    public boolean isOverloaded() {
        return openTaskCount > getOpenTaskLimit();
    }

    public void incrementOpenTasks() { openTaskCount++; }

    public void decrementOpenTasks() {
        if (openTaskCount > 0) openTaskCount--;
    }

    public String getCollaboratorId()             { return collaboratorId; }
    public String getName()                       { return name; }
    public CollaboratorCategory getCategory()     { return category; }
    public void setCategory(CollaboratorCategory c) { this.category = c; }
    public int getOpenTaskCount()                 { return openTaskCount; }
    public void setOpenTaskCount(int count)       { this.openTaskCount = count; }
}
