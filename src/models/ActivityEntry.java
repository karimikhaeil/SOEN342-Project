package models;

import java.time.LocalDateTime;

public class ActivityEntry {
    private String entryId;
    private ActionType actionType;
    private LocalDateTime timestamp;

    public ActivityEntry(String entryId, ActionType actionType) {
        this(entryId, actionType, LocalDateTime.now());
    }

    public ActivityEntry(String entryId, ActionType actionType, LocalDateTime timestamp) {
        this.entryId = entryId;
        this.actionType = actionType;
        this.timestamp = timestamp;
    }

    public String getEntryId() { return entryId; }
    public ActionType getActionType() { return actionType; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
