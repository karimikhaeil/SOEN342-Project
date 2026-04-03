import gateway.ICalGatewayImpl;
import models.*;
import persistence.JsonTaskRepository;
import services.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

/**
 * Iteration 3 – Deployment and OCL.
 *
 * New features:
 *  - iCal export (single task / project / filtered list)
 *  - Overloaded-collaborators report (menu option 7)
 *  - Persistence (auto-load on start, auto-save on exit)
 */
public class Main {

    private static final String DATA_DIR = "data";

    private static TaskService  taskService;
    private static CSVImporter  importer;
    private static CSVExporter  exporter;
    private static Scanner      sc = new Scanner(System.in);

    public static void main(String[] args) {
        // Wire up persistence and iCal gateway
        JsonTaskRepository repo    = new JsonTaskRepository(DATA_DIR);
        ICalGatewayImpl    gateway = new ICalGatewayImpl();
        taskService = new TaskService(repo, gateway);
        importer    = new CSVImporter(taskService);
        exporter    = new CSVExporter();

        // Load persisted data from previous run
        try { taskService.load(); }
        catch (Exception e) {
            System.out.println("Note: starting fresh (" + e.getMessage() + ")");
        }

        // Seed sample data only when nothing was loaded
        if (taskService.getAllTasks().isEmpty()) {
            seedSampleData();
        }

        // Main menu loop
        boolean running = true;
        while (running) {
            printMenu();
            String choice = sc.nextLine().trim();
            switch (choice) {
                case "1": listAllTasks();           break;
                case "2": createTaskInteractive();  break;
                case "3": searchTasksInteractive(); break;
                case "4": exportICalSingleTask();   break;
                case "5": exportICalProject();      break;
                case "6": exportICalFiltered();     break;
                case "7": showOverloadedCollabs();  break;
                case "8": exportCSV();              break;
                case "9": running = false;          break;
                default:
                    System.out.println("Unknown option. Please enter 1-9.");
            }
        }

        // Auto-save on exit
        try { taskService.save(); }
        catch (Exception e) {
            System.err.println("Warning: could not save data: " + e.getMessage());
        }
        System.out.println("\nGoodbye!");
    }

    // Menu 

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

    // Option handlers 

    private static void listAllTasks() {
        List<Task> all = taskService.getAllTasks();
        if (all.isEmpty()) { System.out.println("No tasks."); return; }
        System.out.println("\n-- All Tasks --");
        for (int i = 0; i < all.size(); i++) {
            Task t = all.get(i);
            System.out.printf("[%d] %s | %s | %s | due: %s%n",
                i + 1, t.getTitle(), t.getStatus(), t.getPriorityLevel(),
                t.getDueDate() != null ? t.getDueDate() : "none");
            for (Subtask s : t.getSubtasks()) {
                System.out.printf("     -> [%s] %s%n", s.getStatus(), s.getTitle());
            }
        }
    }

