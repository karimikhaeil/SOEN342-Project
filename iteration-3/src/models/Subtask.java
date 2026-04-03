package models;

public class Subtask {
    private String subtaskId;
    private String title;
    private SubtaskStatus status;
    private Collaborator assignedTo;

    public Subtask(String subtaskId, String title) {
        this.subtaskId = subtaskId;
        this.title     = title;
        this.status    = SubtaskStatus.open;
    }

    public void assignCollaborator(Collaborator collaborator) {
        this.assignedTo = collaborator;
    }

    public void reassignCollaborator(Collaborator collaborator) {
        if (collaborator != null && !collaborator.canAcceptTask()) {
            throw new IllegalStateException(
                "Collaborator " + collaborator.getName()
                + " has reached their open task limit of "
                + collaborator.getOpenTaskLimit());
        }
        if (this.assignedTo != null) this.assignedTo.decrementOpenTasks();
        this.assignedTo = collaborator;
        if (collaborator != null) collaborator.incrementOpenTasks();
    }

    public void complete() {
        if (this.status == SubtaskStatus.completed) return;
        this.status = SubtaskStatus.completed;
        if (assignedTo != null) assignedTo.decrementOpenTasks();
    }

    public void setStatus(SubtaskStatus status) {
        if (status == SubtaskStatus.completed) { complete(); return; }
        this.status = status;
    }

    /**
     * Sets status directly without side-effects on collaborator counts.
     * Used only by the persistence layer during deserialisation.
     */
    public void forceStatus(SubtaskStatus status) {
        this.status = status;
    }

    public String getSubtaskId()          { return subtaskId; }
    public String getTitle()              { return title; }
    public void setTitle(String title)    { this.title = title; }
    public SubtaskStatus getStatus()      { return status; }
    public Collaborator getAssignedTo()   { return assignedTo; }
}
