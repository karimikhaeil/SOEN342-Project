package services;

import models.*;
import java.util.List;

/**
 * OCL Constraint Validator
 * Enforces business rules that cannot be fully captured by UML:
 * - A task cannot have more than 20 sub-tasks
 * - The number of open tasks without a due date should not exceed 50
 * - The limit for open tasks for each collaborator category is a positive integer
 * - No collaborator must be overloaded (open assigned tasks <= category limit)
 */
public class ConstraintValidator {
    
    private static final int MAX_SUBTASKS_PER_TASK = 20;
    private static final int MAX_OPEN_TASKS_WITHOUT_DUE_DATE = 50;

    /**
     * Validates that adding a subtask to a task does not exceed the 20 subtask limit.
     * @param task The task to validate
     * @return true if the task can accept another subtask
     */
    public static boolean canAddSubtask(Task task) {
        return task.getSubtasks().size() < MAX_SUBTASKS_PER_TASK;
    }

    /**
     * Gets the constraint violation message for subtask addition.
     * @return Error message if constraint is violated
     */
    public static String getSubtaskConstraintError(Task task) {
        if (!canAddSubtask(task)) {
            return "Task '" + task.getTitle() + "' has reached the maximum of " 
                + MAX_SUBTASKS_PER_TASK + " subtasks.";
        }
        return null;
    }

    /**
     * Validates that the system has not exceeded 50 open tasks without a due date.
     * @param allTasks List of all tasks in the system
     * @return true if the system can accept another open task without a due date
     */
    public static boolean canAddOpenTaskWithoutDueDate(List<Task> allTasks) {
        long openWithoutDueDate = allTasks.stream()
            .filter(t -> t.getStatus() == TaskStatus.open && t.getDueDate() == null)
            .count();
        return openWithoutDueDate < MAX_OPEN_TASKS_WITHOUT_DUE_DATE;
    }

    /**
     * Gets the constraint violation message for open tasks without due date.
     * @return Error message if constraint is violated
     */
    public static String getOpenTasksConstraintError(List<Task> allTasks) {
        if (!canAddOpenTaskWithoutDueDate(allTasks)) {
            return "System has reached the maximum of " 
                + MAX_OPEN_TASKS_WITHOUT_DUE_DATE 
                + " open tasks without a due date.";
        }
        return null;
    }

    /**
     * Validates that a collaborator is not overloaded.
     * Overload occurs when open assigned task count exceeds category limit.
     * @param collaborator The collaborator to validate
     * @return true if the collaborator is not overloaded
     */
    public static boolean isCollaboratorNotOverloaded(Collaborator collaborator) {
        return collaborator.getOpenTaskCount() <= collaborator.getOpenTaskLimit();
    }

    /**
     * Gets the constraint violation message for collaborator overload.
     * @return Error message if collaborator is overloaded, null otherwise
     */
    public static String getCollaboratorOverloadError(Collaborator collaborator) {
        if (!isCollaboratorNotOverloaded(collaborator)) {
            return "Collaborator '" + collaborator.getName() + "' (" 
                + collaborator.getCategory() + ") is overloaded: "
                + collaborator.getOpenTaskCount() + " open tasks exceeds limit of " 
                + collaborator.getOpenTaskLimit() + ".";
        }
        return null;
    }

    /**
     * Finds all overloaded collaborators in a list.
     * @param collaborators List of collaborators to check
     * @return List of overloaded collaborators
     */
    public static List<Collaborator> findOverloadedCollaborators(List<Collaborator> collaborators) {
        return collaborators.stream()
            .filter(c -> !isCollaboratorNotOverloaded(c))
            .toList();
    }
}
