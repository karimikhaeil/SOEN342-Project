# System Operations and Operation Contracts

## System Operations  
1. createTask(title, description?, priorityLevel, dueDate?)`
2. updateTask(taskId, title?, description?, priorityLevel?, dueDate?)`
3. changeTaskStatus(taskId, newStatus)`

# Operation Contracts

## 1. Operation Contract — createTask(title, description?, priorityLevel, dueDate?) 

### Preconditions
- `title` is not null and not empty.
- `priorityLevel` is a valid `PriorityLevel` value.
- `dueDate` is null or a valid date.

### Postconditions (Success)
- A new `Task t` is created.
- `t.taskId` is generated and unique.
- `t.title = title`
- `t.description = description` (if provided)
- `t.priorityLevel = priorityLevel`
- `t.dueDate = dueDate` (if provided)
- `t.creationDate = currentDateTime`
- `t.status = open`
- A new `ActivityEntry a` is created and associated with `t`:
  - `a.timestamp = currentDateTime`
  - `a.actionType = created`

### Postconditions (Failure)
- No `Task` is created.
- No `ActivityEntry` is created.

## 2. Operation Contract — updateTask(taskId, title?, description?, priorityLevel?, dueDate?)

### Preconditions
- A `Task t` exists such that `t.taskId = taskId`.
- If `title` is provided, it is not empty.
- If `priorityLevel` is provided, it is a valid `PriorityLevel` value.
- If `dueDate` is provided, it is a valid date.

### Postconditions (Success)
- The existing `Task t` is updated as follows:
  - If `title` is provided → `t.title = title`
  - If `description` is provided → `t.description = description`
  - If `priorityLevel` is provided → `t.priorityLevel = priorityLevel`
  - If `dueDate` is provided → `t.dueDate = dueDate`
- A new `ActivityEntry a` is created and associated with `t`:
  - `a.timestamp = currentDateTime`
  - `a.actionType = updated`

### Postconditions (Failure)
- If the task does not exist → no changes occur.
- If input data is invalid → no changes occur.
- No `ActivityEntry` is created.

## 3. Operation Contract — changeTaskStatus(taskId, newStatus) 

### Preconditions
- A `Task t` exists such that `t.taskId = taskId`.
- `newStatus` is a valid `TaskStatus` value.

### Postconditions (Success)
- `t.status = newStatus`
- A new `ActivityEntry a` is created and associated with `t`:
  - `a.timestamp = currentDateTime`
  - `a.actionType = newStatus`

### Postconditions (Failure)
- If the task does not exist → no changes occur.
- If `newStatus` is invalid → no changes occur.
- No `ActivityEntry` is created.
