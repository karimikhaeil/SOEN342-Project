# Fully Dressed Use Case: Create Task  

Primary Actor: User  
Goal: Create a new task in the system  
Scope: Personal Task Management System  
Level: User goal  

# Preconditions
The system is running  
The user has access to the create task functionality  

# Postconditions on Success  
A new task is created  
The task has: 
- a unique taskId
- title set to the provided value
- status = open
- creationDate = currentDateTime
- priorityLevel, description, and dueDate set if provided

The system returns taskCreated(taskId)

# Postconditions on Failure
No task is created  
The system returns displayError("Task title is required")

# Main Success Scenario
1. The user initiates task creation
2. The user provides title, priotityLevel, and optional description and dueDate
3. The system validates the input
4. The system created the Task
5. The system returns taskCreated(taskId)

# Failure Case
1. The system detects that the title is empty or invalid
2. The system returns displayError("Task title is required")
3. The use case ends without creating a task
  
