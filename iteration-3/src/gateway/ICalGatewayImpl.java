package gateway;

import models.*;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Concrete Gateway that writes iCalendar (.ics) files conforming to RFC 5545.
 *
 * Implemented as plain text — no external library required.
 * To swap in iCal4j, simply write a new class that implements ICalGateway.
 * Zero changes needed anywhere else.
 *
 * Rules enforced:
 * - Only tasks with a due date are exported.
 * - Subtasks are NOT separate VEVENTs; they appear in the DESCRIPTION.
 * - Each VEVENT includes: SUMMARY, DESCRIPTION, DTSTART/DTEND,
 *   STATUS, PRIORITY, and CATEGORIES (project name).
 */
public class ICalGatewayImpl implements ICalGateway {

    private static final DateTimeFormatter ICAL_DATE =
        DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public void exportTask(Task task, String filePath) throws IOException {
        exportTasks(List.of(task), filePath);
    }

    @Override
    public void exportTasks(List<Task> tasks, String filePath)
            throws IOException {
        try (PrintWriter writer =
                new PrintWriter(new BufferedWriter(new FileWriter(filePath)))) {

            writer.println("BEGIN:VCALENDAR");
            writer.println("VERSION:2.0");
            writer.println("PRODID:-//SOEN342 Task Manager//EN");
            writer.println("CALSCALE:GREGORIAN");
            writer.println("METHOD:PUBLISH");

            int exported = 0;
            for (Task task : tasks) {
                if (task.getDueDate() == null) continue; // skip tasks with no due date
                writeVEvent(writer, task);
                exported++;
            }

            writer.println("END:VCALENDAR");
            System.out.println("iCal export complete: " + filePath
                + " (" + exported + " event(s) written)");
        }
    }

    private void writeVEvent(PrintWriter writer, Task task) {
        LocalDate due   = task.getDueDate();
        LocalDate dtend = due.plusDays(1); // all-day event: DTEND = day after

        writer.println("BEGIN:VEVENT");
        writer.println("UID:" + UUID.randomUUID() + "@soen342");
        writer.println("SUMMARY:" + fold(escape(task.getTitle())));
        writer.println("DTSTART;VALUE=DATE:" + due.format(ICAL_DATE));
        writer.println("DTEND;VALUE=DATE:"   + dtend.format(ICAL_DATE));
        writer.println("STATUS:" + toICalStatus(task.getStatus()));
        writer.println("PRIORITY:" + toICalPriority(task.getPriorityLevel()));

        if (task.getProject() != null) {
            writer.println("CATEGORIES:" + escape(task.getProject().getName()));
        }

        String desc = buildDescription(task);
        if (!desc.isBlank()) {
            writer.println("DESCRIPTION:" + fold(escape(desc)));
        }

        writer.println("END:VEVENT");
    }

    /**
     * Builds the DESCRIPTION: task description + subtask summary.
     * Subtasks are NOT written as separate VEVENTs.
     */
    private String buildDescription(Task task) {
        StringBuilder sb = new StringBuilder();

        if (task.getDescription() != null && !task.getDescription().isBlank()) {
            sb.append(task.getDescription());
        }

        List<Subtask> subtasks = task.getSubtasks();
        if (!subtasks.isEmpty()) {
            if (sb.length() > 0) sb.append("\\n\\n");
            sb.append("Subtasks (").append(subtasks.size()).append("):");
            for (Subtask st : subtasks) {
                sb.append("\\n- [")
                  .append(st.getStatus().name().toUpperCase())
                  .append("] ")
                  .append(st.getTitle());
                if (st.getAssignedTo() != null) {
                    sb.append(" (assigned: ")
                      .append(st.getAssignedTo().getName())
                      .append(")");
                }
            }
        }
        return sb.toString();
    }

    private String toICalStatus(TaskStatus status) {
        switch (status) {
            case completed: return "COMPLETED";
            case cancelled: return "CANCELLED";
            default:        return "NEEDS-ACTION";
        }
    }

    /** iCal PRIORITY: 1=high, 5=medium, 9=low */
    private String toICalPriority(PriorityLevel level) {
        switch (level) {
            case high:   return "1";
            case medium: return "5";
            case low:    return "9";
            default:     return "5";
        }
    }

    /** Escapes special characters per RFC 5545. */
    private String escape(String value) {
        if (value == null) return "";
        return value
            .replace("\\", "\\\\")
            .replace(";",  "\\;")
            .replace(",",  "\\,");
    }

    /** Folds long lines at 75 characters per RFC 5545 §3.1. */
    private String fold(String content) {
        if (content.length() <= 75) return content;
        StringBuilder sb = new StringBuilder();
        int pos = 0;
        while (pos < content.length()) {
            int end = Math.min(pos + 75, content.length());
            if (pos > 0) sb.append("\r\n ");
            sb.append(content, pos, end);
            pos = end;
        }
        return sb.toString();
    }
}