    private static void createTaskInteractive() {
        System.out.print("Title: ");
        String title = sc.nextLine().trim();
        System.out.print("Description (optional): ");
        String desc = sc.nextLine().trim();
        System.out.print("Priority (low/medium/high) [medium]: ");
        String pStr = sc.nextLine().trim();
        PriorityLevel priority;
        try { priority = PriorityLevel.valueOf(pStr.isBlank() ? "medium" : pStr.toLowerCase()); }
        catch (Exception e) { priority = PriorityLevel.medium; }
        System.out.print("Due date (YYYY-MM-DD, blank = none): ");
        String dateStr = sc.nextLine().trim();
        LocalDate dueDate = null;
        if (!dateStr.isBlank()) {
            try { dueDate = LocalDate.parse(dateStr); }
            catch (Exception e) { System.out.println("Invalid date, skipping."); }
        }
        try {
            Task t = taskService.createTask(
                title, desc.isBlank() ? null : desc, priority, dueDate);
            System.out.println("Created: " + t.getTitle());
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void searchTasksInteractive() {
        System.out.print("Keyword (blank = all open tasks): ");
        String kw = sc.nextLine().trim();
        SearchCriteria criteria = null;
        if (!kw.isBlank()) {
            criteria = new SearchCriteria();
            criteria.setTitleKeyword(kw);
        }
        List<Task> results = taskService.searchTasks(criteria);
        System.out.println("Results (" + results.size() + "):");
        for (Task t : results) {
            System.out.printf("  - %s | due: %s%n",
                t.getTitle(),
                t.getDueDate() != null ? t.getDueDate() : "none");
        }
    }

    private static void exportICalSingleTask() {
        listAllTasks();
        List<Task> all = taskService.getAllTasks();
        if (all.isEmpty()) return;
        System.out.print("Enter task number: ");
        try {
            int idx = Integer.parseInt(sc.nextLine().trim()) - 1;
            Task task = all.get(idx);
            System.out.print("Output file [export_task.ics]: ");
            String file = sc.nextLine().trim();
            if (file.isBlank()) file = "export_task.ics";
            taskService.exportTaskToICal(task, file);
        } catch (Exception e) {
            System.out.println("Export failed: " + e.getMessage());
        }
    }

    private static void exportICalProject() {
        System.out.println("Projects: " + taskService.getProjectRegistry().keySet());
        System.out.print("Project name: ");
        String name = sc.nextLine().trim();
        System.out.print("Output file [export_project.ics]: ");
        String file = sc.nextLine().trim();
        if (file.isBlank()) file = "export_project.ics";
        try {
            taskService.exportProjectToICal(name, file);
        } catch (Exception e) {
            System.out.println("Export failed: " + e.getMessage());
        }
    }

    private static void exportICalFiltered() {
        System.out.print("Due date from (YYYY-MM-DD, blank = none): ");
        String from = sc.nextLine().trim();
        System.out.print("Due date to   (YYYY-MM-DD, blank = none): ");
        String to   = sc.nextLine().trim();
        System.out.print("Only open tasks? (y/n) [y]: ");
        String openOnly = sc.nextLine().trim();

        SearchCriteria c = new SearchCriteria();
        if (!from.isBlank()) { try { c.setStartDate(LocalDate.parse(from)); } catch (Exception ignored) {} }
        if (!to.isBlank())   { try { c.setEndDate(LocalDate.parse(to));     } catch (Exception ignored) {} }
        if (!"n".equalsIgnoreCase(openOnly)) c.setStatus(TaskStatus.open);

        System.out.print("Output file [export_filtered.ics]: ");
        String file = sc.nextLine().trim();
        if (file.isBlank()) file = "export_filtered.ics";
        try {
            taskService.exportFilteredToICal(c, file);
        } catch (Exception e) {
            System.out.println("Export failed: " + e.getMessage());
        }
    }

    /** Menu option 7 – lists overloaded collaborators. */
    private static void showOverloadedCollabs() {
        List<Collaborator> overloaded = taskService.listOverloadedCollaborators();
        System.out.println("\n-- Overloaded Collaborators --");
        if (overloaded.isEmpty()) {
            System.out.println("  None. All collaborators are within their limits.");
        } else {
            System.out.printf("  %-20s %-14s %6s %6s%n",
                "Name", "Category", "Open", "Limit");
            System.out.println("  " + "-".repeat(50));
            for (Collaborator c : overloaded) {
                System.out.printf("  %-20s %-14s %6d %6d  *** OVERLOADED%n",
                    c.getName(), c.getCategory(),
                    c.getOpenTaskCount(), c.getOpenTaskLimit());
            }
        }
    }

    private static void exportCSV() {
        System.out.print("Output file [output.csv]: ");
        String file = sc.nextLine().trim();
        if (file.isBlank()) file = "output.csv";
        try { exporter.export(taskService.getAllTasks(), file); }
        catch (Exception e) { System.out.println("CSV export failed: " + e.getMessage()); }
    }

    // Sample data

    private static void seedSampleData() {
        System.out.println("Creating sample data for first run...");

        Task t1 = taskService.createTask("Fix login bug",
            "Critical auth issue", PriorityLevel.high,
            LocalDate.of(2026, 5, 10));

        Task t2 = taskService.createTask("Write unit tests",
            "Cover all service methods", PriorityLevel.medium,
            LocalDate.of(2026, 5, 15));

        taskService.createTask("Update documentation",
            "Refresh API docs", PriorityLevel.low,
            LocalDate.of(2026, 5, 20));

        // Set up a project with collaborators
        Project alpha = taskService.resolveProject("Project Alpha");
        alpha.setDescription("Main backend project");

        Collaborator alice = new Collaborator(
            UUID.randomUUID().toString(), "Alice", CollaboratorCategory.Senior);
        alpha.addCollaborator(alice);
        t1.setProject(alpha);
        t1.addSubtaskForCollaborator(
            UUID.randomUUID().toString(), "Reproduce auth failure", alice);

        // Bob is Senior (limit=2) but has 3 tasks → demonstrates overload detection
        Collaborator bob = new Collaborator(
            UUID.randomUUID().toString(), "Bob", CollaboratorCategory.Senior);
        alpha.addCollaborator(bob);
        t2.setProject(alpha);
        bob.incrementOpenTasks();
        bob.incrementOpenTasks();
        bob.incrementOpenTasks(); // 3 > limit of 2 → overloaded

        System.out.println("Sample data ready.");
    }
}
```

---

## Step 4 — Also copy the unchanged model files

You need to copy these 9 files **exactly as they are** from `iteration-2/src/models/` into `iteration-3/src/models/`. In GitHub you do this by:

1. Open the file in `iteration-2/src/models/`
2. Click the **copy raw file** button (or select all and copy)
3. Go back to your repo, make sure you're on `iteration-3`
4. Click **Add file → Create new file**
5. Name it `iteration-3/src/models/FileName.java`
6. Paste and commit

Do this for each of these files (don't change anything):
- `ActionType.java`
- `ActivityEntry.java`
- `CollaboratorCategory.java`
- `PriorityLevel.java`
- `Project.java`
- `RecurrencePattern.java`
- `RecurrenceType.java`
- `Tag.java`
- `Task.java`
- `TaskOccurrence.java`
- `TaskStatus.java`
- `SubtaskStatus.java`

Also copy these 3 service files unchanged from `iteration-2/src/services/`:
- `CSVExporter.java`
- `CSVImporter.java`
- `SearchCriteria.java`

---

## Step 5 — Verify your branch looks right

After all commits, your `iteration-3` branch should show this under `iteration-3/src/`:
```
gateway/      ← 2 new files
models/       ← 14 files (12 copied + Collaborator + Subtask updated)
persistence/  ← 2 new files
services/     ← 4 files (3 copied + TaskService updated)
Main.java     ← new interactive menu
