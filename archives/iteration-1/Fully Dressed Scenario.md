# Fully Dressed Use Case: Create Task  

- Primary Actor: User  
- Goal: Create a new task in the system  
- Scope: Personal Task Management System  
- Level: User goal  

## Stakeholders and Interests
- User: wants to create a task quickly and correctly.
- System: must store the task and record a history entry.

## Trigger
- User selects the "Create Task" command.

## Preconditions
- The system is running.
- The user has access to the Create Task functionality.

## Postconditions on Success  
- A new Task is created.
- The Task has:
  - a unique taskId  
  - title set to the provided value  
  - status = open  
  - creationDate = currentDateTime  
  - priorityLevel, description, and dueDate set if provided  
- The system returns `taskCreated(taskId)`.

## Postconditions on Failure
- No Task is created.
- No ActivityEntry is created.
- The system returns `displayError("Task title is required")`.

## Main Success Scenario
1. The user initiates task creation.
2. The user provides title, priorityLevel, and optional description and dueDate.
3. The system validates the input.
4. The system creates the Task.
5. The system returns `taskCreated(taskId)`.

## Extensions

3a. Title is empty or invalid  
   1. The system detects that the title is empty or invalid.  
   2. The system returns `displayError("Task title is required")`.  
   3. The use case ends.
