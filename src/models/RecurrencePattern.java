package models;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class RecurrencePattern {
    private String patternId;
    private RecurrenceType type;
    private int interval;
    private LocalDate startDate;
    private LocalDate endDate;

    public RecurrencePattern(String patternId, RecurrenceType type,
                             LocalDate startDate, LocalDate endDate,
                             int interval) {
        if (patternId == null || patternId.isBlank()) {
            throw new IllegalArgumentException("Pattern id is required");
        }
        if (type == null) {
            throw new IllegalArgumentException("Recurrence type is required");
        }
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start and end dates are required");
        }
        if (interval <= 0) {
            throw new IllegalArgumentException("Recurrence interval must be positive");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }

        this.patternId = patternId;
        this.type = type;
        this.startDate = startDate;
        this.endDate = endDate;
        this.interval = interval;
    }

    public List<TaskOccurrence> generateOccurrences(String baseTitle) {
        List<TaskOccurrence> occurrences = new ArrayList<>();
        LocalDate current = startDate;
        int count = 1;

        while (!current.isAfter(endDate)) {
            String id = patternId + "-occ" + count++;
            occurrences.add(new TaskOccurrence(id, baseTitle, current));
            switch (type) {
                case daily -> current = current.plusDays(interval);
                case weekly -> current = current.plusWeeks(interval);
                case monthly -> current = current.plusMonths(interval);
                default -> current = current.plusDays(interval);
            }
        }
        return occurrences;
    }

    public String getPatternId() { return patternId; }
    public RecurrenceType getType() { return type; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public int getInterval() { return interval; }
}
