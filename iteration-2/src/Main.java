import models.*;
import services.*;
import java.time.LocalDate;
import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        printSection("Task Management System");

        TaskService taskService = new TaskService();
        CSVImporter importer    = new CSVImporter(taskService);
        CSVExporter exporter    = new CSVExporter();

        printSection("1. Creating Manual Tasks");
        Task t1 = taskService.createTask(
            "Fix login bug", "Critical auth issue",
            PriorityLevel.high,
            LocalDate.of(2025, 6, 10));

        Task t2 = taskService.createTask(
            "Write unit tests", "Cover all service methods",
            PriorityLevel.medium,
            LocalDate.of(2025, 6, 15));

        Task t3 = taskService.createTask(
            "Update documentation", null,
            PriorityLevel.low,
            LocalDate.of(2025, 6, 20));

        printTaskSummary(t1);
        printTaskSummary(t2);
        printTaskSummary(t3);

        printSection("2. Creating a Recurring Task");
        RecurrencePattern pattern = new RecurrencePattern(
            "rp-001",
            RecurrenceType.weekly,
            LocalDate.of(2025, 6, 1),
            LocalDate.of(2025, 7, 1),
            1
        );
        Task recurring = taskService.createRecurringTask(
            "Weekly standup", "Team sync",
            PriorityLevel.low, pattern);
        System.out.println("Created recurring task: " + recurring.getTitle());
        System.out.println("Occurrences generated: "
            + recurring.getOccurrences().size());

        printSection("3. Importing Tasks from CSV");
        createSampleCSV("tasks.csv");
        int importedCount = importer.importTasks("tasks.csv");
        System.out.println("Imported rows: " + importedCount);

        printSection("4. Search Results - Open Tasks");
        List<Task> results = taskService.searchTasks(null);
        for (Task task : results) {
            printTaskSummary(task);
        }

        printSection("5. Search Results - Keyword 'test'");
        SearchCriteria criteria = new SearchCriteria();
        criteria.setTitleKeyword("test");
        List<Task> filtered = taskService.searchTasks(criteria);
        for (Task task : filtered) {
            System.out.println("- " + task.getTitle());
        }

        printSection("6. Exporting Tasks");
        exporter.export(taskService.getAllTasks(), "output.csv");
    }

    private static void createSampleCSV(String path) throws Exception {
        try (java.io.PrintWriter pw =
                new java.io.PrintWriter(new java.io.FileWriter(path))) {
            pw.println("TaskName,Description,Subtask,Status,Priority,"
                     + "DueDate,ProjectName,ProjectDescription,"
                     + "Collaborator,CollaboratorCategory");
            pw.println("Deploy backend,Push to prod,Production rollout,open,high,"
                     + "2025-06-12,Project Alpha,Main backend project,"
                     + "Alice,Senior");
            pw.println("Design mockups,UI screens,Landing page draft,open,medium,"
                     + "2025-06-18,Project Alpha,Main backend project,"
                     + "Bob,Junior");
            pw.println("Database migration,Move to new schema,,open,high,"
                     + "2025-06-08,Project Beta,Data migration project,,");
        }
        System.out.println("Created demo CSV file: " + path);
    }

    private static void printSection(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }

    private static void printTaskSummary(Task task) {
        System.out.println("- " + task.getTitle()
            + " | status: " + task.getStatus()
            + " | priority: " + task.getPriorityLevel()
            + " | due: " + task.getDueDate());
    }
}
