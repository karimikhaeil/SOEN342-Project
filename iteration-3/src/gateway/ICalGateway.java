package gateway;

import models.Task;
import java.io.IOException;
import java.util.List;

/**
 * Gateway interface that decouples the domain layer from the iCalendar
 * export mechanism. The domain only knows about this interface.
 * Gateway pattern: domain calls exportTask/exportTasks and is
 * completely shielded from iCal implementation details.
 */
public interface ICalGateway {

    /**
     * Export a single task to an .ics file.
     * Tasks without a due date are silently skipped.
     */
    void exportTask(Task task, String filePath) throws IOException;

    /**
     * Export a list of tasks to a single .ics file.
     * Tasks without a due date are silently skipped.
     */
    void exportTasks(List<Task> tasks, String filePath) throws IOException;
}
