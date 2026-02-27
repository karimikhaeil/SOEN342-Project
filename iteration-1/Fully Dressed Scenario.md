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
- The system is running  
- The user has access to the create task functionality  

## Postconditions on Success  
- A new task is created  
- The task has: 
  - a unique taskId
  - title set to the provided value
  - status = open
  - creationDate = currentDateTime
  - priorityLevel, description, and dueDate set if provided
- The system returns taskCreated(taskId)

## Postconditions on Failure
- No Task object is created.
- No ActivityEntry is created.
- The system displays an appropriate error message.

## Main Success Scenario
1. The user initiates task creation
2. The user provides title, priorityLevel, and optional description and dueDate
3. The system validates the input
4. The system creates the Task
5. The system returns taskCreated(taskId)

## Extensions (Failure Cases)
4a. Title is empty  <br>
  4a1. System detects empty title. <br>
  4a2. System displays error message.<br>
  4a3. Use case ends. 
4b. Invalid priorityLevel
  4b1. System detects invalid priority.
  4b2. System displays error message.
  4b3. Use case ends.
4c. Invalid dueDate
  4c1. System detects invalid date.
  4c2. System displays error message.
  4c3. Use case ends.

## Failure Case
1. The system detects that the title is empty or invalid
2. The system returns displayError("Task title is required")
3. The use case ends without creating a task
  
