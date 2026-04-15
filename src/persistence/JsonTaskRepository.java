package persistence;

import models.*;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class JsonTaskRepository implements TaskRepository {

    private final String dataDir;
    private static final String TASKS_FILE = "tasks.json";
    private static final String PROJECTS_FILE = "projects.json";

    public JsonTaskRepository(String dataDir) {
        this.dataDir = dataDir;
        new File(dataDir).mkdirs();
    }

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

    private String taskToJson(Task task) {
        StringBuilder sb = new StringBuilder("  {");
        sb.append(jf("taskId", task.getTaskId())).append(",");
        sb.append(jf("title", task.getTitle())).append(",");
        sb.append(jf("description", task.getDescription())).append(",");
        sb.append(jf("creationDate", task.getCreationDate().toString())).append(",");
        sb.append(jf("status", task.getStatus().name())).append(",");
        sb.append(jf("priority", task.getPriorityLevel().name())).append(",");
        sb.append(jf("dueDate", task.getDueDate() != null ? task.getDueDate().toString() : null)).append(",");
        sb.append(jf("projectId", task.getProject() != null ? task.getProject().getProjectId() : null)).append(",");
        sb.append("\"isRecurring\":").append(task.isRecurring()).append(",");
        sb.append("\"tags\":").append(tagsToJson(task.getTags())).append(",");
        sb.append("\"history\":").append(historyToJson(task.getHistory())).append(",");
        if (task.getRecurrencePattern() != null) {
            sb.append("\"recurrence\":").append(recurrenceToJson(task.getRecurrencePattern())).append(",");
        } else {
            sb.append("\"recurrence\":null,");
        }
        sb.append("\"subtasks\":[");
        List<Subtask> subtasks = task.getSubtasks();
        for (int i = 0; i < subtasks.size(); i++) {
            sb.append(subtaskToJson(subtasks.get(i)));
            if (i < subtasks.size() - 1) sb.append(",");
        }
        sb.append("]}");
        return sb.toString();
    }

    private String tagsToJson(List<Tag> tags) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < tags.size(); i++) {
            sb.append(jf("name", tags.get(i).getName()).replaceFirst("^\"name\":", "{").concat("}"));
            if (i < tags.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private String historyToJson(List<ActivityEntry> history) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < history.size(); i++) {
            ActivityEntry entry = history.get(i);
            sb.append("{")
                .append(jf("entryId", entry.getEntryId())).append(",")
                .append(jf("actionType", entry.getActionType().name())).append(",")
                .append(jf("timestamp", entry.getTimestamp().toString()))
                .append("}");
            if (i < history.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private String recurrenceToJson(RecurrencePattern recurrence) {
        return "{"
            + jf("patternId", recurrence.getPatternId()) + ","
            + jf("type", recurrence.getType().name()) + ","
            + jf("startDate", recurrence.getStartDate().toString()) + ","
            + jf("endDate", recurrence.getEndDate().toString()) + ","
            + "\"interval\":" + recurrence.getInterval()
            + "}";
    }

    private String subtaskToJson(Subtask subtask) {
        return "{"
            + jf("subtaskId", subtask.getSubtaskId()) + ","
            + jf("title", subtask.getTitle()) + ","
            + jf("status", subtask.getStatus().name()) + ","
            + jf("collaboratorId", subtask.getAssignedTo() != null
                ? subtask.getAssignedTo().getCollaboratorId()
                : null)
            + "}";
    }

    private String projectToJson(Project project) {
        StringBuilder sb = new StringBuilder("  {");
        sb.append(jf("projectId", project.getProjectId())).append(",");
        sb.append(jf("name", project.getName())).append(",");
        sb.append(jf("description", project.getDescription())).append(",");
        sb.append("\"collaborators\":[");
        List<Collaborator> collaborators = project.getCollaborators();
        for (int i = 0; i < collaborators.size(); i++) {
            sb.append(collaboratorToJson(collaborators.get(i)));
            if (i < collaborators.size() - 1) sb.append(",");
        }
        sb.append("]}");
        return sb.toString();
    }

    private String collaboratorToJson(Collaborator collaborator) {
        return "{"
            + jf("collaboratorId", collaborator.getCollaboratorId()) + ","
            + jf("name", collaborator.getName()) + ","
            + jf("category", collaborator.getCategory().name()) + ","
            + "\"openTaskCount\":" + collaborator.getOpenTaskCount()
            + "}";
    }

    private List<Task> parseTasks(String json) {
        List<Task> tasks = new ArrayList<>();
        for (String obj : splitJsonArray(json)) {
            try {
                tasks.add(parseTask(obj));
            } catch (Exception e) {
                System.err.println("Warning: skipping malformed task: " + e.getMessage());
            }
        }
        return tasks;
    }

    private Task parseTask(String obj) {
        String taskId = extractString(obj, "taskId");
        String title = extractString(obj, "title");
        String priority = extractString(obj, "priority");
        String status = extractString(obj, "status");
        String dueDate = extractString(obj, "dueDate");
        String projectId = extractString(obj, "projectId");
        String creationDate = extractString(obj, "creationDate");

        Task task = new Task(taskId, title, PriorityLevel.valueOf(priority));
        task.setDescription(extractString(obj, "description"));
        task.setStatus(TaskStatus.valueOf(status));
        if (creationDate != null && !creationDate.equals("null") && !creationDate.isBlank()) {
            task.setCreationDate(LocalDateTime.parse(creationDate));
        }
        if (dueDate != null && !dueDate.equals("null") && !dueDate.isBlank()) {
            task.setDueDate(LocalDate.parse(dueDate));
        }
        if (projectId != null && !projectId.equals("null") && !projectId.isBlank()) {
            task.getTags().add(new Tag("__projectId__:" + projectId));
        }

        for (String tagObj : splitJsonArray(extractArray(obj, "tags"))) {
            if (tagObj.isBlank()) continue;
            String tagName = extractString(tagObj, "name");
            if (tagName != null && !tagName.isBlank()) {
                task.getTags().add(new Tag(tagName));
            }
        }

        for (String historyObj : splitJsonArray(extractArray(obj, "history"))) {
            if (historyObj.isBlank()) continue;
            String entryId = extractString(historyObj, "entryId");
            String actionType = extractString(historyObj, "actionType");
            String timestamp = extractString(historyObj, "timestamp");
            if (entryId != null && actionType != null && timestamp != null) {
                task.addActivityEntry(new ActivityEntry(
                    entryId,
                    ActionType.valueOf(actionType),
                    LocalDateTime.parse(timestamp)
                ));
            }
        }

        String recurrenceObj = extractObject(obj, "recurrence");
        if (recurrenceObj != null && !recurrenceObj.equals("null")) {
            RecurrencePattern recurrence = new RecurrencePattern(
                extractString(recurrenceObj, "patternId"),
                RecurrenceType.valueOf(extractString(recurrenceObj, "type")),
                LocalDate.parse(extractString(recurrenceObj, "startDate")),
                LocalDate.parse(extractString(recurrenceObj, "endDate")),
                Integer.parseInt(extractString(recurrenceObj, "interval"))
            );
            task.setRecurrencePattern(recurrence);
        }

        for (String subtaskObj : splitJsonArray(extractArray(obj, "subtasks"))) {
            if (subtaskObj.isBlank()) continue;
            String subtaskId = extractString(subtaskObj, "subtaskId");
            String subtaskTitle = extractString(subtaskObj, "title");
            String subtaskStatus = extractString(subtaskObj, "status");
            String collaboratorId = extractString(subtaskObj, "collaboratorId");

            Subtask subtask = new Subtask(subtaskId, subtaskTitle);
            if (collaboratorId != null && !collaboratorId.equals("null") && !collaboratorId.isBlank()) {
                Collaborator placeholder = new Collaborator(
                    collaboratorId, "__unresolved__", CollaboratorCategory.Junior);
                subtask.assignCollaborator(placeholder);
            }
            subtask.forceStatus(SubtaskStatus.valueOf(subtaskStatus));
            task.getSubtasks().add(subtask);
        }

        return task;
    }

    private Map<String, Project> parseProjects(String json) {
        Map<String, Project> map = new LinkedHashMap<>();
        for (String obj : splitJsonArray(json)) {
            try {
                Project project = parseProject(obj);
                map.put(project.getName().toLowerCase().trim(), project);
            } catch (Exception e) {
                System.err.println("Warning: skipping malformed project: " + e.getMessage());
            }
        }
        return map;
    }

    private Project parseProject(String obj) {
        String id = extractString(obj, "projectId");
        String name = extractString(obj, "name");
        String description = extractString(obj, "description");
        Project project = new Project(id, name);
        project.setDescription(description);
        for (String collaboratorObj : splitJsonArray(extractArray(obj, "collaborators"))) {
            if (collaboratorObj.isBlank()) continue;
            String collaboratorId = extractString(collaboratorObj, "collaboratorId");
            String collaboratorName = extractString(collaboratorObj, "name");
            String category = extractString(collaboratorObj, "category");
            String count = extractString(collaboratorObj, "openTaskCount");
            Collaborator collaborator = new Collaborator(
                collaboratorId,
                collaboratorName,
                CollaboratorCategory.valueOf(category)
            );
            if (count != null && !count.isBlank() && !count.equals("null")) {
                collaborator.setOpenTaskCount(Integer.parseInt(count.trim()));
            }
            project.addCollaborator(collaborator);
        }
        return project;
    }

    private String extractString(String json, String key) {
        String pattern = "\"" + key + "\":";
        int idx = json.indexOf(pattern);
        if (idx < 0) return null;
        int start = idx + pattern.length();
        if (start >= json.length()) return null;
        if (json.startsWith("null", start)) return "null";
        if (json.charAt(start) != '"') {
            int end = findPrimitiveEnd(json, start);
            return end < 0 ? null : json.substring(start, end).trim();
        }

        StringBuilder sb = new StringBuilder();
        int i = start + 1;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                if (next == '"') { sb.append('"'); i += 2; continue; }
                if (next == '\\') { sb.append('\\'); i += 2; continue; }
                if (next == 'n') { sb.append('\n'); i += 2; continue; }
                if (next == 'r') { sb.append('\r'); i += 2; continue; }
            }
            if (c == '"') break;
            sb.append(c);
            i++;
        }
        return sb.toString();
    }

    private int findPrimitiveEnd(String json, int start) {
        int i = start;
        while (i < json.length()
            && json.charAt(i) != ','
            && json.charAt(i) != '}'
            && json.charAt(i) != ']') {
            i++;
        }
        return i;
    }

    private String extractArray(String json, String key) {
        String pattern = "\"" + key + "\":[";
        int idx = json.indexOf(pattern);
        if (idx < 0) return "[]";
        int start = idx + pattern.length() - 1;
        int depth = 0;
        int i = start;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '[') depth++;
            else if (c == ']') {
                depth--;
                if (depth == 0) return json.substring(start + 1, i);
            }
            i++;
        }
        return "";
    }

    private String extractObject(String json, String key) {
        String pattern = "\"" + key + "\":";
        int idx = json.indexOf(pattern);
        if (idx < 0) return null;
        int start = idx + pattern.length();
        if (json.startsWith("null", start)) return "null";
        if (start >= json.length() || json.charAt(start) != '{') return null;
        int depth = 0;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) return json.substring(start, i + 1);
            }
        }
        return null;
    }

    private List<String> splitJsonArray(String body) {
        List<String> items = new ArrayList<>();
        if (body == null || body.isBlank()) return items;
        int depth = 0;
        int start = -1;
        boolean inString = false;

        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            char prev = i > 0 ? body.charAt(i - 1) : '\0';
            if (c == '"' && prev != '\\') {
                inString = !inString;
            }
            if (inString) continue;

            if ((c == '{' || c == '[') && depth == 0) {
                start = i;
                depth++;
            } else if (c == '{' || c == '[') {
                depth++;
            } else if (c == '}' || c == ']') {
                depth--;
                if (depth == 0 && start >= 0) {
                    items.add(body.substring(start, i + 1).trim());
                    start = -1;
                }
            }
        }
        return items;
    }

    private String jf(String key, String value) {
        if (value == null) {
            return "\"" + key + "\":null";
        }
        return "\"" + key + "\":\"" + escapeJson(value) + "\"";
    }

    private String escapeJson(String value) {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r");
    }

    private void write(String filename, String content) throws IOException {
        Files.write(Paths.get(dataDir, filename), content.getBytes());
    }
}
