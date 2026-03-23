package models;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RecurrencePattern {
    private String patternId;
    private RecurrenceType type;
    private int interval;
    private LocalDate startDate;
    private LocalDate endDate;

    public RecurrencePattern(String patternId, RecurrenceType type,
                             LocalDate startDate, LocalDate endDate,
                             int interval) {
        this.patternId = patternId;
        this.type      = type;
        this.startDate = startDate;
        this.endDate   = endDate;
        this.interval  = interval;
    }

    public List<TaskOccurrence> generateOccurrences(String baseTitle) {
        List<TaskOccurrence> occurrences = new ArrayList<>();
        LocalDate current = startDate;
        int count = 1;

        while (!current.isAfter(endDate)) {
            String id = patternId + "-occ" + count++;
            occurrences.add(new TaskOccurrence(id, baseTitle, current));
            switch (type) {
                case daily:   current = current.plusDays(interval);   break;
                case weekly:  current = current.plusWeeks(interval);  break;
                case monthly: current = current.plusMonths(interval); break;
                default:      current = current.plusDays(interval);   break;
            }
        }
        return occurrences;
    }

    public String getPatternId()    { return patternId; }
    public RecurrenceType getType() { return type; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate()   { return endDate; }
    public int getInterval()        { return interval; }
}
