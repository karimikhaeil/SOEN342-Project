# OCL Constraints – SOEN 342 Iteration 3

The following Object Constraint Language (OCL) constraints capture the
business rules that cannot be fully expressed in the UML class diagram alone.

## Constraint 1 – A task cannot have more than 20 sub-tasks
```ocl
context Task
inv MaxSubtasks:
  self.subtasks->size() <= 20
```

**Java enforcement:**
`TaskService.addSubtask()` throws `IllegalStateException` when
`task.getSubtasks().size() >= 20` before adding a new subtask.


## Constraint 2 – Open tasks without a due date must not exceed 50
```ocl
context TaskService
inv MaxNoDueDateOpenTasks:
  Task.allInstances()
    ->select(t | t.status = TaskStatus::open and t.dueDate.oclIsUndefined())
    ->size() <= 50
```

**Java enforcement:**
`TaskService.createTask()` counts existing open tasks with a `null` due date
before adding a new one and throws `IllegalStateException` when the count
would reach or exceed 50.


## Constraint 3 – The open-task limit per collaborator category is a positive integer
```ocl
context Collaborator
inv PositiveCategoryLimit:
  self.getOpenTaskLimit() > 0
```

**Java enforcement:**
The limits are declared as named constants in `Collaborator`:
`SENIOR_LIMIT = 2`, `INTERMEDIATE_LIMIT = 5`, `JUNIOR_LIMIT = 10`.
All three are positive integers, satisfying the invariant by construction.


## Constraint 4 – No collaborator must be overloaded
```ocl
context Collaborator
inv NoOverload:
  self.openTaskCount <= self.getOpenTaskLimit()
```

**Java enforcement (preventive):**
`Collaborator.canAcceptTask()` returns `false` when the limit is reached.
`Task.addSubtaskForCollaborator()` and `Subtask.reassignCollaborator()`
both throw `IllegalStateException` if the collaborator is at their limit.

**Java enforcement (detective):**
`TaskService.listOverloadedCollaborators()` returns all collaborators
where `openTaskCount > openTaskLimit`, powering the menu option that
lists overloaded collaborators.

