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

The system returns taskCreated(taskId0
  
