import gateway.ICalGatewayImpl;
import models.Collaborator;
import models.CollaboratorCategory;
import models.PriorityLevel;
import models.Project;
import models.RecurrencePattern;
import models.RecurrenceType;
import models.Subtask;
import models.Task;
import models.TaskStatus;
import persistence.JsonTaskRepository;
import services.CSVExporter;
import services.TaskService;

import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

public class Main {

    private static final String DATA_DIR = "data";

    private static TaskService taskService;
    private static CSVExporter exporter;
    private static final Scanner SCANNER = new Scanner(System.in);

    public static void main(String[] args) {
        JsonTaskRepository repository = new JsonTaskRepository(DATA_DIR);
        ICalGatewayImpl gateway = new ICalGatewayImpl();
        taskService = new TaskService(repository, gateway);
        exporter = new CSVExporter();

        try {
            taskService.load();
        } catch (Exception e) {
            System.out.println("Note: starting fresh (" + e.getMessage() + ")");
        }

        if (taskService.getAllTasks().isEmpty()) {
            seedSampleData();
        }

        boolean running = true;
        while (running) {
            printMenu();
            String choice = SCANNER.nextLine().trim();
            switch (choice) {
                case "1" -> listAllTasks();
                case "2" -> createTaskInteractive();
                case "3" -> searchTasksInteractive();
                case "4" -> exportICalSingleTask();
                case "5" -> exportICalProject();
                case "6" -> exportICalFiltered();
                case "7" -> showOverloadedCollaborators();
                case "8" -> exportCsv();
                case "9" -> running = false;
                default -> System.out.println("Unknown option. Please enter 1-9.");
            }
        }

        try {
            taskService.save();
        } catch (Exception e) {
            System.err.println("Warning: could not save data: " + e.getMessage());
        }

        System.out.println();
        System.out.println("Goodbye.");
    }

    private static void printMenu() {
        System.out.println();
        System.out.println("===================================");
        System.out.println("  Task Management System - Iter 3 ");
        System.out.println("===================================");
        System.out.println("  1. List all tasks");
        System.out.println("  2. Create a task");
        System.out.println("  3. Search tasks");
        System.out.println("  4. Export single task to iCal");
        System.out.println("  5. Export project to iCal");
        System.out.println("  6. Export filtered tasks to iCal");
        System.out.println("  7. List overloaded collaborators");
        System.out.println("  8. Export all tasks to CSV");
        System.out.println("  9. Exit (saves data)");
        System.out.println("===================================");
        System.out.print("Choice: ");
    }

    private static void listAllTasks() {
        List<Task> allTasks = taskService.getAllTasks();
        if (allTasks.isEmpty()) {
            System.out.println("No tasks.");
            return;
        }

        System.out.println();
        System.out.println("-- All Tasks --");
        for (int i = 0; i < allTasks.size(); i++) {
            Task task = allTasks.get(i);
            System.out.printf("[%d] %s | %s | %s | due: %s%n",
                i + 1,
                task.getTitle(),
                task.getStatus(),
                task.getPriorityLevel(),
                task.getDueDate() != null ? task.getDueDate() : "none");
            for (Subtask subtask : task.getSubtasks()) {
                System.out.printf("     -> [%s] %s%n",
                    subtask.getStatus(),
                    subtask.getTitle());
            }
        }
    }

