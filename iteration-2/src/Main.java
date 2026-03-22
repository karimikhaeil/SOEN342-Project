import models.*;
import services.*;
import java.time.LocalDate;
import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {

        TaskService taskService = new TaskService();
        CSVImporter importer    = new CSVImporter(taskService);
        CSVExporter exporter    = new CSVExporter();

        // 1. Manually create some tasks 
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

        // 2. Create a recurring task
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
        System.out.println("Recurring task occurrences generated: "
            + recurring.getOccurrences().size());

        // 3. Import from CSV 
        // Creates tasks.csv first as a demo input file
        createSampleCSV("tasks.csv");
        importer.importTasks("tasks.csv");

        // 4. Search 
        System.out.println("\n── Search: all open tasks ──");
        List<Task> results = taskService.searchTasks(null);
        for (Task t : results) {
            System.out.println("  " + t.getTitle()
                + " | " + t.getStatus()
                + " | due: " + t.getDueDate());
        }

        // 5. Search with criteria 
        System.out.println("\n── Search: keyword 'test' ──");
        SearchCriteria criteria = new SearchCriteria();
        criteria.setTitleKeyword("test");
        List<Task> filtered = taskService.searchTasks(criteria);
        for (Task t : filtered) {
            System.out.println("  " + t.getTitle());
        }

        // 6. Export all tasks to CSV 
        exporter.export(taskService.getAllTasks(), "output.csv");
    }

    // Creates a sample CSV file to demonstrate import
    private static void createSampleCSV(String path) throws Exception {
        try (java.io.PrintWriter pw =
                new java.io.PrintWriter(new java.io.FileWriter(path))) {
            pw.println("TaskName,Description,Subtask,Status,Priority,"
                     + "DueDate,ProjectName,ProjectDescription,"
                     + "Collaborator,CollaboratorCategory");
            pw.println("Deploy backend,Push to prod,,open,high,"
                     + "2025-06-12,Project Alpha,Main backend project,"
                     + "Alice,Senior");
            pw.println("Design mockups,UI screens,,open,medium,"
                     + "2025-06-18,Project Alpha,Main backend project,"
                     + "Bob,Junior");
            pw.println("Database migration,Move to new schema,,open,high,"
                     + "2025-06-08,Project Beta,Data migration project,,");
        }
        System.out.println("tasks.csv created.");
    }
}
