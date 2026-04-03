package persistence;

import models.Project;
import models.Task;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Persistence repository interface.
 * The domain and service layers depend only on this interface,
 * keeping them independent of any concrete storage technology.
 */
public interface TaskRepository {

    /** Persist the full task list and project registry. */
    void save(List<Task> tasks, Map<String, Project> projects)
        throws IOException;

    /** Load tasks. Returns an empty list if no data exists yet. */
    List<Task> loadTasks() throws IOException;

    /** Load projects. Returns an empty map if no data exists yet. */
    Map<String, Project> loadProjects() throws IOException;
}
