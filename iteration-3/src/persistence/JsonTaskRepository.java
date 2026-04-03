package persistence;

import models.*;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

/**
 * File-based persistence using a hand-rolled JSON serialiser.
 * No external dependencies — compiles with plain javac.
 *
 * Stores data in two files under the configured data directory:
 *   tasks.json    – all tasks (subtasks embedded)
 *   projects.json – project registry (collaborators embedded)
 */
public class JsonTaskRepository implements TaskRepository {

    private final String dataDir;
    private static final String TASKS_FILE    = "tasks.json";
    private static final String PROJECTS_FILE = "projects.json";

    public JsonTaskRepository(String dataDir) {
        this.dataDir = dataDir;
        new File(dataDir).mkdirs();
    }

    // ── Save ─────────────────────────────────────────────────────────────

    @Override
    public void save(List<Task> tasks, Map<String, Project> projects)
            throws IOException {
        saveTasks(tasks);
        saveProjects(projects);
    }

    private void saveTasks(List<Task> tasks) throws IOException {
        StringBuilder sb = new StringBuilder("[\n");
        for (int i = 0; i < tasks.size(); i++) {
            sb.append(taskToJson(tasks.get(i)));
            if (i < tasks.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("]");
        write(TASKS_FILE, sb.toString());
    }

    private void saveProjects(Map<String, Project> projects) throws IOException {
        List<Project> list = new ArrayList<>(projects.values());
        StringBuilder sb = new StringBuilder("[\n");
        for (int i = 0; i < list.size(); i++) {
            sb.append(projectToJson(list.get(i)));
            if (i < list.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("]");
        write(PROJECTS_FILE, sb.toString());
    }

    // ── Load ─────────────────────────────────────────────────────────────

    @Override
    public List<Task> loadTasks() throws IOException {
        String path = dataDir + File.separator + TASKS_FILE;
        if (!new File(path).exists()) return new ArrayList<>();
        String json = new String(Files.readAllBytes(Paths.get(path)));
        return parseTasks(json);
    }

    @Override
    public Map<String, Project> loadProjects() throws IOException {
        String path = dataDir + File.separator + PROJECTS_FILE;
        if (!new File(path).exists()) return new HashMap<>();
        String json = new String(Files.readAllBytes(Paths.get(path)));
        return parseProjects(json);
    }

    // ── Serialisation (Task → JSON) ───────────────────────────────────────

    private String taskToJson(Task t) {
        StringBuilder sb = new StringBuilder("  {");
        sb.append(jf("taskId",      t.getTaskId())).append(",");
        sb.append(jf("title",       t.getTitle())).append(",");
        sb.append(jf("description", t.getDescription())).append(",");
        sb.append(jf("status",      t.getStatus().name())).append(",");
        sb.append(jf("priority",    t.getPriorityLevel().name())).append(",");
        sb.append(jf("dueDate",     t.getDueDate() != null
                                    ? t.getDueDate().toString() : null)).append(",");
        sb.append(jf("projectId",   t.getProject() != null
                                    ? t.getProject().getProjectId() : null)).append(",");
        sb.append(jf("isRecurring", String.valueOf(t.isRecurring()))).append(",");
        sb.append("\"subtasks\":[");
        List<Subtask> subs = t.getSubtasks();
        for (int i = 0; i < subs.size(); i++) {
            sb.append(subtaskToJson(subs.get(i)));
            if (i < subs.size() - 1) sb.append(",");
        }
        sb.append("]}");
        return sb.toString();
    }

    private String subtaskToJson(Subtask s) {
        return "{" + jf("subtaskId", s.getSubtaskId()) + ","
             + jf("title",  s.getTitle()) + ","
             + jf("status", s.getStatus().name()) + ","
             + jf("collaboratorId", s.getAssignedTo() != null
                                    ? s.getAssignedTo().getCollaboratorId()
                                    : null)
             + "}";
    }

    private String projectToJson(Project p) {
        StringBuilder sb = new StringBuilder("  {");
        sb.append(jf("projectId",   p.getProjectId())).append(",");
        sb.append(jf("name",        p.getName())).append(",");
        sb.append(jf("description", p.getDescription())).append(",");
        sb.append("\"collaborators\":[");
        List<Collaborator> collabs = p.getCollaborators();
        for (int i = 0; i < collabs.size(); i++) {
            sb.append(collaboratorToJson(collabs.get(i)));
            if (i < collabs.size() - 1) sb.append(",");
        }
        sb.append("]}");
        return sb.toString();
    }

    private String collaboratorToJson(Collaborator c) {
        return "{" + jf("collaboratorId", c.getCollaboratorId()) + ","
             + jf("name",     c.getName()) + ","
             + jf("category", c.getCategory().name()) + ","
             + "\"openTaskCount\":" + c.getOpenTaskCount()
             + "}";
    }

    // ── Deserialisation (JSON → Task) ─────────────────────────────────────

    private List<Task> parseTasks(String json) {
        List<Task> tasks = new ArrayList<>();
        for (String obj : splitJsonArray(json)) {
            try { tasks.add(parseTask(obj)); }
            catch (Exception e) {
                System.err.println("Warning: skipping malformed task: "
                    + e.getMessage());
            }
        }
        return tasks;
    }

    private Task parseTask(String obj) {
        String taskId   = extractString(obj, "taskId");
        String title    = extractString(obj, "title");
        String priority = extractString(obj, "priority");
        String status   = extractString(obj, "status");
        String dueDate  = extractString(obj, "dueDate");
        String projectId = extractString(obj, "projectId");

        Task task = new Task(taskId, title, PriorityLevel.valueOf(priority));
        task.setDescription(extractString(obj, "description"));
        task.setStatus(TaskStatus.valueOf(status));
        if (dueDate != null && !dueDate.equals("null") && !dueDate.isBlank()) {
            task.setDueDate(LocalDate.parse(dueDate));
        }
        // Store projectId as a tag; TaskService.load() resolves it later
        if (projectId != null && !projectId.equals("null") && !projectId.isBlank()) {
            task.getTags().add(new Tag("__projectId__:" + projectId));
        }

        // Parse subtasks
        for (String sObj : splitJsonArray(extractArray(obj, "subtasks"))) {
            if (sObj.isBlank()) continue;
            String sid      = extractString(sObj, "subtaskId");
            String stitle   = extractString(sObj, "title");
            String sstatus  = extractString(sObj, "status");
            String collabId = extractString(sObj, "collaboratorId");

            Subtask sub = new Subtask(sid, stitle);
            if (collabId != null && !collabId.equals("null") && !collabId.isBlank()) {
                // Placeholder — real collaborator resolved by TaskService.load()
                Collaborator placeholder = new Collaborator(
                    collabId, "__unresolved__", CollaboratorCategory.Junior);
                sub.assignCollaborator(placeholder);
            }
            // forceStatus avoids side-effects on collaborator counts during load
            sub.forceStatus(SubtaskStatus.valueOf(sstatus));
            task.getSubtasks().add(sub);
        }
        return task;
    }

    private Map<String, Project> parseProjects(String json) {
        Map<String, Project> map = new HashMap<>();
        for (String obj : splitJsonArray(json)) {
            try {
                Project p = parseProject(obj);
                map.put(p.getName().toLowerCase().trim(), p);
            } catch (Exception e) {
                System.err.println("Warning: skipping malformed project: "
                    + e.getMessage());
            }
        }
        return map;
    }

    private Project parseProject(String obj) {
        String id   = extractString(obj, "projectId");
        String name = extractString(obj, "name");
        String desc = extractString(obj, "description");
        Project p = new Project(id, name);
        p.setDescription(desc);
        for (String cObj : splitJsonArray(extractArray(obj, "collaborators"))) {
            if (cObj.isBlank()) continue;
            String cid   = extractString(cObj, "collaboratorId");
            String cname = extractString(cObj, "name");
            String cat   = extractString(cObj, "category");
            String count = extractString(cObj, "openTaskCount");
            Collaborator collab = new Collaborator(
                cid, cname, CollaboratorCategory.valueOf(cat));
            if (count != null && !count.isBlank() && !count.equals("null")) {
                collab.setOpenTaskCount(Integer.parseInt(count.trim()));
            }
            p.addCollaborator(collab);
        }
        return p;
    }

    // ── Minimal JSON utilities ─────────────────────────────────────────────

    private String jf(String key, String value) {
        if (value == null || value.equals("null"))
            return "\"" + key + "\":null";
        return "\"" + key + "\":\"" + value.replace("\\", "\\\\")
                                           .replace("\"", "\\\"") + "\"";
    }

    private String extractString(String json, String key) {
        String pattern = "\"" + key + "\":";
        int idx = json.indexOf(pattern);
        if (idx < 0) return null;
        int start = idx + pattern.length();
        if (start >= json.length()) return null;
        if (json.charAt(start) == 'n') return "null";
        if (json.charAt(start) != '"') {
            int end = json.indexOf(',', start);
            if (end < 0) end = json.indexOf('}', start);
            return end < 0 ? null : json.substring(start, end).trim();
        }
        StringBuilder sb = new StringBuilder();
        int i = start + 1;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                if (next == '"')  { sb.append('"');  i += 2; continue; }
                if (next == '\\') { sb.append('\\'); i += 2; continue; }
                if (next == 'n')  { sb.append('\n'); i += 2; continue; }
            }
            if (c == '"') break;
            sb.append(c);
            i++;
        }
        return sb.toString();
    }

    private String extractArray(String json, String key) {
        String pattern = "\"" + key + "\":[";
        int idx = json.indexOf(pattern);
        if (idx < 0) return "[]";
        int start = idx + pattern.length() - 1;
        int depth = 0, i = start;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '[') depth++;
            else if (c == ']') { depth--; if (depth == 0) return json.substring(start + 1, i); }
            i++;
        }
        return "";
    }

    private List<String> splitJsonArray(String body) {
        List<String> items = new ArrayList<>();
        if (body == null || body.isBlank()) return items;
        int depth = 0, start = -1;
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if ((c == '{' || c == '[') && depth == 0) { start = i; depth++; }
            else if (c == '{' || c == '[') depth++;
            else if (c == '}' || c == ']') {
                depth--;
                if (depth == 0 && start >= 0) {
                    items.add(body.substring(start, i + 1).trim());
                    start = -1;
                }
            }
        }
        return items;
    }

    private void write(String filename, String content) throws IOException {
        Files.write(Paths.get(dataDir, filename), content.getBytes());
    }
}
