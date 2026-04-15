import gateway.ICalGatewayImpl;
import models.ActionType;
import models.ActivityEntry;
import models.Collaborator;
import models.CollaboratorCategory;
import models.PriorityLevel;
import models.Project;
import models.RecurrencePattern;
import models.RecurrenceType;
import models.Subtask;
import models.Tag;
import models.Task;
import models.TaskStatus;
import persistence.JsonTaskRepository;
import services.CSVExporter;
import services.CSVImporter;
import services.SearchCriteria;
import services.TaskService;

import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

public class Main {

    private static final String DATA_DIR = "data";

    private static TaskService taskService;
    private static CSVExporter csvExporter;
    private static CSVImporter csvImporter;
    private static final Scanner SCANNER = new Scanner(System.in);

    public static void main(String[] args) {
        JsonTaskRepository repository = new JsonTaskRepository(DATA_DIR);
        ICalGatewayImpl gateway = new ICalGatewayImpl();
        taskService = new TaskService(repository, gateway);
        csvExporter = new CSVExporter();
        csvImporter = new CSVImporter(taskService);

        try {
            taskService.load();
        } catch (Exception e) {
            System.out.println("Note: starting fresh (" + e.getMessage() + ")");
        }

        boolean running = true;
        while (running) {
            printMenu();
            String choice = prompt("Choice");

            try {
                switch (choice) {
                    case "1" -> listAllTasks();
                    case "2" -> createTaskMenu();
                    case "3" -> createProjectInteractive();
                    case "4" -> createCollaboratorInteractive();
                    case "5" -> manageTasksMenu();
                    case "6" -> manageProjectsMenu();
                    case "7" -> manageCollaboratorsMenu();
                    case "8" -> searchTasksInteractive();
                    case "9" -> viewTaskHistoryInteractive();
                    case "10" -> importTasksInteractive();
                    case "11" -> exportTasksMenu();
                    case "12" -> saveStateInteractive();
                    case "13" -> running = false;
                    default -> System.out.println("Unknown option. Please enter 1-13.");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }

            System.out.println();
        }

        try {
            taskService.save();
        } catch (Exception e) {
            System.err.println("Warning: could not save data: " + e.getMessage());
        }

        System.out.println("Goodbye.");
    }

    private static void printMenu() {
        System.out.println();
        System.out.println("=================================================");
        System.out.println("  Task Management System");
        System.out.println("=================================================");
        System.out.println("  1. List all tasks");
        System.out.println("  2. Create task");
        System.out.println("  3. Create project");
        System.out.println("  4. Create collaborator");
        System.out.println("  5. Manage tasks");
        System.out.println("  6. Manage projects");
        System.out.println("  7. Manage collaborators");
        System.out.println("  8. Search tasks");
        System.out.println("  9. View task activity history");
        System.out.println("  10. Import tasks from CSV");
        System.out.println("  11. Export tasks");
        System.out.println("  12. Save system state");
        System.out.println("  13. Exit");
        System.out.println("=================================================");
    }

    private static void createTaskMenu() {
        System.out.println("1. Regular task");
        System.out.println("2. Recurring task");
        String choice = prompt("Choice");
        if ("1".equals(choice)) {
            createTaskInteractive();
        } else if ("2".equals(choice)) {
            createRecurringTaskInteractive();
        } else {
            System.out.println("Invalid choice.");
        }
    }

    private static void manageTasksMenu() {
        System.out.println("1. Update task");
        System.out.println("2. Change task status");
        System.out.println("3. Reopen task");
        System.out.println("4. Manage tags");
        System.out.println("5. Manage subtasks");
        System.out.println("6. Assign collaborator to task");
        String choice = prompt("Choice");
        switch (choice) {
            case "1" -> updateTaskInteractive();
            case "2" -> changeTaskStatusInteractive();
            case "3" -> reopenTaskInteractive();
            case "4" -> manageTagsInteractive();
            case "5" -> addSubtaskInteractive();
            case "6" -> assignCollaboratorToTaskInteractive();
            default -> System.out.println("Invalid choice.");
        }
    }

    private static void manageProjectsMenu() {
        System.out.println("1. Assign task to project");
        System.out.println("2. Remove task from project");
        String choice = prompt("Choice");
        if ("1".equals(choice)) {
            assignTaskToProjectInteractive();
        } else if ("2".equals(choice)) {
            removeTaskFromProjectInteractive();
        } else {
            System.out.println("Invalid choice.");
        }
    }

    private static void manageCollaboratorsMenu() {
        System.out.println("1. View overloaded collaborators");
        String choice = prompt("Choice");
        if ("1".equals(choice)) {
            showOverloadedCollaborators();
        } else {
            System.out.println("Invalid choice.");
        }
    }

    private static void exportTasksMenu() {
        System.out.println("1. Export tasks to CSV");
        System.out.println("2. Export single task to iCal");
        System.out.println("3. Export project to iCal");
        System.out.println("4. Export filtered tasks to iCal");
        String choice = prompt("Choice");
        switch (choice) {
            case "1" -> exportCsvInteractive();
            case "2" -> exportICalSingleTask();
            case "3" -> exportICalProject();
            case "4" -> exportICalFiltered();
            default -> System.out.println("Invalid choice.");
        }
    }

    private static void listAllTasks() {
        List<Task> allTasks = taskService.getAllTasks();
        if (allTasks.isEmpty()) {
            System.out.println("No tasks.");
            return;
        }

        for (int i = 0; i < allTasks.size(); i++) {
            Task task = allTasks.get(i);
            System.out.printf("[%d] %s | %s | %s | due: %s | project: %s%n",
                i + 1,
                task.getTitle(),
                task.getStatus(),
                task.getPriorityLevel(),
                task.getDueDate() != null ? task.getDueDate() : "none",
                task.getProject() != null ? task.getProject().getName() : "-");

            if (task.getDescription() != null && !task.getDescription().isBlank()) {
                System.out.println("     desc: " + task.getDescription());
            }

            if (!task.getTags().isEmpty()) {
                System.out.print("     tags: ");
                for (int j = 0; j < task.getTags().size(); j++) {
                    System.out.print(task.getTags().get(j).getName());
                    if (j < task.getTags().size() - 1) System.out.print(", ");
                }
                System.out.println();
            }

            for (Subtask subtask : task.getSubtasks()) {
                String assigned = subtask.getAssignedTo() != null
                    ? subtask.getAssignedTo().getName()
                    : "unassigned";
                System.out.printf("     -> [%s] %s (%s)%n",
                    subtask.getStatus(),
                    subtask.getTitle(),
                    assigned);
            }
        }
    }

    private static void createTaskInteractive() {
        String title = prompt("Title");
        String description = blankToNull(prompt("Description (optional)"));
        PriorityLevel priority = parsePriority(promptWithDefault(
            "Priority (low/medium/high)", "medium"));
        LocalDate dueDate = parseOptionalDate(prompt(
            "Due date (YYYY-MM-DD, blank = none)"));

        Task task = taskService.createTask(title, description, priority, dueDate);
        System.out.println("Created task: " + task.getTitle());
    }

    private static void createRecurringTaskInteractive() {
        String title = prompt("Title");
        String description = blankToNull(prompt("Description (optional)"));
        PriorityLevel priority = parsePriority(promptWithDefault(
            "Priority (low/medium/high)", "medium"));
        RecurrenceType type = RecurrenceType.valueOf(
            prompt("Recurrence type (daily/weekly/monthly)").toLowerCase());
        LocalDate startDate = LocalDate.parse(prompt("Start date (YYYY-MM-DD)"));
        LocalDate endDate = LocalDate.parse(prompt("End date (YYYY-MM-DD)"));
        int interval = Integer.parseInt(prompt("Interval"));

        RecurrencePattern pattern = new RecurrencePattern(
            UUID.randomUUID().toString(),
            type,
            startDate,
            endDate,
            interval
        );

        Task task = taskService.createRecurringTask(title, description, priority, pattern);
        System.out.println("Created recurring task: " + task.getTitle());
        System.out.println("Occurrences generated: " + task.getOccurrences().size());
    }

    private static void updateTaskInteractive() {
        Task task = selectTask();
        String newTitle = promptWithDefault("New title", task.getTitle());
        String currentDescription = task.getDescription() == null ? "" : task.getDescription();
        String newDescription = promptWithDefault("New description", currentDescription);
        PriorityLevel priority = parsePriority(promptWithDefault(
            "New priority (low/medium/high)", task.getPriorityLevel().name()));
        String currentDueDate = task.getDueDate() == null ? "" : task.getDueDate().toString();
        LocalDate dueDate = parseOptionalDate(promptWithDefault(
            "New due date (YYYY-MM-DD, blank = none)", currentDueDate));

        taskService.updateTask(
            task.getTaskId(),
            newTitle,
            blankToNull(newDescription),
            priority,
            dueDate
        );
        System.out.println("Task updated.");
    }

    private static void changeTaskStatusInteractive() {
        Task task = selectTask();
        TaskStatus status = TaskStatus.valueOf(
            prompt("New status (open/completed/cancelled)").toLowerCase());
        taskService.changeTaskStatus(task.getTaskId(), status);
        System.out.println("Task status updated.");
    }

    private static void reopenTaskInteractive() {
        Task task = selectTask();
        taskService.reopenTask(task.getTaskId());
        System.out.println("Task reopened.");
    }

    private static void manageTagsInteractive() {
        Task task = selectTask();
        System.out.println("1. Add tag");
        System.out.println("2. Remove tag");
        String choice = prompt("Choice");

        if ("1".equals(choice)) {
            String tagName = prompt("Tag name");
            taskService.addTagToTask(task.getTaskId(), tagName);
            System.out.println("Tag added.");
        } else if ("2".equals(choice)) {
            String tagName = prompt("Tag name");
            taskService.removeTagFromTask(task.getTaskId(), tagName);
            System.out.println("Tag removed.");
        }
    }

    private static void addSubtaskInteractive() {
        Task task = selectTask();
        String title = prompt("Subtask title");
        System.out.println("1. Add plain subtask");
        System.out.println("2. Add subtask and assign collaborator");
        String choice = prompt("Choice");
        if ("1".equals(choice)) {
            taskService.addSubtaskToTask(task.getTaskId(), title);
            System.out.println("Subtask added.");
        } else {
            Collaborator collaborator = chooseCollaboratorForTask(task, true);
            taskService.assignCollaboratorToTask(
                task.getTaskId(),
                title,
                collaborator.getCollaboratorId()
            );
            System.out.println("Subtask added and collaborator assigned.");
        }
    }

    private static void createProjectInteractive() {
        String name = prompt("Project name");
        Project project = taskService.createProject(
            name,
            blankToNull(prompt("Project description (optional)"))
        );
        System.out.println("Project ready: " + project.getName());
    }

    private static void assignTaskToProjectInteractive() {
        Task task = selectTask();
        String projectName = prompt("Project name");
        String description = blankToNull(prompt("Project description (optional)"));
        taskService.createProject(projectName, description);
        taskService.assignTaskToProject(task.getTaskId(), projectName);
        System.out.println("Task assigned to project.");
    }

    private static void removeTaskFromProjectInteractive() {
        Task task = selectTask();
        taskService.removeTaskFromProject(task.getTaskId());
        System.out.println("Task removed from project.");
    }

    private static void createCollaboratorInteractive() {
        String name = prompt("Collaborator name");
        CollaboratorCategory category = parseCategory(
            prompt("Category (Senior/Intermediate/Junior)"));
        String projectName = prompt("Project name");
        taskService.createProject(projectName, null);
        Collaborator collaborator = taskService.createCollaborator(name, category);
        taskService.assignCollaboratorToProject(collaborator.getCollaboratorId(), projectName);
        System.out.println("Collaborator created.");
    }

    private static void assignCollaboratorToTaskInteractive() {
        Task task = selectTask();
        Collaborator collaborator = chooseCollaboratorForTask(task, false);
        String subtaskTitle = prompt("Subtask title");
        taskService.assignCollaboratorToTask(
            task.getTaskId(),
            subtaskTitle,
            collaborator.getCollaboratorId()
        );
        System.out.println("Collaborator assigned through subtask.");
    }

    private static void searchTasksInteractive() {
        SearchCriteria criteria = new SearchCriteria();
        String keyword = prompt("Keyword (blank allowed)");
        String status = prompt("Status (open/completed/cancelled, blank allowed)");
        String startDate = prompt("Start date (YYYY-MM-DD, blank allowed)");
        String endDate = prompt("End date (YYYY-MM-DD, blank allowed)");

        if (!keyword.isBlank()) criteria.setTitleKeyword(keyword);
        if (!status.isBlank()) criteria.setStatus(TaskStatus.valueOf(status.toLowerCase()));
        if (!startDate.isBlank()) criteria.setStartDate(LocalDate.parse(startDate));
        if (!endDate.isBlank()) criteria.setEndDate(LocalDate.parse(endDate));

        List<Task> results = taskService.searchTasks(
            keyword.isBlank() && status.isBlank() && startDate.isBlank() && endDate.isBlank()
                ? null
                : criteria
        );
        System.out.println("Results (" + results.size() + "):");
        for (Task task : results) {
            System.out.printf("  - %s | %s | due: %s%n",
                task.getTitle(),
                task.getStatus(),
                task.getDueDate() != null ? task.getDueDate() : "none");
        }
    }

    private static void viewTaskHistoryInteractive() {
        Task task = selectTask();
        List<ActivityEntry> history = taskService.getTaskActivityHistory(task.getTaskId());
        if (history.isEmpty()) {
            System.out.println("No activity entries.");
            return;
        }
        for (ActivityEntry entry : history) {
            System.out.println(entry.getTimestamp() + " - " + entry.getActionType());
        }
    }

    private static void importTasksInteractive() {
        String filePath = prompt("CSV file path");
        try {
            int count = csvImporter.importTasks(filePath);
            System.out.println("Imported " + count + " task(s).");
        } catch (Exception e) {
            System.out.println("Import failed: " + e.getMessage());
        }
    }

    private static void exportCsvInteractive() {
        String filePath = promptWithDefault("Output CSV file", "output.csv");
        try {
            csvExporter.export(taskService.getAllTasks(), filePath);
        } catch (Exception e) {
            System.out.println("CSV export failed: " + e.getMessage());
        }
    }

    private static void exportICalSingleTask() {
        Task task = selectTask();
        String filePath = promptWithDefault("Output ICS file", "export_task.ics");
        try {
            taskService.exportTaskToICal(task, filePath);
        } catch (Exception e) {
            System.out.println("Export failed: " + e.getMessage());
        }
    }

    private static void exportICalProject() {
        System.out.println("Projects: " + taskService.getProjectRegistry().keySet());
        String projectName = prompt("Project name");
        String filePath = promptWithDefault("Output ICS file", "export_project.ics");
        try {
            taskService.exportProjectToICal(projectName, filePath);
        } catch (Exception e) {
            System.out.println("Export failed: " + e.getMessage());
        }
    }

    private static void exportICalFiltered() {
        SearchCriteria criteria = new SearchCriteria();
        String keyword = prompt("Keyword (blank allowed)");
        String status = prompt("Status (open/completed/cancelled, blank allowed)");
        String from = prompt("Due date from (YYYY-MM-DD, blank allowed)");
        String to = prompt("Due date to (YYYY-MM-DD, blank allowed)");

        if (!keyword.isBlank()) criteria.setTitleKeyword(keyword);
        if (!status.isBlank()) criteria.setStatus(TaskStatus.valueOf(status.toLowerCase()));
        if (!from.isBlank()) criteria.setStartDate(LocalDate.parse(from));
        if (!to.isBlank()) criteria.setEndDate(LocalDate.parse(to));

        String filePath = promptWithDefault("Output ICS file", "export_filtered.ics");
        try {
            taskService.exportFilteredToICal(criteria, filePath);
        } catch (Exception e) {
            System.out.println("Export failed: " + e.getMessage());
        }
    }

    private static void showOverloadedCollaborators() {
        List<Collaborator> overloaded = taskService.listOverloadedCollaborators();
        if (overloaded.isEmpty()) {
            System.out.println("No overloaded collaborators.");
            return;
        }

        System.out.printf("%-20s %-14s %6s %6s%n", "Name", "Category", "Open", "Limit");
        System.out.println("-".repeat(54));
        for (Collaborator collaborator : overloaded) {
            System.out.printf("%-20s %-14s %6d %6d%n",
                collaborator.getName(),
                collaborator.getCategory(),
                collaborator.getOpenTaskCount(),
                collaborator.getOpenTaskLimit());
        }
    }

    private static void saveStateInteractive() {
        try {
            taskService.save();
        } catch (Exception e) {
            System.out.println("Save failed: " + e.getMessage());
        }
    }

    private static Task selectTask() {
        listAllTasks();
        List<Task> tasks = taskService.getAllTasks();
        if (tasks.isEmpty()) {
            throw new IllegalStateException("No tasks available.");
        }
        int index = Integer.parseInt(prompt("Task number")) - 1;
        if (index < 0 || index >= tasks.size()) {
            throw new IllegalArgumentException("Invalid task number.");
        }
        return tasks.get(index);
    }

    private static Collaborator chooseCollaboratorForTask(Task task, boolean allowPromptToCreate) {
        if (task.getProject() == null) {
            throw new IllegalStateException("Task must belong to a project before assigning collaborators.");
        }

        List<Collaborator> collaborators = new java.util.ArrayList<>(task.getProject().getCollaborators());
        if (collaborators.isEmpty()) {
            if (!allowPromptToCreate) {
                throw new IllegalStateException("No collaborators available in this task's project.");
            }
            System.out.println("No collaborators in project. Create one now.");
            String name = prompt("Collaborator name");
            CollaboratorCategory category = parseCategory(
                prompt("Category (Senior/Intermediate/Junior)"));
            Collaborator collaborator = taskService.createCollaborator(name, category);
            taskService.assignCollaboratorToProject(
                collaborator.getCollaboratorId(),
                task.getProject().getName()
            );
            return collaborator;
        }

        for (int i = 0; i < collaborators.size(); i++) {
            Collaborator collaborator = collaborators.get(i);
            System.out.printf("%d. %s (%s)%n",
                i + 1,
                collaborator.getName(),
                collaborator.getCategory());
        }
        int index = Integer.parseInt(prompt("Collaborator number")) - 1;
        if (index < 0 || index >= collaborators.size()) {
            throw new IllegalArgumentException("Invalid collaborator number.");
        }
        return collaborators.get(index);
    }

    private static PriorityLevel parsePriority(String value) {
        return PriorityLevel.valueOf(value.trim().toLowerCase());
    }

    private static CollaboratorCategory parseCategory(String value) {
        String normalized = value.trim().toLowerCase();
        normalized = Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
        return CollaboratorCategory.valueOf(normalized);
    }

    private static LocalDate parseOptionalDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDate.parse(value);
    }

    private static String prompt(String label) {
        System.out.print(label + ": ");
        return SCANNER.nextLine().trim();
    }

    private static String promptWithDefault(String label, String defaultValue) {
        System.out.print(label + " [" + defaultValue + "]: ");
        String value = SCANNER.nextLine().trim();
        return value.isBlank() ? defaultValue : value;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