    private static void createTaskInteractive() {
        System.out.print("Title: ");
        String title = SCANNER.nextLine().trim();

        System.out.print("Description (optional): ");
        String description = SCANNER.nextLine().trim();

        System.out.print("Priority (low/medium/high) [medium]: ");
        String priorityInput = SCANNER.nextLine().trim().toLowerCase();
        PriorityLevel priority;
        try {
            priority = PriorityLevel.valueOf(
                priorityInput.isBlank() ? "medium" : priorityInput
            );
        } catch (Exception e) {
            priority = PriorityLevel.medium;
        }

        System.out.print("Due date (YYYY-MM-DD, blank = none): ");
        String dateInput = SCANNER.nextLine().trim();
        LocalDate dueDate = null;
        if (!dateInput.isBlank()) {
            try {
                dueDate = LocalDate.parse(dateInput);
            } catch (Exception e) {
                System.out.println("Invalid date, skipping.");
            }
        }

        try {
            Task task = taskService.createTask(
                title,
                description.isBlank() ? null : description,
                priority,
                dueDate
            );
            System.out.println("Created: " + task.getTitle());
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void searchTasksInteractive() {
        System.out.print("Keyword (blank = all open tasks): ");
        String keyword = SCANNER.nextLine().trim();

        services.SearchCriteria criteria = null;
        if (!keyword.isBlank()) {
            criteria = new services.SearchCriteria();
            criteria.setTitleKeyword(keyword);
        }

        List<Task> results = taskService.searchTasks(criteria);
        System.out.println("Results (" + results.size() + "):");
        for (Task task : results) {
            System.out.printf("  - %s | due: %s%n",
                task.getTitle(),
                task.getDueDate() != null ? task.getDueDate() : "none");
        }
    }

    private static void exportICalSingleTask() {
        listAllTasks();
        List<Task> allTasks = taskService.getAllTasks();
        if (allTasks.isEmpty()) {
            return;
        }

        System.out.print("Enter task number: ");
        try {
            int index = Integer.parseInt(SCANNER.nextLine().trim()) - 1;
            Task task = allTasks.get(index);
            System.out.print("Output file [export_task.ics]: ");
            String filePath = SCANNER.nextLine().trim();
            if (filePath.isBlank()) {
                filePath = "export_task.ics";
            }
            taskService.exportTaskToICal(task, filePath);
        } catch (Exception e) {
            System.out.println("Export failed: " + e.getMessage());
        }
    }

    private static void exportICalProject() {
        System.out.println("Projects: " + taskService.getProjectRegistry().keySet());
        System.out.print("Project name: ");
        String projectName = SCANNER.nextLine().trim();
        System.out.print("Output file [export_project.ics]: ");
        String filePath = SCANNER.nextLine().trim();
        if (filePath.isBlank()) {
            filePath = "export_project.ics";
        }

        try {
            taskService.exportProjectToICal(projectName, filePath);
        } catch (Exception e) {
            System.out.println("Export failed: " + e.getMessage());
        }
    }

    private static void exportICalFiltered() {
        System.out.print("Due date from (YYYY-MM-DD, blank = none): ");
        String from = SCANNER.nextLine().trim();
        System.out.print("Due date to   (YYYY-MM-DD, blank = none): ");
        String to = SCANNER.nextLine().trim();
        System.out.print("Only open tasks? (y/n) [y]: ");
        String openOnly = SCANNER.nextLine().trim();

        services.SearchCriteria criteria = new services.SearchCriteria();
        if (!from.isBlank()) {
            try {
                criteria.setStartDate(LocalDate.parse(from));
            } catch (Exception ignored) {
            }
        }
        if (!to.isBlank()) {
            try {
                criteria.setEndDate(LocalDate.parse(to));
            } catch (Exception ignored) {
            }
        }
        if (!"n".equalsIgnoreCase(openOnly)) {
            criteria.setStatus(TaskStatus.open);
        }

        System.out.print("Output file [export_filtered.ics]: ");
        String filePath = SCANNER.nextLine().trim();
        if (filePath.isBlank()) {
            filePath = "export_filtered.ics";
        }

        try {
            taskService.exportFilteredToICal(criteria, filePath);
        } catch (Exception e) {
            System.out.println("Export failed: " + e.getMessage());
        }
    }

    private static void showOverloadedCollaborators() {
        List<Collaborator> overloaded = taskService.listOverloadedCollaborators();
        System.out.println();
        System.out.println("-- Overloaded Collaborators --");
        if (overloaded.isEmpty()) {
            System.out.println("  None. All collaborators are within their limits.");
            return;
        }

        System.out.printf("  %-20s %-14s %6s %6s%n",
            "Name",
            "Category",
            "Open",
            "Limit");
        System.out.println("  " + "-".repeat(50));
        for (Collaborator collaborator : overloaded) {
            System.out.printf("  %-20s %-14s %6d %6d  *** OVERLOADED%n",
                collaborator.getName(),
                collaborator.getCategory(),
                collaborator.getOpenTaskCount(),
                collaborator.getOpenTaskLimit());
        }
    }

    private static void exportCsv() {
        System.out.print("Output file [output.csv]: ");
        String filePath = SCANNER.nextLine().trim();
        if (filePath.isBlank()) {
            filePath = "output.csv";
        }
        try {
            exporter.export(taskService.getAllTasks(), filePath);
        } catch (Exception e) {
            System.out.println("CSV export failed: " + e.getMessage());
        }
    }

    private static void seedSampleData() {
        System.out.println("Creating sample data for first run...");

        Task t1 = taskService.createTask(
            "Fix login bug",
            "Critical auth issue",
            PriorityLevel.high,
            LocalDate.of(2026, 5, 10)
        );

        Task t2 = taskService.createTask(
            "Write unit tests",
            "Cover all service methods",
            PriorityLevel.medium,
            LocalDate.of(2026, 5, 15)
        );

        taskService.createTask(
            "Update documentation",
            "Refresh API docs",
            PriorityLevel.low,
            LocalDate.of(2026, 5, 20)
        );

        Project alpha = taskService.resolveProject("Project Alpha");
        alpha.setDescription("Main backend project");

        Collaborator alice = new Collaborator(
            UUID.randomUUID().toString(),
            "Alice",
            CollaboratorCategory.Senior
        );
        alpha.addCollaborator(alice);
        t1.setProject(alpha);
        t1.addSubtaskForCollaborator(
            UUID.randomUUID().toString(),
            "Reproduce auth failure",
            alice
        );

        Collaborator bob = new Collaborator(
            UUID.randomUUID().toString(),
            "Bob",
            CollaboratorCategory.Senior
        );
        alpha.addCollaborator(bob);
        t2.setProject(alpha);
        bob.incrementOpenTasks();
        bob.incrementOpenTasks();
        bob.incrementOpenTasks();

        RecurrencePattern pattern = new RecurrencePattern(
            "rp-001",
            RecurrenceType.weekly,
            LocalDate.of(2026, 5, 1),
            LocalDate.of(2026, 5, 29),
            1
        );
        taskService.createRecurringTask(
            "Weekly standup",
            "Team sync",
            PriorityLevel.low,
            pattern
        );

        System.out.println("Sample data ready.");
    }
}
