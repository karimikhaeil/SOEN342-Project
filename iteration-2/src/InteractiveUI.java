import models.*;
import services.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

/**
 * Interactive UI for Task Management System
 * Concordia University Colors: Burgundy (#800020) and White (#FFFFFF)
 */
public class InteractiveUI {
    
    private TaskService taskService;
    private PersistenceManager persistence;
    private Scanner scanner;
    private List<Collaborator> collaborators;  // Store collaborators
    
    // ANSI Color codes for Concordia colors
    private static final String BURGUNDY = "\u001B[38;5;52m";  // Dark red/burgundy
    private static final String WHITE = "\u001B[97m";           // Bright white
    private static final String RESET = "\u001B[0m";
    private static final String BOLD = "\u001B[1m";
    
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
                case "1":
                    createTask();
                    break;
                case "2":
                    viewAllTasks();
                    break;
                case "3":
                    searchTasks();
                    break;
                case "4":
                    addSubtaskToTask();
                    break;
                case "5":
                    createCollaborator();
                    break;
                case "6":
                    viewAllCollaborators();
                    break;
                case "7":
                    exportTasks();
                    break;
                case "8":
                    checkOverloadedCollaborators();
                    break;
                case "9":
                    saveState();
                    break;
                case "10":
                    running = false;
                    printGoodbye();
                    break;
                default:
                    printError("Invalid option. Please try again.");
            }
        }
        scanner.close();
    }
    
    private void printHeader() {
        clearScreen();
        printBurgundy("╔════════════════════════════════════════════════════╗");
        printBurgundy("║");
        printBurgundy("║  ");
        System.out.print(BURGUNDY + BOLD + "TASK MANAGEMENT SYSTEM" + RESET);
        printBurgundy("                    ║");
        printBurgundy("║  ");
        System.out.print(BURGUNDY + "Concordia University" + RESET);
        printBurgundy("                         ║");
        printBurgundy("║");
        printBurgundy("╚════════════════════════════════════════════════════╝");
        println();
    }
    
    private void printMainMenu() {
        printBurgundy("\n╭─ MAIN MENU ─────────────────────────────────────╮");
        println("│");
        println("│  1. Create New Task");
        println("│  2. View All Tasks");
        println("│  3. Search Tasks");
        println("│  4. Add Subtask to Task");
        println("│  5. Create Collaborator");
        println("│  6. View All Collaborators");
        println("│  7. Export Tasks (CSV)");
        println("│  8. Check Overloaded Collaborators");
        println("│  9. Save System State");
        println("│  10. Exit");
        println("│");
        printBurgundy("╰──────────────────────────────────────────────────╯");
        print("\nSelect option (1-10): ");
    }
    
    private void createTask() {
        clearSection();
        printBurgundy("╭─ CREATE NEW TASK ───────────────────────────────╮");
        println("│");
        
        print("│ Task Title: ");
        String title = scanner.nextLine().trim();
        if (title.isEmpty()) {
            printError("Title cannot be empty.");
            return;
        }
        
        print("│ Description (optional): ");
        String description = scanner.nextLine().trim();
        
        print("│ Priority (high/medium/low) [default: medium]: ");
        String priorityStr = scanner.nextLine().trim().toLowerCase();
        PriorityLevel priority = PriorityLevel.medium;
        try {
            if (!priorityStr.isEmpty()) {
                priority = PriorityLevel.valueOf(priorityStr);
            }
        } catch (IllegalArgumentException e) {
            printWarning("Invalid priority. Using medium.");
        }
        
        print("│ Due Date (yyyy-MM-dd) (optional): ");
        String dateStr = scanner.nextLine().trim();
        LocalDate dueDate = null;
        if (!dateStr.isEmpty()) {
            try {
                dueDate = LocalDate.parse(dateStr);
            } catch (Exception e) {
                printWarning("Invalid date format. Task created without due date.");
            }
        }
        
        try {
            Task task = taskService.createTask(title, description, priority, dueDate);
            println("│");
            printSuccess("✓ Task created successfully!");
            printTaskSummary(task);
        } catch (IllegalStateException e) {
            printError(e.getMessage());
        }
        
        println("│");
        printBurgundy("╰──────────────────────────────────────────────────╯");
    }
    
    private void viewAllTasks() {
        clearSection();
        printBurgundy("╭─ ALL TASKS ─────────────────────────────────────╮");
        
        List<Task> tasks = taskService.getAllTasks();
        
        if (tasks.isEmpty()) {
            println("│");
            printWarning("│ No tasks found.");
            println("│");
        } else {
            println("│");
            for (int i = 0; i < tasks.size(); i++) {
                print("│ ");
                System.out.print(BOLD + (i + 1) + ". " + RESET);
                Task task = tasks.get(i);
                String status = task.getStatus().toString().toUpperCase();
                String priority = task.getPriorityLevel().toString().toUpperCase();
                String dueDate = task.getDueDate() != null ? task.getDueDate().toString() : "No due date";
                
                println(task.getTitle());
                println("│    Status: " + status + " | Priority: " + priority);
                println("│    Due: " + dueDate);
                if (task.getDescription() != null && !task.getDescription().isEmpty()) {
                    println("│    Desc: " + task.getDescription());
                }
                
                // Show subtasks if any
                if (!task.getSubtasks().isEmpty()) {
                    println("│    Subtasks (" + task.getSubtasks().size() + "):");
                    for (Subtask subtask : task.getSubtasks()) {
                        print("│      • ");
                        println(subtask.getTitle() + " [" + subtask.getStatus() + "]");
                        if (subtask.getAssignedTo() != null) {
                            println("│        Assigned to: " + subtask.getAssignedTo().getName());
                        }
                    }
                }
                println("│");
            }
        }
        
        printBurgundy("╰──────────────────────────────────────────────────╯");
    }
    
    private void searchTasks() {
        clearSection();
        printBurgundy("╭─ SEARCH TASKS ──────────────────────────────────╮");
        println("│");
        
        print("│ Search by keyword: ");
        String keyword = scanner.nextLine().trim();
        
        println("│");
        
        SearchCriteria criteria = new SearchCriteria();
        criteria.setTitleKeyword(keyword);
        List<Task> results = taskService.searchTasks(criteria);
        
        if (results.isEmpty()) {
            printWarning("│ No tasks found matching '" + keyword + "'");
        } else {
            System.out.print(BURGUNDY + "│ Found " + RESET);
            System.out.print(BOLD + results.size() + RESET);
            println(" task(s):");
            println("│");
            
            for (int i = 0; i < results.size(); i++) {
                print("│ ");
                System.out.print(BOLD + (i + 1) + ". " + RESET);
                Task task = results.get(i);
                println(task.getTitle() + " (" + task.getStatus() + ")");
            }
        }
        
        println("│");
        printBurgundy("╰──────────────────────────────────────────────────╯");
    }
    
    private void addSubtaskToTask() {
        clearSection();
        printBurgundy("╭─ ADD SUBTASK TO TASK ──────────────────────────╮");
        println("│");
        
        List<Task> tasks = taskService.getAllTasks();
        if (tasks.isEmpty()) {
            printWarning("│ No tasks available. Create a task first.");
            println("│");
            printBurgundy("╰──────────────────────────────────────────────────╯");
            return;
        }
        
        // Show available tasks
        println("│ Available Tasks:");
        println("│");
        for (int i = 0; i < tasks.size(); i++) {
            print("│ ");
            System.out.print(BOLD + (i + 1) + ". " + RESET);
            println(tasks.get(i).getTitle());
        }
        
        println("│");
        print("│ Select task number (1-" + tasks.size() + "): ");
        
        String taskChoice = scanner.nextLine().trim();
        int taskIndex = -1;
        try {
            taskIndex = Integer.parseInt(taskChoice) - 1;
            if (taskIndex < 0 || taskIndex >= tasks.size()) {
                printError("Invalid task number.");
                return;
            }
        } catch (NumberFormatException e) {
            printError("Please enter a valid number.");
            return;
        }
        
        Task selectedTask = tasks.get(taskIndex);
        
        // Check if task already has 20 subtasks
        if (selectedTask.getSubtasks().size() >= 20) {
            printError("This task already has the maximum of 20 subtasks.");
            println("│");
            printBurgundy("╰──────────────────────────────────────────────────╯");
            return;
        }
        
        println("│");
        print("│ Subtask Title: ");
        String subtaskTitle = scanner.nextLine().trim();
        if (subtaskTitle.isEmpty()) {
            printError("Subtask title cannot be empty.");
            return;
        }
        
        // Select collaborator
        if (collaborators.isEmpty()) {
            printWarning("│ No collaborators available. Create a collaborator first.");
            println("│");
            printBurgundy("╰──────────────────────────────────────────────────╯");
            return;
        }
        
        println("│");
        println("│ Assign to Collaborator:");
        println("│");
        for (int i = 0; i < collaborators.size(); i++) {
            print("│ ");
            System.out.print(BOLD + (i + 1) + ". " + RESET);
            Collaborator collab = collaborators.get(i);
            println(collab.getName() + " (" + collab.getCategory() + ")");
        }
        
        println("│");
        print("│ Select collaborator (1-" + collaborators.size() + "): ");
        
        String collabChoice = scanner.nextLine().trim();
        int collabIndex = -1;
        try {
            collabIndex = Integer.parseInt(collabChoice) - 1;
            if (collabIndex < 0 || collabIndex >= collaborators.size()) {
                printError("Invalid collaborator number.");
                return;
            }
        } catch (NumberFormatException e) {
            printError("Please enter a valid number.");
            return;
        }
        
        Collaborator selectedCollab = collaborators.get(collabIndex);
        
        // Create subtask
        try {
            String subtaskId = UUID.randomUUID().toString();
            Subtask subtask = selectedTask.addSubtaskForCollaborator(
                subtaskId, 
                subtaskTitle, 
                selectedCollab
            );
            
            println("│");
            printSuccess("✓ Subtask added successfully!");
            println("│  Subtask: " + subtask.getTitle());
            println("│  Assigned to: " + selectedCollab.getName());
            println("│  Task now has: " + selectedTask.getSubtasks().size() + " subtask(s)");
        } catch (IllegalStateException e) {
            println("│");
            printError(e.getMessage());
        }
        
        println("│");
        printBurgundy("╰──────────────────────────────────────────────────╯");
    }
    
    private void createCollaborator() {
        clearSection();
        printBurgundy("╭─ CREATE COLLABORATOR ──────────────────────────╮");
        println("│");
        
        print("│ Collaborator Name: ");
        String name = scanner.nextLine().trim();
        if (name.isEmpty()) {
            printError("Name cannot be empty.");
            return;
        }
        
        println("│");
        println("│ Category:");
        println("│   1. Senior (Max 2 open tasks)");
        println("│   2. Intermediate (Max 5 open tasks)");
        println("│   3. Junior (Max 10 open tasks)");
        print("│ Select category (1-3): ");
        
        String categoryChoice = scanner.nextLine().trim();
        CollaboratorCategory category = CollaboratorCategory.Junior;
        
        switch (categoryChoice) {
            case "1":
                category = CollaboratorCategory.Senior;
                break;
            case "2":
                category = CollaboratorCategory.Intermediate;
                break;
            case "3":
                category = CollaboratorCategory.Junior;
                break;
            default:
                printWarning("Invalid category. Using Junior.");
                category = CollaboratorCategory.Junior;
        }
        
        String collabId = UUID.randomUUID().toString();
        Collaborator collaborator = new Collaborator(collabId, name, category);
        collaborators.add(collaborator);
        
        println("│");
        printSuccess("✓ Collaborator created successfully!");
        println("│  Name: " + collaborator.getName());
        println("│  Category: " + collaborator.getCategory());
        println("│  Task Limit: " + collaborator.getOpenTaskLimit());
        
        println("│");
        printBurgundy("╰──────────────────────────────────────────────────╯");
    }
    
    private void viewAllCollaborators() {
        clearSection();
        printBurgundy("╭─ ALL COLLABORATORS ─────────────────────────────╮");
        
        if (collaborators.isEmpty()) {
            println("│");
            printWarning("│ No collaborators found.");
            println("│");
        } else {
            println("│");
            for (int i = 0; i < collaborators.size(); i++) {
                print("│ ");
                System.out.print(BOLD + (i + 1) + ". " + RESET);
                Collaborator collab = collaborators.get(i);
                println(collab.getName());
                println("│    Category: " + collab.getCategory());
                println("│    Task Limit: " + collab.getOpenTaskLimit());
                println("│    Current Open Tasks: " + collab.getOpenTaskCount());
                println("│");
            }
        }
        
        printBurgundy("╰──────────────────────────────────────────────────╯");
    }
    
    private void exportTasks() {
        clearSection();
        printBurgundy("╭─ EXPORT TASKS ──────────────────────────────────╮");
        println("│");
        
        try {
            CSVExporter exporter = new CSVExporter();
            List<Task> tasks = taskService.getAllTasks();
            
            if (tasks.isEmpty()) {
                printWarning("│ No tasks to export.");
            } else {
                exporter.export(tasks, "tasks_export.csv");
                println("│");
                printSuccess("✓ Tasks exported to tasks_export.csv");
            }
        } catch (Exception e) {
            printError("Export failed: " + e.getMessage());
        }
        
        println("│");
        printBurgundy("╰──────────────────────────────────────────────────╯");
    }
    
    private void checkOverloadedCollaborators() {
        clearSection();
        printBurgundy("╭─ COLLABORATOR WORKLOAD CHECK ──────────────────╮");
        println("│");
        
        List<Collaborator> overloaded = taskService.getOverloadedCollaborators();
        
        if (overloaded.isEmpty()) {
            println("│");
            printSuccess("✓ All collaborators have manageable workload");
        } else {
            System.out.print(BURGUNDY + "│ ⚠ Found " + RESET);
            System.out.print(BOLD + overloaded.size() + RESET);
            println(" overloaded collaborator(s):");
            println("│");
            
            for (Collaborator c : overloaded) {
                print("│ • ");
                System.out.print(BOLD + c.getName() + RESET);
                println(" (" + c.getCategory() + ")");
                println("│   Open tasks: " + c.getOpenTaskCount() + " / Limit: " + c.getOpenTaskLimit());
            }
        }
        
        println("│");
        printBurgundy("╰──────────────────────────────────────────────────╯");
    }
    
    private void saveState() {
        clearSection();
        printBurgundy("╭─ SAVE SYSTEM STATE ─────────────────────────────╮");
        println("│");
        
        try {
            persistence.saveState(taskService);
            println("│");
            printSuccess("✓ System state saved successfully");
            println("│   File: data.json");
        } catch (Exception e) {
            printError("Save failed: " + e.getMessage());
        }
        
        println("│");
        printBurgundy("╰──────────────────────────────────────────────────╯");
    }
    
    private void printGoodbye() {
        clearScreen();
        printBurgundy("╔════════════════════════════════════════════════════╗");
        printBurgundy("║");
        println("║  Thank you for using Task Management System!");
        println("║  Goodbye!");
        printBurgundy("║");
        printBurgundy("╚════════════════════════════════════════════════════╝");
    }
    
    // Helper methods for styling
    private void printBurgundy(String text) {
        System.out.println(BURGUNDY + text + RESET);
    }
    
    private void printSuccess(String text) {
        System.out.println(BURGUNDY + text + RESET);
    }
    
    private void printError(String text) {
        System.out.println(BURGUNDY + "✗ ERROR: " + text + RESET);
    }
    
    private void printWarning(String text) {
        System.out.println(BURGUNDY + "⚠ " + text + RESET);
    }
    
    private void println(String text) {
        System.out.println(WHITE + text + RESET);
    }
    
    private void println() {
        System.out.println();
    }
    
    private void print(String text) {
        System.out.print(WHITE + text + RESET);
    }
    
    private void printTaskSummary(Task task) {
        println("│");
        println("│  Title: " + task.getTitle());
        if (task.getDescription() != null && !task.getDescription().isEmpty()) {
            println("│  Description: " + task.getDescription());
        }
        println("│  Priority: " + task.getPriorityLevel());
        println("│  Due Date: " + (task.getDueDate() != null ? task.getDueDate() : "No due date"));
    }
    
    private void clearSection() {
        for (int i = 0; i < 3; i++) {
            System.out.println();
        }
    }
    
    private void clearScreen() {
        // Clear screen (works on Windows PowerShell and Unix)
        try {
            if (System.getProperty("os.name").contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (Exception e) {
            // Fallback: just print some newlines
            for (int i = 0; i < 50; i++) {
                System.out.println();
            }
        }
    }
    
    public static void main(String[] args) {
        InteractiveUI ui = new InteractiveUI();
        ui.start();
    }
}
