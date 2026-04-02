import models.*;
import services.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

public class InteractiveUI {

    private final TaskService taskService;
    private final PersistenceManager persistence;
    private final Scanner scanner;
    private final List<Collaborator> collaborators;

    public InteractiveUI() {
        this.taskService = new TaskService();
        this.persistence = new PersistenceManager();
        this.scanner = new Scanner(System.in);
        this.collaborators = new ArrayList<>();
    }

    public void start() {
        printHeader();
        boolean running = true;

        while (running) {
            printMainMenu();
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> createTask();
                case "2" -> viewAllTasks();
                case "3" -> searchTasks();
                case "4" -> addSubtaskToTask();
                case "5" -> createCollaborator();
                case "6" -> viewAllCollaborators();
                case "7" -> exportTasks();
                case "8" -> checkOverloadedCollaborators();
                case "9" -> saveState();
                case "10" -> running = false;
                default -> System.out.println("Invalid option. Please try again.");
            }
        }

        System.out.println("Goodbye.");
        scanner.close();
    }

    private void printHeader() {
        System.out.println();
        System.out.println("======================================");
        System.out.println(" Task Management System - Interactive ");
        System.out.println("======================================");
    }

    private void printMainMenu() {
        System.out.println();
        System.out.println("1. Create New Task");
        System.out.println("2. View All Tasks");
        System.out.println("3. Search Tasks");
        System.out.println("4. Add Subtask to Task");
        System.out.println("5. Create Collaborator");
        System.out.println("6. View All Collaborators");
        System.out.println("7. Export Tasks");
        System.out.println("8. Check Overloaded Collaborators");
        System.out.println("9. Save System State");
        System.out.println("10. Exit");
        System.out.print("Select option (1-10): ");
    }

    private void createTask() {
        printSection("Create New Task");

        System.out.print("Task Title: ");
        String title = scanner.nextLine().trim();
        if (title.isEmpty()) {
            System.out.println("Title cannot be empty.");
            return;
        }

        System.out.print("Description (optional): ");
        String description = scanner.nextLine().trim();

        System.out.print("Priority (high/medium/low) [default: medium]: ");
        String priorityInput = scanner.nextLine().trim().toLowerCase();
        PriorityLevel priority = PriorityLevel.medium;
        if (!priorityInput.isEmpty()) {
            try {
                priority = PriorityLevel.valueOf(priorityInput);
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid priority. Using medium.");
            }
        }

        System.out.print("Due Date (yyyy-MM-dd) (optional): ");
        String dueDateInput = scanner.nextLine().trim();
        LocalDate dueDate = null;
        if (!dueDateInput.isEmpty()) {
            try {
                dueDate = LocalDate.parse(dueDateInput);
            } catch (Exception e) {
                System.out.println("Invalid date format. Task created without due date.");
            }
        }

        try {
            Task task = taskService.createTask(title, description, priority, dueDate);
            System.out.println("Task created successfully.");
            printTaskSummary(task);
        } catch (IllegalStateException e) {
            System.out.println(e.getMessage());
        }
    }

    private void viewAllTasks() {
        printSection("All Tasks");

        List<Task> tasks = taskService.getAllTasks();
        if (tasks.isEmpty()) {
            System.out.println("No tasks found.");
            return;
        }

        for (int i = 0; i < tasks.size(); i++) {
            Task task = tasks.get(i);
            System.out.println((i + 1) + ". " + task.getTitle());
            printTaskSummary(task);
            if (!task.getSubtasks().isEmpty()) {
                System.out.println("   Subtasks:");
                for (Subtask subtask : task.getSubtasks()) {
                    System.out.println("   - " + subtask.getTitle()
                        + " [" + subtask.getStatus() + "]"
                        + (subtask.getAssignedTo() != null
                            ? " -> " + subtask.getAssignedTo().getName()
                            : ""));
                }
            }
            System.out.println();
        }
    }

    private void searchTasks() {
        printSection("Search Tasks");

        System.out.print("Keyword: ");
        String keyword = scanner.nextLine().trim();

        SearchCriteria criteria = new SearchCriteria();
        criteria.setTitleKeyword(keyword);
        List<Task> results = taskService.searchTasks(criteria);

        if (results.isEmpty()) {
            System.out.println("No tasks found matching '" + keyword + "'.");
            return;
        }

        System.out.println("Found " + results.size() + " task(s):");
        for (Task task : results) {
            System.out.println("- " + task.getTitle() + " (" + task.getStatus() + ")");
        }
    }

    private void addSubtaskToTask() {
        printSection("Add Subtask To Task");

        List<Task> tasks = taskService.getAllTasks();
        if (tasks.isEmpty()) {
            System.out.println("No tasks available. Create a task first.");
            return;
        }

        Task selectedTask = chooseTask(tasks, "Select task number");
        if (selectedTask == null) {
            return;
        }

        if (selectedTask.getSubtasks().size() >= 20) {
            System.out.println("This task already has the maximum of 20 subtasks.");
            return;
        }

        System.out.print("Subtask Title: ");
        String subtaskTitle = scanner.nextLine().trim();
        if (subtaskTitle.isEmpty()) {
            System.out.println("Subtask title cannot be empty.");
            return;
        }

        if (collaborators.isEmpty()) {
            System.out.println("No collaborators available. Create a collaborator first.");
            return;
        }

        Collaborator collaborator = chooseCollaborator();
        if (collaborator == null) {
            return;
        }

        try {
            Subtask subtask = selectedTask.addSubtaskForCollaborator(
                UUID.randomUUID().toString(),
                subtaskTitle,
                collaborator
            );
            System.out.println("Subtask added successfully.");
            System.out.println("Subtask: " + subtask.getTitle());
            System.out.println("Assigned to: " + collaborator.getName());
        } catch (IllegalStateException e) {
            System.out.println(e.getMessage());
        }
    }

    private void createCollaborator() {
        printSection("Create Collaborator");

        System.out.print("Collaborator Name: ");
        String name = scanner.nextLine().trim();
        if (name.isEmpty()) {
            System.out.println("Name cannot be empty.");
            return;
        }

        System.out.println("1. Senior (Max 2 open tasks)");
        System.out.println("2. Intermediate (Max 5 open tasks)");
        System.out.println("3. Junior (Max 10 open tasks)");
        System.out.print("Select category (1-3): ");

        String categoryChoice = scanner.nextLine().trim();
        CollaboratorCategory category = switch (categoryChoice) {
            case "1" -> CollaboratorCategory.Senior;
            case "2" -> CollaboratorCategory.Intermediate;
            default -> CollaboratorCategory.Junior;
        };

        Collaborator collaborator = new Collaborator(
            UUID.randomUUID().toString(),
            name,
            category
        );
        collaborators.add(collaborator);

        System.out.println("Collaborator created successfully.");
        System.out.println("Name: " + collaborator.getName());
        System.out.println("Category: " + collaborator.getCategory());
        System.out.println("Task limit: " + collaborator.getOpenTaskLimit());
    }

    private void viewAllCollaborators() {
        printSection("All Collaborators");

        if (collaborators.isEmpty()) {
            System.out.println("No collaborators found.");
            return;
        }

        for (int i = 0; i < collaborators.size(); i++) {
            Collaborator collaborator = collaborators.get(i);
            System.out.println((i + 1) + ". " + collaborator.getName());
            System.out.println("   Category: " + collaborator.getCategory());
            System.out.println("   Open tasks: " + collaborator.getOpenTaskCount()
                + " / " + collaborator.getOpenTaskLimit());
        }
    }

    private void exportTasks() {
        printSection("Export Tasks");

        List<Task> tasks = taskService.getAllTasks();
        if (tasks.isEmpty()) {
            System.out.println("No tasks available to export.");
            return;
        }

        System.out.println("1. Export all tasks to CSV");
        System.out.println("2. Export one task to iCal (.ics)");
        System.out.println("3. Export one project to iCal (.ics)");
        System.out.println("4. Export filtered tasks to iCal (.ics)");
        System.out.print("Choose export option (1-4): ");
        String choice = scanner.nextLine().trim();

        try {
            switch (choice) {
                case "1" -> exportCsv(tasks);
                case "2" -> exportSingleTaskToCalendar(tasks);
                case "3" -> exportProjectToCalendar(tasks);
                case "4" -> exportFilteredTasksToCalendar();
                default -> System.out.println("Invalid export option.");
            }
        } catch (Exception e) {
            System.out.println("Export failed: " + e.getMessage());
        }
    }

    private void exportCsv(List<Task> tasks) throws Exception {
        CSVExporter exporter = new CSVExporter();
        exporter.export(tasks, "tasks_export.csv");
        System.out.println("Tasks exported to tasks_export.csv");
    }

    private void exportSingleTaskToCalendar(List<Task> tasks) throws Exception {
        List<Task> dueDatedTasks = tasks.stream()
            .filter(task -> task.getDueDate() != null)
            .toList();

        if (dueDatedTasks.isEmpty()) {
            System.out.println("No tasks with due dates are available for calendar export.");
            return;
        }

        Task task = chooseTask(dueDatedTasks, "Select due-dated task number");
        if (task == null) {
            return;
        }

        System.out.print("Output file name [task.ics]: ");
        String fileName = scanner.nextLine().trim();
        if (fileName.isEmpty()) {
            fileName = "task.ics";
        }

        CalendarExporter exporter = new CalendarExporter();
        exporter.exportTask(task, fileName);
    }

    private void exportProjectToCalendar(List<Task> tasks) throws Exception {
        List<String> projectNames = tasks.stream()
            .map(Task::getProject)
            .filter(project -> project != null)
            .map(Project::getName)
            .distinct()
            .toList();

        if (projectNames.isEmpty()) {
            System.out.println("No project-linked tasks are available for calendar export.");
            return;
        }

        for (int i = 0; i < projectNames.size(); i++) {
            System.out.println((i + 1) + ". " + projectNames.get(i));
        }
        System.out.print("Select project number: ");
        String input = scanner.nextLine().trim();

        int index;
        try {
            index = Integer.parseInt(input) - 1;
        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid number.");
            return;
        }

        if (index < 0 || index >= projectNames.size()) {
            System.out.println("Invalid project number.");
            return;
        }

        String projectName = projectNames.get(index);
        System.out.print("Output file name [project.ics]: ");
        String fileName = scanner.nextLine().trim();
        if (fileName.isEmpty()) {
            fileName = "project.ics";
        }

        CalendarExporter exporter = new CalendarExporter();
        exporter.exportProjectTasks(projectName, tasks, fileName);
    }

    private void exportFilteredTasksToCalendar() throws Exception {
        SearchCriteria criteria = new SearchCriteria();

        System.out.print("Keyword filter (optional): ");
        String keyword = scanner.nextLine().trim();
        if (!keyword.isEmpty()) {
            criteria.setTitleKeyword(keyword);
        }

        System.out.print("Status filter (open/completed/cancelled, optional): ");
        String status = scanner.nextLine().trim().toLowerCase();
        if (!status.isEmpty()) {
            try {
                criteria.setStatus(TaskStatus.valueOf(status));
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid status. Skipping status filter.");
            }
        }

        List<Task> filtered = taskService.searchTasks(criteria);
        if (filtered.isEmpty()) {
            System.out.println("No tasks matched the selected filters.");
            return;
        }

        System.out.print("Output file name [filtered.ics]: ");
        String fileName = scanner.nextLine().trim();
        if (fileName.isEmpty()) {
            fileName = "filtered.ics";
        }

        CalendarExporter exporter = new CalendarExporter();
        exporter.exportFilteredTasks(filtered, fileName);
    }

    private void checkOverloadedCollaborators() {
        printSection("Collaborator Workload Check");

        List<Collaborator> overloaded = taskService.getOverloadedCollaborators();
        if (overloaded.isEmpty()) {
            System.out.println("No overloaded collaborators.");
            System.out.println("Current assignment rules prevent overload during normal task assignment.");
            return;
        }

        System.out.println("Found " + overloaded.size() + " overloaded collaborator(s):");
        for (Collaborator collaborator : overloaded) {
            System.out.println("- " + collaborator.getName()
                + " (" + collaborator.getCategory() + "): "
                + collaborator.getOpenTaskCount() + " open tasks exceeds limit of "
                + collaborator.getOpenTaskLimit());
        }
    }

    private void saveState() {
        printSection("Save System State");

        try {
            persistence.saveState(taskService);
            System.out.println("System state saved to data.json");
        } catch (Exception e) {
            System.out.println("Save failed: " + e.getMessage());
        }
    }

    private Task chooseTask(List<Task> tasks, String prompt) {
        for (int i = 0; i < tasks.size(); i++) {
            Task task = tasks.get(i);
            System.out.println((i + 1) + ". " + task.getTitle()
                + (task.getDueDate() != null ? " (due " + task.getDueDate() + ")" : ""));
        }
        System.out.print(prompt + " (1-" + tasks.size() + "): ");

        String input = scanner.nextLine().trim();
        int index;
        try {
            index = Integer.parseInt(input) - 1;
        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid number.");
            return null;
        }

        if (index < 0 || index >= tasks.size()) {
            System.out.println("Invalid task number.");
            return null;
        }
        return tasks.get(index);
    }

    private Collaborator chooseCollaborator() {
        for (int i = 0; i < collaborators.size(); i++) {
            Collaborator collaborator = collaborators.get(i);
            System.out.println((i + 1) + ". " + collaborator.getName()
                + " (" + collaborator.getCategory() + ")"
                + " - open tasks: " + collaborator.getOpenTaskCount()
                + "/" + collaborator.getOpenTaskLimit());
        }
        System.out.print("Select collaborator (1-" + collaborators.size() + "): ");

        String input = scanner.nextLine().trim();
        int index;
        try {
            index = Integer.parseInt(input) - 1;
        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid number.");
            return null;
        }

        if (index < 0 || index >= collaborators.size()) {
            System.out.println("Invalid collaborator number.");
            return null;
        }
        return collaborators.get(index);
    }

    private void printTaskSummary(Task task) {
        System.out.println("   Status: " + task.getStatus());
        System.out.println("   Priority: " + task.getPriorityLevel());
        System.out.println("   Due Date: "
            + (task.getDueDate() != null ? task.getDueDate() : "No due date"));
        if (task.getDescription() != null && !task.getDescription().isEmpty()) {
            System.out.println("   Description: " + task.getDescription());
        }
    }

    private void printSection(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }

    public static void main(String[] args) {
        new InteractiveUI().start();
    }
}
